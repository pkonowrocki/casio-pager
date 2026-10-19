package app.pkonowrocki.casiopager.link

import app.pkonowrocki.casiopager.protocol.Gatt
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private class BulkPort(private val blob: ByteArray?, private val writeOk: Boolean = true) : WatchPort {
    val events = mutableListOf<String>()
    override suspend fun write(uuid: String, bytes: ByteArray, noResponse: Boolean): Boolean {
        events += "write ${if (uuid == Gatt.CMD) "cmd" else uuid} ${bytes.toHex()} noResponse=$noResponse"
        return writeOk
    }
    override suspend fun awaitReply(op: Int, timeoutMs: Long): ByteArray? = null
    override fun discardData(uuid: String) { events += "discard ${if (uuid == Gatt.BULK) "bulk" else uuid}" }
    override suspend fun awaitData(uuid: String, minBytes: Int, timeoutMs: Long): ByteArray? {
        events += "await ${if (uuid == Gatt.BULK) "bulk" else uuid} $minBytes"
        return blob
    }
}

private fun blob(steps: Int): ByteArray {
    val b = ByteArray(400)
    for (i in 6 until 146 step 2) { b[i] = 0xFE.toByte(); b[i + 1] = 0xFF.toByte() }
    for (i in 318 until 374) b[i] = 0xFF.toByte()
    byteArrayOf(0x26, 0x10, 0x14, 0x10, 0x20, 0x30).copyInto(b)   // 2026-10-14 10:20:30
    b[374] = steps.toByte(); b[375] = (steps shr 8).toByte()
    b[378] = 0; b[379] = 0
    for (i in 382 until 388) b[i] = 0
    return b
}

/** Answers each await with the next item (null = the watch said nothing). */
private class SequencePort(private val blobs: List<ByteArray?>) : WatchPort {
    val events = mutableListOf<String>()
    private var next = 0
    override suspend fun write(uuid: String, bytes: ByteArray, noResponse: Boolean): Boolean {
        events += "write ${bytes.toHex()}"
        return true
    }
    override suspend fun awaitReply(op: Int, timeoutMs: Long): ByteArray? = null
    override fun discardData(uuid: String) { events += "discard" }
    override suspend fun awaitData(uuid: String, minBytes: Int, timeoutMs: Long): ByteArray? {
        events += "await"
        return blobs.getOrNull(next++)
    }
}

class WatchSessionLifeLogTest {
    @Test fun asksReadsTheBulkDataAndThenClosesTheTransferLikeTheCasioApp() = runTest {
        val port = BulkPort(blob(0))
        val log = WatchSession(port).readLifeLog()!!
        assertEquals(0, log.stepsToday)
        assertEquals(
            listOf(
                "discard bulk",
                "write cmd 0011000000 noResponse=false",
                "await bulk 400",
                "write cmd 0411000000 noResponse=false",   // the closing command, as the app sends it
            ),
            port.events,
        )
    }

    @Test fun peekLeavesTheTransferOpenAndSendsNoClosingCommand() = runTest {
        val port = BulkPort(blob(0))
        WatchSession(port).readLifeLog(finish = false)!!
        assertEquals(listOf("discard bulk", "write cmd 0011000000 noResponse=false", "await bulk 400"), port.events)
    }

    @Test fun aTransferLeftOpenEarlierIsClosedAndTheReadRetriedOnce() = runTest {
        val port = SequencePort(listOf(null, blob(7)))   // first try: silence; second try: the blob
        val log = WatchSession(port).readLifeLog()!!
        assertEquals(7, log.stepsToday)
        assertEquals(
            listOf("discard", "write 0011000000", "await",      // no answer
                   "write 0411000000",                          // close whatever was open
                   "discard", "write 0011000000", "await",      // try again
                   "write 0411000000"),                         // and close this one
            port.events,
        )
    }

    @Test fun twoSilentTriesGiveUpWithNull() = runTest {
        assertNull(WatchSession(SequencePort(listOf(null, null))).readLifeLog())
    }

    @Test fun nothingReceivedAFailedWriteOrANonsenseBlobGiveNull() = runTest {
        assertNull(WatchSession(BulkPort(null)).readLifeLog())
        assertNull(WatchSession(BulkPort(blob(0), writeOk = false)).readLifeLog())
        assertNull(WatchSession(BulkPort(ByteArray(400))).readLifeLog()) // all zero: month 0 is not a date
    }
}
