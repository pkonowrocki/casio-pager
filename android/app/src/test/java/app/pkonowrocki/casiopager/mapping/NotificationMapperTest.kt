package app.pkonowrocki.casiopager.mapping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private fun raw(
    pkg: String = "com.whatsapp", key: String = "0|com.whatsapp|1|null|10", category: String? = "msg",
    callType: Int? = null, summary: Boolean = false, title: String? = "Anna", messaging: Boolean = true,
    group: Boolean = false, sender: String? = "Anna", msgTime: Long? = 1000, post: Long = 2000, convTitle: String? = null,
) = RawNotification(pkg, key, category, callType, summary, title, messaging, group, sender, msgTime, post, conversationTitle = convTitle)

class NotificationMapperTest {
    @Test fun oneToOneWhatsApp() {
        val e = NotificationMapper.map(raw())!!
        assertEquals("whatsapp", e.app)
        assertEquals("Anna", e.sender)
        assertEquals(1000L, e.timestampMs)
        assertEquals(false, e.isGroup)
        assertEquals(false, e.isCall)
    }

    @Test fun groupUsesPerMessageSenderNotGroupTitle() {
        val e = NotificationMapper.map(raw(title = "Family chat", group = true, sender = "Anna"))!!
        assertEquals("Anna", e.sender)
        assertEquals(true, e.isGroup)
    }

    @Test fun aGroupCarriesItsTitleAndANormalChatDoesNot() {
        assertEquals("Family", NotificationMapper.map(raw(title = "x", group = true, convTitle = "Family"))!!.groupTitle)
        assertEquals("Family chat", NotificationMapper.map(raw(title = "Family chat", group = true))!!.groupTitle)
        assertNull(NotificationMapper.map(raw(group = false, convTitle = "Family"))!!.groupTitle)
    }

    @Test fun groupWithoutKnownSenderIsDropped() =
        assertNull(NotificationMapper.map(raw(group = true, sender = null)))

    @Test fun groupSummaryIsDropped() = assertNull(NotificationMapper.map(raw(summary = true, messaging = false)))

    @Test fun smsAndMessengerPackages() {
        assertEquals("sms", NotificationMapper.map(raw(pkg = "com.google.android.apps.messaging"))!!.app)
        assertEquals("messenger", NotificationMapper.map(raw(pkg = "com.facebook.orca"))!!.app)
    }

    @Test fun anyOtherAppIsKeptUnderItsPackageName() {
        val e = NotificationMapper.map(raw(pkg = "org.signal", category = "msg", messaging = false, title = "Bob", sender = null))!!
        assertEquals("org.signal", e.app)
        assertEquals("Bob", e.sender)
    }

    @Test fun telegramHasItsOwnName() = assertEquals("telegram", NotificationMapper.map(raw(pkg = "org.telegram.messenger"))!!.app)

    @Test fun ongoingNotificationFromAnotherAppIsDropped() =
        assertNull(NotificationMapper.map(raw(pkg = "com.spotify.music", messaging = false, title = "Song").copy(isOngoing = true)))

    @Test fun groupSummaryFromAnyAppIsStillDropped() =
        assertNull(NotificationMapper.map(raw(pkg = "org.signal", summary = true, messaging = false)))

    @Test fun nonMessagingStyleFallsBackToTitleAndPostTime() {
        val e = NotificationMapper.map(raw(pkg = "com.facebook.orca", messaging = false, sender = null, msgTime = null, post = 777))!!
        assertEquals("Anna", e.sender)
        assertEquals(777L, e.timestampMs)
    }

    @Test fun incomingCallBeepsWithCallerName() {
        val e = NotificationMapper.map(raw(pkg = "com.google.android.dialer", category = "call", callType = 1, title = "Anna", messaging = false, sender = null))!!
        assertEquals("call", e.app)
        assertEquals("Anna", e.sender)
        assertEquals(true, e.isCall)
    }

    @Test fun answeredCallDoesNotBeep() =
        assertNull(NotificationMapper.map(raw(pkg = "com.google.android.dialer", category = "call", callType = 2, messaging = false)))

    @Test fun callWithoutTypeIsTreatedAsIncoming() {
        val e = NotificationMapper.map(raw(pkg = "com.google.android.dialer", category = "call", callType = null, messaging = false))
        assertEquals("call", e!!.app)
    }

    @Test fun blankSenderIsDropped() = assertNull(NotificationMapper.map(raw(title = " ", sender = " ")))
}
