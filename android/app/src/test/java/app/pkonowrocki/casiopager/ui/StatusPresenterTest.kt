package app.pkonowrocki.casiopager.ui

import app.pkonowrocki.casiopager.R
import app.pkonowrocki.casiopager.link.LinkState
import org.junit.Assert.assertEquals
import org.junit.Test

class StatusPresenterTest {
    @Test fun aHeldLongPressLinkIsGoodAndSaysSince() {
        val v = StatusPresenter.present(LinkState.Held(1), since = "22:00")
        assertEquals(StatusLevel.GOOD, v.level)
        assertEquals(R.string.status_linked_title, v.title)
        assertEquals(R.string.status_linked_hint_since, v.hint)
        assertEquals(listOf<Any>("22:00"), v.hintArgs)
    }

    @Test fun aHeldLinkWithUnknownKindIsStillGood() {
        val v = StatusPresenter.present(LinkState.Held(null), since = null)
        assertEquals(StatusLevel.GOOD, v.level)
        assertEquals(R.string.status_linked_hint, v.hint)
    }

    @Test fun aShortButtonSessionIsNotShownAsLinked() {
        val v = StatusPresenter.present(LinkState.Held(4), since = "22:00")
        assertEquals(StatusLevel.WAITING, v.level)
        assertEquals(R.string.status_short_title, v.title)
        assertEquals(listOf<Any>(4), v.hintArgs)
    }

    @Test fun everyOtherStateHasItsOwnLevelAndTitle() {
        fun p(s: LinkState) = StatusPresenter.present(s, null)
        assertEquals(StatusLevel.WAITING, p(LinkState.Connecting).level)
        assertEquals(R.string.status_waiting_title, p(LinkState.Connecting).title)
        assertEquals(StatusLevel.WAITING, p(LinkState.Initialising).level)
        assertEquals(StatusLevel.BAD, p(LinkState.Lost).level)
        assertEquals(R.string.status_lost_title, p(LinkState.Lost).title)
        assertEquals(StatusLevel.IDLE, p(LinkState.NotPaired).level)
        assertEquals(R.string.status_unpaired_title, p(LinkState.NotPaired).title)
        assertEquals(StatusLevel.IDLE, p(LinkState.Idle).level)
        assertEquals(R.string.status_idle_title, p(LinkState.Idle).title)
    }
}
