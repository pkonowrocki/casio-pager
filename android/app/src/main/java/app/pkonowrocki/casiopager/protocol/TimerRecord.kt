package app.pkonowrocki.casiopager.protocol

/**
 * The watch's countdown timer (opcode 0x18): `18 hours minutes seconds ...`. Setting it does not start it, you start it
 * with the watch's buttons. Everything after the duration is kept exactly as the watch sent it.
 */
class TimerRecord private constructor(private val raw: ByteArray) {
    val hours: Int get() = raw[1].toInt() and 0xFF
    val minutes: Int get() = raw[2].toInt() and 0xFF
    val seconds: Int get() = raw[3].toInt() and 0xFF
    val totalSeconds: Int get() = hours * 3600 + minutes * 60 + seconds

    fun withDuration(hours: Int, minutes: Int, seconds: Int): TimerRecord {
        require(hours in 0..23 && minutes in 0..59 && seconds in 0..59) { "timer $hours:$minutes:$seconds is out of range" }
        val copy = raw.copyOf()
        copy[1] = hours.toByte(); copy[2] = minutes.toByte(); copy[3] = seconds.toByte()
        return TimerRecord(copy)
    }

    fun toPacket(): ByteArray = raw.copyOf()

    companion object {
        fun parse(packet: ByteArray): TimerRecord? =
            if (packet.size >= 4 && packet[0] == 0x18.toByte()) TimerRecord(packet.copyOf()) else null
    }
}
