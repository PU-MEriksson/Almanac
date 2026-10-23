package com.gezimos.katapult.ui

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gezimos.katapult.R
import com.gezimos.katapult.astro.MoonPhase
import com.gezimos.katapult.astro.SkyDay
import com.gezimos.katapult.astro.SunTimes
import com.gezimos.katapult.astro.WheelEvent
import java.util.Date
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The three sky lines under the date: moon phase, sunrise–sunset, next wheel-of-the-year
 * event, left-aligned and optionally inside a card. Same font and size as the status row
 * above the clock; the card has the same corners as the home islands but a lighter line.
 */
@Composable
fun SkyInfoLines(sky: SkyDay, card: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val timeFormat = DateFormat.getTimeFormat(context)
    val cardShape = RoundedCornerShape(19.dp)

    Column(
        modifier = modifier
            .then(
                if (card) Modifier
                    .background(LocalSurface.current, cardShape)
                    .border(1.5.dp, LocalInk.current, cardShape)
                    .padding(start = 14.dp, end = 18.dp, top = 10.dp, bottom = 10.dp)
                else Modifier
            ),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        SkyLine(icon = { MoonIcon(sky.moon) }) {
            stringResource(
                if (sky.moon.waxing) R.string.sky_moon_waxing else R.string.sky_moon_waning,
                (sky.moon.illumination * 100).roundToInt(),
            )
        }
        when (val sun = sky.sun) {
            is SunTimes.RiseSet -> SkyLine(icon = { SkyIcon(Icons.Rounded.WbTwilight) }) {
                stringResource(
                    R.string.sky_sun_times,
                    timeFormat.format(Date.from(sun.sunrise)),
                    timeFormat.format(Date.from(sun.sunset)),
                )
            }
            SunTimes.MidnightSun -> SkyLine(icon = { SkyIcon(Icons.Rounded.WbSunny) }) {
                stringResource(R.string.sky_midnight_sun)
            }
            SunTimes.PolarNight -> SkyLine(icon = { MoonIcon(sky.moon) }) {
                stringResource(R.string.sky_polar_night)
            }
        }
        val next = sky.nextWheelEvent
        val name = stringResource(wheelEventName(next.event))
        SkyLine(icon = { WheelIcon(next.event) }) {
            when (next.daysUntil) {
                0 -> stringResource(R.string.sky_event_today, name)
                1 -> stringResource(R.string.sky_event_tomorrow, name)
                else -> stringResource(R.string.sky_event_in_days, name, next.daysUntil)
            }
        }
    }
}

/** Width of the icon column, so the text of all three lines starts at the same place. */
private val IconSlot = 22.dp

@Composable
private fun SkyLine(icon: (@Composable () -> Unit)?, text: @Composable () -> String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(IconSlot), contentAlignment = Alignment.Center) {
            icon?.invoke()
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = text(),
            fontSize = 18.sp,
            fontFamily = LatoFamily,
            color = LocalInk.current,
        )
    }
}

@Composable
private fun SkyIcon(imageVector: ImageVector) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = LocalInk.current,
        modifier = Modifier.size(IconSlot),
    )
}

/**
 * The moon drawn as an outlined disc with its shadowed part filled in ink (like ◐), as seen
 * from the northern hemisphere: a waxing moon is lit on the right. Only solid ink, no grey,
 * so it stays crisp on e-ink.
 */
@Composable
fun MoonIcon(moon: MoonPhase, modifier: Modifier = Modifier.size(20.dp)) {
    val ink = LocalInk.current
    Canvas(modifier) {
        // Thin outline: on the Kompakt's screen 1dp is about 1.3 pixels, which leaves room
        // for a thin crescent of shadow to show near full moon.
        val strokeWidth = 1.dp.toPx()
        val radius = size.minDimension / 2 - strokeWidth / 2
        val shadow = moonShadowPath(center, radius, moon.illumination)
        // The path is drawn for a waxing moon (shadow on the left); mirror it when waning.
        scale(scaleX = if (moon.waxing) 1f else -1f, scaleY = 1f, pivot = center) {
            drawPath(shadow, ink)
        }
        drawCircle(ink, radius, center, style = Stroke(strokeWidth))
    }
}

/**
 * The wheel of the year: a circle with a cross for the solstices and equinoxes, and a dot on
 * the rim where [event] sits. Winter solstice is at the top, spring equinox on the right,
 * summer solstice at the bottom and autumn equinox on the left, so the dot moves clockwise
 * through the year.
 */
@Composable
fun WheelIcon(event: WheelEvent, modifier: Modifier = Modifier.size(20.dp)) {
    val ink = LocalInk.current
    Canvas(modifier) {
        val strokeWidth = 1.dp.toPx()
        val dotRadius = 3.dp.toPx()
        // Leave room for the dot, which is centred on the rim.
        val radius = size.minDimension / 2 - dotRadius
        drawCircle(ink, radius, center, style = Stroke(strokeWidth))
        drawLine(ink, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), strokeWidth)
        drawLine(ink, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), strokeWidth)
        // Degrees clockwise from the top: the winter solstice (sun at 270°) is 0°.
        val angle = Math.toRadians(event.sunLongitude - 270.0)
        val dot = Offset(
            x = center.x + (radius * sin(angle)).toFloat(),
            y = center.y - (radius * cos(angle)).toFloat(),
        )
        drawCircle(ink, dotRadius, dot)
    }
}

/**
 * Outline of the dark part of a waxing moon with [illumination] 0..1. It runs down the left
 * edge of the disc, then back up along the terminator (the line between day and night on the
 * moon), which seen from Earth is half an ellipse with width |1 − 2 × illumination| × radius.
 */
private fun moonShadowPath(center: Offset, radius: Float, illumination: Double): Path {
    val disc = Rect(center, radius)
    val halfWidth = (abs(1 - 2 * illumination) * radius).toFloat()
    val terminator = Rect(center.x - halfWidth, center.y - radius, center.x + halfWidth, center.y + radius)
    return Path().apply {
        moveTo(center.x, center.y - radius)
        // Left edge, top → bottom (angles are clockwise from 3 o'clock, so −90° is the top).
        arcTo(disc, -90f, -180f, false)
        // Terminator, bottom → top: bulging right for a crescent (more shadow), left for gibbous.
        arcTo(terminator, 90f, if (illumination < 0.5) -180f else 180f, false)
        close()
    }
}

private fun wheelEventName(event: WheelEvent): Int = when (event) {
    WheelEvent.IMBOLC -> R.string.sky_imbolc
    WheelEvent.SPRING_EQUINOX -> R.string.sky_spring_equinox
    WheelEvent.BELTANE -> R.string.sky_beltane
    WheelEvent.SUMMER_SOLSTICE -> R.string.sky_summer_solstice
    WheelEvent.LUGHNASADH -> R.string.sky_lughnasadh
    WheelEvent.AUTUMN_EQUINOX -> R.string.sky_autumn_equinox
    WheelEvent.SAMHAIN -> R.string.sky_samhain
    WheelEvent.WINTER_SOLSTICE -> R.string.sky_winter_solstice
}
