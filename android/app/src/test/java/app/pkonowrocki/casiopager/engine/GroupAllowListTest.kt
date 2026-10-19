package app.pkonowrocki.casiopager.engine

import app.pkonowrocki.casiopager.model.AppMode
import app.pkonowrocki.casiopager.model.AppRule
import app.pkonowrocki.casiopager.model.Decision
import app.pkonowrocki.casiopager.model.DecisionEntry
import app.pkonowrocki.casiopager.model.GroupRule
import app.pkonowrocki.casiopager.model.IncomingEvent
import app.pkonowrocki.casiopager.model.Settings
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

private val NOON = LocalDateTime.of(2026, 10, 14, 12, 0)
private fun inGroup(title: String?, sender: String = "Anna", ts: Long = 1, app: String = "whatsapp") =
    IncomingEvent(app, sender, "k", ts, isGroup = true, isCall = false, groupTitle = title)

class GroupAllowListTest {
    private val family = GroupRule("whatsapp", "Family")

    @Test fun aGroupIsSilentUnlessItIsListedOrAllGroupsAreOn() {
        val e = AlertEngine()
        assertEquals(Decision.Ignore, e.decide(inGroup("Family"), Settings(), NOON, 0))
        assertEquals("group", e.lastIgnoreReason)
        assertEquals(Decision.Beep, AlertEngine().decide(inGroup("Family"), Settings(groupRules = listOf(family)), NOON, 0))
        assertEquals(Decision.Beep, AlertEngine().decide(inGroup("Other", "Anna"), Settings(alertInGroups = true, appRules = listOf(AppRule("whatsapp", AppMode.EVERYONE))), NOON, 0))
    }

    @Test fun aListedGroupBeepsForAnyMember() {
        val s = Settings(groupRules = listOf(family), appRules = listOf(AppRule("whatsapp", AppMode.PEOPLE)))
        assertEquals(Decision.Beep, AlertEngine().decide(inGroup("Family", sender = "Stranger"), s, NOON, 0))
    }

    @Test fun theGroupTitleIsMatchedLikeAName() {
        val s = Settings(groupRules = listOf(family))
        assertEquals(Decision.Beep, AlertEngine().decide(inGroup("  FAMILY "), s, NOON, 0))
        assertEquals(Decision.Ignore, AlertEngine().decide(inGroup("Family 2"), s, NOON, 0))
        assertEquals(Decision.Ignore, AlertEngine().decide(inGroup(null), s, NOON, 0))
    }

    @Test fun aGroupIsListedPerApp() {
        val s = Settings(groupRules = listOf(family))
        assertEquals(Decision.Ignore, AlertEngine().decide(inGroup("Family", app = "telegram"), s, NOON, 0))
    }

    @Test fun anAppSetToOffStaysSilentEvenForAListedGroup() {
        val s = Settings(groupRules = listOf(family), appRules = listOf(AppRule("whatsapp", AppMode.OFF)))
        val e = AlertEngine()
        assertEquals(Decision.Ignore, e.decide(inGroup("Family"), s, NOON, 0))
        assertEquals("app-off", e.lastIgnoreReason)
    }

    @Test fun throttleAndDedupeStillApplyToAListedGroup() {
        val s = Settings(groupRules = listOf(family))
        val e = AlertEngine()
        e.decide(inGroup("Family", ts = 1), s, NOON, 0)
        assertEquals(Decision.Ignore, e.decide(inGroup("Family", ts = 2), s, NOON, 1_000))
        assertEquals("throttle", e.lastIgnoreReason)
        assertEquals(Decision.Ignore, e.decide(inGroup("Family", ts = 1), s, NOON, 100_000))
        assertEquals("duplicate", e.lastIgnoreReason)
    }
}

class DecisionBufferTest {
    @Test fun keepsTheNewestThirtyNewestFirst() {
        var list = emptyList<DecisionEntry>()
        for (i in 1..35) list = pushDecision(list, DecisionEntry(i.toLong(), "sms", "no-rule"))
        assertEquals(DECISIONS_KEPT, list.size)
        assertEquals(35L, list.first().atMs)
        assertEquals(6L, list.last().atMs)
    }
}
