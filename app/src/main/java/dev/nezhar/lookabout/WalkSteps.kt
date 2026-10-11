package dev.nezhar.lookabout

import dev.nezhar.lookabout.walk.Outcome
import dev.nezhar.lookabout.walk.Shot

/** The four screens Walk repeats for every picture. */
enum class WalkStep {
    NextPicture,
    Checking,
    Accepted,
    NotYet,
}

/**
 * The screen Walk shows for [shot]: Checking while a check runs, a verdict once the photo is
 * checked, and Next picture before that (no photo yet, or a photo whose check is not in).
 */
fun walkStep(shot: Shot, checking: Boolean): WalkStep = when {
    checking -> WalkStep.Checking
    !shot.isChecked -> WalkStep.NextPicture
    shot.outcome == Outcome.ACCEPTED -> WalkStep.Accepted
    else -> WalkStep.NotYet
}

/** The walker's position as shown on screen: index 1 of 5 shots is "2 of 5". */
fun positionText(index: Int, total: Int): String = "${index + 1} of $total"

/** The first of [sizes] (largest first) for which [fits] is true, or the last when none fits. */
fun <T> largestThatFits(sizes: List<T>, fits: (T) -> Boolean): T {
    require(sizes.isNotEmpty()) { "No sizes to choose from" }
    return sizes.firstOrNull(fits) ?: sizes.last()
}

/** How long the stand-in check waits, in milliseconds. */
const val STAND_IN_CHECK_MILLIS = 2_000L

/**
 * The stand-in check's answers until the camera and the model are wired in (PW-14): the sample
 * walk's 3 of 4 YES for the first picture, 1 of 4 for the second, 4 of 4 for the third and 1 of 4
 * for the fourth, then the same again from the fifth picture on.
 */
fun standInAnswers(shotIndex: Int): List<Boolean> = when (shotIndex.mod(4)) {
    0 -> listOf(true, true, false, true)
    1 -> listOf(false, true, false, false)
    2 -> listOf(true, true, true, true)
    else -> listOf(true, false, false, false)
}
