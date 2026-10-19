package app.pkonowrocki.casiopager.log

import app.pkonowrocki.casiopager.Pager
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object LokiPusher {
    private val labels = mapOf("app" to "casiopager", "device" to "pixel7")

    fun start(scope: CoroutineScope): Job = scope.launch {
        while (isActive) {
            delay(15_000)
            val st = Pager.settings.value
            if (!st.lokiActive) continue
            val base = st.lokiUrl.trim().trimEnd('/')
            val lines = PagerLog.drain()
            if (lines.isEmpty()) continue
            val ok = withContext(Dispatchers.IO) { post("$base/loki/api/v1/push", LokiPayload.build(lines, labels)) }
            if (!ok) PagerLog.putBack(lines)
        }
    }

    private fun post(url: String, body: String): Boolean = runCatching {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.connectTimeout = 5_000
        c.readTimeout = 5_000
        c.setRequestProperty("Content-Type", "application/json")
        c.doOutput = true
        c.outputStream.use { it.write(body.toByteArray()) }
        (c.responseCode in 200..299).also { c.disconnect() }
    }.getOrDefault(false)
}
