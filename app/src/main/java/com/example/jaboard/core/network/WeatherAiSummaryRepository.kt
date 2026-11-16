package com.example.jaboard.core.network

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.example.jaboard.core.common.Formatters
import com.example.jaboard.core.common.TemperatureUnit
import com.example.jaboard.core.database.JaBoardDatabase
import com.example.jaboard.core.database.WeatherAiSummaryEntity
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileInputStream
import java.util.Properties
import java.util.concurrent.TimeUnit

class WeatherAiSummaryRepository(private val context: Context) {

  private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()
  private val requestAdapter: JsonAdapter<GroqChatRequest> = moshi.adapter(GroqChatRequest::class.java)
  private val responseAdapter: JsonAdapter<GroqChatResponse> = moshi.adapter(GroqChatResponse::class.java)

  private val okHttpClient: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.SECONDS)
      .readTimeout(15, TimeUnit.SECONDS)
      .build()

  private val database: JaBoardDatabase = JaBoardDatabase.getInstance(context)

  companion object {
    private const val TAG = "WeatherAiSummary"
    private const val GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
    private const val GROQ_MODEL = "llama-3.1-8b-instant"
    private const val CACHE_TTL_MINUTES = 60L
    val SYSTEM_PROMPT = """
      You are Pixel Weather's AI assistant. Create a concise, natural 2-sentence weather summary.
      Tone: Conversational, helpful, concise like Google Pixel Weather.
      Include one quick tip if relevant (umbrella, sunglasses, light jacket, hydration).
      No bullet points, no markdown. Just 2 readable sentences.
    """.trimIndent()
  }

  private fun loadGroqApiKey(): String {
    val props = Properties()
    // Try loading from assets first (for debug builds where local.properties is copied)
    try {
      context.assets.open("local.properties").use { input ->
        props.load(input)
      }
    } catch (e: Exception) {
      // Fallback: try reading from project root (for debug builds)
      try {
        val file = File(context.filesDir.parentFile?.parentFile, "local.properties")
        if (file.exists()) {
          FileInputStream(file).use { props.load(it) }
        }
      } catch (e2: Exception) {
        Log.w(TAG, "Could not load local.properties for Groq API key", e2)
      }
    }
    return props.getProperty("GROQ_API_KEY_INTERNAL") ?: ""
  }

  suspend fun generateSummary(
    report: WeatherReport,
    unit: TemperatureUnit,
  ): String = withContext(Dispatchers.IO) {
    val locationKey = report.locationKey
    val now = System.currentTimeMillis()

    // Check cache first
    val cached = database.weatherAiSummaryDao().getSummary(locationKey)
    if (cached != null && (now - cached.generatedAt) < CACHE_TTL_MINUTES * 60 * 1000) {
      Log.d(TAG, "Returning cached AI summary for $locationKey")
      return@withContext cached.summaryText
    }

    val apiKey = loadGroqApiKey()
    if (apiKey.isBlank()) {
      Log.w(TAG, "GROQ_API_KEY not configured in local.properties, using fallback")
      return@withContext fallbackSummary(report, unit)
    }

    val trimmedPayload = buildTrimmedPayload(report, unit)
    val request = GroqChatRequest(
      model = GROQ_MODEL,
      messages = listOf(
        GroqMessage(role = "system", content = SYSTEM_PROMPT),
        GroqMessage(role = "user", content = trimmedPayload),
      ),
      temperature = 0.3f,
      maxTokens = 120,
    )

    val jsonBody = requestAdapter.toJson(request) ?: return@withContext fallbackSummary(report, unit)
    val httpRequest = Request.Builder()
      .url(GROQ_ENDPOINT)
      .header("Authorization", "Bearer $apiKey")
      .header("Content-Type", "application/json")
      .post(jsonBody.toRequestBody("application/json".toMediaType()))
      .build()

    try {
      val response = okHttpClient.newCall(httpRequest).execute()
      if (!response.isSuccessful) {
        Log.e(TAG, "Groq API error: ${response.code} ${response.body?.string()}")
        return@withContext fallbackSummary(report, unit)
      }

      val responseBody = response.body?.string() ?: return@withContext fallbackSummary(report, unit)
      val groqResponse = responseAdapter.fromJson(responseBody)

      val summary = groqResponse?.choices?.firstOrNull()?.message?.content?.trim()
        .takeIf { it != null && it.isNotBlank() }
        ?: return@withContext fallbackSummary(report, unit)

      // Cache the result
      database.weatherAiSummaryDao().insertSummary(
        WeatherAiSummaryEntity(
          locationKey = locationKey,
          summaryText = summary,
          generatedAt = now,
        )
      )
      Log.d(TAG, "Generated and cached AI summary for $locationKey")
      summary
    } catch (e: Exception) {
      Log.e(TAG, "Groq generation failed, using fallback", e)
      fallbackSummary(report, unit)
    }
  }

  private fun buildTrimmedPayload(report: WeatherReport, unit: TemperatureUnit): String {
    val today = report.daily.firstOrNull()
    val temp = Formatters.formatTemperature(report.tempC, unit)
    val feelsLike = Formatters.formatTemperature(report.feelsLikeC, unit)
    val high = today?.maxTempC?.let { Formatters.formatTemperature(it, unit) } ?: "N/A"
    val low = today?.minTempC?.let { Formatters.formatTemperature(it, unit) } ?: "N/A"
    val rainProb = report.rainProbabilityNow ?: 0
    val uv = report.uvIndexNow?.let { String.format("%.1f", it) } ?: "Moderate"
    val humidity = report.humidityPercent
    val wind = "${report.windSpeedKmh.toInt()} km/h"
    val condition = report.description
    val city = "${report.cityName}, ${report.country}"

    return """
      City: $city
      Now: $condition, $temp (feels $feelsLike)
      Today: High $high / Low $low
      Rain: $rainProb%, Humidity: $humidity%, UV: $uv
      Wind: $wind
    """.trimIndent()
  }

  fun fallbackSummary(report: WeatherReport, unit: TemperatureUnit): String {
    val temp = Formatters.formatTemperature(report.tempC, unit)
    val rain = report.rainProbabilityNow ?: 0
    val desc = report.description.lowercase()

    val rainNote = when {
      rain >= 60 -> "High chance of rain ($rain%), keep an umbrella close."
      rain >= 30 -> "Possible scattered showers ($rain%)."
      else -> "Low chance of precipitation."
    }

    val uvNote = when {
      (report.uvIndexNow ?: 0.0) >= 8.0 -> "UV index very high—wear sunscreen."
      (report.uvIndexNow ?: 0.0) >= 5.0 -> "Moderate UV levels today."
      else -> "Pleasant conditions outside."
    }

    return "Currently $temp with $desc in ${report.cityName}. $rainNote $uvNote"
  }

  suspend fun clearCache() {
    database.weatherAiSummaryDao().clearAll()
  }
}

data class GroqChatRequest(
  val model: String,
  val messages: List<GroqMessage>,
  val temperature: Float,
  val maxTokens: Int,
)

data class GroqMessage(
  val role: String,
  val content: String,
)

data class GroqChatResponse(
  val choices: List<GroqChoice>?,
)

data class GroqChoice(
  val message: GroqMessage?,
  val finishReason: String?,
)