package app.pkonowrocki.casiopager.ui

import androidx.annotation.StringRes
import app.pkonowrocki.casiopager.R

enum class SetupStep { PAIR, PERMISSIONS, LISTENER, BATTERY }

/** [why] is the one sentence the first-run screen gives for asking. */
data class SetupItem(val step: SetupStep, @StringRes val label: Int, @StringRes val why: Int, val done: Boolean)

object SetupChecklist {
    fun items(paired: Boolean, permissions: Boolean, listener: Boolean, battery: Boolean) = listOf(
        SetupItem(SetupStep.PAIR, R.string.setup_pair, R.string.why_pair, paired),
        SetupItem(SetupStep.PERMISSIONS, R.string.setup_permissions, R.string.why_permissions, permissions),
        SetupItem(SetupStep.LISTENER, R.string.setup_listener, R.string.why_listener, listener),
        SetupItem(SetupStep.BATTERY, R.string.setup_battery, R.string.why_battery, battery),
    )

    fun complete(items: List<SetupItem>) = items.all { it.done }
}
