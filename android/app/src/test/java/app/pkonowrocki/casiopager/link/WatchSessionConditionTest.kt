package app.pkonowrocki.casiopager.link

import app.pkonowrocki.casiopager.protocol.Gatt
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private class ConditionPort(private val reply: ByteArray?, private val writeOk: Boolean = true) : WatchPort {
    val writes = mutableListOf<Pair<String, List<Int>>>()
    override suspend fun write(uuid: String, bytes: ByteArray, noResponse: Boolean): Boolean {
        writes += uuid to bytes.map { it.toInt() and 0xFF }
        return writeOk
    }
    override suspend fun awaitReply(op: Int, timeoutMs: Long): ByteArray? = if (op == 0x28) reply else null
}

class WatchSessionConditionTest {
    private val raw = byteArrayOf(0x28, 0x1d, 0x19, 0, 0, 0, 2, 0, 0)

    @Test fun readsTheWatchConditionAndReturnsTheRawBytes() = runTest {
        val port = ConditionPort(raw)
        val r = WatchSession(port).condition()
        assertEquals(raw.toList(), r!!.toList())
        assertEquals(Gatt.REQUEST to listOf(0x28), port.writes.single())
    }

    @Test fun noReplyOrAFailedWriteGivesNullAndDoesNotThrow() = runTest {
        assertNull(WatchSession(ConditionPort(null)).condition())
        assertNull(WatchSession(ConditionPort(raw, writeOk = false)).condition())
    }

    @Test fun hexOfTheRawReplyIsWhatGetsLogged() = assertEquals("281d19000000020000", raw.toHex())
}
