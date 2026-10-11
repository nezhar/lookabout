package dev.nezhar.lookabout

import android.util.Log
import dev.nezhar.lookabout.walk.ArchivedWalkSummary
import dev.nezhar.lookabout.walk.Walk
import dev.nezhar.lookabout.walk.WalkArchive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The [WalkArchive] in [dir] for the app: [save] queues a write in the background, the other calls
 * are suspend functions that run on [Dispatchers.IO], so all are safe to call from the main thread.
 */
class ArchivedWalks(dir: File) {
    private val archive = WalkArchive(dir)

    // Not tied to a screen or ViewModel, so a save queued just before the app closes still runs.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // One writer in call order, so the latest results of a walk are the ones that stay.
    private val queue = Channel<Walk>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (walk in queue) {
                try {
                    archive.save(walk)
                } catch (e: Exception) {
                    Log.w(Gemma.TAG, "Could not archive the walk", e)
                }
            }
        }
    }

    /** Saves [walk] to the archive in the background, overwriting its earlier save. A failure is only logged. */
    fun save(walk: Walk) {
        queue.trySend(walk)
    }

    /** Every readable archived walk, newest first. */
    suspend fun list(): List<ArchivedWalkSummary> = withContext(Dispatchers.IO) { archive.list() }

    /** The archived walk with [id], or null when it is missing or unreadable. */
    suspend fun load(id: Long): Walk? = withContext(Dispatchers.IO) { archive.load(id) }

    /** Deletes the archived walk with [id], keeping its photos. True when it is gone. */
    suspend fun delete(id: Long): Boolean = withContext(Dispatchers.IO) { archive.delete(id) }
}
