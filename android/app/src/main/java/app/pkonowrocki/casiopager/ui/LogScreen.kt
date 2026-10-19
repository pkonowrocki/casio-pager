package app.pkonowrocki.casiopager.ui

import app.pkonowrocki.casiopager.R
import androidx.compose.ui.res.stringResource
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pkonowrocki.casiopager.log.PagerLog
import kotlinx.coroutines.delay

/** The last day or so of what the app did, without names or message text. Share it to send it somewhere. */
@Composable
fun LogScreen() {
    val ctx = LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    val lines by produceState(emptyList<String>(), refresh) {
        while (true) {
            value = PagerLog.tail(400)
            delay(2_000)
        }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(lines.size) { if (lines.isNotEmpty()) listState.scrollToItem(lines.size - 1) }

    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, ctx.getString(R.string.log_subject))
                    .putExtra(Intent.EXTRA_TEXT, PagerLog.tail(2_000).joinToString("\n"))
                ctx.startActivity(Intent.createChooser(send, ctx.getString(R.string.log_chooser)))
            }) { Text(stringResource(R.string.log_share)) }
            OutlinedButton(onClick = { PagerLog.clearFile(); refresh++ }) { Text(stringResource(R.string.log_clear)) }
            Text(stringResource(R.string.log_lines, lines.size), Modifier.padding(top = 10.dp))
        }
        LazyColumn(state = listState) {
            items(lines) { Text(it, fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 13.sp) }
        }
    }
}
