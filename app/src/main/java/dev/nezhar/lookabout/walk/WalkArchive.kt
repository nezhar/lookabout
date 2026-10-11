package dev.nezhar.lookabout.walk

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/** The archive's folder name in `filesDir`. */
const val WALK_ARCHIVE_DIR = "walks"

/**
 * What a list of past walks needs about one archived walk. [id] is the walk's start time and
 * names its file. [agreedCount] of [checkCount] checks kept Gemma's answer. [thumbnailPath] is the
 * photo of the first accepted shot, or null when no shot was accepted.
 */
data class ArchivedWalkSummary(
    val id: Long,
    val startedAtMillis: Long,
    val topic: String,
    val sky: String,
    val acceptedCount: Int,
    val pictureCount: Int,
    val agreedCount: Int,
    val checkCount: Int,
    val thumbnailPath: String?,
)

/** The archive id of [walk]: its start time, so saving the same walk again overwrites its file. */
fun archiveId(walk: Walk): Long = walk.startedAtMillis

/** The archive file name of the walk with [id], e.g. `walk-1760000000123.json`. */
fun archiveFileName(id: Long): String = "walk-$id.json"

private val archiveFilePattern = Regex("""walk-(\d+)\.json""")

/** The id in an archive file name, or null when [name] is not one (a temp file, for example). */
fun archiveIdOf(name: String): Long? =
    archiveFilePattern.matchEntire(name)?.groupValues?.get(1)?.toLongOrNull()

/** The summary of [walk], archived under [id]. */
fun archivedWalkSummary(id: Long, walk: Walk): ArchivedWalkSummary =
    ArchivedWalkSummary(
        id = id,
        startedAtMillis = walk.startedAtMillis,
        topic = walk.topic,
        sky = walk.sky,
        acceptedCount = walk.acceptedCount,
        pictureCount = walk.shots.size,
        agreedCount = walk.agreedCount,
        checkCount = walk.checkCount,
        thumbnailPath = walk.shots.firstOrNull { it.outcome == Outcome.ACCEPTED }?.photoPath,
    )

/** Newest walk first; walks started at the same time by id, highest first. */
fun newestFirst(summaries: List<ArchivedWalkSummary>): List<ArchivedWalkSummary> =
    summaries.sortedWith(compareByDescending<ArchivedWalkSummary> { it.startedAtMillis }.thenByDescending { it.id })

/**
 * Every finished walk with its results, one JSON file per walk in [dir], written with the same
 * codec as the current walk ([encodeSavedWalk]). The photos stay where they are; an archived walk
 * keeps their paths.
 *
 * Plain blocking file work: call it off the main thread. Calls are serialised, so a save never
 * races a delete of the same walk. Unreadable files are skipped and never throw.
 */
class WalkArchive(private val dir: File) {

    /**
     * Saves [walk] under [archiveId], overwriting an earlier save of the same walk. Writes a temp
     * file and renames it over the old one, so a reader never sees half a walk. Throws [IOException]
     * when the file cannot be written.
     */
    @Synchronized
    fun save(walk: Walk) {
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Could not create $dir")
        val id = archiveId(walk)
        val file = File(dir, archiveFileName(id))
        val temp = File(dir, archiveFileName(id) + ".tmp")
        val text = encodeSavedWalk(SavedWalk(walk.copy(pendingPhotoPath = null), onSummary = true))
        try {
            FileOutputStream(temp).use { out ->
                out.write(text.toByteArray(Charsets.UTF_8))
                out.fd.sync()
            }
            if (!temp.renameTo(file)) throw IOException("Could not rename $temp to $file")
        } catch (t: Throwable) {
            temp.delete()
            throw t
        }
    }

    /** Every readable archived walk, newest first. */
    @Synchronized
    fun list(): List<ArchivedWalkSummary> {
        val files = dir.listFiles().orEmpty()
        val summaries = files.mapNotNull { file ->
            val id = archiveIdOf(file.name) ?: return@mapNotNull null
            read(file)?.let { archivedWalkSummary(id, it) }
        }
        return newestFirst(summaries)
    }

    /** The archived walk with [id], or null when there is none or its file cannot be read. */
    @Synchronized
    fun load(id: Long): Walk? = read(File(dir, archiveFileName(id)))

    /**
     * Every photo path the archived walks reference, or null when that is not known for sure: the
     * folder cannot be listed or an archive file cannot be read.
     */
    @Synchronized
    fun photoPaths(): Set<String>? {
        if (!dir.exists()) return emptySet()
        val files = dir.listFiles() ?: return null
        val paths = HashSet<String>()
        for (file in files) {
            if (archiveIdOf(file.name) == null) continue
            val walk = read(file) ?: return null
            walk.shots.mapNotNullTo(paths) { it.photoPath }
        }
        return paths
    }

    /** Deletes the archived walk with [id]; its photos are left alone. True when it is gone. */
    @Synchronized
    fun delete(id: Long): Boolean {
        val file = File(dir, archiveFileName(id))
        return !file.exists() || file.delete()
    }

    private fun read(file: File): Walk? =
        try {
            if (file.isFile) decodeSavedWalk(file.readText(Charsets.UTF_8))?.walk else null
        } catch (e: Exception) {
            null
        }
}
