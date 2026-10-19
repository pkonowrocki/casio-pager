package app.pkonowrocki.casiopager.log

import java.net.URI
import java.net.URLDecoder

/** `casiopager://diagnostics?url=<percent-encoded Loki base URL>`: a one-tap way to set the diagnostics server. */
object DiagnosticsLink {
    private const val MAX_URL = 512

    /** The Loki base URL inside a setup link, or null for anything that is not a well-formed one. */
    fun parse(link: String?): String? = runCatching {
        val u = URI(link ?: return null)
        if (u.scheme != "casiopager" || u.host != "diagnostics") return null
        val raw = u.rawQuery?.split('&')?.firstOrNull { it.startsWith("url=") }?.removePrefix("url=") ?: return null
        val url = URLDecoder.decode(raw, "UTF-8").trim()
        url.takeIf { it.length <= MAX_URL && hostOf(it) != null }
    }.getOrNull()

    /** The host of an http(s) URL, or null. Only this is ever shown: the path is a secret. */
    fun hostOf(url: String): String? = runCatching {
        val u = URI(url.trim())
        u.host?.takeIf { it.isNotBlank() && (u.scheme == "http" || u.scheme == "https") }
    }.getOrNull()
}
