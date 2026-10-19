package app.pkonowrocki.casiopager.protocol

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

/** A real blob from the Casio app's sync at 15:51 on 2026-10-13: the watch had not counted a step yet. */
private const val CAPTURED_EMPTY = "261013155101fefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffefffeff000000000000000000000000000000000000000000000000fefffffffefffffffefffffffefffffffefffffffefffffffefffffffefffffffefffffffefffffffefffffffefffffffefffffffeffffff0000000000000000000000000000000000000000000000000000"

/** Builds a 400-byte blob with the library's layout so the arithmetic can be checked. */
private class BlobBuilder(year: Int = 26, month: Int = 10, day: Int = 14, hour: Int = 10, minute: Int = 20, second: Int = 30) {
    val b = ByteArray(400)

    init {
        for (i in 6 until 146 step 2) { b[i] = 0xFE.toByte(); b[i + 1] = 0xFF.toByte() }   // unused buckets
        for (i in 318 until 374) b[i] = 0xFF.toByte()                                       // no daily history
        listOf(year, month, day, hour, minute, second).forEachIndexed { i, v -> b[i] = ((v / 10 shl 4) or (v % 10)).toByte() }
    }

    fun u16(offset: Int, v: Int) { b[offset] = v.toByte(); b[offset + 1] = (v shr 8).toByte() }
    fun u32(offset: Int, v: Int) { u16(offset, v and 0xFFFF); u16(offset + 2, v ushr 16) }
    fun record(index: Int, vararg buckets: Int) = buckets.forEachIndexed { i, v -> u16(6 + index * 10 + i * 2, v) }
}

class LifeLogTest {
    @Test fun aRealEmptyBlobDecodesToZeroSteps() {
        val l = LifeLogParser.parse(hex(CAPTURED_EMPTY))!!
        assertEquals(LocalDateTime.of(2026, 10, 13, 15, 51, 1), l.stamp)
        assertEquals(0, l.stepsToday)
        assertEquals(0, l.distanceMeters)
        assertEquals(List(7) { null }, l.lastDays)
        assertEquals(List(24) { null }, l.hourlySteps)
    }

    @Test fun todaysStepsHourlyRecordsAndPendingHourAreReconciled() {
        val w = BlobBuilder(hour = 10)
        w.record(0, 10, 20, 30, 40, 50)      // the last full hour: 09:00, 150 steps
        w.record(1, 100, 0, 0, 0, 0)         // 08:00, 100 steps
        w.u16(382, 5); w.u16(384, 5); w.u16(386, 5)   // the hour in progress: 15 steps so far
        w.u32(374, 265)                      // today = 150 + 100 + 15
        w.u32(378, 190)
        val l = LifeLogParser.parse(w.b)!!
        assertEquals(265, l.stepsToday)
        assertEquals(190, l.distanceMeters)
        assertEquals(150, l.hourlySteps[9])
        assertEquals(100, l.hourlySteps[8])
        assertEquals(15, l.hourlySteps[10])
        assertEquals(265, l.hourlySteps.filterNotNull().sum())
    }

    @Test fun dailyHistoryKeepsStepsAndDistanceAndTreatsTheSentinelAsMissing() {
        val w = BlobBuilder()
        w.u32(374, 0); w.u32(378, 0)
        w.u32(318, 4000); w.u32(322, 3000)
        val l = LifeLogParser.parse(w.b)!!
        assertEquals(DaySummary(4000, 3000), l.lastDays[0])
        assertNull(l.lastDays[1])
    }

    @Test fun worksInYearsOtherThanTheOneThatLookedLikeAMarker() {
        val l = LifeLogParser.parse(BlobBuilder(year = 27).b)!!
        assertEquals(2027, l.stamp.year)
    }

    @Test fun rejectsBlobsThatAreTooShortOrHaveNoSensibleTimestamp() {
        assertNull(LifeLogParser.parse(ByteArray(0)))
        assertNull(LifeLogParser.parse(ByteArray(300)))
        val bad = BlobBuilder().b
        bad[1] = 0x1A.toByte()                 // month 1A is not BCD
        assertNull(LifeLogParser.parse(bad))
        val month13 = BlobBuilder(month = 13).b
        assertNull(LifeLogParser.parse(month13))
        assertNotNull(LifeLogParser.parse(BlobBuilder().b))
    }
}
