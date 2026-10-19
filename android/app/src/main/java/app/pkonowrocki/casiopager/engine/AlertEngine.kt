package app.pkonowrocki.casiopager.engine

import app.pkonowrocki.casiopager.model.AppMode
import app.pkonowrocki.casiopager.model.AppRule
import app.pkonowrocki.casiopager.model.Decision
import app.pkonowrocki.casiopager.model.IncomingEvent
import app.pkonowrocki.casiopager.model.QuietHours
import app.pkonowrocki.casiopager.model.Rule
import app.pkonowrocki.casiopager.model.Settings
import java.text.Normalizer
import java.time.LocalDateTime
import java.util.Locale

private val INVISIBLE = Regex("""[\u200B-\u200F\u202A-\u202E\u2066-\u2069]""") // zero-width and bidi control characters
private val SPACES = Regex("""\s+""")

fun normalizeName(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFKC)
        .replace(INVISIBLE, "")
        .replace(SPACES, " ")
        .trim()
        .lowercase(Locale.ROOT)

/** The quiet window belongs to the day it starts on (Friday 22:00-07:00 covers Saturday 02:00). */
fun isQuiet(q: QuietHours, now: LocalDateTime): Boolean {
    if (!q.enabled || q.start == q.end) return false
    val t = now.toLocalTime()
    val today = now.dayOfWeek
    return when {
        q.start < q.end -> t >= q.start && t < q.end && today in q.days
        t >= q.start -> today in q.days
        else -> t < q.end && today.minus(1) in q.days
    }
}

private fun isGroupAllowed(s: Settings, e: IncomingEvent): Boolean {
    val title = normalizeName(e.groupTitle ?: return false)
    return title.isNotEmpty() && s.groupRules.any { it.app == e.app && normalizeName(it.group) == title }
}

class AlertEngine {
    private val seen = LinkedHashSet<String>() // ponytail: bounded by hand, no LRU class
    private var lastBeepMs: Long? = null
    private val lastCallBeepMs = HashMap<String, Long>()
    private var undo: (() -> Unit)? = null

    /** Why the last [decide] returned Ignore (null after a Beep). For the log: no names, only the reason. */
    @Volatile var lastIgnoreReason: String? = null
        private set

    /** The last [Decision.Beep] never reached the watch: forget it so the throttle and dedupe do not swallow a retry. */
    @Synchronized fun beepFailed() {
        undo?.invoke()
        undo = null
    }

    private fun ignore(reason: String): Decision {
        lastIgnoreReason = reason
        return Decision.Ignore
    }

    @Synchronized fun decide(e: IncomingEvent, s: Settings, now: LocalDateTime, nowMs: Long): Decision {
        val sender = normalizeName(e.sender)
        if (sender.isEmpty()) return ignore("blank-sender")
        val allowedGroup = e.isGroup && isGroupAllowed(s, e)
        if (e.isGroup && !s.alertInGroups && !allowedGroup) return ignore("group")

        val dedupeKey = if (e.isCall) null else "${e.conversationKey}|${e.timestampMs}"
        if (dedupeKey != null) {
            if (!seen.add(dedupeKey)) return ignore("duplicate")
            if (seen.size > 500) seen.remove(seen.first())
        }

        val appRule = s.appRules.firstOrNull { it.app == e.app } ?: AppRule(e.app)
        if (appRule.mode == AppMode.OFF) return ignore("app-off")
        val personRule = s.rules.firstOrNull {
            it.app == e.app && normalizeName(it.sender).let { n -> n.isNotEmpty() && n == sender }
        }
        val rule = personRule ?: when {
            allowedGroup || appRule.mode == AppMode.EVERYONE -> Rule(e.app, e.sender, appRule.alwaysBeep)
            else -> return ignore("no-rule")
        }

        if (isQuiet(s.quietHours, now) && !(rule.alwaysBeep || (e.isCall && s.quietHours.callsBypass))) {
            return ignore("quiet-hours")
        }

        val prevBeep = lastBeepMs
        val prevCall = lastCallBeepMs[sender]
        if (e.isCall) {
            if (prevCall != null && nowMs - prevCall < s.callRepeatMs) return ignore("call-repeat-gap")
            lastCallBeepMs[sender] = nowMs
        } else {
            if (prevBeep != null && nowMs - prevBeep < s.throttleMs) return ignore("throttle")
        }
        lastBeepMs = nowMs
        undo = {
            lastBeepMs = prevBeep
            if (e.isCall) {
                if (prevCall == null) lastCallBeepMs.remove(sender) else lastCallBeepMs[sender] = prevCall
            } else if (dedupeKey != null) {
                seen.remove(dedupeKey)
            }
        }
        lastIgnoreReason = null
        return Decision.Beep
    }
}
