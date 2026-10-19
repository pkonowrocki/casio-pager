package app.pkonowrocki.casiopager.log

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class LokiPayloadTest {
    @Test fun buildsPushBody() {
        val body = LokiPayload.build(
            listOf(1_700_000_000_123L to "link held kind=1", 1_700_000_001_000L to "say \"hi\"\nnext"),
            mapOf("app" to "casiopager", "device" to "pixel7"),
        )
        val stream = JSONObject(body).getJSONArray("streams").getJSONObject(0)
        assertEquals("casiopager", stream.getJSONObject("stream").getString("app"))
        val v = stream.getJSONArray("values")
        assertEquals("1700000000123000000", v.getJSONArray(0).getString(0)) // ms -> ns
        assertEquals("link held kind=1", v.getJSONArray(0).getString(1))
        assertEquals("say \"hi\"\nnext", v.getJSONArray(1).getString(1))    // escaping survives
    }

    @Test fun emptyInputGivesEmptyValues() {
        val v = JSONObject(LokiPayload.build(emptyList(), mapOf("app" to "x"))).getJSONArray("streams").getJSONObject(0).getJSONArray("values")
        assertEquals(0, v.length())
    }
}
