package app.pkonowrocki.casiopager.engine

import app.pkonowrocki.casiopager.model.*
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private val AT = LocalDateTime.of(2026, 10, 14, 2, 0) // a Wednesday, 02:00
private val R = Rule("whatsapp", "Anna")
private fun m(sender: String = "Anna", ts: Long = 1, group: Boolean = false) =
    IncomingEvent("whatsapp", sender, "k", ts, isGroup = group, isCall = false)
private fun ring() = IncomingEvent("call", "Anna", "c", 0, isGroup = false, isCall = true)

class AlertEngineReasonTest {
    private fun reason(e: AlertEngine, ev: IncomingEvent, s: Settings, nowMs: Long = 0, at: LocalDateTime = AT): String? {
        e.decide(ev, s, at, nowMs)
        return e.lastIgnoreReason
    }

    @Test fun explainsWhyEachMessageStayedSilent() {
        val s = Settings(rules = listOf(R))
        assertEquals("blank-sender", reason(AlertEngine(), m(sender = " "), s))
        assertEquals("group", reason(AlertEngine(), m(group = true), s))
        assertEquals("no-rule", reason(AlertEngine(), m(sender = "Bob"), s))

        val e = AlertEngine()
        e.decide(m(ts = 5), s, AT, 0)
        assertEquals("duplicate", reason(e, m(ts = 5), s, 100))
        assertEquals("throttle", reason(e, m(ts = 6), s, 1_000))

        val quiet = s.copy(quietHours = QuietHours(true, LocalTime.of(22, 0), LocalTime.of(7, 0)))
        assertEquals("quiet-hours", reason(AlertEngine(), m(), quiet))
    }

    @Test fun explainsACallThatRepeatedTooSoon() {
        val s = Settings(rules = listOf(Rule("call", "Anna")))
        val e = AlertEngine()
        e.decide(ring(), s, AT, 0)
        assertEquals("call-repeat-gap", reason(e, ring(), s, 1_000))
    }

    @Test fun aBeepClearsTheReason() {
        val e = AlertEngine()
        e.decide(m(sender = "Bob"), Settings(rules = listOf(R)), AT, 0)         // ignored: no-rule
        e.decide(m(ts = 2), Settings(rules = listOf(R)), AT, 10_000)             // a different message: beeps
        assertNull(e.lastIgnoreReason)
    }
}
