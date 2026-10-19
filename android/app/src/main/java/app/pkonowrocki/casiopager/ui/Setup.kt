package app.pkonowrocki.casiopager.ui

import android.Manifest
import android.bluetooth.BluetoothManager
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothLeDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pkonowrocki.casiopager.Pager
import app.pkonowrocki.casiopager.link.LinkState
import app.pkonowrocki.casiopager.link.WatchLinkService
import app.pkonowrocki.casiopager.log.PagerLog
import app.pkonowrocki.casiopager.model.WatchAddress
import java.util.regex.Pattern
import kotlinx.coroutines.delay

/**
 * The four setup steps with their current state and what the Set up button of each one does, plus the one-glance self-check
 * and what its button does. Shared by Status and the first-run screen.
 */
class SetupState(
    val items: List<SetupItem>,
    val fix: (SetupStep) -> Unit,
    val check: CheckResult,
    val act: (CheckAction) -> Unit,
) {
    val permissionsDone get() = items.first { it.step == SetupStep.PERMISSIONS }.done
}

/** A snapshot of everything the self-check looks at. Not composable, so the quick settings tile can use it too. */
fun readCheckInput(ctx: Context, link: LinkState, lastAlert: String? = null): CheckInput {
    fun granted(p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED
    return CheckInput(
        link = link,
        paired = WatchAddress.parse(Pager.settings.value.watchAddress) != null,
        bluetoothPermission = granted(Manifest.permission.BLUETOOTH_CONNECT),
        notificationPermission = granted(Manifest.permission.POST_NOTIFICATIONS),
        listenerEnabled = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName),
        batteryIgnored = ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName),
        bluetoothOn = ctx.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true,
        serviceRunning = Pager.link != null, // set while WatchLinkService runs
        lastAlert = lastAlert,
    )
}

@Composable
fun rememberSetup(): SetupState {
    val ctx = LocalContext.current
    val link by Pager.linkState.collectAsStateWithLifecycle()
    val last by Pager.lastAlert.collectAsStateWithLifecycle()
    Pager.settings.collectAsStateWithLifecycle().value // recompose when the watch address changes
    // read here so we recompose every second and re-check permissions and Bluetooth
    produceState(0) { while (true) { delay(1_000); value++ } }.value
    val askPerms = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
    val pickWatch = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}

    val input = readCheckInput(ctx, link, last?.let { clock(it.atMs) })
    val items = SetupChecklist.items(
        paired = input.paired,
        permissions = input.bluetoothPermission && input.notificationPermission,
        listener = input.listenerEnabled,
        battery = input.batteryIgnored,
    )
    val fix: (SetupStep) -> Unit = { step ->
        when (step) {
            SetupStep.PAIR -> pairWatch(ctx) { pickWatch.launch(IntentSenderRequest.Builder(it).build()) }
            SetupStep.PERMISSIONS ->
                askPerms.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.POST_NOTIFICATIONS))
            SetupStep.LISTENER -> ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            SetupStep.BATTERY -> ctx.startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))
            )
        }
    }
    val act: (CheckAction) -> Unit = { action ->
        when (action) {
            CheckAction.BLUETOOTH_SETTINGS -> ctx.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            CheckAction.SETUP_PERMISSIONS -> fix(SetupStep.PERMISSIONS)
            CheckAction.SETUP_LISTENER -> fix(SetupStep.LISTENER)
            CheckAction.PAIR -> fix(SetupStep.PAIR)
            CheckAction.SETUP_BATTERY -> fix(SetupStep.BATTERY)
            CheckAction.START -> ContextCompat.startForegroundService(ctx, Intent(ctx, WatchLinkService::class.java))
        }
    }
    return SetupState(items, fix, SelfCheck.check(input), act)
}

/** Long-press the watch while the picker is open. Once associated, the system wakes PresenceService when it advertises. */
private fun pairWatch(ctx: Context, launch: (IntentSender) -> Unit) {
    val cdm = ctx.getSystemService(CompanionDeviceManager::class.java)
    val request = AssociationRequest.Builder()
        .addDeviceFilter(
            BluetoothLeDeviceFilter.Builder().setNamePattern(Pattern.compile("CASIO ABL-100WE.*")).build()
        )
        .setSingleDevice(true)
        .build()
    cdm.associate(request, ctx.mainExecutor, object : CompanionDeviceManager.Callback() {
        override fun onAssociationPending(intentSender: IntentSender) = launch(intentSender)

        override fun onAssociationCreated(associationInfo: AssociationInfo) {
            val address = associationInfo.deviceMacAddress?.toString()?.uppercase() ?: return
            Pager.update { it.copy(watchAddress = address) } // from now on the app talks to this watch only
            PagerLog.i("presence: associated, observing")
            @Suppress("DEPRECATION")
            cdm.startObservingDevicePresence(address)
        }

        override fun onFailure(error: CharSequence?) { PagerLog.i("presence: association failed") }
    })
}
