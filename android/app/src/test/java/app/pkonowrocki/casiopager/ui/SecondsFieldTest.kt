package app.pkonowrocki.casiopager.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SecondsFieldTest {
    @Test fun wholeSecondsShowWithoutDecimals() {
        assertEquals("30", msToSecondsText(30_000))
        assertEquals("0", msToSecondsText(0))
    }

    @Test fun fractionsAreKept() = assertEquals("2.5", msToSecondsText(2_500))

    @Test fun typedSecondsBecomeMilliseconds() {
        assertEquals(30_000L, secondsTextToMs("30"))
        assertEquals(2_500L, secondsTextToMs("2,5"))
        assertEquals(2_500L, secondsTextToMs(" 2.5 "))
    }

    @Test fun junkAndNegativesAreRejected() {
        assertNull(secondsTextToMs(""))
        assertNull(secondsTextToMs("abc"))
        assertNull(secondsTextToMs("-1"))
    }
}
