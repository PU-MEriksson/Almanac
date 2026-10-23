package com.gezimos.katapult.astro

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * The eight points of the wheel of the year (northern hemisphere), in calendar order.
 * [sunLongitude] is where the sun is on the ecliptic at that moment; cross-quarter days
 * sit halfway between a solstice and an equinox.
 */
enum class WheelEvent(val isCrossQuarter: Boolean, val sunLongitude: Double) {
    IMBOLC(true, 315.0),
    SPRING_EQUINOX(false, 0.0),
    BELTANE(true, 45.0),
    SUMMER_SOLSTICE(false, 90.0),
    LUGHNASADH(true, 135.0),
    AUTUMN_EQUINOX(false, 180.0),
    SAMHAIN(true, 225.0),
    WINTER_SOLSTICE(false, 270.0),
}

enum class CrossQuarterMode {
    /** Feb 1, May 1, Aug 1, Nov 1. */
    TRADITIONAL,

    /** When the sun is exactly halfway between solstice and equinox (around Feb 4, May 5, Aug 7, Nov 7). */
    ASTRONOMICAL,
}

data class UpcomingWheelEvent(
    val event: WheelEvent,
    val date: LocalDate,
    /** 0 = today, 1 = tomorrow. */
    val daysUntil: Int,
)

object WheelOfYear {
    /** The sun moves about 0.9856° along the ecliptic per day (360° per tropical year). */
    private const val SUN_DEGREES_PER_DAY = 360.0 / 365.2422

    /** The next event on or after [today]. Days are counted as calendar days in [zone]. */
    fun next(
        today: LocalDate,
        zone: ZoneId,
        mode: CrossQuarterMode = CrossQuarterMode.TRADITIONAL,
        includeCrossQuarter: Boolean = true,
    ): UpcomingWheelEvent {
        val events = WheelEvent.values().filter { includeCrossQuarter || !it.isCrossQuarter }
        // Every event happens once per calendar year, so this year and next always contain the answer.
        return listOf(today.year, today.year + 1)
            .flatMap { year -> events.map { event -> event to date(event, year, zone, mode) } }
            .filter { (_, date) -> !date.isBefore(today) }
            .minByOrNull { (_, date) -> date }!!
            .let { (event, date) -> UpcomingWheelEvent(event, date, ChronoUnit.DAYS.between(today, date).toInt()) }
    }

    /** The calendar date (in [zone]) of [event] in [year]. */
    fun date(event: WheelEvent, year: Int, zone: ZoneId, mode: CrossQuarterMode): LocalDate = when (event) {
        WheelEvent.SPRING_EQUINOX -> localDate(Seasons.instant(year, Season.MARCH_EQUINOX), zone)
        WheelEvent.SUMMER_SOLSTICE -> localDate(Seasons.instant(year, Season.JUNE_SOLSTICE), zone)
        WheelEvent.AUTUMN_EQUINOX -> localDate(Seasons.instant(year, Season.SEPTEMBER_EQUINOX), zone)
        WheelEvent.WINTER_SOLSTICE -> localDate(Seasons.instant(year, Season.DECEMBER_SOLSTICE), zone)
        else -> when (mode) {
            CrossQuarterMode.TRADITIONAL -> LocalDate.of(year, traditionalMonth(event), 1)
            CrossQuarterMode.ASTRONOMICAL -> localDate(astronomicalInstant(event, year), zone)
        }
    }

    /** The moment the sun reaches the event's longitude in [year] (used for cross-quarter days). */
    fun astronomicalInstant(event: WheelEvent, year: Int): Instant {
        // Start from the approximate date and correct with Newton's method.
        var estimate = LocalDate.of(year, traditionalMonth(event), 5).atStartOfDay().toInstant(ZoneOffset.UTC)
        repeat(4) {
            val sunLongitude = sunPosition(centuriesTT(estimate)).apparentLongitude
            val error = signed180(event.sunLongitude - sunLongitude)
            estimate = estimate.plusDays(error / SUN_DEGREES_PER_DAY)
        }
        return estimate
    }

    private fun traditionalMonth(event: WheelEvent): Int = when (event) {
        WheelEvent.IMBOLC -> 2
        WheelEvent.SPRING_EQUINOX -> 3
        WheelEvent.BELTANE -> 5
        WheelEvent.SUMMER_SOLSTICE -> 6
        WheelEvent.LUGHNASADH -> 8
        WheelEvent.AUTUMN_EQUINOX -> 9
        WheelEvent.SAMHAIN -> 11
        WheelEvent.WINTER_SOLSTICE -> 12
    }

    private fun localDate(instant: Instant, zone: ZoneId): LocalDate = instant.atZone(zone).toLocalDate()
}
