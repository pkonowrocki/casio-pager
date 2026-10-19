package app.pkonowrocki.casiopager.tile

import app.pkonowrocki.casiopager.R
import app.pkonowrocki.casiopager.link.LinkState
import app.pkonowrocki.casiopager.ui.CheckInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TileModelTest {
    private val good = CheckInput(
        link = LinkState.Held(1), paired = true, bluetoothPermission = true, notificationPermission = true,
        listenerEnabled = true, batteryIgnored = true, bluetoothOn = true, serviceRunning = true,
    )
    private val min = 60_000L

    @Test fun durations() {
        assertEquals("<1 min", TileModel.held(0))
        assertEquals("<1 min", TileModel.held(59_999))
        assertEquals("1 min", TileModel.held(min))
        assertEquals("20 min", TileModel.held(20 * min))
        assertEquals("1 h 20 min", TileModel.held(80 * min))
        assertEquals("2 h", TileModel.held(120 * min))
        assertEquals("25 h 5 min", TileModel.held(25 * 60 * min + 5 * min))
        assertEquals("<1 min", TileModel.held(-5_000)) // clock went backwards
    }

    @Test fun aHeldLinkIsActiveAndShowsHowLong() {
        val v = TileModel.of(good, sinceMs = 0, nowMs = 80 * min)
        assertTrue(v.active)
        assertEquals(R.string.tile_linked, v.subtitle)
        assertEquals("1 h 20 min", v.arg)
    }

    @Test fun noLinkAsksForTheLongPress() {
        val v = TileModel.of(good.copy(link = LinkState.Lost), null, 0)
        assertFalse(v.active)
        assertEquals(R.string.tile_waiting, v.subtitle)
    }

    @Test fun aShortSessionIsNotActive() {
        assertFalse(TileModel.of(good.copy(link = LinkState.Held(3)), 0, min).active)
    }

    @Test fun blockingProblemsSayWhatIsMissing() {
        assertEquals(R.string.tile_unpaired, TileModel.of(good.copy(paired = false), null, 0).subtitle)
        assertEquals(R.string.tile_setup, TileModel.of(good.copy(listenerEnabled = false), null, 0).subtitle)
        assertEquals(R.string.tile_setup, TileModel.of(good.copy(bluetoothOn = false), null, 0).subtitle)
        assertEquals(R.string.tile_setup, TileModel.of(good.copy(notificationPermission = false), null, 0).subtitle)
        assertEquals(R.string.tile_start, TileModel.of(good.copy(serviceRunning = false, link = LinkState.Idle), null, 0).subtitle)
    }

    @Test fun theBatteryWarningDoesNotStopTheTileFromShowingTheLink() {
        assertTrue(TileModel.of(good.copy(batteryIgnored = false), 0, min).active)
    }
}
