package app.pkonowrocki.casiopager.storage

import app.pkonowrocki.casiopager.BuildConfig
import app.pkonowrocki.casiopager.model.Settings
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsLokiDefaultTest {
    @Test fun aFreshInstallUsesTheBuildsLokiUrl() {
        assertEquals(BuildConfig.LOKI_URL, Settings().lokiUrl)
        assertEquals(BuildConfig.LOKI_URL, SettingsCodec.decode(null).lokiUrl)
    }

    @Test fun aBuildWithoutTheSecretPushesNowhere() {
        // a fork or a PR from a fork gets no LOKI_URL secret: the default is then blank, which means off
        if (BuildConfig.LOKI_URL.isEmpty()) assertEquals("", Settings().lokiUrl)
    }

    @Test fun settingsSavedBeforeTheDefaultExistedGetIt() {
        // what the app wrote before: a blank url and no explicit "off"
        val old = """{"lokiUrl":"","throttleMs":3000}"""
        assertEquals(Settings.DEFAULT_LOKI_URL, SettingsCodec.decode(old).lokiUrl)
    }

    @Test fun clearingTheUrlTurnsPushingOffAndItStaysOff() {
        val off = SettingsCodec.decode(SettingsCodec.encode(Settings(lokiUrl = "")))
        assertEquals("", off.lokiUrl)
    }

    @Test fun aCustomUrlIsKept() {
        val custom = SettingsCodec.decode(SettingsCodec.encode(Settings(lokiUrl = "http://loki.lan:3100")))
        assertEquals("http://loki.lan:3100", custom.lokiUrl)
    }
}

class SettingsLokiEnabledTest {
    @Test fun settingsSavedBeforeTheSwitchWithAServerCountAsOn() {
        val old = """{"lokiUrl":"http://loki.lan:3100","lokiOff":false}"""
        val s = SettingsCodec.decode(old)
        assertEquals("http://loki.lan:3100", s.lokiUrl)
        assertEquals(true, s.lokiEnabled)
        assertEquals(true, s.lokiActive)
    }

    @Test fun settingsSavedBeforeTheSwitchWithoutAServerAreOff() {
        val s = SettingsCodec.decode("""{"lokiUrl":"","lokiOff":true}""")
        assertEquals(false, s.lokiEnabled)
        assertEquals(false, s.lokiActive)
    }

    @Test fun switchingOffKeepsTheServer() {
        val s = SettingsCodec.decode(SettingsCodec.encode(Settings(lokiUrl = "http://loki.lan:3100", lokiEnabled = false)))
        assertEquals("http://loki.lan:3100", s.lokiUrl)
        assertEquals(false, s.lokiEnabled)
        assertEquals(false, s.lokiActive)
    }

    @Test fun onNeedsAServerToo() {
        assertEquals(false, Settings(lokiUrl = "", lokiEnabled = true).lokiActive)
        assertEquals(true, Settings(lokiUrl = "http://x.y", lokiEnabled = true).lokiActive)
    }
}
