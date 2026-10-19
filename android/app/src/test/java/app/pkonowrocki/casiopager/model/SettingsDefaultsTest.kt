package app.pkonowrocki.casiopager.model

import app.pkonowrocki.casiopager.storage.SettingsCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsDefaultsTest {
    @Test fun noWatchIsPairedUntilThePersonPairsOne() {
        assertEquals("", Settings().watchAddress)
        assertEquals("", SettingsCodec.decode(null).watchAddress)
        assertNull(WatchAddress.parse(Settings().watchAddress)) // so the service has nothing to connect to
    }

    @Test fun anAlreadySavedAddressIsKept() {
        val s = Settings(watchAddress = "AA:BB:CC:DD:EE:FF")
        assertEquals("AA:BB:CC:DD:EE:FF", SettingsCodec.decode(SettingsCodec.encode(s)).watchAddress)
    }
}

class ThrottleDefaultTest {
    @org.junit.Test fun furtherMessagesStayQuietFor30SecondsAfterABeep() =
        org.junit.Assert.assertEquals(30_000L, Settings().throttleMs)
}
