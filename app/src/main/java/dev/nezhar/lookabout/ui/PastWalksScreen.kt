package dev.nezhar.lookabout.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nezhar.lookabout.DELETE_WALK_TEXT
import dev.nezhar.lookabout.NO_WALKS_TEXT
import dev.nezhar.lookabout.PastWalkRow
import dev.nezhar.lookabout.pastWalkRows
import dev.nezhar.lookabout.ui.theme.Accepted
import dev.nezhar.lookabout.ui.theme.ActionGap
import dev.nezhar.lookabout.ui.theme.LastRowDivider
import dev.nezhar.lookabout.ui.theme.LookaboutTheme
import dev.nezhar.lookabout.ui.theme.PageInset
import dev.nezhar.lookabout.ui.theme.RowDivider
import dev.nezhar.lookabout.ui.theme.SummaryThumbCompact
import dev.nezhar.lookabout.ui.theme.SummaryThumbWide
import dev.nezhar.lookabout.ui.theme.caption
import dev.nezhar.lookabout.ui.theme.screenTitle
import dev.nezhar.lookabout.ui.theme.summaryPrompt
import dev.nezhar.lookabout.walk.ArchivedWalkSummary

/** Above the title, under the status bar, as on List. */
private val TopPadding = 24.dp

/** Between the title and the first row, as on List. */
private val HeaderGap = 28.dp

/** Under the text action, above the navigation bar. */
private val BottomPadding = 16.dp

/** The 412 dp screens set thumbnails and text larger than the 360 dp ones, as on Summary. */
private val WideFrom = 400.dp

/**
 * Past walks: every archived walk, newest first, one row each with the thumbnail of its first
 * accepted photo (or the No photo tile of Summary), the date, the topic and "3 of 5 accepted",
 * with a line under each row as on List. A tap opens the walk on Summary; a long press asks
 * "Delete this walk? Its photos stay in the gallery." under the row, with Delete and Keep. The
 * rows scroll; Back to Setup stays pinned at the bottom. An empty archive shows "No walks yet.".
 *
 * @param pastWalks the archived walks, or null while the archive is being read.
 */
@Composable
fun PastWalksScreen(
    pastWalks: List<ArchivedWalkSummary>?,
    onOpen: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = remember(pastWalks) { pastWalks?.let { pastWalkRows(it) } }
    var confirming by remember { mutableStateOf<Long?>(null) }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth >= WideFrom
        val thumbSize = if (wide) SummaryThumbWide else SummaryThumbCompact
        val topicStyle = MaterialTheme.typography.summaryPrompt(wide)
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = PageInset, end = PageInset, top = TopPadding, bottom = 8.dp),
            ) {
                item {
                    Text(
                        "Past walks",
                        style = MaterialTheme.typography.screenTitle,
                        modifier = Modifier.padding(bottom = HeaderGap),
                    )
                }
                when {
                    rows == null -> {}
                    rows.isEmpty() -> item {
                        Text(
                            NO_WALKS_TEXT,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else -> itemsIndexed(rows, key = { _, row -> row.id }) { index, row ->
                        PastWalkItem(
                            row = row,
                            thumbSize = thumbSize,
                            topicStyle = topicStyle,
                            last = index == rows.lastIndex,
                            confirming = confirming == row.id,
                            onOpen = {
                                confirming = null
                                onOpen(row.id)
                            },
                            onAskDelete = { confirming = row.id },
                            onDelete = {
                                confirming = null
                                onDelete(row.id)
                            },
                            onKeep = { confirming = null },
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = PageInset, end = PageInset, top = 8.dp, bottom = BottomPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TextAction("Back to Setup", onClick = onBack)
            }
        }
    }
}

/**
 * One past walk: the thumbnail and beside it the date, the topic and the accepted total; the
 * whole row opens the walk, a long press asks to delete it. While [confirming], the confirm line
 * with Delete and Keep shows under the row. A line under it, a stronger one under the [last].
 */
@Composable
private fun PastWalkItem(
    row: PastWalkRow,
    thumbSize: Dp,
    topicStyle: TextStyle,
    last: Boolean,
    confirming: Boolean,
    onOpen: () -> Unit,
    onAskDelete: () -> Unit,
    onDelete: () -> Unit,
    onKeep: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClickLabel = "Open",
                    onLongClickLabel = "Delete",
                    onLongClick = onAskDelete,
                    onClick = onOpen,
                )
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Thumbnail(row.thumbnailPath, thumbSize)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    row.date,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(row.topic, style = topicStyle)
                Spacer(Modifier.height(2.dp))
                Text(row.acceptedText, style = MaterialTheme.typography.caption, color = Accepted)
            }
        }
        if (confirming) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(DELETE_WALK_TEXT, style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(ActionGap)) {
                    TextAction("Delete", onClick = onDelete)
                    TextAction("Keep", onClick = onKeep)
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = if (last) LastRowDivider else RowDivider)
    }
}

/** Room for a 24 dp status bar and a 24 dp gesture bar, as on the deck's 360 x 800 dp phone. */
private val PreviewBars = Modifier.padding(top = 24.dp, bottom = 24.dp)

/** Three archived walks, the oldest without an accepted photo. */
private val sampleWalks = listOf(
    ArchivedWalkSummary(
        id = 1_791_460_800_000L,
        startedAtMillis = 1_791_460_800_000L,
        topic = "Reflections",
        sky = "Rain",
        acceptedCount = 0,
        pictureCount = 3,
        agreedCount = 10,
        checkCount = 12,
        thumbnailPath = null,
    ),
    ArchivedWalkSummary(
        id = 1_791_633_600_000L,
        startedAtMillis = 1_791_633_600_000L,
        topic = "Autumn in the city",
        sky = "Overcast",
        acceptedCount = 3,
        pictureCount = 5,
        agreedCount = 15,
        checkCount = 16,
        thumbnailPath = "/preview/1.jpg",
    ),
    ArchivedWalkSummary(
        id = 1_791_720_000_000L,
        startedAtMillis = 1_791_720_000_000L,
        topic = "Doors and windows",
        sky = "Clear",
        acceptedCount = 4,
        pictureCount = 6,
        agreedCount = 22,
        checkCount = 24,
        thumbnailPath = "/preview/2.jpg",
    ),
)

@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 360, heightDp = 800)
@Composable
private fun PastWalksPreview() {
    LookaboutTheme {
        PastWalksScreen(pastWalks = sampleWalks, onOpen = {}, onDelete = {}, onBack = {}, modifier = PreviewBars)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 360, heightDp = 800)
@Composable
private fun PastWalksEmptyPreview() {
    LookaboutTheme {
        PastWalksScreen(pastWalks = emptyList(), onOpen = {}, onDelete = {}, onBack = {}, modifier = PreviewBars)
    }
}
