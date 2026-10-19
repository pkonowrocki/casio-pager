package app.pkonowrocki.casiopager.tile

import androidx.annotation.StringRes
import app.pkonowrocki.casiopager.R
import app.pkonowrocki.casiopager.ui.CheckInput
import app.pkonowrocki.casiopager.ui.Problem
import app.pkonowrocki.casiopager.ui.SelfCheck

/** What the quick settings tile shows. [arg] is the format argument of [subtitle], if it has one. */
data class TileView(val active: Boolean, @StringRes val subtitle: Int, val arg: String? = null)

object TileModel {
    fun of(i: CheckInput, sinceMs: Long?, nowMs: Long): TileView = when (SelfCheck.check(i).problem) {
        Problem.NOT_PAIRED -> TileView(false, R.string.tile_unpaired)
        Problem.BLUETOOTH_OFF, Problem.PERMISSION, Problem.LISTENER -> TileView(false, R.string.tile_setup)
        Problem.SERVICE_STOPPED -> TileView(false, R.string.tile_start)
        // BATTERY and NO_LINK are not blocking: the tile just says whether the link is held
        else ->
            if (SelfCheck.linked(i.link)) TileView(true, R.string.tile_linked, sinceMs?.let { held(nowMs - it) } ?: "")
            else TileView(false, R.string.tile_waiting)
    }

    /** "<1 min", "20 min", "1 h 20 min", "2 h". Units are the same in every language the app speaks. */
    fun held(ms: Long): String {
        val min = (ms / 60_000).coerceAtLeast(0)
        return when {
            min < 1 -> "<1 min"
            min < 60 -> "$min min"
            min % 60 == 0L -> "${min / 60} h"
            else -> "${min / 60} h ${min % 60} min"
        }
    }
}
