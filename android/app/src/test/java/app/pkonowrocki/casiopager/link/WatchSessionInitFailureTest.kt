package app.pkonowrocki.casiopager.link

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FailingPort(private val failFromWrite: Int) : WatchPort {
    var writes = 0
    override suspend fun write(uuid: String, bytes: ByteArray, noResponse: Boolean): Boolean = ++writes < failFromWrite
    override suspend fun awaitReply(op: Int, timeoutMs: Long): ByteArray? = null
}

class WatchSessionInitFailureTest {
    @Test fun initStopsAtTheFirstFailedWriteInsteadOfWaitingOutEveryTimeout() = runTest {
        val port = FailingPort(failFromWrite = 1) // link already closed: every write fails
        val r = WatchSession(port).initialise()
        assertFalse(r.ok)
        assertEquals(1, port.writes)
        assertEquals(0, r.timeouts)
    }

    @Test fun initThatLosesTheLinkMidwayIsReportedAsFailed() = runTest {
        val port = FailingPort(failFromWrite = 5)
        assertFalse(WatchSession(port).initialise().ok)
        assertEquals(5, port.writes)
    }

    @Test fun aCompleteInitIsOk() = runTest {
        assertTrue(WatchSession(FailingPort(failFromWrite = Int.MAX_VALUE)).initialise().ok)
    }
}
