package dev.nezhar.lookabout.walk

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WalkJsonTest {
    private val questions = listOf("Is it \"red\"?", "Is the sky visible?", "Is it a close-up?", "Übers Wasser?")

    private fun pictures(count: Int) = (1..count).map { Picture("Picture $it", questions) }

    private fun roundTrip(saved: SavedWalk): SavedWalk? = decodeSavedWalk(encodeSavedWalk(saved))

    /** Accepted with a correction, skipped, not yet, a photo waiting for its check, and a pending camera photo. */
    private fun walkWithEverything(): Walk =
        Walk.start("Autumn colour / \"wet\" streets", "Some cloud", pictures(6), 1_760_000_000_123L)
            .photoRequested("/pictures/photo-1.jpg").photoTaken()
            .recordCheck(listOf(true, false, false, true), 9_412L)
            .flip(1)
            .nextPicture()
            .skip()
            .photoRequested("/pictures/photo-3.jpg").photoTaken()
            .recordCheck(listOf(false, false, false, true), 8_000L)
            .flip(3)
            .skip()
            .photoRequested("/pictures/photo-4.jpg").photoTaken()
            .photoRequested("/pictures/photo-5.jpg")

    @Test
    fun aWalkWithEveryKindOfShotRoundTrips() {
        val walk = walkWithEverything()
        // The walk holds what the test means to cover.
        assertEquals(Outcome.ACCEPTED, walk.shots[0].outcome)
        assertEquals(listOf(false, true, false, false), walk.shots[0].corrected)
        assertEquals(Outcome.SKIPPED, walk.shots[1].outcome)
        assertNull(walk.shots[1].photoPath)
        assertEquals(Outcome.NOT_YET, walk.shots[2].outcome)
        assertEquals(Outcome.PENDING, walk.shots[3].outcome)
        assertEquals("/pictures/photo-4.jpg", walk.shots[3].photoPath)
        assertNull(walk.shots[3].checkMillis)
        assertEquals(3, walk.currentIndex)
        assertEquals("/pictures/photo-5.jpg", walk.pendingPhotoPath)

        val saved = SavedWalk(walk, onSummary = false)
        assertEquals(saved, roundTrip(saved))
    }

    @Test
    fun aNewWalkWithNullFieldsRoundTrips() {
        val walk = Walk.start("Reflections", "Rain", pictures(3), 0L)
        val saved = SavedWalk(walk, onSummary = false)
        val back = roundTrip(saved)
        assertEquals(saved, back)
        assertNull(back!!.walk.pendingPhotoPath)
        assertNull(back.walk.shots[0].photoPath)
        assertNull(back.walk.shots[0].checkMillis)
    }

    @Test
    fun aFinishedWalkOnSummaryRoundTrips() {
        val walk = Walk.start("Small details", "Fog", pictures(3), 42L).skip().skip().skip()
        assertEquals(3, walk.currentIndex)
        val saved = SavedWalk(walk, onSummary = true)
        assertEquals(saved, roundTrip(saved))
    }

    @Test
    fun aWalkCheckedAtEndRoundTripsInTheMiddleOfItsCheck() {
        // Past the last picture, one photo checked and two still to check.
        val walk = Walk.start("Reflections", "Rain", pictures(4), 7L, checkAtEnd = true)
            .photoRequested("/pictures/photo-1.jpg").photoTaken().nextPicture()
            .skip()
            .photoRequested("/pictures/photo-3.jpg").photoTaken().nextPicture()
            .photoRequested("/pictures/photo-4.jpg").photoTaken().nextPicture()
            .recordCheck(listOf(true, true, false, false), 5_000L, shotIndex = 0)
        assertEquals(4, walk.currentIndex)
        assertTrue(walk.checkAtEnd)
        assertEquals(listOf(2, 3), walk.shotsToCheck)

        val saved = SavedWalk(walk, onSummary = false)
        val back = roundTrip(saved)
        assertEquals(saved, back)
        assertEquals(listOf(2, 3), back!!.walk.shotsToCheck)
    }

    @Test
    fun aWalkSavedBeforeCheckAtEndExistedLoadsWithItOff() {
        val back = decodeSavedWalk(edited { remove("checkAtEnd") })
        assertEquals(false, back?.walk?.checkAtEnd)
    }

    @Test
    fun theCheckAtEndSettingIsSaved() {
        val walk = Walk.start("Reflections", "Rain", pictures(3), 0L, checkAtEnd = true)
        assertEquals(true, JSONObject(encodeSavedWalk(SavedWalk(walk, onSummary = false))).getBoolean("checkAtEnd"))
        assertEquals(false, JSONObject(edited { }).getBoolean("checkAtEnd"))
    }

    @Test
    fun garbageIsNotLoaded() {
        listOf("", "not json", "null", "[]", "{}", "{\"version\": 1}", "{\"version\": \"x\"}").forEach {
            assertNull(it, decodeSavedWalk(it))
        }
    }

    @Test
    fun aCutOffFileIsNotLoaded() {
        val text = encodeSavedWalk(SavedWalk(walkWithEverything(), onSummary = false))
        assertNull(decodeSavedWalk(text.substring(0, text.length / 2)))
        assertNull(decodeSavedWalk(text.dropLast(1)))
    }

    /** A saved new walk of three shots, changed by [change] after encoding. */
    private fun edited(change: JSONObject.() -> Unit): String {
        val walk = Walk.start("Reflections", "Rain", pictures(3), 0L)
        return JSONObject(encodeSavedWalk(SavedWalk(walk, onSummary = false))).apply(change).toString()
    }

    private val JSONObject.firstShot: JSONObject get() = getJSONArray("shots").getJSONObject(0)

    @Test
    fun theEditHelperKeepsAValidWalk() {
        assertEquals(3, decodeSavedWalk(edited { })?.walk?.shots?.size)
    }

    @Test
    fun anotherVersionIsNotLoaded() {
        assertNull(decodeSavedWalk(edited { put("version", 2) }))
    }

    @Test
    fun aWalkTheRulesRejectIsNotLoaded() {
        assertNull(decodeSavedWalk(edited { getJSONArray("shots").remove(2) }))
        assertNull(decodeSavedWalk(edited { put("currentIndex", 4) }))
        assertNull(decodeSavedWalk(edited { firstShot.getJSONArray("answers").put(true).put(false).put(true) }))
        assertNull(decodeSavedWalk(edited { firstShot.getJSONArray("questions").remove(1) }))
        assertNull(decodeSavedWalk(edited { firstShot.getJSONArray("corrected").remove(0) }))
        assertNull(decodeSavedWalk(edited { firstShot.put("outcome", "MAYBE") }))
        assertNull(decodeSavedWalk(edited { remove("topic") }))
        assertNull(decodeSavedWalk(edited { firstShot.put("checkMillis", "soon") }))
        assertNull(decodeSavedWalk(edited { put("checkAtEnd", "soon") }))
    }
}
