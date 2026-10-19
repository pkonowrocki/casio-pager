package app.pkonowrocki.casiopager.ui

import androidx.annotation.StringRes
import app.pkonowrocki.casiopager.R
import app.pkonowrocki.casiopager.model.AppMode
import app.pkonowrocki.casiopager.engine.normalizeName
import app.pkonowrocki.casiopager.model.AppRule
import app.pkonowrocki.casiopager.model.GroupRule
import app.pkonowrocki.casiopager.model.SeenGroup
import app.pkonowrocki.casiopager.model.Rule

/** Apps with a fixed name and their own section, in the order they are shown. Everything else is listed under "Other apps". */
val TOP_APPS = listOf("messenger", "call", "sms", "whatsapp", "telegram")

fun appLabel(app: String): String = when (app) {
    "whatsapp" -> "WhatsApp"
    "sms" -> "SMS"
    "messenger" -> "Messenger"
    "call" -> "Phone call"
    "telegram" -> "Telegram"
    else -> app.replaceFirstChar { it.uppercase() }
}

/**
 * Other well-known messengers and mail apps, keyed by package name (their rules are stored under it, so nothing saved
 * earlier moves). They get their own "More messengers" section; the name is the fallback when Android has no label.
 */
val MORE_MESSENGERS = mapOf(
    "org.thoughtcrime.securesms" to "Signal",
    "com.viber.voip" to "Viber",
    "com.discord" to "Discord",
    "com.Slack" to "Slack",
    "com.microsoft.teams" to "Microsoft Teams",
    "com.google.android.apps.dynamite" to "Google Chat",
    "com.google.android.gm" to "Gmail",
    "com.microsoft.office.outlook" to "Outlook",
    "com.instagram.android" to "Instagram",
    "com.skype.raider" to "Skype",
    "ch.threema.app" to "Threema",
)

private val COLLATOR: java.text.Collator = java.text.Collator.getInstance().apply { strength = java.text.Collator.PRIMARY }

/** [apps] ordered by their readable name (case and accents ignored), then by key so equal names keep a fixed order. */
fun sortedByLabel(apps: Collection<String>, label: (String) -> String): List<String> =
    apps.distinct().sortedWith(compareBy<String, String>(COLLATOR) { label(it) }.thenBy { it })

/**
 * The messengers shown above "Other apps": those installed ([isInstalled]) or already mentioned (a rule, or seen
 * posting), so an app the phone does not have is not offered. Sorted by name.
 */
fun moreMessengers(mentioned: Collection<String>, isInstalled: (String) -> Boolean, label: (String) -> String): List<String> =
    sortedByLabel(MORE_MESSENGERS.keys.filter { it in mentioned || isInstalled(it) }, label)

/**
 * Apps that have no section of their own. Ones with a setting ([hasSetting]) come first so they are easy to find, then the
 * rest; each group is sorted by the name the user sees, not by package name.
 */
fun otherApps(apps: Collection<String>, label: (String) -> String = { it }, hasSetting: (String) -> Boolean = { false }): List<String> {
    val rest = apps.filter { it !in TOP_APPS && it !in MORE_MESSENGERS }.distinct()
    val (set, plain) = rest.partition(hasSetting)
    return sortedByLabel(set, label) + sortedByLabel(plain, label)
}

/** The saved mode for [app], or the default (only the people listed) when nothing is saved for it yet. */
fun appRuleFor(rules: List<AppRule>, app: String): AppRule = rules.firstOrNull { it.app == app } ?: AppRule(app)

/** Replaces the entry for [app] with [change] applied to its current value (or to the default if it had none). */
fun withAppRule(rules: List<AppRule>, app: String, change: (AppRule) -> AppRule): List<AppRule> =
    rules.filter { it.app != app } + change(appRuleFor(rules, app))

/** People with their own rule under [app], in the order they were added. */
fun peopleFor(rules: List<Rule>, app: String): List<Rule> = rules.filter { it.app == app }

/** A summary line as a string or plural resource id ([count] is set for a plural) so it follows the language. */
data class ModeSummary(val res: Int, val count: Int? = null)

/** Group chats allowed under [app], in the order they were added. */
fun groupsFor(rules: List<GroupRule>, app: String): List<GroupRule> = rules.filter { it.app == app }

/** Seen groups of [app] that are not allowed yet (compared the way the engine compares them). */
fun groupSuggestions(seen: List<SeenGroup>, allowed: List<GroupRule>, app: String): List<SeenGroup> =
    seen.filter { s -> s.app == app && allowed.none { it.app == app && normalizeName(it.group) == normalizeName(s.group) } }

fun modeSummary(mode: AppMode, people: Int): ModeSummary = when (mode) {
    AppMode.OFF -> ModeSummary(R.string.summary_off)
    AppMode.EVERYONE -> if (people == 0) ModeSummary(R.string.summary_everyone) else ModeSummary(R.plurals.summary_everyone_own, people)
    AppMode.PEOPLE -> if (people == 0) ModeSummary(R.string.summary_nobody) else ModeSummary(R.plurals.summary_people, people)
}
