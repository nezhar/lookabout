package dev.nezhar.lookabout

import dev.nezhar.lookabout.walk.Outcome
import dev.nezhar.lookabout.walk.Shot
import dev.nezhar.lookabout.walk.builtInPictures
import org.junit.Assert.assertEquals
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
    fun standInAnswersFollowTheSampleWalk() {
        assertEquals("75%, 3 of 4", coverageText(standInAnswers(0)))
        assertEquals("25%, 1 of 4", coverageText(standInAnswers(1)))
        assertEquals("100%, 4 of 4", coverageText(standInAnswers(2)))
        assertEquals("25%, 1 of 4", coverageText(standInAnswers(3)))
        assertEquals(standInAnswers(0), standInAnswers(4))
        assertEquals(standInAnswers(1), standInAnswers(5))
    }

    @Test
    fun standInAnswersShowBothVerdicts() {
        val outcomes = (0..3).map { withPhoto.withCheck(standInAnswers(it), 2000).outcome }
        assertEquals(listOf(Outcome.ACCEPTED, Outcome.NOT_YET, Outcome.ACCEPTED, Outcome.NOT_YET), outcomes)
    }
}
