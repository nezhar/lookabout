package dev.nezhar.lookabout

import dev.nezhar.lookabout.walk.MAX_QUESTION_CHARS
import dev.nezhar.lookabout.walk.MAX_QUESTION_WORDS
import dev.nezhar.lookabout.walk.MAX_TEXT_CHARS
import dev.nezhar.lookabout.walk.MAX_TEXT_WORDS
import dev.nezhar.lookabout.walk.Picture
import dev.nezhar.lookabout.walk.QUESTIONS_PER_PICTURE
import dev.nezhar.lookabout.walk.builtInPictures
import kotlin.random.Random

private val spaces = Regex("""\s+""")

/** A `PICTURE:` line, after any heading marks, bold marks or numbering such as `1.` or `2)`. */
private val pictureLine =
    Regex("""^(?:[#>*_\s]|\d+[.)])*picture\s*[*_]*\s*:\s*(.*)$""", RegexOption.IGNORE_CASE)

/** A question line: `-`, `*` or `•`, a space, then the question. */
private val questionLine = Regex("""^[-*•]\s+(.*)$""")

private val monthNames = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

private val countWords = mapOf(3 to "three", 4 to "four", 5 to "five", 6 to "six")

/**
 * The time of day for an hour of the clock (0 to 23): morning from 5, midday from 11,
 * afternoon from 14, evening from 18 and night from 22.
 */
fun timeOfDay(hour: Int): String = when (hour) {
    in 5..10 -> "morning"
    in 11..13 -> "midday"
    in 14..17 -> "afternoon"
    in 18..21 -> "evening"
    else -> "night"
}

/** The English name of a month, 1 (January) to 12 (December). */
fun monthName(month: Int): String {
    require(month in 1..12) { "A month is 1 to 12, not $month" }
    return monthNames[month - 1]
}

/** The number of pictures as an English word for List's progress line ("five"). */
fun countWord(count: Int): String = countWords[count] ?: count.toString()

/**
 * The prompt that asks the model for [count] pictures. Send it with a temperature of about 0.8,
 * up to 600 tokens and a new random seed on every call, so New list does not repeat itself.
 */
fun listPrompt(count: Int, topic: String, sky: String, timeOfDay: String, month: String): String =
    """
    |You are setting a photo challenge for a walk.
    |Topic: ${topic.trim()}
    |Sky: $sky. Time of day: $timeOfDay. Month: $month.
    |
    |Write exactly $count different pictures to take.
    |For each one, write a line that starts with "PICTURE:" and has at most 12 words.
    |Under it, write exactly four lines that start with "- ".
    |Each is a yes/no question of at most 10 words that someone can answer by looking at the photo.
    |Ask only about things that are visible. Do not ask about quality, mood or beauty.
    |Fit the sky and the time of day. Write nothing else.
    """.trimMargin()

/**
 * The pictures in a reply to [listPrompt], in order, keeping only those with exactly four
 * questions inside the word and character limits. A later picture with the same text as an
 * earlier one (ignoring case) is dropped.
 *
 * Lenient about decoration: case, bold marks and numbering before `PICTURE:` are ignored, the
 * bullet may be `-`, `*` or `•`, and any other line is skipped.
 */
fun parsePictures(reply: String): List<Picture> {
    val pictures = mutableListOf<Picture>()
    var text: String? = null
    val questions = mutableListOf<String>()

    fun close() {
        val current = text
        if (current != null && fitsText(current) && questions.size == QUESTIONS_PER_PICTURE &&
            questions.all(::fitsQuestion) && pictures.none { sameText(it.text, current) }
        ) {
            pictures += Picture(current, questions.toList())
        }
        text = null
        questions.clear()
    }

    for (raw in reply.lines()) {
        val line = raw.trim()
        val picture = pictureLine.matchEntire(line)
        if (picture != null) {
            close()
            text = clean(picture.groupValues[1])
            continue
        }
        val question = questionLine.matchEntire(line)
        if (question != null && text != null) questions += clean(question.groupValues[1])
    }
    close()
    return pictures
}

/**
 * Exactly [count] pictures: the [written] ones first, then built-in pictures in random order
 * whose text is not already in the list (ignoring case and surrounding spaces).
 */
fun completeList(written: List<Picture>, count: Int, random: Random = Random.Default): List<Picture> {
    val pictures = mutableListOf<Picture>()
    for (picture in written + builtInPictures.shuffled(random)) {
        if (pictures.size == count) break
        if (pictures.none { sameText(it.text, picture.text) }) pictures += picture
    }
    return pictures
}

/**
 * Asks for [count] pictures with [prompt] and returns exactly [count] pictures.
 *
 * [ask] sends a prompt with a seed and returns the reply; every call gets a new seed from
 * [random]. If fewer than [count] pictures survive [parsePictures], it asks once more and keeps
 * the reply with more of them. [completeList] fills any gap from the built-in list, and a failed
 * call (an exception) counts as a reply without pictures, so the built-in list fills it all.
 */
fun writeList(
    prompt: String,
    count: Int,
    ask: (prompt: String, seed: Int) -> String,
    random: Random = Random.Default,
): List<Picture> {
    fun attempt(): List<Picture> =
        try {
            parsePictures(ask(prompt, random.nextInt()))
        } catch (e: Exception) {
            emptyList()
        }

    var written = attempt()
    if (written.size < count) {
        val again = attempt()
        if (again.size > written.size) written = again
    }
    return completeList(written, count, random)
}

private fun clean(text: String): String =
    text.trim().trim('*', '_').trim().replace(spaces, " ")

private fun words(text: String): Int = text.split(' ').count { it.isNotEmpty() }

private fun fitsText(text: String): Boolean =
    text.isNotEmpty() && words(text) <= MAX_TEXT_WORDS && text.length <= MAX_TEXT_CHARS

private fun fitsQuestion(question: String): Boolean =
    question.isNotEmpty() && words(question) <= MAX_QUESTION_WORDS &&
        question.length <= MAX_QUESTION_CHARS

private fun sameText(a: String, b: String): Boolean = a.trim().equals(b.trim(), ignoreCase = true)
