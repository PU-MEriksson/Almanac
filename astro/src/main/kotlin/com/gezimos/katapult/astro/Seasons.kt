package com.gezimos.katapult.astro

import java.time.Instant

enum class Season {
    MARCH_EQUINOX,
    JUNE_SOLSTICE,
    SEPTEMBER_EQUINOX,
    DECEMBER_SOLSTICE,
}

/**
 * Equinoxes and solstices with Jean Meeus' method (Astronomical Algorithms ch. 27).
 * Accurate to about a minute for the years 1000–3000.
 */
object Seasons {
    /** Periodic terms (Meeus table 27.C). Columns: A, B (degrees), C (degrees per century). */
    private val PERIODIC_TERMS = arrayOf(
        doubleArrayOf(485.0, 324.96, 1934.136),
        doubleArrayOf(203.0, 337.23, 32964.467),
        doubleArrayOf(199.0, 342.08, 20.186),
        doubleArrayOf(182.0, 27.85, 445267.112),
        doubleArrayOf(156.0, 73.14, 45036.886),
        doubleArrayOf(136.0, 171.52, 22518.443),
        doubleArrayOf(77.0, 222.54, 65928.934),
        doubleArrayOf(74.0, 296.72, 3034.906),
        doubleArrayOf(70.0, 243.58, 9037.513),
        doubleArrayOf(58.0, 119.81, 33718.147),
        doubleArrayOf(52.0, 297.17, 150.678),
        doubleArrayOf(50.0, 21.02, 2281.226),
        doubleArrayOf(45.0, 247.54, 29929.562),
        doubleArrayOf(44.0, 325.15, 31555.956),
        doubleArrayOf(29.0, 60.93, 4443.417),
        doubleArrayOf(18.0, 155.12, 67555.328),
        doubleArrayOf(17.0, 288.79, 4562.452),
        doubleArrayOf(16.0, 198.04, 62894.029),
        doubleArrayOf(14.0, 199.76, 31436.921),
        doubleArrayOf(12.0, 95.39, 14577.848),
        doubleArrayOf(12.0, 287.11, 31931.756),
        doubleArrayOf(12.0, 320.81, 34777.259),
        doubleArrayOf(9.0, 227.73, 1222.114),
        doubleArrayOf(8.0, 15.45, 16859.074),
    )

    fun instant(year: Int, season: Season): Instant {
        val jde0 = meanJde(year, season)
        val t = (jde0 - J2000) / 36_525.0
        val w = 35999.373 * t - 2.47
        val deltaLambda = 1 + 0.0334 * cosd(w) + 0.0007 * cosd(2 * w)
        var s = 0.0
        for (row in PERIODIC_TERMS) s += row[0] * cosd(row[1] + row[2] * t)
        val jde = jde0 + 0.00001 * s / deltaLambda
        // JDE is in dynamical time; subtract ΔT to get clock time (UT).
        return instantOfJulianDay(jde - DELTA_T_SECONDS / 86_400.0)
    }

    /** Mean time of the event (Meeus table 27.B, years 1000–3000). */
    private fun meanJde(year: Int, season: Season): Double {
        val y = (year - 2000) / 1000.0
        val y2 = y * y
        val y3 = y2 * y
        val y4 = y3 * y
        return when (season) {
            Season.MARCH_EQUINOX -> 2451623.80984 + 365242.37404 * y + 0.05169 * y2 - 0.00411 * y3 - 0.00057 * y4
            Season.JUNE_SOLSTICE -> 2451716.56767 + 365241.62603 * y + 0.00325 * y2 + 0.00888 * y3 - 0.00030 * y4
            Season.SEPTEMBER_EQUINOX -> 2451810.21715 + 365242.01767 * y - 0.11575 * y2 + 0.00337 * y3 + 0.00078 * y4
            Season.DECEMBER_SOLSTICE -> 2451900.05952 + 365242.74049 * y - 0.06223 * y2 - 0.00823 * y3 + 0.00032 * y4
        }
    }
}
