package dev.nezhar.lookabout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.nezhar.lookabout.WalkStep
import dev.nezhar.lookabout.coverageText
import dev.nezhar.lookabout.endCheckProgress
import dev.nezhar.lookabout.endCheckText
import dev.nezhar.lookabout.positionText
import dev.nezhar.lookabout.ui.theme.LookaboutTheme
import dev.nezhar.lookabout.walk.Picture
import dev.nezhar.lookabout.walk.Shot
import dev.nezhar.lookabout.walk.Walk
import dev.nezhar.lookabout.walk.builtInPictures
import dev.nezhar.lookabout.walkStep

/** Height of the buttons at the bottom: above the 48 dp minimum, for a thumb outdoors. */
private val BottomButtonHeight = 56.dp

/** The tallest the photo on Accepted and Not yet gets. */
private val PhotoMaxHeight = 144.dp

/** The photo on Accepted and Not yet gives up height for the criteria down to this. */
private val PhotoMinHeight = 80.dp

/**
 * Walk: one picture at a time, in four screens: Next picture, Checking, Accepted and Not yet,
 * plus Check paused after a failed check. When the walk checks its photos at the end, Photo
 * taken shows instead of Checking after Shoot. Which one shows follows from the current shot,
 * [checking], [checkPaused] and the walk's setting (see [walkStep]). The screen stays on while
 * Checking shows.
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
        .padding(horizontal = 24.dp, vertical = 16.dp)
    if (shot == null) {
        // Next picture and Skip on the last picture open Summary, so this does not normally show.
        Box(modifier = screenModifier, contentAlignment = Alignment.BottomCenter) {
            MainButton("Finish walk", onFinish, Modifier.fillMaxWidth())
        }
        return
    }
    when (walkStep(shot, checking, checkPaused, walk.checkAtEnd)) {
        WalkStep.NextPicture -> NextPictureScreen(walk, shot, onShoot, onSkip, screenModifier)
        WalkStep.Checking -> {
            KeepScreenOn()
            CheckingScreen(walk, shot, screenModifier)
        }
        WalkStep.Accepted -> VerdictScreen(shot, accepted = true, onFlip = onFlip, modifier = screenModifier) {
            MainButton(
                if (walk.isLast) "Finish walk" else "Next picture",
                onNextPicture,
                Modifier.weight(1f),
            )
        }
        WalkStep.NotYet -> VerdictScreen(shot, accepted = false, onFlip = onFlip, modifier = screenModifier) {
            SecondaryButton("Skip", onSkip, Modifier.weight(1f))
            MainButton("Retake", onRetake, Modifier.weight(1f))
        }
        WalkStep.CheckPaused -> CheckPausedScreen(shot, modifier = screenModifier) {
            SecondaryButton("Skip", onSkip, Modifier.weight(1f))
            MainButton("Try again", onTryAgain, Modifier.weight(1f))
        }
        WalkStep.PhotoTaken -> PhotoTakenScreen(walk, shot, modifier = screenModifier) {
            SecondaryButton("Retake", onRetake, Modifier.weight(1f))
            MainButton(
                if (walk.isLast) "Finish walk" else "Next picture",
                onNextPicture,
                Modifier.weight(1f),
            )
        }
    }
}

/**
 * The check at the end of the walk: "Checking 2 of 5" with a spinner, how far the check is, and
 * the picture and photo being checked. No actions; the screen stays on.
 *
 * @param shotIndex the shot whose photo is being checked.
 */
@Composable
fun EndCheckScreen(walk: Walk, shotIndex: Int, modifier: Modifier = Modifier) {
    KeepScreenOn()
    val shot = walk.shots.getOrNull(shotIndex)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
            Text(endCheckText(walk, shotIndex), style = MaterialTheme.typography.headlineMedium)
        }
        LinearProgressIndicator(
            progress = { endCheckProgress(walk, shotIndex) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (shot != null) {
            Text(shot.text, style = MaterialTheme.typography.titleLarge)
            WalkPhoto(
                shot.photoPath,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .align(Alignment.CenterHorizontally)
                    .aspectRatio(3f / 4f),
            )
        }
    }
}

/**
 * Photo taken, when the photos are checked at the end: the position, the title, one line of text
 * and the photo, laid out like Check paused, then [buttons] at the bottom. No result is shown.
 */
@Composable
private fun PhotoTakenScreen(
    walk: Walk,
    shot: Shot,
    modifier: Modifier = Modifier,
    buttons: @Composable RowScope.() -> Unit,
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PositionRow(walk)
            Column {
                Text(
                    "Photo taken",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text("Checked at the end of the walk.", style = MaterialTheme.typography.titleLarge)
            }
            WalkPhoto(
                shot.photoPath,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .align(Alignment.CenterHorizontally)
                    .heightIn(min = 80.dp)
                    .aspectRatio(4f / 3f),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = buttons,
        )
    }
}

/** "2 of 5" and the progress dots. */
@Composable
private fun PositionRow(walk: Walk, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            positionText(walk.currentIndex, walk.shots.size),
            style = MaterialTheme.typography.titleLarge,
        )
        ProgressDots(count = walk.shots.size, current = walk.currentIndex)
    }
}

/** Next picture: the position, the picture text as large as fits, then Skip and Shoot. */
@Composable
private fun NextPictureScreen(
    walk: Walk,
    shot: Shot,
    onShoot: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        PositionRow(walk)
        PictureText(
            shot.text,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 24.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SecondaryButton("Skip", onSkip, Modifier.weight(1f))
            MainButton("Shoot", onShoot, Modifier.weight(1f))
        }
    }
}

/** Checking: the photo just taken and "Checking" with a progress indicator. No actions. */
@Composable
private fun CheckingScreen(walk: Walk, shot: Shot, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PositionRow(walk)
        WalkPhoto(
            shot.photoPath,
            modifier = Modifier
                .weight(1f, fill = false)
                .align(Alignment.CenterHorizontally)
                .aspectRatio(3f / 4f),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
            Text("Checking", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

/**
 * Accepted or Not yet: the verdict, the coverage, the photo, the four criteria with ticks or
 * crosses and the hint, then [buttons] at the bottom. Tapping a criterion calls [onFlip] with its
 * index; the verdict, coverage and buttons follow the shot's answers after corrections.
 *
 * The photo is at most [PhotoMaxHeight] high and gives up height, down to [PhotoMinHeight], when
 * the criteria need it. At 360 x 800 dp with a 24 dp status bar and a 48 dp navigation bar, Not yet
 * with four corrected three-line criteria works out as: 800 - 72 bars - 32 screen padding = 696;
 * buttons 16 + 56 = 72, leaving 624 for the column above them; verdict and coverage 36 + 28 = 64,
 * two gaps of 12, criteria 4 x (72 text + 8 padding + 16 "Changed by you") = 384, hint 8 + 20 = 28,
 * so 500, which leaves 124 dp for the photo (two-line criteria leave 220, capped at 144).
 */
@Composable
private fun VerdictScreen(
    shot: Shot,
    accepted: Boolean,
    onFlip: (Int) -> Unit,
    modifier: Modifier = Modifier,
    buttons: @Composable RowScope.() -> Unit,
) {
    val answers = shot.finalAnswers
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                VerdictHeader(accepted)
                Text(coverageText(answers), style = MaterialTheme.typography.titleLarge)
            }
            WalkPhoto(
                shot.photoPath,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .align(Alignment.CenterHorizontally)
                    .heightIn(min = PhotoMinHeight, max = PhotoMaxHeight)
                    .aspectRatio(4f / 3f),
            )
            Column {
                shot.questions.forEachIndexed { index, question ->
                    CriterionRow(
                        question,
                        yes = answers.getOrElse(index) { false },
                        corrected = shot.corrected.getOrElse(index) { false },
                        onTap = { onFlip(index) },
                    )
                }
                Text(
                    "Tap a check to change it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = buttons,
        )
    }
}

/**
 * Check paused: the check of the photo failed. The title, one line of text and the photo, laid
 * out like Not yet, then [buttons] at the bottom. No result is shown.
 */
@Composable
private fun CheckPausedScreen(
    shot: Shot,
    modifier: Modifier = Modifier,
    buttons: @Composable RowScope.() -> Unit,
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(
                    "Check paused",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text("Couldn't check this photo.", style = MaterialTheme.typography.titleLarge)
            }
            WalkPhoto(
                shot.photoPath,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .align(Alignment.CenterHorizontally)
                    .heightIn(min = 80.dp)
                    .aspectRatio(4f / 3f),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = buttons,
        )
    }
}

@Composable
private fun MainButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier.heightIn(min = BottomButtonHeight)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SecondaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = BottomButtonHeight)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
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

private fun Walk.withPreviewPhoto(): Walk = photoRequested("/preview/photo.jpg").photoTaken()

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun NextPicturePreview() {
    LookaboutTheme {
        WalkScreen(previewWalk(), false, false, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun CheckingPreview() {
    LookaboutTheme {
        WalkScreen(previewWalk().withPreviewPhoto(), true, false, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun AcceptedPreview() {
    val walk = previewWalk().withPreviewPhoto().recordCheck(listOf(true, true, false, true), 2000)
    LookaboutTheme {
        WalkScreen(walk, false, false, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun NotYetPreview() {
    val walk = previewWalk().withPreviewPhoto().recordCheck(listOf(false, true, false, false), 2000)
    LookaboutTheme {
        WalkScreen(walk, false, false, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun CheckPausedPreview() {
    LookaboutTheme {
        WalkScreen(previewWalk().withPreviewPhoto(), false, true, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun PhotoTakenPreview() {
    LookaboutTheme {
        WalkScreen(previewWalk().copy(checkAtEnd = true).withPreviewPhoto(), false, false, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun EndCheckPreview() {
    val walk = previewWalk().copy(checkAtEnd = true).withPreviewPhoto().copy(currentIndex = 5)
    LookaboutTheme {
        EndCheckScreen(walk, shotIndex = 1)
    }
}

/**
 * Not yet with all four answers changed by the walker, at 360 x 800 dp with room left for a 24 dp
 * status bar and a 48 dp navigation bar: the photo gives up height so the hint and both buttons
 * stay clear of the criteria.
 */
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
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
            modifier = Modifier.padding(top = 24.dp, bottom = 48.dp),
        )
    }
}
