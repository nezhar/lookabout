package dev.nezhar.lookabout

import dev.nezhar.lookabout.walk.Picture
import dev.nezhar.lookabout.walk.Walk

/** The screen the app shows. One value of this decides it: there is no navigation library. */
enum class Screen {
    Setup,
    List,
    Walk,
    Summary,

    /** Model test and the photo check, opened by a long press on the Setup title. */
    Debug,

    /** Every archived walk, newest first, opened by Past walks on Setup. */
    PastWalks,

    /** One archived walk on the Summary screen, read-only, opened from Past walks. */
    PastSummary,
}

/**
 * Where system Back leads from this screen: List, Summary, Debug and Past walks return to Setup, a
 * past walk's Summary returns to Past walks, a walk ignores Back (it returns itself), and on Setup
 * it is null, so Back leaves the app as usual.
 */
val Screen.backTarget: Screen?
    get() = when (this) {
        Screen.Setup -> null
        Screen.Walk -> Screen.Walk
        Screen.List, Screen.Summary, Screen.Debug, Screen.PastWalks -> Screen.Setup
        Screen.PastSummary -> Screen.PastWalks
    }

/** The numbers of pictures Setup offers. */
val PICTURE_COUNTS: List<Int> = (Walk.MIN_SHOTS..Walk.MAX_SHOTS).toList()

/** The topic suggestions on Setup, in order. The first is preselected. */
val SUGGESTED_TOPICS: List<String> = listOf(
    "Autumn colour",
    "Reflections",
    "Light and shadow",
    "Doors and windows",
    "Small details",
    "Street life",
)

/** The sky options on Setup, in order. */
val SKY_OPTIONS: List<String> = listOf("Clear", "Some cloud", "Overcast", "Rain", "Fog")

/** Most characters in a "Your own" topic. */
const val MAX_OWN_TOPIC_CHARS = 30

/**
 * What the walker chose on Setup.
 *
 * @property count the number of pictures, 3 to 6.
 * @property topic the chosen suggestion; ignored while [ownTopic] is chosen.
 * @property ownTopic true when "Your own" is chosen.
 * @property ownText the "Your own" text as typed, at most [MAX_OWN_TOPIC_CHARS] characters.
 * @property sky the chosen sky, or null until one is chosen.
 * @property checkAtEnd true when "Check photos at the end" is on: the walk takes every photo first
 *   and checks them all at Finish walk.
 */
data class SetupChoices(
    val count: Int = 5,
    val topic: String = SUGGESTED_TOPICS.first(),
    val ownTopic: Boolean = false,
    val ownText: String = "",
    val sky: String? = null,
    val checkAtEnd: Boolean = false,
) {
    /** The topic the list is made for: the trimmed "Your own" text or the chosen suggestion. */
    val topicText: String get() = if (ownTopic) ownText.trim() else topic

    /** True when a sky is chosen and, with "Your own", the text is not blank. */
    val isComplete: Boolean get() = sky != null && topicText.isNotEmpty()

    /** Make my list is enabled once the model is ready and the choices are complete. */
    fun canMakeList(modelReady: Boolean): Boolean = modelReady && isComplete

    fun withCount(count: Int): SetupChoices = if (count in PICTURE_COUNTS) copy(count = count) else this

    /** Chooses a suggestion, which also leaves "Your own". */
    fun withTopic(topic: String): SetupChoices = copy(topic = topic, ownTopic = false)

    /** Chooses "Your own"; the text typed earlier is kept. */
    fun withOwnTopic(): SetupChoices = copy(ownTopic = true)

    /** The "Your own" text, cut to [MAX_OWN_TOPIC_CHARS] characters. */
    fun withOwnText(text: String): SetupChoices = copy(ownText = text.take(MAX_OWN_TOPIC_CHARS))

    fun withSky(sky: String): SetupChoices = copy(sky = sky)

    fun withCheckAtEnd(on: Boolean): SetupChoices = copy(checkAtEnd = on)
}

/** The challenge shown on List: the topic and sky from Setup and the pictures to find. */
data class ChallengeList(
    val topic: String,
    val sky: String,
    val pictures: List<Picture>,
) {
    /** A new walk at its first picture; with [checkAtEnd] its photos are checked at Finish walk. */
    fun startWalk(startedAtMillis: Long, checkAtEnd: Boolean = false): Walk =
        Walk.start(topic, sky, pictures, startedAtMillis, checkAtEnd)
}

/** The line under the topic on List, such as "Overcast · 5 pictures". */
fun listMetaText(sky: String, count: Int): String = "$sky · $count pictures"
