package dev.nezhar.lookabout

import dev.nezhar.lookabout.walk.Walk
import dev.nezhar.lookabout.walk.builtInPictures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WalkNoticeTest {
    private val walk = Walk.start("Reflections", "Rain", builtInPictures.take(5), 0L)

    @Test
    fun showsThePictureTextAndPosition() {
        val second = walk.skip()
        assertEquals(WalkNotice(second.shots[1].text, "2 of 5"), walkNotice(second, Screen.Walk))
        assertEquals(WalkNotice(walk.shots[0].text, "1 of 5"), walkNotice(walk, Screen.Walk))
    }

    @Test
    fun noNoticeWithoutAWalkOrOffWalk() {
        assertNull(walkNotice(null, Screen.Walk))
        assertNull(walkNotice(walk, Screen.Summary))
        assertNull(walkNotice(walk, Screen.Setup))
    }

    @Test
    fun noNoticeOnceTheWalkIsFinished() {
        val pastTheEnd = walk.skip().skip().skip().skip().skip()
        assertNull(walkNotice(pastTheEnd, Screen.Walk))
        val acceptedLast = walk.skip().skip().skip().skip()
            .photoRequested("/p/5.jpg").photoTaken()
            .recordCheck(listOf(true, true, true, true), 1000)
        assertTrue(acceptedLast.isFinished)
        assertNull(walkNotice(acceptedLast, Screen.Walk))
    }

    @Test
    fun aNotYetLastPictureStillHasANotice() {
        val notYetLast = walk.skip().skip().skip().skip()
            .photoRequested("/p/5.jpg").photoTaken()
            .recordCheck(listOf(false, false, false, false), 1000)
        assertEquals("5 of 5", walkNotice(notYetLast, Screen.Walk)?.position)
    }

    @Test
    fun aPictureWithoutAPhotoWaitsForOne() {
        assertTrue(walk.waitsForPhoto())
        assertTrue(walk.skip().waitsForPhoto())
    }

    @Test
    fun aPictureWithAPhotoOrWithTheCameraOpenDoesNotWait() {
        val cameraOpen = walk.photoRequested("/p/1.jpg")
        assertFalse(cameraOpen.waitsForPhoto())
        assertFalse(cameraOpen.photoTaken().waitsForPhoto())
        val checked = cameraOpen.photoTaken().recordCheck(listOf(false, false, false, false), 1000)
        assertFalse(checked.waitsForPhoto())
    }

    @Test
    fun aWalkPastTheLastPictureDoesNotWait() {
        assertFalse(walk.skip().skip().skip().skip().skip().waitsForPhoto())
    }
}
