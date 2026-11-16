package com.example.jaboard.feature.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jaboard.core.common.TemperatureUnit
import com.example.jaboard.core.datastore.SavedLocation
import com.example.jaboard.core.datastore.UserPreferencesRepository
import com.example.jaboard.core.network.GeocodingPlaceDto
import com.example.jaboard.core.network.WeatherFetchResult
import com.example.jaboard.core.network.WeatherReport
import com.example.jaboard.core.network.WeatherRepository
import com.example.jaboard.core.system.LocationRepository
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WeatherUiState(
  val savedLocation: SavedLocation? = null,
  val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
  val report: WeatherReport? = null,
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
  val searchQuery: String = "",
  val searchResults: List<GeocodingPlaceDto> = emptyList(),
  val isSearchingCities: Boolean = false,
  val isResolvingGps: Boolean = false,
  val aiSummary: String? = null,
  val isGeneratingAiSummary: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModel(
  private val weatherRepository: WeatherRepository,
  private val userPreferencesRepository: UserPreferencesRepository,
  private val locationRepository: LocationRepository,
  private val weatherAiSummaryRepository: com.example.jaboard.core.network.WeatherAiSummaryRepository? = null,
) : ViewModel() {

  private val searchQuery = MutableStateFlow("")
  private val searchResults = MutableStateFlow<List<GeocodingPlaceDto>>(emptyList())
  private val isSearching = MutableStateFlow(false)
  private val isLoading = MutableStateFlow(false)
  private val isResolvingGps = MutableStateFlow(false)
  private val errorMessage = MutableStateFlow<String?>(null)
  private val aiSummary = MutableStateFlow<String?>(null)
  private val isGeneratingAiSummary = MutableStateFlow(false)

  private var searchJob: Job? = null

  private val cachedReportFlow =
    userPreferencesRepository.preferences.flatMapLatest { prefs ->
      val loc = prefs.savedLocation
      if (loc == null) flowOf(null)
      else weatherRepository.observeCachedWeather(loc.key)
    }

  val uiState: StateFlow<WeatherUiState> =
    combine(
        userPreferencesRepository.preferences,
        cachedReportFlow,
        isLoading,
        errorMessage,
        searchQuery,
        searchResults,
        isSearching,
        isResolvingGps,
        aiSummary,
        isGeneratingAiSummary,
      ) { values ->
        val prefs = values[0] as com.example.jaboard.core.datastore.UserPreferences
        val cached = values[1] as WeatherReport?
        val loading = values[2] as Boolean
        val err = values[3] as String?
        val query = values[4] as String
        @Suppress("UNCHECKED_CAST") val results = values[5] as List<GeocodingPlaceDto>
        val searching = values[6] as Boolean
        val resolvingGps = values[7] as Boolean
        val summary = values[8] as String?
        val generatingAi = values[9] as Boolean

        WeatherUiState(
          savedLocation = prefs.savedLocation,
          temperatureUnit = prefs.temperatureUnit,
          report = cached,
          isLoading = loading,
          errorMessage = err,
          searchQuery = query,
          searchResults = results,
          isSearchingCities = searching,
          isResolvingGps = resolvingGps,
          aiSummary = summary,
          isGeneratingAiSummary = generatingAi,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WeatherUiState(),
      )

  fun generateAiSummary() {
    val report = uiState.value.report ?: return
    val unit = uiState.value.temperatureUnit
    val repo = weatherAiSummaryRepository ?: return
    viewModelScope.launch {
      isGeneratingAiSummary.value = true
      try {
        val result = repo.generateSummary(report, unit)
        aiSummary.value = result
      } catch (e: Exception) {
        aiSummary.value = repo.fallbackSummary(report, unit)
      } finally {
        isGeneratingAiSummary.value = false
      }
    }
  }

  fun hasLocationPermission(): Boolean = locationRepository.hasCoarseLocationPermission()

  fun refreshIfConfigured() {
    val loc = uiState.value.savedLocation ?: return
    fetchForLocation(loc)
  }

  fun onSearchQueryChanged(query: String) {
    searchQuery.value = query
    searchJob?.cancel()
    if (query.trim().length < 2) {
      searchResults.value = emptyList()
      isSearching.value = false
      return
    }
    searchJob =
      viewModelScope.launch {
        delay(350L)
        isSearching.value = true
        val res = weatherRepository.searchCities(query)
        res
          .onSuccess { places ->
            searchResults.value = places
            errorMessage.value = null
          }
          .onFailure { err ->
            searchResults.value = emptyList()
            errorMessage.value = err.localizedMessage ?: "Unable to search cities offline."
          }
        isSearching.value = false
      }
  }

  fun selectCity(place: GeocodingPlaceDto) {
    val key = String.format(Locale.US, "%.2f_%.2f", place.latitude, place.longitude)
    val loc =
      SavedLocation(
        key = key,
        cityName = place.name,
        country = listOfNotNull(place.admin1, place.country).joinToString(", "),
        latitude = place.latitude,
        longitude = place.longitude,
      )
    viewModelScope.launch {
      userPreferencesRepository.saveLocation(loc)
      searchQuery.value = ""
      searchResults.value = emptyList()
      fetchForLocation(loc)
    }
  }

  fun useCurrentDeviceLocation() {
    viewModelScope.launch {
      isResolvingGps.value = true
      errorMessage.value = null
      val result = locationRepository.resolveCurrentLocation()
      result
        .onSuccess { loc ->
          userPreferencesRepository.saveLocation(loc)
          fetchForLocation(loc)
        }
        .onFailure { err ->
          errorMessage.value = err.message ?: "Could not determine device location."
        }
      isResolvingGps.value = false
    }
  }

  fun toggleTemperatureUnit() {
    viewModelScope.launch {
      val next =
        if (uiState.value.temperatureUnit == TemperatureUnit.CELSIUS) TemperatureUnit.FAHRENHEIT
        else TemperatureUnit.CELSIUS
      userPreferencesRepository.setTemperatureUnit(next)
    }
  }

  private fun fetchForLocation(location: SavedLocation) {
    viewModelScope.launch {
      isLoading.value = true
      errorMessage.value = null
      when (val result = weatherRepository.refreshWeather(location)) {
        is WeatherFetchResult.Success -> Unit
        is WeatherFetchResult.Failure -> errorMessage.value = result.message
      }
      isLoading.value = false
    }
  }
}
