package dev.nezhar.lookabout

import dev.nezhar.lookabout.walk.Outcome
import dev.nezhar.lookabout.walk.QUESTIONS_PER_PICTURE
import dev.nezhar.lookabout.walk.Shot
import dev.nezhar.lookabout.walk.Walk

/**
 * The screens Walk repeats for every picture; CheckPaused only after a failed check, PhotoTaken
 * only when the photos are checked at the end of the walk.
 */
enum class WalkStep {
    NextPicture,
    Checking,
    Accepted,
    NotYet,
    CheckPaused,
    PhotoTaken,
}

/**
 * The screen Walk shows for [shot]: Checking while a check runs, a verdict once the photo is
 * checked, Photo taken for an unchecked photo when photos are checked at the end ([checkAtEnd]),
 * Check paused when the check of its photo failed ([paused]), and Next picture otherwise (no
 * photo yet, or a photo whose check is not in).
 */
fun walkStep(shot: Shot, checking: Boolean, paused: Boolean = false, checkAtEnd: Boolean = false): WalkStep = when {
    checking -> WalkStep.Checking
    shot.isChecked && shot.outcome == Outcome.ACCEPTED -> WalkStep.Accepted
    shot.isChecked -> WalkStep.NotYet
    checkAtEnd && shot.hasPhoto -> WalkStep.PhotoTaken
    paused && shot.hasPhoto -> WalkStep.CheckPaused
    else -> WalkStep.NextPicture
}

/**
 * True when [walk] is due for its check at the end: photos are checked at the end, the walker has
 * moved past the last picture (Finish walk, or Skip on the last) and Summary is not open yet. The
 * photos still to check are [Walk.shotsToCheck]; a photo whose check failed stays among them.
 */
fun checksAtEnd(walk: Walk, onSummary: Boolean): Boolean =
    walk.checkAtEnd && walk.current == null && !onSummary

/**
 * The end check's position for the photo of shot [shotIndex] among the walk's photos, such as
 * "Checking 2 of 5" for the second of five photos. Shots without a photo are not counted.
 */
fun endCheckText(walk: Walk, shotIndex: Int): String {
    val withPhoto = walk.shots.indices.filter { walk.shots[it].hasPhoto }
    val position = withPhoto.indexOf(shotIndex).coerceAtLeast(0)
    return "Checking ${positionText(position, withPhoto.size)}"
}

/** How far the end check is, 0 to 1: the photos before [shotIndex] out of all of the walk's photos. */
fun endCheckProgress(walk: Walk, shotIndex: Int): Float {
    val withPhoto = walk.shots.indices.filter { walk.shots[it].hasPhoto }
    if (withPhoto.isEmpty()) return 0f
    return withPhoto.count { it < shotIndex }.toFloat() / withPhoto.size
}

/**
 * True when the end check may start checking: the model is loaded ([modelReady]) and [walk] has
 * a photo without a result. Until the model is ready the end check waits instead of failing
 * every photo.
 */
fun canStartEndCheck(walk: Walk, modelReady: Boolean): Boolean =
    modelReady && walk.shotsToCheck.isNotEmpty()

/**
 * True when Summary offers Check again: a photo of [walk] has no result (its check failed or never
 * ran) and no end check is running ([endCheckRunning]).
 */
fun offersCheckAgain(walk: Walk, endCheckRunning: Boolean): Boolean =
    !endCheckRunning && walk.shotsToCheck.isNotEmpty()

/** The end check's heading while it waits for the model. */
const val MODEL_WAIT_TEXT = "Getting the model ready"

/**
 * The end check's heading: [MODEL_WAIT_TEXT] while it waits for the model ([waiting]), else its
 * position such as "Checking 2 of 5" (see [endCheckText]).
 */
fun endCheckTitle(walk: Walk, shotIndex: Int, waiting: Boolean): String =
    if (waiting) MODEL_WAIT_TEXT else endCheckText(walk, shotIndex)

/**
 * The line under the heading while the end check waits for a model that is not on the phone or
 * failed to load ([modelUnavailable]); null while the model loads or downloads.
 */
fun modelWaitNote(modelUnavailable: Boolean): String? =
    if (modelUnavailable) "The model is not on the phone or failed to load. The check starts as soon as it is ready." else null

/**
 * True when the waiting end check should load the model again: the model failed to load
 * ([modelFailed]) and this end check has not retried yet ([alreadyRetried]). It retries once only,
 * so a model that keeps failing stays Failed.
 */
fun retriesModelLoad(modelFailed: Boolean, alreadyRetried: Boolean): Boolean =
    modelFailed && !alreadyRetried

/**
 * True when the waiting end check offers Show results now: it waits for the model ([waiting]) and
 * the model is not on the phone or failed to load ([modelUnavailable]), so it may never be ready.
 */
fun offersShowResultsNow(waiting: Boolean, modelUnavailable: Boolean): Boolean =
    waiting && modelUnavailable

/** A failed check: the shot at [shotIndex] and the photo the check was made for. */
data class PausedCheck(val shotIndex: Int, val photoPath: String) {
    /** True when [shot], at [index], still holds the photo whose check failed and has no result. */
    fun appliesTo(index: Int, shot: Shot?): Boolean =
        shot != null && index == shotIndex && shot.photoPath == photoPath && !shot.isChecked
}

/** The answers to record from [result], or null when the check failed and nothing is recorded. */
fun answersToRecord(result: CheckResult): List<Boolean>? =
    result.answers.takeIf { result.failure == null && it.size == QUESTIONS_PER_PICTURE }

/** The walker's position as shown on screen: index 1 of 5 shots is "2 of 5". */
fun positionText(index: Int, total: Int): String = "${index + 1} of $total"

/**
 * Tallest the photo on Accepted and Not yet gets, in dp, on a screen whose content is
 * [contentHeight] dp high (between the top and bottom padding, inside the system bars). Up to the
 * 728 dp of 360 x 800 with a 24 dp status bar and a 24 dp gesture bar it is the deck's 144; a
 * taller screen gives half of its extra height to the photo, so 412 x 915 (843 dp of content)
 * reaches about 200, as in the deck. The photo still gives up height when the criteria need it.
 */
fun verdictPhotoMaxHeight(contentHeight: Float): Float =
    COMPACT_VERDICT_PHOTO + ((contentHeight - COMPACT_CONTENT_HEIGHT) / 2).coerceAtLeast(0f)

/** The verdict photo's height at 360 x 800 dp (deck page 7). */
const val COMPACT_VERDICT_PHOTO = 144f

/** The Walk content height at 360 x 800 dp with 24 dp bars and the 8 + 16 dp screen padding. */
const val COMPACT_CONTENT_HEIGHT = 728f

/** The first of [sizes] (largest first) for which [fits] is true, or the last when none fits. */
fun <T> largestThatFits(sizes: List<T>, fits: (T) -> Boolean): T {
    require(sizes.isNotEmpty()) { "No sizes to choose from" }
    return sizes.firstOrNull(fits) ?: sizes.last()
}
