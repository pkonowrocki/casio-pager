package app.pkonowrocki.casiopager.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatchAddressTest {
    @Test fun acceptsColonSeparatedHexInAnyCaseAndCanonicalisesIt() =
        assertEquals("AA:BB:CC:DD:EE:FF", WatchAddress.parse(" aa:bb:cc:dd:ee:ff "))

    @Test fun rejectsHalfTypedOrMalformedAddresses() {
        listOf(
            "", "AA", "AA:BB:CC:DD:EE", "AA:BB:CC:DD:EE:4", "AA-BB-CC-DD-EE-FF",
            "GA:BB:CC:DD:EE:FF", "AA:BB:CC:DD:EE:FF:00", "AA:BB:CC:DD:EE:FF x",
        ).forEach { assertNull("should reject " + it, WatchAddress.parse(it)) }
    }
}
