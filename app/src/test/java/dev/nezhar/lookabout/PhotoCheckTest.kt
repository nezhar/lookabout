package dev.nezhar.lookabout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoCheckTest {
    @Test
    fun promptNumbersTheFourQuestions() {
        val prompt = checkPrompt(
            listOf("Is a person in the frame?", "Is the sky visible?", "Are trees or plants visible?", "Is a building visible?"),
        )
        assertEquals(
            "Look at the photo. Answer each question with YES or NO only.\n" +
                "Write exactly one line per question, in this form: 1: YES\n" +
                "\n" +
                "1. Is a person in the frame?\n" +
                "2. Is the sky visible?\n" +
                "3. Are trees or plants visible?\n" +
                "4. Is a building visible?",
            prompt,
        )
    }

    @Test
    fun promptKeepsEachQuestionOnOneLine() {
        val prompt = checkPrompt(listOf("A?", "  Is the sky\nvisible?  ", "C?", "D?"))
        assertTrue(prompt.endsWith("1. A?\n2. Is the sky visible?\n3. C?\n4. D?"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun promptNeedsFourQuestions() {
        checkPrompt(listOf("A?", "B?", "C?"))
    }

    @Test
    fun readsACleanReply() {
        val reply = "1: YES\n2: NO\n3: YES\n4: NO"
        assertEquals(mapOf(1 to true, 2 to false, 3 to true, 4 to false), parseAnswers(reply))
        assertEquals(listOf(true, false, true, false), readAnswers(reply))
    }

    @Test
    fun readsMixedCase() {
        assertEquals(listOf(true, false, true, false), readAnswers("1: yes\n2: No\n3: YeS\n4: nO"))
    }

    @Test
    fun readsEverySeparator() {
        for (separator in listOf(":", ".", ")", " -")) {
            assertEquals(separator, mapOf(1 to true), parseAnswers("1$separator YES"))
        }
        assertEquals(listOf(true, false, true, false), readAnswers("1: YES\n2. NO\n3) YES\n4 - NO"))
    }

    @Test
    fun readsBoldMarks() {
        assertEquals(
            listOf(true, false, true, false),
            readAnswers("**1: YES**\n**2:** NO\n**3**: **YES**\n__4__ - __no__"),
        )
    }

    @Test
    fun readsFourAnswersOnOneLine() {
        val expected = listOf(true, false, true, false)
        assertEquals(expected, readAnswers("1: YES, 2: NO, 3: YES, 4: NO"))
        assertEquals(expected, readAnswers("1: YES; 2: NO; 3: YES; 4: NO"))
    }

    @Test
    fun missingLineCountsAsNo() {
        val answers = readAnswers("1: YES\n2: YES\n4: NO")
        assertEquals(listOf(true, true, false, false), answers)
    }

    @Test
    fun unreadableAnswerCountsAsNo() {
        assertEquals(listOf(true, false, false, true), readAnswers("1: YES\n2: maybe\n3: NOT SURE\n4: YES"))
    }

    @Test
    fun ignoresExtraTextAroundTheLines() {
        val reply = "Sure! Here are my answers:\r\n\r\n" +
            "1: YES (a person walks past)\r\n" +
            "2: NO\r\n" +
            "3: YES - lots of trees\r\n" +
            "4: NO\r\n" +
            "Let me know if you need more."
        assertEquals(listOf(true, false, true, false), readAnswers(reply))
    }

    @Test
    fun replyWithoutAReadableAnswerFailsTheCheck() {
        for (reply in listOf("", "I can't see the photo.", "1: MAYBE\n2: YESPLEASE", "5: YES\n0: NO")) {
            assertNull(reply, readAnswers(reply))
        }
    }

    @Test
    fun fourNoAnswersAreAVerdictNotAFailure() {
        val answers = readAnswers("1: NO\n2: NO\n3: NO\n4: NO")
        assertEquals(listOf(false, false, false, false), answers)
        assertEquals(0, coveragePercent(answers!!))
        assertFalse(accepted(answers))
    }

    @Test
    fun acceptedFromTwoOfFour() {
        assertFalse(accepted(listOf(true, false, false, false)))
        assertEquals(25, coveragePercent(listOf(true, false, false, false)))
        assertTrue(accepted(listOf(true, true, false, false)))
        assertEquals(50, coveragePercent(listOf(true, true, false, false)))
        assertTrue(accepted(readAnswers("1: NO\n2: YES\n3: NO\n4: YES")!!))
        assertFalse(accepted(readAnswers("1: NO\n2: YES\n3: NO\n4: NO")!!))
    }

    @Test
    fun coverageCountsYesAnswersOutOfFour() {
        for (yes in 0..4) {
            val answers = List(4) { it < yes }
            assertEquals(yes * 25, coveragePercent(answers))
            assertEquals(yes >= 2, accepted(answers))
        }
    }

    @Test
    fun noAnswersAreNeverAccepted() {
        assertFalse(accepted(emptyList()))
        assertEquals(0, coveragePercent(emptyList()))
    }
}
