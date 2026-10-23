package com.gezimos.katapult.astro

import java.time.LocalDate
import java.time.ZoneId

/** Everything the sky info lines show for one day. */
data class SkyDay(
    val date: LocalDate,
    val sun: SunTimes,
    /** The moon at local noon, so the value is stable for the whole day. */
    val moon: MoonPhase,
    val nextWheelEvent: UpcomingWheelEvent,
)

object SkyInfo {
    fun forDay(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zone: ZoneId,
        crossQuarterMode: CrossQuarterMode = CrossQuarterMode.TRADITIONAL,
        includeCrossQuarter: Boolean = true,
    ): SkyDay = SkyDay(
        date = date,
        sun = Sun.times(date, latitude, longitude, zone),
        moon = Moon.phase(date.atTime(12, 0).atZone(zone).toInstant()),
        nextWheelEvent = WheelOfYear.next(date, zone, crossQuarterMode, includeCrossQuarter),
    )
}
