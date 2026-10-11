package dev.nezhar.lookabout

import dev.nezhar.lookabout.walk.Outcome
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SummaryTextTest {
    @Test
    fun dateUsesZoneAndLocale() {
        // 2023-11-14T22:13:20Z
        val millis = 1_700_000_000_000L
        assertEquals("14 November 2023", walkDateText(millis, ZoneOffset.UTC, Locale.ENGLISH))
        assertEquals("15 November 2023", walkDateText(millis, ZoneId.of("Asia/Tokyo"), Locale.ENGLISH))
    }

    @Test
    fun dateReadsDayMonthYearInEveryLocaleOrder() {
        // 2026-10-10T12:00:00Z; the US order would be "October 10, 2026".
        val millis = 1_791_633_600_000L
        assertEquals("10 October 2026", walkDateText(millis, ZoneOffset.UTC, Locale.US))
        assertEquals("10 October 2026", walkDateText(millis, ZoneOffset.UTC, Locale.ENGLISH))
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

    @Test
    fun agreementReadsAgreedOfChecks() {
        assertEquals("Gemma and you agreed on 15 of 16 checks.", agreementText(15, 16))
        assertEquals("Gemma and you agreed on 0 of 4 checks.", agreementText(0, 4))
        assertEquals("Gemma and you agreed on 4 of 4 checks.", agreementText(4, 4))
    }

    @Test
    fun agreementIsHiddenWithoutCheckedPhotos() {
        assertNull(agreementText(0, 0))
    }

    @Test
    fun badgeNamesTheOutcome() {
        assertEquals("Accepted", badgeText(Outcome.ACCEPTED, hasPhoto = true))
        assertEquals("Not yet", badgeText(Outcome.NOT_YET, hasPhoto = true))
        assertEquals("Skipped", badgeText(Outcome.SKIPPED, hasPhoto = false))
    }

    @Test
    fun photoWhoseCheckNeverCameInIsNotChecked() {
        assertEquals("Not checked", badgeText(Outcome.PENDING, hasPhoto = true))
    }

    @Test
    fun pendingShotWithoutAPhotoReadsSkipped() {
        assertEquals("Skipped", badgeText(Outcome.PENDING, hasPhoto = false))
    }
}
