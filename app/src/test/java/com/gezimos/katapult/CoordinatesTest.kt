package com.gezimos.katapult

import com.gezimos.katapult.util.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoordinatesTest {
    @Test
    fun parsesDecimalPointAndComma() {
        assertEquals(59.33, Coordinates.parse("59.33", Coordinates.MAX_LATITUDE)!!, 1e-9)
        assertEquals(59.33, Coordinates.parse(" 59,33 ", Coordinates.MAX_LATITUDE)!!, 1e-9)
        assertEquals(-5.9, Coordinates.parse("-5,9", Coordinates.MAX_LONGITUDE)!!, 1e-9)
        assertEquals(-5.9, Coordinates.parse("−5.9", Coordinates.MAX_LONGITUDE)!!, 1e-9) // Unicode minus
    }

    @Test
    fun rejectsOutOfRangeAndGarbage() {
        assertNull(Coordinates.parse("91", Coordinates.MAX_LATITUDE))
        assertNull(Coordinates.parse("-180.5", Coordinates.MAX_LONGITUDE))
        assertNull(Coordinates.parse("", Coordinates.MAX_LATITUDE))
        assertNull(Coordinates.parse("59.3.3", Coordinates.MAX_LATITUDE))
        assertNull(Coordinates.parse("abc", Coordinates.MAX_LATITUDE))
    }

    @Test
    fun formatsWithoutTrailingZeros() {
        assertEquals("59.3293", Coordinates.format(59.32932))
        assertEquals("18.07", Coordinates.format(18.07))
        assertEquals("20", Coordinates.format(20.0))
        assertEquals(67.86, Coordinates.roundForStorage(67.8558), 1e-9)
    }
}
