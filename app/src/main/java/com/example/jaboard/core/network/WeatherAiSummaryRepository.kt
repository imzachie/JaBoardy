package com.example.jaboard.core.network

import android.util.Log
import com.example.jaboard.core.common.Formatters
import com.example.jaboard.core.common.TemperatureUnit
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class WeatherAiSummaryRepository {

  private val model: GenerativeModel by lazy {
    Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
  }

  suspend fun generateSummary(
    report: WeatherReport,
    unit: TemperatureUnit,
  ): String = withContext(Dispatchers.IO) {
    val tempFormatted = Formatters.formatTemperature(report.tempC, unit)
    val feelsLikeFormatted = Formatters.formatTemperature(report.feelsLikeC, unit)
    val maxTemp = report.daily.firstOrNull()?.maxTempC?.let { Formatters.formatTemperature(it, unit) } ?: tempFormatted
    val minTemp = report.daily.firstOrNull()?.minTempC?.let { Formatters.formatTemperature(it, unit) } ?: tempFormatted
    val rainProb = report.rainProbabilityNow ?: 0
    val uv = report.uvIndexNow?.let { String.format("%.1f", it) } ?: "Moderate"
    val wind = "${report.windSpeedKmh.toInt()} km/h"

    val prompt = """
      You are Pixel Weather's AI Weather Assistant.
      Create a concise, delightful, and natural 2 to 3 sentence weather summary for ${report.cityName}, ${report.country}.
      Weather details:
      - Current condition: ${report.description}
      - Current Temp: $tempFormatted (Feels like $feelsLikeFormatted)
      - Today's range: High $maxTemp, Low $minTemp
      - Rain probability: $rainProb%
      - Humidity: ${report.humidityPercent}%
      - Wind speed: $wind
      - UV index: $uv

      Tone: Conversational, helpful, and concise like Google Pixel Weather AI reports.
      Include a quick tip if relevant (e.g. umbrella, sunglasses, light jacket, or staying hydrated).
      Do not use bullet points or markdown headings. Keep it directly readable in 2-3 sentences.
    """.trimIndent()

    try {
      withTimeout(30_000L) {
        val response = model.generateContent(prompt)
        val text = response.text?.trim()
        if (!text.isNullOrBlank()) {
          text
        } else {
          fallbackSummary(report, unit)
        }
      }
    } catch (e: Exception) {
      Log.e("WeatherAiSummary", "Gemini generation failed, using intelligent fallback", e)
      fallbackSummary(report, unit)
    }
  }

  fun fallbackSummary(report: WeatherReport, unit: TemperatureUnit): String {
    val temp = Formatters.formatTemperature(report.tempC, unit)
    val rain = report.rainProbabilityNow ?: 0
    val desc = report.description.lowercase()

    val rainNote = when {
      rain >= 60 -> "High chance of rain ($rain%), so keep an umbrella close by."
      rain >= 30 -> "Possible scattered showers ($rain%) during the day."
      else -> "Low probability of precipitation."
    }

    val uvNote = when {
      (report.uvIndexNow ?: 0.0) >= 8.0 -> "UV index is very high—make sure to wear sunscreen."
      (report.uvIndexNow ?: 0.0) >= 5.0 -> "Moderate UV levels today."
      else -> "Pleasant conditions outside."
    }

    return "Currently $temp with $desc in ${report.cityName}. $rainNote $uvNote"
  }
}
