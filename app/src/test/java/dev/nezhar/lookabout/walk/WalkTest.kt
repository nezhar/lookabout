package dev.nezhar.lookabout.walk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WalkTest {
    private val questions = listOf("Question 1?", "Question 2?", "Question 3?", "Question 4?")

    private fun pictures(count: Int) = (1..count).map { Picture("Picture $it", questions) }

    private fun walk(count: Int = 3) = Walk.start("Autumn in the city", "Overcast", pictures(count), START)

    private fun Walk.shoot(path: String) = photoRequested(path).photoTaken()

    @Test
    fun startHoldsTheWalk() {
        val walk = walk(5)

        assertEquals("Autumn in the city", walk.topic)
        assertEquals("Overcast", walk.sky)
        assertEquals(START, walk.startedAtMillis)
        assertEquals(5, walk.shots.size)
        assertEquals(0, walk.currentIndex)
        assertNull(walk.pendingPhotoPath)
        walk.shots.forEachIndexed { i, shot ->
            assertEquals("Picture ${i + 1}", shot.text)
            assertEquals(questions, shot.questions)
            assertEquals(Picture("Picture ${i + 1}", questions), shot.picture)
            assertTrue(shot.answers.isEmpty())
            assertEquals(listOf(N, N, N, N), shot.corrected)
            assertNull(shot.photoPath)
            assertEquals(Outcome.PENDING, shot.outcome)
            assertNull(shot.checkMillis)
        }
    }

    @Test
    fun walkHasThreeToSixShots() {
        assertEquals(3, walk(3).shots.size)
        assertEquals(6, walk(6).shots.size)
        assertThrows(IllegalArgumentException::class.java) { walk(2) }
        assertThrows(IllegalArgumentException::class.java) { walk(7) }
    }

    @Test
    fun pictureHasFourQuestions() {
        assertThrows(IllegalArgumentException::class.java) { Picture("Three", questions.take(3)) }
        assertThrows(IllegalArgumentException::class.java) { Picture("Five", questions + "Question 5?") }
    }

    @Test
    fun takenPhotoMovesThePendingPathToTheShot() {
        val opened = walk().photoRequested("/photos/a.jpg")
        assertEquals("/photos/a.jpg", opened.pendingPhotoPath)
        assertNull(opened.current!!.photoPath)

        val taken = opened.photoTaken()
        assertNull(taken.pendingPhotoPath)
        assertEquals("/photos/a.jpg", taken.current!!.photoPath)
        assertEquals(Outcome.PENDING, taken.current!!.outcome)
    }

    @Test
    fun photoTakenWithoutPendingPathChangesNothing() {
        assertEquals(walk(), walk().photoTaken())
    }

    @Test
    fun cancelledCameraClearsThePendingPathOnly() {
        assertEquals(walk(), walk().photoRequested("/photos/a.jpg").cameraCancelled())
    }

    @Test
    fun cancelledRetakeKeepsTheOldPhotoAndResult() {
        val checked = walk().shoot("/photos/a.jpg").recordCheck(listOf(Y, N, N, N), 5_000).flip(2)
        val retake = checked.photoRequested("/photos/b.jpg")
        assertEquals("/photos/a.jpg", retake.current!!.photoPath)
        assertEquals(checked.current, retake.current)

        assertEquals(checked, retake.cameraCancelled())
    }

    @Test
    fun retakeReplacesThePhotoAndClearsTheResultUntilTheNewCheck() {
        val checked = walk().shoot("/photos/a.jpg").recordCheck(listOf(Y, N, N, N), 5_000).flip(1)

        val retaken = checked.shoot("/photos/b.jpg").current!!
        assertEquals("/photos/b.jpg", retaken.photoPath)
        assertTrue(retaken.answers.isEmpty())
        assertEquals(listOf(N, N, N, N), retaken.corrected)
        assertEquals(Outcome.PENDING, retaken.outcome)
        assertNull(retaken.checkMillis)
        assertEquals(0, retaken.checkCount)

        val rechecked = checked.shoot("/photos/b.jpg").recordCheck(listOf(Y, Y, Y, N), 4_000).current!!
        assertEquals(Outcome.ACCEPTED, rechecked.outcome)
        assertEquals(75, rechecked.coveragePercent)
        assertEquals(4_000L, rechecked.checkMillis)
    }

    @Test
    fun recordedCheckIsAcceptedAtTwoOrMoreYes() {
        val cases = listOf(
            listOf(N, N, N, N) to (Outcome.NOT_YET to 0),
            listOf(N, N, Y, N) to (Outcome.NOT_YET to 25),
            listOf(Y, N, N, Y) to (Outcome.ACCEPTED to 50),
            listOf(Y, Y, N, Y) to (Outcome.ACCEPTED to 75),
            listOf(Y, Y, Y, Y) to (Outcome.ACCEPTED to 100),
        )
        for ((answers, expected) in cases) {
            val shot = walk().shoot("/photos/a.jpg").recordCheck(answers, 6_200).current!!
            assertEquals(answers, shot.answers)
            assertEquals(expected.first, shot.outcome)
            assertEquals(expected.second, shot.coveragePercent)
            assertEquals(6_200L, shot.checkMillis)
            assertEquals("/photos/a.jpg", shot.photoPath)
        }
    }

    @Test
    fun acceptedRuleNeedsAnswers() {
        assertFalse(isAccepted(emptyList()))
        assertFalse(isAccepted(listOf(Y, N, N, N)))
        assertTrue(isAccepted(listOf(N, Y, N, Y)))
    }

    @Test
    fun recordedCheckNeedsFourAnswers() {
        val photo = walk().shoot("/photos/a.jpg")
        assertThrows(IllegalArgumentException::class.java) { photo.recordCheck(listOf(Y, Y, N), 1_000) }
    }

    @Test
    fun checkWithoutPhotoChangesNothing() {
        assertEquals(walk(), walk().recordCheck(listOf(Y, Y, Y, Y), 1_000))
    }

    @Test
    fun flippingCorrectsAnAnswerAndFlippingBackRemovesTheCorrection() {
        val checked = walk().shoot("/photos/a.jpg").recordCheck(listOf(Y, N, N, N), 5_000)
        assertEquals(Outcome.NOT_YET, checked.current!!.outcome)
        assertEquals(25, checked.current!!.coveragePercent)

        val flipped = checked.flip(1)
        val shot = flipped.current!!
        assertEquals(listOf(Y, N, N, N), shot.answers)
        assertEquals(listOf(N, Y, N, N), shot.corrected)
        assertEquals(listOf(Y, Y, N, N), shot.finalAnswers)
        assertEquals(Outcome.ACCEPTED, shot.outcome)
        assertEquals(50, shot.coveragePercent)

        assertEquals(checked, flipped.flip(1))
    }

    @Test
    fun flippingCanTurnAcceptedIntoNotYet() {
        val flipped = walk().shoot("/photos/a.jpg").recordCheck(listOf(Y, Y, N, N), 5_000).flip(0).current!!
        assertEquals(Outcome.NOT_YET, flipped.outcome)
        assertEquals(25, flipped.coveragePercent)
    }

    @Test
    fun flippingAnUncheckedShotOrAnUnknownQuestionChangesNothing() {
        assertEquals(walk(), walk().flip(0))
        val photo = walk().shoot("/photos/a.jpg")
        assertEquals(photo, photo.flip(0))
        val checked = photo.recordCheck(listOf(Y, N, N, N), 5_000)
        assertEquals(checked, checked.flip(4))
        assertEquals(checked, checked.flip(-1))
    }

    @Test
    fun skippingWithoutPhotoMarksTheShotSkipped() {
        val skipped = walk().skip()
        assertEquals(Outcome.SKIPPED, skipped.shots[0].outcome)
        assertEquals(1, skipped.currentIndex)
    }

    @Test
    fun skippingAfterNotYetKeepsTheResult() {
        val checked = walk().shoot("/photos/a.jpg").recordCheck(listOf(Y, N, N, N), 5_000)
        val skipped = checked.skip()
        assertEquals(checked.current, skipped.shots[0])
        assertEquals(Outcome.NOT_YET, skipped.shots[0].outcome)
        assertEquals(1, skipped.currentIndex)
    }

    @Test
    fun skippingAPhotoThatWasNeverCheckedMarksItSkipped() {
        val skipped = walk().shoot("/photos/a.jpg").skip()
        assertEquals(Outcome.SKIPPED, skipped.shots[0].outcome)
        assertNull(skipped.shots[0].photoPath)
        assertEquals(1, skipped.currentIndex)
        assertEquals(0, skipped.checkCount)
    }

    @Test
    fun nextPictureMovesOnOnlyAfterACheck() {
        assertEquals(walk(), walk().nextPicture())
        val photo = walk().shoot("/photos/a.jpg")
        assertEquals(photo, photo.nextPicture())

        val next = photo.recordCheck(listOf(Y, Y, N, N), 5_000).nextPicture()
        assertEquals(1, next.currentIndex)
        assertEquals(Outcome.ACCEPTED, next.shots[0].outcome)
    }

    @Test
    fun walkIsFinishedOnceTheLastShotIsAcceptedOrSkippedPast() {
        val onLast = walk(3).skip().skip()
        assertTrue(onLast.isLast)
        assertFalse(onLast.isFinished)

        val notYet = onLast.shoot("/photos/c.jpg").recordCheck(listOf(Y, N, N, N), 5_000)
        assertFalse(notYet.isFinished)

        val accepted = notYet.flip(3)
        assertTrue(accepted.isFinished)
        assertFalse(accepted.flip(3).isFinished)

        val past = accepted.nextPicture()
        assertTrue(past.isFinished)
        assertEquals(3, past.currentIndex)
        assertNull(past.current)
        assertEquals(past, past.skip())
        assertEquals(past, past.nextPicture())

        assertTrue(notYet.skip().isFinished)
        assertTrue(onLast.skip().isFinished)
    }

    @Test
    fun totalsCountOnlyCheckedPhotos() {
        val walk = walk(3).skip().shoot("/photos/b.jpg").skip()
        assertEquals(0, walk.acceptedCount)
        assertEquals(0, walk.checkCount)
        assertEquals(0, walk.agreedCount)
    }

    @Test
    fun sampleWalkGivesThreeOfFiveAcceptedAndFifteenOfSixteenAgreed() {
        val texts = listOf(
            "A reflection with someone walking past",
            "Fallen leaves on something man-made",
            "A doorway with one strong colour",
            "Looking straight up through bare branches",
            "A lit window on a grey street",
        )
        var walk = Walk.start("Autumn in the city", "Overcast", texts.map { Picture(it, questions) }, START)

        walk = walk.photoRequested("/photos/1.jpg").photoTaken().recordCheck(listOf(Y, Y, N, Y), 6_100)
        assertEquals(Outcome.ACCEPTED, walk.current!!.outcome)
        assertEquals(75, walk.current!!.coveragePercent)
        walk = walk.nextPicture()

        walk = walk.photoRequested("/photos/2.jpg").photoTaken().recordCheck(listOf(Y, N, N, N), 5_800)
        assertEquals(Outcome.NOT_YET, walk.current!!.outcome)
        assertEquals(25, walk.current!!.coveragePercent)
        walk = walk.skip()

        walk = walk.photoRequested("/photos/3.jpg").photoTaken().recordCheck(listOf(Y, Y, Y, Y), 6_400)
        assertEquals(Outcome.ACCEPTED, walk.current!!.outcome)
        assertEquals(100, walk.current!!.coveragePercent)
        walk = walk.nextPicture()

        walk = walk.photoRequested("/photos/4.jpg").photoTaken().recordCheck(listOf(Y, N, N, N), 5_900)
        assertEquals(Outcome.NOT_YET, walk.current!!.outcome)
        assertEquals(25, walk.current!!.coveragePercent)
        walk = walk.flip(1)
        assertEquals(Outcome.ACCEPTED, walk.current!!.outcome)
        assertEquals(50, walk.current!!.coveragePercent)
        walk = walk.nextPicture()

        assertFalse(walk.isFinished)
        walk = walk.skip()

        assertTrue(walk.isFinished)
        assertNull(walk.current)
        assertEquals(
            listOf(Outcome.ACCEPTED, Outcome.NOT_YET, Outcome.ACCEPTED, Outcome.ACCEPTED, Outcome.SKIPPED),
            walk.shots.map { it.outcome },
        )
        assertEquals(listOf(75, 25, 100, 50), walk.shots.take(4).map { it.coveragePercent })
        assertNull(walk.shots[4].photoPath)
        assertEquals(3, walk.acceptedCount)
        assertEquals(5, walk.shots.size)
        assertEquals(15, walk.agreedCount)
        assertEquals(16, walk.checkCount)
    }

    private fun walkCheckedAtEnd(count: Int = 3) =
        Walk.start("Autumn in the city", "Overcast", pictures(count), START, checkAtEnd = true)

    @Test
    fun aWalkChecksEachPhotoAtOnceByDefault() {
        assertFalse(walk().checkAtEnd)
        assertTrue(walkCheckedAtEnd().checkAtEnd)
    }

    @Test
    fun withoutCheckAtEndAnUncheckedPhotoCannotMoveOn() {
        val taken = walk().shoot("/p/1.jpg")
        assertFalse(taken.canMoveOn)
        assertEquals(taken, taken.nextPicture())
    }

    @Test
    fun withCheckAtEndAPhotoMovesOnUnchecked() {
        val fresh = walkCheckedAtEnd()
        assertFalse(fresh.canMoveOn)
        assertEquals(fresh, fresh.nextPicture())

        val moved = fresh.shoot("/p/1.jpg").nextPicture()
        assertEquals(1, moved.currentIndex)
        assertEquals("/p/1.jpg", moved.shots[0].photoPath)
        assertEquals(Outcome.PENDING, moved.shots[0].outcome)
        assertFalse(moved.shots[0].isChecked)
    }

    @Test
    fun withCheckAtEndFinishWalkOnTheLastMovesPastIt() {
        val end = walkCheckedAtEnd().shoot("/p/1.jpg").nextPicture()
            .shoot("/p/2.jpg").nextPicture()
            .shoot("/p/3.jpg").nextPicture()
        assertEquals(3, end.currentIndex)
        assertNull(end.current)
        assertTrue(end.isFinished)
        assertFalse(end.canMoveOn)
    }

    @Test
    fun shotsToCheckAreThePhotosWithoutACheck() {
        val taken = walkCheckedAtEnd(5)
            .shoot("/p/1.jpg").nextPicture()
            .skip()
            .shoot("/p/3.jpg").nextPicture()
            .shoot("/p/4.jpg").nextPicture()
        assertEquals(listOf(0, 2, 3), taken.shotsToCheck)

        val checked = taken.recordCheck(listOf(Y, Y, N, N), 2_000L, shotIndex = 2)
        assertEquals(listOf(0, 3), checked.shotsToCheck)
        assertEquals(Outcome.ACCEPTED, checked.shots[2].outcome)
        assertEquals(4, checked.currentIndex)

        assertEquals(emptyList<Int>(), walk(3).shotsToCheck)
    }

    private companion object {
        const val START = 1_760_000_000_000L
        const val Y = true
        const val N = false
    }
}
