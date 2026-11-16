package com.example.jaboard.feature.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CompassViewModel(private val context: Context) : ViewModel() {

  private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
  private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
  private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

  private val _heading = MutableStateFlow(0f)
  val heading: StateFlow<Float> = _heading.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = 0f,
  )

  private val _accuracy = MutableStateFlow(0f)
  val accuracy: StateFlow<Float> = _accuracy.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = 0f,
  )

  private val _gpsStatus = MutableStateFlow("Initializing...")
  val gpsStatus: StateFlow<String> = _gpsStatus.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = "Initializing...",
  )

  private val _hasPermission = MutableStateFlow(true)
  val hasPermission: StateFlow<Boolean> = _hasPermission.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = true,
  )

  private var sensorListener: SensorEventListener? = null

  init {
    startSensors()
  }

  private fun startSensors() {
    if (accelerometer == null || magnetometer == null) {
      _gpsStatus.value = "Sensors unavailable"
      return
    }

    sensorListener = object : SensorEventListener {
      private val accelerometerValues = FloatArray(3)
      private val magnetometerValues = FloatArray(3)

      override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
          Sensor.TYPE_ACCELEROMETER -> {
            System.arraycopy(event.values, 0, accelerometerValues, 0, 3)
          }
          Sensor.TYPE_MAGNETIC_FIELD -> {
            System.arraycopy(event.values, 0, magnetometerValues, 0, 3)
          }
        }

        // Calculate orientation when we have both
        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerValues, magnetometerValues)) {
          SensorManager.getOrientation(rotationMatrix, orientation)
          val headingRad = orientation[0] // azimuth
          val headingDeg = (Math.toDegrees(headingRad.toDouble()) + 360) % 360
          _heading.value = headingDeg.toFloat()
          _accuracy.value = event.accuracy.toFloat()
          _gpsStatus.value = "Compass Active"
        }
      }

      override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        _accuracy.value = accuracy.toFloat()
        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
          _gpsStatus.value = "Calibrate: move in figure-8"
        } else if (accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {
          _gpsStatus.value = "Compass Active"
        }
      }
    }

    sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
    sensorManager.registerListener(sensorListener, magnetometer, SensorManager.SENSOR_DELAY_UI)
    _gpsStatus.value = "Compass Active"
  }

  override fun onCleared() {
    super.onCleared()
    sensorListener?.let { sensorManager.unregisterListener(it) }
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