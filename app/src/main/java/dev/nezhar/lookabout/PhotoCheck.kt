package dev.nezhar.lookabout

private val whitespace = Regex("""\s+""")

private val answerLine = Regex("""^\W*([0-9]{1,2})\s*[:.)\-]\s*\W*(YES|NO)\b""", RegexOption.IGNORE_CASE)

/**
 * The check prompt for one photo, with its four questions numbered 1 to 4.
 *
 * Send it with the photo at temperature 0.2, thinking off and at most 64 output tokens:
 * `Gemma.ask(prompt, imagePath, temperature = 0.2, maxTokens = 64)`.
 * Each question is put on one line, so line breaks inside a question become spaces.
 */
fun checkPrompt(questions: List<String>): String {
    require(questions.size == 4) { "A photo check has four questions, not ${questions.size}" }
    return "Look at the photo. Answer each question with YES or NO only.\n" +
        "Write exactly one line per question, in this form: 1: YES\n\n" +
        questions.mapIndexed { index, question ->
            "${index + 1}. ${question.trim().replace(whitespace, " ")}"
        }.joinToString("\n")
}

/**
 * The answers by question number. A question the reply does not answer is missing.
 *
 * Lenient about decoration: the reply is split on line breaks, commas and semicolons, bold and
 * italic marks are ignored, and the number may be followed by `:` `.` `)` or `-`.
 */
fun parseAnswers(reply: String): Map<Int, Boolean> =
    reply.split('\r', '\n', ',', ';')
        .mapNotNull { part -> answerLine.find(part.filterNot { it == '*' || it == '_' }.trim()) }
        .associate { it.groupValues[1].toInt() to it.groupValues[2].equals("YES", ignoreCase = true) }

/** Four answers in order. A missing answer counts as NO. */
fun answersFor(parsed: Map<Int, Boolean>, count: Int = 4): List<Boolean> =
    (1..count).map { parsed[it] ?: false }

/**
 * Gemma's four answers to a check reply, in question order, with a missing or unreadable answer as NO.
 *
 * Null when the reply has no readable answer to any of the four questions: the check failed,
 * which is not the same as four NO answers.
 */
fun readAnswers(reply: String): List<Boolean>? {
    val parsed = parseAnswers(reply).filterKeys { it in 1..4 }
    return if (parsed.isEmpty()) null else answersFor(parsed)
}

/** The share of YES answers in percent: 0, 25, 50, 75 or 100 for four answers. No answers is 0. */
fun coveragePercent(answers: List<Boolean>): Int =
    if (answers.isEmpty()) 0 else answers.count { it } * 100 / answers.size

/** Accepted at 50% coverage or more, so two of four YES answers. No answers is never accepted. */
fun accepted(answers: List<Boolean>): Boolean =
    answers.isNotEmpty() && answers.count { it } * 2 >= answers.size
