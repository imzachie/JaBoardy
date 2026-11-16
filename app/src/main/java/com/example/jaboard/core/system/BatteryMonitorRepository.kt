package com.example.jaboard.core.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

data class BatterySnapshot(
  val levelPercent: Int,
  val isCharging: Boolean,
  val statusLabel: String,
  val chargingSourceLabel: String,
  val temperatureCelsius: Float?,
  val voltageMilliVolts: Int?,
  val technology: String?,
  val healthLabel: String,
  val currentMicroAmps: Int?,
)

class BatteryMonitorRepository(private val context: Context) {

  fun observeBattery(): Flow<BatterySnapshot> =
    callbackFlow {
        fun emitSnapshot(intent: Intent?) {
          trySend(readSnapshot(intent))
        }

        val initialIntent =
          context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        emitSnapshot(initialIntent)

        val receiver =
          object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
              emitSnapshot(intent)
            }
          }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        awaitClose { runCatching { context.unregisterReceiver(receiver) } }
      }
      .distinctUntilChanged()

  fun readCurrentSnapshot(): BatterySnapshot {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    return readSnapshot(intent)
  }

  private fun readSnapshot(intent: Intent?): BatterySnapshot {
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val computedPct =
      if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt().coerceIn(0, 100)
      else bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0, 100) ?: 0

    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val isCharging =
      status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL

    val statusLabel =
      when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
        BatteryManager.BATTERY_STATUS_FULL -> "Full"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
        else -> "Unknown"
      }

    val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
    val sourceLabel =
      when (plugged) {
        BatteryManager.BATTERY_PLUGGED_AC -> "AC Adapter"
        BatteryManager.BATTERY_PLUGGED_USB -> "USB"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
        else ->
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && plugged == 8) "Dock"
          else if (plugged > 0) "External Power"
          else "On Battery"
      }

    val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
    val tempC =
      if (tempTenths != null && tempTenths != Int.MIN_VALUE && tempTenths > -500) {
        tempTenths / 10f
      } else {
        null
      }

    val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
    val voltageMv = if (voltage > 0) voltage else null

    val tech =
      intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)?.takeIf { it.isNotBlank() }

    val health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
    val healthLabel =
      when (health) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
        BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
        else -> "Not reported"
      }

    val currentNow =
      bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)?.takeIf {
        it != Int.MIN_VALUE && it != 0
      }

    return BatterySnapshot(
      levelPercent = computedPct,
      isCharging = isCharging,
      statusLabel = statusLabel,
      chargingSourceLabel = sourceLabel,
      temperatureCelsius = tempC,
      voltageMilliVolts = voltageMv,
      technology = tech,
      healthLabel = healthLabel,
      currentMicroAmps = currentNow,
    )
  }
}
