package com.example.jaboard.core.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class TemperatureUnit {
  CELSIUS,
  FAHRENHEIT,
}

object Formatters {
  fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    val tb = gb * 1024.0
    return when {
      bytes >= tb -> String.format(Locale.getDefault(), "%.2f TB", bytes / tb)
      bytes >= gb -> String.format(Locale.getDefault(), "%.1f GB", bytes / gb)
      bytes >= mb -> String.format(Locale.getDefault(), "%.0f MB", bytes / mb)
      bytes >= kb -> String.format(Locale.getDefault(), "%.0f KB", bytes / kb)
      else -> "$bytes B"
    }
  }

  fun formatTemperature(celsius: Double, unit: TemperatureUnit): String {
    val value =
      if (unit == TemperatureUnit.FAHRENHEIT) {
        celsius * 9.0 / 5.0 + 32.0
      } else {
        celsius
      }
    val suffix = if (unit == TemperatureUnit.FAHRENHEIT) "°F" else "°C"
    return "${value.roundToInt()}$suffix"
  }

  fun formatShortTemp(celsius: Double, unit: TemperatureUnit): String {
    val value =
      if (unit == TemperatureUnit.FAHRENHEIT) {
        celsius * 9.0 / 5.0 + 32.0
      } else {
        celsius
      }
    return "${value.roundToInt()}°"
  }

  fun formatMediaTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
  }

  fun formatStopwatch(ms: Long): String {
    val safeMs = ms.coerceAtLeast(0L)
    val minutes = (safeMs / 60_000L)
    val seconds = (safeMs % 60_000L) / 1000L
    val centiseconds = (safeMs % 1000L) / 10L
    return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, centiseconds)
  }

  fun formatTimer(ms: Long): String {
    val totalSeconds = ((ms.coerceAtLeast(0L) + 999L) / 1000L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
      String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
      String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
  }

  fun formatClockTime(
    epochMillis: Long,
    use24Hour: Boolean,
    showSeconds: Boolean,
    showMillis: Boolean,
    locale: Locale = Locale.getDefault(),
  ): String {
    val pattern = buildString {
      append(if (use24Hour) "HH:mm" else "h:mm")
      if (showSeconds) append(":ss")
      if (showMillis) append(".SSS")
      if (!use24Hour) append(" a")
    }
    return SimpleDateFormat(pattern, locale).format(Date(epochMillis))
  }

  fun formatRelativeTimestamp(epochMillis: Long): String {
    val diff = System.currentTimeMillis() - epochMillis
    val mins = diff / 60_000L
    return when {
      mins < 1L -> "Just now"
      mins < 60L -> "${mins}m ago"
      mins < 1440L -> "${mins / 60L}h ago"
      else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMillis))
    }
  }

  fun wmoCodeDescription(code: Int): String =
    when (code) {
      0 -> "Clear sky"
      1 -> "Mainly clear"
      2 -> "Partly cloudy"
      3 -> "Overcast"
      45,
      48 -> "Foggy"
      51,
      53,
      55 -> "Drizzle"
      56,
      57 -> "Freezing drizzle"
      61,
      63,
      65 -> "Rain"
      66,
      67 -> "Freezing rain"
      71,
      73,
      75 -> "Snowfall"
      77 -> "Snow grains"
      80,
      81,
      82 -> "Rain showers"
      85,
      86 -> "Snow showers"
      95 -> "Thunderstorm"
      96,
      99 -> "Severe thunderstorm"
      else -> "Conditions unavailable"
    }
}
