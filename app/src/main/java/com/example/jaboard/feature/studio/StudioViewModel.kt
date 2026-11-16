package com.example.jaboard.feature.studio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jaboard.core.database.CardSize
import com.example.jaboard.core.database.DashboardCardEntity
import com.example.jaboard.core.database.DashboardPreset
import com.example.jaboard.core.database.DashboardRepository
import com.example.jaboard.core.datastore.UserPreferences
import com.example.jaboard.core.datastore.UserPreferencesRepository
import com.example.jaboard.core.design.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StudioUiState(
  val cards: List<DashboardCardEntity> = emptyList(),
  val preferences: UserPreferences = UserPreferences(),
)

class StudioViewModel(
  private val dashboardRepository: DashboardRepository,
  private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

  val uiState: StateFlow<StudioUiState> =
    combine(dashboardRepository.cards, userPreferencesRepository.preferences) { cards, prefs ->
        StudioUiState(cards = cards.sortedBy { it.sortOrder }, preferences = prefs)
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StudioUiState(),
      )

  fun moveCard(fromIndex: Int, toIndex: Int) {
    val current = uiState.value.cards
    if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) return
    viewModelScope.launch {
      val mutable = current.toMutableList()
      val moved = mutable.removeAt(fromIndex)
      mutable.add(toIndex, moved)
      dashboardRepository.saveOrder(mutable)
    }
  }

  fun toggleVisibility(cardId: String, visible: Boolean) {
    viewModelScope.launch {
      dashboardRepository.setVisibility(cardId, visible, uiState.value.cards)
    }
  }

  fun setCardSize(cardId: String, size: CardSize) {
    viewModelScope.launch {
      dashboardRepository.setCardSize(cardId, size, uiState.value.cards)
    }
  }

  fun applyPreset(preset: DashboardPreset) {
    viewModelScope.launch {
      dashboardRepository.applyPreset(preset)
      userPreferencesRepository.setActivePreset(preset)
    }
  }

  fun restoreDefaults() {
    applyPreset(DashboardPreset.EXPRESSIVE_BENTO)
  }

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch { userPreferencesRepository.setThemeMode(mode) }
  }

  fun setUseDynamicColor(enabled: Boolean) {
    viewModelScope.launch { userPreferencesRepository.setUseDynamicColor(enabled) }
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
}
