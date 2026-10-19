package app.pkonowrocki.casiopager.protocol

/**
 * The watch's basic-settings record (opcode 0x13, 12 bytes including the opcode on the ABL-100WE).
 * The bits used: byte 1 bit 0x01 = 24 h display, 0x02 = button sound, 0x10 = power saving; byte 2 bit 0x01 = light
 * duration. For sound and power saving a CLEAR bit means ON (polarity taken from the open-source library; the captured
 * record agrees with the defaults). Every other byte is kept exactly as read, so a change never disturbs other settings.
 */
class BasicSettings private constructor(private val raw: ByteArray) {
    val use24h: Boolean get() = has(1, 0x01)
    val buttonSound: Boolean get() = !has(1, 0x02)
    val powerSaving: Boolean get() = !has(1, 0x10)
    val longLight: Boolean get() = has(2, 0x01)

    fun with24h(on: Boolean) = withBit(1, 0x01, on)
    fun withButtonSound(on: Boolean) = withBit(1, 0x02, !on)
    fun withPowerSaving(on: Boolean) = withBit(1, 0x10, !on)
    fun withLongLight(long: Boolean) = withBit(2, 0x01, long)

    /** The full record to write back to the watch (a copy). */
    fun toPacket(): ByteArray = raw.copyOf()

    private fun has(index: Int, mask: Int) = raw[index].toInt() and mask != 0

    private fun withBit(index: Int, mask: Int, set: Boolean): BasicSettings {
        val copy = raw.copyOf()
        val v = copy[index].toInt()
        copy[index] = (if (set) v or mask else v and mask.inv()).toByte()
        return BasicSettings(copy)
    }

    companion object {
        /** Null for anything that is not a basic-settings reply (an error packet, another opcode, too short). */
        fun parse(packet: ByteArray): BasicSettings? =
            if (packet.size >= 3 && packet[0] == 0x13.toByte()) BasicSettings(packet.copyOf()) else null
    }
}
