package app.pkonowrocki.casiopager.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }

class TimerRecordTest {
    private val threeMinutes = "1800030000aabbcc"   // 18 | 00:03:00 | trailing bytes the watch owns

    @Test fun readsHoursMinutesSecondsAndTheTotal() {
        val t = TimerRecord.parse(hex(threeMinutes))!!
        assertEquals(0, t.hours); assertEquals(3, t.minutes); assertEquals(0, t.seconds)
        assertEquals(180, t.totalSeconds)
    }

    @Test fun aNewDurationChangesOnlyTheThreeDurationBytes() {
        val t = TimerRecord.parse(hex(threeMinutes))!!.withDuration(1, 30, 15)
        assertEquals("18011e0f00aabbcc", t.toPacket().hex())   // hours 01, minutes 1e, seconds 0f; opcode and tail untouched
        assertEquals(1 * 3600 + 30 * 60 + 15, t.totalSeconds)
    }

    @Test fun rejectsDurationsTheWatchCannotHold() {
        val t = TimerRecord.parse(hex(threeMinutes))!!
        assertThrows(IllegalArgumentException::class.java) { t.withDuration(24, 0, 0) }
        assertThrows(IllegalArgumentException::class.java) { t.withDuration(0, 60, 0) }
        assertThrows(IllegalArgumentException::class.java) { t.withDuration(0, 0, 60) }
        assertThrows(IllegalArgumentException::class.java) { t.withDuration(-1, 0, 0) }
    }

    @Test fun rejectsAnythingThatIsNotATimerReply() {
        assertNull(TimerRecord.parse(hex("ff8118")))
        assertNull(TimerRecord.parse(hex("1300030000")))
        assertNull(TimerRecord.parse(hex("180003")))
        assertNull(TimerRecord.parse(byteArrayOf()))
    }
}

class ConnectionSettingsTest {
    /** The record as the watch sent it in the 15:51 session, connection time still at 10 minutes. */
    private val tenMinutes = "110f0f0f0600500004000100001e0a"

    @Test fun readsTheRealRecord() {
        val c = ConnectionSettings.parse(hex(tenMinutes))!!
        assertEquals(10, c.minutes)
        assertTrue(c.autoReconnect)
        assertEquals(0, c.reconnectHour)
        assertEquals(30, c.reconnectMinute)
    }

    @Test fun threeMinutesIsExactlyWhatTheCasioAppLeftBehind() =
        assertEquals("110f0f0f0600500004000100001e03", ConnectionSettings.parse(hex(tenMinutes))!!.withMinutes(3).toPacket().hex())

    @Test fun onlyTheLastByteChanges() {
        val before = hex(tenMinutes)
        val after = ConnectionSettings.parse(before)!!.withMinutes(5).toPacket()
        for (i in 0 until before.size - 1) assertEquals("byte $i", before[i], after[i])
        assertEquals(5.toByte(), after.last())
    }

    @Test fun onlyTheThreeOfferedLengthsAreAccepted() {
        val c = ConnectionSettings.parse(hex(tenMinutes))!!
        listOf(3, 5, 10).forEach { assertEquals(it, c.withMinutes(it).minutes) }
        listOf(0, 1, 4, 11, 60).forEach { m -> assertThrows(IllegalArgumentException::class.java) { c.withMinutes(m) } }
    }

    @Test fun theAutoReconnectFlagIsBit7OfTheHourByte() {
        val off = hex("110f0f0f0600500004000100801e0a")   // hour byte 0x80: flag set, hour 0
        val c = ConnectionSettings.parse(off)!!
        assertFalse(c.autoReconnect)
        assertEquals(0, c.reconnectHour)
    }

    @Test fun rejectsWrongOpcodeOrTooShort() {
        assertNull(ConnectionSettings.parse(hex("ff8111")))
        assertNull(ConnectionSettings.parse(hex("130f0f0f0600500004000100001e0a")))
        assertNull(ConnectionSettings.parse(hex("110f0f0f06")))
    }
}
