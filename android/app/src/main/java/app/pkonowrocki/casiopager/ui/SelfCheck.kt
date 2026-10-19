package app.pkonowrocki.casiopager.ui

import androidx.annotation.StringRes
import app.pkonowrocki.casiopager.R
import app.pkonowrocki.casiopager.link.LinkState

enum class Problem { BLUETOOTH_OFF, PERMISSION, LISTENER, NOT_PAIRED, SERVICE_STOPPED, BATTERY, NO_LINK }

/** What the button of a problem does. */
enum class CheckAction { BLUETOOTH_SETTINGS, SETUP_PERMISSIONS, SETUP_LISTENER, PAIR, START, SETUP_BATTERY }

/** Everything the self-check looks at. [lastAlert] is already formatted (the presenter stays pure). */
data class CheckInput(
    val link: LinkState,
    val paired: Boolean,
    val bluetoothPermission: Boolean,
    val notificationPermission: Boolean,
    val listenerEnabled: Boolean,
    val batteryIgnored: Boolean,
    val bluetoothOn: Boolean,
    val serviceRunning: Boolean,
    val lastAlert: String? = null,
)

/** [problem] is null when all is good. [button] and [action] are null when there is nothing to tap (the user long-presses the watch). */
data class CheckResult(
    val problem: Problem?,
    val level: StatusLevel,
    @StringRes val title: Int,
    @StringRes val hint: Int,
    val hintArgs: List<Any> = emptyList(),
    @StringRes val button: Int? = null,
    val action: CheckAction? = null,
)

/** The single most important thing that stops alerts, or "all good". Pure, so it is unit-tested. */
object SelfCheck {
    fun linked(link: LinkState) = link is LinkState.Held && (link.kind == null || link.kind == 1)

    fun check(i: CheckInput): CheckResult = when {
        !i.bluetoothOn -> CheckResult(
            Problem.BLUETOOTH_OFF, StatusLevel.BAD, R.string.check_bt_off_title, R.string.check_bt_off_hint,
            button = R.string.check_bt_button, action = CheckAction.BLUETOOTH_SETTINGS,
        )
        !(i.bluetoothPermission && i.notificationPermission) -> CheckResult(
            Problem.PERMISSION, StatusLevel.BAD, R.string.check_perm_title, R.string.check_perm_hint,
            button = R.string.setup_fix, action = CheckAction.SETUP_PERMISSIONS,
        )
        !i.listenerEnabled -> CheckResult(
            Problem.LISTENER, StatusLevel.BAD, R.string.check_listener_title, R.string.check_listener_hint,
            button = R.string.setup_fix, action = CheckAction.SETUP_LISTENER,
        )
        !i.paired -> CheckResult(
            Problem.NOT_PAIRED, StatusLevel.BAD, R.string.check_unpaired_title, R.string.check_unpaired_hint,
            button = R.string.setup_fix, action = CheckAction.PAIR,
        )
        !i.serviceRunning -> CheckResult(
            Problem.SERVICE_STOPPED, StatusLevel.WAITING, R.string.check_stopped_title, R.string.check_stopped_hint,
            button = R.string.action_start, action = CheckAction.START,
        )
        !i.batteryIgnored -> CheckResult(
            Problem.BATTERY, StatusLevel.WAITING, R.string.check_battery_title, R.string.check_battery_hint,
            button = R.string.setup_fix, action = CheckAction.SETUP_BATTERY,
        )
        i.link == LinkState.Initialising ->
            CheckResult(Problem.NO_LINK, StatusLevel.WAITING, R.string.check_linking_title, R.string.check_linking_hint)
        !linked(i.link) ->
            CheckResult(Problem.NO_LINK, StatusLevel.WAITING, R.string.check_nolink_title, R.string.check_nolink_hint)
        i.lastAlert != null ->
            CheckResult(null, StatusLevel.GOOD, R.string.check_ok_title, R.string.check_ok_hint_last, listOf(i.lastAlert))
        else -> CheckResult(null, StatusLevel.GOOD, R.string.check_ok_title, R.string.check_ok_hint)
    }
}
