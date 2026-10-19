package app.pkonowrocki.casiopager.engine

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RingRepeaterTest {
    @Test fun repeatsEveryIntervalWhileRingingUpToTheCap() = runTest {
        var beeps = 0
        RingRepeater(intervalMs = 5_000, maxRepeats = 3).run(ringing = { true }, beep = { beeps++; true })
        assertEquals(3, beeps)
        assertEquals(15_000, currentTime)
    }

    @Test fun stopsAsSoonAsTheCallIsNoLongerRinging() = runTest {
        var beeps = 0
        var ringing = true
        val job = launch { RingRepeater(5_000, 6).run(ringing = { ringing }, beep = { beeps++; true }) }
        advanceTimeBy(5_001)          // first repeat
        assertEquals(1, beeps)
        ringing = false               // answered or declined
        advanceTimeBy(5_000)
        job.join()
        assertEquals(1, beeps)        // no beep after it stopped ringing
    }

    @Test fun doesNotBeepAtAllWhenAlreadyStopped() = runTest {
        var beeps = 0
        RingRepeater(5_000, 6).run(ringing = { false }, beep = { beeps++; true })
        assertEquals(0, beeps)
    }

    @Test fun stopsWhenTheWatchLinkIsGone() = runTest {
        var attempts = 0
        RingRepeater(5_000, 6).run(ringing = { true }, beep = { attempts++; attempts < 2 }) // 2nd attempt fails
        assertEquals(2, attempts)
    }
}
