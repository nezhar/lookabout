package dev.nezhar.lookabout

import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class SummaryTextTest {
    @Test
    fun dateUsesZoneAndLocale() {
        // 2023-11-14T22:13:20Z
        val millis = 1_700_000_000_000L
        assertEquals("November 14, 2023", walkDateText(millis, ZoneOffset.UTC, Locale.US))
        assertEquals("November 15, 2023", walkDateText(millis, ZoneId.of("Asia/Tokyo"), Locale.US))
    }

    @Test
    fun checkTimeRoundsToNearestWholeSecond() {
        assertEquals("6 s", checkTimeText(6_000))
        assertEquals("6 s", checkTimeText(6_499))
        assertEquals("7 s", checkTimeText(6_500))
        assertEquals("12 s", checkTimeText(11_800))
    }

    @Test
    fun shortCheckReadsOneSecondAndNoTimeReadsZero() {
        assertEquals("1 s", checkTimeText(1))
        assertEquals("1 s", checkTimeText(400))
        assertEquals("0 s", checkTimeText(0))
        assertEquals("0 s", checkTimeText(-5))
    }

    @Test
    fun coverageIsAPercentage() {
        assertEquals("75%", coverageText(75))
        assertEquals("0%", coverageText(0))
    }

    @Test
    fun totalReadsAcceptedOfAll() {
        assertEquals("3 of 5 accepted", acceptedTotalText(3, 5))
        assertEquals("0 of 6 accepted", acceptedTotalText(0, 6))
    }
}
