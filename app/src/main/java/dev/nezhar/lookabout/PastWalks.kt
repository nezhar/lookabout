package dev.nezhar.lookabout

import dev.nezhar.lookabout.walk.ArchivedWalkSummary
import dev.nezhar.lookabout.walk.Walk
import dev.nezhar.lookabout.walk.newestFirst
import java.io.File
import java.time.ZoneId
import java.util.Locale

/** What Past walks says when the archive holds no readable walk. */
const val NO_WALKS_TEXT = "No walks yet."

/** The confirm line after a long press on a past walk. */
const val DELETE_WALK_TEXT = "Delete this walk? Its photos stay in the gallery."

/**
 * One row on Past walks: the [date] the walk started, its [topic], the total such as "3 of 5
 * accepted" and the photo of the first accepted shot, or null for the No photo tile.
 */
data class PastWalkRow(
    val id: Long,
    val date: String,
    val topic: String,
    val acceptedText: String,
    val thumbnailPath: String?,
)

/** The row of one archived walk, its date in the given zone and locale. */
fun pastWalkRow(
    summary: ArchivedWalkSummary,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): PastWalkRow = PastWalkRow(
    id = summary.id,
    date = walkDateText(summary.startedAtMillis, zone, locale),
    topic = summary.topic,
    acceptedText = acceptedTotalText(summary.acceptedCount, summary.pictureCount),
    thumbnailPath = summary.thumbnailPath,
)

/** The rows of Past walks, newest walk first. */
fun pastWalkRows(
    summaries: List<ArchivedWalkSummary>,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): List<PastWalkRow> = newestFirst(summaries).map { pastWalkRow(it, zone, locale) }

/** Setup offers Past walks once the archive is known to hold at least one walk. */
fun offersPastWalks(pastWalks: List<ArchivedWalkSummary>?): Boolean = !pastWalks.isNullOrEmpty()

/** True when [path] lies directly in [photoDir], the app's Pictures folder: one of the app's own photos. */
fun isAppPhoto(path: String, photoDir: String): Boolean =
    File(path).absoluteFile.normalize().parentFile == File(photoDir).absoluteFile.normalize()

/**
 * The photo files of [walk] that are the app's own copies: every shot's photo that lies directly
 * in [photoDir], the app's Pictures folder, each once. Anything elsewhere (such as a gallery copy)
 * is never listed, so deleting a walk cannot touch it.
 */
fun appPhotoPaths(walk: Walk, photoDir: String): List<String> =
    walk.shots
        .mapNotNull { it.photoPath }
        .filter { isAppPhoto(it, photoDir) }
        .distinct()

/** Every photo [walk] references: its shots' photos and the one the camera is about to write. */
fun walkPhotoPaths(walk: Walk): Set<String> =
    walk.shots.mapNotNull { it.photoPath }.toSet() + listOfNotNull(walk.pendingPhotoPath)

/**
 * The photos that [before] referenced and [after], the same walk one step later, no longer does:
 * the photo Retake replaced, or the unchecked photo dropped when its shot was skipped. A photo
 * still on any shot of [after], or about to be written by the camera ([Walk.pendingPhotoPath]),
 * is never listed. Each path once.
 */
fun droppedPhotos(before: Walk, after: Walk): List<String> {
    val kept = walkPhotoPaths(after)
    return before.shots.mapNotNull { it.photoPath }.distinct().filter { it !in kept }
}

/**
 * Of the [dropped] photos, those that may be deleted once the walk file has been written: none
 * when that write failed ([written] false) or the walk file cannot be read ([savedWalkPhotos]
 * null), else those the saved walk does not reference ([savedWalkPhotos], empty when no walk is
 * saved).
 */
fun photosSafeToDelete(dropped: List<String>, written: Boolean, savedWalkPhotos: Set<String>?): List<String> =
    if (!written || savedWalkPhotos == null) emptyList() else dropped.filter { it !in savedWalkPhotos }
