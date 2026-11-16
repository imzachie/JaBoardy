package com.example.jaboard.feature.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jaboard.core.common.TemperatureUnit
import com.example.jaboard.core.database.DashboardPreset
import com.example.jaboard.core.database.DashboardRepository
import com.example.jaboard.core.database.NotesRepository
import com.example.jaboard.core.database.TodoRepository
import com.example.jaboard.core.datastore.UserPreferences
import com.example.jaboard.core.datastore.UserPreferencesRepository
import com.example.jaboard.core.design.ThemeMode
import com.example.jaboard.core.network.WeatherRepository
import com.example.jaboard.core.system.MediaSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PermissionStatusSnapshot(
  val locationGranted: Boolean = false,
  val calendarGranted: Boolean = false,
  val notificationsGranted: Boolean = true,
  val notificationListenerGranted: Boolean = false,
)

data class SettingsUiState(
  val preferences: UserPreferences = UserPreferences(),
  val permissions: PermissionStatusSnapshot = PermissionStatusSnapshot(),
  val statusBanner: String? = null,
)

class SettingsViewModel(
  private val context: Context,
  private val userPreferencesRepository: UserPreferencesRepository,
  private val dashboardRepository: DashboardRepository,
  private val weatherRepository: WeatherRepository,
  private val notesRepository: NotesRepository,
  private val todoRepository: TodoRepository,
  private val mediaSessionRepository: MediaSessionRepository,
) : ViewModel() {

  private val permissionSnapshot = MutableStateFlow(readPermissions())
  private val statusBanner = MutableStateFlow<String?>(null)

  val uiState: StateFlow<SettingsUiState> =
    combine(userPreferencesRepository.preferences, permissionSnapshot, statusBanner) {
        prefs,
        perms,
        banner ->
        SettingsUiState(preferences = prefs, permissions = perms, statusBanner = banner)
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(permissions = readPermissions()),
      )

  fun refreshPermissions() {
    permissionSnapshot.value = readPermissions()
  }

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch { userPreferencesRepository.setThemeMode(mode) }
  }

  fun setUseDynamicColor(enabled: Boolean) {
    viewModelScope.launch { userPreferencesRepository.setUseDynamicColor(enabled) }
  }

  fun setTemperatureUnit(unit: TemperatureUnit) {
    viewModelScope.launch { userPreferencesRepository.setTemperatureUnit(unit) }
  }

  fun setShowClockSeconds(show: Boolean) {
    viewModelScope.launch { userPreferencesRepository.setShowClockSeconds(show) }
  }

  fun setShowClockMillis(show: Boolean) {
    viewModelScope.launch { userPreferencesRepository.setShowClockMillis(show) }
  }

  fun setUse24HourClock(use24Hour: Boolean) {
    viewModelScope.launch { userPreferencesRepository.setUse24HourClock(use24Hour) }
  }

  fun resetDashboardToDefault() {
    viewModelScope.launch {
      dashboardRepository.applyPreset(DashboardPreset.EXPRESSIVE_BENTO)
      userPreferencesRepository.setActivePreset(DashboardPreset.EXPRESSIVE_BENTO)
      statusBanner.value = "Dashboard restored to Expressive Bento defaults."
    }
  }

  fun clearWeatherCacheAndLocation() {
    viewModelScope.launch {
      weatherRepository.clearCache()
      userPreferencesRepository.clearLocation()
      statusBanner.value = "Weather cache and saved city cleared."
    }
  }

  fun clearAllPersonalProductivityData() {
    viewModelScope.launch {
      notesRepository.clearAll()
      todoRepository.clearAll()
      statusBanner.value = "Local notes and to-do lists cleared."
    }
  }

  fun dismissBanner() {
    statusBanner.value = null
  }

  fun openNotificationListenerSettings() {
    mediaSessionRepository.openNotificationAccessSettings()
  }

  fun openAppSettings() {
    val intent =
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    context.startActivity(intent)
  }

  private fun readPermissions(): PermissionStatusSnapshot {
    val loc =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    val cal =
      ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
        PackageManager.PERMISSION_GRANTED
    val notif =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      } else {
        true
      }
    val nls = mediaSessionRepository.hasNotificationListenerAccess()
    return PermissionStatusSnapshot(
      locationGranted = loc,
      calendarGranted = cal,
      notificationsGranted = notif,
      notificationListenerGranted = nls,
    )
  }
}
