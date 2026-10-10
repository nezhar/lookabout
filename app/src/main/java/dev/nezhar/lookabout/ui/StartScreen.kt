package dev.nezhar.lookabout.ui

import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.nezhar.lookabout.Gemma
import dev.nezhar.lookabout.modelPath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun StartScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Lookabout")
        Button(
            enabled = !running,
            onClick = {
                running = true
                result = null
                scope.launch {
                    try {
                        result = withContext(Dispatchers.Default) {
                            val start = SystemClock.elapsedRealtime()
                            Gemma.load(modelPath(context), context.cacheDir.path)
                            val loaded = SystemClock.elapsedRealtime()
                            val reply = Gemma.ask("Reply with the single word READY.")
                            val answered = SystemClock.elapsedRealtime()
                            val report = "Backend: ${Gemma.backendName}\n" +
                                "Load time: ${loaded - start} ms\n" +
                                "Reply: $reply\nAnswer time: ${answered - loaded} ms"
                            Log.d("Gemma", report)
                            report
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (t: Throwable) {
                        Log.e("Gemma", "Model test failed", t)
                        result = "Model test failed: ${t.message ?: t.javaClass.simpleName}"
                    } finally {
                        running = false
                    }
                }
            },
        ) {
            Text("Model test")
        }
        if (running) {
            CircularProgressIndicator()
            Text("Loading model and asking for READY… This may take a while.")
        }
        result?.let { Text(it) }
    }
}
