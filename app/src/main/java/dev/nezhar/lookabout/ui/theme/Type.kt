package dev.nezhar.lookabout.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private fun style(size: TextUnit, lineHeight: TextUnit, weight: FontWeight) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
)

/**
 * The type of design/values.md (size / line height in sp) on the Material 3 roles. The deck's
 * styles (page 7):
 *
 * - Prompt 57 / 64, 45 / 52, 36 / 44 bold: [Typography.displayLarge], `displayMedium`,
 *   `displaySmall`; see [prompts].
 * - Verdict 32 / 40 medium: [Typography.headlineLarge], see [verdict].
 * - Coverage 20 / 24 medium: [Typography.titleLarge], see [coverage].
 * - Criteria 16 / 20 regular: [Typography.bodyLarge], see [criteria]. Also the counter, the topic
 *   line, "You can keep looking around." and error lines, read off at about 16 sp.
 * - Buttons 18 / 22 medium: [Typography.labelLarge], which Material buttons use; see [button].
 * - Caption 14 / 20 regular: [Typography.bodyMedium], see [caption].
 * - Correction 12 / 18 medium: [Typography.labelMedium], see [correction].
 *
 * Read off the screens, so approximate: screen titles about 28 bold (`headlineMedium`), Summary
 * title and headline about 24 bold (`headlineSmall`), Setup section labels about 18 medium
 * (`titleMedium`).
 */
val Typography = Typography(
    displayLarge = style(57.sp, 64.sp, FontWeight.Bold),
    displayMedium = style(45.sp, 52.sp, FontWeight.Bold),
    displaySmall = style(36.sp, 44.sp, FontWeight.Bold),
    headlineLarge = style(32.sp, 40.sp, FontWeight.Medium),
    headlineMedium = style(28.sp, 36.sp, FontWeight.Bold),
    headlineSmall = style(24.sp, 32.sp, FontWeight.Bold),
    titleLarge = style(20.sp, 24.sp, FontWeight.Medium),
    titleMedium = style(18.sp, 22.sp, FontWeight.Medium),
    titleSmall = style(14.sp, 20.sp, FontWeight.Medium),
    bodyLarge = style(16.sp, 20.sp, FontWeight.Normal),
    bodyMedium = style(14.sp, 20.sp, FontWeight.Normal),
    bodySmall = style(12.sp, 16.sp, FontWeight.Normal),
    labelLarge = style(18.sp, 22.sp, FontWeight.Medium),
    labelMedium = style(12.sp, 18.sp, FontWeight.Medium),
    labelSmall = style(11.sp, 16.sp, FontWeight.Medium),
)

/** The three sizes of the picture text on Next picture, largest first. */
val Typography.prompts: List<TextStyle> get() = listOf(displayLarge, displayMedium, displaySmall)

/** "Accepted", "Not yet", "Checking", "Check paused". */
val Typography.verdict: TextStyle get() = headlineLarge

/** "75%, 3 of 4" under the verdict. */
val Typography.coverage: TextStyle get() = titleLarge

/** A criterion question. */
val Typography.criteria: TextStyle get() = bodyLarge

/** Main buttons and text actions. */
val Typography.button: TextStyle get() = labelLarge

/** "Tap a check to change it.", meta lines. */
val Typography.caption: TextStyle get() = bodyMedium

/** "Changed by you". */
val Typography.correction: TextStyle get() = labelMedium

// Read off the Setup, List and Summary screens (design/values.md, "Text without a deck style"),
// so approximate. Where the 360 and 412 dp screens differ, [wide] picks the 412 size.

/** "Lookabout", "Your list": about 28 sp, bold. */
val Typography.screenTitle: TextStyle get() = headlineMedium

/** "Making your list": the screen title size, medium. */
val Typography.waitingTitle: TextStyle get() = headlineMedium.copy(fontWeight = FontWeight.Medium)

/** Setup section labels "Pictures", "Topic", "Look up. What is the sky doing?": about 18 sp, medium. */
val Typography.sectionLabel: TextStyle get() = titleMedium

/** The numbers in the Setup segmented control: about 18 sp. */
val Typography.segment: TextStyle get() = titleMedium

/** Setup chip labels: about 15 sp, medium. */
val Typography.chip: TextStyle get() = style(15.sp, 20.sp, FontWeight.Medium)

/** The topic on List, "Autumn in the city": about 22 sp, medium. */
val Typography.listTopic: TextStyle get() = style(22.sp, 28.sp, FontWeight.Medium)

/** A picture on List: about 18 sp at 360 dp, 20 sp at 412 dp, regular. */
fun Typography.listRow(wide: Boolean): TextStyle =
    if (wide) style(20.sp, 26.sp, FontWeight.Normal) else style(18.sp, 24.sp, FontWeight.Normal)

/** The number of a picture on List: about 16 sp, in the primary colour. */
val Typography.listNumber: TextStyle get() = bodyLarge

/** The topic on Summary: about 24 sp, bold. */
val Typography.summaryTitle: TextStyle get() = headlineSmall

/** "3 of 5 accepted": about 24 sp at 360 dp, 26 sp at 412 dp, bold. */
fun Typography.summaryHeadline(wide: Boolean): TextStyle =
    if (wide) style(26.sp, 32.sp, FontWeight.Bold) else headlineSmall

/** "1. A reflection ..." on Summary: about 15 sp at 360 dp, 16 sp at 412 dp, medium. */
fun Typography.summaryPrompt(wide: Boolean): TextStyle =
    if (wide) style(16.sp, 20.sp, FontWeight.Medium) else style(15.sp, 17.sp, FontWeight.Medium)

/** The per-photo line on Summary ("75% · 6 s", "Not checked"): about 13 sp. */
val Typography.summaryDetail: TextStyle get() = style(13.sp, 16.sp, FontWeight.Normal)

/** The words in Summary status chips ("Accepted", "Skipped"): about 13 sp, medium. */
val Typography.summaryChip: TextStyle get() = style(13.sp, 18.sp, FontWeight.Medium)
