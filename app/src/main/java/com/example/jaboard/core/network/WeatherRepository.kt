package com.example.jaboard.core.network

import com.example.jaboard.core.common.Formatters
import com.example.jaboard.core.database.WeatherCacheDao
import com.example.jaboard.core.database.WeatherCacheEntity
import com.example.jaboard.core.datastore.SavedLocation
import com.squareup.moshi.Moshi
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

data class HourlyForecastItem(
  val isoTime: String,
  val label: String,
  val tempC: Double,
  val rainProbability: Int,
  val weatherCode: Int,
  val uvIndex: Double = 0.0,
)

data class DailyForecastItem(
  val isoDate: String,
  val dayLabel: String,
  val weatherCode: Int,
  val maxTempC: Double,
  val minTempC: Double,
  val rainProbability: Int,
  val uvIndexMax: Double = 0.0,
  val sunrise: String? = null,
  val sunset: String? = null,
)

data class WeatherReport(
  val locationKey: String,
  val cityName: String,
  val country: String,
  val tempC: Double,
  val feelsLikeC: Double,
  val humidityPercent: Int,
  val windSpeedKmh: Double,
  val weatherCode: Int,
  val description: String,
  val isDay: Boolean,
  val rainProbabilityNow: Int?,
  val uvIndexNow: Double? = null,
  val surfacePressureHpa: Double? = null,
  val visibilityKm: Double? = null,
  val windDirectionDeg: Int? = null,
  val sunriseToday: String? = null,
  val sunsetToday: String? = null,
  val hourly: List<HourlyForecastItem>,
  val daily: List<DailyForecastItem>,
  val fetchedAt: Long,
)

sealed interface WeatherFetchResult {
  data class Success(val report: WeatherReport) : WeatherFetchResult

  data class Failure(val message: String, val cachedReport: WeatherReport?) : WeatherFetchResult
}

class WeatherRepository(private val weatherCacheDao: WeatherCacheDao) {

  private val moshi: Moshi = Moshi.Builder().build()
  private val forecastAdapter = moshi.adapter(OpenMeteoForecastResponse::class.java)

  private val okHttpClient: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(12, TimeUnit.SECONDS)
      .readTimeout(12, TimeUnit.SECONDS)
      .build()

  private val forecastApi: OpenMeteoForecastApi =
    Retrofit.Builder()
      .baseUrl("https://api.open-meteo.com/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(OpenMeteoForecastApi::class.java)

  private val geocodingApi: OpenMeteoGeocodingApi =
    Retrofit.Builder()
      .baseUrl("https://geocoding-api.open-meteo.com/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(OpenMeteoGeocodingApi::class.java)

  fun observeCachedWeather(locationKey: String): Flow<WeatherReport?> =
    weatherCacheDao.observeWeather(locationKey).map { entity ->
      entity?.let { parseEntity(it) }
    }

  suspend fun searchCities(query: String): Result<List<GeocodingPlaceDto>> =
    withContext(Dispatchers.IO) {
      runCatching {
        if (query.trim().length < 2) emptyList()
        else geocodingApi.searchCity(query.trim()).results.orEmpty()
      }
    }

  suspend fun refreshWeather(location: SavedLocation): WeatherFetchResult =
    withContext(Dispatchers.IO) {
      val cached = weatherCacheDao.getWeather(location.key)?.let { parseEntity(it) }
      try {
        val response =
          forecastApi.getForecast(latitude = location.latitude, longitude = location.longitude)
        val now = System.currentTimeMillis()
        val json = forecastAdapter.toJson(response)
        val entity =
          WeatherCacheEntity(
            locationKey = location.key,
            cityName = location.cityName,
            country = location.country,
            latitude = location.latitude,
            longitude = location.longitude,
            payloadJson = json,
            fetchedAt = now,
          )
        weatherCacheDao.upsertWeather(entity)
        val report = mapToReport(entity, response)
        WeatherFetchResult.Success(report)
      } catch (e: Exception) {
        val reason =
          when {
            e.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
              "No internet connection. Showing last saved forecast if available."
            else -> e.localizedMessage ?: "Unable to fetch forecast right now."
          }
        WeatherFetchResult.Failure(reason, cached)
      }
    }

  suspend fun clearCache() {
    weatherCacheDao.clearAll()
  }

  private fun parseEntity(entity: WeatherCacheEntity): WeatherReport? =
    runCatching {
      val response = forecastAdapter.fromJson(entity.payloadJson) ?: return null
      mapToReport(entity, response)
    }.getOrNull()

  private fun mapToReport(
    entity: WeatherCacheEntity,
    response: OpenMeteoForecastResponse,
  ): WeatherReport {
    val current = response.current
    val currentCode = current?.weatherCode ?: 0
    val currentIsoHour = current?.time?.take(13)

    val hourlyDto = response.hourly
    val hourlyItems = mutableListOf<HourlyForecastItem>()
    if (hourlyDto != null) {
      val startIndex =
        if (currentIsoHour != null) {
          hourlyDto.time.indexOfFirst { it.startsWith(currentIsoHour) }.coerceAtLeast(0)
        } else {
          0
        }
      val endIndex = (startIndex + 24).coerceAtMost(hourlyDto.time.size)
      for (i in startIndex until endIndex) {
        val timeStr = hourlyDto.time[i]
        val hourPart = timeStr.substringAfter("T", "00:00").take(5)
        hourlyItems.add(
          HourlyForecastItem(
            isoTime = timeStr,
            label = if (i == startIndex) "Now" else hourPart,
            tempC = hourlyDto.temperature2m.getOrNull(i) ?: 0.0,
            rainProbability = hourlyDto.precipitationProbability.getOrNull(i) ?: 0,
            weatherCode = hourlyDto.weatherCode.getOrNull(i) ?: currentCode,
            uvIndex = hourlyDto.uvIndex.getOrNull(i) ?: 0.0,
          )
        )
      }
    }

    val dailyDto = response.daily
    val dailyItems = mutableListOf<DailyForecastItem>()
    if (dailyDto != null) {
      for (i in dailyDto.time.indices) {
        val dateStr = dailyDto.time[i]
        dailyItems.add(
          DailyForecastItem(
            isoDate = dateStr,
            dayLabel = if (i == 0) "Today" else formatDayName(dateStr),
            weatherCode = dailyDto.weatherCode.getOrNull(i) ?: currentCode,
            maxTempC = dailyDto.temperature2mMax.getOrNull(i) ?: 0.0,
            minTempC = dailyDto.temperature2mMin.getOrNull(i) ?: 0.0,
            rainProbability = dailyDto.precipitationProbabilityMax.getOrNull(i) ?: 0,
            uvIndexMax = dailyDto.uvIndexMax.getOrNull(i) ?: 0.0,
            sunrise = dailyDto.sunrise.getOrNull(i)?.substringAfter("T", "")?.take(5),
            sunset = dailyDto.sunset.getOrNull(i)?.substringAfter("T", "")?.take(5),
          )
        )
      }
    }

    val visibilityKm = current?.visibility?.let { it / 1000.0 }

    return WeatherReport(
      locationKey = entity.locationKey,
      cityName = entity.cityName,
      country = entity.country,
      tempC = current?.temperature2m ?: 0.0,
      feelsLikeC = current?.apparentTemperature ?: (current?.temperature2m ?: 0.0),
      humidityPercent = current?.relativeHumidity2m ?: 0,
      windSpeedKmh = current?.windSpeed10m ?: 0.0,
      weatherCode = currentCode,
      description = Formatters.wmoCodeDescription(currentCode),
      isDay = (current?.isDay ?: 1) == 1,
      rainProbabilityNow = hourlyItems.firstOrNull()?.rainProbability,
      uvIndexNow = current?.uvIndex ?: dailyItems.firstOrNull()?.uvIndexMax,
      surfacePressureHpa = current?.surfacePressure,
      visibilityKm = visibilityKm,
      windDirectionDeg = current?.windDirection10m,
      sunriseToday = dailyItems.firstOrNull()?.sunrise,
      sunsetToday = dailyItems.firstOrNull()?.sunset,
      hourly = hourlyItems,
      daily = dailyItems,
      fetchedAt = entity.fetchedAt,
    )
  }

  private fun formatDayName(isoDate: String): String {
    return runCatching {
      val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
      val date = sdf.parse(isoDate) ?: return isoDate
      java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()).format(date)
    }.getOrDefault(isoDate)
  }
}
