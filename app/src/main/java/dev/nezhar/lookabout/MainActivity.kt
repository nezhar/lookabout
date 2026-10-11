package dev.nezhar.lookabout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.nezhar.lookabout.ui.LookaboutApp
import dev.nezhar.lookabout.ui.theme.LookaboutTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LookaboutTheme {
                LookaboutApp()
            }
        }
    }
}
