package dev.nezhar.lookabout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.nezhar.lookabout.R
import dev.nezhar.lookabout.largestThatFits
import dev.nezhar.lookabout.ui.theme.Accepted
import dev.nezhar.lookabout.ui.theme.AcceptedTint
import dev.nezhar.lookabout.ui.theme.ButtonIconSize
import dev.nezhar.lookabout.ui.theme.ChipRadius
import dev.nezhar.lookabout.ui.theme.CorrectionExtra
import dev.nezhar.lookabout.ui.theme.CorrectionFill
import dev.nezhar.lookabout.ui.theme.CriterionRowHeight
import dev.nezhar.lookabout.ui.theme.DisabledFill
import dev.nezhar.lookabout.ui.theme.DisabledText
import dev.nezhar.lookabout.ui.theme.ErrorCardHeight
import dev.nezhar.lookabout.ui.theme.InactiveDot
import dev.nezhar.lookabout.ui.theme.LastRowDivider
import dev.nezhar.lookabout.ui.theme.MainButtonHeight
import dev.nezhar.lookabout.ui.theme.NotYet
import dev.nezhar.lookabout.ui.theme.NotYetTint
import dev.nezhar.lookabout.ui.theme.PhotoRadius
import dev.nezhar.lookabout.ui.theme.ProgressDotGap
import dev.nezhar.lookabout.ui.theme.ProgressDotSize
import dev.nezhar.lookabout.ui.theme.RowDivider
import dev.nezhar.lookabout.ui.theme.RowIconSize
import dev.nezhar.lookabout.ui.theme.SecondaryActionHeight
import dev.nezhar.lookabout.ui.theme.SecondaryText
import dev.nezhar.lookabout.ui.theme.VerdictCircleSize
import dev.nezhar.lookabout.ui.theme.VerdictIconSize
import dev.nezhar.lookabout.ui.theme.button
import dev.nezhar.lookabout.ui.theme.correction
import dev.nezhar.lookabout.ui.theme.coverage
import dev.nezhar.lookabout.ui.theme.criteria
import dev.nezhar.lookabout.ui.theme.prompts
import dev.nezhar.lookabout.ui.theme.verdict
import java.io.File

/**
 * The picture text in the largest of the three prompt sizes ([prompts]: 57, 45 and 36 sp) that
 * fits the space it is given: every line within the height and no word wider than a line, so no
 * word is split across lines. Measured before drawing, so the text is drawn once at its final
 * size. When none fits, the smallest is used.
 */
@Composable
fun PictureText(text: String, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val styles = MaterialTheme.typography.prompts
    BoxWithConstraints(modifier) {
        val space = constraints
        val style = remember(text, space, styles, measurer) {
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
 * One 8 dp dot per picture, 14 dp centre to centre: the pictures done and the current one in the
 * primary colour, the ones ahead grey. Decoration only; the counter says the same.
 */
@Composable
fun ProgressDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    val reached = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier.clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(ProgressDotGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(ProgressDotSize)
                    .clip(CircleShape)
                    .background(if (index <= current) reached else InactiveDot),
            )
        }
    }
}

/**
 * A walk photo, decoded off the main thread at the size shown, cropped to fill [modifier]'s size,
 * with 16 dp corners. Without a path, or while loading, or when the file cannot be read, a plain
 * box stands in for it.
 */
@Composable
fun WalkPhoto(path: String?, modifier: Modifier = Modifier) {
    val standIn = MaterialTheme.colorScheme.surfaceVariant
    val painter = remember(standIn) { ColorPainter(standIn) }
    AsyncImage(
        model = path?.let { File(it) },
        contentDescription = "Your photo",
        modifier = modifier.clip(RoundedCornerShape(PhotoRadius)),
        placeholder = painter,
        error = painter,
        fallback = painter,
        contentScale = ContentScale.Crop,
    )
}

/**
 * The verdict: a tick in a green circle and "Accepted", or a pause in an amber circle and
 * "Not yet", in the Verdict style, with the [coverage] line ("75%, 3 of 4") under them in the
 * same colour. 48 dp for the circle row plus 24 dp for the coverage line.
 */
@Composable
fun VerdictHeader(accepted: Boolean, coverage: String, modifier: Modifier = Modifier) {
    val color = if (accepted) Accepted else NotYet
    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(VerdictCircleSize)
                    .clip(CircleShape)
                    .background(if (accepted) AcceptedTint else NotYetTint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(if (accepted) R.drawable.ic_tick else R.drawable.ic_pause),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(VerdictIconSize),
                )
            }
            Text(
                if (accepted) "Accepted" else "Not yet",
                style = MaterialTheme.typography.verdict,
                color = color,
            )
        }
        Text(coverage, style = MaterialTheme.typography.coverage, color = color)
    }
}

/**
 * One criterion with a green tick (YES) or a grey cross (NO), showing the answer after the
 * walker's correction, and a line under it: a thin one, or a stronger one under the [last] row.
 * The whole row is the tap target: tapping it calls [onTap] to change the answer. A [corrected]
 * row is filled, has rounded corners and reads "Changed by you" under the question. At least
 * 64 dp high, 80 dp when corrected; the icon is centred 20 dp from the row's start and the
 * question starts 44 dp in.
 */
@Composable
fun CriterionRow(
    question: String,
    yes: Boolean,
    corrected: Boolean,
    last: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val line = if (last) LastRowDivider else RowDivider
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (corrected) CriterionRowHeight + CorrectionExtra else CriterionRowHeight)
            .drawWithContent {
                drawContent()
                val stroke = 1.dp.toPx()
                val y = size.height - stroke / 2
                drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
            }
            .clip(RoundedCornerShape(ChipRadius))
            .background(if (corrected) CorrectionFill else Color.Transparent)
            .clickable(onClickLabel = "Change the answer", role = Role.Button, onClick = onTap)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(if (yes) R.drawable.ic_tick else R.drawable.ic_cross),
            contentDescription = if (yes) "Yes" else "No",
            tint = if (yes) Accepted else SecondaryText,
            modifier = Modifier.size(RowIconSize),
        )
        Column {
            Text(question, style = MaterialTheme.typography.criteria)
            if (corrected) {
                Text(
                    "Changed by you",
                    style = MaterialTheme.typography.correction,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * An error line in its card: an error icon and [text] in the error colour on a light red fill
 * with 12 dp corners, at least 56 dp high. The icon is centred 24 dp from the card's start and
 * the text starts 44 dp in, as in a criterion row.
 */
@Composable
fun ErrorCard(text: String, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.error
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ErrorCardHeight)
            .clip(RoundedCornerShape(ChipRadius))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_error),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(RowIconSize),
        )
        Text(text, style = MaterialTheme.typography.bodyLarge, color = color)
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

/**
 * The main button at the bottom of a screen: 56 dp high, full width, fully rounded, with [icon]
 * before [label] when given. Disabled ("Getting the model ready", Make my list before the choices
 * are complete) it turns grey.
 */
@Composable
fun MainButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Int? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = DisabledFill,
            disabledContentColor = DisabledText,
        ),
        modifier = modifier
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
}

/** The text-only secondary action under the main button ("New list", "Cancel"): primary, 48 dp high. */
@Composable
fun TextAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = SecondaryActionHeight),
    ) {
        Text(label, style = MaterialTheme.typography.button)
    }
}
