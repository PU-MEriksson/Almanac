package com.gezimos.katapult.astro

import java.time.Instant
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin

// Small helpers shared by the sun, moon and season calculations. The astronomy formulas
// are written in degrees (as in Meeus' book), so these trig helpers take degrees too.

internal const val J2000 = 2451545.0

/**
 * ΔT = TT − UT: the difference between the smooth "dynamical" time the formulas use and
 * clock time. It is about 69 s in the 2020s and drifts slowly, so a constant is accurate
 * to well under a minute for decades.
 */
internal const val DELTA_T_SECONDS = 69.0

private const val MILLIS_PER_DAY = 86_400_000.0
private const val UNIX_EPOCH_JD = 2440587.5

internal fun julianDay(instant: Instant): Double =
    instant.toEpochMilli() / MILLIS_PER_DAY + UNIX_EPOCH_JD

internal fun instantOfJulianDay(jd: Double): Instant =
    Instant.ofEpochMilli(((jd - UNIX_EPOCH_JD) * MILLIS_PER_DAY).roundToLong())

/** Julian centuries since J2000 in dynamical time: the "T" in Meeus' formulas. */
internal fun centuriesTT(instant: Instant): Double =
    (julianDay(instant) + DELTA_T_SECONDS / 86_400.0 - J2000) / 36_525.0

internal fun Instant.plusDays(days: Double): Instant =
    plusMillis((days * MILLIS_PER_DAY).roundToLong())

/** Wraps an angle into 0..360°. */
internal fun norm360(deg: Double): Double {
    val r = deg % 360.0
    return if (r < 0) r + 360.0 else r
}

/** Wraps an angle into -180..180°, e.g. "how far ahead/behind" one angle is of another. */
internal fun signed180(deg: Double): Double = norm360(deg + 180.0) - 180.0

internal fun sind(deg: Double) = sin(deg * PI / 180.0)
internal fun cosd(deg: Double) = cos(deg * PI / 180.0)
internal fun asind(x: Double) = asin(x) * 180.0 / PI
internal fun acosd(x: Double) = acos(x) * 180.0 / PI
internal fun atan2d(y: Double, x: Double) = atan2(y, x) * 180.0 / PI
