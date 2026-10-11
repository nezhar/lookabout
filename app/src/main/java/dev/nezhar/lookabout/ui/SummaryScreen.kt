package dev.nezhar.lookabout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.nezhar.lookabout.acceptedTotalText
import dev.nezhar.lookabout.NOT_CHECKED_TEXT
import dev.nezhar.lookabout.agreementText
import dev.nezhar.lookabout.badgeText
import dev.nezhar.lookabout.checkTimeText
import dev.nezhar.lookabout.coverageText
import dev.nezhar.lookabout.ui.theme.Accepted
import dev.nezhar.lookabout.ui.theme.LookaboutTheme
import dev.nezhar.lookabout.ui.theme.NotYet
import dev.nezhar.lookabout.walk.Outcome
import dev.nezhar.lookabout.walk.Shot
import dev.nezhar.lookabout.walk.Walk
import dev.nezhar.lookabout.walk.builtInPictures
import dev.nezhar.lookabout.walkDateText
import java.io.File

/** Width and height of a photo tile; Coil decodes the photo at this size. */
private val PhotoSize = 72.dp

/**
 * Summary: the topic, the date the walk started, the total, how often the walker and Gemma agreed
 * (only when a photo was checked) and one row per picture with its photo, text, coverage, check
 * time and badge. The rows scroll under the pinned New walk button.
 *
 * @param onNewWalk returns to Setup.
 */
@Composable
fun SummaryScreen(
    walk: Walk,
    onNewWalk: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val date = remember(walk.startedAtMillis) { walkDateText(walk.startedAtMillis) }
    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(walk.topic, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        date,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        acceptedTotalText(walk.acceptedCount, walk.shots.size),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    agreementText(walk.agreedCount, walk.checkCount)?.let { agreement ->
                        Text(
                            agreement,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(walk.shots) { shot ->
                ShotRow(shot)
            }
        }

        Button(
            onClick = onNewWalk,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text("New walk")
        }
    }
}

/** One picture: photo tile, text, and a line with the badge, coverage and check time. */
@Composable
private fun ShotRow(shot: Shot) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PhotoTile(shot.photoPath)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(shot.text, style = MaterialTheme.typography.bodyLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val badge = badgeText(shot.outcome, shot.hasPhoto)
                    OutcomeBadge(shot.outcome, badge)
                    val checkMillis = shot.checkMillis
                    val detail = when {
                        shot.isChecked && checkMillis != null ->
                            "${coverageText(shot.coveragePercent)} · ${checkTimeText(checkMillis)}"
                        // The badge already says it.
                        badge == NOT_CHECKED_TEXT -> null
                        else -> NOT_CHECKED_TEXT
                    }
                    if (detail != null) {
                        Text(
                            detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** The photo, decoded off the main thread at the tile's size, or a "No photo" tile. */
@Composable
private fun PhotoTile(photoPath: String?) {
    val tileColor = MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = Modifier
            .size(PhotoSize)
            .clip(RoundedCornerShape(8.dp))
            .background(tileColor),
        contentAlignment = Alignment.Center,
    ) {
        if (photoPath != null) {
            AsyncImage(
                model = File(photoPath),
                contentDescription = "Photo",
                modifier = Modifier.fillMaxSize(),
                placeholder = ColorPainter(tileColor),
                error = ColorPainter(tileColor),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                "No photo",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The badge: [word] (see [badgeText]: Accepted, Not yet, Skipped or Not checked) on the colour of
 * [outcome]. Skipped and Not checked share the neutral colour.
 */
@Composable
private fun OutcomeBadge(outcome: Outcome, word: String) {
    val (background, content) = when (outcome) {
        Outcome.ACCEPTED -> Accepted to Color.White
        Outcome.NOT_YET -> NotYet to Color.White
        Outcome.SKIPPED, Outcome.PENDING ->
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        word,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
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

/** The sample walk from the design brief, plus one more accepted picture when [count] is 6. */
private fun previewWalk(count: Int): Walk {
    val walk = Walk.start("Autumn colour", "Some cloud", builtInPictures.take(count), 1_791_700_000_000L)
        .checkedPreview("/preview/1.jpg", listOf(true, true, true, false), 6_200L)
        .checkedPreview("/preview/2.jpg", listOf(true, false, false, false), 5_800L)
        .checkedPreview("/preview/3.jpg", listOf(true, true, true, true), 7_100L)
        // 25% from Gemma, then one NO corrected to YES: 50%, accepted.
        .checkedPreview("/preview/4.jpg", listOf(true, false, false, false), 6_400L, flips = listOf(1))
        .skip()
    return if (count > 5) {
        walk.checkedPreview("/preview/6.jpg", listOf(true, true, false, true), 8_300L)
    } else {
        walk
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SummaryPreview() {
    LookaboutTheme {
        SummaryScreen(walk = previewWalk(5), onNewWalk = {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SummarySixPreview() {
    LookaboutTheme {
        SummaryScreen(walk = previewWalk(6), onNewWalk = {})
    }
}

/** Every picture skipped: no photo was checked, so the agreement line is hidden. */
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SummaryNothingCheckedPreview() {
    val walk = Walk.start("Autumn colour", "Some cloud", builtInPictures.take(3), 1_791_700_000_000L)
        .skip().skip().skip()
    LookaboutTheme {
        SummaryScreen(walk = walk, onNewWalk = {})
    }
}

/** Check at the end, where the check of the second photo failed: it reads Not checked. */
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SummaryNotCheckedPreview() {
    val walk = Walk.start("Autumn colour", "Some cloud", builtInPictures.take(3), 1_791_700_000_000L, checkAtEnd = true)
        .checkedPreview("/preview/1.jpg", listOf(true, true, true, false), 6_200L)
        .photoRequested("/preview/2.jpg").photoTaken().nextPicture()
        .checkedPreview("/preview/3.jpg", listOf(true, true, false, false), 5_900L)
    LookaboutTheme {
        SummaryScreen(walk = walk, onNewWalk = {})
    }
}
