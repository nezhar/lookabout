package dev.nezhar.lookabout

import dev.nezhar.lookabout.walk.Picture
import dev.nezhar.lookabout.walk.builtInPictures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ListWritingTest {
    private val fourQuestions = """
        - Is a door visible?
        - Is the door red?
        - Does the door fill most of the frame?
        - Is it shot straight on?
    """.trimIndent()

    private fun block(text: String) = "PICTURE: $text\n$fourQuestions"

    private fun reply(vararg texts: String) = texts.joinToString("\n\n") { block(it) }

    // Prompt and clock

    @Test
    fun promptCarriesCountTopicSkyTimeOfDayAndMonth() {
        val prompt = listPrompt(4, "Reflections", "Overcast", "evening", "October")
        assertTrue(prompt.startsWith("You are setting a photo challenge for a walk.\n"))
        assertTrue(prompt.contains("Topic: Reflections\n"))
        assertTrue(prompt.contains("Sky: Overcast. Time of day: evening. Month: October.\n"))
        assertTrue(prompt.contains("Write exactly 4 different pictures to take."))
        assertTrue(prompt.endsWith("Fit the sky and the time of day. Write nothing else."))
    }

    @Test
    fun timeOfDayFollowsTheHour() {
        assertEquals("night", timeOfDay(0))
        assertEquals("night", timeOfDay(4))
        assertEquals("morning", timeOfDay(5))
        assertEquals("morning", timeOfDay(10))
        assertEquals("midday", timeOfDay(11))
        assertEquals("midday", timeOfDay(13))
        assertEquals("afternoon", timeOfDay(14))
        assertEquals("afternoon", timeOfDay(17))
        assertEquals("evening", timeOfDay(18))
        assertEquals("evening", timeOfDay(21))
        assertEquals("night", timeOfDay(22))
        assertEquals("night", timeOfDay(23))
    }

    @Test
    fun monthNamesAreEnglish() {
        assertEquals("January", monthName(1))
        assertEquals("October", monthName(10))
        assertEquals("December", monthName(12))
        assertThrows(IllegalArgumentException::class.java) { monthName(0) }
        assertThrows(IllegalArgumentException::class.java) { monthName(13) }
    }

    @Test
    fun countsAreWords() {
        assertEquals(listOf("three", "four", "five", "six"), (3..6).map(::countWord))
    }

    // Parser

    @Test
    fun cleanReplyGivesEveryPicture() {
        val pictures = parsePictures(reply("A red door", "A wet bench", "A puddle with a reflection"))
        assertEquals(listOf("A red door", "A wet bench", "A puddle with a reflection"), pictures.map { it.text })
        assertEquals(
            listOf(
                "Is a door visible?",
                "Is the door red?",
                "Does the door fill most of the frame?",
                "Is it shot straight on?",
            ),
            pictures.first().questions,
        )
    }

    @Test
    fun shortReplyGivesFewerPictures() {
        assertEquals(listOf("A red door"), parsePictures(reply("A red door")).map { it.text })
        assertEquals(emptyList<Picture>(), parsePictures(""))
    }

    @Test
    fun replyCutOffInsideAPictureDropsThatPicture() {
        val cut = reply("A red door") + "\n\nPICTURE: A wet bench\n- Is a bench visible?\n- Is it"
        assertEquals(listOf("A red door"), parsePictures(cut).map { it.text })
    }

    @Test
    fun pictureWithThreeQuestionsIsDropped() {
        val three = "PICTURE: A wet bench\n- Is a bench visible?\n- Is it wet?\n- Is it empty?"
        assertEquals(listOf("A red door"), parsePictures(three + "\n" + reply("A red door")).map { it.text })
    }

    @Test
    fun pictureWithFiveQuestionsIsDropped() {
        val five = block("A wet bench") + "\n- Is it empty?"
        assertEquals(listOf("A red door"), parsePictures(five + "\n" + reply("A red door")).map { it.text })
    }

    @Test
    fun questionOverSixtyCharactersDropsThePicture() {
        val long = "Is the extraordinarily overgrown hedge visible on the righthand?"
        assertTrue(long.length > 60 && long.split(" ").size <= 10)
        val reply = "PICTURE: A hedge\n- Is a hedge visible?\n- $long\n- Is it green?\n- Is it close?"
        assertEquals(emptyList<Picture>(), parsePictures(reply))
    }

    @Test
    fun questionAtSixtyCharactersIsKept() {
        val sixty = "Is the " + "a".repeat(52) + "?"
        assertEquals(60, sixty.length)
        val reply = "PICTURE: A hedge\n- Is a hedge visible?\n- $sixty\n- Is it green?\n- Is it close?"
        assertEquals(1, parsePictures(reply).size)
    }

    @Test
    fun questionOverTenWordsDropsThePicture() {
        val reply = "PICTURE: A hedge\n- Is a hedge visible?\n" +
            "- Is it a big old hedge on the left of a path?\n- Is it green?\n- Is it close?"
        assertEquals(emptyList<Picture>(), parsePictures(reply))
    }

    @Test
    fun textOverTheLimitsDropsThePicture() {
        val thirteenWords = "A very long picture text that has one more word than the limit"
        assertEquals(13, thirteenWords.split(" ").size)
        val seventyOne = "A " + "b".repeat(69)
        assertEquals(71, seventyOne.length)
        assertEquals(emptyList<Picture>(), parsePictures(reply(thirteenWords, seventyOne)))
    }

    @Test
    fun boldAndNumberedPictureLinesAreRead() {
        val reply = listOf(
            "**PICTURE:** A red door",
            "1. PICTURE: A wet bench",
            "2) **Picture**: A puddle",
            "### picture: A lamp post",
            "**3. PICTURE: A gate**",
        ).joinToString("\n") { "$it\n$fourQuestions" }
        assertEquals(
            listOf("A red door", "A wet bench", "A puddle", "A lamp post", "A gate"),
            parsePictures(reply).map { it.text },
        )
    }

    @Test
    fun starAndDotBulletsAreRead() {
        val reply = "PICTURE: A red door\n* Is a door visible?\n* **Is the door red?**\n" +
            "• Does it fill the frame?\n  - Is it shot straight on?"
        val pictures = parsePictures(reply)
        assertEquals(1, pictures.size)
        assertEquals(
            listOf("Is a door visible?", "Is the door red?", "Does it fill the frame?", "Is it shot straight on?"),
            pictures.single().questions,
        )
    }

    @Test
    fun strayTextIsSkipped() {
        val reply = "Here are five pictures for your walk!\n\n" +
            "- A stray bullet before any picture\n" +
            block("A red door") + "\n\nNote: watch the traffic.\n\n" +
            "PICTURE: A wet bench\nThe bench should be in a park.\n$fourQuestions\n\nEnjoy your walk!"
        assertEquals(listOf("A red door", "A wet bench"), parsePictures(reply).map { it.text })
        assertEquals(4, parsePictures(reply).first().questions.size)
    }

    @Test
    fun repeatedTextIsKeptOnce() {
        assertEquals(listOf("A red door"), parsePictures(reply("A red door", "a red door ")).map { it.text })
    }

    @Test
    fun emptyPictureTextIsDropped() {
        assertEquals(emptyList<Picture>(), parsePictures("PICTURE:\n$fourQuestions"))
    }

    // Filling the gap

    @Test
    fun completeListKeepsWrittenPicturesFirstAndFillsFromTheBuiltInList() {
        val written = parsePictures(reply("A red door", "A wet bench"))
        val list = completeList(written, 5, Random(1))
        assertEquals(5, list.size)
        assertEquals(written, list.take(2))
        assertTrue(list.drop(2).all { it in builtInPictures })
        assertEquals(5, list.map { it.text.lowercase() }.toSet().size)
    }

    @Test
    fun completeListSkipsBuiltInTextsAlreadyUsedIgnoringCase() {
        val taken = builtInPictures.take(5).map { Picture("  " + it.text.uppercase() + " ", it.questions) }
        val list = completeList(taken, 6, Random(2))
        assertEquals(6, list.size)
        val added = list.last()
        assertTrue(added in builtInPictures.drop(5))
    }

    @Test
    fun completeListCutsALongListToTheCount() {
        val written = parsePictures(reply("A", "B", "C", "D", "E"))
        assertEquals(listOf("A", "B", "C"), completeList(written, 3).map { it.text })
    }

    @Test
    fun completeListOfNothingIsTheBuiltInList() {
        val list = completeList(emptyList(), 6, Random(3))
        assertEquals(6, list.size)
        assertTrue(list.all { it in builtInPictures })
        assertEquals(6, list.toSet().size)
    }

    // Asking

    @Test
    fun fullReplyAsksOnce() {
        val seeds = mutableListOf<Int>()
        val list = writeList("prompt", 3, { _, seed -> seeds += seed; reply("A", "B", "C") })
        assertEquals(listOf("A", "B", "C"), list.map { it.text })
        assertEquals(1, seeds.size)
    }

    @Test
    fun shortReplyAsksAgainWithANewSeedAndKeepsTheBetterReply() {
        val seeds = mutableListOf<Int>()
        val replies = ArrayDeque(listOf(reply("A"), reply("B", "C")))
        val list = writeList("prompt", 4, { _, seed -> seeds += seed; replies.removeFirst() }, Random(4))
        assertEquals(2, seeds.size)
        assertTrue(seeds[0] != seeds[1])
        assertEquals(4, list.size)
        assertEquals(listOf("B", "C"), list.take(2).map { it.text })
        assertTrue(list.drop(2).all { it in builtInPictures })
    }

    @Test
    fun worseSecondReplyKeepsTheFirst() {
        val replies = ArrayDeque(listOf(reply("A", "B"), reply("C")))
        val list = writeList("prompt", 4, { _, _ -> replies.removeFirst() })
        assertEquals(listOf("A", "B"), list.take(2).map { it.text })
        assertEquals(4, list.size)
    }

    @Test
    fun failedCallsFallBackToTheBuiltInList() {
        var calls = 0
        val list = writeList("prompt", 5, { _, _ -> calls++; error("Model not loaded") })
        assertEquals(2, calls)
        assertEquals(5, list.size)
        assertTrue(list.all { it in builtInPictures })
    }

    @Test
    fun failedSecondCallKeepsTheFirstReply() {
        var calls = 0
        val list = writeList("prompt", 3, { _, _ -> if (calls++ == 0) reply("A") else error("Boom") })
        assertEquals("A", list.first().text)
        assertEquals(3, list.size)
    }

    @Test
    fun everyCallPassesThePrompt() {
        val prompts = mutableListOf<String>()
        writeList("the prompt", 3, { p, _ -> prompts += p; "" })
        assertEquals(listOf("the prompt", "the prompt"), prompts)
    }
}
