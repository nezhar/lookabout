package dev.nezhar.lookabout.walk

/** Every picture has exactly this many yes/no questions. */
const val QUESTIONS_PER_PICTURE = 4

/**
 * A picture to find on a walk: a short text and exactly four yes/no questions about the photo.
 * A walk's shots are created from pictures (see [Walk.start]).
 */
data class Picture(
    val text: String,
    val questions: List<String>,
) {
    init {
        require(questions.size == QUESTIONS_PER_PICTURE) {
            "A picture has $QUESTIONS_PER_PICTURE questions, not ${questions.size}"
        }
    }
}

/** What became of a shot. */
enum class Outcome {
    /** Not checked yet: no photo, or a photo whose check is not in. */
    PENDING,

    /** Checked, with two or more YES answers after corrections. */
    ACCEPTED,

    /** Checked, with fewer than two YES answers after corrections. */
    NOT_YET,

    /** Skipped without a checked photo. */
    SKIPPED,
}

/** Two or more YES of four is accepted (50% or more). No answers is never accepted. */
fun isAccepted(answers: List<Boolean>): Boolean =
    answers.isNotEmpty() && answers.count { it } * 2 >= answers.size
