package app.pkonowrocki.casiopager.mapping

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KnownPackagesTest {
    @Test fun knowsTheMessagingAndDialerPackagesButNotOthers() {
        listOf("com.whatsapp", "com.facebook.orca", "com.google.android.apps.messaging", "com.google.android.dialer", "org.telegram.messenger")
            .forEach { assertTrue(it, NotificationMapper.isKnownPackage(it)) }
        assertFalse(NotificationMapper.isKnownPackage("com.spotify.music"))
    }
}
