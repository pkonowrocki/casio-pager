package app.pkonowrocki.casiopager.link

import app.pkonowrocki.casiopager.protocol.BasicSettings
import app.pkonowrocki.casiopager.protocol.ConnectionSettings
import app.pkonowrocki.casiopager.protocol.LifeLog
import app.pkonowrocki.casiopager.protocol.TimerRecord

sealed interface LinkState {
    data object Idle : LinkState
    data object NotPaired : LinkState
    data object Connecting : LinkState
    data object Initialising : LinkState
    data class Held(val kind: Int?) : LinkState
    data object Lost : LinkState
}

/** Reading and changing the watch's own settings; both return null when no link is held or the watch refuses. */
interface WatchControl {
    suspend fun readBasic(): BasicSettings?
    suspend fun changeBasic(change: (BasicSettings) -> BasicSettings): BasicSettings?
    suspend fun readLifeLog(): LifeLog?
    suspend fun readTimer(): TimerRecord?
    suspend fun changeTimer(change: (TimerRecord) -> TimerRecord): TimerRecord?
    suspend fun readConnection(): ConnectionSettings?
    suspend fun changeConnection(change: (ConnectionSettings) -> ConnectionSettings): ConnectionSettings?
}

interface AlertSink {
    /** Returns true only when the write reached the watch; false (no link, or the write failed) otherwise. */
    suspend fun alert(): Boolean
}
