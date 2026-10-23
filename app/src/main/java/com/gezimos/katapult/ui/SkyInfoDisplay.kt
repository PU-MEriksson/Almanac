package com.gezimos.katapult.ui

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import kotlin.math.roundToInt

/**
 * The three sky lines under the date: moon phase, sunrise–sunset, next wheel-of-the-year
 * event. Same size and font as the status row above the clock.
 */
@Composable
fun SkyInfoLines(sky: SkyDay, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val timeFormat = DateFormat.getTimeFormat(context)

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
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
        SkyLine(icon = null) {
            when (next.daysUntil) {
                0 -> stringResource(R.string.sky_event_today, name)
                1 -> stringResource(R.string.sky_event_tomorrow, name)
                else -> stringResource(R.string.sky_event_in_days, name, next.daysUntil)
            }
        }
    }
}

@Composable
private fun SkyLine(icon: (@Composable () -> Unit)?, text: @Composable () -> String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(6.dp))
        }
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
        modifier = Modifier.size(18.dp),
    )
}

/**
 * The moon drawn as an outlined disc with its shadowed part filled in ink (like ◐), as seen
 * from the northern hemisphere: a waxing moon is lit on the right. Only solid ink, no grey,
 * so it stays crisp on e-ink.
 */
@Composable
fun MoonIcon(moon: MoonPhase, modifier: Modifier = Modifier.size(18.dp)) {
    val ink = LocalInk.current
    Canvas(modifier) {
        val strokeWidth = 1.5.dp.toPx()
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
