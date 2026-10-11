package dev.nezhar.lookabout

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import dev.nezhar.lookabout.ui.LookaboutApp
import dev.nezhar.lookabout.ui.theme.LookaboutTheme

/** The navigation bar scrim behind three-button navigation, as enableEdgeToEdge uses by default in light mode. */
private val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)

class MainActivity : ComponentActivity() {
    /** Taps on the walk notice since this activity was created; a recreated activity starts again at 0. */
    private var noticeTaps by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Cold start from the walk notice; a recreated activity already handled its intent.
        if (savedInstanceState == null && intent.isWalkNoticeTap()) noticeTaps++
        // The app is always light, so the bar icons stay dark even when the phone is in dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(LIGHT_SCRIM, LIGHT_SCRIM),
        )
        setContent {
            LookaboutTheme {
                LookaboutApp(noticeTaps = noticeTaps)
            }
        }
    }

    /** A tap on the walk notice while the app is open (launchMode singleTop). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.isWalkNoticeTap()) noticeTaps++
    }

    private fun Intent?.isWalkNoticeTap(): Boolean = this?.getBooleanExtra(EXTRA_FROM_WALK_NOTICE, false) == true
}
