package app.pkonowrocki.casiopager.ui

import app.pkonowrocki.casiopager.R
import app.pkonowrocki.casiopager.model.GroupRule
import app.pkonowrocki.casiopager.model.SeenGroup
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupSuggestionsTest {
    private val rules = listOf(GroupRule("whatsapp", "Family"), GroupRule("telegram", "Dev"))

    @Test fun groupsForKeepsOneAppsListInOrder() {
        assertEquals(listOf(GroupRule("whatsapp", "Family")), groupsFor(rules, "whatsapp"))
        assertEquals(emptyList<GroupRule>(), groupsFor(rules, "sms"))
    }

    @Test fun suggestionsLeaveOutAllowedGroupsAndOtherApps() {
        val seen = listOf(SeenGroup("whatsapp", " family "), SeenGroup("whatsapp", "Work"), SeenGroup("telegram", "Work"))
        assertEquals(listOf(SeenGroup("whatsapp", "Work")), groupSuggestions(seen, rules, "whatsapp"))
    }
}

class DecisionLabelTest {
    @Test fun everyEngineReasonHasItsOwnText() {
        assertEquals(R.string.decision_no_rule, decisionLabel("no-rule"))
        assertEquals(R.string.decision_group, decisionLabel("group"))
        assertEquals(R.string.decision_app_off, decisionLabel("app-off"))
        assertEquals(R.string.decision_quiet, decisionLabel("quiet-hours"))
        assertEquals(R.string.decision_duplicate, decisionLabel("duplicate"))
        assertEquals(R.string.decision_throttle, decisionLabel("throttle"))
        assertEquals(R.string.decision_call_repeat, decisionLabel("call-repeat-gap"))
        assertEquals(R.string.decision_beep, decisionLabel("beep"))
        assertEquals(R.string.decision_no_link, decisionLabel("no-link"))
        assertEquals(R.string.decision_other, decisionLabel("blank-sender"))
    }
}
