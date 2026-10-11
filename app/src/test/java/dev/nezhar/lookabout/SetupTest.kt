package dev.nezhar.lookabout

import dev.nezhar.lookabout.walk.Outcome
import dev.nezhar.lookabout.walk.builtInPictures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupTest {
    @Test
    fun defaultsAreFivePicturesFirstTopicAndNoSky() {
        val choices = SetupChoices()
        assertEquals(5, choices.count)
        assertEquals("Autumn colour", choices.topicText)
        assertFalse(choices.ownTopic)
        assertNull(choices.sky)
    }

    @Test
    fun setupOffersTheCountsTopicsAndSkies() {
        assertEquals(listOf(3, 4, 5, 6), PICTURE_COUNTS)
        assertEquals(
            listOf("Autumn colour", "Reflections", "Light and shadow", "Doors and windows", "Small details", "Street life"),
            SUGGESTED_TOPICS,
        )
        assertEquals(listOf("Clear", "Some cloud", "Overcast", "Rain", "Fog"), SKY_OPTIONS)
    }

    @Test
    fun makeMyListNeedsTheModelAndASky() {
        val noSky = SetupChoices()
        assertFalse(noSky.canMakeList(modelReady = true))
        val withSky = noSky.withSky("Clear")
        assertTrue(withSky.canMakeList(modelReady = true))
        assertFalse(withSky.canMakeList(modelReady = false))
    }

    @Test
    fun yourOwnNeedsNonBlankText() {
        val own = SetupChoices().withSky("Fog").withOwnTopic()
        assertFalse(own.canMakeList(modelReady = true))
        assertFalse(own.withOwnText("   ").canMakeList(modelReady = true))
        val named = own.withOwnText("  Old signs ")
        assertTrue(named.canMakeList(modelReady = true))
        assertEquals("Old signs", named.topicText)
    }

    @Test
    fun yourOwnTextIsCutToThirtyCharacters() {
        val text = "a".repeat(40)
        assertEquals(MAX_OWN_TOPIC_CHARS, SetupChoices().withOwnText(text).ownText.length)
        assertEquals("Short", SetupChoices().withOwnText("Short").ownText)
    }

    @Test
    fun choosingASuggestionLeavesYourOwnAndKeepsTheText() {
        val own = SetupChoices().withOwnTopic().withOwnText("Old signs")
        val suggestion = own.withTopic("Reflections")
        assertFalse(suggestion.ownTopic)
        assertEquals("Reflections", suggestion.topicText)
        assertEquals("Old signs", suggestion.withOwnTopic().topicText)
    }

    @Test
    fun countOutsideThreeToSixIsIgnored() {
        assertEquals(3, SetupChoices().withCount(3).count)
        assertEquals(6, SetupChoices().withCount(6).count)
        assertEquals(5, SetupChoices().withCount(2).count)
        assertEquals(5, SetupChoices().withCount(7).count)
    }

    @Test
    fun backReturnsToSetupFromListSummaryAndDebug() {
        assertEquals(Screen.Setup, Screen.List.backTarget)
        assertEquals(Screen.Setup, Screen.Summary.backTarget)
        assertEquals(Screen.Setup, Screen.Debug.backTarget)
    }

    @Test
    fun backIsIgnoredDuringAWalkAndLeavesFromSetup() {
        assertEquals(Screen.Walk, Screen.Walk.backTarget)
        assertNull(Screen.Setup.backTarget)
    }

    @Test
    fun startWalkMakesAWalkFromTheList() {
        val pictures = builtInPictures.take(4)
        val walk = ChallengeList("Reflections", "Rain", pictures).startWalk(1234L)
        assertEquals("Reflections", walk.topic)
        assertEquals("Rain", walk.sky)
        assertEquals(1234L, walk.startedAtMillis)
        assertEquals(pictures, walk.shots.map { it.picture })
        assertEquals(0, walk.currentIndex)
        assertTrue(walk.shots.all { it.outcome == Outcome.PENDING })
    }
}
