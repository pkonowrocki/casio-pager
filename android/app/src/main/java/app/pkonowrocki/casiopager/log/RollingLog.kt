package app.pkonowrocki.casiopager.log

import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** A text log in [dir] that rotates at [maxBytes] and keeps one previous file, so the newest lines always survive. */
class RollingLog(
    private val dir: File,
    private val maxBytes: Long = 512 * 1024,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    private val current = File(dir, "pager.log")
    private val previous = File(dir, "pager.log.1")
    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @Synchronized fun append(message: String) {
        dir.mkdirs()
        if (current.length() > maxBytes) {
            previous.delete()
            current.renameTo(previous)
        }
        val oneLine = message.replace('\n', ' ').replace('\r', ' ')
        current.appendText(stamp.format(Instant.ofEpochMilli(nowMs()).atZone(zone)) + " " + oneLine + "\n")
    }

    @Synchronized fun tail(maxLines: Int): List<String> = (lines(previous) + lines(current)).takeLast(maxLines)

    @Synchronized fun clear() {
        current.delete()
        previous.delete()
    }

    private fun lines(f: File): List<String> = if (f.exists()) f.readLines() else emptyList()
}
