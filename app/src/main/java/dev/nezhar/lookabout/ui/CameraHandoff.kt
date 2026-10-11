package dev.nezhar.lookabout.ui

import android.content.ActivityNotFoundException
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.nezhar.lookabout.CameraTarget
import dev.nezhar.lookabout.Gemma
import java.io.File
import java.io.IOException

/**
 * Hands off to the phone's camera app for one photo. Returns the function that opens the camera.
 *
 * The photo lands in a new file in the app's Pictures folder and comes back through [onPhoto].
 * When the camera is cancelled, returns nothing or cannot be opened, the empty file is deleted and
 * [onCancelled] is called. The target path is kept in saved state, so the result still matches
 * when Android recreates the screen while the camera app is in front.
 *
 * Needs no CAMERA permission: the camera app takes the photo and writes it through a FileProvider
 * Uri.
 */
@Composable
fun rememberCameraHandoff(onPhoto: (File) -> Unit, onCancelled: () -> Unit = {}): () -> Unit {
    val context = LocalContext.current.applicationContext
    var targetPath by rememberSaveable { mutableStateOf<String?>(null) }
    val currentOnPhoto by rememberUpdatedState(onPhoto)
    val currentOnCancelled by rememberUpdatedState(onCancelled)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val path = targetPath
        targetPath = null
        if (path == null) {
            currentOnCancelled()
            return@rememberLauncherForActivityResult
        }
        val file = File(path)
        if (saved && file.length() > 0) {
            currentOnPhoto(file)
        } else {
            file.delete()
            currentOnCancelled()
        }
    }

    return {
        // A target left over from a hand-off whose result never came: drop it if still empty.
        targetPath?.let { stale -> File(stale).takeIf { it.length() == 0L }?.delete() }
        targetPath = null
        val target = try {
            CameraTarget.create(context)
        } catch (e: IOException) {
            Log.w(Gemma.TAG, "Could not create the photo file", e)
            null
        } catch (e: IllegalArgumentException) {
            // FileProvider: the file is outside the configured paths.
            Log.w(Gemma.TAG, "Could not share the photo file", e)
            null
        }
        if (target == null) {
            Toast.makeText(context, "Could not prepare a photo file", Toast.LENGTH_SHORT).show()
            currentOnCancelled()
        } else {
            targetPath = target.file.path
            try {
                launcher.launch(target.uri)
            } catch (e: ActivityNotFoundException) {
                Log.w(Gemma.TAG, "No camera app", e)
                targetPath = null
                target.file.delete()
                Toast.makeText(context, "No camera app found", Toast.LENGTH_SHORT).show()
                currentOnCancelled()
            }
        }
    }
}
