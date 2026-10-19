package app.pkonowrocki.casiopager.engine

import app.pkonowrocki.casiopager.model.*
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

private val NOON2 = LocalDateTime.of(2026, 10, 13, 12, 0)
private val RULE = Rule("whatsapp", "Anna")
private val CALL_RULE = Rule("call", "Anna")
private val S = Settings(rules = listOf(RULE, CALL_RULE))
private fun message(ts: Long) = IncomingEvent("whatsapp", "Anna", "k", ts, isGroup = false, isCall = false)
private val RING = IncomingEvent("call", "Anna", "c", 0, isGroup = false, isCall = true)

class AlertEngineRollbackTest {
    @Test fun aBeepThatNeverReachedTheWatchDoesNotStartTheThrottleOrUseUpTheMessage() {
        val e = AlertEngine()
        assertEquals(Decision.Beep, e.decide(message(5), S, NOON2, 1_000))
        e.beepFailed()
        assertEquals(Decision.Beep, e.decide(message(5), S, NOON2, 1_500)) // same message, inside the throttle window
    }

    @Test fun aFailedCallBeepDoesNotStartTheRepeatGap() {
        val e = AlertEngine()
        assertEquals(Decision.Beep, e.decide(RING, S, NOON2, 0))
        e.beepFailed()
        assertEquals(Decision.Beep, e.decide(RING, S, NOON2, 1_000))
    }

    @Test fun aSuccessfulBeepStillThrottles() {
        val e = AlertEngine()
        assertEquals(Decision.Beep, e.decide(message(1), S, NOON2, 1_000))
        assertEquals(Decision.Ignore, e.decide(message(2), S, NOON2, 1_500))
    }
}
