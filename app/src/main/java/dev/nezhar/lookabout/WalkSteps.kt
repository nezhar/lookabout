package dev.nezhar.lookabout

import dev.nezhar.lookabout.walk.Outcome
import dev.nezhar.lookabout.walk.QUESTIONS_PER_PICTURE
import dev.nezhar.lookabout.walk.Shot

/** The screens Walk repeats for every picture; CheckPaused only after a failed check. */
enum class WalkStep {
    NextPicture,
    Checking,
    Accepted,
    NotYet,
    CheckPaused,
}

/**
 * The screen Walk shows for [shot]: Checking while a check runs, a verdict once the photo is
 * checked, Check paused when the check of its photo failed ([paused]), and Next picture otherwise
 * (no photo yet, or a photo whose check is not in).
 */
fun walkStep(shot: Shot, checking: Boolean, paused: Boolean = false): WalkStep = when {
    checking -> WalkStep.Checking
    shot.isChecked && shot.outcome == Outcome.ACCEPTED -> WalkStep.Accepted
    shot.isChecked -> WalkStep.NotYet
    paused && shot.hasPhoto -> WalkStep.CheckPaused
    else -> WalkStep.NextPicture
}

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

/** The first of [sizes] (largest first) for which [fits] is true, or the last when none fits. */
fun <T> largestThatFits(sizes: List<T>, fits: (T) -> Boolean): T {
    require(sizes.isNotEmpty()) { "No sizes to choose from" }
    return sizes.firstOrNull(fits) ?: sizes.last()
}
