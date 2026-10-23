package com.gezimos.katapult.astro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

class MoonTest {
    private class Row(val phase: PrincipalPhase, val utc: String)

    // Reference: NASA / Fred Espenak, "Phases of the Moon 2001–2100" (eclipse.gsfc.nasa.gov), times in UT.
    private val rows = listOf(
        Row(PrincipalPhase.FULL_MOON, "2026-01-03T10:03:00Z"),
        Row(PrincipalPhase.NEW_MOON, "2026-01-18T19:52:00Z"),
        Row(PrincipalPhase.FULL_MOON, "2026-02-01T22:09:00Z"),
        Row(PrincipalPhase.NEW_MOON, "2026-02-17T12:01:00Z"),
        Row(PrincipalPhase.FULL_MOON, "2026-03-03T11:38:00Z"),
        Row(PrincipalPhase.NEW_MOON, "2026-03-19T01:23:00Z"),
        Row(PrincipalPhase.FULL_MOON, "2026-04-02T02:12:00Z"),
        Row(PrincipalPhase.NEW_MOON, "2026-04-17T11:52:00Z"),
        Row(PrincipalPhase.FULL_MOON, "2026-05-01T17:23:00Z"),
        Row(PrincipalPhase.NEW_MOON, "2026-05-16T20:01:00Z"),
        Row(PrincipalPhase.FULL_MOON, "2026-05-31T08:45:00Z"),
        Row(PrincipalPhase.NEW_MOON, "2026-06-15T02:54:00Z"),
        Row(PrincipalPhase.FULL_MOON, "2026-06-29T23:57:00Z"),
        Row(PrincipalPhase.NEW_MOON, "2026-07-14T09:43:00Z"),
        Row(PrincipalPhase.FULL_MOON, "2026-07-29T14:36:00Z"),
        Row(PrincipalPhase.NEW_MOON, "2026-08-12T17:37:00Z"),
        Row(PrincipalPhase.FULL_MOON, "2026-08-28T04:18:00Z"),
        Row(PrincipalPhase.NEW_MOON, "2026-09-11T03:27:00Z"),
        Row(PrincipalPhase.FULL_MOON, "2026-09-26T16:49:00Z"),
        Row(PrincipalPhase.NEW_MOON, "2026-10-10T15:50:00Z"),
        Row(PrincipalPhase.FIRST_QUARTER, "2026-10-18T16:13:00Z"),
        Row(PrincipalPhase.FULL_MOON, "2026-10-26T04:12:00Z"),
        Row(PrincipalPhase.LAST_QUARTER, "2026-11-01T20:28:00Z"),
        Row(PrincipalPhase.NEW_MOON, "2026-11-09T07:02:00Z"),
        Row(PrincipalPhase.FIRST_QUARTER, "2026-11-17T11:48:00Z"),
        Row(PrincipalPhase.FULL_MOON, "2026-11-24T14:53:00Z"),
        Row(PrincipalPhase.LAST_QUARTER, "2026-12-01T06:09:00Z"),
        Row(PrincipalPhase.NEW_MOON, "2026-12-09T00:52:00Z"),
        Row(PrincipalPhase.FIRST_QUARTER, "2026-12-17T05:43:00Z"),
        Row(PrincipalPhase.FULL_MOON, "2026-12-24T01:28:00Z"),
        Row(PrincipalPhase.LAST_QUARTER, "2026-12-30T18:59:00Z"),
    )

    @Test
    fun phaseTimesMatchReference() {
        println("\nMoon phases (UTC) — expected (NASA) vs actual")
        for (row in rows) {
            val expected = Instant.parse(row.utc)
            val actual = Moon.next(row.phase, expected.minus(Duration.ofDays(10)))
            val minutesOff = Duration.between(expected.plusSeconds(30), actual).seconds / 60.0
            println("%-14s %s / %s (%+.1f min)".format(row.phase, row.utc.take(16), actual.toString().take(16), minutesOff))
            assertTrue("${row.phase} ${row.utc}", abs(minutesOff) <= 60.0)
        }
    }

    @Test
    fun illuminationAtPrincipalPhases() {
        println("\nMoon illumination at the reference phase times — expected vs actual")
        val expectedIllumination = mapOf(
            PrincipalPhase.NEW_MOON to 0.0,
            PrincipalPhase.FIRST_QUARTER to 0.5,
            PrincipalPhase.FULL_MOON to 1.0,
            PrincipalPhase.LAST_QUARTER to 0.5,
        )
        for (row in rows) {
            val phase = Moon.phase(Instant.parse(row.utc))
            val expected = expectedIllumination.getValue(row.phase)
            println("%-14s %s  %3.0f%% / %5.1f%%".format(row.phase, row.utc.take(10), expected * 100, phase.illumination * 100))
            // At a new or full moon the moon is usually a little above/below the sun's path,
            // so it is never exactly 0 % or 100 %.
            assertEquals("${row.phase} ${row.utc}", expected, phase.illumination, 0.02)
        }
    }

    @Test
    fun waxingAndWaning() {
        assertTrue(Moon.phase(Instant.parse("2026-10-18T16:13:00Z")).waxing) // first quarter
        assertFalse(Moon.phase(Instant.parse("2026-11-01T20:28:00Z")).waxing) // last quarter
        assertTrue(Moon.phase(Instant.parse("2026-10-26T00:00:00Z")).waxing) // 4 h before full
        assertFalse(Moon.phase(Instant.parse("2026-10-26T08:00:00Z")).waxing) // 4 h after full
    }
}
