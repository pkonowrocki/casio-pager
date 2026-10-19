package app.pkonowrocki.casiopager.link

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import app.pkonowrocki.casiopager.log.PagerLog
import app.pkonowrocki.casiopager.protocol.Gatt
import app.pkonowrocki.casiopager.protocol.Replies
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

@SuppressLint("MissingPermission")
class GattClient(private val ctx: Context, private val device: BluetoothDevice) : WatchPort {
    private var gatt: BluetoothGatt? = null
    private val ready = CompletableDeferred<Boolean>()
    val closed = CompletableDeferred<Int>()
    private val replies = Channel<ByteArray>(Channel.UNLIMITED)
    private val bulk = Channel<ByteArray>(Channel.UNLIMITED) // characteristic 24: the Life Log blob arrives here
    private val lock = Mutex()
    private var pendingWrite: CompletableDeferred<Int>? = null
    private var pendingDescriptor: CompletableDeferred<Int>? = null

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) g.discoverServices()
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                ready.complete(false)
                closed.complete(status)
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            ready.complete(status == BluetoothGatt.GATT_SUCCESS)
        }

        override fun onCharacteristicWrite(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
            pendingWrite?.complete(status)
        }

        override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) {
            pendingDescriptor?.complete(status)
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray) =
            handle(c, value)

        @Deprecated("API < 33")
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT < 33) handle(c, c.value ?: return)
        }

        private fun handle(c: BluetoothGattCharacteristic, value: ByteArray) {
            when (c.uuid.toString()) {
                Gatt.DATA -> replies.trySend(value.copyOf())
                Gatt.BULK -> bulk.trySend(value.copyOf())
            }
        }
    }

    /** autoConnect = true: the stack connects as soon as the watch advertises (a long-press on the watch). */
    suspend fun connect() {
        gatt = device.connectGatt(ctx, true, callback, BluetoothDevice.TRANSPORT_LE)
        check(ready.await()) { "gatt not ready" }
    }

    suspend fun enableNotifications(vararg uuids: String) {
        val g = gatt ?: error("not connected")
        val service = g.getService(UUID.fromString(Gatt.SERVICE)) ?: error("service missing")
        for (u in uuids) {
            val c = service.getCharacteristic(UUID.fromString(u)) ?: continue
            g.setCharacteristicNotification(c, true)
            val d = c.getDescriptor(UUID.fromString(Gatt.CCCD)) ?: continue
            val done = CompletableDeferred<Int>().also { pendingDescriptor = it }
            if (Build.VERSION.SDK_INT >= 33) {
                g.writeDescriptor(d, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                run { d.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE; g.writeDescriptor(d) }
            }
            withTimeoutOrNull(3_000) { done.await() } ?: PagerLog.i("descriptor write timeout")
        }
    }

    override suspend fun write(uuid: String, bytes: ByteArray, noResponse: Boolean): Boolean = lock.withLock {
        val g = gatt ?: return false
        if (closed.isCompleted) return false
        val c = g.getService(UUID.fromString(Gatt.SERVICE))?.getCharacteristic(UUID.fromString(uuid)) ?: return false
        val type = if (noResponse) BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE else BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        val done = CompletableDeferred<Int>().also { pendingWrite = it }
        val started = if (Build.VERSION.SDK_INT >= 33) {
            g.writeCharacteristic(c, bytes, type) == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            run { c.writeType = type; c.value = bytes; g.writeCharacteristic(c) }
        }
        if (!started) return false
        val status = withTimeoutOrNull(3_000) { done.await() }
        status == BluetoothGatt.GATT_SUCCESS
    }

    override suspend fun awaitReply(op: Int, timeoutMs: Long): ByteArray? = withTimeoutOrNull(timeoutMs) {
        var r = replies.receive()
        while (Replies.opcode(r) != op) r = replies.receive()
        r
    }

    override suspend fun awaitData(uuid: String, minBytes: Int, timeoutMs: Long): ByteArray? {
        if (uuid != Gatt.BULK) return null
        return withTimeoutOrNull(timeoutMs) {
            var all = ByteArray(0)
            while (all.size < minBytes) all += bulk.receive()
            all
        }
    }

    override fun discardData(uuid: String) {
        if (uuid == Gatt.BULK) while (bulk.tryReceive().isSuccess) { /* drop stale chunks */ }
    }

    fun close() {
        gatt?.close()
        gatt = null
        closed.complete(-1)
    }
}
