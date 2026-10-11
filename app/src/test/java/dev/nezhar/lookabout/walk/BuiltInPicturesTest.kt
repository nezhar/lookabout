package dev.nezhar.lookabout.walk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BuiltInPicturesTest {
    private fun words(s: String) = s.split(" ").size

    @Test
    fun listHasAtLeastEightPictures() {
        assertTrue(builtInPictures.size >= 8)
    }

    @Test
    fun textsAreDistinct() {
        assertEquals(builtInPictures.size, builtInPictures.map { it.text }.toSet().size)
    }

    @Test
    fun everyEntryKeepsTheLimits() {
        builtInPictures.forEach { picture ->
            val text = picture.text
            assertTrue("Too many words: $text", words(text) <= MAX_TEXT_WORDS)
            assertTrue("Too long: $text", text.length <= MAX_TEXT_CHARS)
            assertEquals("Not four questions: $text", QUESTIONS_PER_PICTURE, picture.questions.size)
            picture.questions.forEach { question ->
                assertTrue("Too many words: $question", words(question) <= MAX_QUESTION_WORDS)
                assertTrue("Too long: $question", question.length <= MAX_QUESTION_CHARS)
                assertTrue("Not a question: $question", question.endsWith("?"))
            }
        }
    }

    @Test
    fun firstFiveAreTheSampleWalk() {
        assertEquals(
            listOf(
                "A reflection with someone walking past",
                "Fallen leaves on something man-made",
                "A doorway with one strong colour",
                "Looking straight up through bare branches",
                "A lit window on a grey street",
            ),
            builtInPictures.take(5).map { it.text },
        )
    }

    @Test
    fun pickReturnsThatManyDifferentBuiltInPictures() {
        for (count in 3..6) {
            for (seed in 0..20) {
                val picked = pickBuiltInPictures(count, Random(seed))
                assertEquals(count, picked.size)
                assertEquals(count, picked.toSet().size)
                assertTrue(builtInPictures.containsAll(picked))
            }
        }
    }

    @Test
    fun pickIsRepeatableWithTheSameSeed() {
        assertEquals(pickBuiltInPictures(5, Random(42)), pickBuiltInPictures(5, Random(42)))
    }

    @Test
    fun pickOrderIsRandom() {
        val orders = (0..20).map { pickBuiltInPictures(6, Random(it)) }.toSet()
        assertNotEquals(1, orders.size)
    }

    @Test
    fun pickRejectsCountsOutsideThreeToSix() {
        assertThrows(IllegalArgumentException::class.java) { pickBuiltInPictures(2) }
        assertThrows(IllegalArgumentException::class.java) { pickBuiltInPictures(7) }
    }

    @Test
    fun exceptSkipsGivenTexts() {
        val taken = listOf(builtInPictures[0].text, builtInPictures[3].text, "Not a built-in picture")
        val rest = builtInPicturesExcept(taken)

        assertEquals(builtInPictures.size - 2, rest.size)
        assertTrue(rest.none { it.text in taken })
        assertEquals(builtInPictures, builtInPicturesExcept(emptyList()))
    }
}
