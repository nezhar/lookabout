package dev.nezhar.lookabout.ui

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.nezhar.lookabout.CheckResult
import dev.nezhar.lookabout.DEFAULT_CHECK_QUESTIONS
import dev.nezhar.lookabout.Gemma
import dev.nezhar.lookabout.accepted
import dev.nezhar.lookabout.checkPhoto
import dev.nezhar.lookabout.coverageText
import dev.nezhar.lookabout.decodeScaled
import dev.nezhar.lookabout.formatSeconds
import dev.nezhar.lookabout.ui.theme.LookaboutTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The long edge of the on-screen preview, in pixels. */
private const val PREVIEW_MAX_EDGE = 768

/** What the check screen shows below the questions. */
sealed interface CheckState {
    data object Idle : CheckState
    data object Running : CheckState

    /** A finished check, with the questions it was asked with. */
    data class Done(val questions: List<String>, val result: CheckResult) : CheckState
}

/**
 * The photo check debug screen: four editable questions, a photo from the system picker (no
 * storage permission) and the check's answers, coverage, verdict and model time.
 *
 * Self-contained, but not scrollable itself: put it in a scrolling container.
 */
@Composable
fun CheckScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var questions by remember { mutableStateOf(DEFAULT_CHECK_QUESTIONS) }
    var photo by remember { mutableStateOf<Uri?>(null) }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    var state by remember { mutableStateOf<CheckState>(CheckState.Idle) }

    // Checks a new or the current photo with the current questions. Any photo source can call it.
    fun startCheck(uri: Uri) {
        if (state == CheckState.Running) return
        photo = uri
        val asked = questions
        state = CheckState.Running
        scope.launch {
            state = CheckState.Done(asked, checkPhoto(context, uri, asked))
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) startCheck(uri)
    }

    // A small preview, decoded off the main thread.
    LaunchedEffect(photo) {
        preview = null
        val uri = photo ?: return@LaunchedEffect
        preview = withContext(Dispatchers.IO) {
            try {
                decodeScaled(context, uri, PREVIEW_MAX_EDGE).asImageBitmap()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Throwable) {
                Log.w(Gemma.TAG, "Could not show the photo", t)
                null
            }
        }
    }

    CheckContent(
        questions = questions,
        onQuestionChange = { index, text ->
            questions = questions.toMutableList().also { it[index] = text }
        },
        preview = preview,
        state = state,
        onPick = {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onCheckAgain = photo?.let { uri -> { startCheck(uri) } },
        modifier = modifier,
    )
}

/**
 * The check screen's layout. [onCheckAgain] is null while there is no photo to check again.
 */
@Composable
fun CheckContent(
    questions: List<String>,
    onQuestionChange: (index: Int, text: String) -> Unit,
    preview: ImageBitmap?,
    state: CheckState,
    onPick: () -> Unit,
    onCheckAgain: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val running = state == CheckState.Running
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Photo check", style = MaterialTheme.typography.titleMedium)
        questions.forEachIndexed { index, question ->
            OutlinedTextField(
                value = question,
                onValueChange = { onQuestionChange(index, it) },
                label = { Text("Question ${index + 1}") },
                singleLine = true,
                enabled = !running,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onPick, enabled = !running) {
                Text("Pick photo")
            }
            if (onCheckAgain != null) {
                OutlinedButton(onClick = onCheckAgain, enabled = !running) {
                    Text("Check again")
                }
            }
        }
        if (preview != null) {
            Image(
                bitmap = preview,
                contentDescription = "The photo being checked",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
            )
        }
        when (state) {
            CheckState.Idle -> Unit
            CheckState.Running -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Text("Checking the photo…")
            }
            is CheckState.Done -> CheckResultText(state.questions, state.result)
        }
    }
}

@Composable
private fun CheckResultText(questions: List<String>, result: CheckResult) {
    val failure = result.failure
    if (failure != null) {
        Text("Check failed: $failure", color = MaterialTheme.colorScheme.error)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        questions.zip(result.answers).forEach { (question, yes) ->
            Text("${if (yes) "YES" else "NO"}  $question")
        }
        Text("Coverage: ${coverageText(result.answers)}")
        Text(
            if (accepted(result.answers)) "Accepted" else "Not yet",
            style = MaterialTheme.typography.titleMedium,
        )
        Text("Model call: ${formatSeconds(result.modelMs)}")
    }
}

@Preview(showBackground = true)
@Composable
private fun CheckContentDonePreview() {
    LookaboutTheme {
        CheckContent(
            questions = DEFAULT_CHECK_QUESTIONS,
            onQuestionChange = { _, _ -> },
            preview = null,
            state = CheckState.Done(
                DEFAULT_CHECK_QUESTIONS,
                CheckResult(listOf(false, true, true, true), 2840, "1: NO\n2: YES\n3: YES\n4: YES", null),
            ),
            onPick = {},
            onCheckAgain = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CheckContentFailedPreview() {
    LookaboutTheme {
        CheckContent(
            questions = DEFAULT_CHECK_QUESTIONS,
            onQuestionChange = { _, _ -> },
            preview = null,
            state = CheckState.Done(DEFAULT_CHECK_QUESTIONS, CheckResult.failed("Model not loaded")),
            onPick = {},
            onCheckAgain = null,
        )
    }
}
