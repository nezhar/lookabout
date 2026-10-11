package dev.nezhar.lookabout

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A new, empty file in the app's Pictures folder for the camera app to write one photo into,
 * with the `content:` Uri the camera app gets write access to.
 */
class CameraTarget(val file: File, val uri: Uri) {
    companion object {
        /** Creates the empty file. Throws [IOException] when the app's Pictures folder is unavailable. */
        fun create(context: Context): CameraTarget {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: throw IOException("The app's Pictures folder is not available")
            if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Could not create $dir")
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            // createTempFile picks a name that does not exist yet and creates the file.
            val file = File.createTempFile("photo-$stamp-", ".jpg", dir)
            return try {
                CameraTarget(file, photoUri(context, file))
            } catch (t: Throwable) {
                file.delete()
                throw t
            }
        }

        /** The FileProvider Uri of [file], which must be in the app's Pictures folder. */
        fun photoUri(context: Context, file: File): Uri =
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
