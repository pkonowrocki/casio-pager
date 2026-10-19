package app.pkonowrocki.casiopager.ui

import app.pkonowrocki.casiopager.R
import androidx.compose.ui.res.stringResource
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pkonowrocki.casiopager.Pager
import app.pkonowrocki.casiopager.engine.normalizeName
import app.pkonowrocki.casiopager.model.AppMode
import app.pkonowrocki.casiopager.model.AppRule
import app.pkonowrocki.casiopager.model.GroupRule
import app.pkonowrocki.casiopager.model.SeenGroup
import app.pkonowrocki.casiopager.model.Rule
import app.pkonowrocki.casiopager.model.SeenSender
import app.pkonowrocki.casiopager.model.Settings
import kotlinx.coroutines.delay

/** A fixed name for the known apps; for anything else, the name Android shows for the app in its settings. */
fun appTitle(context: Context, app: String): String {
    if (app == "call") return context.getString(R.string.app_call)
    if (app in TOP_APPS) return appLabel(app)
    return runCatching {
        context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(app, 0)).toString()
    }.getOrDefault(MORE_MESSENGERS[app] ?: app)
}

@Composable
fun RulesScreen() {
    val context = LocalContext.current
    val settings by Pager.settings.collectAsStateWithLifecycle()
    val seen by Pager.seen.collectAsStateWithLifecycle()
    val seenGroups by Pager.seenGroups.collectAsStateWithLifecycle()
    var removed by remember { mutableStateOf<Rule?>(null) }
    LaunchedEffect(removed) { if (removed != null) { delay(6_000); removed = null } }
    var open by remember { mutableStateOf(emptySet<String>()) }
    var othersOpen by remember { mutableStateOf(false) }

    val mentioned = settings.rules.map { it.app } + settings.appRules.map { it.app } + seen.map { it.app }
    fun hasSetting(a: String) = settings.appRules.any { it.app == a } || settings.rules.any { it.app == a }
    fun installed(pkg: String) = runCatching { context.packageManager.getApplicationInfo(pkg, 0) }.isSuccess
    val others = otherApps(mentioned, { appTitle(context, it) }, ::hasSetting)
    val messengers = moreMessengers(mentioned, ::installed) { appTitle(context, it) }

    @Composable
    fun card(app: String) = AppCard(
        app = app,
        title = appTitle(context, app),
        settings = settings,
        seen = seen,
        seenGroups = seenGroups,
        expanded = app in open,
        onToggle = { open = if (app in open) open - app else open + app },
        onRemove = { removed = it },
    )

    LazyColumn(
        Modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        removed?.let { r ->
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.rules_removed, appTitle(context, r.app), r.sender))
                        TextButton(onClick = { Pager.update { s -> s.copy(rules = s.rules + r) }; removed = null }) { Text(stringResource(R.string.undo)) }
                    }
                }
            }
        }

        item { Text(stringResource(R.string.rules_title), style = MaterialTheme.typography.titleMedium) }
        item { Hint(stringResource(R.string.rules_hint)) }
        items(TOP_APPS, key = { it }) { app -> card(app) }

        if (messengers.isNotEmpty()) {
            item { Text(stringResource(R.string.rules_more_messengers), style = MaterialTheme.typography.titleMedium) }
            items(messengers, key = { it }) { app -> card(app) }
        }

        if (others.isNotEmpty()) {
            // many apps post notifications and most never matter: keep them folded unless one of them has a setting
            val customised = others.any { a -> settings.appRules.any { it.app == a } || settings.rules.any { it.app == a } }
            val showOthers = othersOpen || customised
            item {
                TextButton(onClick = { othersOpen = !othersOpen }, enabled = !customised) {
                    Text(stringResource(R.string.rules_other_n, others.size) + if (showOthers) "  ▴" else "  ▾", style = MaterialTheme.typography.titleMedium)
                }
            }
            if (showOthers) items(others, key = { it }) { app -> card(app) }
        }

        if (seen.isNotEmpty() || seenGroups.isNotEmpty()) {
            item { TextButton(onClick = { Pager.clearSeen() }) { Text(stringResource(R.string.rules_clear_seen)) } }
        }
    }
}

@Composable
private fun AppCard(
    app: String,
    title: String,
    settings: Settings,
    seen: List<SeenSender>,
    seenGroups: List<SeenGroup>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onRemove: (Rule) -> Unit,
) {
    val appRule = appRuleFor(settings.appRules, app)
    val people = peopleFor(settings.rules, app)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(modeSummaryText(modeSummary(appRule.mode, people.size)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(stringResource(if (expanded) R.string.rule_done else R.string.rule_edit), style = MaterialTheme.typography.labelLarge)
            }
            if (expanded) ModeEditor(app, appRule, people, seen, settings, seenGroups, onRemove)
        }
    }
}

@Composable
private fun ModeEditor(
    app: String, appRule: AppRule, people: List<Rule>, seen: List<SeenSender>,
    settings: Settings, seenGroups: List<SeenGroup>, onRemove: (Rule) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppMode.values().forEach { m ->
                FilterChip(
                    selected = appRule.mode == m,
                    onClick = { Pager.update { s -> s.copy(appRules = withAppRule(s.appRules, app) { it.copy(mode = m) }) } },
                    label = { Text(stringResource(modeChipLabel(m))) },
                )
            }
        }

        when (appRule.mode) {
            AppMode.OFF -> Hint(stringResource(R.string.hint_off))
            AppMode.EVERYONE -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(appRule.alwaysBeep, { v -> setAppRule(app) { it.copy(alwaysBeep = v) } })
                    Text(stringResource(R.string.also_quiet), style = MaterialTheme.typography.bodyMedium)
                }
                if (people.isNotEmpty()) Text(stringResource(R.string.own_setting), style = MaterialTheme.typography.labelLarge)
                people.forEach { r -> PersonRow(r, onRemove) }
                Text(stringResource(R.string.recently_seen), style = MaterialTheme.typography.labelLarge)
                SeenList(app, people, seen)
                GroupSection(app, settings, seenGroups)
            }
            AppMode.PEOPLE -> {
                if (people.isEmpty()) Hint(stringResource(R.string.hint_nobody))
                people.forEach { r -> PersonRow(r, onRemove) }
                Text(stringResource(R.string.recently_seen), style = MaterialTheme.typography.labelLarge)
                SeenList(app, people, seen)
                GroupSection(app, settings, seenGroups)
            }
        }
    }
}

/** Group chats are silent unless all groups are allowed in Settings or this chat is listed here. */
@Composable
private fun GroupSection(app: String, settings: Settings, seenGroups: List<SeenGroup>) {
    val allowed = groupsFor(settings.groupRules, app)
    val suggestions = groupSuggestions(seenGroups, settings.groupRules, app)
    Text(stringResource(R.string.groups_title), style = MaterialTheme.typography.labelLarge)
    Hint(stringResource(if (settings.alertInGroups) R.string.groups_all_on else R.string.groups_hint))
    allowed.forEach { g ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(g.group, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = { Pager.update { s -> s.copy(groupRules = s.groupRules - g) } }) { Text(stringResource(R.string.remove)) }
        }
    }
    if (suggestions.isNotEmpty()) {
        Text(stringResource(R.string.groups_seen), style = MaterialTheme.typography.labelLarge)
        suggestions.forEach { g ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(g.group, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { Pager.update { c -> c.copy(groupRules = c.groupRules + GroupRule(g.app, g.group)) } }) {
                        Text(stringResource(R.string.groups_allow))
                    }
                }
            }
        }
    }
}

@Composable
private fun SeenList(app: String, people: List<Rule>, seen: List<SeenSender>) {
    val suggestions = seen.filter { s -> s.app == app && people.none { normalizeName(it.sender) == normalizeName(s.sender) } }
    if (suggestions.isEmpty()) Hint(stringResource(R.string.hint_nothing_new))
    suggestions.forEach { s -> Suggestion(s) }
}

private fun setAppRule(app: String, change: (AppRule) -> AppRule) =
    Pager.update { s -> s.copy(appRules = withAppRule(s.appRules, app, change)) }

private fun modeChipLabel(m: AppMode) = when (m) {
    AppMode.OFF -> R.string.mode_off
    AppMode.EVERYONE -> R.string.mode_everyone
    AppMode.PEOPLE -> R.string.mode_people
}

@Composable
private fun modeSummaryText(m: ModeSummary): String {
    val res = LocalContext.current.resources
    return if (m.count == null) res.getString(m.res) else res.getQuantityString(m.res, m.count, m.count)
}

@Composable
private fun PersonRow(r: Rule, onRemove: (Rule) -> Unit) {
    fun change(new: Rule) = Pager.update { s -> s.copy(rules = s.rules.map { if (it == r) new else it }) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(r.sender, style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(r.alwaysBeep, { v -> change(r.copy(alwaysBeep = v)) })
                Text(stringResource(R.string.also_quiet), style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = {
                Pager.update { s -> s.copy(rules = s.rules - r) }
                onRemove(r)
            }) { Text(stringResource(R.string.remove)) }
        }
    }
}

@Composable
private fun Suggestion(s: SeenSender) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(s.sender, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { Pager.update { c -> c.copy(rules = c.rules + Rule(s.app, s.sender)) } }) { Text(stringResource(R.string.person_add)) }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
