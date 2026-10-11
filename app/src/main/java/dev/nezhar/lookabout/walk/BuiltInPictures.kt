package dev.nezhar.lookabout.walk

import kotlin.random.Random

/** Most words in a picture's text. A word is whatever stands between spaces. */
const val MAX_TEXT_WORDS = 12

/** Most characters in a picture's text. */
const val MAX_TEXT_CHARS = 70

/** Most words in a question. */
const val MAX_QUESTION_WORDS = 10

/** Most characters in a question. */
const val MAX_QUESTION_CHARS = 60

/**
 * The built-in pictures, so a walk never depends on the model writing the list. The first five
 * are the sample walk from the design brief.
 */
val builtInPictures: List<Picture> = listOf(
    Picture(
        "A reflection with someone walking past",
        listOf(
            "Is water or glass visible?",
            "Is there a clear reflection?",
            "Is a person in the frame?",
            "Was it taken from a low angle?",
        ),
    ),
    Picture(
        "Fallen leaves on something man-made",
        listOf(
            "Are fallen leaves visible?",
            "Are they lying on a man-made object?",
            "Are the leaves the main subject?",
            "Is it a close-up?",
        ),
    ),
    Picture(
        "A doorway with one strong colour",
        listOf(
            "Is a door visible?",
            "Does one colour dominate?",
            "Does the doorway fill most of the frame?",
            "Is it shot straight on?",
        ),
    ),
    Picture(
        "Looking straight up through bare branches",
        listOf(
            "Are branches visible?",
            "Is the sky visible?",
            "Is the camera pointing upwards?",
            "Is the frame free of buildings?",
        ),
    ),
    Picture(
        "A lit window on a grey street",
        listOf(
            "Is a window visible?",
            "Is there light inside the window?",
            "Is a street visible?",
            "Is the rest of the scene grey or dim?",
        ),
    ),
    Picture(
        "Something small and overlooked, shot so close it fills the whole frame",
        listOf(
            "Is it a close-up?",
            "Does the subject fill at least half of the frame?",
            "Is the subject a small object?",
            "Is the background blurred or plain?",
        ),
    ),
    Picture(
        "A bicycle leaning against a wall or fence",
        listOf(
            "Is a bicycle visible?",
            "Is it leaning against a wall or fence?",
            "Is the whole bicycle in the frame?",
            "Is the frame free of people?",
        ),
    ),
    Picture(
        "An empty bench with trees behind it",
        listOf(
            "Is a bench visible?",
            "Is the bench empty?",
            "Is the whole bench in the frame?",
            "Are trees or plants visible behind it?",
        ),
    ),
)

/** The built-in pictures whose text is not among [texts], in list order. */
fun builtInPicturesExcept(texts: Collection<String>): List<Picture> =
    builtInPictures.filter { it.text !in texts }

/** [count] different built-in pictures in random order. [count] must be 3 to 6. */
fun pickBuiltInPictures(count: Int, random: Random = Random.Default): List<Picture> {
    require(count in Walk.MIN_SHOTS..Walk.MAX_SHOTS) {
        "A walk has ${Walk.MIN_SHOTS} to ${Walk.MAX_SHOTS} pictures, not $count"
    }
    return builtInPictures.shuffled(random).take(count)
}
