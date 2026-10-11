package dev.nezhar.lookabout.ui

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import dev.nezhar.lookabout.needsNotificationPermission

private const val PREFS_NAME = "notifications"
private const val KEY_ASKED = "asked_post_notifications"

/**
 * Asks once for permission to post notifications (Android 13 and later), for the walk notice.
 * Returns the function that asks; it does nothing when the permission is already granted or was
 * asked for before, so a refusal is never asked again. [onAnswered] runs after the walker answers,
 * whatever the answer.
 */
@Composable
fun rememberNotificationPermissionAsk(onAnswered: () -> Unit): () -> Unit {
    val context = LocalContext.current.applicationContext
    val currentOnAnswered by rememberUpdatedState(onAnswered)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        currentOnAnswered()
    }
    return {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (needsNotificationPermission(context) && !prefs.getBoolean(KEY_ASKED, false)) {
            prefs.edit { putBoolean(KEY_ASKED, true) }
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
