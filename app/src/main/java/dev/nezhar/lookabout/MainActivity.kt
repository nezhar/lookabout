package dev.nezhar.lookabout

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.nezhar.lookabout.ui.LookaboutApp
import dev.nezhar.lookabout.ui.theme.LookaboutTheme

/** The navigation bar scrim behind three-button navigation, as enableEdgeToEdge uses by default in light mode. */
private val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always light, so the bar icons stay dark even when the phone is in dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(LIGHT_SCRIM, LIGHT_SCRIM),
        )
        setContent {
            LookaboutTheme {
                LookaboutApp()
            }
        }
    }
}
