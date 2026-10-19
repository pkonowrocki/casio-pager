package app.pkonowrocki.casiopager.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import app.pkonowrocki.casiopager.Pager
import app.pkonowrocki.casiopager.R
import app.pkonowrocki.casiopager.log.DiagnosticsLink

class MainActivity : ComponentActivity() {
    /** A diagnostics server from a setup link, waiting for the user to confirm. Never logged: its path is a secret. */
    private var pendingServer by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge() // status and navigation bar icons follow the light or dark theme (they vanished on a light background)
        super.onCreate(savedInstanceState)
        Pager.init(this)
        if (savedInstanceState == null) takeLink(intent)
        setContent {
            PagerTheme {
                PagerApp()
                pendingServer?.let { url ->
                    AlertDialog(
                        onDismissRequest = { pendingServer = null },
                        title = { Text(stringResource(R.string.diag_dialog_title, DiagnosticsLink.hostOf(url) ?: "")) },
                        text = { Text(stringResource(R.string.diag_dialog_body)) },
                        confirmButton = {
                            TextButton({
                                Pager.update { it.copy(lokiUrl = url, lokiEnabled = true) }
                                pendingServer = null
                            }) { Text(stringResource(R.string.diag_enable)) }
                        },
                        dismissButton = { TextButton({ pendingServer = null }) { Text(stringResource(R.string.diag_cancel)) } },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        takeLink(intent)
    }

    private fun takeLink(intent: Intent?) {
        DiagnosticsLink.parse(intent?.dataString)?.let { pendingServer = it }
    }
}
