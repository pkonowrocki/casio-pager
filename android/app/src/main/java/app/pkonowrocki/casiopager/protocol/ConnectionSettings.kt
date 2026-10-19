package app.pkonowrocki.casiopager.protocol

/**
 * The watch's Bluetooth settings record (opcode 0x11, 15 bytes with the opcode). The last byte is "Connection time with
 * the app" in minutes (3, 5 or 10; verified: the app changed `0a` to `03`); it does not limit a held link. Bytes 12 and 13
 * are the hour and minute of the first automatic connection of the day, bit 7 of byte 12 switching automatic connections
 * off (from the app's code; the windows themselves are not measured yet). Everything else is kept as read.
 */
class ConnectionSettings private constructor(private val raw: ByteArray) {
    val minutes: Int get() = raw[14].toInt() and 0xFF
    val autoReconnect: Boolean get() = raw[12].toInt() and 0x80 == 0
    val reconnectHour: Int get() = raw[12].toInt() and 0x7F
    val reconnectMinute: Int get() = raw[13].toInt() and 0xFF

    fun withMinutes(minutes: Int): ConnectionSettings {
        require(minutes in OFFERED) { "connection time $minutes min is not one of $OFFERED" }
        val copy = raw.copyOf()
        copy[14] = minutes.toByte()
        return ConnectionSettings(copy)
    }

    fun toPacket(): ByteArray = raw.copyOf()

    companion object {
        val OFFERED = listOf(3, 5, 10)

        fun parse(packet: ByteArray): ConnectionSettings? =
            if (packet.size >= 15 && packet[0] == 0x11.toByte()) ConnectionSettings(packet.copyOf()) else null
    }
}
