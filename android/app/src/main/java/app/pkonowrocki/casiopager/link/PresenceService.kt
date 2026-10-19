package app.pkonowrocki.casiopager.link

import android.Manifest
import android.companion.CompanionDeviceService
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import app.pkonowrocki.casiopager.Pager
import app.pkonowrocki.casiopager.log.PagerLog

/** Woken by the system when the paired watch starts advertising (a long-press), even if the app is not running. */
class PresenceService : CompanionDeviceService() {
    @Deprecated("kept for API 31-35")
    override fun onDeviceAppeared(address: String) {
        Pager.init(applicationContext)
        PagerLog.i("presence: watch appeared")
        // a connectedDevice foreground service needs the Bluetooth permission (targetSdk 34+); without it starting crashes
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            PagerLog.i("presence: Bluetooth permission not granted, not starting")
            return
        }
        ContextCompat.startForegroundService(this, Intent(this, WatchLinkService::class.java))
    }

    @Deprecated("kept for API 31-35")
    override fun onDeviceDisappeared(address: String) {
        Pager.init(applicationContext)
        PagerLog.i("presence: watch disappeared")
    }
}
