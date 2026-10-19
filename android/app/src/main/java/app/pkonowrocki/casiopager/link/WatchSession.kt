package app.pkonowrocki.casiopager.link

import app.pkonowrocki.casiopager.protocol.BasicSettings
import app.pkonowrocki.casiopager.protocol.ConnectionSettings
import app.pkonowrocki.casiopager.protocol.TimerRecord
import app.pkonowrocki.casiopager.protocol.Gatt
import app.pkonowrocki.casiopager.protocol.LifeLog
import app.pkonowrocki.casiopager.protocol.LifeLogParser
import app.pkonowrocki.casiopager.protocol.INIT_SEQUENCE
import app.pkonowrocki.casiopager.protocol.InitStep
import app.pkonowrocki.casiopager.protocol.Packets
import app.pkonowrocki.casiopager.protocol.Replies
import java.time.LocalDateTime
import kotlinx.coroutines.delay

interface WatchPort {
    suspend fun write(uuid: String, bytes: ByteArray, noResponse: Boolean): Boolean
    suspend fun awaitReply(op: Int, timeoutMs: Long): ByteArray?

    /** Collects notifications from one characteristic until at least [minBytes] arrived; null on timeout. */
    suspend fun awaitData(uuid: String, minBytes: Int, timeoutMs: Long): ByteArray? = null

    /** Forgets anything already received on that characteristic, so the next read only sees fresh data. */
    fun discardData(uuid: String) {}
}

/** [ok] is false when a write failed (the link closed), so the caller must not report the link as held. */
fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

data class InitResult(val kind: Int?, val timeouts: Int, val ok: Boolean = true)

class WatchSession(
    private val port: WatchPort,
    private val clock: () -> LocalDateTime = { LocalDateTime.now() },
) {
    suspend fun initialise(): InitResult {
        var kind: Int? = null
        var timeouts = 0
        for (step in INIT_SEQUENCE) {
            when (step) {
                is InitStep.Ask -> {
                    if (!port.write(Gatt.REQUEST, Packets.request(step.op), true)) return InitResult(kind, timeouts, ok = false)
                    val reply = port.awaitReply(step.op, REPLY_TIMEOUT_MS)
                    if (reply == null) timeouts++ // the script also carries on after a missing reply
                    if (step.op == 0x10 && reply != null) kind = Replies.connectionKind(reply)
                    if (step.echo && reply != null && !port.write(Gatt.DATA, reply, false)) {
                        return InitResult(kind, timeouts, ok = false)
                    }
                }
                InitStep.WriteName ->
                    if (!port.write(Gatt.DATA, Packets.watchName, false)) return InitResult(kind, timeouts, ok = false)
                InitStep.WriteTime ->
                    if (!port.write(Gatt.DATA, Packets.timeWrite(clock()), false)) return InitResult(kind, timeouts, ok = false)
            }
            delay(STEP_GAP_MS)
        }
        return InitResult(kind, timeouts)
    }

    suspend fun keepalive(): Boolean = port.write(Gatt.REQUEST, Packets.keepalive, true)

    /**
     * Raw reply to opcode 0x28 ("watch condition": battery and temperature on other Casio models). On the ABL-100WE it
     * has returned the same bytes for hours, so we only log it and look for a change; null when there is no reply.
     */
    suspend fun condition(): ByteArray? =
        if (port.write(Gatt.REQUEST, Packets.request(0x28), true)) port.awaitReply(0x28, REPLY_TIMEOUT_MS) else null

    /**
     * Reads the step counter the way the Casio app does: `00 11 00 00 00` to characteristic 23, the 400-byte blob arrives
     * on 24, then (when [finish]) the closing `04 11 00 00 00`. The watch answers only one request per open transfer, so
     * if the first try gets silence a transfer from earlier is closed and the read is retried once. `finish = false` is the
     * library's "peek" read; it leaves the transfer open, so the next read needs that recovery.
     */
    suspend fun readLifeLog(finish: Boolean = true): LifeLog? {
        repeat(2) { attempt ->
            port.discardData(Gatt.BULK)
            if (!port.write(Gatt.CMD, LIFELOG_START, false)) return null
            val blob = port.awaitData(Gatt.BULK, LIFELOG_BYTES, LIFELOG_TIMEOUT_MS)
            if (blob != null) {
                if (finish) port.write(Gatt.CMD, LIFELOG_END, false)
                return LifeLogParser.parse(blob)
            }
            if (attempt == 0) port.write(Gatt.CMD, LIFELOG_END, false) // close whatever was left open, then ask again
        }
        return null
    }

    /** Reads the watch's basic settings (opcode 0x13); null when there is no usable reply. */
    suspend fun readBasic(): BasicSettings? = readRecord(0x13, BasicSettings::parse)

    /**
     * Read-modify-write like the Casio app: read the record, change it, write the WHOLE record back, then read it again
     * and return what the watch now holds. Nothing is written when the current record cannot be read or the change is
     * invalid; null on any failure.
     */
    suspend fun changeBasic(change: (BasicSettings) -> BasicSettings): BasicSettings? =
        changeRecord(0x13, BasicSettings::parse, BasicSettings::toPacket, change)

    suspend fun readTimer(): TimerRecord? = readRecord(0x18, TimerRecord::parse)

    suspend fun changeTimer(change: (TimerRecord) -> TimerRecord): TimerRecord? =
        changeRecord(0x18, TimerRecord::parse, TimerRecord::toPacket, change)

    suspend fun readConnection(): ConnectionSettings? = readRecord(0x11, ConnectionSettings::parse)

    suspend fun changeConnection(change: (ConnectionSettings) -> ConnectionSettings): ConnectionSettings? =
        changeRecord(0x11, ConnectionSettings::parse, ConnectionSettings::toPacket, change)

    private suspend fun <T> readRecord(op: Int, parse: (ByteArray) -> T?): T? {
        if (!port.write(Gatt.REQUEST, Packets.request(op), true)) return null
        return port.awaitReply(op, REPLY_TIMEOUT_MS)?.let(parse)
    }

    private suspend fun <T> changeRecord(op: Int, parse: (ByteArray) -> T?, packet: (T) -> ByteArray, change: (T) -> T): T? {
        val current = readRecord(op, parse) ?: return null
        val next = runCatching { change(current) }.getOrNull() ?: return null   // an invalid change writes nothing
        if (!port.write(Gatt.DATA, packet(next), false)) return null
        return readRecord(op, parse)
    }

    suspend fun alert(): Boolean = port.write(Gatt.DATA, Packets.alert, false)

    private companion object {
        const val REPLY_TIMEOUT_MS = 3_000L
        const val LIFELOG_BYTES = 400
        const val LIFELOG_TIMEOUT_MS = 4_000L
        val LIFELOG_START = byteArrayOf(0x00, 0x11, 0x00, 0x00, 0x00)
        val LIFELOG_END = byteArrayOf(0x04, 0x11, 0x00, 0x00, 0x00)
        const val STEP_GAP_MS = 150L
    }
}
