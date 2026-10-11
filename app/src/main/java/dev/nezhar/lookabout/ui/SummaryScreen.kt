package dev.nezhar.lookabout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.nezhar.lookabout.ui.theme.LookaboutTheme
import dev.nezhar.lookabout.walk.Walk
import dev.nezhar.lookabout.walk.builtInPictures

/**
 * Summary: how the walk went. A placeholder until PW-15: it shows the topic, the accepted count
 * and a New walk button.
 *
 * @param onNewWalk returns to Setup.
 */
@Composable
fun SummaryScreen(
    walk: Walk,
    onNewWalk: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(walk.topic, style = MaterialTheme.typography.headlineMedium)
        Text(
            "${walk.acceptedCount} of ${walk.shots.size} accepted",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onNewWalk, modifier = Modifier.fillMaxWidth()) {
            Text("New walk")
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun SummaryPreview() {
    LookaboutTheme {
        SummaryScreen(
            walk = Walk.start("Autumn colour", "Some cloud", builtInPictures.take(5), 0L),
            onNewWalk = {},
        )
    }
}
