package app.pkonowrocki.casiopager.protocol

import java.time.DateTimeException
import java.time.LocalDateTime

data class DaySummary(val steps: Int, val distanceMeters: Int)

/**
 * The watch's Life Log (step counter) blob: 400 bytes on characteristic 24 after `00 11 00 00 00` is written to 23.
 * Layout from the open-source library (it reads the same ABL-100WE blob): a 6-byte BCD timestamp, then 10-byte records of
 * five 16-bit buckets (one record per finished hour, newest first; 0xFFFE means no data), seven daily summaries at 318
 * (steps and metres, 32-bit each), today's steps at 374, today's distance at 378 and the hour in progress at 382.
 * The numbers are cross-checked against the watch's own display, see docs/PROTOCOL.md.
 */
data class LifeLog(
    val stamp: LocalDateTime,
    val stepsToday: Int?,
    val distanceMeters: Int?,
    /** index = hour of day; the hour in progress holds its steps so far */
    val hourlySteps: List<Int?>,
    /** the seven stored daily summaries, newest first; null when the slot is empty */
    val lastDays: List<DaySummary?>,
)

object LifeLogParser {
    private const val HEADER = 6
    private const val RECORD = 10
    private const val SCAN_LIMIT = 146
    private const val DAILY = 318
    private const val STEPS_TODAY = 374
    private const val DISTANCE_TODAY = 378
    private const val PENDING = 382
    private const val MIN_SIZE = PENDING + 6
    private const val NO_DATA_BUCKET = 0xFFFE

    fun parse(blob: ByteArray): LifeLog? {
        if (blob.size < MIN_SIZE) return null
        val stamp = timestamp(blob) ?: return null

        val stepsToday = u32(blob, STEPS_TODAY).takeIf { it >= 0 }
        val distance = u32(blob, DISTANCE_TODAY).takeIf { it >= 0 }
        val pending = (0 until 3).sumOf { bucket(blob, PENDING + it * 2) }

        var recordEnd = HEADER
        if (stepsToday != null) {
            var best = Int.MAX_VALUE
            for (end in HEADER..SCAN_LIMIT step RECORD) {
                var front = 0
                for (offset in HEADER until end step RECORD) front += (0 until 5).sumOf { bucket(blob, offset + it * 2) }
                val diff = kotlin.math.abs(stepsToday - pending - front)
                if (diff < best) { best = diff; recordEnd = end }
            }
        }

        val hourly = MutableList<Int?>(24) { null }
        var index = 0
        for (offset in HEADER until recordEnd step RECORD) {
            val steps = (0 until 5).sumOf { bucket(blob, offset + it * 2) }
            if (steps > 0) hourly[((stamp.hour - index - 1) % 24 + 24) % 24] = steps
            index++
        }
        if (pending > 0) hourly[stamp.hour] = pending

        val days = (0 until 7).map { i ->
            val steps = u32(blob, DAILY + i * 8)
            val metres = u32(blob, DAILY + i * 8 + 4)
            if (steps < 0) null else DaySummary(steps, metres.coerceAtLeast(0))
        }
        return LifeLog(stamp, stepsToday, distance, hourly, days)
    }

    private fun timestamp(b: ByteArray): LocalDateTime? = try {
        val f = (0 until 6).map { bcd(b[it].toInt() and 0xFF) ?: return null }
        LocalDateTime.of(2000 + f[0], f[1], f[2], f[3], f[4], f[5])
    } catch (e: DateTimeException) {
        null
    }

    private fun bcd(v: Int): Int? = if (v shr 4 > 9 || v and 0xF > 9) null else (v shr 4) * 10 + (v and 0xF)

    private fun u16(b: ByteArray, o: Int) = (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8)

    private fun u32(b: ByteArray, o: Int) = u16(b, o) or (u16(b, o + 2) shl 16) // negative = the watch's "no data"

    /** One 16-bit step bucket; the "no data" markers count as zero. */
    private fun bucket(b: ByteArray, o: Int): Int = u16(b, o).let { if (it == NO_DATA_BUCKET || it == 0xFFFF) 0 else it }
}
