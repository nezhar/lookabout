package dev.nezhar.lookabout.ui.theme

import androidx.compose.ui.unit.dp

// The sizes of design/values.md in dp. The first block is the deck's (page 7); the rest are read
// off its screens (good to about 1 dp).

/** Left and right inset on every screen. */
val PageInset = 20.dp

/** Height of the main button: full width inside the inset, fully rounded. */
val MainButtonHeight = 56.dp

/** Height of the text-only secondary action ("Skip", "New list") under the main button. */
val SecondaryActionHeight = 48.dp

/** The smallest touch target. */
val MinTarget = 48.dp

/** Between the main button and the secondary action. */
val ActionGap = 8.dp

/** Corner radius of photos. */
val PhotoRadius = 16.dp

/** Corner radius of chips, a corrected criterion row, the error card and Summary thumbnails. */
val ChipRadius = 12.dp

/** The photo on Accepted and Not yet gives up height for the criteria down to this (departure 2). */
val VerdictPhotoMinHeight = 80.dp

/** Height of a criterion row; a corrected row adds [CorrectionExtra]. */
val CriterionRowHeight = 64.dp

/** What "Changed by you" adds to a criterion row. */
val CorrectionExtra = 16.dp

/** Height of the top bar on the loop screens: the counter and the progress dots. */
val TopBarHeight = 48.dp

/** A progress dot. */
val ProgressDotSize = 8.dp

/** Between two progress dots (14 dp centre to centre). */
val ProgressDotGap = 6.dp

/** The tinted circle behind the verdict icon. */
val VerdictCircleSize = 48.dp

/** The verdict icon inside its circle. */
val VerdictIconSize = 28.dp

/** Icons in rows and cards. */
val RowIconSize = 24.dp

/** Icons in buttons ("Shoot", "Next picture"). */
val ButtonIconSize = 20.dp

/** Height of the error card ("Couldn't check this photo."). */
val ErrorCardHeight = 56.dp

/** Thickness of the linear progress indicators. */
val ProgressBarHeight = 4.dp

/** Width of the progress indicator on Checking, centred. */
val CheckingProgressWidth = 180.dp

/** Height of the Setup segmented control. */
val SegmentHeight = 52.dp

/** Height of a Setup chip; chips are [ChipGap] apart. */
val ChipHeight = 48.dp

/** Between chips. */
val ChipGap = 8.dp

/** Height of the outlined text field. */
val TextFieldHeight = 56.dp

/** Height of a List row. */
val ListRowHeight = 88.dp

/** The tinted circle around a List number. */
val ListNumberCircleSize = 36.dp

/** Summary thumbnails at 360 dp wide. */
val SummaryThumbCompact = 76.dp

/** Summary thumbnails at 412 dp wide. */
val SummaryThumbWide = 88.dp

/** Between Summary rows. */
val SummaryRowGap = 12.dp
