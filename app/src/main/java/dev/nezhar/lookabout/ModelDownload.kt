package dev.nezhar.lookabout

import java.io.File
import java.io.IOException

/** The model file's name, in `filesDir` and in the download folder. */
const val MODEL_FILE_NAME = "gemma-4-E2B-it.litertlm"

/** Where the app downloads the model from. */
const val MODEL_URL =
    "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/$MODEL_FILE_NAME"

/** The download's size as Setup shows it. */
const val MODEL_SIZE_TEXT = "2.6 GB"

/** How often Setup asks the system how far the download has got, in milliseconds. */
const val DOWNLOAD_POLL_MS = 1_000L

/** What happens when the app starts, or Try again is pressed after a failed load. */
enum class ModelStart {
    /** The model file is there: load it. */
    Load,

    /** No model file, but a download was started earlier: follow it. */
    FollowDownload,

    /** No model file and no download: Setup offers the download. */
    OfferDownload,
}

/** The model file wins over a remembered download; without either, Setup offers the download. */
fun modelStart(fileFound: Boolean, downloadId: Long?): ModelStart = when {
    fileFound -> ModelStart.Load
    downloadId != null -> ModelStart.FollowDownload
    else -> ModelStart.OfferDownload
}

/** A download's status, as the system reports it. */
enum class DownloadPhase { Pending, Running, Paused, Successful, Failed }

/**
 * One look at the download.
 *
 * @property totalBytes the file's size, or -1 while the server has not said it yet.
 * @property reason why a failed download failed, or why a paused one waits (the system's code).
 * @property localPath where the system wrote the file, if it says.
 */
data class DownloadSnapshot(
    val phase: DownloadPhase,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val reason: Int = 0,
    val localPath: String? = null,
)

/** What to do after a look at the download. */
sealed interface DownloadStep {
    /**
     * Still going: show [percent] (null while unknown). [pausedReason] is the system's reason code
     * while the download is paused, null while it runs.
     */
    data class Running(val percent: Int?, val pausedReason: Int?) : DownloadStep

    /** The whole file is there: move it into place and load it. */
    data object Finished : DownloadStep

    /** It stopped: show [message] and Try again. */
    data class Failed(val message: String) : DownloadStep
}

/** Decides the next step from [snapshot]; null means the system no longer knows the download. */
fun downloadStep(snapshot: DownloadSnapshot?): DownloadStep {
    if (snapshot == null) return DownloadStep.Failed("The download was stopped.")
    val percent = downloadPercent(snapshot.downloadedBytes, snapshot.totalBytes)
    return when (snapshot.phase) {
        DownloadPhase.Pending, DownloadPhase.Running -> DownloadStep.Running(percent, pausedReason = null)
        DownloadPhase.Paused -> DownloadStep.Running(percent, pausedReason = snapshot.reason)
        DownloadPhase.Successful -> DownloadStep.Finished
        DownloadPhase.Failed -> DownloadStep.Failed(downloadFailureText(snapshot.reason))
    }
}

/** The share downloaded, 0 to 100, rounded down; null while the size is unknown. */
fun downloadPercent(downloadedBytes: Long, totalBytes: Long): Int? {
    if (totalBytes <= 0) return null
    return (downloadedBytes.coerceIn(0, totalBytes) * 100 / totalBytes).toInt()
}

// DownloadManager's reason codes for a paused download that waits for a network.
private const val PAUSED_WAITING_FOR_NETWORK = 2
private const val PAUSED_QUEUED_FOR_WIFI = 3

/**
 * The line Setup shows while the model downloads. [pausedReason] is the system's reason code while
 * the download is paused, null while it runs: a download that waits for a network says it waits
 * for Wi-Fi (the download is Wi-Fi only), any other pause says it will retry.
 */
fun downloadProgressText(percent: Int?, pausedReason: Int?): String {
    val status = when (pausedReason) {
        null -> return if (percent != null) "Downloading the model: $percent%" else "Downloading the model"
        PAUSED_WAITING_FOR_NETWORK, PAUSED_QUEUED_FOR_WIFI -> "Waiting for Wi-Fi"
        else -> "Paused, will retry"
    }
    return if (percent != null) "$status ($percent%)" else status
}

// DownloadManager's reason codes for a failed download.
private const val ERROR_INSUFFICIENT_SPACE = 1006
private const val ERROR_DEVICE_NOT_FOUND = 1007

/** The line Setup shows when the download failed with the system's [reason] code. */
fun downloadFailureText(reason: Int): String = when (reason) {
    ERROR_INSUFFICIENT_SPACE -> "The download failed: the phone needs $MODEL_SIZE_TEXT of free space."
    ERROR_DEVICE_NOT_FOUND -> "The download failed: the phone's storage is not available."
    in 400..599 -> "The download failed: the server answered $reason."
    else -> "The download failed (code $reason)."
}

/**
 * Moves the downloaded [source] to [target]. A rename is tried first; when it fails (the two are
 * on different file systems) the file is copied to `<target>.part`, renamed to [target] and only
 * then is [source] deleted, so [target] never exists half written. Blocks: call it off the main
 * thread. Throws [IOException] when the file could not be moved.
 */
fun installModel(
    source: File,
    target: File,
    rename: (File, File) -> Boolean = { from, to -> from.renameTo(to) },
) {
    if (!source.isFile) throw IOException("The downloaded file is missing")
    target.parentFile?.mkdirs()
    if (rename(source, target)) return
    val part = File(target.path + ".part")
    try {
        source.inputStream().use { input ->
            part.outputStream().use { output -> input.copyTo(output, 1 shl 20) }
        }
        if (!rename(part, target)) throw IOException("Could not put the model file in place")
    } catch (t: Throwable) {
        part.delete()
        throw t
    }
    source.delete()
}
