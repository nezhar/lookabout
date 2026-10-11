package dev.nezhar.lookabout.walk

/**
 * A photo walk: the topic and sky from Setup, the start time in epoch milliseconds, 3 to 6 shots,
 * the index of the current shot and the path of a photo the camera is taking, if any.
 *
 * Immutable: every step returns an updated walk. [currentIndex] runs from 0 to `shots.size`;
 * `shots.size` means the walker has moved past the last shot, and [current] is then null.
 */
data class Walk(
    val topic: String,
    val sky: String,
    val startedAtMillis: Long,
    val shots: List<Shot>,
    val currentIndex: Int = 0,
    val pendingPhotoPath: String? = null,
) {
    init {
        require(shots.size in MIN_SHOTS..MAX_SHOTS) { "A walk has $MIN_SHOTS to $MAX_SHOTS shots, not ${shots.size}" }
        require(currentIndex in 0..shots.size) { "Current index $currentIndex is outside the walk" }
    }

    /** The shot the walker is on, or null once past the last one. */
    val current: Shot? get() = shots.getOrNull(currentIndex)

    /** True while the walker is on the last shot. */
    val isLast: Boolean get() = currentIndex == shots.lastIndex

    /** Finished once the last shot is accepted, or the walker has moved past it (by Skip or Finish walk). */
    val isFinished: Boolean
        get() = currentIndex >= shots.size || (isLast && shots.last().outcome == Outcome.ACCEPTED)

    /** Shots accepted, after corrections. */
    val acceptedCount: Int get() = shots.count { it.outcome == Outcome.ACCEPTED }

    /** Checks made: four per checked photo. */
    val checkCount: Int get() = shots.sumOf { it.checkCount }

    /** Checks where the walker and Gemma agreed: an answer that stays corrected is a disagreement. */
    val agreedCount: Int get() = shots.sumOf { it.agreedCount }

    /** Shoot or Retake: remember where the camera will write the photo. The shot is not changed yet. */
    fun photoRequested(path: String): Walk = copy(pendingPhotoPath = path)

    /**
     * The camera reported a photo: the pending path becomes the current shot's photo, replacing an
     * older one and clearing its result until the new check is in. Without a pending path nothing changes.
     */
    fun photoTaken(): Walk {
        val path = pendingPhotoPath ?: return this
        return updateShot(currentIndex) { it.withPhoto(path) }.copy(pendingPhotoPath = null)
    }

    /** The camera was cancelled: the pending path is cleared and nothing else changes. */
    fun cameraCancelled(): Walk = copy(pendingPhotoPath = null)

    /**
     * Records a check on a shot, the current one by default: Gemma's four answers and the time the
     * check took. A shot without a photo is left unchanged. A failed check records nothing.
     */
    fun recordCheck(answers: List<Boolean>, checkMillis: Long, shotIndex: Int = currentIndex): Walk =
        updateShot(shotIndex) { it.withCheck(answers, checkMillis) }

    /** Flips the answer to [question] (0 to 3) on a checked shot, the current one by default. */
    fun flip(question: Int, shotIndex: Int = currentIndex): Walk =
        updateShot(shotIndex) { it.flipped(question) }

    /** Skip: see [Shot.skipped] for what happens to the shot, then move on to the next one. */
    fun skip(): Walk {
        if (current == null) return this
        return updateShot(currentIndex) { it.skipped() }.copy(currentIndex = currentIndex + 1)
    }

    /** Next picture, or Finish walk on the last: move on, keeping the result. Only for a checked shot. */
    fun nextPicture(): Walk =
        if (current?.isChecked == true) copy(currentIndex = currentIndex + 1) else this

    private fun updateShot(index: Int, change: (Shot) -> Shot): Walk {
        if (index !in shots.indices) return this
        return copy(shots = shots.mapIndexed { i, shot -> if (i == index) change(shot) else shot })
    }

    companion object {
        const val MIN_SHOTS = 3
        const val MAX_SHOTS = 6

        /** A new walk at its first shot, one shot per picture. */
        fun start(topic: String, sky: String, pictures: List<Picture>, startedAtMillis: Long): Walk =
            Walk(
                topic = topic,
                sky = sky,
                startedAtMillis = startedAtMillis,
                shots = pictures.map { Shot.of(it) },
            )
    }
}
