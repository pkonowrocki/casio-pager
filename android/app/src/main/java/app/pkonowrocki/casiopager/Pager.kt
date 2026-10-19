package app.pkonowrocki.casiopager

import android.content.Context
import app.pkonowrocki.casiopager.engine.AlertEngine
import app.pkonowrocki.casiopager.engine.pushDecision
import app.pkonowrocki.casiopager.link.AlertSink
import app.pkonowrocki.casiopager.link.LinkState
import app.pkonowrocki.casiopager.link.WatchControl
import app.pkonowrocki.casiopager.log.PagerLog
import app.pkonowrocki.casiopager.model.IncomingEvent
import app.pkonowrocki.casiopager.model.DecisionEntry
import app.pkonowrocki.casiopager.model.SeenGroup
import app.pkonowrocki.casiopager.model.SeenSender
import app.pkonowrocki.casiopager.model.Settings
import app.pkonowrocki.casiopager.protocol.LifeLog
import app.pkonowrocki.casiopager.storage.DayEntry
import app.pkonowrocki.casiopager.storage.SettingsStore
import app.pkonowrocki.casiopager.storage.StepsHistory
import kotlinx.coroutines.flow.MutableStateFlow

/** The last alert that reached the watch. Only the app, never a name, so it is safe to show and to log. */
data class LastAlert(val atMs: Long, val app: String)

object Pager {
    val lastAlert = MutableStateFlow<LastAlert?>(null)
    val linkSinceMs = MutableStateFlow<Long?>(null)
    /** When the app last wrote the time to the watch (any kind of session). Kept across restarts. */
    val lastClockSyncMs = MutableStateFlow<Long?>(null)
    val stepsHistory = MutableStateFlow<List<DayEntry>>(emptyList())
    val stepsUpdatedMs = MutableStateFlow<Long?>(null)
    val engine = AlertEngine()
    val settings = MutableStateFlow(Settings())
    val seen = MutableStateFlow<List<SeenSender>>(emptyList())
    val seenGroups = MutableStateFlow<List<SeenGroup>>(emptyList())
    /** The last decisions, newest first, for the "why didn't it beep?" screen. In memory only. */
    val decisions = MutableStateFlow<List<DecisionEntry>>(emptyList())
    val linkState = MutableStateFlow<LinkState>(LinkState.Idle)
    @Volatile var link: AlertSink? = null
    @Volatile var control: WatchControl? = null
    private var store: SettingsStore? = null

    @Synchronized fun init(ctx: Context) {
        PagerLog.attach(ctx.applicationContext.filesDir)
        if (store != null) return
        store = SettingsStore(ctx.applicationContext).also {
            settings.value = it.load()
            seen.value = it.loadSeen()
            seenGroups.value = it.loadSeenGroups()
            lastClockSyncMs.value = it.loadClockSync()
            stepsHistory.value = it.loadSteps()
            stepsUpdatedMs.value = it.loadStepsUpdated()
        }
    }

    @Synchronized fun update(change: (Settings) -> Settings) {
        settings.value = change(settings.value)
        store?.save(settings.value)
    }

    @Synchronized fun recordClockSync(ms: Long) {
        lastClockSyncMs.value = ms
        store?.saveClockSync(ms)
    }

    /** A Life Log read from the watch: fold it into the stored history. */
    @Synchronized fun recordLifeLog(log: LifeLog, ms: Long) {
        stepsHistory.value = StepsHistory.merge(stepsHistory.value, log)
        stepsUpdatedMs.value = ms
        store?.saveSteps(stepsHistory.value, ms)
    }

    @Synchronized fun clearSeen() {
        seen.value = emptyList()
        store?.saveSeen(seen.value)
        seenGroups.value = emptyList()
        store?.saveSeenGroups(seenGroups.value)
    }

    @Synchronized fun recordDecision(app: String, outcome: String) {
        decisions.value = pushDecision(decisions.value, DecisionEntry(System.currentTimeMillis(), app, outcome))
    }

    @Synchronized fun recordSeen(e: IncomingEvent) {
        val s = SeenSender(e.app, e.sender)
        if (s !in seen.value) {
            seen.value = (listOf(s) + seen.value).take(50)
            store?.saveSeen(seen.value)
        }
        val title = e.groupTitle ?: return
        val g = SeenGroup(e.app, title)
        if (g in seenGroups.value) return
        seenGroups.value = (listOf(g) + seenGroups.value).take(50)
        store?.saveSeenGroups(seenGroups.value)
    }
}
