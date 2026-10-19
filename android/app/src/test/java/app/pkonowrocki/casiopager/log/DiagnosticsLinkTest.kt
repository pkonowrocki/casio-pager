package app.pkonowrocki.casiopager.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiagnosticsLinkTest {
    @Test fun anEncodedHttpsUrlIsDecoded() =
        assertEquals(
            "https://loki.example.org/push/abc_DEF-123",
            DiagnosticsLink.parse("casiopager://diagnostics?url=https%3A%2F%2Floki.example.org%2Fpush%2Fabc_DEF-123"),
        )

    @Test fun httpIsAccepted() =
        assertEquals("http://loki.lan:3100", DiagnosticsLink.parse("casiopager://diagnostics?url=http%3A%2F%2Floki.lan%3A3100"))

    @Test fun otherSchemesHostsAndMissingUrlsAreRejected() {
        assertNull(DiagnosticsLink.parse(null))
        assertNull(DiagnosticsLink.parse(""))
        assertNull(DiagnosticsLink.parse("casiopager://diagnostics"))
        assertNull(DiagnosticsLink.parse("casiopager://diagnostics?url="))
        assertNull(DiagnosticsLink.parse("casiopager://other?url=https%3A%2F%2Fa.b"))
        assertNull(DiagnosticsLink.parse("https://diagnostics?url=https%3A%2F%2Fa.b"))
        assertNull(DiagnosticsLink.parse("casiopager://diagnostics?url=ftp%3A%2F%2Fa.b"))
        assertNull(DiagnosticsLink.parse("casiopager://diagnostics?url=javascript%3Aalert(1)"))
        assertNull(DiagnosticsLink.parse("casiopager://diagnostics?url=notaurl"))
        assertNull(DiagnosticsLink.parse("casiopager://diagnostics?url=https%3A%2F%2F"))
    }

    @Test fun anOversizedUrlIsRejected() {
        val long = "https://a.b/" + "x".repeat(600)
        assertNull(DiagnosticsLink.parse("casiopager://diagnostics?url=" + java.net.URLEncoder.encode(long, "UTF-8")))
    }

    @Test fun onlyTheHostIsExposed() {
        assertEquals("loki.example.org", DiagnosticsLink.hostOf("https://loki.example.org/push/secret"))
        assertNull(DiagnosticsLink.hostOf("not a url"))
        assertNull(DiagnosticsLink.hostOf(""))
    }
}
