package dev.nezhar.lookabout.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.nezhar.lookabout.MAX_OWN_TOPIC_CHARS
import dev.nezhar.lookabout.PICTURE_COUNTS
import dev.nezhar.lookabout.SKY_OPTIONS
import dev.nezhar.lookabout.SUGGESTED_TOPICS
import dev.nezhar.lookabout.SetupChoices
import dev.nezhar.lookabout.ui.theme.LookaboutTheme

/**
 * Setup: the number of pictures, the topic and the sky, then Make my list. A long press on the
 * title opens the debug screen; nothing on screen shows it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    choices: SetupChoices,
    model: ModelState,
    onChoicesChange: (SetupChoices) -> Unit,
    onMakeList: () -> Unit,
    onTryAgain: () -> Unit,
    onOpenDebug: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val openDebug by rememberUpdatedState(onOpenDebug)
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column {
                Text(
                    "Lookabout",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(onLongPress = { openDebug() })
                    },
                )
                Text(
                    "A little walk. A fresh look.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SetupSection("Number of pictures") {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    PICTURE_COUNTS.forEachIndexed { index, count ->
                        SegmentedButton(
                            selected = choices.count == count,
                            onClick = { onChoicesChange(choices.withCount(count)) },
                            shape = SegmentedButtonDefaults.itemShape(index, PICTURE_COUNTS.size),
                        ) {
                            Text("$count")
                        }
                    }
                }
            }

            SetupSection("Topic") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SUGGESTED_TOPICS.forEach { topic ->
                        FilterChip(
                            selected = !choices.ownTopic && choices.topic == topic,
                            onClick = { onChoicesChange(choices.withTopic(topic)) },
                            label = { Text(topic) },
                        )
                    }
                    FilterChip(
                        selected = choices.ownTopic,
                        onClick = { onChoicesChange(choices.withOwnTopic()) },
                        label = { Text("Your own") },
                    )
                }
                if (choices.ownTopic) {
                    OutlinedTextField(
                        value = choices.ownText,
                        onValueChange = { onChoicesChange(choices.withOwnText(it)) },
                        label = { Text("Your own topic") },
                        supportingText = { Text("${choices.ownText.length} / $MAX_OWN_TOPIC_CHARS") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            SetupSection("Look up. What is the sky doing?") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SKY_OPTIONS.forEach { sky ->
                        FilterChip(
                            selected = choices.sky == sky,
                            onClick = { onChoicesChange(choices.withSky(sky)) },
                            label = { Text(sky) },
                        )
                    }
                }
                LocationAsk()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (model) {
                ModelState.Loading -> Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Getting the model ready")
                }
                ModelState.Ready -> Button(
                    onClick = onMakeList,
                    enabled = choices.canMakeList(modelReady = true),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Make my list")
                }
                is ModelState.Failed -> {
                    Text(
                        "The model did not load: ${model.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(onClick = onTryAgain, modifier = Modifier.fillMaxWidth()) {
                        Text("Try again")
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun SetupLoadingPreview() {
    LookaboutTheme {
        SetupScreen(
            choices = SetupChoices(),
            model = ModelState.Loading,
            onChoicesChange = {},
            onMakeList = {},
            onTryAgain = {},
            onOpenDebug = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun SetupOwnTopicPreview() {
    LookaboutTheme {
        SetupScreen(
            choices = SetupChoices(ownTopic = true, ownText = "Old signs", sky = "Some cloud"),
            model = ModelState.Ready,
            onChoicesChange = {},
            onMakeList = {},
            onTryAgain = {},
            onOpenDebug = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun SetupFailedPreview() {
    LookaboutTheme {
        SetupScreen(
            choices = SetupChoices(sky = "Clear"),
            model = ModelState.Failed("Model file not found"),
            onChoicesChange = {},
            onMakeList = {},
            onTryAgain = {},
            onOpenDebug = {},
        )
    }
}
