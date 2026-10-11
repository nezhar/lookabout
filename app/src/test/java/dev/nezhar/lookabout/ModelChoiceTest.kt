package dev.nezhar.lookabout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ModelChoiceTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val gib = 1024L * 1024 * 1024

    @Test
    fun choicesMapToTheirFiles() {
        assertEquals("gemma-4-E2B-it.litertlm", ModelChoice.E2B.fileName)
        assertEquals("gemma-4-E4B-it.litertlm", ModelChoice.E4B.fileName)
        // The in-app download stays E2B, under the same name.
        assertEquals(MODEL_FILE_NAME, ModelChoice.E2B.fileName)
    }

    @Test
    fun twelveGigabytePhonesGetTheChoice() {
        // A 12 GB phone reports a bit less than 12 GiB.
        assertTrue(offersModelChoice(11L * gib + 512L * 1024 * 1024))
        assertTrue(offersModelChoice(11L * gib))
        assertTrue(offersModelChoice(16L * gib))
    }

    @Test
    fun smallerPhonesDoNot() {
        assertFalse(offersModelChoice(11L * gib - 1))
        assertFalse(offersModelChoice(7L * gib + 600L * 1024 * 1024))
        assertFalse(offersModelChoice(0))
    }

    @Test
    fun savedKeysReadBackAndAnythingElseIsE2B() {
        assertEquals(ModelChoice.E4B, modelChoiceFromKey(ModelChoice.E4B.name))
        assertEquals(ModelChoice.E2B, modelChoiceFromKey(ModelChoice.E2B.name))
        assertEquals(ModelChoice.E2B, modelChoiceFromKey(null))
        assertEquals(ModelChoice.E2B, modelChoiceFromKey("E9B"))
    }

    @Test
    fun e4bIsUsedWhenOfferedAndFound() {
        assertEquals(ModelChoice.E4B, usableModelChoice(ModelChoice.E4B, offersChoice = true) { true })
    }

    @Test
    fun missingE4bFileFallsBackToE2b() {
        assertEquals(ModelChoice.E2B, usableModelChoice(ModelChoice.E4B, offersChoice = true) { false })
    }

    @Test
    fun phonesWithoutTheChoiceAlwaysUseE2b() {
        assertEquals(ModelChoice.E2B, usableModelChoice(ModelChoice.E4B, offersChoice = false) { true })
    }

    @Test
    fun e2bNeedsNoFileCheck() {
        // A missing E2B file is handled by the download, not by the choice.
        assertEquals(ModelChoice.E2B, usableModelChoice(ModelChoice.E2B, offersChoice = true) { false })
    }

    @Test
    fun ownFilesFolderWinsOverTheDevelopmentFolder() {
        val files = temp.newFolder("files")
        val dev = temp.newFolder("llm")
        File(files, ModelChoice.E4B.fileName).writeText("model")
        assertEquals(File(files, ModelChoice.E4B.fileName), modelFile(ModelChoice.E4B, files, dev))
        assertEquals(File(dev, ModelChoice.E2B.fileName), modelFile(ModelChoice.E2B, files, dev))
    }

    @Test
    fun defaultDevelopmentFolderIsTheAdbOne() {
        val files = temp.newFolder("files")
        assertEquals(
            "/data/local/tmp/llm/gemma-4-E4B-it.litertlm",
            modelFile(ModelChoice.E4B, files).path,
        )
    }

    @Test
    fun labelsComeFromTheFileName() {
        assertEquals("Gemma 4 E4B", modelLabel("/data/local/tmp/llm/gemma-4-E4B-it.litertlm"))
        assertEquals("Gemma 4 E2B", modelLabel("/data/user/0/dev.nezhar.lookabout/files/gemma-4-E2B-it.litertlm"))
        assertEquals("other.litertlm", modelLabel("/sdcard/other.litertlm"))
    }

    @Test
    fun missingTextNamesTheFileAndTheFallback() {
        val text = missingModelText(ModelChoice.E4B)
        assertTrue(text.contains("gemma-4-E4B-it.litertlm"))
        assertTrue(text.contains("/data/local/tmp/llm/"))
        assertTrue(text.endsWith("Staying on Gemma 4 E2B."))
    }
}
