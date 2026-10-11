package dev.nezhar.lookabout

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/** Where the sun stands: [elevation] above the horizon and [azimuth] clockwise from north, in degrees. */
data class SunPosition(val elevation: Double, val azimuth: Double)

private const val MILLIS_PER_DAY = 86_400_000L

private val compassPoints = listOf(
    "north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west",
)

/**
 * The sun's position seen from [latitude] and [longitude] (degrees, north and east positive) at
 * [epochMillis] (UTC), with the NOAA solar calculator's formulas (after Meeus, *Astronomical
 * Algorithms*). Good to well under a degree for years around 2000; the elevation is geometric,
 * without atmospheric refraction (under 1° except right at the horizon).
 */
fun sunPosition(latitude: Double, longitude: Double, epochMillis: Long): SunPosition {
    val julianDay = epochMillis / MILLIS_PER_DAY.toDouble() + 2_440_587.5
    val t = (julianDay - 2_451_545.0) / 36_525.0

    val meanLongitude = (280.46646 + t * (36_000.76983 + t * 0.0003032)).mod(360.0)
    val meanAnomaly = Math.toRadians(357.52911 + t * (35_999.05029 - 0.0001537 * t))
    val eccentricity = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)
    val center = sin(meanAnomaly) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
        sin(2 * meanAnomaly) * (0.019993 - 0.000101 * t) +
        sin(3 * meanAnomaly) * 0.000289
    val omega = Math.toRadians(125.04 - 1934.136 * t)
    val apparentLongitude = Math.toRadians(meanLongitude + center - 0.00569 - 0.00478 * sin(omega))
    val meanObliquity = 23.0 + (26.0 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60.0) / 60.0
    val obliquity = Math.toRadians(meanObliquity + 0.00256 * cos(omega))
    val declination = asin(sin(obliquity) * sin(apparentLongitude))

    val y = tan(obliquity / 2).let { it * it }
    val l0 = Math.toRadians(meanLongitude)
    val equationOfTimeMinutes = 4 * Math.toDegrees(
        y * sin(2 * l0) - 2 * eccentricity * sin(meanAnomaly) +
            4 * eccentricity * y * sin(meanAnomaly) * cos(2 * l0) -
            0.5 * y * y * sin(4 * l0) - 1.25 * eccentricity * eccentricity * sin(2 * meanAnomaly),
    )
    val utcMinutes = epochMillis.mod(MILLIS_PER_DAY) / 60_000.0
    val trueSolarMinutes = (utcMinutes + equationOfTimeMinutes + 4 * longitude).mod(1440.0)
    val hourAngle = Math.toRadians(trueSolarMinutes / 4 - 180)

    val lat = Math.toRadians(latitude)
    val sinElevation = sin(lat) * sin(declination) + cos(lat) * cos(declination) * cos(hourAngle)
    val elevation = Math.toDegrees(asin(sinElevation.coerceIn(-1.0, 1.0)))
    val azimuth = (
        Math.toDegrees(atan2(sin(hourAngle), cos(hourAngle) * sin(lat) - tan(declination) * cos(lat))) + 180
        ).mod(360.0)
    return SunPosition(elevation, azimuth)
}

/**
 * How high the sun is, in words: "below the horizon" under 0°, "low" under 20°, "medium" under
 * 45° and "high" from 45°. Low sun gives long shadows and warm light; high sun short shadows.
 */
fun elevationBand(elevation: Double): String = when {
    elevation < 0 -> "below the horizon"
    elevation < 20 -> "low"
    elevation < 45 -> "medium"
    else -> "high"
}

/** The nearest of the eight compass points for an [azimuth] in degrees clockwise from north. */
fun compassPoint(azimuth: Double): String =
    compassPoints[floor((azimuth.mod(360.0) + 22.5) / 45).toInt() % 8]

/** The sun for the list prompt: "low in the south-west", or "below the horizon" without a direction. */
fun sunWords(position: SunPosition): String {
    val band = elevationBand(position.elevation)
    return if (position.elevation < 0) band else "$band in the ${compassPoint(position.azimuth)}"
}
