package app.pkonowrocki.casiopager.model

object WatchAddress {
    private val FORMAT = Regex("""^([0-9A-F]{2}:){5}[0-9A-F]{2}$""")

    /** The canonical upper-case address, or null when [text] is not a complete Bluetooth address (e.g. half typed). */
    fun parse(text: String): String? = text.trim().uppercase().takeIf { FORMAT.matches(it) }
}
