package com.example.jaboard.core.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class OpenMeteoForecastResponse(
  val latitude: Double,
  val longitude: Double,
  val timezone: String? = null,
  val current: CurrentWeatherDto? = null,
  val hourly: HourlyWeatherDto? = null,
  val daily: DailyWeatherDto? = null,
)

@JsonClass(generateAdapter = true)
data class CurrentWeatherDto(
  val time: String? = null,
  @Json(name = "temperature_2m") val temperature2m: Double? = null,
  @Json(name = "relative_humidity_2m") val relativeHumidity2m: Int? = null,
  @Json(name = "apparent_temperature") val apparentTemperature: Double? = null,
  @Json(name = "is_day") val isDay: Int? = null,
  @Json(name = "weather_code") val weatherCode: Int? = null,
  @Json(name = "wind_speed_10m") val windSpeed10m: Double? = null,
  @Json(name = "wind_direction_10m") val windDirection10m: Int? = null,
  @Json(name = "surface_pressure") val surfacePressure: Double? = null,
  @Json(name = "uv_index") val uvIndex: Double? = null,
  @Json(name = "visibility") val visibility: Double? = null,
)

@JsonClass(generateAdapter = true)
data class HourlyWeatherDto(
  val time: List<String> = emptyList(),
  @Json(name = "temperature_2m") val temperature2m: List<Double?> = emptyList(),
  @Json(name = "precipitation_probability") val precipitationProbability: List<Int?> = emptyList(),
  @Json(name = "weather_code") val weatherCode: List<Int?> = emptyList(),
  @Json(name = "uv_index") val uvIndex: List<Double?> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class DailyWeatherDto(
  val time: List<String> = emptyList(),
  @Json(name = "weather_code") val weatherCode: List<Int?> = emptyList(),
  @Json(name = "temperature_2m_max") val temperature2mMax: List<Double?> = emptyList(),
  @Json(name = "temperature_2m_min") val temperature2mMin: List<Double?> = emptyList(),
  @Json(name = "precipitation_probability_max")
  val precipitationProbabilityMax: List<Int?> = emptyList(),
  @Json(name = "uv_index_max") val uvIndexMax: List<Double?> = emptyList(),
  val sunrise: List<String?> = emptyList(),
  val sunset: List<String?> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
  val results: List<GeocodingPlaceDto>? = null,
)

@JsonClass(generateAdapter = true)
data class GeocodingPlaceDto(
  val id: Long = 0L,
  val name: String,
  val latitude: Double,
  val longitude: Double,
  val country: String? = null,
  val admin1: String? = null,
)

interface OpenMeteoForecastApi {
  @GET("v1/forecast")
  suspend fun getForecast(
    @Query("latitude") latitude: Double,
    @Query("longitude") longitude: Double,
    @Query("current")
    current: String =
      "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m,wind_direction_10m,surface_pressure,uv_index,visibility",
    @Query("hourly")
    hourly: String = "temperature_2m,precipitation_probability,weather_code,uv_index",
    @Query("daily")
    daily: String =
      "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,uv_index_max,sunrise,sunset",
    @Query("timezone") timezone: String = "auto",
    @Query("forecast_days") forecastDays: Int = 10,
  ): OpenMeteoForecastResponse
}

interface OpenMeteoGeocodingApi {
  @GET("v1/search")
  suspend fun searchCity(
    @Query("name") query: String,
    @Query("count") count: Int = 10,
    @Query("language") language: String = "en",
    @Query("format") format: String = "json",
  ): GeocodingResponse
}
