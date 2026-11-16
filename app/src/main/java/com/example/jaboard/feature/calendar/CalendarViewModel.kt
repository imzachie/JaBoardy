package com.example.jaboard.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jaboard.core.system.CalendarEventItem
import com.example.jaboard.core.system.CalendarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CalendarUiState(
  val hasPermission: Boolean = false,
  val isLoading: Boolean = false,
  val events: List<CalendarEventItem> = emptyList(),
)

class CalendarViewModel(private val calendarRepository: CalendarRepository) : ViewModel() {

  private val _uiState =
    MutableStateFlow(CalendarUiState(hasPermission = calendarRepository.hasCalendarPermission()))
  val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

  init {
    refresh()
  }

  fun refresh() {
    val granted = calendarRepository.hasCalendarPermission()
    if (!granted) {
      _uiState.value = CalendarUiState(hasPermission = false, isLoading = false, events = emptyList())
      return
    }
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(hasPermission = true, isLoading = true)
      val loaded = calendarRepository.loadUpcomingEvents(daysAhead = 14)
      _uiState.value = CalendarUiState(hasPermission = true, isLoading = false, events = loaded)
    }
  }
}
