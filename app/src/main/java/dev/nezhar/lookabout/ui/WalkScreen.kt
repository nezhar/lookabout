package dev.nezhar.lookabout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.nezhar.lookabout.R
import dev.nezhar.lookabout.WalkStep
import dev.nezhar.lookabout.coverageText
import dev.nezhar.lookabout.endCheckProgress
import dev.nezhar.lookabout.endCheckTitle
import dev.nezhar.lookabout.modelWaitNote
import dev.nezhar.lookabout.positionText
import dev.nezhar.lookabout.verdictPhotoMaxHeight
import dev.nezhar.lookabout.ui.theme.ActionGap
import dev.nezhar.lookabout.ui.theme.ButtonIconSize
import dev.nezhar.lookabout.ui.theme.CheckingProgressWidth
import dev.nezhar.lookabout.ui.theme.LookaboutTheme
import dev.nezhar.lookabout.ui.theme.MainButtonHeight
import dev.nezhar.lookabout.ui.theme.PageInset
import dev.nezhar.lookabout.ui.theme.PrimaryTint
import dev.nezhar.lookabout.ui.theme.SecondaryActionHeight
import dev.nezhar.lookabout.ui.theme.TopBarHeight
import dev.nezhar.lookabout.ui.theme.VerdictPhotoMinHeight
import dev.nezhar.lookabout.ui.theme.button
import dev.nezhar.lookabout.ui.theme.caption
import dev.nezhar.lookabout.ui.theme.verdict
import dev.nezhar.lookabout.walk.Picture
import dev.nezhar.lookabout.walk.Shot
import dev.nezhar.lookabout.walk.Walk
import dev.nezhar.lookabout.walk.builtInPictures
import dev.nezhar.lookabout.walkStep

/** Above the top bar, under the status bar. */
private val TopPadding = 8.dp

/** Under the buttons, above the navigation bar. */
private val BottomPadding = 16.dp

/** Between the top bar and the photo on Checking, Check paused and Photo taken. */
private val PhotoGap = 24.dp

/** Between the photo, the title, the progress indicator and the line under it on Checking. */
private val StatusGap = 28.dp

/** The smallest gap above the buttons on Next picture, Accepted and Not yet. */
private val ButtonsGap = 8.dp

/**
 * Walk: one picture at a time, in four screens: Next picture, Checking, Accepted and Not yet,
 * plus Check paused after a failed check. When the walk checks its photos at the end, Photo
 * taken shows instead of Checking after Shoot. Which one shows follows from the current shot,
 * [checking], [checkPaused] and the walk's setting (see [walkStep]). The screen stays on while
 * Checking shows.
 *
 * Layout from design/values.md: a 20 dp page inset, the counter and progress dots in a 48 dp top
 * bar 8 dp under the status bar, and at the bottom, 16 dp above the navigation bar, the 56 dp main
 * button with the 48 dp text action 8 dp under it. Every screen fits 360 x 800 dp without
 * scrolling. With a 24 dp status bar and a 48 dp three-button navigation bar that leaves 728 dp,
 * and with the test texts (the previews) it works out as:
 *
 * - Next picture: 8 top + 48 bar + 28 + 20 topic + 24 = 128 above the picture text; 16 + 56 Shoot
 *   + 8 + 48 Skip + 16 bottom = 144 below it, leaving 456. The longest picture text a walk
 *   allows, "Something small and overlooked, shot so close it fills the whole frame", takes 7
 *   lines of 57 / 64 sp at 320 dp wide (as on the deck's long prompt screen): 448, so it keeps
 *   the largest size. A text that needs 8 lines (512) drops to 45 / 52, then to 36 / 44.
 * - Checking: 8 + 48 bar + 24 + 240 photo (320 x 240, 4:3) + 28 + 40 "Checking" + 28 + 4
 *   indicator + 28 + 20 "You can keep looking around." = 468.
 * - Accepted and Not yet: 8 + 48 bar + 16 + 48 verdict + 24 coverage + 12 + photo + 12 + rows +
 *   4 + 20 hint, then at least 8 + 56 Retake + 8 + 48 Skip + 16 bottom: 328 plus the photo and
 *   rows, leaving 400 for them. The photo's cap is 144 here (704 dp of content, see
 *   [verdictPhotoMaxHeight]). Four two-line test criteria take 4 x 64 = 256 (40 text + 16
 *   padding fits 64), so the photo keeps its 144. Four corrected two-line criteria take 4 x 80 =
 *   320 (40 + 18 "Changed by you" + 16 fits 80), and the photo gives up height down to its 80.
 *   Accepted has no Skip, so 56 more to spare. With a 24 dp gesture bar (728 dp of content, cap
 *   still 144) this is the deck's geometry: photo at y 180, criteria at y 336, Retake at y 648,
 *   Skip at y 712. At 412 x 915 with 24 dp bars the content is 843 dp, the cap 201 and the space
 *   for the photo and rows 867 - 328 = 539, so the photo shows at about 200 as in the deck, even
 *   with four corrected rows (320).
 * - Check paused: 8 + 48 bar + 24 + 240 photo + 28 + 40 "Check paused" + 36 + 56 error card +
 *   8 + 56 Try again + 8 + 48 Skip + 16 bottom = 616.
 * - Photo taken: as Check paused with 28 + 20 for its line of text in place of 36 + 56 for the
 *   card, Retake in place of Skip: 572.
 *
 * @param checking true while the current photo is being checked; no actions are offered then.
 * @param checkPaused true when the check of the current photo failed.
 * @param onShoot takes a photo for the current picture (Next picture).
 * @param onSkip moves on without an accepted photo (Next picture, Not yet, Check paused); on the last picture it opens Summary.
 * @param onRetake takes a new photo for the current picture (Not yet, Photo taken).
 * @param onTryAgain checks the same photo again (Check paused).
 * @param onNextPicture moves on after Accepted or Photo taken; on the last picture this is Finish walk.
 * @param onFinish opens Summary; only offered if the walk is already past its last picture.
 * @param onFlip changes the answer to a question (0 to 3) on Accepted and Not yet; a second tap
 *   restores Gemma's answer.
 */
@Composable
fun WalkScreen(
    walk: Walk,
    checking: Boolean,
    checkPaused: Boolean,
    onShoot: () -> Unit,
    onSkip: () -> Unit,
    onRetake: () -> Unit,
    onTryAgain: () -> Unit,
    onNextPicture: () -> Unit,
    onFinish: () -> Unit,
    onFlip: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shot = walk.current
    val screenModifier = modifier
        .fillMaxSize()
        .padding(start = PageInset, end = PageInset, top = TopPadding, bottom = BottomPadding)
    if (shot == null) {
        // Next picture and Skip on the last picture open Summary, so this does not normally show.
        Box(modifier = screenModifier, contentAlignment = Alignment.BottomCenter) {
            Actions("Finish walk", R.drawable.ic_arrow_forward, onFinish)
        }
        return
    }
    val nextLabel = if (walk.isLast) "Finish walk" else "Next picture"
    when (walkStep(shot, checking, checkPaused, walk.checkAtEnd)) {
        WalkStep.NextPicture -> NextPictureScreen(walk, shot, onShoot, onSkip, screenModifier)
        WalkStep.Checking -> {
            KeepScreenOn()
            CheckingScreen(walk, shot, screenModifier)
        }
        WalkStep.Accepted -> VerdictScreen(walk, shot, accepted = true, onFlip = onFlip, modifier = screenModifier) {
            Actions(nextLabel, R.drawable.ic_arrow_forward, onNextPicture)
        }
        WalkStep.NotYet -> VerdictScreen(walk, shot, accepted = false, onFlip = onFlip, modifier = screenModifier) {
            Actions("Retake", R.drawable.ic_camera, onRetake, "Skip", onSkip)
        }
        WalkStep.CheckPaused -> PhotoStatusScreen(walk, shot, "Check paused", modifier = screenModifier, actions = {
            Actions("Try again", null, onTryAgain, "Skip", onSkip)
        }) {
            Spacer(Modifier.height(36.dp))
            ErrorCard("Couldn't check this photo.")
        }
        WalkStep.PhotoTaken -> PhotoStatusScreen(walk, shot, "Photo taken", modifier = screenModifier, actions = {
            Actions(nextLabel, R.drawable.ic_arrow_forward, onNextPicture, "Retake", onRetake)
        }) {
            Spacer(Modifier.height(StatusGap))
            SecondaryLine("Checked at the end of the walk.")
        }
    }
}

/**
 * The check at the end of the walk, laid out like Checking: how far the check is in the top bar,
 * the photo being checked, "Checking 2 of 5" with an indeterminate indicator, and the picture
 * text. Until the model is ready the title says "Getting the model ready" instead, with a line
 * under it when the model is not on the phone or failed to load, and Show results now as a text
 * action at the bottom. No other actions; the screen stays on. With the test texts at 360 x 800
 * dp: 8 + 48 bar + 24 + 240 photo + 28 + 40 + 28 + 4 + 28 + 40 (the test picture text in two
 * lines) = 488; waiting for a missing model adds 28 + 48 for the note (two lines) and 8 + 48
 * for Show results now: 620; for a model that failed to load the note takes three lines (72): 644.
 *
 * @param shotIndex the shot whose photo is being checked, or checked first once the model is ready.
 * @param waitingForModel true while the check waits for the model.
 * @param modelUnavailable true when the model is not on the phone or failed to load; a waiting
 *   check says so under the heading.
 * @param modelMissing true when the model is not on the phone: the note then points to Show
 *   results now, as the model cannot get ready during the walk.
 * @param canShowResultsNow true when Show results now shows: the check waits for a model that is
 *   not on the phone or failed to load.
 * @param onShowResultsNow stops the check and opens Summary.
 */
@Composable
fun EndCheckScreen(
    walk: Walk,
    shotIndex: Int,
    modifier: Modifier = Modifier,
    waitingForModel: Boolean = false,
    modelUnavailable: Boolean = false,
    modelMissing: Boolean = false,
    canShowResultsNow: Boolean = false,
    onShowResultsNow: () -> Unit = {},
) {
    KeepScreenOn()
    val shot = walk.shots.getOrNull(shotIndex)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = PageInset, end = PageInset, top = TopPadding, bottom = BottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TopBarHeight),
                contentAlignment = Alignment.Center,
            ) {
                LinearProgressIndicator(
                    progress = { endCheckProgress(walk, shotIndex) },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = PrimaryTint,
                )
            }
            if (shot != null) {
                Spacer(Modifier.height(PhotoGap))
                StatusPhoto(shot.photoPath)
            }
            Spacer(Modifier.height(StatusGap))
            StatusTitle(endCheckTitle(walk, shotIndex, waitingForModel))
            val note = if (waitingForModel) modelWaitNote(modelUnavailable, modelMissing) else null
            if (note != null) {
                Spacer(Modifier.height(StatusGap))
                SecondaryLine(note)
            }
            Spacer(Modifier.height(StatusGap))
            CheckingIndicator()
            if (shot != null) {
                Spacer(Modifier.height(StatusGap))
                SecondaryLine(shot.text)
            }
        }
        if (canShowResultsNow) {
            Spacer(Modifier.height(ButtonsGap))
            TextButton(
                onClick = onShowResultsNow,
                modifier = Modifier.heightIn(min = SecondaryActionHeight),
            ) {
                Text("Show results now", style = MaterialTheme.typography.button)
            }
        }
    }
}

/** The top bar of the loop screens: "2 of 5" at the start, the progress dots at the end. */
@Composable
private fun TopBar(walk: Walk, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(TopBarHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            positionText(walk.currentIndex, walk.shots.size),
            style = MaterialTheme.typography.bodyLarge,
        )
        ProgressDots(count = walk.shots.size, current = walk.currentIndex)
    }
}

/**
 * Next picture: the top bar, the walk's topic, the picture text in the largest of the three
 * prompt sizes that fits, then Shoot and Skip.
 */
@Composable
private fun NextPictureScreen(
    walk: Walk,
    shot: Shot,
    onShoot: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        TopBar(walk)
        Text(
            walk.topic,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 28.dp),
        )
        PictureText(
            shot.text,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 16.dp),
        )
        Actions("Shoot", R.drawable.ic_camera, onShoot, "Skip", onSkip)
    }
}

/**
 * Checking: the photo just taken, "Checking", an indeterminate indicator and "You can keep
 * looking around.", all centred. No actions.
 */
@Composable
private fun CheckingScreen(walk: Walk, shot: Shot, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopBar(walk)
        Spacer(Modifier.height(PhotoGap))
        StatusPhoto(shot.photoPath)
        Spacer(Modifier.height(StatusGap))
        StatusTitle("Checking")
        Spacer(Modifier.height(StatusGap))
        CheckingIndicator()
        Spacer(Modifier.height(StatusGap))
        SecondaryLine("You can keep looking around.")
    }
}

/**
 * Accepted or Not yet: the verdict and coverage, the photo, the four criteria with ticks or
 * crosses and the hint, then [actions] at the bottom. Tapping a criterion calls [onFlip] with its
 * index; the verdict, coverage and actions follow the shot's answers after corrections.
 *
 * The photo is full width, at most [verdictPhotoMaxHeight] high for the screen's height (144 dp
 * at 360 x 800, about 200 at 412 x 915), and gives up height, down to [VerdictPhotoMinHeight],
 * when the criteria rows grow, so the actions stay in place (see [WalkScreen] for the numbers).
 */
@Composable
private fun VerdictScreen(
    walk: Walk,
    shot: Shot,
    accepted: Boolean,
    onFlip: (Int) -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit,
) {
    val answers = shot.finalAnswers
    BoxWithConstraints(modifier = modifier) {
        val photoMax = verdictPhotoMaxHeight(maxHeight.value).dp
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                TopBar(walk)
                Spacer(Modifier.height(16.dp))
                VerdictHeader(accepted, coverageText(answers))
                Spacer(Modifier.height(12.dp))
                WalkPhoto(
                    shot.photoPath,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(min = VerdictPhotoMinHeight, max = photoMax)
                        .fillMaxWidth()
                        .fillMaxHeight(),
                )
                Spacer(Modifier.height(12.dp))
                shot.questions.forEachIndexed { index, question ->
                    CriterionRow(
                        question,
                        yes = answers.getOrElse(index) { false },
                        corrected = shot.corrected.getOrElse(index) { false },
                        last = index == shot.questions.lastIndex,
                        onTap = { onFlip(index) },
                    )
                }
                Text(
                    "Tap a check to change it.",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(ButtonsGap))
            actions()
        }
    }
}

/**
 * Check paused and Photo taken: the top bar, the photo as on Checking, [title], then [below]
 * (the error card, or a line of text), and [actions] at the bottom. No result is shown.
 */
@Composable
private fun PhotoStatusScreen(
    walk: Walk,
    shot: Shot,
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit,
    below: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopBar(walk)
            Spacer(Modifier.height(PhotoGap))
            StatusPhoto(shot.photoPath)
            Spacer(Modifier.height(StatusGap))
            StatusTitle(title)
            below()
        }
        Spacer(Modifier.height(ButtonsGap))
        actions()
    }
}

/** The photo on Checking, Check paused and Photo taken: full width at 4:3, smaller if it must. */
@Composable
private fun ColumnScope.StatusPhoto(path: String?) {
    WalkPhoto(
        path,
        modifier = Modifier
            .weight(1f, fill = false)
            .aspectRatio(4f / 3f),
    )
}

/** "Checking", "Check paused", "Photo taken": centred, in the Verdict style. */
@Composable
private fun StatusTitle(text: String) {
    Text(text, style = MaterialTheme.typography.verdict, textAlign = TextAlign.Center)
}

/** A centred line in the secondary colour, such as "You can keep looking around.". */
@Composable
private fun SecondaryLine(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/** The indeterminate check indicator: 180 dp wide, primary on its light track. */
@Composable
private fun CheckingIndicator() {
    LinearProgressIndicator(
        modifier = Modifier.width(CheckingProgressWidth),
        trackColor = PrimaryTint,
    )
}

/**
 * The actions at the bottom: the main button ([label], with [icon] before it when given), full
 * width, and under it, 8 dp apart, the text action [secondaryLabel] when given.
 */
@Composable
private fun Actions(
    label: String,
    icon: Int?,
    onClick: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ActionGap),
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(MainButtonHeight),
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonIconSize),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(label, style = MaterialTheme.typography.button)
        }
        if (secondaryLabel != null) {
            TextButton(
                onClick = onSecondary,
                modifier = Modifier.heightIn(min = SecondaryActionHeight),
            ) {
                Text(secondaryLabel, style = MaterialTheme.typography.button)
            }
        }
    }
}

/** The layout test texts: the longest picture text and criteria a walk allows. */
private val previewPicture = Picture(
    "Something small and overlooked, shot so close it fills the whole frame",
    listOf(
        "Are streetlights or shop windows switched on in the picture?",
        "Is the subject sharp, detailed and filling the whole frame?",
        "Does the background look plain, soft or blurred behind it?",
        "Was it shot close enough to show fine surface textures?",
    ),
)

/** A five-picture walk on the test picture, which is the second: "2 of 5". */
private fun previewWalk(): Walk = Walk.start(
    "Small details",
    "Overcast",
    listOf(builtInPictures[0], previewPicture) + builtInPictures.subList(1, 4),
    0L,
).copy(currentIndex = 1)

/** Room for a 24 dp status bar and a 48 dp three-button navigation bar, as on a 360 x 800 dp phone. */
private val PreviewBars = Modifier.padding(top = 24.dp, bottom = 48.dp)

private fun Walk.withPreviewPhoto(): Walk = photoRequested("/preview/photo.jpg").photoTaken()

@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 360, heightDp = 800)
@Composable
private fun NextPicturePreview() {
    LookaboutTheme {
        WalkScreen(previewWalk(), false, false, {}, {}, {}, {}, {}, {}, {}, modifier = PreviewBars)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 360, heightDp = 800)
@Composable
private fun CheckingPreview() {
    LookaboutTheme {
        WalkScreen(previewWalk().withPreviewPhoto(), true, false, {}, {}, {}, {}, {}, {}, {}, modifier = PreviewBars)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 360, heightDp = 800)
@Composable
private fun AcceptedPreview() {
    val walk = previewWalk().withPreviewPhoto().recordCheck(listOf(true, true, false, true), 2000)
    LookaboutTheme {
        WalkScreen(walk, false, false, {}, {}, {}, {}, {}, {}, {}, modifier = PreviewBars)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 360, heightDp = 800)
@Composable
private fun NotYetPreview() {
    val walk = previewWalk().withPreviewPhoto().recordCheck(listOf(false, true, false, false), 2000)
    LookaboutTheme {
        WalkScreen(walk, false, false, {}, {}, {}, {}, {}, {}, {}, modifier = PreviewBars)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 360, heightDp = 800)
@Composable
private fun CheckPausedPreview() {
    LookaboutTheme {
        WalkScreen(previewWalk().withPreviewPhoto(), false, true, {}, {}, {}, {}, {}, {}, {}, modifier = PreviewBars)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 360, heightDp = 800)
@Composable
private fun PhotoTakenPreview() {
    LookaboutTheme {
        WalkScreen(previewWalk().copy(checkAtEnd = true).withPreviewPhoto(), false, false, {}, {}, {}, {}, {}, {}, {}, modifier = PreviewBars)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 360, heightDp = 800)
@Composable
private fun EndCheckPreview() {
    val walk = previewWalk().copy(checkAtEnd = true).withPreviewPhoto().copy(currentIndex = 5)
    LookaboutTheme {
        EndCheckScreen(walk, shotIndex = 1, modifier = PreviewBars)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 360, heightDp = 800)
@Composable
private fun EndCheckWaitingPreview() {
    val walk = previewWalk().copy(checkAtEnd = true).withPreviewPhoto().copy(currentIndex = 5)
    LookaboutTheme {
        EndCheckScreen(
            walk,
            shotIndex = 1,
            modifier = PreviewBars,
            waitingForModel = true,
            modelUnavailable = true,
            modelMissing = true,
            canShowResultsNow = true,
        )
    }
}

/**
 * Not yet with all four answers changed by the walker, at 360 x 800 dp with room left for a 24 dp
 * status bar and a 48 dp navigation bar: the photo gives up height, down to 80 dp, so the hint and
 * both buttons stay clear of the criteria.
 */
@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 360, heightDp = 800)
@Composable
private fun NotYetCorrectedPreview() {
    val walk = previewWalk().withPreviewPhoto()
        .recordCheck(listOf(true, true, true, false), 2000)
        .flip(0).flip(1).flip(2).flip(3)
    LookaboutTheme {
        WalkScreen(
            walk,
            false,
            false,
            {}, {}, {}, {}, {}, {}, {},
            modifier = PreviewBars,
        )
    }
}
