package app.pkonowrocki.casiopager.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupChecklistTest {
    @Test fun listsTheStepsInTheOrderYouDoThem() {
        val items = SetupChecklist.items(paired = false, permissions = false, listener = false, battery = false)
        assertEquals(listOf(SetupStep.PAIR, SetupStep.PERMISSIONS, SetupStep.LISTENER, SetupStep.BATTERY), items.map { it.step })
        assertFalse(SetupChecklist.complete(items))
    }

    @Test fun marksOnlyTheFinishedStepsDone() {
        val items = SetupChecklist.items(paired = true, permissions = true, listener = false, battery = true)
        assertEquals(listOf(true, true, false, true), items.map { it.done })
        assertFalse(SetupChecklist.complete(items))
    }

    @Test fun completeOnlyWhenEverythingIsDone() =
        assertTrue(SetupChecklist.complete(SetupChecklist.items(true, true, true, true)))
}
