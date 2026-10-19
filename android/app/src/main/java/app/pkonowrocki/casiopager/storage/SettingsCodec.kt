package app.pkonowrocki.casiopager.storage

import app.pkonowrocki.casiopager.model.*
import java.time.DayOfWeek
import java.time.LocalTime
import org.json.JSONArray
import org.json.JSONObject

object SettingsCodec {
    fun encode(s: Settings): String {
        val rules = JSONArray()
        s.rules.forEach {
            rules.put(JSONObject().put("app", it.app).put("sender", it.sender).put("alwaysBeep", it.alwaysBeep))
        }
        val days = JSONArray()
        s.quietHours.days.forEach { days.put(it.value) }
        val q = JSONObject().put("enabled", s.quietHours.enabled)
            .put("start", s.quietHours.start.toString()).put("end", s.quietHours.end.toString())
            .put("days", days).put("callsBypass", s.quietHours.callsBypass)
        val appRules = JSONArray()
        s.appRules.forEach {
            appRules.put(JSONObject().put("app", it.app).put("mode", it.mode.name).put("alwaysBeep", it.alwaysBeep))
        }
        val groups = JSONArray()
        s.groupRules.forEach { groups.put(JSONObject().put("app", it.app).put("group", it.group)) }
        return JSONObject().put("rules", rules).put("groupRules", groups).put("quiet", q).put("appRules", appRules)
            .put("throttleMs", s.throttleMs).put("callRepeatMs", s.callRepeatMs)
            .put("alertInGroups", s.alertInGroups).put("watchAddress", s.watchAddress)
            .put("lokiUrl", s.lokiUrl).put("lokiOff", s.lokiUrl.isBlank()).put("lokiEnabled", s.lokiEnabled).put("onboarded", s.onboarded).toString()
    }

    fun decode(text: String?): Settings = runCatching {
        val d = Settings()
        val o = JSONObject(text ?: return d)
        val rules = o.optJSONArray("rules")?.let { a ->
            (0 until a.length()).map { i ->
                val r = a.getJSONObject(i)
                Rule(r.getString("app"), r.getString("sender"), r.optBoolean("alwaysBeep")) // an old "tone" key is ignored
            }
        } ?: d.rules
        val q = o.optJSONObject("quiet")?.let { j ->
            QuietHours(
                enabled = j.optBoolean("enabled"),
                start = LocalTime.parse(j.optString("start", d.quietHours.start.toString())),
                end = LocalTime.parse(j.optString("end", d.quietHours.end.toString())),
                days = j.optJSONArray("days")?.let { a -> (0 until a.length()).map { DayOfWeek.of(a.getInt(it)) }.toSet() }
                    ?: d.quietHours.days,
                callsBypass = j.optBoolean("callsBypass", true),
            )
        } ?: d.quietHours
        val appRules = o.optJSONArray("appRules")?.let { a ->
            (0 until a.length()).map { i ->
                val r = a.getJSONObject(i)
                AppRule(r.getString("app"), AppMode.valueOf(r.optString("mode", "PEOPLE")), r.optBoolean("alwaysBeep"))
            }
        } ?: d.appRules
        val groupRules = o.optJSONArray("groupRules")?.let { a ->
            (0 until a.length()).map { i -> a.getJSONObject(i).let { GroupRule(it.getString("app"), it.getString("group")) } }
        } ?: d.groupRules
        val lokiUrl = if (o.optBoolean("lokiOff")) "" else o.optString("lokiUrl", "").ifBlank { d.lokiUrl }
        Settings(
            rules = rules,
            quietHours = q,
            throttleMs = o.optLong("throttleMs", d.throttleMs),
            callRepeatMs = o.optLong("callRepeatMs", d.callRepeatMs),
            alertInGroups = o.optBoolean("alertInGroups"),
            watchAddress = o.optString("watchAddress", d.watchAddress),
            // saved before the default existed: blank without an explicit off means "use the default"
            lokiUrl = lokiUrl,
            // saved before the switch existed: a server that was set was in use
            lokiEnabled = if (o.has("lokiEnabled")) o.optBoolean("lokiEnabled") else lokiUrl.isNotBlank(),
            appRules = appRules,
            groupRules = groupRules,
            // settings saved before the first-run screen existed belong to someone already set up
            onboarded = o.optBoolean("onboarded", true),
        )
    }.getOrDefault(Settings())

    fun encodeSeen(list: List<SeenSender>): String {
        val a = JSONArray()
        list.forEach { a.put(JSONObject().put("app", it.app).put("sender", it.sender)) }
        return a.toString()
    }

    fun decodeSeen(text: String?): List<SeenSender> = runCatching {
        val a = JSONArray(text ?: return emptyList())
        (0 until a.length()).map { SeenSender(a.getJSONObject(it).getString("app"), a.getJSONObject(it).getString("sender")) }
    }.getOrDefault(emptyList())

    fun encodeSeenGroups(list: List<SeenGroup>): String {
        val a = JSONArray()
        list.forEach { a.put(JSONObject().put("app", it.app).put("group", it.group)) }
        return a.toString()
    }

    fun decodeSeenGroups(text: String?): List<SeenGroup> = runCatching {
        val a = JSONArray(text ?: return emptyList())
        (0 until a.length()).map { a.getJSONObject(it).let { j -> SeenGroup(j.getString("app"), j.getString("group")) } }
    }.getOrDefault(emptyList())
}
