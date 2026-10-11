package dev.nezhar.lookabout

import dev.nezhar.lookabout.walk.Outcome
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The date a walk started as on the deck, such as "10 October 2026": day, month name, year in
 * this order whatever the locale, with the month named in [locale], in the given zone.
 */
fun walkDateText(
    startedAtMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String = DateTimeFormatter.ofPattern("d MMMM yyyy", locale)
    .format(Instant.ofEpochMilli(startedAtMillis).atZone(zone))

/**
 * A check time in whole seconds, such as "6 s". Rounds to the nearest second (half up), and any
 * check under half a second still reads "1 s", so a check that ran never shows "0 s".
 */
fun checkTimeText(checkMillis: Long): String {
    val seconds = if (checkMillis <= 0L) 0L else maxOf(1L, (checkMillis + 500L) / 1000L)
    return "$seconds s"
}

/** Coverage such as "75%". */
fun coverageText(percent: Int): String = "$percent%"

/** The total such as "3 of 5 accepted". */
fun acceptedTotalText(accepted: Int, total: Int): String = "$accepted of $total accepted"

/**
 * How often the walker kept Gemma's answer, such as "Gemma and you agreed on 15 of 16 checks.".
 * [checks] counts only checked photos (four per photo); null when no photo was checked, so the
 * line is hidden.
 */
fun agreementText(agreed: Int, checks: Int): String? =
    if (checks <= 0) null else "Gemma and you agreed on $agreed of $checks checks."

/**
 * The word on a picture's badge on Summary. A shot that is still pending but has a photo had its
 * check fail (for example at the end of the walk) and reads "Not checked"; a pending shot without
 * a photo was never shot and reads "Skipped", like a skipped one.
 */
fun badgeText(outcome: Outcome, hasPhoto: Boolean): String = when (outcome) {
    Outcome.ACCEPTED -> "Accepted"
    Outcome.NOT_YET -> "Not yet"
    Outcome.SKIPPED -> "Skipped"
    Outcome.PENDING -> if (hasPhoto) NOT_CHECKED_TEXT else "Skipped"
}

/** What Summary says about a photo without a check. */
const val NOT_CHECKED_TEXT = "Not checked"
