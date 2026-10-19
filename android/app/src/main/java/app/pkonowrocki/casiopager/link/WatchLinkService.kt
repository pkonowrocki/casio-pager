package app.pkonowrocki.casiopager.link

import app.pkonowrocki.casiopager.R
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.pkonowrocki.casiopager.Pager
import app.pkonowrocki.casiopager.log.LokiPusher
import app.pkonowrocki.casiopager.log.PagerLog
import app.pkonowrocki.casiopager.model.WatchAddress
import app.pkonowrocki.casiopager.protocol.BasicSettings
import app.pkonowrocki.casiopager.protocol.ConnectionSettings
import app.pkonowrocki.casiopager.protocol.LifeLog
import app.pkonowrocki.casiopager.protocol.TimerRecord
import app.pkonowrocki.casiopager.protocol.Gatt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@SuppressLint("MissingPermission")
class WatchLinkService : Service(), AlertSink, WatchControl {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    @Volatile private var session: WatchSession? = null

    override fun onBind(intent: Intent?): IBinder? = null

    private val bluetoothState = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                BluetoothAdapter.STATE_ON -> "on"
                BluetoothAdapter.STATE_OFF -> "off"
                BluetoothAdapter.STATE_TURNING_ON -> "turning on"
                BluetoothAdapter.STATE_TURNING_OFF -> "turning off"
                else -> "unknown"
            }
            PagerLog.i("bluetooth adapter $state")
        }
    }

    override fun onCreate() {
        super.onCreate()
        Pager.init(this)
        ContextCompat.registerReceiver(
            this, bluetoothState, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    private fun phoneBattery(): Int =
        getSystemService(BatteryManager::class.java).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Pager.init(this)
        startInForeground(getString(R.string.fg_waiting))
        Pager.link = this
        Pager.control = this
        PagerLog.i("service started")
        if (job?.isActive != true) {
            job = scope.launch { runLoop() }
            LokiPusher.start(scope)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        PagerLog.i("service stopped")
        runCatching { unregisterReceiver(bluetoothState) }
        Pager.link = null
        Pager.control = null
        Pager.linkState.value = LinkState.Idle
        scope.cancel()
        super.onDestroy()
    }

    override suspend fun alert(): Boolean {
        val s = session
        if (s == null || Pager.linkState.value !is LinkState.Held) {
            PagerLog.i("alert skipped: no link")
            return false
        }
        val sent = s.alert()
        if (!sent) PagerLog.i("alert write failed")
        return sent
    }

    private fun heldSession(): WatchSession? = session?.takeIf { Pager.linkState.value is LinkState.Held }

    override suspend fun readBasic(): BasicSettings? {
        val s = heldSession() ?: return null
        return s.readBasic().also { PagerLog.i("watch13 read " + (it?.toPacket()?.toHex() ?: "failed")) }
    }

    override suspend fun changeBasic(change: (BasicSettings) -> BasicSettings): BasicSettings? {
        val s = heldSession() ?: return null
        return s.changeBasic(change).also { PagerLog.i("watch13 changed, now " + (it?.toPacket()?.toHex() ?: "FAILED")) }
    }

    override suspend fun readLifeLog(): LifeLog? {
        val s = heldSession() ?: return null
        return s.readLifeLog().also {
            PagerLog.i("lifelog read " + if (it != null) "ok" else "failed") // no step numbers in the log
            it?.let { l -> Pager.recordLifeLog(l, System.currentTimeMillis()) }
        }
    }

    /** One Life Log read into the stored history; logs only the outcome, never a number. */
    private suspend fun syncSteps(s: WatchSession, what: String): Boolean {
        val log = s.readLifeLog()
        PagerLog.i("$what " + if (log != null) "ok" else "failed")
        log?.let { Pager.recordLifeLog(it, System.currentTimeMillis()) }
        return log != null
    }

    override suspend fun readTimer(): TimerRecord? {
        val s = heldSession() ?: return null
        return s.readTimer().also { PagerLog.i("watch18 read " + (it?.toPacket()?.toHex() ?: "failed")) }
    }

    override suspend fun changeTimer(change: (TimerRecord) -> TimerRecord): TimerRecord? {
        val s = heldSession() ?: return null
        return s.changeTimer(change).also { PagerLog.i("watch18 changed, now " + (it?.toPacket()?.toHex() ?: "FAILED")) }
    }

    override suspend fun readConnection(): ConnectionSettings? {
        val s = heldSession() ?: return null
        return s.readConnection().also { PagerLog.i("watch11 read " + (it?.toPacket()?.toHex() ?: "failed")) }
    }

    override suspend fun changeConnection(change: (ConnectionSettings) -> ConnectionSettings): ConnectionSettings? {
        val s = heldSession() ?: return null
        return s.changeConnection(change).also { PagerLog.i("watch11 changed, now " + (it?.toPacket()?.toHex() ?: "FAILED")) }
    }

    private suspend fun runLoop() {
        while (true) {
            val address = WatchAddress.parse(Pager.settings.value.watchAddress)
            if (address == null) {
                PagerLog.i(if (Pager.settings.value.watchAddress.isBlank()) "no watch paired" else "invalid watch address in settings")
                Pager.linkState.value = LinkState.NotPaired
                delay(5_000)
                continue
            }
            var client: GattClient? = null
            try {
                val adapter = getSystemService(BluetoothManager::class.java).adapter
                val c = GattClient(this, adapter.getRemoteDevice(address)).also { client = it }
                Pager.linkState.value = LinkState.Connecting
                c.connect()
                c.enableNotifications(Gatt.DATA, *Gatt.NOTIFY_ONLY.toTypedArray())
                Pager.linkState.value = LinkState.Initialising
                val s = WatchSession(c)
                session = s
                val init = s.initialise()
                if (!init.ok) error("init failed: the link closed during setup")
                Pager.recordClockSync(System.currentTimeMillis())
                Pager.linkState.value = LinkState.Held(init.kind)
                Pager.linkSinceMs.value = System.currentTimeMillis()
                startInForeground(if (init.kind == 1) getString(R.string.fg_linked) else getString(R.string.fg_linked_short, init.kind))
                PagerLog.i("link held kind=${init.kind} initTimeouts=${init.timeouts}")
                val heldSince = System.currentTimeMillis()
                var lastStepsMs = heldSince // the read right after the link counts as the first one
                if (init.kind == 1) {
                    syncSteps(s, "steps sync")
                } else if (c.closed.isCompleted) {
                    PagerLog.i("window lifelog skipped, link already closed") // learning whether a read fits in the ~8 s window
                } else {
                    syncSteps(s, "window lifelog")
                }
                while (true) {
                    val status = withTimeoutOrNull(KEEPALIVE_MS) { c.closed.await() }
                    if (status != null) {
                        PagerLog.i("link closed by the stack status=$status after ${(System.currentTimeMillis() - heldSince) / 1000}s")
                        break
                    }
                    if (!s.keepalive()) { PagerLog.i("keepalive failed"); break }
                    val condition = s.condition()?.toHex() ?: "none"
                    PagerLog.i("keepalive ok held=${(System.currentTimeMillis() - heldSince) / 1000}s phone=${phoneBattery()}% watch28=$condition")
                    // the keepalive stays the plain read; steps are fetched on their own, slower, cadence
                    if (System.currentTimeMillis() - lastStepsMs >= STEPS_EVERY_MS) {
                        syncSteps(s, "steps sync")
                        lastStepsMs = System.currentTimeMillis()
                    }
                }
            } catch (e: CancellationException) {
                client?.close(); throw e
            } catch (e: Exception) {
                PagerLog.i("link error ${e.javaClass.simpleName}")
            } finally {
                session = null
                client?.close()
            }
            PagerLog.i("link lost")
            Pager.linkSinceMs.value = null
            Pager.linkState.value = LinkState.Lost
            startInForeground(getString(R.string.fg_lost))
            delay(2_000)
        }
    }

    private fun startInForeground(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW))
        val n: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle(getString(R.string.app_name)).setContentText(text).setOngoing(true).build()
        ServiceCompat.startForeground(this, 1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
    }

    private companion object {
        const val CHANNEL = "link"
        const val KEEPALIVE_MS = 150_000L
        const val STEPS_EVERY_MS = 20 * 60_000L // ponytail: fixed 20 min, a setting if anyone asks
    }
}
