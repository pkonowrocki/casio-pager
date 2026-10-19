package app.pkonowrocki.casiopager.link

import app.pkonowrocki.casiopager.protocol.Gatt
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

private class BasicPort(private val reply: ByteArray?, private val failWriteAt: Int = Int.MAX_VALUE) : WatchPort {
    val writes = mutableListOf<Pair<String, String>>()
    override suspend fun write(uuid: String, bytes: ByteArray, noResponse: Boolean): Boolean {
        writes += uuid to bytes.toHex()
        return writes.size < failWriteAt
    }
    override suspend fun awaitReply(op: Int, timeoutMs: Long): ByteArray? = if (op == 0x13) reply else null
}

class WatchSessionBasicTest {
    private val captured = hex("130500000000000000000000")

    @Test fun readsTheBasicSettings() = runTest {
        val port = BasicPort(captured)
        val s = WatchSession(port).readBasic()!!
        assertTrue(s.powerSaving)
        assertEquals(listOf(Gatt.REQUEST to "13"), port.writes)
    }

    @Test fun anErrorReplyOrNoReplyIsNull() = runTest {
        assertNull(WatchSession(BasicPort(hex("ff8113"))).readBasic())
        assertNull(WatchSession(BasicPort(null)).readBasic())
    }

    @Test fun aChangeReadsThenWritesTheWholeRecordThenReadsBack() = runTest {
        val port = BasicPort(captured)
        val back = WatchSession(port).changeBasic { it.withPowerSaving(false) }
        assertEquals(
            listOf(Gatt.REQUEST to "13", Gatt.DATA to "131500000000000000000000", Gatt.REQUEST to "13"),
            port.writes,
        )
        assertTrue(back != null) // the fake keeps replying with the old record; the real watch replies with the new one
    }

    @Test fun nothingIsWrittenWhenTheCurrentRecordCannotBeRead() = runTest {
        val port = BasicPort(null)
        assertNull(WatchSession(port).changeBasic { it.withPowerSaving(false) })
        assertEquals(listOf(Gatt.REQUEST to "13"), port.writes)   // only the read attempt
    }

    @Test fun aFailedWriteIsReportedAsNull() = runTest {
        val port = BasicPort(captured, failWriteAt = 2) // the second write (the record itself) fails
        assertNull(WatchSession(port).changeBasic { it.withLongLight(true) })
        assertFalse(port.writes.any { it.first == Gatt.REQUEST && port.writes.indexOf(it) > 1 })
    }
}
