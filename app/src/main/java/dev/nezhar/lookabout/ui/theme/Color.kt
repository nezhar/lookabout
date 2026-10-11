package dev.nezhar.lookabout.ui.theme

import androidx.compose.ui.graphics.Color

// The colours of design/values.md. The first six are the deck's (page 7); the rest are sampled
// from its screens. Theme.kt maps them onto the Material 3 colour scheme where a role fits; the
// ones without a Material role are used directly.

/** Main button fill, text actions (Skip, New list), active progress dots, progress indicator, "Changed by you". */
val Primary = Color(0xFF205D75)

/** An accepted picture: the "Accepted" word, its coverage line, ticks in the criteria, Summary headline. */
val Accepted = Color(0xFF24623E)

/** A picture that is not accepted yet: the "Not yet" word, its coverage line, the pause icon. */
val NotYet = Color(0xFF8A4B00)

/** Prompts, titles, criteria, the counter. */
val MainText = Color(0xFF19231F)

/** The screen background. */
val Background = Color(0xFFFAFAF7)

/** A criterion row the walker corrected (with "Changed by you"). */
val CorrectionFill = Color(0xFFDCECF2)

/** Light tint of [Primary]: selected segments and chips, list number circles, progress indicator track. */
val PrimaryTint = Color(0xFFDCECF2)

/** The 48 dp circle behind the Accepted tick; Summary "Accepted" chip fill. */
val AcceptedTint = Color(0xFFE5F2E8)

/** The 48 dp circle behind the Not yet pause; Summary "Not yet" chip fill. */
val NotYetTint = Color(0xFFFFF0DB)

/** Topic line, "You can keep looking around.", "Tap a check to change it.", meta lines, the cross of a No. */
val SecondaryText = Color(0xFF414E47)

/** Summary per-photo line ("75% · 6 s", "Not checked"). */
val FaintText = Color(0xFF78817B)

/** Text and icons on primary buttons; fill of unselected segments, chips and the text field. */
val White = Color(0xFFFFFFFF)

/** The thin line between criteria rows and between list rows. */
val RowDivider = Color(0xFFE2E8E2)

/** The slightly stronger line under the last criterion and under the last list row. */
val LastRowDivider = Color(0xFFCFD9D1)

/** 1 dp outline of unselected segments, chips and the outlined text field. */
val Outline = Color(0xFF67766C)

/** 2 dp border of a selected chip (close to [Primary]). */
val SelectedBorder = Color(0xFF1E5C74)

/** Progress dots of pictures not reached yet. */
val InactiveDot = Color(0xFFD5DDD6)

/** Fill of a disabled main button ("Getting the model ready"). */
val DisabledFill = Color(0xFFDFE4DF)

/** Text of a disabled main button. */
val DisabledText = Color(0xFF556059)

/** Error lines and the error icon ("Couldn't check this photo."). */
val ErrorText = Color(0xFF9E231C)

/** The card around an error line. */
val ErrorFill = Color(0xFFFCEAE7)

/** Summary "No photo" tile and "Skipped" chip. */
val NeutralFill = Color(0xFFEFF2EF)
