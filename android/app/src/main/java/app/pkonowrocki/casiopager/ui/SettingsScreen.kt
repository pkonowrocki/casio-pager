package app.pkonowrocki.casiopager.ui

import app.pkonowrocki.casiopager.R
import androidx.compose.ui.res.stringResource
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pkonowrocki.casiopager.BuildConfig
import app.pkonowrocki.casiopager.Pager
import app.pkonowrocki.casiopager.log.DiagnosticsLink
import app.pkonowrocki.casiopager.model.WatchAddress
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

private const val UNLOCK_TAPS = 7

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen() {
    var sub by remember { mutableStateOf<String?>(null) } // "logs" or "decisions": a sub-screen with a back button
    if (sub != null) {
        BackHandler { sub = null }
        Column {
            OutlinedButton({ sub = null }, Modifier.padding(start = 12.dp, top = 8.dp)) { Text(stringResource(R.string.log_back)) }
            if (sub == "logs") LogScreen() else DecisionsScreen()
        }
        return
    }
    val ctx = LocalContext.current
    val s by Pager.settings.collectAsStateWithLifecycle()
    val q = s.quietHours
    var taps by remember { mutableStateOf(0) } // session-only: diagnostics lock again on the next start
    val unlocked = taps >= UNLOCK_TAPS

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Section(stringResource(R.string.section_alerts)) {
            SwitchRow(stringResource(R.string.set_groups), s.alertInGroups) { v -> Pager.update { it.copy(alertInGroups = v) } }
            SecondsField(stringResource(R.string.set_throttle), s.throttleMs) { v -> Pager.update { it.copy(throttleMs = v) } }
            SecondsField(stringResource(R.string.set_call_repeat), s.callRepeatMs) { v -> Pager.update { it.copy(callRepeatMs = v) } }
        }

        Section(stringResource(R.string.section_quiet)) {
            SwitchRow(stringResource(R.string.set_enabled), q.enabled) { v -> Pager.update { it.copy(quietHours = it.quietHours.copy(enabled = v)) } }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeField(stringResource(R.string.set_from), q.start, Modifier.weight(1f)) { t -> Pager.update { it.copy(quietHours = it.quietHours.copy(start = t)) } }
                TimeField(stringResource(R.string.set_until), q.end, Modifier.weight(1f)) { t -> Pager.update { it.copy(quietHours = it.quietHours.copy(end = t)) } }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) { // wraps instead of squeezing a day at large text sizes
                DayOfWeek.values().forEach { d ->
                    FilterChip(selected = d in q.days, onClick = {
                        Pager.update { cur ->
                            val days = cur.quietHours.days
                            cur.copy(quietHours = cur.quietHours.copy(days = if (d in days) days - d else days + d))
                        }
                    }, label = { Text(dayLabel(d, Locale.getDefault())) },
                        leadingIcon = if (d in q.days) ({ Text("✓") }) else null) // the tick makes selected obvious in both themes
                }
            }
            SwitchRow(stringResource(R.string.set_calls_beep), q.callsBypass) { v -> Pager.update { it.copy(quietHours = it.quietHours.copy(callsBypass = v)) } }
        }

        Section(stringResource(R.string.section_watch)) {
            val paired = WatchAddress.parse(s.watchAddress) != null
            Text(stringResource(if (paired) R.string.set_watch_state_paired else R.string.set_watch_state_not))
            // the raw address is for troubleshooting only, so it lives behind the same seven taps as diagnostics
            if (unlocked) AddressField(s.watchAddress) { a -> Pager.update { it.copy(watchAddress = a) } }
        }

        Section(stringResource(R.string.section_help)) {
            OutlinedButton({ sub = "decisions" }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.set_decisions)) }
            OutlinedButton({ sub = "logs" }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.set_logs)) }
        }

        if (unlocked) Section(stringResource(R.string.section_diagnostics)) {
            val host = DiagnosticsLink.hostOf(s.lokiUrl)
            SwitchRow(stringResource(R.string.diag_send), s.lokiActive) { v -> Pager.update { it.copy(lokiEnabled = v) } }
            Text(
                if (host != null) stringResource(R.string.diag_server, host) else stringResource(R.string.diag_no_server),
                style = MaterialTheme.typography.bodySmall,
            )
            if (host != null) OutlinedButton({ Pager.update { it.copy(lokiUrl = "", lokiEnabled = false) } }) {
                Text(stringResource(R.string.diag_remove))
            }
        }

        Section(stringResource(R.string.section_about)) {
            Text(
                stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                Modifier.fillMaxWidth().clickable {
                    if (unlocked) return@clickable
                    taps++
                    val left = UNLOCK_TAPS - taps
                    val msg = when {
                        left == 0 -> ctx.getString(R.string.diag_unlocked)
                        left <= 3 -> ctx.getString(R.string.diag_taps, left)
                        else -> null
                    }
                    msg?.let { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
                },
            )
            Text(stringResource(R.string.about_disclaimer), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** A short day name for the chips: up to three letters, without the trailing dot some locales add ("pon."). */
internal fun dayLabel(d: DayOfWeek, locale: Locale): String = d.getDisplayName(TextStyle.SHORT, locale).trimEnd('.').take(3)

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f))
        Switch(checked, onChange)
    }
}

/** Only a complete, valid address is saved; a half-typed one stays in the field (red) and never reaches the service. */
@Composable
private fun AddressField(value: String, onValid: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        text,
        { v -> text = v; WatchAddress.parse(v)?.let(onValid) },
        label = { Text(stringResource(R.string.set_watch_address)) },
        singleLine = true,
        isError = text.isNotEmpty() && WatchAddress.parse(text) == null,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TimeField(label: String, value: LocalTime, modifier: Modifier, onValid: (LocalTime) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        text, { v -> text = v; runCatching { LocalTime.parse(v) }.onSuccess(onValid) },
        label = { Text(label) }, singleLine = true, modifier = modifier,
    )
}

/** Stored in milliseconds, shown and typed in seconds ("30", or "2.5"). */
internal fun msToSecondsText(ms: Long): String =
    if (ms % 1000 == 0L) (ms / 1000).toString() else (ms / 1000.0).toString().trimEnd('0').trimEnd('.')

internal fun secondsTextToMs(text: String): Long? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 && it.isFinite() }?.let { Math.round(it * 1000) }

@Composable
private fun SecondsField(label: String, valueMs: Long, onValid: (Long) -> Unit) {
    var text by remember(valueMs) { mutableStateOf(msToSecondsText(valueMs)) }
    // the label sits above the field, not inside it: long translations and large text sizes would overlap a floating label
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            text, { v -> text = v; secondsTextToMs(v)?.let(onValid) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
    }
}
