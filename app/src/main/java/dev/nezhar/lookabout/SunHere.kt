package dev.nezhar.lookabout

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager

/** SharedPreferences file and key that remember the location question was asked. */
const val LOCATION_PREFS = "location"
const val LOCATION_ASKED_KEY = "asked"

/** True when the walker allowed coarse location. */
fun hasCoarseLocation(context: Context): Boolean =
    context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/**
 * The sun in words ([sunWords]) at the phone's last known location at [epochMillis], or null
 * without coarse location or a known location. Uses the network provider, then the passive one;
 * nothing new is requested. The location is used for this calculation only: it is not stored and
 * never leaves the phone.
 */
fun sunHere(context: Context, epochMillis: Long): String? {
    if (!hasCoarseLocation(context)) return null
    val manager = context.getSystemService(LocationManager::class.java) ?: return null
    val location = lastKnown(manager, LocationManager.NETWORK_PROVIDER)
        ?: lastKnown(manager, LocationManager.PASSIVE_PROVIDER)
        ?: return null
    return sunWords(sunPosition(location.latitude, location.longitude, epochMillis))
}

// Coarse location is checked in sunHere; a SecurityException is caught here anyway.
@SuppressLint("MissingPermission")
private fun lastKnown(manager: LocationManager, provider: String): Location? =
    try {
        manager.getLastKnownLocation(provider)
    } catch (e: SecurityException) {
        null
    } catch (e: IllegalArgumentException) {
        // A provider the phone does not have.
        null
    }
