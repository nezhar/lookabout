package dev.nezhar.lookabout

import dev.nezhar.lookabout.walk.Shot
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
}
