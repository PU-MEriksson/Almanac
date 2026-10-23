package com.gezimos.katapult.astro

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.tan

/** Sunrise and sunset for one day, or why there isn't one. */
sealed class SunTimes {
    data class RiseSet(val sunrise: Instant, val sunset: Instant) : SunTimes()

    /** The sun stays above the horizon all day. */
    object MidnightSun : SunTimes()

    /** The sun stays below the horizon all day. */
    object PolarNight : SunTimes()
}

internal class SunPosition(
    /** Apparent ecliptic longitude, degrees. 0° = March equinox, 90° = June solstice… */
    val apparentLongitude: Double,
    /** Degrees north (+) or south (−) of the celestial equator. */
    val declination: Double,
    val distanceAu: Double,
    /** How far a sundial is ahead of mean clock time, minutes. */
    val equationOfTimeMinutes: Double,
)

/**
 * Sun position for [t] Julian centuries (TT) since J2000.
 * Meeus, Astronomical Algorithms ch. 25 (low accuracy, ~0.01°) and ch. 28 for the equation
 * of time. These are the same formulas as the NOAA solar calculator spreadsheet.
 */
internal fun sunPosition(t: Double): SunPosition {
    val meanLongitude = norm360(280.46646 + t * (36000.76983 + t * 0.0003032))
    val meanAnomaly = 357.52911 + t * (35999.05029 - t * 0.0001537)
    val eccentricity = 0.016708634 - t * (0.000042037 + t * 0.0000001267)
    val equationOfCentre =
        sind(meanAnomaly) * (1.914602 - t * (0.004817 + t * 0.000014)) +
            sind(2 * meanAnomaly) * (0.019993 - t * 0.000101) +
            sind(3 * meanAnomaly) * 0.000289
    val trueLongitude = meanLongitude + equationOfCentre
    val trueAnomaly = meanAnomaly + equationOfCentre
    val distance = 1.000001018 * (1 - eccentricity * eccentricity) / (1 + eccentricity * cosd(trueAnomaly))

    // Correct for aberration (−0.00569°) and nutation (−0.00478° · sin Ω).
    val omega = 125.04 - 1934.136 * t
    val apparentLongitude = norm360(trueLongitude - 0.00569 - 0.00478 * sind(omega))

    val meanObliquity = 23.0 + (26.0 + (21.448 - t * (46.8150 + t * (0.00059 - t * 0.001813))) / 60.0) / 60.0
    val obliquity = meanObliquity + 0.00256 * cosd(omega)
    val declination = asind(sind(obliquity) * sind(apparentLongitude))

    val y = tan(obliquity / 2 * PI / 180.0).pow(2)
    val equationOfTimeRadians =
        y * sind(2 * meanLongitude) -
            2 * eccentricity * sind(meanAnomaly) +
            4 * eccentricity * y * sind(meanAnomaly) * cosd(2 * meanLongitude) -
            0.5 * y * y * sind(4 * meanLongitude) -
            1.25 * eccentricity * eccentricity * sind(2 * meanAnomaly)
    val equationOfTimeMinutes = 4 * equationOfTimeRadians * 180.0 / PI

    return SunPosition(apparentLongitude, declination, distance, equationOfTimeMinutes)
}

/** Sunrise/sunset with the NOAA method. Accurate to about a minute outside the polar circles. */
object Sun {
    /**
     * The sun "rises" when its centre is 0.833° below the horizon: 0.567° for atmospheric
     * refraction plus 0.266° for the sun's radius (the top edge touches the horizon).
     */
    private const val SUNRISE_ALTITUDE = -0.833

    /** Sunrise and sunset on [date] (a calendar date in [zone]) at the given position. */
    fun times(date: LocalDate, latitude: Double, longitude: Double, zone: ZoneId): SunTimes {
        val localNoon = date.atTime(12, 0).atZone(zone).toInstant()
        val transit = solarNoonNear(solarNoonNear(localNoon, longitude), longitude)

        val cosHourAngle = cosHourAngle(latitude, sunPosition(centuriesTT(transit)).declination)
        if (cosHourAngle < -1) return SunTimes.MidnightSun
        if (cosHourAngle > 1) return SunTimes.PolarNight

        return SunTimes.RiseSet(
            sunrise = riseOrSet(transit, latitude, rising = true),
            sunset = riseOrSet(transit, latitude, rising = false),
        )
    }

    /** Cosine of the hour angle between solar noon and sunrise/sunset. Outside −1..1 means no sunrise/sunset. */
    private fun cosHourAngle(latitude: Double, declination: Double): Double =
        (sind(SUNRISE_ALTITUDE) - sind(latitude) * sind(declination)) /
            (cosd(latitude) * cosd(declination))

    /** The moment the sun is highest in the sky (due south), within 12 hours of [near]. */
    private fun solarNoonNear(near: Instant, longitude: Double): Instant {
        val equationOfTime = sunPosition(centuriesTT(near)).equationOfTimeMinutes
        // At longitude 0° solar noon is 12:00 UTC minus the equation of time;
        // every degree east makes it 4 minutes earlier.
        val minutesAfterUtcMidnight = 720.0 - 4.0 * longitude - equationOfTime
        var noon = near.truncatedTo(ChronoUnit.DAYS).plusMillis((minutesAfterUtcMidnight * 60_000).roundToLong())
        val halfDay = Duration.ofHours(12)
        while (Duration.between(near, noon) > halfDay) noon = noon.minus(Duration.ofDays(1))
        while (Duration.between(noon, near) > halfDay) noon = noon.plus(Duration.ofDays(1))
        return noon
    }

    /**
     * Sunrise (or sunset) for the day around [transit]. The sun's declination changes during
     * the day, so the first estimate is refined by recomputing at the estimated time.
     */
    private fun riseOrSet(transit: Instant, latitude: Double, rising: Boolean): Instant {
        val transitEquationOfTime = sunPosition(centuriesTT(transit)).equationOfTimeMinutes
        var estimate = transit
        repeat(3) {
            val sun = sunPosition(centuriesTT(estimate))
            // Near the polar circles the sun may only just reach the horizon; clamp to noon/midnight.
            val hourAngle = acosd(cosHourAngle(latitude, sun.declination).coerceIn(-1.0, 1.0))
            // The sun moves 15° of hour angle per hour, i.e. 4 minutes per degree. The
            // equation of time also shifts slightly between noon and the estimate.
            val minutesFromNoon = (if (rising) -4 * hourAngle else 4 * hourAngle) +
                (transitEquationOfTime - sun.equationOfTimeMinutes)
            estimate = transit.plusMillis((minutesFromNoon * 60_000).roundToLong())
        }
        return estimate
    }
}
