package dev.nezhar.lookabout.walk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class WalkArchiveTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val questions = listOf("Is it red?", "Is the sky visible?", "Is it a close-up?", "Übers Wasser?")

    private fun pictures(count: Int) = (1..count).map { Picture("Picture $it", questions) }

    private fun dir(): File = File(temp.root, WALK_ARCHIVE_DIR)

    /** A finished walk: not yet, skipped, accepted with a correction, and a photo left unchecked. */
    private fun finishedWalk(startedAtMillis: Long = 1_760_000_000_123L): Walk =
        Walk.start("Autumn \"wet\" streets", "Rain", pictures(4), startedAtMillis)
            .photoRequested("/pictures/photo-1.jpg").photoTaken()
            .recordCheck(listOf(false, false, false, true), 8_000L)
            .nextPicture()
            .skip()
            .photoRequested("/pictures/photo-3.jpg").photoTaken()
            .recordCheck(listOf(true, false, false, true), 9_412L)
            .flip(1)
            .nextPicture()
            .photoRequested("/pictures/photo-4.jpg").photoTaken()
            .skip()

    @Test
    fun anArchivedWalkRoundTripsWithAllItsResults() {
        val walk = finishedWalk()
        assertEquals(Outcome.NOT_YET, walk.shots[0].outcome)
        assertEquals(Outcome.SKIPPED, walk.shots[1].outcome)
        assertEquals(Outcome.ACCEPTED, walk.shots[2].outcome)
        assertEquals(Outcome.SKIPPED, walk.shots[3].outcome)
        assertTrue(walk.isFinished)

        val archive = WalkArchive(dir())
        archive.save(walk)

        assertTrue(File(dir(), "walk-1760000000123.json").isFile)
        assertEquals(walk, archive.load(1_760_000_000_123L))
    }

    @Test
    fun theListHoldsWhatAHistoryRowNeeds() {
        val walk = finishedWalk()
        val archive = WalkArchive(dir())
        archive.save(walk)

        val summary = archive.list().single()
        assertEquals(
            ArchivedWalkSummary(
                id = 1_760_000_000_123L,
                startedAtMillis = 1_760_000_000_123L,
                topic = "Autumn \"wet\" streets",
                sky = "Rain",
                acceptedCount = 1,
                pictureCount = 4,
                agreedCount = 7,
                checkCount = 8,
                thumbnailPath = "/pictures/photo-3.jpg",
            ),
            summary,
        )
    }

    @Test
    fun aWalkWithoutAnAcceptedShotHasNoThumbnail() {
        val walk = Walk.start("Reflections", "Sun", pictures(3), 5L).skip().skip().skip()
        val summary = archivedWalkSummary(archiveId(walk), walk)
        assertNull(summary.thumbnailPath)
        assertEquals(0, summary.acceptedCount)
        assertEquals(0, summary.checkCount)
        assertEquals(3, summary.pictureCount)
    }

    @Test
    fun theListIsNewestFirst() {
        val archive = WalkArchive(dir())
        archive.save(finishedWalk(2_000L))
        archive.save(finishedWalk(1_000L))
        archive.save(finishedWalk(3_000L))

        assertEquals(listOf(3_000L, 2_000L, 1_000L), archive.list().map { it.id })
    }

    @Test
    fun unreadableFilesAreSkipped() {
        val archive = WalkArchive(dir())
        archive.save(finishedWalk(1_000L))
        File(dir(), archiveFileName(2_000L)).writeText("not a walk")
        File(dir(), archiveFileName(3_000L)).writeText("""{"version": 99}""")
        File(dir(), archiveFileName(4_000L) + ".tmp").writeText("half a")
        File(dir(), "notes.txt").writeText("hello")
        File(dir(), archiveFileName(5_000L)).mkdirs()

        assertEquals(listOf(1_000L), archive.list().map { it.id })
        assertNull(archive.load(2_000L))
        assertNull(archive.load(3_000L))
        assertNull(archive.load(5_000L))
        assertNull(archive.load(6_000L))
    }

    @Test
    fun anEmptyOrMissingArchiveListsNothing() {
        assertEquals(emptyList<ArchivedWalkSummary>(), WalkArchive(dir()).list())
    }

    @Test
    fun savingTheSameWalkAgainOverwritesIt() {
        val archive = WalkArchive(dir())
        val first = finishedWalk()
        archive.save(first)
        // Two more corrections on the accepted shot turn it into Not yet.
        val second = first.flip(0, shotIndex = 2).flip(3, shotIndex = 2)
        assertEquals(Outcome.NOT_YET, second.shots[2].outcome)
        archive.save(second)

        assertEquals(listOf("walk-1760000000123.json"), dir().list()!!.toList())
        assertEquals(second, archive.load(1_760_000_000_123L))
        val summary = archive.list().single()
        assertEquals(0, summary.acceptedCount)
        assertEquals(5, summary.agreedCount)
        assertNull(summary.thumbnailPath)
    }

    @Test
    fun deleteRemovesOnlyThatWalk() {
        val archive = WalkArchive(dir())
        archive.save(finishedWalk(1_000L))
        archive.save(finishedWalk(2_000L))

        assertTrue(archive.delete(1_000L))
        assertTrue(archive.delete(1_000L))
        assertNull(archive.load(1_000L))
        assertEquals(listOf(2_000L), archive.list().map { it.id })
    }

    @Test
    fun aPendingCameraPhotoIsNotArchived() {
        val walk = finishedWalk().copy(pendingPhotoPath = "/pictures/photo-9.jpg")
        val archive = WalkArchive(dir())
        archive.save(walk)
        assertEquals(walk.copy(pendingPhotoPath = null), archive.load(archiveId(walk)))
    }

    @Test
    fun fileNamesCarryTheId() {
        assertEquals("walk-42.json", archiveFileName(42L))
        assertEquals(42L, archiveIdOf("walk-42.json"))
        assertNull(archiveIdOf("walk-42.json.tmp"))
        assertNull(archiveIdOf("walk-.json"))
        assertNull(archiveIdOf("walk.json"))
        assertNull(archiveIdOf("walk-abc.json"))
    }

    @Test
    fun newestFirstOrdersByStartTime() {
        val walk = Walk.start("T", "S", pictures(3), 0L)
        val summaries = listOf(10L, 30L, 20L).map { archivedWalkSummary(it, walk.copy(startedAtMillis = it)) }
        assertEquals(listOf(30L, 20L, 10L), newestFirst(summaries).map { it.startedAtMillis })
    }

    @Test
    fun photoPathsListEveryArchivedPhoto() {
        val archive = WalkArchive(dir())
        assertEquals(emptySet<String>(), archive.photoPaths())

        archive.save(finishedWalk())
        assertEquals(setOf("/pictures/photo-1.jpg", "/pictures/photo-3.jpg"), archive.photoPaths())
    }

    @Test
    fun photoPathsAreUnknownWhenAnArchivedWalkCannotBeRead() {
        val archive = WalkArchive(dir())
        archive.save(finishedWalk())
        File(dir(), "walk-5.json").writeText("not a walk")
        assertNull(archive.photoPaths())
    }
}
