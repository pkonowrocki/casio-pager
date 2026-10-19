package app.pkonowrocki.casiopager.ui

import app.pkonowrocki.casiopager.R
import app.pkonowrocki.casiopager.model.AppMode
import app.pkonowrocki.casiopager.model.AppRule
import app.pkonowrocki.casiopager.model.Rule
import org.junit.Assert.assertEquals
import org.junit.Test

class RuleGroupsTest {
    @Test fun topAppsAreInTheOrderTheyAreShown() =
        assertEquals(listOf("messenger", "call", "sms", "whatsapp", "telegram"), TOP_APPS)

    @Test fun otherAppsAreTheOnesNotAtTheTopSortedAndWithoutDuplicates() =
        assertEquals(
            listOf("com.spotify.music", "org.signal"),
            otherApps(listOf("whatsapp", "org.signal", "com.spotify.music", "org.signal", "telegram")),
        )

    @Test fun appLabelsAreReadable() {
        assertEquals("WhatsApp", appLabel("whatsapp"))
        assertEquals("SMS", appLabel("sms"))
        assertEquals("Messenger", appLabel("messenger"))
        assertEquals("Phone call", appLabel("call"))
        assertEquals("Telegram", appLabel("telegram"))
        assertEquals("Signal", appLabel("signal"))
    }

    @Test fun anAppWithNothingSavedDefaultsToOnlyThePeopleListed() =
        assertEquals(AppRule("sms", AppMode.PEOPLE), appRuleFor(emptyList(), "sms"))

    @Test fun changingOneAppLeavesTheOthersAlone() {
        val saved = listOf(AppRule("sms", AppMode.EVERYONE))
        val changed = withAppRule(saved, "whatsapp") { it.copy(mode = AppMode.OFF) }
        assertEquals(AppRule("sms", AppMode.EVERYONE), changed.first { it.app == "sms" })
        assertEquals(AppMode.OFF, appRuleFor(changed, "whatsapp").mode)
        assertEquals(2, changed.size)
    }

    @Test fun peopleForAnAppKeepsTheOrderTheyWereAdded() {
        val rules = listOf(Rule("whatsapp", "Zed"), Rule("sms", "Anna"), Rule("whatsapp", "Amy"))
        assertEquals(listOf("Zed", "Amy"), peopleFor(rules, "whatsapp").map { it.sender })
        assertEquals(emptyList<Rule>(), peopleFor(rules, "telegram"))
    }

    @Test fun summaries() {
        assertEquals(ModeSummary(R.string.summary_off), modeSummary(AppMode.OFF, 3))
        assertEquals(ModeSummary(R.string.summary_everyone), modeSummary(AppMode.EVERYONE, 0))
        assertEquals(ModeSummary(R.plurals.summary_everyone_own, 2), modeSummary(AppMode.EVERYONE, 2))
        assertEquals(ModeSummary(R.string.summary_nobody), modeSummary(AppMode.PEOPLE, 0))
        assertEquals(ModeSummary(R.plurals.summary_people, 1), modeSummary(AppMode.PEOPLE, 1))
        assertEquals(ModeSummary(R.plurals.summary_people, 3), modeSummary(AppMode.PEOPLE, 3))
    }
}
