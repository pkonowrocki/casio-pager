package app.pkonowrocki.casiopager.mapping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private fun plain(pkg: String, category: String?, title: String?) = RawNotification(
    pkg, "k", category, null, false, title, false, false, null, null, 1,
)

class NotificationMapperNoiseTest {
    @Test fun systemNotificationsFromAMessagingAppAreNotPeopleWritingToYou() {
        assertNull(NotificationMapper.map(plain("com.whatsapp", "service", "WhatsApp")))
        assertNull(NotificationMapper.map(plain("com.whatsapp", null, "Checking for new messages")))
        assertNull(NotificationMapper.map(plain("com.facebook.orca", "status", "Messenger")))
    }

    @Test fun aPlainNotificationMarkedAsAMessageStillCounts() =
        assertEquals("Anna", NotificationMapper.map(plain("com.facebook.orca", "msg", "Anna"))!!.sender)
}
