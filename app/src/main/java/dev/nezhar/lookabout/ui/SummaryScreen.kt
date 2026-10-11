package dev.nezhar.lookabout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.nezhar.lookabout.NOT_CHECKED_TEXT
import dev.nezhar.lookabout.R
import dev.nezhar.lookabout.acceptedTotalText
import dev.nezhar.lookabout.agreementText
import dev.nezhar.lookabout.badgeText
import dev.nezhar.lookabout.checkTimeText
import dev.nezhar.lookabout.coverageText
import dev.nezhar.lookabout.ui.theme.Accepted
import dev.nezhar.lookabout.ui.theme.AcceptedTint
import dev.nezhar.lookabout.ui.theme.ActionGap
import dev.nezhar.lookabout.ui.theme.ChipRadius
import dev.nezhar.lookabout.ui.theme.FaintText
import dev.nezhar.lookabout.ui.theme.LookaboutTheme
import dev.nezhar.lookabout.ui.theme.NeutralFill
import dev.nezhar.lookabout.ui.theme.NotYet
import dev.nezhar.lookabout.ui.theme.NotYetTint
import dev.nezhar.lookabout.ui.theme.PageInset
import dev.nezhar.lookabout.ui.theme.RowIconSize
import dev.nezhar.lookabout.ui.theme.SecondaryText
import dev.nezhar.lookabout.ui.theme.SummaryRowGap
import dev.nezhar.lookabout.ui.theme.SummaryThumbCompact
import dev.nezhar.lookabout.ui.theme.SummaryThumbWide
import dev.nezhar.lookabout.ui.theme.caption
import dev.nezhar.lookabout.ui.theme.summaryChip
import dev.nezhar.lookabout.ui.theme.summaryDetail
import dev.nezhar.lookabout.ui.theme.summaryHeadline
import dev.nezhar.lookabout.ui.theme.summaryPrompt
import dev.nezhar.lookabout.ui.theme.summaryTitle
import dev.nezhar.lookabout.walk.Outcome
import dev.nezhar.lookabout.walk.Shot
import dev.nezhar.lookabout.walk.Walk
import dev.nezhar.lookabout.walk.builtInPictures
import dev.nezhar.lookabout.walkDateText
import java.io.File

/** Above "Your walk", under the status bar (read off the deck). */
private val TopPadding = 16.dp

/** Under the main button, above the navigation bar. */
private val BottomPadding = 16.dp

/** The 412 dp screens set thumbnails and text larger than the 360 dp ones. */
private val WideFrom = 400.dp

/** The icons in the status chips and on the "No photo" tile's chip. */
private val ChipIconSize = 16.dp

/**
 * Summary, the contact sheet of the walk: "Your walk", the topic, the date the walk started and
 * the total ("3 of 5 accepted") with a tick in the accepted colour; one row per picture with a
 * square thumbnail (or a "No photo" tile), "1. <picture>", coverage and check time ("75% · 6 s",
 * or "Not checked") and a status chip; then how often the walker and Gemma agreed (only when a
 * photo was checked). Values from design/values.md. New walk stays pinned at the bottom, with
 * Check again as a text action under it while a photo has no result; when the rows do not fit,
 * such as six pictures at 360 dp, everything above them scrolls under them.
 *
 * @param onNewWalk returns to Setup.
 * @param canCheckAgain true when a photo has no result: Check again shows.
 * @param onCheckAgain checks the photos without a result again, then Summary shows again.
 */
@Composable
fun SummaryScreen(
    walk: Walk,
    onNewWalk: () -> Unit,
    modifier: Modifier = Modifier,
    canCheckAgain: Boolean = false,
    onCheckAgain: () -> Unit = {},
) {
    val date = remember(walk.startedAtMillis) { walkDateText(walk.startedAtMillis) }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth >= WideFrom
        val thumbSize = if (wide) SummaryThumbWide else SummaryThumbCompact
        val promptStyle = MaterialTheme.typography.summaryPrompt(wide)
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = PageInset, end = PageInset, top = TopPadding, bottom = 8.dp),
            ) {
                item {
                    SummaryHeader(walk, date, wide, modifier = Modifier.padding(bottom = 16.dp))
                }
                itemsIndexed(walk.shots) { index, shot ->
                    ShotRow(
                        number = index + 1,
                        shot = shot,
                        thumbSize = thumbSize,
                        promptStyle = promptStyle,
                        modifier = Modifier.padding(
                            bottom = if (index == walk.shots.lastIndex) 0.dp else SummaryRowGap,
                        ),
                    )
                }
                agreementText(walk.agreedCount, walk.checkCount)?.let { agreement ->
                    item {
                        Text(
                            agreement,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 20.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = PageInset, end = PageInset, top = 8.dp, bottom = BottomPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ActionGap),
            ) {
                MainButton("New walk", onClick = onNewWalk)
                if (canCheckAgain) {
                    TextAction("Check again", onClick = onCheckAgain)
                }
            }
        }
    }
}

/** "Your walk", the topic, the date and the total with its tick. */
@Composable
private fun SummaryHeader(walk: Walk, date: String, wide: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            "Your walk",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(walk.topic, style = MaterialTheme.typography.summaryTitle)
        Spacer(Modifier.height(4.dp))
        Text(
            date,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_tick),
                contentDescription = null,
                tint = Accepted,
                modifier = Modifier.size(RowIconSize),
            )
            Text(
                acceptedTotalText(walk.acceptedCount, walk.shots.size),
                style = MaterialTheme.typography.summaryHeadline(wide),
                color = Accepted,
            )
        }
    }
}

/**
 * One picture: the thumbnail, and beside it "[number]. <picture>" at the top and the per-photo
 * line and status chip at the bottom, level with the thumbnail's lower edge.
 */
@Composable
private fun ShotRow(
    number: Int,
    shot: Shot,
    thumbSize: Dp,
    promptStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Thumbnail(shot.photoPath, thumbSize)
        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = thumbSize),
        ) {
            Text("$number. ${shot.text}", style = promptStyle)
            Spacer(Modifier.weight(1f))
            val badge = badgeText(shot.outcome, shot.hasPhoto)
            val checkMillis = shot.checkMillis
            val detail = when {
                shot.isChecked && checkMillis != null ->
                    "${coverageText(shot.coveragePercent)} · ${checkTimeText(checkMillis)}"
                // The chip already says it.
                badge == NOT_CHECKED_TEXT -> null
                else -> NOT_CHECKED_TEXT
            }
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.summaryDetail, color = FaintText)
            }
            Spacer(Modifier.height(2.dp))
            StatusChip(shot.outcome, badge)
        }
    }
}

/**
 * The photo as a square with 12 dp corners, decoded off the main thread at that size, or without
 * a photo a neutral tile with the skip icon and "No photo".
 */
@Composable
private fun Thumbnail(photoPath: String?, side: Dp) {
    Box(
        modifier = Modifier
            .size(side)
            .clip(RoundedCornerShape(ChipRadius))
            .background(NeutralFill),
        contentAlignment = Alignment.Center,
    ) {
        if (photoPath != null) {
            val standIn = remember { ColorPainter(NeutralFill) }
            AsyncImage(
                model = File(photoPath),
                contentDescription = "Photo",
                modifier = Modifier.fillMaxSize(),
                placeholder = standIn,
                error = standIn,
                contentScale = ContentScale.Crop,
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(R.drawable.ic_skip),
                    contentDescription = null,
                    tint = SecondaryText,
                    modifier = Modifier.size(RowIconSize),
                )
                Text("No photo", style = MaterialTheme.typography.summaryChip, color = SecondaryText)
            }
        }
    }
}

/**
 * The status chip: [word] (see [badgeText]) with its icon on the tint of [outcome]: a tick on
 * light green for Accepted, a pause on light amber for Not yet, the skip icon on grey for
 * Skipped, and grey without an icon for Not checked.
 */
@Composable
private fun StatusChip(outcome: Outcome, word: String) {
    val (fill, content) = when (outcome) {
        Outcome.ACCEPTED -> AcceptedTint to Accepted
        Outcome.NOT_YET -> NotYetTint to NotYet
        Outcome.SKIPPED, Outcome.PENDING -> NeutralFill to SecondaryText
    }
    val icon = when {
        outcome == Outcome.ACCEPTED -> R.drawable.ic_tick
        outcome == Outcome.NOT_YET -> R.drawable.ic_pause
        word == NOT_CHECKED_TEXT -> null
        else -> R.drawable.ic_skip
    }
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(fill)
            .padding(start = if (icon != null) 8.dp else 10.dp, end = 10.dp, top = 2.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(ChipIconSize),
            )
        }
        Text(word, style = MaterialTheme.typography.summaryChip, color = content)
    }
}

/** Takes a photo for the current shot, checks it, applies [flips] and moves on. */
private fun Walk.checkedPreview(
    path: String,
    answers: List<Boolean>,
    checkMillis: Long,
    flips: List<Int> = emptyList(),
): Walk {
    var walk = photoRequested(path).photoTaken().recordCheck(answers, checkMillis)
    flips.forEach { walk = walk.flip(it) }
    return walk.nextPicture()
}

/**
 * The deck's sample walk, "Autumn in the city", Overcast, started on 10 October 2026: 3 of 5
 * accepted, agreed on 15 of 16 checks. With [count] 6 a sixth, accepted picture follows.
 */
private fun previewWalk(count: Int): Walk {
    val walk = Walk.start("Autumn in the city", "Overcast", builtInPictures.take(count), 1_791_633_600_000L)
        .checkedPreview("/preview/1.jpg", listOf(true, true, true, false), 6_200L)
        .checkedPreview("/preview/2.jpg", listOf(true, false, false, false), 6_800L)
        .checkedPreview("/preview/3.jpg", listOf(true, true, true, true), 6_100L)
        // 25% from Gemma, then one NO corrected to YES: 50%, accepted.
        .checkedPreview("/preview/4.jpg", listOf(true, false, false, false), 7_900L, flips = listOf(1))
        .skip()
    return if (count > 5) {
        walk.checkedPreview("/preview/6.jpg", listOf(true, true, false, true), 8_300L)
    } else {
        walk
    }
}

/** Room for a 24 dp status bar and a 24 dp gesture bar, as on the deck's 360 x 800 dp phone. */
private val PreviewBars = Modifier.padding(top = 24.dp, bottom = 24.dp)

@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 360, heightDp = 800)
@Composable
private fun SummaryPreview() {
    LookaboutTheme {
        SummaryScreen(walk = previewWalk(5), onNewWalk = {}, modifier = PreviewBars)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 412, heightDp = 915)
@Composable
private fun SummaryWidePreview() {
    LookaboutTheme {
        SummaryScreen(walk = previewWalk(5), onNewWalk = {}, modifier = PreviewBars)
    }
}

/** Six pictures: the rows and the agreement line scroll under New walk. */
@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 360, heightDp = 800)
@Composable
private fun SummarySixPreview() {
    LookaboutTheme {
        SummaryScreen(walk = previewWalk(6), onNewWalk = {}, modifier = PreviewBars)
    }
}

/** Every picture skipped: no photo was checked, so the agreement line is hidden. */
@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 360, heightDp = 800)
@Composable
private fun SummaryNothingCheckedPreview() {
    val walk = Walk.start("Autumn colour", "Some cloud", builtInPictures.take(3), 1_791_633_600_000L)
        .skip().skip().skip()
    LookaboutTheme {
        SummaryScreen(walk = walk, onNewWalk = {}, modifier = PreviewBars)
    }
}

/** Check at the end, where the check of the second photo failed: it reads Not checked. */
@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 360, heightDp = 800)
@Composable
private fun SummaryNotCheckedPreview() {
    val walk = Walk.start("Autumn colour", "Some cloud", builtInPictures.take(3), 1_791_633_600_000L, checkAtEnd = true)
        .checkedPreview("/preview/1.jpg", listOf(true, true, true, false), 6_200L)
        .photoRequested("/preview/2.jpg").photoTaken().nextPicture()
        .checkedPreview("/preview/3.jpg", listOf(true, true, false, false), 5_900L)
    LookaboutTheme {
        SummaryScreen(walk = walk, onNewWalk = {}, modifier = PreviewBars, canCheckAgain = true)
    }
}
