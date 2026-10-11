package dev.nezhar.lookabout

import dev.nezhar.lookabout.walk.SavedWalk
import dev.nezhar.lookabout.walk.Walk
import dev.nezhar.lookabout.walk.builtInPictures
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class WalkStoreTest {
    @get:Rule
    val temp = TemporaryFolder()

    private fun store() = WalkStore(File(temp.root, WALK_FILE_NAME))

    private val walk = Walk.start("Autumn colour", "Clear", builtInPictures.take(3), 1L)
        .photoRequested("/data/pictures/photo-1.jpg").photoTaken()

    @Test
    fun aSaveCompletesOnceTheFileHoldsIt() {
        runBlocking {
            val store = store()
            assertTrue(withTimeout(5_000) { store.save(SavedWalk(walk, onSummary = false)).await() })
            assertEquals(walk, store.load()?.walk)
            assertEquals(setOf("/data/pictures/photo-1.jpg"), store.photoPaths())
        }
    }

    @Test
    fun anOvertakenSaveCompletesWithTheNewerOne() {
        runBlocking {
            val store = store()
            val newer = walk.skip()
            val first = store.save(SavedWalk(walk, onSummary = false))
            val second = store.save(SavedWalk(newer, onSummary = false))
            assertTrue(withTimeout(5_000) { first.await() })
            assertTrue(withTimeout(5_000) { second.await() })
            assertEquals(newer, store.load()?.walk)
            assertEquals(emptySet<String>(), store.photoPaths())
        }
    }

    @Test
    fun withoutASavedWalkThereAreNoPhotos() {
        runBlocking {
            val store = store()
            assertEquals(emptySet<String>(), store.photoPaths())
            store.save(SavedWalk(walk, onSummary = false))
            assertTrue(withTimeout(5_000) { store.save(null).await() })
            assertEquals(emptySet<String>(), store.photoPaths())
        }
    }
}
