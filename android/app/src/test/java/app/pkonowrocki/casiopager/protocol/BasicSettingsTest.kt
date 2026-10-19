package app.pkonowrocki.casiopager.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }

/** The record as the watch sent it at 15:54:12 (power saving on, 24 h, button sound on, short light). */
private const val CAPTURED = "130500000000000000000000"

class BasicSettingsTest {
    private fun read() = BasicSettings.parse(hex(CAPTURED))!!

    @Test fun readsTheCapturedRecord() {
        val s = read()
        assertTrue(s.use24h)
        assertTrue(s.buttonSound)
        assertTrue(s.powerSaving)   // a CLEAR 0x10 bit means power saving is on
        assertFalse(s.longLight)
    }

    @Test fun longLightIsExactlyWhatTheCasioAppWroteAt155415() =
        assertEquals("130501000000000000000000", read().withLongLight(true).toPacket().hex())

    @Test fun powerSavingOffSetsTheBitAndOnClearsItAgain() {
        val off = read().withPowerSaving(false)
        assertFalse(off.powerSaving)
        assertEquals("131500000000000000000000", off.toPacket().hex())
        assertEquals(CAPTURED, off.withPowerSaving(true).toPacket().hex())
    }

    @Test fun buttonSoundAndTwentyFourHourBits() {
        assertEquals("130700000000000000000000", read().withButtonSound(false).toPacket().hex())
        assertEquals("130400000000000000000000", read().with24h(false).toPacket().hex())
        assertFalse(read().withButtonSound(false).buttonSound)
        assertFalse(read().with24h(false).use24h)
    }

    @Test fun aChangeTouchesOnlyItsOwnBitAndLeavesEveryOtherByteAlone() {
        val busy = BasicSettings.parse(hex("13ee5a1122334455667788aa"))!!   // arbitrary values everywhere
        val changed = busy.withPowerSaving(!busy.powerSaving).toPacket()
        val before = busy.toPacket()
        for (i in before.indices) {
            if (i == 1) assertEquals((before[1].toInt() xor 0x10).toByte(), changed[1]) else assertEquals("byte $i", before[i], changed[i])
        }
    }

    @Test fun rejectsAnythingThatIsNotABasicSettingsReply() {
        assertNull(BasicSettings.parse(hex("ff8113")))      // the watch's error packet
        assertNull(BasicSettings.parse(hex("11")))          // a different opcode
        assertNull(BasicSettings.parse(hex("1305")))        // too short to hold the light byte
        assertNull(BasicSettings.parse(byteArrayOf()))
        assertNotNull(BasicSettings.parse(hex("130500")))
    }

    @Test fun thePacketIsACopyNotTheInternalArray() {
        val s = read()
        s.toPacket()[1] = 0x7f
        assertEquals(CAPTURED, s.toPacket().hex())
    }
}
