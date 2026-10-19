package app.pkonowrocki.casiopager.protocol

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

class ProtocolTest {
    @Test fun alertPackets() {
        assertEquals("0101", Packets.alert.toHex())
    }

    @Test fun keepaliveIsReadOf11() = assertEquals("11", Packets.keepalive.toHex())

    @Test fun timeWriteMatchesCapture() {
        // captured from the official app at 2026-10-13 15:55:02, fraction 0x70/256 s (a Tuesday)
        val t = LocalDateTime.of(2026, 10, 13, 15, 55, 2, 437_500_000)
        assertEquals("09ea070a0d0f3702027001", Packets.timeWrite(t).toHex())
    }

    @Test fun watchNamePacketMatchesCapture() =
        assertEquals("23434153494f2041424c2d313030574500000000", Packets.watchName.toHex())

    @Test fun connectionKindIsByteEightOfThe10Reply() {
        assertEquals(1, Replies.connectionKind(hex("10442cbf6591e77f01030fffffffff0c000000")))
        assertEquals(4, Replies.connectionKind(hex("10442cbf6591e77f04030fffffffff0c000000")))
        assertNull(Replies.connectionKind(hex("11")))
        assertNull(Replies.connectionKind(hex("ff8109")))
    }

    @Test fun initSequenceMatchesTheWorkingScript() {
        val ops = INIT_SEQUENCE.map {
            when (it) {
                is InitStep.Ask -> "A%02x%s".format(it.op, if (it.echo) "e" else "")
                InitStep.WriteName -> "N"
                InitStep.WriteTime -> "T"
            }
        }
        assertEquals(
            listOf("A22", "A10", "N", "A11e", "A26", "A28", "A20", "A28", "A20", "A28", "A1de", "A1ee", "A1ee", "T"),
            ops,
        )
        assertTrue(INIT_SEQUENCE.last() === InitStep.WriteTime)
    }
}
