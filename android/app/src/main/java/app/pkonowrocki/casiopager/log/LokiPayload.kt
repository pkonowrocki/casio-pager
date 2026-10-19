package app.pkonowrocki.casiopager.log

import org.json.JSONArray
import org.json.JSONObject

object LokiPayload {
    fun build(lines: List<Pair<Long, String>>, labels: Map<String, String>): String {
        val values = JSONArray()
        lines.forEach { (ms, line) -> values.put(JSONArray().put((ms * 1_000_000L).toString()).put(line)) }
        val stream = JSONObject().put("stream", JSONObject(labels)).put("values", values)
        return JSONObject().put("streams", JSONArray().put(stream)).toString()
    }
}
