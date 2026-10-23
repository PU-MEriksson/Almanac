package com.gezimos.katapult.util

import com.gezimos.katapult.astro.CrossQuarterMode
import com.gezimos.katapult.astro.SkyDay
import com.gezimos.katapult.astro.SkyInfo
import java.time.LocalDate
import java.time.ZoneId

/**
 * Today's sky info for the clock. The clock asks every 15 seconds, but the calculation only
 * runs again when the date, time zone or a sky setting has changed. Returning the same
 * object also means Compose doesn't redraw the lines, which matters on e-ink.
 */
object SkyHelper {
    private data class Inputs(
        val date: LocalDate,
        val zone: ZoneId,
        val latitude: Double,
        val longitude: Double,
        val crossQuarter: Int,
    )

    private var cachedInputs: Inputs? = null
    private var cachedDay: SkyDay? = null

    /** Null when the sky info is turned off or no location is set. */
    @Synchronized
    fun today(prefs: PrefsManager): SkyDay? {
        if (!prefs.showSky) return null
        val latitude = prefs.skyLatitude ?: return null
        val longitude = prefs.skyLongitude ?: return null
        val zone = ZoneId.systemDefault()
        val inputs = Inputs(LocalDate.now(zone), zone, latitude, longitude, prefs.skyCrossQuarter)
        if (inputs == cachedInputs) return cachedDay

        val day = SkyInfo.forDay(
            date = inputs.date,
            latitude = latitude,
            longitude = longitude,
            zone = zone,
            crossQuarterMode = if (inputs.crossQuarter == PrefsManager.SKY_CROSS_QUARTER_ASTRONOMICAL)
                CrossQuarterMode.ASTRONOMICAL else CrossQuarterMode.TRADITIONAL,
            includeCrossQuarter = inputs.crossQuarter != PrefsManager.SKY_CROSS_QUARTER_HIDDEN,
        )
        cachedInputs = inputs
        cachedDay = day
        return day
    }
}
