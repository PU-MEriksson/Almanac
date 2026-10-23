package com.gezimos.katapult.astro

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WheelOfYearTest {
    private val sweden = ZoneId.of("Europe/Stockholm")

    private fun next(
        today: String,
        mode: CrossQuarterMode = CrossQuarterMode.TRADITIONAL,
        includeCrossQuarter: Boolean = true,
    ) = WheelOfYear.next(LocalDate.parse(today), sweden, mode, includeCrossQuarter)

    @Test
    fun samhainInNineDays() {
        // The example from CLAUDE.md, on the day this was written.
        assertEquals(UpcomingWheelEvent(WheelEvent.SAMHAIN, LocalDate.parse("2026-11-01"), 9), next("2026-10-23"))
    }

    @Test
    fun todayAndTomorrow() {
        assertEquals(1, next("2026-10-31").daysUntil)
        assertEquals(0, next("2026-11-01").daysUntil)
        assertEquals(WheelEvent.WINTER_SOLSTICE, next("2026-11-02").event)
    }

    @Test
    fun winterSolsticeUsesSwedishDate() {
        // 20:50 UTC on Dec 21 is 21:50 in Sweden — same date.
        assertEquals(LocalDate.parse("2026-12-21"), next("2026-12-01").date)
        // 02:43 UTC on Dec 22 2027.
        assertEquals(LocalDate.parse("2027-12-22"), next("2027-12-01").date)
    }

    @Test
    fun wrapsIntoNextYear() {
        assertEquals(UpcomingWheelEvent(WheelEvent.IMBOLC, LocalDate.parse("2027-02-01"), 41), next("2026-12-22"))
    }

    @Test
    fun withoutCrossQuarterDays() {
        assertEquals(
            UpcomingWheelEvent(WheelEvent.WINTER_SOLSTICE, LocalDate.parse("2026-12-21"), 59),
            next("2026-10-23", includeCrossQuarter = false),
        )
        assertEquals(WheelEvent.SPRING_EQUINOX, next("2026-12-22", includeCrossQuarter = false).event)
    }

    @Test
    fun astronomicalCrossQuarterDays() {
        println("\nAstronomical cross-quarter days 2026 (sun at 315°/45°/135°/225°)")
        // No published reference found for these; the dates are the Swedish calendar dates
        // of the computed instants, which were sanity-checked by hand.
        val expectedDates = mapOf(
            WheelEvent.IMBOLC to "2026-02-03",
            WheelEvent.BELTANE to "2026-05-05",
            WheelEvent.LUGHNASADH to "2026-08-07",
            WheelEvent.SAMHAIN to "2026-11-07",
        )
        for (event in expectedDates.keys) {
            val instant = WheelOfYear.astronomicalInstant(event, 2026)
            val sunLongitude = sunPosition(centuriesTT(instant)).apparentLongitude
            println("%-11s %s UTC (sun at %.4f°)".format(event, instant.toString().take(16), sunLongitude))
        }
        for ((event, expected) in expectedDates) {
            val instant = WheelOfYear.astronomicalInstant(event, 2026)
            assertEquals(event.sunLongitude, sunPosition(centuriesTT(instant)).apparentLongitude, 0.001)
            assertEquals(LocalDate.parse(expected), WheelOfYear.date(event, 2026, sweden, CrossQuarterMode.ASTRONOMICAL))
        }
    }
}
