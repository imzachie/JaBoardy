package com.example.jaboard.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DashboardCardType(
  val id: String,
  val defaultTitle: String,
  val subtitle: String,
  val supportedSizes: List<CardSize>,
) {
  CLOCK(
    id = "clock",
    defaultTitle = "Clock & Date",
    subtitle = "Live time, date, and quick timer access",
    supportedSizes = listOf(CardSize.COMPACT, CardSize.WIDE),
  ),
  WEATHER(
    id = "weather",
    defaultTitle = "Weather",
    subtitle = "Local forecast, conditions, and rain outlook",
    supportedSizes = listOf(CardSize.COMPACT, CardSize.WIDE, CardSize.EXPANDED),
  ),
  BATTERY(
    id = "battery",
    defaultTitle = "Battery",
    subtitle = "Charge level, status, and thermal health",
    supportedSizes = listOf(CardSize.COMPACT, CardSize.WIDE),
  ),
  NOW_PLAYING(
    id = "now_playing",
    defaultTitle = "Now Playing",
    subtitle = "Live media session playback controls",
    supportedSizes = listOf(CardSize.WIDE, CardSize.EXPANDED),
  ),
  RAM(
    id = "ram",
    defaultTitle = "RAM Usage",
    subtitle = "Active system memory allocation",
    supportedSizes = listOf(CardSize.COMPACT, CardSize.WIDE),
  ),
  STORAGE(
    id = "storage",
    defaultTitle = "Storage",
    subtitle = "Internal flash space used and free",
    supportedSizes = listOf(CardSize.COMPACT, CardSize.WIDE),
  ),
  CPU(
    id = "cpu",
    defaultTitle = "CPU Monitor",
    subtitle = "Live usage, load, frequency & thermal",
    supportedSizes = listOf(CardSize.COMPACT, CardSize.WIDE, CardSize.EXPANDED),
  ),
  QUICK_ACTIONS(
    id = "quick_actions",
    defaultTitle = "Quick Actions",
    subtitle = "One-tap shortcuts to daily tools",
    supportedSizes = listOf(CardSize.COMPACT, CardSize.WIDE),
  ),
  CALENDAR(
    id = "calendar",
    defaultTitle = "Upcoming Events",
    subtitle = "Schedule from your device calendar",
    supportedSizes = listOf(CardSize.WIDE, CardSize.EXPANDED),
  );

  companion object {
    fun fromId(id: String): DashboardCardType =
      entries.firstOrNull { it.id == id } ?: CLOCK
  }
}

enum class CardSize(val label: String, val columnSpan: Int) {
  COMPACT("Compact", 1),
  WIDE("Wide", 2),
  EXPANDED("Expanded", 2);

  companion object {
    fun fromName(name: String): CardSize =
      entries.firstOrNull { it.name == name } ?: COMPACT
  }
}

enum class DashboardPreset(val label: String, val description: String) {
  EXPRESSIVE_BENTO("Expressive Bento", "Mixed compact and wide cards for balanced glanceability"),
  SYSTEM_TELEMETRY("System Telemetry", "Prioritizes Battery, RAM, Storage, and live clock"),
  DAILY_FOCUS("Daily Focus", "Weather, Calendar, Quick Actions, and Now Playing up front"),
  MINIMALIST("Minimalist", "Essential Clock, Weather, and Battery only"),
}

@Entity(tableName = "dashboard_cards")
data class DashboardCardEntity(
  @PrimaryKey val cardId: String,
  val title: String,
  val sortOrder: Int,
  val isVisible: Boolean,
  val cardSize: String,
)

object DashboardSeeds {
  fun cardsForPreset(preset: DashboardPreset): List<DashboardCardEntity> =
    when (preset) {
      DashboardPreset.EXPRESSIVE_BENTO ->
        listOf(
          DashboardCardEntity("clock", "Clock & Date", 0, true, CardSize.WIDE.name),
          DashboardCardEntity("weather", "Weather", 1, true, CardSize.EXPANDED.name),
          DashboardCardEntity("battery", "Battery", 2, true, CardSize.COMPACT.name),
          DashboardCardEntity("ram", "RAM Usage", 3, true, CardSize.COMPACT.name),
          DashboardCardEntity("cpu", "CPU Monitor", 4, true, CardSize.COMPACT.name),
          DashboardCardEntity("now_playing", "Now Playing", 5, true, CardSize.WIDE.name),
          DashboardCardEntity("storage", "Storage", 6, true, CardSize.COMPACT.name),
          DashboardCardEntity("quick_actions", "Quick Actions", 7, true, CardSize.COMPACT.name),
          DashboardCardEntity("calendar", "Upcoming Events", 8, true, CardSize.WIDE.name),
        )
      DashboardPreset.SYSTEM_TELEMETRY ->
        listOf(
          DashboardCardEntity("battery", "Battery", 0, true, CardSize.WIDE.name),
          DashboardCardEntity("ram", "RAM Usage", 1, true, CardSize.COMPACT.name),
          DashboardCardEntity("cpu", "CPU Monitor", 2, true, CardSize.COMPACT.name),
          DashboardCardEntity("storage", "Storage", 3, true, CardSize.COMPACT.name),
          DashboardCardEntity("clock", "Clock & Date", 4, true, CardSize.WIDE.name),
          DashboardCardEntity("weather", "Weather", 5, true, CardSize.WIDE.name),
          DashboardCardEntity("now_playing", "Now Playing", 6, true, CardSize.WIDE.name),
          DashboardCardEntity("quick_actions", "Quick Actions", 7, false, CardSize.COMPACT.name),
          DashboardCardEntity("calendar", "Upcoming Events", 8, false, CardSize.WIDE.name),
        )
      DashboardPreset.DAILY_FOCUS ->
        listOf(
          DashboardCardEntity("clock", "Clock & Date", 0, true, CardSize.WIDE.name),
          DashboardCardEntity("weather", "Weather", 1, true, CardSize.WIDE.name),
          DashboardCardEntity("calendar", "Upcoming Events", 2, true, CardSize.EXPANDED.name),
          DashboardCardEntity("quick_actions", "Quick Actions", 3, true, CardSize.WIDE.name),
          DashboardCardEntity("now_playing", "Now Playing", 4, true, CardSize.WIDE.name),
          DashboardCardEntity("battery", "Battery", 5, true, CardSize.COMPACT.name),
          DashboardCardEntity("ram", "RAM Usage", 6, false, CardSize.COMPACT.name),
          DashboardCardEntity("cpu", "CPU Monitor", 7, false, CardSize.COMPACT.name),
          DashboardCardEntity("storage", "Storage", 8, false, CardSize.COMPACT.name),
        )
      DashboardPreset.MINIMALIST ->
        listOf(
          DashboardCardEntity("clock", "Clock & Date", 0, true, CardSize.WIDE.name),
          DashboardCardEntity("weather", "Weather", 1, true, CardSize.WIDE.name),
          DashboardCardEntity("battery", "Battery", 2, true, CardSize.WIDE.name),
          DashboardCardEntity("now_playing", "Now Playing", 3, false, CardSize.WIDE.name),
          DashboardCardEntity("ram", "RAM Usage", 4, false, CardSize.COMPACT.name),
          DashboardCardEntity("cpu", "CPU Monitor", 5, false, CardSize.COMPACT.name),
          DashboardCardEntity("storage", "Storage", 6, false, CardSize.COMPACT.name),
          DashboardCardEntity("quick_actions", "Quick Actions", 7, false, CardSize.COMPACT.name),
          DashboardCardEntity("calendar", "Upcoming Events", 8, false, CardSize.WIDE.name),
        )
    }
}
