package app.pkonowrocki.casiopager.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import app.pkonowrocki.casiopager.Pager
import app.pkonowrocki.casiopager.link.WatchLinkService
import app.pkonowrocki.casiopager.log.PagerLog
import app.pkonowrocki.casiopager.ui.MainActivity
import app.pkonowrocki.casiopager.ui.Problem
import app.pkonowrocki.casiopager.ui.SelfCheck
import app.pkonowrocki.casiopager.ui.readCheckInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Quick settings tile: lit while a long-press link is held, and a one-tap way to start the app's service or open it. */
class PagerTileService : TileService() {
    private var scope: CoroutineScope? = null

    override fun onStartListening() {
        Pager.init(applicationContext)
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Main).also { scope = it }
        s.launch { Pager.linkState.collect { refresh() } }
        s.launch { while (true) { delay(30_000); refresh() } } // keeps the duration and the setup state current
    }

    override fun onStopListening() {
        scope?.cancel()
        scope = null
    }

    override fun onClick() {
        Pager.init(applicationContext)
        val input = readCheckInput(this, Pager.linkState.value)
        // start the link service only when it is not running and nothing blocks it; otherwise show the app
        if (SelfCheck.check(input).problem == Problem.SERVICE_STOPPED) {
            try {
                ContextCompat.startForegroundService(this, Intent(this, WatchLinkService::class.java))
                PagerLog.i("tile: started the service")
                refresh()
                return
            } catch (e: Exception) {
                PagerLog.i("tile: could not start the service, opening the app (${e.javaClass.simpleName})")
            }
        }
        openApp()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated") // the Intent overload is only used below API 34
    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val view = TileModel.of(readCheckInput(this, Pager.linkState.value), Pager.linkSinceMs.value, System.currentTimeMillis())
        tile.state = if (view.active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = if (view.arg != null) getString(view.subtitle, view.arg) else getString(view.subtitle)
        tile.updateTile()
    }
}
