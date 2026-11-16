package com.example.jaboard.core.system

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.jaboard.core.datastore.SavedLocation
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class LocationRepository(private val context: Context) {

  fun hasCoarseLocationPermission(): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
      PackageManager.PERMISSION_GRANTED ||
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
  }

  @SuppressLint("MissingPermission")
  suspend fun resolveCurrentLocation(): Result<SavedLocation> =
    withContext(Dispatchers.IO) {
      if (!hasCoarseLocationPermission()) {
        return@withContext Result.failure(SecurityException("Location permission not granted"))
      }
      val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
      val location =
        getLastKnownBest(lm)
          ?: withTimeoutOrNull(8_000L) { requestFreshFix(lm) }
          ?: return@withContext Result.failure(
            IllegalStateException("Unable to obtain a device location fix. Try searching for your city manually.")
          )

      val (city, country) = reverseGeocode(location.latitude, location.longitude)
      val key = String.format(Locale.US, "%.2f_%.2f", location.latitude, location.longitude)
      Result.success(
        SavedLocation(
          key = key,
          cityName = city,
          country = country,
          latitude = location.latitude,
          longitude = location.longitude,
        )
      )
    }

  @SuppressLint("MissingPermission")
  private fun getLastKnownBest(lm: LocationManager): Location? {
    val providers =
      listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
    return providers
      .mapNotNull { provider ->
        runCatching {
          if (lm.isProviderEnabled(provider)) lm.getLastKnownLocation(provider) else null
        }.getOrNull()
      }
      .maxByOrNull { it.time }
  }

  @SuppressLint("MissingPermission")
  private suspend fun requestFreshFix(lm: LocationManager): Location? {
    val provider =
      when {
        lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        else -> return null
      }

    return suspendCancellableCoroutine { cont ->
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val signal = CancellationSignal()
        cont.invokeOnCancellation { signal.cancel() }
        runCatching {
          lm.getCurrentLocation(provider, signal, context.mainExecutor) { loc ->
            if (cont.isActive) cont.resume(loc)
          }
        }.onFailure { if (cont.isActive) cont.resume(null) }
      } else {
        @Suppress("DEPRECATION")
        val listener =
          object : LocationListener {
            override fun onLocationChanged(location: Location) {
              if (cont.isActive) cont.resume(location)
              runCatching { lm.removeUpdates(this) }
            }
          }
        cont.invokeOnCancellation { runCatching { lm.removeUpdates(listener) } }
        @Suppress("DEPRECATION")
        runCatching { lm.requestSingleUpdate(provider, listener, Looper.getMainLooper()) }
          .onFailure { if (cont.isActive) cont.resume(null) }
      }
    }
  }

  @Suppress("DEPRECATION")
  private fun reverseGeocode(lat: Double, lon: Double): Pair<String, String> {
    return runCatching {
      if (!Geocoder.isPresent()) return@runCatching defaultCityLabel(lat, lon)
      val geocoder = Geocoder(context, Locale.getDefault())
      val addresses = geocoder.getFromLocation(lat, lon, 1)
      val addr = addresses?.firstOrNull()
      val city =
        addr?.locality
          ?: addr?.subAdminArea
          ?: addr?.adminArea
          ?: String.format(Locale.US, "%.2f, %.2f", lat, lon)
      val country = addr?.countryName.orEmpty()
      city to country
    }.getOrElse { defaultCityLabel(lat, lon) }
  }

  private fun defaultCityLabel(lat: Double, lon: Double): Pair<String, String> =
    String.format(Locale.US, "Current Area (%.2f, %.2f)", lat, lon) to ""

  @SuppressLint("MissingPermission")
  fun observeLocation(): kotlinx.coroutines.flow.Flow<Location?> = channelFlow {
    if (!hasCoarseLocationPermission()) {
      close()
      return@channelFlow
    }

    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = mutableListOf<String>()

    if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) providers.add(LocationManager.GPS_PROVIDER)
    if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) providers.add(LocationManager.NETWORK_PROVIDER)
    if (providers.isEmpty()) {
      close()
      return@channelFlow
    }

    val locationListener = object : LocationListener {
      override fun onLocationChanged(location: Location) {
        trySend(location)
      }

      override fun onProviderEnabled(provider: String) {}
      override fun onProviderDisabled(provider: String) {}
      override fun onStatusChanged(provider: String, status: Int, extras: android.os.Bundle?) {}
    }

    val callback = object : android.location.LocationListener {
      override fun onLocationChanged(location: Location) {
        trySend(location)
      }

      override fun onProviderEnabled(provider: String) {}
      override fun onProviderDisabled(provider: String) {}
      override fun onStatusChanged(provider: String, status: Int, extras: android.os.Bundle?) {}
    }

    providers.forEach { provider ->
      try {
        // Use the deprecated API which works on all Android versions
        // For GPS provider, use 0 distance to get bearing updates even when stationary
        val minDistance = if (provider == LocationManager.GPS_PROVIDER) 0f else 1f
        @Suppress("DEPRECATION")
        lm.requestLocationUpdates(provider, 1000L, minDistance, locationListener, Looper.getMainLooper())
      } catch (e: SecurityException) {
        // Permission may have been revoked
      }
    }

    // Emit last known location immediately
    getLastKnownBest(lm)?.let { trySend(it) }

    awaitClose {
      providers.forEach { provider ->
        runCatching {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            lm.removeUpdates(callback)
          } else {
            @Suppress("DEPRECATION")
            lm.removeUpdates(locationListener)
          }
        }
      }
    }
  }
}
