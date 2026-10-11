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
        assertTrue(droppedAppPhotos(first, requested, "/data/pictures").isEmpty())

        val retaken = requested.photoTaken()
        assertEquals(listOf("/data/pictures/photo-1.jpg"), droppedAppPhotos(requested, retaken, "/data/pictures"))
    }

    @Test
    fun skipDropsAnUncheckedPhotoButKeepsACheckedOne() {
        val walk = Walk.start("Autumn colour", "Clear", builtInPictures.take(3), 1L)
            .photoRequested("/data/pictures/photo-1.jpg").photoTaken()
        assertEquals(listOf("/data/pictures/photo-1.jpg"), droppedAppPhotos(walk, walk.skip(), "/data/pictures"))

        val checked = walk.recordCheck(listOf(true, false, false, false), 6_000L)
        assertTrue(droppedAppPhotos(checked, checked.skip(), "/data/pictures").isEmpty())
    }

    @Test
    fun aDroppedPhotoOutsideTheAppFolderIsNeverListed() {
        val walk = Walk.start("Autumn colour", "Clear", builtInPictures.take(3), 1L)
            .photoRequested("/sdcard/DCIM/photo-1.jpg").photoTaken()
        assertTrue(droppedAppPhotos(walk, walk.skip(), "/data/pictures").isEmpty())
    }

    @Test
    fun aPhotoStillOnAnotherShotIsKept() {
        val walk = Walk.start("Autumn colour", "Clear", builtInPictures.take(3), 1L)
            .photoRequested("/data/pictures/photo-1.jpg").photoTaken()
            .recordCheck(listOf(true, true, true, false), 6_000L)
            .nextPicture()
            .photoRequested("/data/pictures/photo-1.jpg").photoTaken()
        assertTrue(droppedAppPhotos(walk, walk.skip(), "/data/pictures").isEmpty())
    }

    @Test
    fun stepsThatKeepThePhotosDropNothing() {
        val walk = Walk.start("Autumn colour", "Clear", builtInPictures.take(3), 1L)
            .photoRequested("/data/pictures/photo-1.jpg").photoTaken()
        val checked = walk.recordCheck(listOf(true, true, true, false), 6_000L)
        assertTrue(droppedAppPhotos(walk, checked, "/data/pictures").isEmpty())
        assertTrue(droppedAppPhotos(checked, checked.flip(0), "/data/pictures").isEmpty())
        assertTrue(droppedAppPhotos(checked, checked.nextPicture(), "/data/pictures").isEmpty())
    }

    @Test
    fun aWalkWithoutPhotosHasNoneToDelete() {
        val walk = Walk.start("Autumn colour", "Clear", builtInPictures.take(3), 1L).skip().skip().skip()
        assertTrue(appPhotoPaths(walk, "/data/pictures").isEmpty())
    }
}
