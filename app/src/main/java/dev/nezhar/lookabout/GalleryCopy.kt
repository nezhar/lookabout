package dev.nezhar.lookabout

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.IOException
import java.time.LocalDateTime

/**
 * Copies the JPEG [file] into the phone's gallery under [GALLERY_FOLDER], so it can be edited and
 * shared from there. The app keeps using its own file. Needs no storage permission: an app may
 * always add its own pictures to MediaStore (Android 10 and later).
 *
 * Blocking file work: call it off the main thread. Never throws: a failure is logged, any
 * half-written gallery entry is deleted, and null is returned.
 */
fun copyToGallery(context: Context, file: File): Uri? {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, galleryName(LocalDateTime.now()))
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, GALLERY_FOLDER)
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    var uri: Uri? = null
    return try {
        val entry = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("The gallery did not create an entry")
        uri = entry
        val output = resolver.openOutputStream(entry, "w")
            ?: throw IOException("Could not open $entry for writing")
        output.use { out -> file.inputStream().use { it.copyTo(out) } }
        val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
        resolver.update(entry, done, null, null)
        entry
    } catch (t: Throwable) {
        Log.w(Gemma.TAG, "Could not copy the photo to the gallery", t)
        uri?.let { entry ->
            try {
                resolver.delete(entry, null, null)
            } catch (e: Throwable) {
                Log.w(Gemma.TAG, "Could not delete the half-written gallery entry", e)
            }
        }
        null
    }
}
