package dev.nezhar.lookabout

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Checks one photo against four questions: makes a small upright JPEG copy in `cacheDir`, loads
 * Gemma if needed, asks the check prompt with the copy and reads the answers.
 *
 * [photo] may be a `content:` Uri from the picker or `Uri.fromFile(file)` for a camera photo.
 * Runs on `Dispatchers.Default`, so it is safe to call from the main thread. Never throws except
 * on cancellation: any error comes back as a failed [CheckResult].
 */
suspend fun checkPhoto(context: Context, photo: Uri, questions: List<String>): CheckResult =
    withContext(Dispatchers.Default) {
        try {
            val copy = smallCopy(context, photo)
            try {
                Gemma.load(modelPath(context), context.cacheDir.path)
                runCheck(
                    questions,
                    ask = { prompt -> Gemma.ask(prompt, copy.path, temperature = 0.2, maxTokens = 64) },
                    nowMs = { SystemClock.elapsedRealtime() },
                )
            } finally {
                copy.delete()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Throwable) {
            Log.e(Gemma.TAG, "Photo check failed", t)
            CheckResult.failed(t.message ?: t.javaClass.simpleName)
        }
    }.also { result ->
        Log.i(
            Gemma.TAG,
            "Photo check: answers ${result.answers}, model call ${result.modelMs} ms, " +
                "reply \"${result.reply.trim()}\", failure ${result.failure}",
        )
    }

/**
 * Decodes [photo] upright (ImageDecoder applies the EXIF orientation itself) with its long edge
 * at most [maxEdge]. Blocks: call it off the main thread.
 */
fun decodeScaled(context: Context, photo: Uri, maxEdge: Int = CHECK_PHOTO_MAX_EDGE): Bitmap {
    val source = ImageDecoder.createSource(context.contentResolver, photo)
    return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        val (width, height) = scaledSize(info.size.width, info.size.height, maxEdge)
        decoder.setTargetSize(width, height)
        // A hardware bitmap cannot be read back for compressing.
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
}

/** Writes the model's copy of [photo]: upright, at most 1024 px on the long edge, JPEG in `cacheDir`. */
private fun smallCopy(context: Context, photo: Uri): File {
    val bitmap = decodeScaled(context, photo)
    val dir = File(context.cacheDir, "check").apply { mkdirs() }
    val file = File.createTempFile("photo-", ".jpg", dir)
    try {
        file.outputStream().use { out ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)) { "Could not write the photo copy" }
        }
    } catch (t: Throwable) {
        file.delete()
        throw t
    } finally {
        bitmap.recycle()
    }
    return file
}
