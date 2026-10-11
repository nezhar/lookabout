package dev.nezhar.lookabout

/** The long edge, in pixels, of the photo copy the model gets. The Gallery app uses the same. */
const val CHECK_PHOTO_MAX_EDGE = 1024

/** The check screen's four questions before they are edited. */
val DEFAULT_CHECK_QUESTIONS = listOf(
    "Is a person in the frame?",
    "Is the sky visible?",
    "Are trees or plants visible?",
    "Is a building visible?",
)

/**
 * The outcome of one photo check.
 *
 * @property answers the four answers in question order; empty when the check failed.
 * @property modelMs how long the model call took, in milliseconds; 0 when it was not reached.
 * @property reply the model's raw reply; empty when it was not reached.
 * @property failure why the check failed, or null when it has answers.
 */
data class CheckResult(
    val answers: List<Boolean>,
    val modelMs: Long,
    val reply: String,
    val failure: String?,
) {
    val failed: Boolean get() = failure != null

    companion object {
        fun failed(message: String, modelMs: Long = 0, reply: String = ""): CheckResult =
            CheckResult(emptyList(), modelMs, reply, message)
    }
}

/**
 * Asks the check prompt for [questions] through [ask] and reads the four answers.
 * [ask] gets the prompt and returns the model's reply; [nowMs] is the clock used to time it.
 * A reply without any readable answer is a failed check. Exceptions from [ask] propagate.
 */
fun runCheck(
    questions: List<String>,
    ask: (prompt: String) -> String,
    nowMs: () -> Long = { System.nanoTime() / 1_000_000 },
): CheckResult {
    val prompt = checkPrompt(questions)
    val start = nowMs()
    val reply = ask(prompt)
    val modelMs = nowMs() - start
    val answers = readAnswers(reply)
        ?: return CheckResult.failed("No readable answer in the reply: \"${reply.trim()}\"", modelMs, reply)
    return CheckResult(answers, modelMs, reply, null)
}

/**
 * The size to decode a [width] x [height] image at so its long edge is at most [maxEdge],
 * keeping the aspect ratio. Images that already fit keep their size. Each side is at least 1.
 */
fun scaledSize(width: Int, height: Int, maxEdge: Int = CHECK_PHOTO_MAX_EDGE): Pair<Int, Int> {
    require(width > 0 && height > 0) { "Image size must be positive, not $width x $height" }
    require(maxEdge > 0) { "maxEdge must be positive, not $maxEdge" }
    val long = maxOf(width, height)
    if (long <= maxEdge) return width to height
    fun scale(side: Int): Int = maxOf(1L, (side.toLong() * maxEdge + long / 2) / long).toInt()
    return if (width >= height) maxEdge to scale(height) else scale(width) to maxEdge
}

/** Milliseconds as seconds with one decimal, rounded half up: 9412 is "9.4 s". */
fun formatSeconds(ms: Long): String {
    val tenths = (ms + 50) / 100
    return "${tenths / 10}.${tenths % 10} s"
}

/** The coverage as shown on screen, such as "75%, 3 of 4". */
fun coverageText(answers: List<Boolean>): String =
    "${coveragePercent(answers)}%, ${answers.count { it }} of ${answers.size}"
