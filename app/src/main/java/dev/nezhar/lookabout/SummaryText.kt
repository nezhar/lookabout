package dev.nezhar.lookabout

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** The date a walk started, such as "October 11, 2026", in the given zone and locale. */
fun walkDateText(
    startedAtMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
    .withLocale(locale)
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
