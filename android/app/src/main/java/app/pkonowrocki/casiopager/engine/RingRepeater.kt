package app.pkonowrocki.casiopager.engine

import kotlinx.coroutines.delay

/**
 * While a call keeps ringing, repeat the alert every [intervalMs], at most [maxRepeats] times.
 * Stops early when the call stops ringing or when [beep] reports that the alert did not go out (link gone).
 */
class RingRepeater(private val intervalMs: Long, private val maxRepeats: Int) {
    suspend fun run(ringing: () -> Boolean, beep: suspend () -> Boolean) {
        repeat(maxRepeats) {
            delay(intervalMs)
            if (!ringing()) return
            if (!beep()) return
        }
    }
}
