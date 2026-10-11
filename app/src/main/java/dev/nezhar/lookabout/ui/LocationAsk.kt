package dev.nezhar.lookabout.ui

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.edit
import dev.nezhar.lookabout.LOCATION_ASKED_KEY
import dev.nezhar.lookabout.LOCATION_PREFS
import dev.nezhar.lookabout.hasCoarseLocation

/**
 * A reason line and a small button that asks once for coarse location, so the list can fit the
 * sun. Shows nothing once location is allowed or the question was asked (remembered in
 * SharedPreferences), and nothing in previews.
 */
@Composable
fun LocationAsk(modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) return
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(LOCATION_PREFS, Context.MODE_PRIVATE) }
    var asked by remember { mutableStateOf(prefs.getBoolean(LOCATION_ASKED_KEY, false)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    if (asked || hasCoarseLocation(context)) return
    Column(modifier = modifier) {
        Text(
            "Your rough location tells Gemma where the sun is. It stays on the phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = {
            prefs.edit { putBoolean(LOCATION_ASKED_KEY, true) }
            asked = true
            launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }) {
            Text("Use my location for the light")
        }
    }
}
