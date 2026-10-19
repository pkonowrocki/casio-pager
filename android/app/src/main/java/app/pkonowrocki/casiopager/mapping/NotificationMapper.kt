package app.pkonowrocki.casiopager.mapping

import app.pkonowrocki.casiopager.model.IncomingEvent

data class RawNotification(
    val pkg: String,
    val key: String,
    val category: String?,
    val callType: Int?,            // Notification.EXTRA_CALL_TYPE: 1 incoming, 2 ongoing, 3 screening
    val isGroupSummary: Boolean,
    val title: String?,
    val isMessagingStyle: Boolean,
    val isGroupConversation: Boolean,
    val lastMessageSender: String?,
    val lastMessageTimeMs: Long?,
    val postTimeMs: Long,
    val isOngoing: Boolean = false, // FLAG_ONGOING_EVENT: players, timers, downloads
    val conversationTitle: String? = null, // MessagingStyle conversation title: the group's name
)

object NotificationMapper {
    private val DIALERS = setOf(
        "com.google.android.dialer", "com.android.dialer", "com.android.incallui",
        "com.samsung.android.incallui", "com.android.server.telecom",
    )

    private val APPS = mapOf(
        "com.whatsapp" to "whatsapp",
        "com.whatsapp.w4b" to "whatsapp",
        "com.google.android.apps.messaging" to "sms",
        "com.facebook.orca" to "messenger",
        "org.telegram.messenger" to "telegram",
    )

    /**
     * True for the apps with a fixed name and their own top section in the rules; any other app is still mapped, under
     * its package name. Used to log why one of the known apps' notifications was dropped.
     */
    fun isKnownPackage(pkg: String) = pkg in APPS || pkg in DIALERS

    fun map(n: RawNotification): IncomingEvent? {
        if (n.isGroupSummary) return null

        if (n.category == "call") {
            if (n.pkg !in DIALERS) return null                      // VoIP and chat apps have no callType contract
            if (n.callType != null && n.callType != 1) return null // ongoing or screening: no beep
            val caller = n.title?.trim().orEmpty()
            if (caller.isEmpty()) return null
            return IncomingEvent("call", caller, n.key, 0, isGroup = false, isCall = true)
        }

        // Any app can create notifications. The known ones skip system noise (sync, "checking for new messages");
        // the rest are kept as long as they are not ongoing, since those are media or timers, not someone writing.
        val known = APPS[n.pkg]
        if (known != null) {
            if (!n.isMessagingStyle && n.category != "msg") return null
        } else if (n.isOngoing) return null
        val app = known ?: n.pkg
        val sender = when {
            n.isMessagingStyle && n.isGroupConversation -> n.lastMessageSender
            n.isMessagingStyle -> n.lastMessageSender ?: n.title
            else -> n.title
        }?.trim().orEmpty()
        if (sender.isEmpty()) return null
        return IncomingEvent(
            app = app,
            sender = sender,
            conversationKey = n.key,
            timestampMs = n.lastMessageTimeMs ?: n.postTimeMs,
            isGroup = n.isGroupConversation,
            isCall = false,
            groupTitle = if (n.isGroupConversation) (n.conversationTitle ?: n.title)?.trim()?.takeIf { it.isNotEmpty() } else null,
        )
    }
}
