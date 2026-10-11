package dev.nezhar.lookabout

import dev.nezhar.lookabout.walk.Picture
import dev.nezhar.lookabout.walk.Shot
import dev.nezhar.lookabout.walk.Walk
import dev.nezhar.lookabout.walk.builtInPictures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WalkStepsTest {
    private val fresh = Shot.of(builtInPictures.first())
    private val withPhoto = fresh.withPhoto("/photos/1.jpg")

    @Test
    fun positionIsOneBased() {
        assertEquals("1 of 5", positionText(0, 5))
        assertEquals("2 of 5", positionText(1, 5))
        assertEquals("6 of 6", positionText(5, 6))
    }

    @Test
    fun aShotWithoutACheckShowsNextPicture() {
        assertEquals(WalkStep.NextPicture, walkStep(fresh, checking = false))
        assertEquals(WalkStep.NextPicture, walkStep(withPhoto, checking = false))
    }

    @Test
    fun checkingWinsOverEverythingElse() {
        assertEquals(WalkStep.Checking, walkStep(withPhoto, checking = true))
        val checked = withPhoto.withCheck(listOf(true, true, true, true), 2000)
        assertEquals(WalkStep.Checking, walkStep(checked, checking = true))
    }

    @Test
    fun aCheckedShotShowsItsVerdict() {
        val accepted = withPhoto.withCheck(listOf(true, true, false, false), 2000)
        assertEquals(WalkStep.Accepted, walkStep(accepted, checking = false))
        val notYet = withPhoto.withCheck(listOf(true, false, false, false), 2000)
        assertEquals(WalkStep.NotYet, walkStep(notYet, checking = false))
    }

    @Test
    fun aFlipChangesTheVerdictShown() {
        val notYet = withPhoto.withCheck(listOf(true, false, false, false), 2000)
        assertEquals(WalkStep.Accepted, walkStep(notYet.flipped(1), checking = false))
    }

    @Test
    fun largestThatFitsTakesTheFirstFittingSize() {
        val sizes = listOf(48, 40, 32)
        assertEquals(48, largestThatFits(sizes) { it <= 50 })
        assertEquals(40, largestThatFits(sizes) { it <= 44 })
        assertEquals(32, largestThatFits(sizes) { it <= 32 })
    }

    @Test
    fun largestThatFitsFallsBackToTheSmallest() {
        assertEquals(32, largestThatFits(listOf(48, 40, 32)) { false })
    }

    @Test(expected = IllegalArgumentException::class)
    fun largestThatFitsNeedsSizes() {
        largestThatFits(emptyList<Int>()) { true }
    }

    @Test
    fun aPausedCheckShowsCheckPaused() {
        assertEquals(WalkStep.CheckPaused, walkStep(withPhoto, checking = false, paused = true))
    }

    @Test
    fun checkingWinsOverAPausedCheck() {
        assertEquals(WalkStep.Checking, walkStep(withPhoto, checking = true, paused = true))
    }

    @Test
    fun aPausedCheckNeedsAnUncheckedPhoto() {
        assertEquals(WalkStep.NextPicture, walkStep(fresh, checking = false, paused = true))
        val checked = withPhoto.withCheck(listOf(true, true, true, true), 2000)
        assertEquals(WalkStep.Accepted, walkStep(checked, checking = false, paused = true))
    }

    @Test
    fun aPausedCheckAppliesToItsShotAndPhotoOnly() {
        val paused = PausedCheck(shotIndex = 1, photoPath = "/photos/1.jpg")
        assertTrue(paused.appliesTo(1, withPhoto))
        assertFalse(paused.appliesTo(0, withPhoto))
        assertFalse(paused.appliesTo(1, fresh.withPhoto("/photos/2.jpg")))
        assertFalse(paused.appliesTo(1, fresh))
        assertFalse(paused.appliesTo(1, null))
        assertFalse(paused.appliesTo(1, withPhoto.withCheck(listOf(true, false, false, false), 2000)))
    }

    @Test
    fun answersAreRecordedOnlyForASuccessfulCheck() {
        val answers = listOf(true, false, true, true)
        assertEquals(answers, answersToRecord(CheckResult(answers, 1800, "1 YES", null)))
        assertNull(answersToRecord(CheckResult.failed("No readable answer", 1800, "hmm")))
        assertNull(answersToRecord(CheckResult(listOf(true), 1800, "1 YES", null)))
    }

    private val questions = listOf("Q1?", "Q2?", "Q3?", "Q4?")

    /** Five pictures checked at the end: photos on 1, 2, 4 and 5, the third skipped, past the last. */
    private fun walkAtItsEnd(): Walk =
        Walk.start("Reflections", "Rain", (1..5).map { Picture("Picture $it", questions) }, 0L, checkAtEnd = true)
            .photoRequested("/p/1.jpg").photoTaken().nextPicture()
            .photoRequested("/p/2.jpg").photoTaken().nextPicture()
            .skip()
            .photoRequested("/p/4.jpg").photoTaken().nextPicture()
            .photoRequested("/p/5.jpg").photoTaken().nextPicture()

    @Test
    fun withCheckAtEndAnUncheckedPhotoShowsPhotoTaken() {
        assertEquals(WalkStep.PhotoTaken, walkStep(withPhoto, checking = false, checkAtEnd = true))
        assertEquals(WalkStep.NextPicture, walkStep(fresh, checking = false, checkAtEnd = true))
        val accepted = withPhoto.withCheck(listOf(true, true, false, false), 2000)
        assertEquals(WalkStep.Accepted, walkStep(accepted, checking = false, checkAtEnd = true))
        val notYet = withPhoto.withCheck(listOf(true, false, false, false), 2000)
        assertEquals(WalkStep.NotYet, walkStep(notYet, checking = false, checkAtEnd = true))
    }

    @Test
    fun withoutCheckAtEndAnUncheckedPhotoNeverShowsPhotoTaken() {
        assertEquals(WalkStep.NextPicture, walkStep(withPhoto, checking = false, checkAtEnd = false))
        assertEquals(WalkStep.CheckPaused, walkStep(withPhoto, checking = false, paused = true))
    }

    @Test
    fun theCheckAtEndRunsOncePastTheLastPictureBeforeSummary() {
        val end = walkAtItsEnd()
        assertTrue(checksAtEnd(end, onSummary = false))
        assertFalse(checksAtEnd(end, onSummary = true))
        // Still on a picture.
        assertFalse(checksAtEnd(end.copy(currentIndex = 4), onSummary = false))
        // A walk that checks each photo at once never checks at the end.
        assertFalse(checksAtEnd(end.copy(checkAtEnd = false), onSummary = false))
        assertEquals(listOf(0, 1, 3, 4), end.shotsToCheck)
    }

    @Test
    fun aFailedCheckAtTheEndLeavesItsShotToCheck() {
        // The second photo's check failed, so nothing was recorded for it; the others are checked.
        val end = walkAtItsEnd()
            .recordCheck(listOf(true, true, true, false), 1000, shotIndex = 0)
            .recordCheck(listOf(false, false, false, true), 1000, shotIndex = 3)
            .recordCheck(listOf(true, true, false, false), 1000, shotIndex = 4)
        assertEquals(listOf(1), end.shotsToCheck)
        assertFalse(end.shots[1].isChecked)
        assertEquals("/p/2.jpg", end.shots[1].photoPath)
    }

    @Test
    fun theEndCheckCountsOnlyPhotos() {
        val end = walkAtItsEnd()
        assertEquals("Checking 1 of 4", endCheckText(end, 0))
        assertEquals("Checking 2 of 4", endCheckText(end, 1))
        assertEquals("Checking 3 of 4", endCheckText(end, 3))
        assertEquals("Checking 4 of 4", endCheckText(end, 4))
    }

    @Test
    fun theEndCheckProgressCountsThePhotosBefore() {
        val end = walkAtItsEnd()
        assertEquals(0f, endCheckProgress(end, 0), 0f)
        assertEquals(0.25f, endCheckProgress(end, 1), 0f)
        assertEquals(0.5f, endCheckProgress(end, 3), 0f)
        assertEquals(0.75f, endCheckProgress(end, 4), 0f)
        val noPhotos = Walk.start("Reflections", "Rain", (1..3).map { Picture("P$it", questions) }, 0L, checkAtEnd = true)
        assertEquals(0f, endCheckProgress(noPhotos, 0), 0f)
    }

    /** [walkAtItsEnd] with every photo checked. */
    private fun allChecked(): Walk = walkAtItsEnd()
        .recordCheck(listOf(true, true, true, false), 1000, shotIndex = 0)
        .recordCheck(listOf(true, false, false, false), 1000, shotIndex = 1)
        .recordCheck(listOf(false, false, false, true), 1000, shotIndex = 3)
        .recordCheck(listOf(true, true, false, false), 1000, shotIndex = 4)

    @Test
    fun theEndCheckWaitsForTheModel() {
        val end = walkAtItsEnd()
        assertFalse(canStartEndCheck(end, modelReady = false))
        assertTrue(canStartEndCheck(end, modelReady = true))
    }

    @Test
    fun theEndCheckDoesNotStartWithNothingToCheck() {
        assertFalse(canStartEndCheck(allChecked(), modelReady = true))
        val noPhotos = Walk.start("Reflections", "Rain", (1..3).map { Picture("P$it", questions) }, 0L, checkAtEnd = true)
            .skip().skip().skip()
        assertFalse(canStartEndCheck(noPhotos, modelReady = true))
    }

    @Test
    fun summaryOffersCheckAgainForAPhotoWithoutAResult() {
        val oneFailed = walkAtItsEnd()
            .recordCheck(listOf(true, true, true, false), 1000, shotIndex = 0)
            .recordCheck(listOf(false, false, false, true), 1000, shotIndex = 3)
            .recordCheck(listOf(true, true, false, false), 1000, shotIndex = 4)
        assertTrue(offersCheckAgain(oneFailed, endCheckRunning = false))
        // Every check failed, e.g. the model was not ready.
        assertTrue(offersCheckAgain(walkAtItsEnd(), endCheckRunning = false))
    }

    @Test
    fun summaryHidesCheckAgainWhenNothingNeedsChecking() {
        assertFalse(offersCheckAgain(allChecked(), endCheckRunning = false))
        // A skipped picture has no photo to check.
        val skipped = Walk.start("Reflections", "Rain", (1..2).map { Picture("P$it", questions) }, 0L, checkAtEnd = true)
            .skip().skip()
        assertFalse(offersCheckAgain(skipped, endCheckRunning = false))
    }

    @Test
    fun summaryHidesCheckAgainWhileTheEndCheckRuns() {
        assertFalse(offersCheckAgain(walkAtItsEnd(), endCheckRunning = true))
    }

    @Test
    fun theEndCheckTitleSaysWhenItWaitsForTheModel() {
        val end = walkAtItsEnd()
        assertEquals("Getting the model ready", endCheckTitle(end, 0, waiting = true))
        assertEquals("Checking 2 of 4", endCheckTitle(end, 1, waiting = false))
    }

    @Test
    fun theWaitNoteShowsOnlyForAMissingOrFailedModel() {
        assertNull(modelWaitNote(modelUnavailable = false))
        assertEquals(
            "The model is not on the phone or failed to load. The check starts as soon as it is ready.",
            modelWaitNote(modelUnavailable = true),
        )
    }
}
