package dev.nezhar.lookabout

import android.util.Log
import dev.nezhar.lookabout.walk.SavedWalk
import dev.nezhar.lookabout.walk.decodeSavedWalk
import dev.nezhar.lookabout.walk.encodeSavedWalk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

/** The saved walk's file name in `filesDir`. */
const val WALK_FILE_NAME = "walk.json"

/**
 * The walk saved in [file], so it survives Android closing the app while the camera is open.
 *
 * [save] writes in the background, one write at a time, and the latest call wins: a write that
 * is overtaken by a newer one before it starts is dropped. Each write goes to a temporary file
 * that is then renamed over [file], so a reader never sees half a walk.
 */
class WalkStore(private val file: File) {
    private val tempFile = File(file.path + ".tmp")

    // Not tied to a screen or ViewModel, so a write queued just before the app closes still runs.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    private val latest = AtomicLong()

    /** The saved walk, or null when there is none or the file cannot be read. Blocks: the file is small. */
    fun load(): SavedWalk? =
        try {
            if (file.isFile) decodeSavedWalk(file.readText(Charsets.UTF_8)) else null
        } catch (e: Exception) {
            Log.w(Gemma.TAG, "Could not read the saved walk", e)
            null
        }

    /** Saves [saved] in the background, or deletes the saved walk when it is null. */
    fun save(saved: SavedWalk?) {
        val text = saved?.let(::encodeSavedWalk)
        val write = latest.incrementAndGet()
        scope.launch {
            lock.withLock {
                if (latest.get() != write) return@withLock
                try {
                    if (text == null) clear() else writeAtomically(text)
                } catch (e: IOException) {
                    Log.w(Gemma.TAG, "Could not save the walk", e)
                }
            }
        }
    }

    private fun writeAtomically(text: String) {
        FileOutputStream(tempFile).use { out ->
            out.write(text.toByteArray(Charsets.UTF_8))
            out.fd.sync()
        }
        if (!tempFile.renameTo(file)) throw IOException("Could not rename $tempFile to $file")
    }

    private fun clear() {
        tempFile.delete()
        if (file.exists() && !file.delete()) throw IOException("Could not delete $file")
    }
}
