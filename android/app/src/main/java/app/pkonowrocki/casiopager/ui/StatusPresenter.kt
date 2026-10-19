package app.pkonowrocki.casiopager.ui

import androidx.annotation.StringRes
import app.pkonowrocki.casiopager.R
import app.pkonowrocki.casiopager.link.LinkState

enum class StatusLevel { GOOD, WAITING, BAD, IDLE }

/** [hintArgs] are the format arguments of the [hint] string resource. */
data class StatusView(
    val level: StatusLevel,
    @StringRes val title: Int,
    @StringRes val hint: Int,
    val hintArgs: List<Any> = emptyList(),
)

/** Turns the link state into what the Status card shows. Pure, so it is unit-tested. */
object StatusPresenter {
    fun present(state: LinkState, since: String?): StatusView = when (state) {
        LinkState.Idle -> StatusView(StatusLevel.IDLE, R.string.status_idle_title, R.string.status_idle_hint)
        LinkState.NotPaired -> StatusView(StatusLevel.IDLE, R.string.status_unpaired_title, R.string.status_unpaired_hint)
        LinkState.Connecting -> StatusView(StatusLevel.WAITING, R.string.status_waiting_title, R.string.status_waiting_hint)
        LinkState.Initialising -> StatusView(StatusLevel.WAITING, R.string.status_connecting_title, R.string.status_connecting_hint)
        LinkState.Lost -> StatusView(StatusLevel.BAD, R.string.status_lost_title, R.string.status_lost_hint)
        is LinkState.Held ->
            if (state.kind == null || state.kind == 1) {
                if (since == null) StatusView(StatusLevel.GOOD, R.string.status_linked_title, R.string.status_linked_hint)
                else StatusView(StatusLevel.GOOD, R.string.status_linked_title, R.string.status_linked_hint_since, listOf(since))
            } else {
                StatusView(StatusLevel.WAITING, R.string.status_short_title, R.string.status_short_hint, listOf(state.kind))
            }
    }
}
