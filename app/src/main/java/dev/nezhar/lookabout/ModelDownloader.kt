package dev.nezhar.lookabout

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Downloads the model with the system's DownloadManager, which keeps going while the app is
 * closed, on Wi-Fi only and with a notification. The download's id is kept in SharedPreferences,
 * so a restart picks up a running download.
 *
 * Every method blocks briefly (disk and system calls): call them off the main thread.
 */
class ModelDownloader(context: Context) {
    private val app = context.applicationContext
    private val manager = app.getSystemService(DownloadManager::class.java)
    private val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** The id of a download started earlier and not yet dealt with, or null. */
    fun savedId(): Long? = prefs.getLong(KEY_ID, NO_ID).takeIf { it != NO_ID }

    /** Starts the download into the app's external files folder and remembers its id. */
    fun start(): Long {
        // A file left from an earlier try would make the system pick another name.
        downloadFile()?.delete()
        val request = DownloadManager.Request(Uri.parse(MODEL_URL))
            .setTitle("Gemma 4 E2B")
            .setDescription("The model Lookabout checks photos with")
            .setAllowedOverMetered(false)
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalFilesDir(app, null, MODEL_FILE_NAME)
        val id = manager.enqueue(request)
        prefs.edit().putLong(KEY_ID, id).commit()
        return id
    }

    /** How the download with [id] stands, or null when the system no longer knows it. */
    fun query(id: Long): DownloadSnapshot? {
        val cursor = manager.query(DownloadManager.Query().setFilterById(id)) ?: return null
        return cursor.use {
            if (!it.moveToFirst()) {
                null
            } else {
                val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                DownloadSnapshot(
                    phase = when (status) {
                        DownloadManager.STATUS_PENDING -> DownloadPhase.Pending
                        DownloadManager.STATUS_PAUSED -> DownloadPhase.Paused
                        DownloadManager.STATUS_SUCCESSFUL -> DownloadPhase.Successful
                        DownloadManager.STATUS_FAILED -> DownloadPhase.Failed
                        else -> DownloadPhase.Running
                    },
                    downloadedBytes = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                    totalBytes = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                    reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)),
                    localPath = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                        ?.let { uri -> Uri.parse(uri).path },
                )
            }
        }
    }

    /**
     * Moves the finished download (at [localPath], or where it was asked to go) into `filesDir`
     * under the name [modelPath] looks for.
     */
    fun install(localPath: String?) {
        val source = localPath?.let { File(it) } ?: downloadFile() ?: error("No download folder")
        installModel(source, File(app.filesDir, MODEL_FILE_NAME))
    }

    /** Stops the download with [id] if it still runs, deletes its file and forgets it. */
    fun discard(id: Long) {
        manager.remove(id)
        prefs.edit().remove(KEY_ID).commit()
    }

    private fun downloadFile(): File? = app.getExternalFilesDir(null)?.let { File(it, MODEL_FILE_NAME) }

    private companion object {
        const val PREFS_NAME = "model_download"
        const val KEY_ID = "download_id"
        const val NO_ID = -1L
    }
}
