package app.pkonowrocki.casiopager.link

import app.pkonowrocki.casiopager.protocol.Gatt
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

private class FakePort(val replies: Map<Int, ByteArray> = emptyMap(), val writeOk: Boolean = true) : WatchPort {
    val writes = mutableListOf<Pair<String, List<Int>>>()
    override suspend fun write(uuid: String, bytes: ByteArray, noResponse: Boolean): Boolean {
        writes += uuid to bytes.map { it.toInt() and 0xFF }
        return writeOk
    }
    override suspend fun awaitReply(op: Int, timeoutMs: Long): ByteArray? = replies[op]
}

private val FIXED = LocalDateTime.of(2026, 10, 13, 15, 55, 2, 437_500_000)

class WatchSessionTest {
    private val replies = mapOf(
        0x10 to hex("10442cbf6591e77f01030fffffffff0c000000"),
        0x11 to hex("110f0f0f0600500004000100001e03"),
    )

    @Test fun initialiseFollowsTheRecipeAndReportsKind() = runTest {
        val port = FakePort(replies)
        val r = WatchSession(port, { FIXED }).initialise()
        assertEquals(1, r.kind)
        assertEquals(listOf(0x22), port.writes.first().second)                   // first request is 22
        assertEquals(Gatt.REQUEST, port.writes.first().first)
        assertTrue(port.writes.any { it.first == Gatt.DATA && it.second.first() == 0x23 })        // name write
        assertTrue(port.writes.any { it.first == Gatt.DATA && it.second == replies[0x11]!!.map { b -> b.toInt() and 0xFF } }) // echo of 11
        val last = port.writes.last()
        assertEquals(Gatt.DATA, last.first)
        assertEquals("09ea070a0d0f3702027001", last.second.joinToString("") { "%02x".format(it) }) // time write is last
    }

    @Test fun missingRepliesDoNotHangOrThrowAndAreCounted() = runTest {
        val port = FakePort(emptyMap())
        val r = WatchSession(port, { FIXED }).initialise()
        assertEquals(null, r.kind)
        assertEquals(12, r.timeouts) // 12 Ask steps, none answered
        assertEquals(0x09, port.writes.last().second.first())  // still sends the time write
    }

    @Test fun alertWritesToDataCharacteristic() = runTest {
        val port = FakePort()
        assertTrue(WatchSession(port).alert())
        assertEquals(Gatt.DATA to listOf(0x01, 0x01), port.writes.single())
    }

    @Test fun failedWritesReturnFalse() = runTest {
        val s = WatchSession(FakePort(writeOk = false))
        assertFalse(s.alert())
        assertFalse(s.keepalive())
    }

    @Test fun keepaliveIsARequestFor11() = runTest {
        val port = FakePort()
        assertTrue(WatchSession(port).keepalive())
        assertEquals(Gatt.REQUEST to listOf(0x11), port.writes.single())
    }
}
