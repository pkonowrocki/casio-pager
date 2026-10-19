package app.pkonowrocki.casiopager.ui

import app.pkonowrocki.casiopager.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pkonowrocki.casiopager.Pager
import app.pkonowrocki.casiopager.link.LinkState
import app.pkonowrocki.casiopager.protocol.BasicSettings
import kotlinx.coroutines.launch

/** The watch's own settings, read from it and written back the way the Casio app does (the whole record, one change). */
@Composable
fun WatchScreen() {
    val link by Pager.linkState.collectAsStateWithLifecycle()
    val held = link is LinkState.Held
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf<BasicSettings?>(null) }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<Int?>(null) }

    fun refresh() {
        scope.launch {
            busy = true
            settings = Pager.control?.readBasic()
            note = if (settings == null) R.string.watch_read_fail else null
            busy = false
        }
    }

    fun change(f: (BasicSettings) -> BasicSettings) {
        scope.launch {
            busy = true
            val now = Pager.control?.changeBasic(f)
            if (now != null) {
                settings = now
                note = R.string.watch_sent
            } else {
                note = R.string.watch_rejected
            }
            busy = false
        }
    }

    LaunchedEffect(held) { if (held) refresh() else { settings = null; note = null } }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ClockSyncLine()
        val s = settings
        when {
            !held -> Hint(stringResource(R.string.watch_link_first))
            s == null -> Hint(stringResource(if (busy) R.string.watch_reading else (note ?: R.string.watch_no_settings)))
        }
        if (s != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(stringResource(R.string.watch_settings_title), style = MaterialTheme.typography.titleMedium)
                    Line(stringResource(R.string.power_saving), stringResource(R.string.power_saving_hint), s.powerSaving, !busy) {
                        change { it.withPowerSaving(!s.powerSaving) }
                    }
                    Line(stringResource(R.string.button_sound), stringResource(R.string.button_sound_hint), s.buttonSound, !busy) {
                        change { it.withButtonSound(!s.buttonSound) }
                    }
                    Line(stringResource(R.string.display_24h), stringResource(R.string.display_24h_hint), s.use24h, !busy) {
                        change { it.with24h(!s.use24h) }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.light_duration))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !s.longLight, enabled = !busy, onClick = { change { it.withLongLight(false) } }, label = { Text(stringResource(R.string.light_short)) })
                            FilterChip(selected = s.longLight, enabled = !busy, onClick = { change { it.withLongLight(true) } }, label = { Text(stringResource(R.string.light_long)) })
                        }
                    }
                }
            }
            OutlinedButton(enabled = !busy, onClick = { refresh() }) { Text(stringResource(R.string.read_again)) }
        }
        note?.let { if (s != null) Text(stringResource(it), style = MaterialTheme.typography.bodySmall) }
        StepsCard(held)
        TimerCard(held)
        ConnectionCard(held)
        Hint(stringResource(R.string.watch_footer))
    }
}

@Composable
private fun Line(title: String, hint: String, on: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(on, { onToggle() }, enabled = enabled)
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
