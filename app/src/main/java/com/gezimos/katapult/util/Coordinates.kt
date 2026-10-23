package com.gezimos.katapult.util

import java.math.BigDecimal
import java.math.RoundingMode

/** Reading and showing latitude/longitude typed in settings. */
object Coordinates {
    const val MAX_LATITUDE = 90.0
    const val MAX_LONGITUDE = 180.0

    /**
     * Parses "59.33", "59,33" (decimal comma) or "-5.9". Returns null if the text isn't a
     * number or is outside -[limit]..[limit].
     */
    fun parse(text: String, limit: Double): Double? {
        val value = text.trim().replace(',', '.').replace('−', '-').toDoubleOrNull() ?: return null
        return value.takeIf { it in -limit..limit }
    }

    /** Up to 4 decimals (about 10 m), without trailing zeros: 59.3293, 18.07, 20. */
    fun format(value: Double): String =
        BigDecimal(value).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

    /** Rounds to 2 decimals (about 1 km): plenty for sunrise times, and less precise to store. */
    fun roundForStorage(value: Double): Double =
        BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toDouble()
}
