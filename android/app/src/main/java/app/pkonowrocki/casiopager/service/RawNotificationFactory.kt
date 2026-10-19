package app.pkonowrocki.casiopager.service

import android.app.Notification
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import app.pkonowrocki.casiopager.mapping.RawNotification

object RawNotificationFactory {
    fun from(sbn: StatusBarNotification): RawNotification {
        val n = sbn.notification
        val extras = n.extras
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n)
        val last = style?.messages?.lastOrNull()
        return RawNotification(
            pkg = sbn.packageName,
            key = sbn.key,
            category = n.category,
            callType = if (extras.containsKey("android.callType")) extras.getInt("android.callType") else null,
            isGroupSummary = n.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            isMessagingStyle = style != null,
            isGroupConversation = style?.isGroupConversation ?: false,
            lastMessageSender = last?.person?.name?.toString(),
            lastMessageTimeMs = last?.timestamp,
            postTimeMs = sbn.postTime,
            isOngoing = n.flags and Notification.FLAG_ONGOING_EVENT != 0,
            conversationTitle = style?.conversationTitle?.toString(),
        )
    }
}
