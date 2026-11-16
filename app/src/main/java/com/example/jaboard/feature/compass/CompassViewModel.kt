package com.example.jaboard.feature.compass

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jaboard.core.system.LocationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CompassViewModel(private val locationRepository: LocationRepository) : ViewModel() {

  private val _heading = MutableStateFlow(0f)
  val heading: StateFlow<Float> = _heading.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = 0f,
  )

  private val _altitude = MutableStateFlow(0.0)
  val altitude: StateFlow<Double> = _altitude.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = 0.0,
  )

  private val _accuracy = MutableStateFlow(0f)
  val accuracy: StateFlow<Float> = _accuracy.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = 0f,
  )

  private val _speed = MutableStateFlow(0f)
  val speed: StateFlow<Float> = _speed.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = 0f,
  )

  private val _latitude = MutableStateFlow(0.0)
  val latitude: StateFlow<Double> = _latitude.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = 0.0,
  )

  private val _longitude = MutableStateFlow(0.0)
  val longitude: StateFlow<Double> = _longitude.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = 0.0,
  )

  private val _gpsStatus = MutableStateFlow("Searching...")
  val gpsStatus: StateFlow<String> = _gpsStatus.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = "Searching...",
  )

  private val _hasPermission = MutableStateFlow(false)
  val hasPermission: StateFlow<Boolean> = _hasPermission.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = false,
  )

  init {
    startLocationUpdates()
  }

  private fun startLocationUpdates() {
    viewModelScope.launch {
      _hasPermission.value = locationRepository.hasCoarseLocationPermission()
      if (!_hasPermission.value) {
        _gpsStatus.value = "Permission required"
        return@launch
      }

      locationRepository.observeLocation().collect { location ->
        if (location != null) {
          _latitude.value = location.latitude
          _longitude.value = location.longitude
          _altitude.value = location.altitude ?: 0.0
          _speed.value = location.speed ?: 0f
          _accuracy.value = location.accuracy

          if (location.hasBearing()) {
            _heading.value = location.bearing
          }

          _gpsStatus.value = "GPS Active"
        } else {
          _gpsStatus.value = "Searching..."
        }
      }
    }
  }

  fun refreshPermission() {
    _hasPermission.value = locationRepository.hasCoarseLocationPermission()
    if (_hasPermission.value) {
      startLocationUpdates()
    }
  }

  fun getCardinalDirection(degrees: Float): String {
    val normalized = (degrees % 360f + 360f) % 360f
    return when {
      normalized >= 337.5f || normalized < 22.5f -> "N"
      normalized < 67.5f -> "NE"
      normalized < 112.5f -> "E"
      normalized < 157.5f -> "SE"
      normalized < 202.5f -> "S"
      normalized < 247.5f -> "SW"
      normalized < 292.5f -> "W"
      else -> "NW"
    }
  }

  fun getCardinalName(degrees: Float): String {
    val normalized = (degrees % 360f + 360f) % 360f
    return when {
      normalized >= 337.5f || normalized < 22.5f -> "North"
      normalized < 67.5f -> "North-East"
      normalized < 112.5f -> "East"
      normalized < 157.5f -> "South-East"
      normalized < 202.5f -> "South"
      normalized < 247.5f -> "South-West"
      normalized < 292.5f -> "West"
      else -> "North-West"
    }
  }
}