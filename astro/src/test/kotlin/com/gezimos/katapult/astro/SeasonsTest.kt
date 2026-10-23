package com.gezimos.katapult.astro

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

class SeasonsTest {
    private class Row(val year: Int, val season: Season, val utc: String)

    // Reference: Fred Espenak, "Solstices and Equinoxes 2001–2100" (astropixels.com), times in UT.
    private val rows = listOf(
        Row(2026, Season.MARCH_EQUINOX, "2026-03-20T14:46:00Z"),
        Row(2026, Season.JUNE_SOLSTICE, "2026-06-21T08:25:00Z"),
        Row(2026, Season.SEPTEMBER_EQUINOX, "2026-09-23T00:06:00Z"),
        Row(2026, Season.DECEMBER_SOLSTICE, "2026-12-21T20:50:00Z"),
        Row(2027, Season.MARCH_EQUINOX, "2027-03-20T20:25:00Z"),
        Row(2027, Season.JUNE_SOLSTICE, "2027-06-21T14:11:00Z"),
        Row(2027, Season.SEPTEMBER_EQUINOX, "2027-09-23T06:02:00Z"),
        Row(2027, Season.DECEMBER_SOLSTICE, "2027-12-22T02:43:00Z"),
    )

    @Test
    fun equinoxesAndSolsticesMatchReference() {
        println("\nEquinoxes / solstices (UTC) — expected (Espenak) vs actual")
        for (row in rows) {
            val actual = Seasons.instant(row.year, row.season)
            val minutesOff = Duration.between(Instant.parse(row.utc).plusSeconds(30), actual).seconds / 60.0
            println("%-18s %s / %s (%+.1f min)".format(row.season, row.utc.take(16), actual.toString().take(16), minutesOff))
            assertTrue("${row.season} ${row.year}", abs(minutesOff) <= 2.0)
        }
    }

    /** Cross-check: the sun-position formula (used for sunrise and cross-quarter days) agrees with Meeus ch. 27. */
    @Test
    fun sunPositionAgreesWithSeasons() {
        for (row in rows) {
            val longitude = sunPosition(centuriesTT(Seasons.instant(row.year, row.season))).apparentLongitude
            val expected = row.season.ordinal * 90.0
            // 0.01° of solar longitude is about 15 minutes.
            assertTrue("${row.season} ${row.year}: $longitude", abs(signed180(longitude - expected)) < 0.01)
        }
    }
}
