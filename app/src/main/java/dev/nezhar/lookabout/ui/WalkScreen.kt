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
 * Walk: one picture at a time. A placeholder until PW-13: it shows the current picture and a
 * Finish walk button.
 *
 * @param onWalkChange replaces the walk with an updated one (a photo, a check, a flip, Skip, Next).
 * @param onFinish opens Summary: Finish walk, or Skip on the last picture.
 */
@Composable
fun WalkScreen(
    walk: Walk,
    onWalkChange: (Walk) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(walk.topic, style = MaterialTheme.typography.headlineMedium)
        val current = walk.current
        if (current != null) {
            Text(
                "Picture ${walk.currentIndex + 1} of ${walk.shots.size}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(current.text, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
            Text("Finish walk")
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun WalkPreview() {
    LookaboutTheme {
        WalkScreen(
            walk = Walk.start("Autumn colour", "Some cloud", builtInPictures.take(5), 0L),
            onWalkChange = {},
            onFinish = {},
        )
    }
}
