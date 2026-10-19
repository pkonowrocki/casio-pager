package app.pkonowrocki.casiopager.storage

import android.content.Context
import app.pkonowrocki.casiopager.model.SeenGroup
import app.pkonowrocki.casiopager.model.SeenSender
import app.pkonowrocki.casiopager.model.Settings

class SettingsStore(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("pager", Context.MODE_PRIVATE)
    fun load(): Settings = SettingsCodec.decode(prefs.getString("settings", null))
    fun save(s: Settings) = prefs.edit().putString("settings", SettingsCodec.encode(s)).apply()
    fun loadSeen(): List<SeenSender> = SettingsCodec.decodeSeen(prefs.getString("seen", null))
    fun loadClockSync(): Long? = prefs.getLong("clockSyncMs", 0L).takeIf { it > 0 }
    fun saveClockSync(ms: Long) = prefs.edit().putLong("clockSyncMs", ms).apply()
    fun loadSteps(): List<DayEntry> = StepsHistory.decode(prefs.getString("steps", null))
    fun loadStepsUpdated(): Long? = prefs.getLong("stepsMs", 0L).takeIf { it > 0 }
    fun saveSteps(l: List<DayEntry>, ms: Long) = prefs.edit().putString("steps", StepsHistory.encode(l)).putLong("stepsMs", ms).apply()
    fun loadSeenGroups(): List<SeenGroup> = SettingsCodec.decodeSeenGroups(prefs.getString("seenGroups", null))
    fun saveSeenGroups(l: List<SeenGroup>) = prefs.edit().putString("seenGroups", SettingsCodec.encodeSeenGroups(l)).apply()
    fun saveSeen(l: List<SeenSender>) = prefs.edit().putString("seen", SettingsCodec.encodeSeen(l)).apply()
}
