package app.pkonowrocki.casiopager.storage

import app.pkonowrocki.casiopager.protocol.LifeLog
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

/** One day of the step counter, as last read from the watch. */
data class DayEntry(val date: LocalDate, val steps: Int, val distanceMeters: Int)

/** The persisted daily history: newest first, one entry per date, latest read wins, capped. Pure, so it is unit-tested. */
object StepsHistory {
    const val CAP = 400

    /** Folds a Life Log read into [history]: today from the live counters, the seven stored summaries as earlier days. */
    fun merge(history: List<DayEntry>, log: LifeLog): List<DayEntry> {
        val day = log.stamp.toLocalDate()
        val incoming = buildList {
            log.stepsToday?.let { add(DayEntry(day, it, log.distanceMeters ?: 0)) }
            log.lastDays.forEachIndexed { i, d -> d?.let { add(DayEntry(day.minusDays(i + 1L), it.steps, it.distanceMeters)) } }
        }
        val byDate = LinkedHashMap<LocalDate, DayEntry>()
        history.forEach { byDate[it.date] = it }
        incoming.forEach { byDate[it.date] = it }
        return byDate.values.sortedByDescending { it.date }.take(CAP)
    }

    fun encode(l: List<DayEntry>): String =
        JSONArray(l.map { JSONObject().put("d", it.date.toString()).put("s", it.steps).put("m", it.distanceMeters) }).toString()

    fun decode(s: String?): List<DayEntry> = runCatching {
        val a = JSONArray(s ?: return emptyList())
        (0 until a.length()).map { a.getJSONObject(it).let { o -> DayEntry(LocalDate.parse(o.getString("d")), o.getInt("s"), o.getInt("m")) } }
    }.getOrDefault(emptyList())
}
