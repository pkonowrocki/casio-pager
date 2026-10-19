package app.pkonowrocki.casiopager.service

import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import app.pkonowrocki.casiopager.LastAlert
import app.pkonowrocki.casiopager.Pager
import app.pkonowrocki.casiopager.engine.RingRepeater
import app.pkonowrocki.casiopager.log.PagerLog
import app.pkonowrocki.casiopager.mapping.NotificationMapper
import app.pkonowrocki.casiopager.model.Decision
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PagerNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val ringing = ConcurrentHashMap.newKeySet<String>() // notification keys with a repeater running

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        Pager.init(applicationContext)
        val raw = RawNotificationFactory.from(sbn)
        val event = NotificationMapper.map(raw)
        if (event == null) {
            if (NotificationMapper.isKnownPackage(raw.pkg)) {
                PagerLog.i("unmapped pkg=${raw.pkg} category=${raw.category} summary=${raw.isGroupSummary} " +
                    "messaging=${raw.isMessagingStyle} group=${raw.isGroupConversation} callType=${raw.callType}")
            }
            return
        }
        Pager.recordSeen(event)
        val decision = Pager.engine.decide(event, Pager.settings.value, LocalDateTime.now(), SystemClock.elapsedRealtime())
        val why = if (decision is Decision.Ignore) " (${Pager.engine.lastIgnoreReason})" else ""
        PagerLog.i("event app=${event.app} call=${event.isCall} group=${event.isGroup} -> ${decision::class.simpleName}$why")
        if (decision !is Decision.Beep) {
            Pager.recordDecision(event.app, Pager.engine.lastIgnoreReason ?: "ignored")
            return
        }
        val key = sbn.key
        scope.launch {
            val sent = Pager.link?.alert() == true
            if (!sent) Pager.engine.beepFailed() // the watch never got it: do not let the throttle swallow a retry
            PagerLog.i("alert app=${event.app} sent=$sent")
            Pager.recordDecision(event.app, if (sent) "beep" else "no-link")
            if (sent) Pager.lastAlert.value = LastAlert(System.currentTimeMillis(), event.app)
            if (sent && event.isCall) keepChirpingWhileRinging(key, decision)
        }
    }

    /** Repeat the alert until the call stops being an incoming call (answered, declined, missed), the link goes, or the cap is hit. */
    private fun keepChirpingWhileRinging(key: String, beep: Decision.Beep) {
        if (!ringing.add(key)) return
        scope.launch {
            try {
                RingRepeater(Pager.settings.value.callRepeatMs, MAX_REPEATS).run(
                    ringing = { stillIncoming(key) },
                    beep = { Pager.link?.alert() == true },
                )
            } finally {
                ringing.remove(key)
            }
        }
    }

    private fun stillIncoming(key: String): Boolean = runCatching {
        activeNotifications.firstOrNull { it.key == key }
            ?.let { NotificationMapper.map(RawNotificationFactory.from(it))?.isCall == true } == true
    }.getOrDefault(false)

    private companion object {
        const val MAX_REPEATS = 6
    }
}
