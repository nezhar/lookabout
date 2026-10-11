package dev.nezhar.lookabout

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Reference values from the NOAA solar calculator formulas, cross-checked with the Astronomical
 * Almanac's low-precision sun formulas (both agree within 0.02°).
 */
class SunPositionTest {
    private val tolerance = 1.0

    private fun assertSun(elevation: Double, azimuth: Double, actual: SunPosition) {
        assertEquals("elevation", elevation, actual.elevation, tolerance)
        assertEquals("azimuth", azimuth, actual.azimuth, tolerance)
    }

    @Test
    fun grazNearSolarNoonAtTheMarchEquinox() {
        // 2026-03-20 11:06 UTC, Graz (47.07 N, 15.44 E): 90° - 47.07° with the sun on the equator.
        val sun = sunPosition(47.07, 15.44, 1_774_004_760_000)
        assertSun(42.87, 180.1, sun)
        assertEquals("medium in the south", sunWords(sun))
    }

    @Test
    fun grazOnAnEquinoxMorning() {
        // 2026-03-20 07:00 UTC.
        val sun = sunPosition(47.07, 15.44, 1_773_990_000_000)
        assertSun(18.91, 111.8, sun)
        assertEquals("low in the east", sunWords(sun))
    }

    @Test
    fun grazOnAWinterSolsticeAfternoon() {
        // 2026-12-21 15:00 UTC, just before sunset.
        val sun = sunPosition(47.07, 15.44, 1_797_865_200_000)
        assertSun(0.72, 233.3, sun)
        assertEquals("low in the south-west", sunWords(sun))
    }

    @Test
    fun grazAtNight() {
        // 2026-03-20 23:00 UTC.
        val sun = sunPosition(47.07, 15.44, 1_774_047_600_000)
        assertSun(-42.78, 358.1, sun)
        assertEquals("below the horizon", sunWords(sun))
    }

    @Test
    fun sydneyInSummerHasTheSunHighInTheNorth() {
        // 2026-01-15 02:00 UTC, Sydney (33.87 S, 151.21 E).
        val sun = sunPosition(-33.87, 151.21, 1_768_442_400_000)
        assertSun(77.24, 4.67, sun)
        assertEquals("high in the north", sunWords(sun))
    }

    @Test
    fun newYorkOnAJulyEvening() {
        // 2026-07-04 22:00 UTC, New York (40.71 N, 74.01 W).
        val sun = sunPosition(40.71, -74.01, 1_783_202_400_000)
        assertSun(25.81, 278.7, sun)
        assertEquals("medium in the west", sunWords(sun))
    }

    @Test
    fun elevationBandsSplitAtZeroTwentyAndFortyFive() {
        assertEquals("below the horizon", elevationBand(-0.1))
        assertEquals("low", elevationBand(0.0))
        assertEquals("low", elevationBand(19.9))
        assertEquals("medium", elevationBand(20.0))
        assertEquals("medium", elevationBand(44.9))
        assertEquals("high", elevationBand(45.0))
        assertEquals("high", elevationBand(90.0))
    }

    @Test
    fun compassPointsAreTheNearestOfEight() {
        assertEquals("north", compassPoint(0.0))
        assertEquals("north", compassPoint(22.4))
        assertEquals("north-east", compassPoint(22.5))
        assertEquals("east", compassPoint(90.0))
        assertEquals("south-east", compassPoint(135.0))
        assertEquals("south", compassPoint(180.0))
        assertEquals("south-west", compassPoint(225.0))
        assertEquals("west", compassPoint(270.0))
        assertEquals("north-west", compassPoint(315.0))
        assertEquals("north", compassPoint(337.5))
        assertEquals("north", compassPoint(359.9))
        assertEquals("north", compassPoint(360.0))
    }

    @Test
    fun belowTheHorizonHasNoDirection() {
        assertEquals("below the horizon", sunWords(SunPosition(-5.0, 225.0)))
        assertEquals("low in the south-west", sunWords(SunPosition(5.0, 225.0)))
        assertEquals("high in the south", sunWords(SunPosition(60.0, 180.0)))
    }
}
