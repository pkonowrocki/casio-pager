package app.pkonowrocki.casiopager.ui

import app.pkonowrocki.casiopager.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.pkonowrocki.casiopager.Pager
import app.pkonowrocki.casiopager.protocol.ConnectionSettings
import app.pkonowrocki.casiopager.protocol.LifeLog
import app.pkonowrocki.casiopager.protocol.TimerRecord
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch

private val HM = DateTimeFormatter.ofPattern("HH:mm")
private val DAY = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

/** Steps: the stored history (read from the watch's Life Log on every link) plus a manual refresh while linked. */
@Composable
fun StepsCard(held: Boolean) {
    val scope = rememberCoroutineScope()
    val history by Pager.stepsHistory.collectAsStateWithLifecycle()
    val updatedMs by Pager.stepsUpdatedMs.collectAsStateWithLifecycle()
    var log by remember { mutableStateOf<LifeLog?>(null) }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<Int?>(null) }
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.steps_title), style = MaterialTheme.typography.titleMedium)
            OutlinedButton(enabled = held && !busy, onClick = {
                scope.launch {
                    busy = true
                    log = Pager.control?.readLifeLog()
                    note = if (log == null) R.string.steps_fail else null
                    busy = false
                }
            }) { Text(stringResource(if (busy) R.string.steps_reading else R.string.steps_read)) }
            if (history.isEmpty()) {
                Text(stringResource(R.string.steps_none), style = MaterialTheme.typography.bodyMedium)
            } else {
                history.firstOrNull { it.date == today }?.let {
                    Text(stringResource(R.string.steps_today_stored, it.steps, it.distanceMeters), style = MaterialTheme.typography.bodyLarge)
                }
                updatedMs?.let { ms ->
                    val at = Instant.ofEpochMilli(ms).atZone(zone)
                    val whenText = when (syncDay(at.toLocalDate(), today)) {
                        SyncDay.TODAY -> stringResource(R.string.day_today) + " " + HM.format(at)
                        SyncDay.YESTERDAY -> stringResource(R.string.day_yesterday) + " " + HM.format(at)
                        SyncDay.EARLIER -> DAY.format(at) + ", " + HM.format(at)
                    }
                    Text(stringResource(R.string.steps_updated, whenText), style = MaterialTheme.typography.bodySmall)
                }
                history.filter { it.date != today }.take(7).forEach {
                    val label = if (it.date == today.minusDays(1)) stringResource(R.string.day_yesterday) else DAY.format(it.date)
                    Text(stringResource(R.string.steps_day_line, label, it.steps, it.distanceMeters), style = MaterialTheme.typography.bodyMedium)
                }
            }
            log?.let { l ->
                val hours = l.hourlySteps.withIndex().filter { it.value != null }
                if (hours.isNotEmpty()) {
                    Text(stringResource(R.string.steps_by_hour, hours.joinToString("  ") { "%02d:00 %d".format(it.index, it.value) }), style = MaterialTheme.typography.bodySmall)
                }
            }
            note?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall) }
            if (!held) Text(stringResource(R.string.available_when_linked), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** The watch's countdown timer. Setting it does not start it: use the watch's buttons. */
@Composable
fun TimerCard(held: Boolean) {
    val scope = rememberCoroutineScope()
    var h by remember { mutableStateOf("") }
    var m by remember { mutableStateOf("") }
    var s by remember { mutableStateOf("") }
    var note by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun show(t: TimerRecord) { h = t.hours.toString(); m = t.minutes.toString(); s = t.seconds.toString() }
    LaunchedEffect(held) { if (held) Pager.control?.readTimer()?.let { show(it) } }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.timer_title), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberBox(stringResource(R.string.unit_h), h, Modifier.weight(1f)) { h = it }
                NumberBox(stringResource(R.string.unit_min), m, Modifier.weight(1f)) { m = it }
                NumberBox(stringResource(R.string.unit_s), s, Modifier.weight(1f)) { s = it }
            }
            Button(enabled = held && !busy, onClick = {
                val hh = h.toIntOrNull(); val mm = m.toIntOrNull(); val ss = s.toIntOrNull()
                if (hh == null || mm == null || ss == null || hh !in 0..23 || mm !in 0..59 || ss !in 0..59) {
                    note = R.string.timer_invalid
                } else scope.launch {
                    busy = true
                    val now = Pager.control?.changeTimer { it.withDuration(hh, mm, ss) }
                    if (now != null) { show(now); note = R.string.timer_ok } else note = R.string.timer_rejected
                    busy = false
                }
            }) { Text(stringResource(R.string.timer_set)) }
            Text(stringResource(note ?: R.string.timer_hint), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** "Connection time with the app": 3, 5 or 10 minutes (it does not limit a held link). */
@Composable
fun ConnectionCard(held: Boolean) {
    val scope = rememberCoroutineScope()
    var conn by remember { mutableStateOf<ConnectionSettings?>(null) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(held) { conn = if (held) Pager.control?.readConnection() else null }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.conn_title), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConnectionSettings.OFFERED.forEach { minutes ->
                    FilterChip(
                        selected = conn?.minutes == minutes,
                        enabled = conn != null && !busy,
                        onClick = {
                            scope.launch {
                                busy = true
                                Pager.control?.changeConnection { it.withMinutes(minutes) }?.let { conn = it }
                                busy = false
                            }
                        },
                        label = { Text(stringResource(R.string.conn_minutes, minutes)) },
                    )
                }
            }
            conn?.let {
                Text(
                    if (it.autoReconnect) stringResource(R.string.conn_auto, it.reconnectHour, it.reconnectMinute) else stringResource(R.string.conn_off),
                    style = MaterialTheme.typography.bodySmall,
                )
            } ?: Text(stringResource(R.string.available_when_linked), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun NumberBox(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(value, { v -> onChange(v.filter { it.isDigit() }.take(2)) }, label = { Text(label) }, singleLine = true, modifier = modifier)
}
