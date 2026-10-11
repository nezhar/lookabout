package dev.nezhar.lookabout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class ModelDownloadTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun urlEndsWithTheFileNameModelPathLooksFor() {
        assertTrue(MODEL_URL.startsWith("https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/"))
        assertTrue(MODEL_URL.endsWith("/gemma-4-E2B-it.litertlm"))
    }

    @Test
    fun modelFileWinsOverADownload() {
        assertEquals(ModelStart.Load, modelStart(fileFound = true, downloadId = null))
        assertEquals(ModelStart.Load, modelStart(fileFound = true, downloadId = 7))
    }

    @Test
    fun rememberedDownloadIsFollowedWithoutAFile() {
        assertEquals(ModelStart.FollowDownload, modelStart(fileFound = false, downloadId = 7))
    }

    @Test
    fun noFileAndNoDownloadOffersTheDownload() {
        assertEquals(ModelStart.OfferDownload, modelStart(fileFound = false, downloadId = null))
    }

    @Test
    fun percentRoundsDownAndStaysInRange() {
        assertEquals(0, downloadPercent(0, 2_580_000_000))
        assertEquals(42, downloadPercent(1_100_000_000, 2_580_000_000))
        assertEquals(99, downloadPercent(2_579_999_999, 2_580_000_000))
        assertEquals(100, downloadPercent(2_580_000_000, 2_580_000_000))
        assertEquals(100, downloadPercent(3_000_000_000, 2_580_000_000))
        assertEquals(0, downloadPercent(-1, 100))
    }

    @Test
    fun percentIsUnknownWithoutASize() {
        assertNull(downloadPercent(0, -1))
        assertNull(downloadPercent(500, 0))
    }

    @Test
    fun progressText() {
        assertEquals("Downloading the model: 42%", downloadProgressText(42, waiting = false))
        assertEquals("Downloading the model", downloadProgressText(null, waiting = false))
        assertEquals("Waiting for Wi-Fi (42%)", downloadProgressText(42, waiting = true))
        assertEquals("Waiting for Wi-Fi", downloadProgressText(null, waiting = true))
    }

    @Test
    fun runningAndPendingDownloadsShowProgress() {
        assertEquals(
            DownloadStep.Running(50, waiting = false),
            downloadStep(DownloadSnapshot(DownloadPhase.Running, 50, 100)),
        )
        assertEquals(
            DownloadStep.Running(null, waiting = false),
            downloadStep(DownloadSnapshot(DownloadPhase.Pending, 0, -1)),
        )
    }

    @Test
    fun pausedDownloadWaits() {
        assertEquals(
            DownloadStep.Running(10, waiting = true),
            downloadStep(DownloadSnapshot(DownloadPhase.Paused, 10, 100, reason = 3)),
        )
    }

    @Test
    fun successfulDownloadIsFinished() {
        assertEquals(DownloadStep.Finished, downloadStep(DownloadSnapshot(DownloadPhase.Successful, 100, 100)))
    }

    @Test
    fun failedDownloadSaysWhy() {
        assertEquals(
            DownloadStep.Failed("The download failed: the phone needs 2.6 GB of free space."),
            downloadStep(DownloadSnapshot(DownloadPhase.Failed, 0, 100, reason = 1006)),
        )
        assertEquals(
            DownloadStep.Failed("The download failed: the server answered 404."),
            downloadStep(DownloadSnapshot(DownloadPhase.Failed, 0, -1, reason = 404)),
        )
        assertEquals(
            DownloadStep.Failed("The download failed (code 1008)."),
            downloadStep(DownloadSnapshot(DownloadPhase.Failed, 0, -1, reason = 1008)),
        )
    }

    @Test
    fun goneDownloadFails() {
        assertEquals(DownloadStep.Failed("The download was stopped."), downloadStep(null))
    }

    @Test
    fun installRenamesWhenItCan() {
        val source = temp.newFile("download.litertlm").apply { writeText("model") }
        val target = File(temp.newFolder("files"), MODEL_FILE_NAME)
        installModel(source, target)
        assertEquals("model", target.readText())
        assertFalse(source.exists())
    }

    @Test
    fun installCopiesWhenRenameFailsThenDeletesTheSource() {
        val source = temp.newFile("download.litertlm").apply { writeText("model") }
        val target = File(temp.newFolder("files"), MODEL_FILE_NAME)
        installModel(source, target, rename = { from, to -> from != source && from.renameTo(to) })
        assertEquals("model", target.readText())
        assertFalse(source.exists())
        assertFalse(File(target.path + ".part").exists())
    }

    @Test
    fun failedInstallKeepsTheSourceAndLeavesNoTarget() {
        val source = temp.newFile("download.litertlm").apply { writeText("model") }
        val target = File(temp.newFolder("files"), MODEL_FILE_NAME)
        try {
            installModel(source, target, rename = { _, _ -> false })
            throw AssertionError("installModel should have thrown")
        } catch (expected: IOException) {
            // The copy could not be put in place.
        }
        assertTrue(source.exists())
        assertFalse(target.exists())
        assertFalse(File(target.path + ".part").exists())
    }

    @Test(expected = IOException::class)
    fun installWithoutTheDownloadedFileThrows() {
        installModel(File(temp.root, "missing"), File(temp.root, MODEL_FILE_NAME))
    }
}
