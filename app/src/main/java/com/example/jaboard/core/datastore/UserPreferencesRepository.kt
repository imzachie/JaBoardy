package com.example.jaboard.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.jaboard.core.common.TemperatureUnit
import com.example.jaboard.core.database.DashboardPreset
import com.example.jaboard.core.design.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jaboard_prefs")

data class SavedLocation(
  val key: String,
  val cityName: String,
  val country: String,
  val latitude: Double,
  val longitude: Double,
)

data class UserPreferences(
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val useDynamicColor: Boolean = true,
  val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
  val activePreset: DashboardPreset = DashboardPreset.EXPRESSIVE_BENTO,
  val showClockSeconds: Boolean = true,
  val showClockMillis: Boolean = false,
  val use24HourClock: Boolean = true,
  val savedLocation: SavedLocation? = null,
)

class UserPreferencesRepository(private val context: Context) {

  private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
    val TEMPERATURE_UNIT = stringPreferencesKey("temperature_unit")
    val ACTIVE_PRESET = stringPreferencesKey("active_preset")
    val SHOW_CLOCK_SECONDS = booleanPreferencesKey("show_clock_seconds")
    val SHOW_CLOCK_MILLIS = booleanPreferencesKey("show_clock_millis")
    val USE_24_HOUR_CLOCK = booleanPreferencesKey("use_24_hour_clock")
    val LOCATION_KEY = stringPreferencesKey("location_key")
    val CITY_NAME = stringPreferencesKey("city_name")
    val COUNTRY = stringPreferencesKey("country")
    val LATITUDE = doublePreferencesKey("latitude")
    val LONGITUDE = doublePreferencesKey("longitude")
  }

  val preferences: Flow<UserPreferences> =
    context.dataStore.data.map { prefs ->
      val theme =
        prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
          ?: ThemeMode.SYSTEM
      val tempUnit =
        prefs[Keys.TEMPERATURE_UNIT]?.let {
          runCatching { TemperatureUnit.valueOf(it) }.getOrNull()
        } ?: TemperatureUnit.CELSIUS
      val preset =
        prefs[Keys.ACTIVE_PRESET]?.let {
          runCatching { DashboardPreset.valueOf(it) }.getOrNull()
        } ?: DashboardPreset.EXPRESSIVE_BENTO

      val locKey = prefs[Keys.LOCATION_KEY]
      val city = prefs[Keys.CITY_NAME]
      val lat = prefs[Keys.LATITUDE]
      val lon = prefs[Keys.LONGITUDE]
      val savedLoc =
        if (locKey != null && city != null && lat != null && lon != null) {
          SavedLocation(
            key = locKey,
            cityName = city,
            country = prefs[Keys.COUNTRY].orEmpty(),
            latitude = lat,
            longitude = lon,
          )
        } else {
          null
        }

      UserPreferences(
        themeMode = theme,
        useDynamicColor = prefs[Keys.USE_DYNAMIC_COLOR] ?: true,
        temperatureUnit = tempUnit,
        activePreset = preset,
        showClockSeconds = prefs[Keys.SHOW_CLOCK_SECONDS] ?: true,
        showClockMillis = prefs[Keys.SHOW_CLOCK_MILLIS] ?: false,
        use24HourClock = prefs[Keys.USE_24_HOUR_CLOCK] ?: true,
        savedLocation = savedLoc,
      )
    }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
  }

  suspend fun setUseDynamicColor(enabled: Boolean) {
    context.dataStore.edit { it[Keys.USE_DYNAMIC_COLOR] = enabled }
  }

  suspend fun setTemperatureUnit(unit: TemperatureUnit) {
    context.dataStore.edit { it[Keys.TEMPERATURE_UNIT] = unit.name }
  }

  suspend fun setActivePreset(preset: DashboardPreset) {
    context.dataStore.edit { it[Keys.ACTIVE_PRESET] = preset.name }
  }

  suspend fun setShowClockSeconds(show: Boolean) {
    context.dataStore.edit { it[Keys.SHOW_CLOCK_SECONDS] = show }
  }

  suspend fun setShowClockMillis(show: Boolean) {
    context.dataStore.edit { it[Keys.SHOW_CLOCK_MILLIS] = show }
  }

  suspend fun setUse24HourClock(use24Hour: Boolean) {
    context.dataStore.edit { it[Keys.USE_24_HOUR_CLOCK] = use24Hour }
  }

  suspend fun saveLocation(location: SavedLocation) {
    context.dataStore.edit { prefs ->
      prefs[Keys.LOCATION_KEY] = location.key
      prefs[Keys.CITY_NAME] = location.cityName
      prefs[Keys.COUNTRY] = location.country
      prefs[Keys.LATITUDE] = location.latitude
      prefs[Keys.LONGITUDE] = location.longitude
    }
  }

  suspend fun clearLocation() {
    context.dataStore.edit { prefs ->
      prefs.remove(Keys.LOCATION_KEY)
      prefs.remove(Keys.CITY_NAME)
      prefs.remove(Keys.COUNTRY)
      prefs.remove(Keys.LATITUDE)
      prefs.remove(Keys.LONGITUDE)
    }
  }
}
