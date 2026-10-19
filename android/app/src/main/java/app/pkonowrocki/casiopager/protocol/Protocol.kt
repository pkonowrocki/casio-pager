package app.pkonowrocki.casiopager.protocol

import java.time.LocalDateTime

object Gatt {
    const val SERVICE = "26eb000d-b012-49a8-b1f8-394fb2032b0f"
    private fun char(n: Int) = "26eb00%02x-b012-49a8-b1f8-394fb2032b0f".format(n)
    val REQUEST = char(0x2C) // write without response: "read opcode X" requests
    val DATA = char(0x2D)    // notify + write: replies, and opcode writes (alerts, time, echoes)
    val CMD = char(0x23)   // notify + write: bulk-transfer commands such as the Life Log request, and the watch's answers
    val BULK = char(0x24)  // notify: bulk data (the 400-byte Life Log blob)
    val NOTIFY_ONLY = listOf(CMD, BULK, char(0x2F)) // enabled like the official app
    const val CCCD = "00002902-0000-1000-8000-00805f9b34fb"
}

object Packets {
    fun request(op: Int) = byteArrayOf(op.toByte())
    val alert = byteArrayOf(0x01, 0x01) // 01 02 sounds the same on the ABL-100WE, so there is only one
    val keepalive = request(0x11)
    val watchName = "23434153494f2041424c2d313030574500000000".chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    /** `09 yearLE month day hour minute second dayOfWeek(Mon=1) fraction(1/256 s) 01` */
    fun timeWrite(t: LocalDateTime): ByteArray {
        val fraction = (t.nano.toLong() * 256 / 1_000_000_000L).toInt()
        return byteArrayOf(
            0x09,
            (t.year and 0xFF).toByte(), (t.year shr 8).toByte(),
            t.monthValue.toByte(), t.dayOfMonth.toByte(),
            t.hour.toByte(), t.minute.toByte(), t.second.toByte(),
            t.dayOfWeek.value.toByte(), fraction.toByte(), 0x01,
        )
    }
}

object Replies {
    fun opcode(p: ByteArray): Int? = p.firstOrNull()?.toInt()?.and(0xFF)

    /** Reply to opcode 0x10: 1 = long-press session (held), 3 = auto time, 4 = button (watch closes it). */
    fun connectionKind(p: ByteArray): Int? =
        if (p.size > 8 && opcode(p) == 0x10) p[8].toInt() and 0xFF else null
}

sealed interface InitStep {
    /** Write a request, wait for the reply with the same opcode, optionally write that reply straight back. */
    data class Ask(val op: Int, val echo: Boolean = false) : InitStep
    data object WriteName : InitStep
    data object WriteTime : InitStep
}

/** The init sequence the official app uses on connect (see docs/PROTOCOL.md); with the time write it holds the link. */
val INIT_SEQUENCE: List<InitStep> = listOf(
    InitStep.Ask(0x22), InitStep.Ask(0x10), InitStep.WriteName,
    InitStep.Ask(0x11, echo = true), InitStep.Ask(0x26), InitStep.Ask(0x28),
    InitStep.Ask(0x20), InitStep.Ask(0x28), InitStep.Ask(0x20), InitStep.Ask(0x28),
    InitStep.Ask(0x1D, echo = true), InitStep.Ask(0x1E, echo = true), InitStep.Ask(0x1E, echo = true),
    InitStep.WriteTime,
)
