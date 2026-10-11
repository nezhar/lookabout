package dev.nezhar.lookabout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.nezhar.lookabout.ChallengeList
import dev.nezhar.lookabout.ui.theme.LookaboutTheme
import dev.nezhar.lookabout.walk.builtInPictures

/**
 * List: the topic and the numbered pictures, all shown up front. The rows scroll under the
 * pinned New list and Start walk buttons.
 */
@Composable
fun ListScreen(
    list: ChallengeList,
    onStartWalk: () -> Unit,
    onNewList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    list.topic,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            itemsIndexed(list.pictures) { index, picture ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(picture.text, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onNewList, modifier = Modifier.weight(1f)) {
                Text("New list")
            }
            Button(onClick = onStartWalk, modifier = Modifier.weight(1f)) {
                Text("Start walk")
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun ListPreview() {
    LookaboutTheme {
        ListScreen(
            list = ChallengeList("Autumn colour", "Some cloud", builtInPictures.take(6)),
            onStartWalk = {},
            onNewList = {},
        )
    }
}
