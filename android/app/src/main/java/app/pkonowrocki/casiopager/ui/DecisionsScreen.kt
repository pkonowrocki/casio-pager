package app.pkonowrocki.casiopager.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pkonowrocki.casiopager.Pager
import app.pkonowrocki.casiopager.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

/** What the engine said, as shown to the user. [outcome] is the engine's ignore reason, or beep / no-link for a decided beep. */
@StringRes
fun decisionLabel(outcome: String): Int = when (outcome) {
    "beep" -> R.string.decision_beep
    "no-link" -> R.string.decision_no_link
    "no-rule" -> R.string.decision_no_rule
    "group" -> R.string.decision_group
    "app-off" -> R.string.decision_app_off
    "quiet-hours" -> R.string.decision_quiet
    "duplicate" -> R.string.decision_duplicate
    "throttle" -> R.string.decision_throttle
    "call-repeat-gap" -> R.string.decision_call_repeat
    else -> R.string.decision_other
}

/** The last 30 notifications the app judged, newest first. Only the app and the verdict: no names, no text. */
@Composable
fun DecisionsScreen() {
    val ctx = LocalContext.current
    val list by Pager.decisions.collectAsStateWithLifecycle()
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.decisions_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.decisions_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (list.isEmpty()) Text(stringResource(R.string.decisions_empty))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list) { d ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(CLOCK.format(Instant.ofEpochMilli(d.atMs)), style = MaterialTheme.typography.bodySmall)
                    Column {
                        Text(appTitle(ctx, d.app), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(decisionLabel(d.outcome)), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
