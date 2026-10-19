package app.pkonowrocki.casiopager.storage

import app.pkonowrocki.casiopager.protocol.DaySummary
import app.pkonowrocki.casiopager.protocol.LifeLog
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class StepsHistoryTest {
    private val d = LocalDate.of(2026, 10, 15)
    private fun log(steps: Int?, vararg days: DaySummary?) =
        LifeLog(LocalDateTime.of(2026, 10, 15, 18, 0), steps, 500, List(24) { null }, days.toList())

    @Test fun todayAndTheStoredSummariesBecomeDatedEntriesNewestFirst() {
        val h = StepsHistory.merge(emptyList(), log(3000, DaySummary(8000, 6000), null, DaySummary(5000, 3500)))
        assertEquals(listOf(d, d.minusDays(1), d.minusDays(3)), h.map { it.date })
        assertEquals(listOf(3000, 8000, 5000), h.map { it.steps })
        assertEquals(500, h[0].distanceMeters)
    }

    @Test fun theLatestReadWinsPerDay() {
        val first = StepsHistory.merge(emptyList(), log(1000))
        val second = StepsHistory.merge(first, log(4000))
        assertEquals(1, second.size)
        assertEquals(4000, second[0].steps)
    }

    @Test fun olderDaysAreKeptWhenTheWatchNoLongerHoldsThem() {
        val old = listOf(DayEntry(d.minusDays(30), 7000, 5000))
        val h = StepsHistory.merge(old, log(100))
        assertEquals(listOf(d, d.minusDays(30)), h.map { it.date })
    }

    @Test fun aReadWithoutTodaysCounterStillStoresTheSummaries() {
        val h = StepsHistory.merge(emptyList(), log(null, DaySummary(9000, 7000)))
        assertEquals(listOf(d.minusDays(1)), h.map { it.date })
    }

    @Test fun theHistoryIsCapped() {
        val big = (0 until 450).map { DayEntry(d.minusDays(100L + it), it, it) }
        val h = StepsHistory.merge(big, log(1))
        assertEquals(StepsHistory.CAP, h.size)
        assertEquals(d, h[0].date)
    }

    @Test fun encodeDecodeRoundTripsAndBadInputIsEmpty() {
        val l = listOf(DayEntry(d, 1, 2), DayEntry(d.minusDays(1), 3, 4))
        assertEquals(l, StepsHistory.decode(StepsHistory.encode(l)))
        assertEquals(emptyList<DayEntry>(), StepsHistory.decode(null))
        assertEquals(emptyList<DayEntry>(), StepsHistory.decode("not json"))
    }
}
