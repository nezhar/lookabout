package dev.nezhar.lookabout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckRunTest {
    @Test
    fun landscapeShrinksToTheLongEdge() {
        assertEquals(1024 to 768, scaledSize(4000, 3000))
    }

    @Test
    fun portraitShrinksToTheLongEdge() {
        assertEquals(768 to 1024, scaledSize(3000, 4000))
    }

    @Test
    fun squareShrinksOnBothSides() {
        assertEquals(1024 to 1024, scaledSize(2048, 2048))
    }

    @Test
    fun shortSideIsRounded() {
        // 1024 * 3024 / 4032 = 768; 1024 * 1000 / 3000 = 341.33
        assertEquals(1024 to 768, scaledSize(4032, 3024))
        assertEquals(1024 to 341, scaledSize(3000, 1000))
        // 1024 * 2000 / 3000 = 682.67
        assertEquals(683 to 1024, scaledSize(2000, 3000))
    }

    @Test
    fun smallImageKeepsItsSize() {
        assertEquals(800 to 600, scaledSize(800, 600))
        assertEquals(1024 to 500, scaledSize(1024, 500))
    }

    @Test
    fun thinImageKeepsAtLeastOnePixel() {
        assertEquals(1024 to 1, scaledSize(100_000, 10))
    }

    @Test
    fun customLongEdge() {
        assertEquals(768 to 576, scaledSize(4000, 3000, maxEdge = 768))
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyImageIsRejected() {
        scaledSize(0, 100)
    }

    @Test
    fun secondsHaveOneDecimal() {
        assertEquals("9.4 s", formatSeconds(9412))
        assertEquals("2.9 s", formatSeconds(2850))
        assertEquals("0.0 s", formatSeconds(0))
        assertEquals("1.0 s", formatSeconds(999))
        assertEquals("12.3 s", formatSeconds(12_345))
    }

    @Test
    fun coverageShowsPercentAndCount() {
        assertEquals("75%, 3 of 4", coverageText(listOf(true, false, true, true)))
        assertEquals("0%, 0 of 4", coverageText(listOf(false, false, false, false)))
    }

    @Test
    fun checkReadsTheAnswersAndTimesTheModelCall() {
        var clock = 1_000L
        var asked = ""
        val result = runCheck(
            DEFAULT_CHECK_QUESTIONS,
            ask = { prompt ->
                asked = prompt
                clock += 2_500
                "1: NO\n2: YES\n3: YES\n4: YES"
            },
            nowMs = { clock },
        )
        assertEquals(checkPrompt(DEFAULT_CHECK_QUESTIONS), asked)
        assertEquals(listOf(false, true, true, true), result.answers)
        assertEquals(2_500L, result.modelMs)
        assertNull(result.failure)
        assertFalse(result.failed)
    }

    @Test
    fun unreadableReplyIsAFailedCheck() {
        val result = runCheck(DEFAULT_CHECK_QUESTIONS, ask = { "I cannot tell." }, nowMs = { 0L })
        assertTrue(result.failed)
        assertTrue(result.answers.isEmpty())
        assertEquals("I cannot tell.", result.reply)
    }

    @Test(expected = IllegalStateException::class)
    fun modelErrorsPropagate() {
        runCheck(DEFAULT_CHECK_QUESTIONS, ask = { error("Model not loaded") })
    }
}
