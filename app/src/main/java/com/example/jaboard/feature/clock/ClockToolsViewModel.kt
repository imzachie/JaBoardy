package com.example.jaboard.feature.clock

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jaboard.core.system.TimerNotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StopwatchUiState(
  val elapsedMs: Long = 0L,
  val isRunning: Boolean = false,
  val lapsMs: List<Long> = emptyList(),
)

data class TimerUiState(
  val totalDurationMs: Long = 5 * 60_000L,
  val remainingMs: Long = 5 * 60_000L,
  val isRunning: Boolean = false,
  val hasCompleted: Boolean = false,
  val label: String = "Focus Timer",
) {
  val progress: Float
    get() =
      if (totalDurationMs <= 0L) 0f
      else (remainingMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
}

class ClockToolsViewModel(private val timerNotificationHelper: TimerNotificationHelper) :
  ViewModel() {

  private val _currentTimeMillis = MutableStateFlow(System.currentTimeMillis())
  val currentTimeMillis: StateFlow<Long> = _currentTimeMillis.asStateFlow()

  private val _stopwatchState = MutableStateFlow(StopwatchUiState())
  val stopwatchState: StateFlow<StopwatchUiState> = _stopwatchState.asStateFlow()

  private val _timerState = MutableStateFlow(TimerUiState())
  val timerState: StateFlow<TimerUiState> = _timerState.asStateFlow()

  private var stopwatchAccumulatedMs = 0L
  private var stopwatchStartElapsed = 0L
  private var stopwatchJob: Job? = null

  private var timerEndElapsed = 0L
  private var timerJob: Job? = null

  init {
    viewModelScope.launch {
      while (true) {
        _currentTimeMillis.value = System.currentTimeMillis()
        delay(1000L)
      }
    }
  }

  fun hasNotificationPermission(): Boolean = timerNotificationHelper.hasNotificationPermission()

  // Stopwatch controls
  fun toggleStopwatch() {
    if (_stopwatchState.value.isRunning) {
      stopwatchAccumulatedMs =
        stopwatchAccumulatedMs + (SystemClock.elapsedRealtime() - stopwatchStartElapsed)
      stopwatchJob?.cancel()
      _stopwatchState.value =
        _stopwatchState.value.copy(elapsedMs = stopwatchAccumulatedMs, isRunning = false)
    } else {
      stopwatchStartElapsed = SystemClock.elapsedRealtime()
      _stopwatchState.value = _stopwatchState.value.copy(isRunning = true)
      stopwatchJob?.cancel()
      stopwatchJob =
        viewModelScope.launch {
          while (true) {
            val current =
              stopwatchAccumulatedMs + (SystemClock.elapsedRealtime() - stopwatchStartElapsed)
            _stopwatchState.value = _stopwatchState.value.copy(elapsedMs = current)
            delay(32L)
          }
        }
    }
  }

  fun recordLap() {
    val current = _stopwatchState.value.elapsedMs
    if (current > 0L) {
      _stopwatchState.value =
        _stopwatchState.value.copy(lapsMs = listOf(current) + _stopwatchState.value.lapsMs)
    }
  }

  fun resetStopwatch() {
    stopwatchJob?.cancel()
    stopwatchAccumulatedMs = 0L
    _stopwatchState.value = StopwatchUiState()
  }

  // Timer controls
  fun setTimerPreset(minutes: Int) {
    timerJob?.cancel()
    val ms = minutes.coerceAtLeast(1) * 60_000L
    _timerState.value =
      _timerState.value.copy(
        totalDurationMs = ms,
        remainingMs = ms,
        isRunning = false,
        hasCompleted = false,
      )
  }

  fun setCustomTimerSeconds(totalSeconds: Long) {
    if (totalSeconds <= 0L) return
    timerJob?.cancel()
    val ms = totalSeconds * 1000L
    _timerState.value =
      _timerState.value.copy(
        totalDurationMs = ms,
        remainingMs = ms,
        isRunning = false,
        hasCompleted = false,
      )
  }

  fun toggleTimer() {
    val current = _timerState.value
    if (current.isRunning) {
      timerJob?.cancel()
      val remaining = (timerEndElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
      _timerState.value = current.copy(remainingMs = remaining, isRunning = false)
    } else {
      val baseMs =
        if (current.remainingMs <= 0L) current.totalDurationMs else current.remainingMs
      timerEndElapsed = SystemClock.elapsedRealtime() + baseMs
      _timerState.value =
        current.copy(remainingMs = baseMs, isRunning = true, hasCompleted = false)
      timerJob?.cancel()
      timerJob =
        viewModelScope.launch {
          while (true) {
            val rem = (timerEndElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            if (rem <= 0L) {
              _timerState.value =
                _timerState.value.copy(remainingMs = 0L, isRunning = false, hasCompleted = true)
              timerNotificationHelper.showTimerFinished(_timerState.value.label)
              break
            } else {
              _timerState.value = _timerState.value.copy(remainingMs = rem)
              delay(100L)
            }
          }
        }
    }
  }

  fun resetTimer() {
    timerJob?.cancel()
    _timerState.value =
      _timerState.value.copy(
        remainingMs = _timerState.value.totalDurationMs,
        isRunning = false,
        hasCompleted = false,
      )
  }
}
