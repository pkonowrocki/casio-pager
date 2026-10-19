package app.pkonowrocki.casiopager.ui

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pkonowrocki.casiopager.Pager
import app.pkonowrocki.casiopager.R
import app.pkonowrocki.casiopager.link.LinkState
import app.pkonowrocki.casiopager.link.WatchLinkService
import app.pkonowrocki.casiopager.log.PagerLog
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private val CLOCK = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
internal fun clock(ms: Long): String = CLOCK.format(Instant.ofEpochMilli(ms))

@Composable
fun StatusScreen(setup: SetupState) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val link by Pager.linkState.collectAsStateWithLifecycle()
    val since by Pager.linkSinceMs.collectAsStateWithLifecycle()
    val last by Pager.lastAlert.collectAsStateWithLifecycle()
    var testNote by remember { mutableStateOf<Int?>(null) }

    val items = setup.items
    val view = StatusPresenter.present(link, since?.let(::clock))
    val held = link is LinkState.Held
    val haptic = LocalHapticFeedback.current

    fun test() = scope.launch {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val sent = Pager.link?.alert() == true
        PagerLog.i("test alert sent=$sent")
        testNote = if (sent) R.string.test_sent else R.string.test_no_link
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SelfCheckCard(setup.check, setup.act)
        // the self-check already says what to do when there is no link; this card adds the detail while one is being set up or held
        if (link is LinkState.Held || link == LinkState.Connecting || link == LinkState.Initialising) StatusCard(view)
        last?.let {
            Text(stringResource(R.string.status_last_alert, clock(it.atMs), appTitle(ctx, it.app)), style = MaterialTheme.typography.bodyMedium)
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.check_watch_title), style = MaterialTheme.typography.titleMedium)
                OutlinedButton(enabled = held, onClick = { test() }) { Text(stringResource(R.string.test_beep)) }
                Text(
                    stringResource(testNote ?: if (held) R.string.test_hint_linked else R.string.available_when_linked),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (!SetupChecklist.complete(items)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.titleMedium)
                    items.forEach { item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text((if (item.done) "✓  " else "○  ") + stringResource(item.label), Modifier.weight(1f))
                            if (!item.done) OutlinedButton(onClick = { setup.fix(item.step) }) { Text(stringResource(R.string.setup_fix)) }
                        }
                    }
                    Text(stringResource(R.string.setup_pair_help), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // quiet secondary actions, grouped at the bottom: the primary action lives in the self-check card above
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (SetupChecklist.complete(items)) TextButton(onClick = { setup.fix(SetupStep.PAIR) }) { Text(stringResource(R.string.pair_other)) }
            TextButton(onClick = { ctx.stopService(Intent(ctx, WatchLinkService::class.java)) }) { Text(stringResource(R.string.action_stop)) }
        }
    }
}

@Composable
private fun levelColors(level: StatusLevel): Pair<Color, Color> {
    val dark = isSystemInDarkTheme()
    return when (level) {
        StatusLevel.GOOD -> if (dark) Color(0xFF1F4D2B) to Color(0xFFD8F5DC) else Color(0xFFCDEBCF) to Color(0xFF0B3318)
        StatusLevel.WAITING -> if (dark) Color(0xFF5A4300) to Color(0xFFFFE9B0) else Color(0xFFFFE6A8) to Color(0xFF3D2E00)
        StatusLevel.BAD -> if (dark) Color(0xFF6B1B14) to Color(0xFFFFDAD6) else Color(0xFFFFD9D6) to Color(0xFF410002)
        StatusLevel.IDLE -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
}

/** "Is it working?" in one glance: a green tick, or the one thing that stops alerts and a button for it. */
@Composable
private fun SelfCheckCard(r: CheckResult, onAct: (CheckAction) -> Unit) {
    val (bg, fg) = levelColors(r.level)
    val container by animateColorAsState(bg, tween(300), label = "check-bg")
    val content by animateColorAsState(fg, tween(300), label = "check-fg")
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = container, contentColor = content)) {
        AnimatedContent(r, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) }, label = "check") { res ->
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text((if (res.problem == null) "✓  " else "") + stringResource(res.title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(res.hint, *res.hintArgs.toTypedArray()), style = MaterialTheme.typography.bodySmall)
                }
                val action = res.action
                val label = res.button
                if (action != null && label != null) OutlinedButton(onClick = { onAct(action) }) { Text(stringResource(label)) }
            }
        }
    }
}

@Composable
private fun StatusCard(v: StatusView) {
    val (bg, fg) = levelColors(v.level)
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = bg, contentColor = fg)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("●  " + stringResource(v.title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(v.hint, *v.hintArgs.toTypedArray()), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
