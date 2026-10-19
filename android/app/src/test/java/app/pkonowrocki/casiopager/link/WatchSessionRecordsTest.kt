package app.pkonowrocki.casiopager.link

import app.pkonowrocki.casiopager.protocol.Gatt
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

private class RecordsPort(private val replies: Map<Int, ByteArray>) : WatchPort {
    val writes = mutableListOf<Pair<String, String>>()
    override suspend fun write(uuid: String, bytes: ByteArray, noResponse: Boolean): Boolean {
        writes += uuid to bytes.toHex()
        return true
    }
    override suspend fun awaitReply(op: Int, timeoutMs: Long): ByteArray? = replies[op]
}

class WatchSessionRecordsTest {
    private val timer = hex("1800030000aabbcc")
    private val conn = hex("110f0f0f0600500004000100001e0a")
    private val port = RecordsPort(mapOf(0x18 to timer, 0x11 to conn))

    @Test fun readsTheTimer() = runTest {
        assertEquals(180, WatchSession(port).readTimer()!!.totalSeconds)
        assertEquals(listOf(Gatt.REQUEST to "18"), port.writes)
    }

    @Test fun settingTheTimerReadsWritesTheWholeRecordAndReadsBack() = runTest {
        WatchSession(port).changeTimer { it.withDuration(0, 25, 0) }
        assertEquals(
            listOf(Gatt.REQUEST to "18", Gatt.DATA to "1800190000aabbcc", Gatt.REQUEST to "18"),
            port.writes,
        )
    }

    @Test fun anImpossibleDurationWritesNothingAndReturnsNull() = runTest {
        assertNull(WatchSession(port).changeTimer { it.withDuration(0, 99, 0) })
        assertEquals(listOf(Gatt.REQUEST to "18"), port.writes)   // only the read
    }

    @Test fun readsAndChangesTheConnectionTime() = runTest {
        assertEquals(10, WatchSession(port).readConnection()!!.minutes)
        port.writes.clear()
        WatchSession(port).changeConnection { it.withMinutes(3) }
        assertEquals(
            listOf(Gatt.REQUEST to "11", Gatt.DATA to "110f0f0f0600500004000100001e03", Gatt.REQUEST to "11"),
            port.writes,
        )
    }

    @Test fun noReplyMeansNullAndNoWrite() = runTest {
        val silent = RecordsPort(emptyMap())
        assertNull(WatchSession(silent).changeConnection { it.withMinutes(5) })
        assertEquals(listOf(Gatt.REQUEST to "11"), silent.writes)
    }
}
