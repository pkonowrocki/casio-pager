package app.pkonowrocki.casiopager.log

import java.io.File
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RollingLogTest {
    @get:Rule val tmp = TemporaryFolder()

    private var now = 1_700_000_000_000L // 2023-11-14 22:13:20 UTC
    private fun log(maxBytes: Long = 10_000) = RollingLog(tmp.root, maxBytes, { now }, ZoneOffset.UTC)

    @Test fun appendsTimestampedLinesAndReturnsThemInOrder() {
        val l = log()
        l.append("one")
        now += 5_000
        l.append("two")
        assertEquals(listOf("2023-11-14 22:13:20 one", "2023-11-14 22:13:25 two"), l.tail(10))
    }

    @Test fun tailReturnsOnlyTheLastLines() {
        val l = log()
        (1..5).forEach { l.append("line$it") }
        assertEquals(listOf("2023-11-14 22:13:20 line4", "2023-11-14 22:13:20 line5"), l.tail(2))
    }

    @Test fun rotatesAtTheSizeCapKeepingTheNewestLinesAcrossBothFiles() {
        val l = log(maxBytes = 200)
        (1..40).forEach { l.append("event number $it") }
        assertTrue(File(tmp.root, "pager.log.1").exists())
        val all = l.tail(1_000)
        assertTrue(all.last().endsWith("event number 40"))
        assertFalse(all.any { it.endsWith("event number 1") })           // the oldest is gone
        assertTrue(all.size < 40)
        assertTrue(File(tmp.root, "pager.log").length() + File(tmp.root, "pager.log.1").length() < 200 * 2 + 100)
        // order is preserved across the rotated file and the current one
        val numbers = all.map { it.substringAfterLast(' ').toInt() }
        assertEquals(numbers.sorted(), numbers)
    }

    @Test fun newlinesInAMessageCannotForgeExtraLines() {
        val l = log()
        l.append("a\nfake line\r\nb")
        assertEquals(1, l.tail(10).size)
    }

    @Test fun survivesBeingReopenedAndCanBeCleared() {
        log().append("kept")
        val reopened = log()
        assertEquals(listOf("2023-11-14 22:13:20 kept"), reopened.tail(10))
        reopened.clear()
        assertEquals(emptyList<String>(), reopened.tail(10))
    }
}
