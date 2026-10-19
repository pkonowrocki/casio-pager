package app.pkonowrocki.casiopager.storage

import app.pkonowrocki.casiopager.model.*
import java.time.DayOfWeek
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsCodecTest {
    @Test fun roundTrip() {
        val s = Settings(
            rules = listOf(Rule("whatsapp", "Anna ❤", alwaysBeep = true), Rule("call", "Anna")),
            quietHours = QuietHours(true, LocalTime.of(23, 30), LocalTime.of(6, 15), setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY), false),
            throttleMs = 4000, callRepeatMs = 7000, alertInGroups = true,
            watchAddress = "AA:BB:CC:DD:EE:FF", lokiUrl = "http://loki.lan:3100",
        )
        assertEquals(s, SettingsCodec.decode(SettingsCodec.encode(s)))
    }

    @Test fun appModesRoundTripAndOldSettingsWithoutThemAreStillValid() {
        val s = Settings(appRules = listOf(AppRule("whatsapp", AppMode.EVERYONE, true), AppRule("org.signal", AppMode.OFF)))
        assertEquals(s, SettingsCodec.decode(SettingsCodec.encode(s)))
        val before = SettingsCodec.decode("{\"rules\":[{\"app\":\"sms\",\"sender\":\"Anna\"}]}")
        assertEquals(emptyList<AppRule>(), before.appRules)
        assertEquals(listOf(Rule("sms", "Anna")), before.rules)
    }

    @Test fun corruptOrMissingGivesDefaults() {
        assertEquals(Settings(), SettingsCodec.decode(null))
        assertEquals(Settings(), SettingsCodec.decode("{not json"))
        assertEquals(Settings(), SettingsCodec.decode("{\"quiet\":{\"start\":\"99:99\"}}"))
    }

    @Test fun seenSendersRoundTrip() {
        val seen = listOf(SeenSender("sms", "Anna"), SeenSender("whatsapp", "Bob"))
        assertEquals(seen, SettingsCodec.decodeSeen(SettingsCodec.encodeSeen(seen)))
        assertEquals(emptyList<SeenSender>(), SettingsCodec.decodeSeen("garbage"))
    }
}

class SettingsOnboardedTest {
    @Test fun aFreshInstallStartsUnonboardedAndTheFlagIsKept() {
        assertEquals(false, SettingsCodec.decode(null).onboarded)
        assertEquals(true, SettingsCodec.decode(SettingsCodec.encode(Settings(onboarded = true))).onboarded)
        assertEquals(false, SettingsCodec.decode(SettingsCodec.encode(Settings(onboarded = false))).onboarded)
    }

    @Test fun settingsSavedBeforeTheFirstRunScreenCountAsOnboarded() =
        assertEquals(true, SettingsCodec.decode("""{"throttleMs":3000}""").onboarded)

    @Test fun anOldToneKeyIsIgnoredAndNeverWrittenAgain() {
        val old = """{"rules":[{"app":"sms","sender":"Anna","tone":"DOUBLE","alwaysBeep":true}],"appRules":[{"app":"sms","mode":"EVERYONE","tone":"DOUBLE","alwaysBeep":false}]}"""
        val d = SettingsCodec.decode(old)
        assertEquals(listOf(Rule("sms", "Anna", alwaysBeep = true)), d.rules)
        assertEquals(listOf(AppRule("sms", AppMode.EVERYONE)), d.appRules)
        assertEquals(false, SettingsCodec.encode(d).contains("tone"))
    }
}
