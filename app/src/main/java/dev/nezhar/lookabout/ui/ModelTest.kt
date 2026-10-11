package dev.nezhar.lookabout.ui

import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.nezhar.lookabout.Gemma
import dev.nezhar.lookabout.modelLabel
import dev.nezhar.lookabout.modelPath
import dev.nezhar.lookabout.ui.theme.LookaboutTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val READY_PROMPT = "Reply with the single word READY."

/** What Model test shows. */
sealed interface ModelTestState {
    data object Idle : ModelTestState
    data object Running : ModelTestState
    data class Done(
        val model: String,
        val modelPath: String,
        val backend: String,
        val speculativeDecoding: Boolean,
        val loadMs: Long,
        val reply: String,
        val answerMs: Long,
    ) : ModelTestState
    data class Failed(val message: String) : ModelTestState
}

/**
 * Model test: loads Gemma, asks for READY and shows the loaded model and its file, the backend,
 * whether speculative decoding is on, the load time, the reply and the answer time.
 * Self-contained, so any screen can show it.
 */
@Composable
fun ModelTest(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<ModelTestState>(ModelTestState.Idle) }

    ModelTestContent(
        state = state,
        onRun = {
            state = ModelTestState.Running
            scope.launch {
                state = try {
                    withContext(Dispatchers.Default) {
                        runModelTest(modelPath(context), context.cacheDir.path)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (t: Throwable) {
                    Log.e(Gemma.TAG, "Model test failed", t)
                    ModelTestState.Failed(t.message ?: t.javaClass.simpleName)
                }
            }
        },
        modifier = modifier,
    )
}

/** Blocks for seconds: run it off the main thread. */
private fun runModelTest(modelPath: String, cacheDir: String): ModelTestState.Done {
    Log.i(Gemma.TAG, "Model test: loading $modelPath")
    val start = SystemClock.elapsedRealtime()
    Gemma.load(modelPath, cacheDir)
    val loaded = SystemClock.elapsedRealtime()
    val reply = Gemma.ask(READY_PROMPT).trim()
    val answered = SystemClock.elapsedRealtime()
    val backend = Gemma.backendName ?: "unknown"
    val speculative = Gemma.speculativeDecoding
    val path = Gemma.loadedModelPath ?: modelPath
    val model = modelLabel(path)
    Log.i(
        Gemma.TAG,
        "Model test: $model ($path), $backend, speculative decoding ${onOff(speculative)}, " +
            "loaded in ${loaded - start} ms, replied \"$reply\" in ${answered - loaded} ms",
    )
    return ModelTestState.Done(model, path, backend, speculative, loaded - start, reply, answered - loaded)
}

private fun onOff(on: Boolean): String = if (on) "on" else "off"

@Composable
fun ModelTestContent(
    state: ModelTestState,
    onRun: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onRun, enabled = state != ModelTestState.Running) {
            Text("Model test")
        }
        when (state) {
            ModelTestState.Idle -> Unit
            ModelTestState.Running -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Text("Loading the model and asking for READY…")
            }
            is ModelTestState.Done -> Text(
                "Model: ${state.model}\n" +
                    "File: ${state.modelPath}\n" +
                    "Backend: ${state.backend}\n" +
                    "Speculative decoding: ${onOff(state.speculativeDecoding)}\n" +
                    "Load time: ${state.loadMs} ms\n" +
                    "Reply: \"${state.reply}\"\n" +
                    "Answer time: ${state.answerMs} ms"
            )
            is ModelTestState.Failed -> Text(
                "Model test failed: ${state.message}",
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ModelTestDonePreview() {
    LookaboutTheme {
        ModelTestContent(
            state = ModelTestState.Done(
                model = "Gemma 4 E2B",
                modelPath = "/data/local/tmp/llm/gemma-4-E2B-it.litertlm",
                backend = "GPU",
                speculativeDecoding = true,
                loadMs = 9412,
                reply = "READY",
                answerMs = 380,
            ),
            onRun = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ModelTestFailedPreview() {
    LookaboutTheme {
        ModelTestContent(
            state = ModelTestState.Failed("Model file not found: /data/local/tmp/llm/gemma-4-E2B-it.litertlm"),
            onRun = {},
        )
    }
}
