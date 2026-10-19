package app.pkonowrocki.casiopager.model

import app.pkonowrocki.casiopager.BuildConfig
import java.time.DayOfWeek
import java.time.LocalTime

data class Rule(
    val app: String,
    val sender: String,
    val alwaysBeep: Boolean = false,
)

/**
 * What an app as a whole does. [PEOPLE] is the default for an app with nothing saved: only the people listed under it
 * beep. [EVERYONE] beeps for any sender, with the quiet-hours switch set here; a person rule still overrides
 * them. [OFF] silences the app, and the people listed under it are kept for when it is turned back on.
 */
enum class AppMode { OFF, EVERYONE, PEOPLE }

data class AppRule(
    val app: String,
    val mode: AppMode = AppMode.PEOPLE,
    val alwaysBeep: Boolean = false,
)

/** A group chat that beeps for any member. Identified by the chat's title, normalized like names. */
data class GroupRule(val app: String, val group: String)

data class QuietHours(
    val enabled: Boolean = false,
    val start: LocalTime = LocalTime.of(22, 0),
    val end: LocalTime = LocalTime.of(7, 0),
    val days: Set<DayOfWeek> = DayOfWeek.values().toSet(),
    val callsBypass: Boolean = true,
)

data class Settings(
    val rules: List<Rule> = emptyList(),
    val quietHours: QuietHours = QuietHours(),
    val throttleMs: Long = 30_000,
    val callRepeatMs: Long = 5_000,
    val alertInGroups: Boolean = false,
    val watchAddress: String = "", // empty until a watch is paired; the app only ever talks to this one address
    val lokiUrl: String = DEFAULT_LOKI_URL, // empty = no server
    val lokiEnabled: Boolean = DEFAULT_LOKI_URL.isNotBlank(), // pushing needs this and a server; off keeps the saved URL
    val appRules: List<AppRule> = emptyList(), // an app with no entry here behaves as AppMode.PEOPLE
    val groupRules: List<GroupRule> = emptyList(), // group chats allowed to beep even with alertInGroups off
    val onboarded: Boolean = false, // the first-run setup screen was finished or skipped
) {
    /** Diagnostics are pushed only when switched on and a server is set. */
    val lokiActive: Boolean get() = lokiEnabled && lokiUrl.isNotBlank()

    companion object {
        /** The build's Loki push URL (the LOKI_URL secret, see build.gradle.kts). Empty when the build has none. */
        val DEFAULT_LOKI_URL: String = BuildConfig.LOKI_URL
    }
}

data class IncomingEvent(
    val app: String,
    val sender: String,
    val conversationKey: String,
    val timestampMs: Long,
    val isGroup: Boolean,
    val isCall: Boolean,
    val groupTitle: String? = null, // the chat's title, for a group conversation
)

sealed interface Decision {
    data object Ignore : Decision
    data object Beep : Decision
}

data class SeenSender(val app: String, val sender: String)

data class SeenGroup(val app: String, val group: String)

/** One line of the "why didn't it beep?" list: no names and no text, only the app and what happened. */
data class DecisionEntry(val atMs: Long, val app: String, val outcome: String)
