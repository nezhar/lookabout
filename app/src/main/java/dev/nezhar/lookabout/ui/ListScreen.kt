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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.nezhar.lookabout.ChallengeList
import dev.nezhar.lookabout.R
import dev.nezhar.lookabout.countWord
import dev.nezhar.lookabout.listMetaText
import dev.nezhar.lookabout.ui.theme.ActionGap
import dev.nezhar.lookabout.ui.theme.LastRowDivider
import dev.nezhar.lookabout.ui.theme.ListNumberCircleSize
import dev.nezhar.lookabout.ui.theme.ListRowHeight
import dev.nezhar.lookabout.ui.theme.LookaboutTheme
import dev.nezhar.lookabout.ui.theme.PageInset
import dev.nezhar.lookabout.ui.theme.PrimaryTint
import dev.nezhar.lookabout.ui.theme.ProgressBarHeight
import dev.nezhar.lookabout.ui.theme.RowDivider
import dev.nezhar.lookabout.ui.theme.caption
import dev.nezhar.lookabout.ui.theme.listNumber
import dev.nezhar.lookabout.ui.theme.listRow
import dev.nezhar.lookabout.ui.theme.listTopic
import dev.nezhar.lookabout.ui.theme.screenTitle
import dev.nezhar.lookabout.ui.theme.waitingTitle
import dev.nezhar.lookabout.walk.builtInPictures

/** Above the title, under the status bar (read off the deck). */
private val TopPadding = 24.dp

/** Between the header and the first row. */
private val HeaderGap = 28.dp

/** Under the text action, above the navigation bar. */
private val BottomPadding = 16.dp

/** The 412 dp screens set the list rows larger than the 360 dp ones. */
private val WideFrom = 400.dp

/**
 * List: "Your list", the topic and "Overcast · 5 pictures", then every picture numbered in a
 * tinted circle with a line under it, all shown before the walk starts (design/values.md: "Reveal
 * every prompt before leaving."). Start walk and New list stay pinned at the bottom; when the rows
 * do not fit, such as six long ones at 360 dp, the header and rows scroll under them.
 */
@Composable
fun ListScreen(
    list: ChallengeList,
    onStartWalk: () -> Unit,
    onNewList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val rowStyle = MaterialTheme.typography.listRow(wide = maxWidth >= WideFrom)
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = PageInset, end = PageInset, top = TopPadding, bottom = 8.dp),
            ) {
                item {
                    ListHeader(
                        list.topic,
                        list.sky,
                        list.pictures.size,
                        modifier = Modifier.padding(bottom = HeaderGap),
                    )
                }
                itemsIndexed(list.pictures) { index, picture ->
                    ListRow(
                        number = index + 1,
                        text = picture.text,
                        style = rowStyle,
                        last = index == list.pictures.lastIndex,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = PageInset, end = PageInset, top = 8.dp, bottom = BottomPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ActionGap),
            ) {
                MainButton("Start walk", onClick = onStartWalk, icon = R.drawable.ic_arrow_forward)
                TextAction("New list", onClick = onNewList)
            }
        }
    }
}

/** "Your list" (title), the topic and the meta line, as on List and while the list is written. */
@Composable
private fun ListHeader(topic: String, sky: String, count: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("Your list", style = MaterialTheme.typography.screenTitle)
        Spacer(Modifier.height(12.dp))
        Text(topic, style = MaterialTheme.typography.listTopic)
        Spacer(Modifier.height(8.dp))
        Text(
            listMetaText(sky, count),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * One picture: its [number] in a 36 dp tinted circle, the text beside it (up to two lines in
 * practice, never cut), at least 88 dp high with a line under it; a stronger one under the [last].
 */
@Composable
private fun ListRow(number: Int, text: String, style: TextStyle, last: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ListRowHeight - 1.dp)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(ListNumberCircleSize)
                    .clip(CircleShape)
                    .background(PrimaryTint),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$number",
                    style = MaterialTheme.typography.listNumber,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(text, style = style, modifier = Modifier.weight(1f))
        }
        HorizontalDivider(thickness = 1.dp, color = if (last) LastRowDivider else RowDivider)
    }
}

/**
 * List while Gemma writes the [count] pictures: the List header for [topic] and [sky], then
 * "Making your list", "Finding five things to notice." (the count as a word), a full-width
 * indeterminate progress bar and "This may take a little while.". No buttons.
 */
@Composable
fun ListProgress(topic: String, sky: String, count: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = PageInset, end = PageInset, top = TopPadding),
    ) {
        ListHeader(topic, sky, count)
        Spacer(Modifier.height(64.dp))
        Text("Making your list", style = MaterialTheme.typography.waitingTitle)
        Spacer(Modifier.height(12.dp))
        Text(
            "Finding ${countWord(count)} things to notice.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(36.dp))
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(ProgressBarHeight),
            trackColor = PrimaryTint,
        )
        Spacer(Modifier.height(32.dp))
        Text(
            "This may take a little while.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Room for a 24 dp status bar and a 24 dp gesture bar, as on the deck's 360 x 800 dp phone. */
private val PreviewBars = Modifier.padding(top = 24.dp, bottom = 24.dp)

/** The deck's sample walk: "Autumn in the city", Overcast, five pictures. */
private fun sampleList(count: Int) = ChallengeList("Autumn in the city", "Overcast", builtInPictures.take(count))

@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 360, heightDp = 800)
@Composable
private fun ListProgressPreview() {
    LookaboutTheme {
        ListProgress(topic = "Autumn in the city", sky = "Overcast", count = 5, modifier = PreviewBars)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 360, heightDp = 800)
@Composable
private fun ListPreview() {
    LookaboutTheme {
        ListScreen(list = sampleList(5), onStartWalk = {}, onNewList = {}, modifier = PreviewBars)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 412, heightDp = 915)
@Composable
private fun ListWidePreview() {
    LookaboutTheme {
        ListScreen(list = sampleList(5), onStartWalk = {}, onNewList = {}, modifier = PreviewBars)
    }
}

/** Six pictures, the sixth the longest built-in one: the rows scroll under the pinned buttons. */
@Preview(showBackground = true, backgroundColor = 0xFFFAFAF7, widthDp = 360, heightDp = 800)
@Composable
private fun ListSixPreview() {
    LookaboutTheme {
        ListScreen(list = sampleList(6), onStartWalk = {}, onNewList = {}, modifier = PreviewBars)
    }
}
