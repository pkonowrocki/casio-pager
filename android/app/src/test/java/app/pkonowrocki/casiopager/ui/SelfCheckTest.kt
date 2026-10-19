package app.pkonowrocki.casiopager.ui

import app.pkonowrocki.casiopager.R
import app.pkonowrocki.casiopager.link.LinkState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SelfCheckTest {
    private val good = CheckInput(
        link = LinkState.Held(1), paired = true, bluetoothPermission = true, notificationPermission = true,
        listenerEnabled = true, batteryIgnored = true, bluetoothOn = true, serviceRunning = true,
    )

    @Test fun linkedWithNothingWrongIsAllGood() {
        val r = SelfCheck.check(good)
        assertNull(r.problem)
        assertEquals(StatusLevel.GOOD, r.level)
        assertEquals(R.string.check_ok_hint, r.hint)
        assertNull(r.action)
    }

    @Test fun allGoodMentionsTheLastAlertWhenThereIsOne() {
        val r = SelfCheck.check(good.copy(lastAlert = "20:22"))
        assertEquals(R.string.check_ok_hint_last, r.hint)
        assertEquals(listOf<Any>("20:22"), r.hintArgs)
    }

    @Test fun eachProblemWinsOverTheOnesBelowIt() {
        val everythingWrong = CheckInput(
            LinkState.Idle, paired = false, bluetoothPermission = false, notificationPermission = false,
            listenerEnabled = false, batteryIgnored = false, bluetoothOn = false, serviceRunning = false,
        )
        val order = listOf(
            Problem.BLUETOOTH_OFF to { i: CheckInput -> i.copy(bluetoothOn = true) },
            Problem.PERMISSION to { i: CheckInput -> i.copy(bluetoothPermission = true, notificationPermission = true) },
            Problem.LISTENER to { i: CheckInput -> i.copy(listenerEnabled = true) },
            Problem.NOT_PAIRED to { i: CheckInput -> i.copy(paired = true) },
            Problem.SERVICE_STOPPED to { i: CheckInput -> i.copy(serviceRunning = true) },
            Problem.BATTERY to { i: CheckInput -> i.copy(batteryIgnored = true) },
            Problem.NO_LINK to { i: CheckInput -> i.copy(link = LinkState.Held(1)) },
        )
        var input = everythingWrong
        for ((problem, fix) in order) {
            assertEquals(problem, SelfCheck.check(input).problem)
            input = fix(input)
        }
        assertNull(SelfCheck.check(input).problem)
    }

    @Test fun bothPermissionsAreNeeded() {
        assertEquals(Problem.PERMISSION, SelfCheck.check(good.copy(notificationPermission = false)).problem)
        assertEquals(Problem.PERMISSION, SelfCheck.check(good.copy(bluetoothPermission = false)).problem)
    }

    @Test fun problemsComeWithTheActionThatFixesThem() {
        assertEquals(CheckAction.BLUETOOTH_SETTINGS, SelfCheck.check(good.copy(bluetoothOn = false)).action)
        assertEquals(CheckAction.SETUP_PERMISSIONS, SelfCheck.check(good.copy(bluetoothPermission = false)).action)
        assertEquals(CheckAction.SETUP_LISTENER, SelfCheck.check(good.copy(listenerEnabled = false)).action)
        assertEquals(CheckAction.PAIR, SelfCheck.check(good.copy(paired = false)).action)
        assertEquals(CheckAction.START, SelfCheck.check(good.copy(serviceRunning = false)).action)
        assertEquals(CheckAction.SETUP_BATTERY, SelfCheck.check(good.copy(batteryIgnored = false)).action)
    }

    @Test fun notBeingLinkedAsksForTheLongPressAndHasNoButton() {
        for (state in listOf(LinkState.Idle, LinkState.Lost, LinkState.Connecting, LinkState.Held(3), LinkState.Held(4))) {
            val r = SelfCheck.check(good.copy(link = state))
            assertEquals(state.toString(), Problem.NO_LINK, r.problem)
            assertEquals(R.string.check_nolink_title, r.title)
            assertNull(r.action)
        }
    }

    @Test fun whileTheLinkIsBeingSetUpItSaysSo() {
        assertEquals(R.string.check_linking_title, SelfCheck.check(good.copy(link = LinkState.Initialising)).title)
    }

    @Test fun aHeldLinkOfUnknownKindCountsAsLinked() {
        assertNull(SelfCheck.check(good.copy(link = LinkState.Held(null))).problem)
    }

    @Test fun blockingProblemsAreRedAndWaitingOnesAreAmber() {
        assertEquals(StatusLevel.BAD, SelfCheck.check(good.copy(listenerEnabled = false)).level)
        assertEquals(StatusLevel.WAITING, SelfCheck.check(good.copy(batteryIgnored = false)).level)
        assertEquals(StatusLevel.WAITING, SelfCheck.check(good.copy(link = LinkState.Idle)).level)
    }
}
