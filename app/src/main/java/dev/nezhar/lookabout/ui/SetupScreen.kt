package dev.nezhar.lookabout.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.nezhar.lookabout.MAX_OWN_TOPIC_CHARS
import dev.nezhar.lookabout.MODEL_SIZE_TEXT
import dev.nezhar.lookabout.PICTURE_COUNTS
import dev.nezhar.lookabout.R
import dev.nezhar.lookabout.SKY_OPTIONS
import dev.nezhar.lookabout.SUGGESTED_TOPICS
import dev.nezhar.lookabout.SetupChoices
import dev.nezhar.lookabout.downloadProgressText
import dev.nezhar.lookabout.ui.theme.ActionGap
import dev.nezhar.lookabout.ui.theme.ChipGap
import dev.nezhar.lookabout.ui.theme.ChipHeight
import dev.nezhar.lookabout.ui.theme.ChipRadius
import dev.nezhar.lookabout.ui.theme.LookaboutTheme
import dev.nezhar.lookabout.ui.theme.MainText
import dev.nezhar.lookabout.ui.theme.MinTarget
import dev.nezhar.lookabout.ui.theme.Outline
import dev.nezhar.lookabout.ui.theme.PageInset
import dev.nezhar.lookabout.ui.theme.PrimaryTint
import dev.nezhar.lookabout.ui.theme.ProgressBarHeight
import dev.nezhar.lookabout.ui.theme.SegmentHeight
import dev.nezhar.lookabout.ui.theme.SelectedBorder
import dev.nezhar.lookabout.ui.theme.TextFieldHeight
import dev.nezhar.lookabout.ui.theme.White
import dev.nezhar.lookabout.ui.theme.caption
import dev.nezhar.lookabout.ui.theme.chip
import dev.nezhar.lookabout.ui.theme.screenTitle
import dev.nezhar.lookabout.ui.theme.sectionLabel
import dev.nezhar.lookabout.ui.theme.segment

/** Above the title, under the status bar (read off the deck). */
private val TopPadding = 24.dp

/** Between the line under the title and the first section. */
private val HeaderGap = 28.dp

/** Between two sections. */
private val SectionGap = 20.dp

/** Between a section label and its control, and between the topic chips and the text field. */
private val LabelGap = 12.dp

/** Under the main button, above the navigation bar. */
private val BottomPadding = 16.dp

/**
 * Setup: the number of pictures, the topic, the sky and whether to check the photos at the end,
 * then Make my list. Values and words from design/values.md: "Lookabout" and its line, the
 * segmented control 3 to 6, the topic and sky chips (selected: light primary fill, 2 dp border,
 * a tick), "Your topic" for "Your own". The choices scroll, so the keyboard never hides the text
 * field; the main button stays pinned at the bottom: "Getting the model ready" (disabled) while the
 * model loads, "Couldn't get the model ready." and Try again when it failed. Without a model file
 * Download the model takes the place of Make my list, with its progress and Cancel while it runs.
 * A long press on the title opens the debug screen; nothing on screen shows it. Once a walk is
 * archived, the text action Past walks shows under the main button.
 *
 * @param showPastWalks true when the archive holds a walk: Past walks shows.
 * @param onPastWalks opens Past walks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    choices: SetupChoices,
    model: ModelState,
    onChoicesChange: (SetupChoices) -> Unit,
    onMakeList: () -> Unit,
    onTryAgain: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onOpenDebug: () -> Unit,
    modifier: Modifier = Modifier,
    showPastWalks: Boolean = false,
    onPastWalks: () -> Unit = {},
) {
    val openDebug by rememberUpdatedState(onOpenDebug)
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = PageInset, end = PageInset, top = TopPadding, bottom = 8.dp),
        ) {
            Text(
                "Lookabout",
                style = MaterialTheme.typography.screenTitle,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onLongPress = { openDebug() })
                },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "A little walk. A fresh look.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(HeaderGap))

            Column(verticalArrangement = Arrangement.spacedBy(SectionGap)) {
                SetupSection("Pictures") {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        PICTURE_COUNTS.forEachIndexed { index, count ->
                            SegmentedButton(
                                selected = choices.count == count,
                                onClick = { onChoicesChange(choices.withCount(count)) },
                                shape = SegmentedButtonDefaults.itemShape(index, PICTURE_COUNTS.size),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = PrimaryTint,
                                    activeContentColor = MainText,
                                    activeBorderColor = Outline,
                                    inactiveContainerColor = White,
                                    inactiveContentColor = MainText,
                                    inactiveBorderColor = Outline,
                                ),
                                modifier = Modifier.height(SegmentHeight),
                            ) {
                                Text("$count", style = MaterialTheme.typography.segment)
                            }
                        }
                    }
                }

                SetupSection("Topic") {
                    ChipRow {
                        SUGGESTED_TOPICS.forEach { topic ->
                            SetupChip(
                                label = topic,
                                selected = !choices.ownTopic && choices.topic == topic,
                                onClick = { onChoicesChange(choices.withTopic(topic)) },
                            )
                        }
                        SetupChip(
                            label = "Your own",
                            selected = choices.ownTopic,
                            onClick = { onChoicesChange(choices.withOwnTopic()) },
                        )
                    }
                    if (choices.ownTopic) {
                        val full = choices.ownText.length >= MAX_OWN_TOPIC_CHARS
                        OutlinedTextField(
                            value = choices.ownText,
                            onValueChange = { onChoicesChange(choices.withOwnText(it)) },
                            label = { Text("Your topic") },
                            // Only when typing stops working, so the field keeps the deck's 56 dp otherwise.
                            supportingText = if (full) {
                                { Text("At most $MAX_OWN_TOPIC_CHARS characters") }
                            } else {
                                null
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = White,
                                unfocusedContainerColor = White,
                                unfocusedBorderColor = Outline,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = TextFieldHeight),
                        )
                    }
                }

                SetupSection("Look up. What is the sky doing?") {
                    ChipRow {
                        SKY_OPTIONS.forEach { sky ->
                            SetupChip(
                                label = sky,
                                selected = choices.sky == sky,
                                onClick = { onChoicesChange(choices.withSky(sky)) },
                            )
                        }
                    }
                    LocationAsk()
                }

                CheckAtEndSwitch(
                    on = choices.checkAtEnd,
                    onChange = { onChoicesChange(choices.withCheckAtEnd(it)) },
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = PageInset, end = PageInset, top = 8.dp, bottom = BottomPadding),
            verticalArrangement = Arrangement.spacedBy(ActionGap),
        ) {
            when (model) {
                ModelState.Loading -> MainButton("Getting the model ready", onClick = {}, enabled = false)
                ModelState.Ready -> MainButton(
                    "Make my list",
                    onClick = onMakeList,
                    enabled = choices.canMakeList(modelReady = true),
                )
                is ModelState.Failed -> {
                    ErrorLine("Couldn't get the model ready.")
                    MainButton("Try again", onClick = onTryAgain)
                }
                ModelState.Missing -> {
                    StatusLine("Use Wi-Fi. This is needed once.")
                    MainButton("Download the model ($MODEL_SIZE_TEXT)", onClick = onDownload)
                }
                is ModelState.Downloading -> {
                    StatusLine(downloadProgressText(model.percent, model.pausedReason))
                    val percent = model.percent
                    val progressModifier = Modifier
                        .fillMaxWidth()
                        .height(ProgressBarHeight)
                    if (percent == null) {
                        LinearProgressIndicator(modifier = progressModifier, trackColor = PrimaryTint)
                    } else {
                        LinearProgressIndicator(
                            progress = { percent / 100f },
                            modifier = progressModifier,
                            trackColor = PrimaryTint,
                        )
                    }
                    TextAction(
                        "Cancel",
                        onClick = onCancelDownload,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
                is ModelState.DownloadFailed -> {
                    ErrorLine(model.message)
                    MainButton("Try again", onClick = onDownload)
                }
            }
            if (showPastWalks) {
                TextAction(
                    "Past walks",
                    onClick = onPastWalks,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

/** A line above the main button, such as the download progress: 16 sp in the secondary colour. */
@Composable
private fun StatusLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** An error line just above the main button, such as "Couldn't get the model ready.": 16 sp, red. */
@Composable
private fun ErrorLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
}

/** Chips that wrap to the next line, 8 dp apart both ways. */
@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(ChipGap),
        verticalArrangement = Arrangement.spacedBy(ChipGap),
    ) {
        content()
    }
}

/**
 * A topic or sky chip, 48 dp high with 12 dp corners: white with a thin outline, or when
 * [selected] the light primary fill, a 2 dp primary border, a tick and primary text.
 */
@Composable
private fun SetupChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.chip) },
        modifier = Modifier.height(ChipHeight),
        leadingIcon = if (selected) {
            {
                Icon(
                    painter = painterResource(R.drawable.ic_tick),
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else {
            null
        },
        shape = RoundedCornerShape(ChipRadius),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = White,
            labelColor = MainText,
            selectedContainerColor = PrimaryTint,
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Outline,
            selectedBorderColor = SelectedBorder,
            borderWidth = 1.dp,
            selectedBorderWidth = 2.dp,
        ),
    )
}

/**
 * "Check photos at the end": the walk takes every photo without waiting and checks them all at
 * Finish walk. Laid out like a section: the label in the section style, a caption under it, the
 * switch at the end. The whole row, at least 48 dp high, toggles the switch.
 */
@Composable
private fun CheckAtEndSwitch(on: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MinTarget)
            .toggleable(value = on, role = Role.Switch, onValueChange = onChange),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Check photos at the end", style = MaterialTheme.typography.sectionLabel)
            Text(
                "Shoot without waiting. Every photo is checked at Finish walk.",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // The row handles the tap, so the switch itself takes none.
        Switch(checked = on, onCheckedChange = null)
    }
}

/** A section: its label (about 18 sp, medium) and 12 dp under it the controls. */
@Composable
private fun SetupSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(LabelGap)) {
        Text(title, style = MaterialTheme.typography.sectionLabel)
        content()
    }
}

/** Room for a 24 dp status bar and a 24 dp gesture bar, as on the deck's 360 x 800 dp phone. */
private val PreviewBars = Modifier.padding(top = 24.dp, bottom = 24.dp)

/** The deck's sample choices: 5 pictures, "Your own" with "Autumn in the city", Overcast. */
private val sampleChoices = SetupChoices(ownTopic = true, ownText = "Autumn in the city", sky = "Overcast")

@Composable
private fun SetupPreview(choices: SetupChoices, model: ModelState, showPastWalks: Boolean = false) {
    LookaboutTheme {
        SetupScreen(
            choices = choices,
            model = model,
            onChoicesChange = {},
            onMakeList = {},
            onTryAgain = {},
            onDownload = {},
            onCancelDownload = {},
            onOpenDebug = {},
            modifier = PreviewBars,
            showPastWalks = showPastWalks,
        )
    }
}

/** What the app starts with: 5 pictures, the first topic, no sky yet, so Make my list waits. */
@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 360, heightDp = 800)
@Composable
private fun SetupDefaultsPreview() {
    SetupPreview(SetupChoices(), ModelState.Ready)
}

@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 360, heightDp = 800)
@Composable
private fun SetupSamplePreview() {
    SetupPreview(sampleChoices, ModelState.Ready)
}

/** With a walk in the archive: Past walks under Make my list. */
@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 360, heightDp = 800)
@Composable
private fun SetupPastWalksPreview() {
    SetupPreview(sampleChoices, ModelState.Ready, showPastWalks = true)
}

@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 412, heightDp = 915)
@Composable
private fun SetupSampleWidePreview() {
    SetupPreview(sampleChoices, ModelState.Ready)
}

@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 360, heightDp = 800)
@Composable
private fun SetupLoadingPreview() {
    SetupPreview(sampleChoices, ModelState.Loading)
}

@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 360, heightDp = 800)
@Composable
private fun SetupFailedPreview() {
    SetupPreview(sampleChoices, ModelState.Failed("Model file not found"))
}

@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 360, heightDp = 800)
@Composable
private fun SetupMissingPreview() {
    SetupPreview(sampleChoices, ModelState.Missing)
}

@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 360, heightDp = 800)
@Composable
private fun SetupDownloadingPreview() {
    SetupPreview(sampleChoices, ModelState.Downloading(percent = 42, pausedReason = null))
}

@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 360, heightDp = 800)
@Composable
private fun SetupDownloadFailedPreview() {
    SetupPreview(sampleChoices, ModelState.DownloadFailed("The download failed. Try again on Wi-Fi."))
}
