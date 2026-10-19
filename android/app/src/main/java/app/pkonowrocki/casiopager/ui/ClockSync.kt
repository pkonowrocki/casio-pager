package app.pkonowrocki.casiopager.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pkonowrocki.casiopager.Pager
import app.pkonowrocki.casiopager.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

enum class SyncDay { TODAY, YESTERDAY, EARLIER }

/** Which day the sync fell on, relative to [today]. Pure, so it is unit-tested. */
fun syncDay(sync: LocalDate, today: LocalDate): SyncDay = when (sync) {
    today -> SyncDay.TODAY
    today.minusDays(1) -> SyncDay.YESTERDAY
    else -> SyncDay.EARLIER
}

private val TIME = DateTimeFormatter.ofPattern("HH:mm")
private val DATE = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

/** The app writes the watch's time in every session, including the four automatic windows, so this shows it is still working. */
@Composable
fun ClockSyncLine() {
    val ms by Pager.lastClockSyncMs.collectAsStateWithLifecycle()
    val zone = ZoneId.systemDefault()
    val text = ms?.let {
        val at = Instant.ofEpochMilli(it).atZone(zone)
        val time = TIME.format(at)
        when (syncDay(at.toLocalDate(), LocalDate.now(zone))) {
            SyncDay.TODAY -> stringResource(R.string.clock_sync_today, time)
            SyncDay.YESTERDAY -> stringResource(R.string.clock_sync_yesterday, time)
            SyncDay.EARLIER -> stringResource(R.string.clock_sync_date, DATE.format(at), time)
        }
    } ?: stringResource(R.string.clock_sync_never)
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
