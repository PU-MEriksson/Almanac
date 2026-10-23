package com.gezimos.katapult.astro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

class SunTest {
    private val sweden = ZoneId.of("Europe/Stockholm")
    private val hhmm = DateTimeFormatter.ofPattern("HH:mm")

    private class Place(val name: String, val latitude: Double, val longitude: Double)

    private val stockholm = Place("Stockholm", 59.3293, 18.0686)
    private val kiruna = Place("Kiruna", 67.8558, 20.2253)

    /** Expected local times, or "midnight sun" / "polar night". */
    private class Row(val place: Place, val date: String, val sunrise: String, val sunset: String)

    // Reference: MET Norway sunrise API (api.met.no/weatherapi/sunrise/3.0), fetched 2026-10-23.
    private val rows = listOf(
        Row(stockholm, "2026-10-23", "07:44", "17:18"),
        Row(stockholm, "2026-10-25", "06:49", "16:13"), // the day clocks go back to winter time
        Row(stockholm, "2026-06-21", "03:30", "22:08"),
        Row(stockholm, "2026-12-21", "08:43", "14:48"),
        Row(kiruna, "2026-10-23", "08:12", "16:33"),
        Row(kiruna, "2026-06-21", "midnight sun", "midnight sun"),
        Row(kiruna, "2026-12-21", "polar night", "polar night"),
    )

    @Test
    fun sunriseAndSunsetMatchReference() {
        println("\nSunrise / sunset (local time) — expected (MET Norway) vs actual")
        println("place      date        sunrise exp/act       sunset exp/act")
        for (row in rows) {
            val date = LocalDate.parse(row.date)
            val actual = Sun.times(date, row.place.latitude, row.place.longitude, sweden)
            when (actual) {
                is SunTimes.RiseSet -> {
                    val rise = actual.sunrise.atZone(sweden).toLocalTime()
                    val set = actual.sunset.atZone(sweden).toLocalTime()
                    println(
                        "%-10s %s  %s / %s (%+.1f min)   %s / %s (%+.1f min)".format(
                            row.place.name, row.date,
                            row.sunrise, rise.format(hhmm), minutesOff(row.sunrise, rise),
                            row.sunset, set.format(hhmm), minutesOff(row.sunset, set),
                        )
                    )
                    assertTrue("${row.place.name} ${row.date} sunrise", abs(minutesOff(row.sunrise, rise)) <= 2.0)
                    assertTrue("${row.place.name} ${row.date} sunset", abs(minutesOff(row.sunset, set)) <= 2.0)
                }
                SunTimes.MidnightSun -> {
                    println("%-10s %s  %s / midnight sun".format(row.place.name, row.date, row.sunrise))
                    assertEquals("midnight sun", row.sunrise)
                }
                SunTimes.PolarNight -> {
                    println("%-10s %s  %s / polar night".format(row.place.name, row.date, row.sunrise))
                    assertEquals("polar night", row.sunrise)
                }
            }
        }
    }

    /**
     * The reference gives whole minutes; compare against the middle of that minute so a
     * rounding difference doesn't count as a full minute off.
     */
    private fun minutesOff(expected: String, actual: LocalTime): Double {
        val expectedMid = LocalTime.parse(expected).plusSeconds(30)
        return Duration.between(expectedMid, actual).seconds / 60.0
    }
}
