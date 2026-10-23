package com.gezimos.katapult.ui

import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gezimos.katapult.R
import com.gezimos.katapult.util.Coordinates
import com.gezimos.katapult.util.LocationHelper

/**
 * Edits the sky info location: two number fields plus a "Use my current location" button
 * that asks for location permission (only then) and fills the fields from one position fix.
 */
@Composable
fun SkyLocationSheet(
    latitude: Double?,
    longitude: Double?,
    onDismiss: () -> Unit,
    onSave: (latitude: Double, longitude: Double) -> Unit,
) {
    val context = LocalContext.current
    var latitudeText by remember { mutableStateOf(latitude?.let(Coordinates::format).orEmpty()) }
    var longitudeText by remember { mutableStateOf(longitude?.let(Coordinates::format).orEmpty()) }
    var status by remember { mutableStateOf<String?>(null) }
    var locating by remember { mutableStateOf(false) }
    var pendingFix by remember { mutableStateOf<CancellationSignal?>(null) }

    DisposableEffect(Unit) {
        onDispose { pendingFix?.cancel() }
    }

    fun startFix() {
        if (!LocationHelper.isLocationEnabled(context)) {
            status = context.getString(R.string.sky_location_off)
            return
        }
        locating = true
        status = context.getString(R.string.sky_locating)
        pendingFix = LocationHelper.requestSingleFix(context) { location ->
            locating = false
            pendingFix = null
            if (location == null) {
                status = context.getString(R.string.sky_location_failed)
            } else {
                latitudeText = Coordinates.format(Coordinates.roundForStorage(location.latitude))
                longitudeText = Coordinates.format(Coordinates.roundForStorage(location.longitude))
                status = null
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { it }) startFix()
        else status = context.getString(R.string.sky_location_denied)
    }

    BottomSheet(onDismiss = onDismiss, imePadding = true) {
        Text(
            text = stringResource(R.string.sky_location),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = LatoFamily,
            color = LocalInk.current,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        CoordinateField(
            label = stringResource(R.string.sky_latitude),
            value = latitudeText,
            onValueChange = { latitudeText = it; status = null },
        )
        Spacer(Modifier.height(12.dp))
        CoordinateField(
            label = stringResource(R.string.sky_longitude),
            value = longitudeText,
            onValueChange = { longitudeText = it; status = null },
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.sky_use_current_location),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = LatoFamily,
            color = LocalInk.current,
            modifier = Modifier
                .border(2.5.dp, LocalInk.current, RoundedCornerShape(10.dp))
                .clickable(enabled = !locating) {
                    if (LocationHelper.hasPermission(context)) startFix()
                    else permissionLauncher.launch(LocationHelper.PERMISSIONS)
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        status?.let {
            Text(
                text = it,
                fontSize = 14.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                text = stringResource(R.string.cancel),
                fontSize = 18.sp,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(12.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.save),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LatoFamily,
                color = LocalInk.current,
                modifier = Modifier
                    .clickable {
                        val lat = Coordinates.parse(latitudeText, Coordinates.MAX_LATITUDE)
                        val lon = Coordinates.parse(longitudeText, Coordinates.MAX_LONGITUDE)
                        if (lat != null && lon != null) onSave(lat, lon)
                        else status = context.getString(R.string.sky_location_invalid)
                    }
                    .padding(12.dp),
            )
        }
    }
}

@Composable
private fun CoordinateField(label: String, value: String, onValueChange: (String) -> Unit) {
    Text(
        text = label,
        fontSize = 14.sp,
        fontFamily = LatoFamily,
        color = LocalInk.current,
        modifier = Modifier.padding(bottom = 4.dp),
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = TextStyle(fontSize = 18.sp, fontFamily = LatoFamily, color = LocalInk.current),
        cursorBrush = SolidColor(LocalInk.current),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.5.dp, LocalInk.current)
            .padding(12.dp),
    )
}
