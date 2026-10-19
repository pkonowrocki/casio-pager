package app.pkonowrocki.casiopager.ui

import java.time.DayOfWeek
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class MoreAppsTest {
    private val labels = mapOf("com.b" to "banana", "com.a" to "Zebra", "com.c" to "Cherry", "com.d" to "apple")

    @Test fun otherAppsAreSortedByTheNameShownNotThePackage() =
        assertEquals(listOf("com.d", "com.b", "com.c", "com.a"), otherApps(labels.keys, label = { labels.getValue(it) }))

    @Test fun appsWithASettingComeFirstThenTheRest() =
        assertEquals(
            listOf("com.c", "com.a", "com.d", "com.b"),
            otherApps(labels.keys, { labels.getValue(it) }, { it == "com.a" || it == "com.c" }),
        )

    @Test fun aWellKnownMessengerIsNotListedUnderOtherApps() =
        assertEquals(listOf("com.spotify.music"), otherApps(listOf("org.thoughtcrime.securesms", "com.spotify.music", "com.google.android.gm")))

    @Test fun moreMessengersShowsInstalledOrMentionedOnesSortedByName() {
        val label = { p: String -> MORE_MESSENGERS.getValue(p) }
        val installed = setOf("com.discord", "com.viber.voip")
        assertEquals(
            listOf("Discord", "Signal", "Viber").map { n -> MORE_MESSENGERS.entries.first { it.value == n }.key },
            moreMessengers(listOf("org.thoughtcrime.securesms"), { it in installed }, label),
        )
        assertEquals(emptyList<String>(), moreMessengers(emptyList(), { false }, label))
    }

    @Test fun rulesSavedUnderAPackageNameStillBelongToThatApp() {
        // keys are package names, so a rule saved before the section existed is found by the same key, not orphaned
        assertEquals("Signal", MORE_MESSENGERS["org.thoughtcrime.securesms"])
        assertEquals(listOf("org.thoughtcrime.securesms"), moreMessengers(listOf("org.thoughtcrime.securesms"), { false }) { MORE_MESSENGERS.getValue(it) })
    }

    @Test fun dayChipsUseUpToThreeLettersWithoutADot() {
        assertEquals("Mon", dayLabel(DayOfWeek.MONDAY, Locale.ENGLISH))
        assertEquals("pon", dayLabel(DayOfWeek.MONDAY, Locale("pl")))
    }
}
