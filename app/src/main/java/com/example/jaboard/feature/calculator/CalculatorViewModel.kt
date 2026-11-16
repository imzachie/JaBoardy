package com.example.jaboard.feature.calculator

import androidx.lifecycle.ViewModel
import com.example.jaboard.core.common.CalculatorEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CalculatorHistoryEntry(val expression: String, val result: String)

data class CalculatorUiState(
  val expression: String = "",
  val livePreview: String = "",
  val errorMessage: String? = null,
  val isScientificExpanded: Boolean = false,
  val history: List<CalculatorHistoryEntry> = emptyList(),
)

class CalculatorViewModel : ViewModel() {
  private val _uiState = MutableStateFlow(CalculatorUiState())
  val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

  fun appendToken(token: String) {
    val current = _uiState.value.expression
    val next = current + token
    updateExpression(next)
  }

  fun backspace() {
    val current = _uiState.value.expression
    if (current.isNotEmpty()) {
      updateExpression(current.dropLast(1))
    }
  }

  fun clear() {
    _uiState.value = _uiState.value.copy(expression = "", livePreview = "", errorMessage = null)
  }

  fun toggleScientific() {
    _uiState.value =
      _uiState.value.copy(isScientificExpanded = !_uiState.value.isScientificExpanded)
  }

  fun evaluateEquals() {
    val expr = _uiState.value.expression
    if (expr.isBlank()) return
    when (val res = CalculatorEngine.evaluate(expr)) {
      is CalculatorEngine.EvalResult.Success -> {
        val entry = CalculatorHistoryEntry(expression = expr, result = res.formatted)
        _uiState.value =
          _uiState.value.copy(
            expression = res.formatted,
            livePreview = "",
            errorMessage = null,
            history = (listOf(entry) + _uiState.value.history).take(15),
          )
      }
      is CalculatorEngine.EvalResult.Error -> {
        _uiState.value = _uiState.value.copy(errorMessage = res.reason)
      }
    }
  }

  fun reuseHistoryResult(result: String) {
    updateExpression(_uiState.value.expression + result)
  }

  private fun updateExpression(next: String) {
    val preview =
      if (next.isBlank()) ""
      else {
        when (val res = CalculatorEngine.evaluate(next)) {
          is CalculatorEngine.EvalResult.Success ->
            if (res.formatted == next) "" else res.formatted
          is CalculatorEngine.EvalResult.Error -> ""
        }
      }
    _uiState.value =
      _uiState.value.copy(expression = next, livePreview = preview, errorMessage = null)
  }
}
