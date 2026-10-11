package dev.nezhar.lookabout.walk

/**
 * One picture of a walk and what happened to it.
 *
 * [answers] are Gemma's four answers, empty until a check is recorded. [corrected] marks, per
 * question, an answer the walker flipped; the answers that count are [finalAnswers]. [outcome]
 * always follows [finalAnswers] once the shot is checked. [checkMillis] is how long the check
 * took, null until a check is recorded. Question indices are 0 to 3.
 */
data class Shot(
    val text: String,
    val questions: List<String>,
    val answers: List<Boolean> = emptyList(),
    val corrected: List<Boolean> = List(QUESTIONS_PER_PICTURE) { false },
    val photoPath: String? = null,
    val outcome: Outcome = Outcome.PENDING,
    val checkMillis: Long? = null,
) {
    init {
        require(questions.size == QUESTIONS_PER_PICTURE) { "A shot has $QUESTIONS_PER_PICTURE questions" }
        require(answers.isEmpty() || answers.size == QUESTIONS_PER_PICTURE) { "A check has $QUESTIONS_PER_PICTURE answers" }
        require(corrected.size == QUESTIONS_PER_PICTURE) { "A shot has $QUESTIONS_PER_PICTURE correction marks" }
    }

    /** The picture this shot was made from. */
    val picture: Picture get() = Picture(text, questions)

    val hasPhoto: Boolean get() = photoPath != null

    /** True once a check is recorded for the current photo. */
    val isChecked: Boolean get() = answers.isNotEmpty()

    /** Gemma's answers with the walker's corrections applied. Empty until checked. */
    val finalAnswers: List<Boolean> get() = answers.mapIndexed { i, yes -> yes != corrected[i] }

    /** YES answers after corrections. */
    val yesCount: Int get() = finalAnswers.count { it }

    /** Coverage after corrections: 0, 25, 50, 75 or 100. */
    val coveragePercent: Int get() = yesCount * 100 / QUESTIONS_PER_PICTURE

    /** Checks on this shot: four once its photo is checked, else none. */
    val checkCount: Int get() = if (isChecked) QUESTIONS_PER_PICTURE else 0

    /** Checks where the walker left Gemma's answer as it was. */
    val agreedCount: Int get() = if (isChecked) corrected.count { !it } else 0

    /** A new photo arrived: it replaces the old one, and the old result is cleared until the new check is in. */
    fun withPhoto(path: String): Shot = copy(
        photoPath = path,
        answers = emptyList(),
        corrected = List(QUESTIONS_PER_PICTURE) { false },
        outcome = Outcome.PENDING,
        checkMillis = null,
    )

    /** Records Gemma's four answers and the check time. A shot without a photo is left unchanged. */
    fun withCheck(answers: List<Boolean>, checkMillis: Long): Shot {
        require(answers.size == QUESTIONS_PER_PICTURE) { "A check has $QUESTIONS_PER_PICTURE answers, not ${answers.size}" }
        if (!hasPhoto) return this
        return copy(
            answers = answers.toList(),
            corrected = List(QUESTIONS_PER_PICTURE) { false },
            outcome = verdict(answers),
            checkMillis = checkMillis,
        )
    }

    /**
     * Flips the answer to [question]: an uncorrected answer becomes corrected, a corrected one goes
     * back to Gemma's answer. The outcome follows. Unchecked shots and unknown indices are left unchanged.
     */
    fun flipped(question: Int): Shot {
        if (!isChecked || question !in 0 until QUESTIONS_PER_PICTURE) return this
        val marks = corrected.toMutableList().also { it[question] = !it[question] }
        val flippedAnswers = answers.mapIndexed { i, yes -> yes != marks[i] }
        return copy(corrected = marks, outcome = verdict(flippedAnswers))
    }

    /**
     * Skip: a checked shot keeps its result (accepted or not yet). Any other shot is marked skipped,
     * and a photo that was taken but never checked (for example after a failed check) is dropped
     * from the shot, so a skipped shot never has a photo and Summary shows it as not checked.
     */
    fun skipped(): Shot =
        if (isChecked) this else copy(photoPath = null, outcome = Outcome.SKIPPED)

    companion object {
        /** A new, unchecked shot for [picture]. */
        fun of(picture: Picture): Shot = Shot(text = picture.text, questions = picture.questions)

        private fun verdict(answers: List<Boolean>): Outcome =
            if (isAccepted(answers)) Outcome.ACCEPTED else Outcome.NOT_YET
    }
}
