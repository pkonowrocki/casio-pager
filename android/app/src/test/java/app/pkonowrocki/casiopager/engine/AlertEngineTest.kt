package app.pkonowrocki.casiopager.engine

import app.pkonowrocki.casiopager.model.*
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val NOON = LocalDateTime.of(2026, 10, 13, 12, 0) // a Tuesday

private fun msg(sender: String = "Anna", app: String = "whatsapp", ts: Long = 1, key: String = "k1", group: Boolean = false) =
    IncomingEvent(app, sender, key, ts, isGroup = group, isCall = false)

private fun call(sender: String = "Anna") = IncomingEvent("call", sender, "call-key", 0, isGroup = false, isCall = true)

private val ANNA = Rule("whatsapp", "Anna")
private fun settings(vararg rules: Rule, q: QuietHours = QuietHours(), group: Boolean = false) =
    Settings(rules = rules.toList(), quietHours = q, alertInGroups = group)

class AlertEngineTest {
    @Test fun noRuleIsSilent() =
        assertEquals(Decision.Ignore, AlertEngine().decide(msg(), settings(), NOON, 0))

    @Test fun matchingRuleBeeps() =
        assertEquals(Decision.Beep, AlertEngine().decide(msg(), settings(ANNA), NOON, 0))

    @Test fun wrongAppDoesNotMatch() =
        assertEquals(Decision.Ignore, AlertEngine().decide(msg(app = "sms"), settings(ANNA), NOON, 0))

    @Test fun namesMatchDespiteCaseNbspDirectionMarksAndSpaces() {
        val e = AlertEngine()
        val s = settings(Rule("whatsapp", "Anna  Kowalska"))
        assertEquals(Decision.Beep, e.decide(msg("‎anna KOWALSKA ", ts = 1), s, NOON, 0))
    }

    @Test fun blankNamesNeverMatch() {
        val e = AlertEngine()
        assertEquals(Decision.Ignore, e.decide(msg(sender = ""), settings(Rule("whatsapp", "")), NOON, 0))
        assertEquals(Decision.Ignore, e.decide(msg(sender = "  "), settings(Rule("whatsapp", " ")), NOON, 10_000))
    }

    @Test fun groupsAreIgnoredUnlessEnabled() {
        assertEquals(Decision.Ignore, AlertEngine().decide(msg(group = true), settings(ANNA), NOON, 0))
        assertEquals(Decision.Beep, AlertEngine().decide(msg(group = true), settings(ANNA, group = true), NOON, 0))
    }

    @Test fun sameMessageRepostedBeepsOnce() {
        val e = AlertEngine()
        assertEquals(Decision.Beep, e.decide(msg(ts = 5), settings(ANNA), NOON, 0))
        assertEquals(Decision.Ignore, e.decide(msg(ts = 5), settings(ANNA), NOON, 60_000))
        assertEquals(Decision.Beep, e.decide(msg(ts = 6), settings(ANNA), NOON, 120_000))
    }

    @Test fun burstInsideThrottleBeepsOnce() {
        val e = AlertEngine()
        val s = settings(ANNA).copy(throttleMs = 3_000)
        assertEquals(Decision.Beep, e.decide(msg(ts = 1), s, NOON, 1_000))
        assertEquals(Decision.Ignore, e.decide(msg(ts = 2), s, NOON, 2_000))
        assertEquals(Decision.Beep, e.decide(msg(ts = 3), s, NOON, 4_001))
    }

    @Test fun callsRepeatAtMostEveryFiveSeconds() {
        val e = AlertEngine()
        val s = settings(Rule("call", "Anna"))
        assertEquals(Decision.Beep, e.decide(call(), s, NOON, 0))
        assertEquals(Decision.Ignore, e.decide(call(), s, NOON, 4_999))
        assertEquals(Decision.Beep, e.decide(call(), s, NOON, 5_000))
    }

    // ---- quiet hours ----
    private val night = QuietHours(enabled = true, start = LocalTime.of(22, 0), end = LocalTime.of(7, 0))

    @Test fun quietHoursSameDayWindow() {
        val q = QuietHours(enabled = true, start = LocalTime.of(13, 0), end = LocalTime.of(15, 0))
        assertFalse(isQuiet(q, LocalDateTime.of(2026, 10, 13, 12, 59)))
        assertTrue(isQuiet(q, LocalDateTime.of(2026, 10, 13, 13, 0)))
        assertFalse(isQuiet(q, LocalDateTime.of(2026, 10, 13, 15, 0)))
    }

    @Test fun quietHoursCrossMidnight() {
        assertTrue(isQuiet(night, LocalDateTime.of(2026, 10, 13, 23, 0)))
        assertTrue(isQuiet(night, LocalDateTime.of(2026, 10, 14, 6, 59)))
        assertFalse(isQuiet(night, LocalDateTime.of(2026, 10, 14, 7, 0)))
    }

    @Test fun quietDaysBelongToTheDayTheWindowStarts() {
        val fridayOnly = night.copy(days = setOf(DayOfWeek.FRIDAY))
        assertTrue(isQuiet(fridayOnly, LocalDateTime.of(2026, 10, 16, 23, 0)))  // Friday 23:00
        assertTrue(isQuiet(fridayOnly, LocalDateTime.of(2026, 10, 17, 2, 0)))   // Saturday 02:00
        assertFalse(isQuiet(fridayOnly, LocalDateTime.of(2026, 10, 18, 2, 0)))  // Sunday 02:00
        assertFalse(isQuiet(fridayOnly, LocalDateTime.of(2026, 10, 17, 23, 0))) // Saturday 23:00
    }

    @Test fun startEqualsEndMeansOff() =
        assertFalse(isQuiet(night.copy(start = LocalTime.of(8, 0), end = LocalTime.of(8, 0)), NOON))

    @Test fun quietHoursSilenceMessagesButCallsAndAlwaysBeepBreakThrough() {
        val q = night
        val at2am = LocalDateTime.of(2026, 10, 14, 2, 0)
        assertEquals(Decision.Ignore, AlertEngine().decide(msg(), settings(ANNA, q = q), at2am, 0))
        assertEquals(Decision.Beep,
            AlertEngine().decide(msg(), settings(ANNA.copy(alwaysBeep = true), q = q), at2am, 0))
        assertEquals(Decision.Beep,
            AlertEngine().decide(call(), settings(Rule("call", "Anna"), q = q), at2am, 0))
        assertEquals(Decision.Ignore,
            AlertEngine().decide(call(), settings(Rule("call", "Anna"), q = q.copy(callsBypass = false)), at2am, 0))
    }
}

class AppModeTest {
    private fun app(mode: AppMode, alwaysBeep: Boolean = false, q: QuietHours = QuietHours()) =
        Settings(appRules = listOf(AppRule("whatsapp", mode, alwaysBeep)), quietHours = q)

    @Test fun everyoneBeepsForAnySender() {
        val s = app(AppMode.EVERYONE)
        assertEquals(Decision.Beep, AlertEngine().decide(msg("Stranger", ts = 1), s, NOON, 0))
    }

    @Test fun everyoneDoesNotLeakIntoOtherApps() =
        assertEquals(Decision.Ignore, AlertEngine().decide(msg("Stranger", app = "sms"), app(AppMode.EVERYONE), NOON, 0))

    @Test fun aPersonRuleOverridesTheAppDefaultInEveryoneMode() {
        val s = app(AppMode.EVERYONE).copy(rules = listOf(ANNA))
        assertEquals(Decision.Beep, AlertEngine().decide(msg("Anna", ts = 1), s, NOON, 0))
        assertEquals(Decision.Beep, AlertEngine().decide(msg("Bob", ts = 2, key = "k2"), s, NOON, 10_000))
    }

    @Test fun offSilencesTheAppEvenForListedPeople() {
        val s = app(AppMode.OFF).copy(rules = listOf(ANNA))
        assertEquals(Decision.Ignore, AlertEngine().decide(msg("Anna"), s, NOON, 0))
    }

    @Test fun offSaysWhy() {
        val e = AlertEngine()
        e.decide(msg("Anna"), app(AppMode.OFF), NOON, 0)
        assertEquals("app-off", e.lastIgnoreReason)
    }

    @Test fun peopleModeNeedsTheSenderListed() {
        val s = app(AppMode.PEOPLE).copy(rules = listOf(ANNA))
        assertEquals(Decision.Ignore, AlertEngine().decide(msg("Bob"), s, NOON, 0))
        assertEquals(Decision.Beep, AlertEngine().decide(msg("Anna", ts = 2, key = "k2"), s, NOON, 0))
    }

    @Test fun anAppWithNoSavedModeBehavesAsPeopleOnly() =
        assertEquals(Decision.Ignore, AlertEngine().decide(msg("Stranger"), Settings(), NOON, 0))

    @Test fun everyoneInQuietHoursNeedsTheAlwaysBeepSwitch() {
        val q = QuietHours(enabled = true, days = DayOfWeek.values().toSet())
        assertEquals(Decision.Ignore, AlertEngine().decide(msg("Stranger"), app(AppMode.EVERYONE, q = q), NOON.withHour(23), 0))
        assertEquals(
            Decision.Beep,
            AlertEngine().decide(msg("Stranger", key = "k3"), app(AppMode.EVERYONE, alwaysBeep = true, q = q), NOON.withHour(23), 0),
        )
    }
}
