package dev.nezhar.lookabout.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * The colours of design/values.md on the Material 3 roles. Containers that standard components
 * fill when selected (segments, filter chips, the progress indicator track) take [PrimaryTint];
 * surfaces stay on [Background] so no Material default tint shows through.
 */
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = White,
    primaryContainer = PrimaryTint,
    onPrimaryContainer = Primary,
    secondary = Primary,
    onSecondary = White,
    secondaryContainer = PrimaryTint,
    onSecondaryContainer = Primary,
    tertiary = Accepted,
    onTertiary = White,
    tertiaryContainer = AcceptedTint,
    onTertiaryContainer = Accepted,
    background = Background,
    onBackground = MainText,
    surface = Background,
    onSurface = MainText,
    surfaceVariant = NeutralFill,
    onSurfaceVariant = SecondaryText,
    surfaceTint = Primary,
    surfaceBright = Background,
    surfaceDim = DisabledFill,
    surfaceContainerLowest = White,
    surfaceContainerLow = Background,
    surfaceContainer = NeutralFill,
    surfaceContainerHigh = NeutralFill,
    surfaceContainerHighest = DisabledFill,
    error = ErrorText,
    onError = White,
    errorContainer = ErrorFill,
    onErrorContainer = ErrorText,
    outline = Outline,
    outlineVariant = RowDivider,
)

/** The light colour scheme whatever the system setting, with no dynamic colour. */
@Composable
fun LookaboutTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
