package app.pkonowrocki.casiopager.storage

import app.pkonowrocki.casiopager.model.GroupRule
import app.pkonowrocki.casiopager.model.SeenGroup
import app.pkonowrocki.casiopager.model.Settings
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupRulesCodecTest {
    @Test fun allowedGroupsRoundTrip() {
        val s = Settings(groupRules = listOf(GroupRule("whatsapp", "Family"), GroupRule("telegram", "Dev")), alertInGroups = true)
        assertEquals(s.groupRules, SettingsCodec.decode(SettingsCodec.encode(s)).groupRules)
    }

    @Test fun settingsSavedBeforeGroupRulesKeepEverythingAndStartWithNone() {
        val old = """{"rules":[{"app":"sms","sender":"Anna","tone":"CHIRP","alwaysBeep":false}],"alertInGroups":true,"throttleMs":4000}"""
        val d = SettingsCodec.decode(old)
        assertEquals(emptyList<GroupRule>(), d.groupRules)
        assertEquals(true, d.alertInGroups)
        assertEquals(4000L, d.throttleMs)
        assertEquals(1, d.rules.size)
    }

    @Test fun seenGroupsRoundTripAndSurviveGarbage() {
        val seen = listOf(SeenGroup("whatsapp", "Family"), SeenGroup("telegram", "Dev"))
        assertEquals(seen, SettingsCodec.decodeSeenGroups(SettingsCodec.encodeSeenGroups(seen)))
        assertEquals(emptyList<SeenGroup>(), SettingsCodec.decodeSeenGroups("garbage"))
        assertEquals(emptyList<SeenGroup>(), SettingsCodec.decodeSeenGroups(null))
    }
}
