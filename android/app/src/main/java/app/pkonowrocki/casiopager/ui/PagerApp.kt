package app.pkonowrocki.casiopager.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pkonowrocki.casiopager.Pager
import app.pkonowrocki.casiopager.R

private val TABS = listOf(
    R.string.tab_status to R.drawable.ic_tab_status,
    R.string.tab_rules to R.drawable.ic_tab_rules,
    R.string.tab_watch to R.drawable.ic_tab_watch,
    R.string.tab_settings to R.drawable.ic_tab_settings,
)

@Composable
fun PagerApp() {
    var tab by remember { mutableIntStateOf(0) }
    val settings by Pager.settings.collectAsStateWithLifecycle()
    val setup = rememberSetup()
    Scaffold(bottomBar = {
        if (settings.onboarded) CappedFontScale { NavigationBar {
            TABS.forEachIndexed { i, (label, icon) ->
                NavigationBarItem(
                    selected = tab == i,
                    onClick = { tab = i },
                    icon = { Icon(painterResource(icon), contentDescription = null) }, // the label already names the tab
                    label = { Text(stringResource(label)) },
                )
            }
        } }
    }) { pad ->
        Box(Modifier.padding(pad)) {
            if (!settings.onboarded) {
                OnboardingScreen(setup) { Pager.update { it.copy(onboarded = true) } }
            } else when (tab) {
                0 -> StatusScreen(setup)
                1 -> RulesScreen()
                2 -> WatchScreen()
                else -> SettingsScreen()
            }
        }
    }
}

/** Large system text sizes are respected everywhere except the bottom bar, whose four labels must stay on one line. */
@Composable
private fun CappedFontScale(content: @Composable () -> Unit) {
    val d = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(d.density, minOf(d.fontScale, 1.3f)), content = content)
}
