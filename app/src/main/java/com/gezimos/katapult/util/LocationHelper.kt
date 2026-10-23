package com.gezimos.katapult.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal

/**
 * One-off position fix for the sky info location. Uses Android's own LocationManager
 * (the Kompakt has no Google Play services) and never tracks location in the background.
 */
object LocationHelper {
    /** The user may grant only "approximate" location; that is plenty for sunrise times. */
    val PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    /** A last known position younger than this is used directly instead of waiting for a new fix. */
    private const val MAX_LAST_KNOWN_AGE_MS = 30 * 60 * 1000L

    fun hasPermission(context: Context): Boolean = PERMISSIONS.any {
        context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationEnabled(context: Context): Boolean =
        context.getSystemService(LocationManager::class.java)?.isLocationEnabled == true

    /**
     * Calls [onResult] on the main thread with one position, or null if none could be found
     * (Android gives up after about 30 s, e.g. indoors without GPS). Returns a signal to
     * cancel the request, or null if it already answered.
     */
    @SuppressLint("MissingPermission")
    fun requestSingleFix(context: Context, onResult: (Location?) -> Unit): CancellationSignal? {
        val manager = context.getSystemService(LocationManager::class.java)
        if (manager == null || !hasPermission(context)) {
            onResult(null)
            return null
        }
        val enabled = manager.getProviders(true)

        val recent = enabled
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .filter { System.currentTimeMillis() - it.time < MAX_LAST_KNOWN_AGE_MS }
            .maxByOrNull { it.time }
        if (recent != null) {
            onResult(recent)
            return null
        }

        val hasFine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        // GPS needs the precise-location permission; "fused" combines GPS and network where available.
        val candidates = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            if (hasFine) add(LocationManager.GPS_PROVIDER)
        }
        val provider = candidates.firstOrNull { it in enabled }
        if (provider == null) {
            onResult(null)
            return null
        }

        val signal = CancellationSignal()
        try {
            manager.getCurrentLocation(provider, signal, context.mainExecutor) { onResult(it) }
        } catch (_: SecurityException) {
            onResult(null)
            return null
        }
        return signal
    }
}
