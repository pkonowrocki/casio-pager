package app.pkonowrocki.casiopager.log

import android.util.Log
import java.io.File

/** Never pass message text or contact names here. Lines go to logcat, a rolling file, and the Loki buffer. */
object PagerLog {
    private const val TAG = "CasioPager"
    private val buffer = ArrayDeque<Pair<Long, String>>() // ponytail: in-memory, drops oldest past 500
    @Volatile private var file: RollingLog? = null

    fun attach(dir: File) {
        if (file == null) file = RollingLog(dir)
    }

    fun tail(maxLines: Int): List<String> = file?.tail(maxLines) ?: emptyList()

    fun clearFile() { file?.clear() }

    @Synchronized fun i(msg: String) {
        Log.i(TAG, msg)
        runCatching { file?.append(msg) } // a full disk must never break the app
        buffer.addLast(System.currentTimeMillis() to msg)
        while (buffer.size > 500) buffer.removeFirst()
    }

    @Synchronized fun drain(): List<Pair<Long, String>> = buffer.toList().also { buffer.clear() }

    @Synchronized fun putBack(lines: List<Pair<Long, String>>) {
        lines.asReversed().forEach { buffer.addFirst(it) }
        while (buffer.size > 500) buffer.removeFirst()
    }
}
