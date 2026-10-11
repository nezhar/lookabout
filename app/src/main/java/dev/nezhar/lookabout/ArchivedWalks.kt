package dev.nezhar.lookabout

import android.util.Log
import dev.nezhar.lookabout.walk.ArchivedWalkSummary
import dev.nezhar.lookabout.walk.Walk
import dev.nezhar.lookabout.walk.WalkArchive
import dev.nezhar.lookabout.walk.archiveId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.completeWith
import kotlinx.coroutines.launch
import java.io.File

/**
 * The [WalkArchive] in a folder for the app, one per folder for the whole process ([at]): [save]
 * queues a write in the background, the other calls are suspend functions. All of them run one
 * after another on a background thread in call order, so all are safe to call from the main
 * thread, a list or delete always sees the saves queued before it, and a save queued before a
 * delete never brings the walk back.
 */
class ArchivedWalks private constructor(dir: File) {
    private val archive = WalkArchive(dir)

    // Not tied to a screen or ViewModel, so a save queued just before the app closes still runs.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // One worker in call order, so the latest results of a walk are the ones that stay.
    private val queue = Channel<() -> Unit>(Channel.UNLIMITED)

    // Ids of walks deleted since the app started; only read and written by the worker.
    private val deleted = HashSet<Long>()

    init {
        scope.launch {
            for (task in queue) {
                try {
                    task()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (t: Throwable) {
                    Log.w(Gemma.TAG, "Could not update the walk archive", t)
                }
            }
        }
    }

    /**
     * Saves [walk] to the archive in the background, overwriting its earlier save. A failure is only
     * logged. A walk deleted from the archive is not saved again.
     */
    fun save(walk: Walk) {
        queue.trySend {
            if (archiveId(walk) !in deleted) archive.save(walk)
        }
    }

    /** Every readable archived walk, newest first. */
    suspend fun list(): List<ArchivedWalkSummary> = inOrder { archive.list() }

    /** The archived walk with [id], or null when it is missing or unreadable. */
    suspend fun load(id: Long): Walk? = inOrder { archive.load(id) }

    /** Deletes the archived walk with [id], keeping its photos. True when it is gone. */
    suspend fun delete(id: Long): Boolean = inOrder { deleteArchived(id) }

    /**
     * Deletes the archived walk with [id] and then the photo files of its shots that lie in
     * [photoDir], the app's own copies ([appPhotoPaths]); copies in the gallery are not touched.
     * True when the archived walk is gone; a photo that cannot be deleted is left.
     */
    suspend fun deleteWithPhotos(id: Long, photoDir: File?): Boolean = inOrder {
        val walk = archive.load(id)
        val gone = deleteArchived(id)
        if (gone && walk != null && photoDir != null) {
            appPhotoPaths(walk, photoDir.path).forEach { File(it).delete() }
        }
        gone
    }

    /**
     * Deletes the photo files at [paths] that no archived walk references, after the saves queued
     * before it. When the archive cannot be read in full, nothing is deleted. Returns how many
     * files were deleted.
     */
    suspend fun deleteUnarchivedPhotos(paths: List<String>): Int = inOrder {
        val archived = archive.photoPaths() ?: return@inOrder 0
        paths.filter { it !in archived }.count { File(it).delete() }
    }

    private fun deleteArchived(id: Long): Boolean {
        deleted += id
        return archive.delete(id)
    }

    /** Runs [block] on the worker after everything queued before it and returns its result or throws its error. */
    private suspend fun <T> inOrder(block: () -> T): T {
        val result = CompletableDeferred<T>()
        queue.send { result.completeWith(runCatching(block)) }
        return result.await()
    }

    companion object {
        private val byDir = HashMap<String, ArchivedWalks>()

        /** The archive in [dir], the same instance every time, so two writers never race on its files. */
        @Synchronized
        fun at(dir: File): ArchivedWalks = byDir.getOrPut(dir.absolutePath) { ArchivedWalks(dir) }
    }
}
