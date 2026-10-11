package dev.nezhar.lookabout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.nezhar.lookabout.R
import dev.nezhar.lookabout.largestThatFits
import dev.nezhar.lookabout.ui.theme.Accepted
import dev.nezhar.lookabout.ui.theme.NotYet
import java.io.File

/** The three sizes of the picture text on Next picture, largest first. */
private val PictureTextSizes = listOf(48.sp, 40.sp, 32.sp)

/**
 * The picture text in the largest of [PictureTextSizes] that fits the space it is given: every
 * line within the height and no word wider than a line. Measured before drawing, so the text is
 * drawn once at its final size. When none fits, the smallest is used.
 */
@Composable
fun PictureText(text: String, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val base = MaterialTheme.typography.headlineLarge
    BoxWithConstraints(modifier) {
        val space = constraints
        val style = remember(text, space, base, measurer) {
            val styles = PictureTextSizes.map { size ->
                base.copy(fontSize = size, lineHeight = size * 1.2f, fontWeight = FontWeight.Medium)
            }
            val words = text.split(' ').filter { it.isNotEmpty() }
            largestThatFits(styles) { style ->
                val wordsFit = words.all { word ->
                    measurer.measure(word, style, softWrap = false, maxLines = 1).size.width <= space.maxWidth
                }
                wordsFit && measurer.measure(
                    text,
                    style,
                    constraints = Constraints(maxWidth = space.maxWidth),
                ).size.height <= space.maxHeight
            }
        }
        Text(text, style = style)
    }
}

/**
 * One dot per picture: pictures behind the walker filled, the current one filled and larger,
 * the ones ahead as rings. Decoration only; the position text says the same.
 */
@Composable
fun ProgressDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    val filled = MaterialTheme.colorScheme.primary
    val ring = MaterialTheme.colorScheme.outline
    Row(
        modifier = modifier.clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val dot = Modifier.size(if (index == current) 14.dp else 10.dp).clip(CircleShape)
            Box(
                modifier = if (index <= current) {
                    dot.background(filled)
                } else {
                    dot.border(1.5.dp, ring, CircleShape)
                },
            )
        }
    }
}

/**
 * A walk photo, decoded off the main thread at the size shown, cropped to fill [modifier]'s size.
 * Without a path, or while loading, or when the file cannot be read, a plain box stands in for it.
 */
@Composable
fun WalkPhoto(path: String?, modifier: Modifier = Modifier) {
    val standIn = MaterialTheme.colorScheme.surfaceVariant
    val painter = remember(standIn) { ColorPainter(standIn) }
    AsyncImage(
        model = path?.let { File(it) },
        contentDescription = "Your photo",
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        placeholder = painter,
        error = painter,
        fallback = painter,
        contentScale = ContentScale.Crop,
    )
}

/** The verdict: "Accepted" or "Not yet", each with its own icon and colour. */
@Composable
fun VerdictHeader(accepted: Boolean, modifier: Modifier = Modifier) {
    val color = if (accepted) Accepted else NotYet
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(if (accepted) R.drawable.ic_accepted else R.drawable.ic_not_yet),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(36.dp),
        )
        Text(
            if (accepted) "Accepted" else "Not yet",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

/** One criterion with a tick (YES) or a cross (NO). At least 48 dp high. */
@Composable
fun CriterionRow(question: String, yes: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(if (yes) R.drawable.ic_tick else R.drawable.ic_cross),
            contentDescription = if (yes) "Yes" else "No",
            tint = if (yes) Accepted else NotYet,
            modifier = Modifier.size(24.dp),
        )
        Text(question, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Keeps the screen on while this is in the composition, such as while a check runs. */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}
