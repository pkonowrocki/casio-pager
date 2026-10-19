package app.pkonowrocki.casiopager.ui

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockSyncTest {
    private val today = LocalDate.of(2026, 10, 15)

    @Test fun sameDayIsToday() = assertEquals(SyncDay.TODAY, syncDay(today, today))
    @Test fun theDayBeforeIsYesterdayEvenAcrossAMonth() {
        assertEquals(SyncDay.YESTERDAY, syncDay(today.minusDays(1), today))
        assertEquals(SyncDay.YESTERDAY, syncDay(LocalDate.of(2026, 9, 30), LocalDate.of(2026, 10, 1)))
    }
    @Test fun olderIsEarlier() = assertEquals(SyncDay.EARLIER, syncDay(today.minusDays(2), today))
}
