package com.example.jaboard.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jaboard.core.common.TemperatureUnit
import com.example.jaboard.core.database.DashboardCardEntity
import com.example.jaboard.core.database.DashboardRepository
import com.example.jaboard.core.datastore.SavedLocation
import com.example.jaboard.core.datastore.UserPreferences
import com.example.jaboard.core.datastore.UserPreferencesRepository
import com.example.jaboard.core.network.WeatherReport
import com.example.jaboard.core.network.WeatherRepository
import com.example.jaboard.core.system.BatteryMonitorRepository
import com.example.jaboard.core.system.BatterySnapshot
import com.example.jaboard.core.system.CalendarEventItem
import com.example.jaboard.core.system.CalendarRepository
import com.example.jaboard.core.system.DeviceMonitorRepository
import com.example.jaboard.core.system.MediaSessionRepository
import com.example.jaboard.core.system.MediaSessionsUiState
import com.example.jaboard.core.system.RamSnapshot
import com.example.jaboard.core.system.StorageSnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
  val cards: List<DashboardCardEntity> = emptyList(),
  val currentTimeMillis: Long = System.currentTimeMillis(),
  val showClockSeconds: Boolean = true,
  val showClockMillis: Boolean = false,
  val use24HourClock: Boolean = true,
  val savedLocation: SavedLocation? = null,
  val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
  val weatherReport: WeatherReport? = null,
  val battery: BatterySnapshot? = null,
  val ram: RamSnapshot? = null,
  val storage: StorageSnapshot? = null,
  val media: MediaSessionsUiState = MediaSessionsUiState(),
  val hasCalendarPermission: Boolean = false,
  val upcomingEvents: List<CalendarEventItem> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
  private val dashboardRepository: DashboardRepository,
  private val userPreferencesRepository: UserPreferencesRepository,
  private val weatherRepository: WeatherRepository,
  private val batteryMonitorRepository: BatteryMonitorRepository,
  private val deviceMonitorRepository: DeviceMonitorRepository,
  private val mediaSessionRepository: MediaSessionRepository,
  private val calendarRepository: CalendarRepository,
) : ViewModel() {

  private val clockFlow =
    userPreferencesRepository.preferences
      .map { it.showClockMillis }
      .distinctUntilChanged()
      .flatMapLatest { showMillis ->
        flow {
          while (true) {
            emit(System.currentTimeMillis())
            delay(if (showMillis) 33L else 1000L)
          }
        }
      }

  private val calendarState =
    MutableStateFlow(calendarRepository.hasCalendarPermission() to emptyList<CalendarEventItem>())

  private val weatherReportFlow =
    userPreferencesRepository.preferences.flatMapLatest { prefs ->
      val loc = prefs.savedLocation
      if (loc == null) flowOf(null)
      else weatherRepository.observeCachedWeather(loc.key)
    }

  val uiState: StateFlow<HomeUiState> =
    combine(
        dashboardRepository.cards,
        clockFlow,
        userPreferencesRepository.preferences,
        weatherReportFlow,
        batteryMonitorRepository.observeBattery(),
        deviceMonitorRepository.observeRam(),
        deviceMonitorRepository.observeStorage(),
        mediaSessionRepository.state,
        calendarState,
      ) { values ->
        @Suppress("UNCHECKED_CAST") val allCards = values[0] as List<DashboardCardEntity>
        val now = values[1] as Long
        val prefs = values[2] as UserPreferences
        val weather = values[3] as WeatherReport?
        val battery = values[4] as BatterySnapshot
        val ram = values[5] as RamSnapshot
        val storage = values[6] as StorageSnapshot
        val media = values[7] as MediaSessionsUiState
        @Suppress("UNCHECKED_CAST")
        val calPair = values[8] as Pair<Boolean, List<CalendarEventItem>>

        HomeUiState(
          cards = allCards.filter { it.isVisible }.sortedBy { it.sortOrder },
          currentTimeMillis = now,
          showClockSeconds = prefs.showClockSeconds,
          showClockMillis = prefs.showClockMillis,
          use24HourClock = prefs.use24HourClock,
          savedLocation = prefs.savedLocation,
          temperatureUnit = prefs.temperatureUnit,
          weatherReport = weather,
          battery = battery,
          ram = ram,
          storage = storage,
          media = media,
          hasCalendarPermission = calPair.first,
          upcomingEvents = calPair.second,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(),
      )

  init {
    refreshSystemIntegrations()
  }

  fun refreshSystemIntegrations() {
    mediaSessionRepository.refreshState()
    val calGranted = calendarRepository.hasCalendarPermission()
    if (!calGranted) {
      calendarState.value = false to emptyList()
    } else {
      viewModelScope.launch {
        val events = calendarRepository.loadUpcomingEvents(daysAhead = 7)
        calendarState.value = true to events
      }
    }
  }

  fun reorderVisibleCards(fromIndex: Int, toIndex: Int) {
    val currentVisible = uiState.value.cards
    if (fromIndex !in currentVisible.indices || toIndex !in currentVisible.indices || fromIndex == toIndex) {
      return
    }
    viewModelScope.launch {
      val mutableVisible = currentVisible.toMutableList()
      val moved = mutableVisible.removeAt(fromIndex)
      mutableVisible.add(toIndex, moved)
      dashboardRepository.saveOrder(mutableVisible)
    }
  }

  fun toggleMediaPlayPause() = mediaSessionRepository.togglePlayPause()

  fun skipNext() = mediaSessionRepository.skipToNext()

  fun skipPrevious() = mediaSessionRepository.skipToPrevious()

  fun openNotificationAccessSettings() = mediaSessionRepository.openNotificationAccessSettings()
}
