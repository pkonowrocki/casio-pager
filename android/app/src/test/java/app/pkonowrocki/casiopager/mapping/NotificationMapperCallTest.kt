package app.pkonowrocki.casiopager.mapping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private fun call(pkg: String, callType: Int?) = RawNotification(
    pkg, "k", "call", callType, false, "Anna", false, false, null, null, 1,
)

class NotificationMapperCallTest {
    @Test fun callCategoryFromANonDialerAppIsIgnored() {
        assertNull(NotificationMapper.map(call("com.whatsapp", null)))
        assertNull(NotificationMapper.map(call("com.example.voip", 1)))
    }

    @Test fun incomingCallFromThePhoneAppStillWorks() =
        assertEquals("call", NotificationMapper.map(call("com.google.android.dialer", 1))!!.app)
}
