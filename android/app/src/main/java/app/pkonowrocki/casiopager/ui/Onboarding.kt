package app.pkonowrocki.casiopager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.pkonowrocki.casiopager.R

/** Shown once, before the tabs: the four setup steps, each with the reason for it. [onFinish] is Done and Skip alike. */
@Composable
fun OnboardingScreen(setup: SetupState, onFinish: () -> Unit) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.onb_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.onb_intro), style = MaterialTheme.typography.bodyLarge)
        setup.items.forEach { item ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            (if (item.done) "✓  " else "○  ") + stringResource(item.label),
                            Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                        )
                        if (!item.done) OutlinedButton(onClick = { setup.fix(item.step) }) { Text(stringResource(R.string.setup_fix)) }
                    }
                    Text(stringResource(item.why), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (SetupChecklist.complete(setup.items)) {
            Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.onb_done)) }
        } else {
            TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.onb_skip)) }
        }
    }
}
