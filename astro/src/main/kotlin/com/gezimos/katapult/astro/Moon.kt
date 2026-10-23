package com.gezimos.katapult.astro

import java.time.Instant
import kotlin.math.abs

/** How the moon looks at a given moment. */
data class MoonPhase(
    /** Fraction of the disc that is lit, 0.0 (new moon) to 1.0 (full moon). */
    val illumination: Double,
    /** True between new and full moon (growing), false between full and new (shrinking). */
    val waxing: Boolean,
    /** How far the moon is ahead of the sun along the ecliptic, 0..360°. 0 = new, 90 = first quarter, 180 = full, 270 = last quarter. */
    val elongation: Double,
)

enum class PrincipalPhase(val elongation: Double) {
    NEW_MOON(0.0),
    FIRST_QUARTER(90.0),
    FULL_MOON(180.0),
    LAST_QUARTER(270.0),
}

internal class MoonPosition(val longitude: Double, val latitude: Double, val distanceKm: Double)

/**
 * Periodic terms for the moon's longitude and distance (Meeus table 47.A), largest first.
 * Columns: multiples of D, M, M′, F, then longitude (10⁻⁶ °) and distance (10⁻³ km)
 * coefficients. Only the largest terms of the full table are used, which keeps the error
 * around 0.01°, i.e. a minute or two in the time of a phase.
 */
private val LONGITUDE_DISTANCE_TERMS = arrayOf(
    doubleArrayOf(0.0, 0.0, 1.0, 0.0, 6288774.0, -20905355.0),
    doubleArrayOf(2.0, 0.0, -1.0, 0.0, 1274027.0, -3699111.0),
    doubleArrayOf(2.0, 0.0, 0.0, 0.0, 658314.0, -2955968.0),
    doubleArrayOf(0.0, 0.0, 2.0, 0.0, 213618.0, -569925.0),
    doubleArrayOf(0.0, 1.0, 0.0, 0.0, -185116.0, 48888.0),
    doubleArrayOf(0.0, 0.0, 0.0, 2.0, -114332.0, -3149.0),
    doubleArrayOf(2.0, 0.0, -2.0, 0.0, 58793.0, 246158.0),
    doubleArrayOf(2.0, -1.0, -1.0, 0.0, 57066.0, -152138.0),
    doubleArrayOf(2.0, 0.0, 1.0, 0.0, 53322.0, -170733.0),
    doubleArrayOf(2.0, -1.0, 0.0, 0.0, 45758.0, -204586.0),
    doubleArrayOf(0.0, 1.0, -1.0, 0.0, -40923.0, -129620.0),
    doubleArrayOf(1.0, 0.0, 0.0, 0.0, -34720.0, 108743.0),
    doubleArrayOf(0.0, 1.0, 1.0, 0.0, -30383.0, 104755.0),
    doubleArrayOf(2.0, 0.0, 0.0, -2.0, 15327.0, 10321.0),
    doubleArrayOf(0.0, 0.0, 1.0, 2.0, -12528.0, 0.0),
    doubleArrayOf(0.0, 0.0, 1.0, -2.0, 10980.0, 79661.0),
    doubleArrayOf(4.0, 0.0, -1.0, 0.0, 10675.0, -34782.0),
    doubleArrayOf(0.0, 0.0, 3.0, 0.0, 10034.0, -23210.0),
    doubleArrayOf(4.0, 0.0, -2.0, 0.0, 8548.0, -21636.0),
    doubleArrayOf(2.0, 1.0, -1.0, 0.0, -7888.0, 24208.0),
    doubleArrayOf(2.0, 1.0, 0.0, 0.0, -6766.0, 30824.0),
    doubleArrayOf(1.0, 0.0, -1.0, 0.0, -5163.0, -8379.0),
    doubleArrayOf(1.0, 1.0, 0.0, 0.0, 4987.0, -16675.0),
    doubleArrayOf(2.0, -1.0, 1.0, 0.0, 4036.0, -12831.0),
    doubleArrayOf(2.0, 0.0, 2.0, 0.0, 3994.0, -10445.0),
    doubleArrayOf(4.0, 0.0, 0.0, 0.0, 3861.0, -11650.0),
    doubleArrayOf(2.0, 0.0, -3.0, 0.0, 3665.0, 14403.0),
    doubleArrayOf(0.0, 1.0, -2.0, 0.0, -2689.0, -7003.0),
    doubleArrayOf(2.0, 0.0, -1.0, 2.0, -2602.0, 0.0),
    doubleArrayOf(2.0, -1.0, -2.0, 0.0, 2390.0, 10056.0),
    doubleArrayOf(1.0, 0.0, 1.0, 0.0, -2348.0, 6322.0),
    doubleArrayOf(2.0, -2.0, 0.0, 0.0, 2236.0, -9884.0),
)

/** Periodic terms for the moon's latitude (Meeus table 47.B), largest first. Columns: D, M, M′, F, latitude (10⁻⁶ °). */
private val LATITUDE_TERMS = arrayOf(
    doubleArrayOf(0.0, 0.0, 0.0, 1.0, 5128122.0),
    doubleArrayOf(0.0, 0.0, 1.0, 1.0, 280602.0),
    doubleArrayOf(0.0, 0.0, 1.0, -1.0, 277693.0),
    doubleArrayOf(2.0, 0.0, 0.0, -1.0, 173237.0),
    doubleArrayOf(2.0, 0.0, -1.0, 1.0, 55413.0),
    doubleArrayOf(2.0, 0.0, -1.0, -1.0, 46271.0),
    doubleArrayOf(2.0, 0.0, 0.0, 1.0, 32573.0),
    doubleArrayOf(0.0, 0.0, 2.0, 1.0, 17198.0),
    doubleArrayOf(2.0, 0.0, 1.0, -1.0, 9266.0),
    doubleArrayOf(0.0, 0.0, 2.0, -1.0, 8822.0),
    doubleArrayOf(2.0, -1.0, 0.0, -1.0, 8216.0),
    doubleArrayOf(2.0, 0.0, -2.0, -1.0, 4324.0),
    doubleArrayOf(2.0, 0.0, 1.0, 1.0, 4200.0),
    doubleArrayOf(2.0, 1.0, 0.0, -1.0, -3359.0),
    doubleArrayOf(2.0, -1.0, -1.0, 1.0, 2463.0),
)

/** Moon position for [t] Julian centuries (TT) since J2000. Meeus ch. 47, shortened. */
internal fun moonPosition(t: Double): MoonPosition {
    val t2 = t * t
    val t3 = t2 * t
    val t4 = t3 * t
    // Fundamental arguments, degrees.
    val meanLongitude = 218.3164477 + 481267.88123421 * t - 0.0015786 * t2 + t3 / 538841.0 - t4 / 65194000.0
    val elongation = 297.8501921 + 445267.1114034 * t - 0.0018819 * t2 + t3 / 545868.0 - t4 / 113065000.0 // D
    val sunAnomaly = 357.5291092 + 35999.0502909 * t - 0.0001536 * t2 + t3 / 24490000.0 // M
    val moonAnomaly = 134.9633964 + 477198.8675055 * t + 0.0087414 * t2 + t3 / 69699.0 - t4 / 14712000.0 // M′
    val latitudeArgument = 93.2720950 + 483202.0175233 * t - 0.0036539 * t2 - t3 / 3526000.0 + t4 / 863310000.0 // F
    val a1 = 119.75 + 131.849 * t
    val a2 = 53.09 + 479264.290 * t
    val a3 = 313.45 + 481266.484 * t
    // The Earth's orbit is slowly getting rounder, which weakens terms that contain M.
    val e = 1 - 0.002516 * t - 0.0000074 * t2

    fun argument(row: DoubleArray) =
        row[0] * elongation + row[1] * sunAnomaly + row[2] * moonAnomaly + row[3] * latitudeArgument

    fun eccentricityFactor(row: DoubleArray) = when (abs(row[1]).toInt()) {
        1 -> e
        2 -> e * e
        else -> 1.0
    }

    var sumLongitude = 0.0
    var sumDistance = 0.0
    for (row in LONGITUDE_DISTANCE_TERMS) {
        val arg = argument(row)
        sumLongitude += row[4] * eccentricityFactor(row) * sind(arg)
        sumDistance += row[5] * eccentricityFactor(row) * cosd(arg)
    }
    sumLongitude += 3958 * sind(a1) + 1962 * sind(meanLongitude - latitudeArgument) + 318 * sind(a2)

    var sumLatitude = 0.0
    for (row in LATITUDE_TERMS) {
        sumLatitude += row[4] * eccentricityFactor(row) * sind(argument(row))
    }
    sumLatitude += -2235 * sind(meanLongitude) + 382 * sind(a3) +
        175 * sind(a1 - latitudeArgument) + 175 * sind(a1 + latitudeArgument) +
        127 * sind(meanLongitude - moonAnomaly) - 115 * sind(meanLongitude + moonAnomaly)

    return MoonPosition(
        longitude = norm360(meanLongitude + sumLongitude / 1_000_000),
        latitude = sumLatitude / 1_000_000,
        distanceKm = 385000.56 + sumDistance / 1000,
    )
}

object Moon {
    private const val AU_KM = 149_597_870.7

    /** The moon gains about 12.19° on the sun per day (360° per 29.53-day synodic month). */
    private const val ELONGATION_DEGREES_PER_DAY = 360.0 / 29.530589

    fun phase(instant: Instant): MoonPhase {
        val t = centuriesTT(instant)
        val moon = moonPosition(t)
        val sun = sunPosition(t)
        // The sun's longitude includes nutation; add the same to the moon so they compare fairly.
        val nutation = -0.00478 * sind(125.04 - 1934.136 * t)
        val elongation = norm360(moon.longitude + nutation - sun.apparentLongitude)

        // Phase angle (Meeus ch. 48): the angle sun–moon–earth. 0° = fully lit, 180° = dark.
        val sunMoonAngle = acosd(cosd(moon.latitude) * cosd(elongation))
        val sunDistanceKm = sun.distanceAu * AU_KM
        val phaseAngle = atan2d(
            sunDistanceKm * sind(sunMoonAngle),
            moon.distanceKm - sunDistanceKm * cosd(sunMoonAngle),
        )
        return MoonPhase(
            illumination = (1 + cosd(phaseAngle)) / 2,
            waxing = elongation < 180.0,
            elongation = elongation,
        )
    }

    /** The first time after [after] that the moon reaches [phase]. */
    fun next(phase: PrincipalPhase, after: Instant): Instant {
        val degreesToGo = norm360(phase.elongation - phase(after).elongation)
        var estimate = after.plusDays(degreesToGo / ELONGATION_DEGREES_PER_DAY)
        // Newton's method: the moon's speed varies, so correct the estimate a few times.
        repeat(5) {
            val error = signed180(phase.elongation - phase(estimate).elongation)
            estimate = estimate.plusDays(error / ELONGATION_DEGREES_PER_DAY)
        }
        return estimate
    }
}
