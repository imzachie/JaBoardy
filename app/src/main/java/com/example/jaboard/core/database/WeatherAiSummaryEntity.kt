package com.example.jaboard.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_ai_summary")
data class WeatherAiSummaryEntity(
  @PrimaryKey val locationKey: String,
  val summaryText: String,
  val generatedAt: Long,
)