package com.example.jaboard.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
  @PrimaryKey val locationKey: String,
  val cityName: String,
  val country: String,
  val latitude: Double,
  val longitude: Double,
  val payloadJson: String,
  val fetchedAt: Long,
)
