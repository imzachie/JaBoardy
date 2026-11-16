package com.example

import com.example.jaboard.core.common.CalculatorEngine
import com.example.jaboard.core.common.Formatters
import com.example.jaboard.core.common.TemperatureUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorAndFormattersTest {

  @Test
  fun `calculator respects operator precedence and parentheses`() {
    val res = CalculatorEngine.evaluate("2+3×4")
    assertTrue(res is CalculatorEngine.EvalResult.Success)
    assertEquals("14", (res as CalculatorEngine.EvalResult.Success).formatted)

    val parens = CalculatorEngine.evaluate("(2+3)×4")
    assertEquals("20", (parens as CalculatorEngine.EvalResult.Success).formatted)
  }

  @Test
  fun `calculator supports percentage and scientific sqrt`() {
    val pct = CalculatorEngine.evaluate("200×15%")
    assertEquals("30", (pct as CalculatorEngine.EvalResult.Success).formatted)

    val root = CalculatorEngine.evaluate("√(81)+2^3")
    assertEquals("17", (root as CalculatorEngine.EvalResult.Success).formatted)
  }

  @Test
  fun `calculator handles division by zero gracefully`() {
    val divZero = CalculatorEngine.evaluate("10÷0")
    assertTrue(divZero is CalculatorEngine.EvalResult.Error)
  }

  @Test
  fun `formatters format temperature, bytes, and stopwatch accurately`() {
    assertEquals("25°C", Formatters.formatTemperature(25.0, TemperatureUnit.CELSIUS))
    assertEquals("77°F", Formatters.formatTemperature(25.0, TemperatureUnit.FAHRENHEIT))
    assertEquals("01:05.23", Formatters.formatStopwatch(65_234L))
    assertEquals("1.0 GB", Formatters.formatBytes(1024L * 1024L * 1024L))
    assertEquals("Thunderstorm", Formatters.wmoCodeDescription(95))
  }

  @Test
  fun `formatClockTime handles 12h, 24h, seconds, and milliseconds`() {
    val cal =
      java.util.Calendar.getInstance().apply {
        set(2026, java.util.Calendar.NOVEMBER, 15, 19, 5, 9)
        set(java.util.Calendar.MILLISECOND, 42)
      }
    val ts = cal.timeInMillis

    assertEquals(
      "19:05:09",
      Formatters.formatClockTime(ts, use24Hour = true, showSeconds = true, showMillis = false, java.util.Locale.US),
    )
    assertEquals(
      "19:05",
      Formatters.formatClockTime(ts, use24Hour = true, showSeconds = false, showMillis = false, java.util.Locale.US),
    )
    assertEquals(
      "19:05:09.042",
      Formatters.formatClockTime(ts, use24Hour = true, showSeconds = true, showMillis = true, java.util.Locale.US),
    )
    assertEquals(
      "7:05:09 PM",
      Formatters.formatClockTime(ts, use24Hour = false, showSeconds = true, showMillis = false, java.util.Locale.US),
    )
    assertEquals(
      "7:05:09.042 PM",
      Formatters.formatClockTime(ts, use24Hour = false, showSeconds = true, showMillis = true, java.util.Locale.US),
    )
  }
}
