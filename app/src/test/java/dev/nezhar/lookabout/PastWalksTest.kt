package dev.nezhar.lookabout

import dev.nezhar.lookabout.walk.ArchivedWalkSummary
import dev.nezhar.lookabout.walk.Walk
import dev.nezhar.lookabout.walk.builtInPictures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import java.util.Locale

class PastWalksTest {
    private fun summary(
        id: Long,
        startedAtMillis: Long = id,
        topic: String = "Autumn colour",
        accepted: Int = 3,
        pictures: Int = 5,
        thumbnail: String? = "/pictures/photo-$id.jpg",
    ) = ArchivedWalkSummary(
        id = id,
        startedAtMillis = startedAtMillis,
        topic = topic,
        sky = "Overcast",
        acceptedCount = accepted,
        pictureCount = pictures,
        agreedCount = 0,
        checkCount = 0,
        thumbnailPath = thumbnail,
    )

    @Test
    fun aRowHasTheDateTopicTotalAndThumbnail() {
        // 2023-11-14T22:13:20Z
        val row = pastWalkRow(
            summary(1_700_000_000_000L, topic = "Autumn in the city", accepted = 3, pictures = 5),
            ZoneOffset.UTC,
            Locale.US,
        )
        assertEquals(
            PastWalkRow(
                id = 1_700_000_000_000L,
                date = "14 November 2023",
                topic = "Autumn in the city",
                acceptedText = "3 of 5 accepted",
                thumbnailPath = "/pictures/photo-1700000000000.jpg",
            ),
            row,
        )
    }

    @Test
    fun aWalkWithoutAnAcceptedPhotoHasNoThumbnail() {
        val row = pastWalkRow(summary(1L, accepted = 0, pictures = 3, thumbnail = null), ZoneOffset.UTC, Locale.US)
        assertEquals("0 of 3 accepted", row.acceptedText)
        assertEquals(null, row.thumbnailPath)
    }

    @Test
    fun rowsAreNewestFirst() {
        val rows = pastWalkRows(
            listOf(summary(2_000L), summary(3_000L), summary(1_000L)),
            ZoneOffset.UTC,
            Locale.US,
        )
        assertEquals(listOf(3_000L, 2_000L, 1_000L), rows.map { it.id })
    }

    @Test
    fun walksStartedAtTheSameTimeAreOrderedByIdHighestFirst() {
        val rows = pastWalkRows(
            listOf(summary(5L, startedAtMillis = 100L), summary(9L, startedAtMillis = 100L)),
            ZoneOffset.UTC,
            Locale.US,
        )
        assertEquals(listOf(9L, 5L), rows.map { it.id })
    }

    @Test
    fun noWalksGiveNoRows() {
        assertTrue(pastWalkRows(emptyList(), ZoneOffset.UTC, Locale.US).isEmpty())
    }

    @Test
    fun setupOffersPastWalksOnlyOnceAWalkIsKnown() {
        assertFalse(offersPastWalks(null))
        assertFalse(offersPastWalks(emptyList()))
        assertTrue(offersPastWalks(listOf(summary(1L))))
    }

    @Test
    fun onlyTheAppsOwnPhotosAreDeleted() {
        val walk = Walk.start("Autumn colour", "Clear", builtInPictures.take(4), 1L)
            .photoRequested("/data/pictures/photo-1.jpg").photoTaken()
            .recordCheck(listOf(true, true, true, false), 6_000L)
            .nextPicture()
            .skip()
            .photoRequested("/sdcard/DCIM/photo-3.jpg").photoTaken()
            .recordCheck(listOf(true, true, true, true), 6_000L)
            .nextPicture()
            .photoRequested("/data/pictures/sub/photo-4.jpg").photoTaken()
            .recordCheck(listOf(true, false, false, false), 6_000L)
            .nextPicture()

        assertEquals(
            listOf("/data/pictures/photo-1.jpg", "/sdcard/DCIM/photo-3.jpg", "/data/pictures/sub/photo-4.jpg"),
            walk.shots.mapNotNull { it.photoPath },
        )
        assertEquals(listOf("/data/pictures/photo-1.jpg"), appPhotoPaths(walk, "/data/pictures"))
        assertEquals(listOf("/data/pictures/photo-1.jpg"), appPhotoPaths(walk, "/data/pictures/"))
    }

    @Test
    fun retakeDropsTheReplacedPhoto() {
        val first = Walk.start("Autumn colour", "Clear", builtInPictures.take(3), 1L)
            .photoRequested("/data/pictures/photo-1.jpg").photoTaken()
            .recordCheck(listOf(true, false, false, false), 6_000L)
        val requested = first.photoRequested("/data/pictures/photo-2.jpg")
        assertTrue(droppedPhotos(first, requested).isEmpty())

        val retaken = requested.photoTaken()
        assertEquals(listOf("/data/pictures/photo-1.jpg"), droppedPhotos(requested, retaken))
    }

    @Test
    fun skipDropsAnUncheckedPhotoButKeepsACheckedOne() {
        val walk = Walk.start("Autumn colour", "Clear", builtInPictures.take(3), 1L)
            .photoRequested("/data/pictures/photo-1.jpg").photoTaken()
        assertEquals(listOf("/data/pictures/photo-1.jpg"), droppedPhotos(walk, walk.skip()))

        val checked = walk.recordCheck(listOf(true, false, false, false), 6_000L)
        assertTrue(droppedPhotos(checked, checked.skip()).isEmpty())
    }

    @Test
    fun aDroppedPhotoOutsideTheAppFolderIsNotTheApps() {
        val walk = Walk.start("Autumn colour", "Clear", builtInPictures.take(3), 1L)
            .photoRequested("/sdcard/DCIM/photo-1.jpg").photoTaken()
        val dropped = droppedPhotos(walk, walk.skip())
        assertEquals(listOf("/sdcard/DCIM/photo-1.jpg"), dropped)
        assertTrue(dropped.none { isAppPhoto(it, "/data/pictures") })
    }

    @Test
    fun onlyFilesDirectlyInThePicturesFolderAreTheApps() {
        assertTrue(isAppPhoto("/data/pictures/photo-1.jpg", "/data/pictures"))
        assertTrue(isAppPhoto("/data/pictures/photo-1.jpg", "/data/pictures/"))
        assertFalse(isAppPhoto("/data/pictures/sub/photo-1.jpg", "/data/pictures"))
        assertFalse(isAppPhoto("/data/pictures/../photo-1.jpg", "/data/pictures"))
        assertFalse(isAppPhoto("/sdcard/DCIM/photo-1.jpg", "/data/pictures"))
    }

    @Test
    fun aWalkReferencesItsShotsAndItsPendingPhoto() {
        val walk = Walk.start("Autumn colour", "Clear", builtInPictures.take(3), 1L)
            .photoRequested("/data/pictures/photo-1.jpg").photoTaken()
            .photoRequested("/data/pictures/photo-2.jpg")
        assertEquals(setOf("/data/pictures/photo-1.jpg", "/data/pictures/photo-2.jpg"), walkPhotoPaths(walk))
    }

    @Test
    fun droppedPhotosAreKeptUnlessTheWalkFileWasWrittenAndRead() {
        val dropped = listOf("/data/pictures/photo-1.jpg", "/data/pictures/photo-2.jpg")
        assertEquals(dropped, photosSafeToDelete(dropped, written = true, savedWalkPhotos = emptySet()))
        assertEquals(
            listOf("/data/pictures/photo-2.jpg"),
            photosSafeToDelete(dropped, written = true, savedWalkPhotos = setOf("/data/pictures/photo-1.jpg")),
        )
        // The write failed, or walk.json is there but unreadable: keep every file.
        assertTrue(photosSafeToDelete(dropped, written = false, savedWalkPhotos = emptySet()).isEmpty())
        assertTrue(photosSafeToDelete(dropped, written = true, savedWalkPhotos = null).isEmpty())
    }

    @Test
    fun aPhotoStillOnAnotherShotIsKept() {
        val walk = Walk.start("Autumn colour", "Clear", builtInPictures.take(3), 1L)
            .photoRequested("/data/pictures/photo-1.jpg").photoTaken()
            .recordCheck(listOf(true, true, true, false), 6_000L)
            .nextPicture()
            .photoRequested("/data/pictures/photo-1.jpg").photoTaken()
        assertTrue(droppedPhotos(walk, walk.skip()).isEmpty())
    }

    @Test
    fun stepsThatKeepThePhotosDropNothing() {
        val walk = Walk.start("Autumn colour", "Clear", builtInPictures.take(3), 1L)
            .photoRequested("/data/pictures/photo-1.jpg").photoTaken()
        val checked = walk.recordCheck(listOf(true, true, true, false), 6_000L)
        assertTrue(droppedPhotos(walk, checked).isEmpty())
        assertTrue(droppedPhotos(checked, checked.flip(0)).isEmpty())
        assertTrue(droppedPhotos(checked, checked.nextPicture()).isEmpty())
    }

    @Test
    fun aWalkWithoutPhotosHasNoneToDelete() {
        val walk = Walk.start("Autumn colour", "Clear", builtInPictures.take(3), 1L).skip().skip().skip()
        assertTrue(appPhotoPaths(walk, "/data/pictures").isEmpty())
    }
}
