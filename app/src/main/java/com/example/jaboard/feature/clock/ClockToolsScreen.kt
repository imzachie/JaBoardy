package com.example.jaboard.feature.clock

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jaboard.core.common.Formatters
import com.example.jaboard.core.design.ExpressiveCard
import com.example.jaboard.core.design.JaBoardMonoStyles
import com.example.jaboard.core.design.JaBoardStatusColors
import com.example.jaboard.core.design.JaBoardUtilityTopBar
import com.example.jaboard.core.design.MetricProgressRing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.sin

enum class ClockToolTab(val key: String, val label: String) {
  CLOCK("clock", "Clock"),
  TIMER("timer", "Timer"),
  STOPWATCH("stopwatch", "Stopwatch");

  companion object {
    fun indexForKey(key: String): Int =
      entries.indexOfFirst { it.key.equals(key, ignoreCase = true) }.coerceAtLeast(0)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockToolsScreen(
  viewModel: ClockToolsViewModel,
  initialTab: String,
  onBack: () -> Unit,
) {
  var selectedTabIndex by rememberSaveable(initialTab) {
    mutableIntStateOf(ClockToolTab.indexForKey(initialTab))
  }
  val currentTab = ClockToolTab.entries[selectedTabIndex]

  Scaffold(
    topBar = {
      JaBoardUtilityTopBar(
        title = currentTab.label,
        subtitle = TimeZone.getDefault().displayName,
        onBack = onBack,
      )
    }
  ) { innerPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
        ClockToolTab.entries.forEachIndexed { idx, tab ->
          Tab(
            selected = selectedTabIndex == idx,
            onClick = { selectedTabIndex = idx },
            text = { Text(tab.label) },
            modifier = Modifier.testTag("clock_tab_${tab.key}"),
          )
        }
      }

      when (currentTab) {
        ClockToolTab.CLOCK -> ClockPane(viewModel)
        ClockToolTab.TIMER -> TimerPane(viewModel)
        ClockToolTab.STOPWATCH -> StopwatchPane(viewModel)
      }
    }
  }
}

@Composable
private fun ClockPane(viewModel: ClockToolsViewModel) {
  val nowMillis by viewModel.currentTimeMillis.collectAsStateWithLifecycle()
  val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
  val dateFormatter = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }
  val cal = remember(nowMillis) { Calendar.getInstance().apply { timeInMillis = nowMillis } }

  val primaryColor = MaterialTheme.colorScheme.primary
  val onSurface = MaterialTheme.colorScheme.onSurface
  val tertiary = MaterialTheme.colorScheme.tertiary
  val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

  Column(
    modifier = Modifier.fillMaxSize().padding(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        Canvas(modifier = Modifier.size(190.dp)) {
          val radius = size.minDimension / 2f - 8.dp.toPx()
          val center = Offset(size.width / 2f, size.height / 2f)

          drawCircle(
            color = trackColor,
            radius = radius,
            center = center,
            style = Stroke(width = 4.dp.toPx()),
          )

          for (tick in 0 until 12) {
            val angleRad = Math.toRadians((tick * 30 - 90).toDouble())
            val startR = radius - 10.dp.toPx()
            val endR = radius - 2.dp.toPx()
            drawLine(
              color = onSurface.copy(alpha = 0.45f),
              start =
                Offset(
                  center.x + (startR * cos(angleRad)).toFloat(),
                  center.y + (startR * sin(angleRad)).toFloat(),
                ),
              end =
                Offset(
                  center.x + (endR * cos(angleRad)).toFloat(),
                  center.y + (endR * sin(angleRad)).toFloat(),
                ),
              strokeWidth = 2.5.dp.toPx(),
              cap = StrokeCap.Round,
            )
          }

          val sec = cal.get(Calendar.SECOND)
          val min = cal.get(Calendar.MINUTE)
          val hr = cal.get(Calendar.HOUR)

          val hourAngle = Math.toRadians(((hr + min / 60f) * 30f - 90f).toDouble())
          val minAngle = Math.toRadians(((min + sec / 60f) * 6f - 90f).toDouble())
          val secAngle = Math.toRadians((sec * 6f - 90f).toDouble())

          drawLine(
            color = onSurface,
            start = center,
            end =
              Offset(
                center.x + (radius * 0.5f * cos(hourAngle)).toFloat(),
                center.y + (radius * 0.5f * sin(hourAngle)).toFloat(),
              ),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round,
          )
          drawLine(
            color = primaryColor,
            start = center,
            end =
              Offset(
                center.x + (radius * 0.72f * cos(minAngle)).toFloat(),
                center.y + (radius * 0.72f * sin(minAngle)).toFloat(),
              ),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
          )
          drawLine(
            color = tertiary,
            start = center,
            end =
              Offset(
                center.x + (radius * 0.82f * cos(secAngle)).toFloat(),
                center.y + (radius * 0.82f * sin(secAngle)).toFloat(),
              ),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
          )
          drawCircle(color = primaryColor, radius = 5.dp.toPx(), center = center)
        }

        Text(
          text = timeFormatter.format(Date(nowMillis)),
          style = JaBoardMonoStyles.HeroNumeric,
        )
        Text(
          text = dateFormatter.format(Date(nowMillis)),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun TimerPane(viewModel: ClockToolsViewModel) {
  val timer by viewModel.timerState.collectAsStateWithLifecycle()

  val notificationLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
      viewModel.toggleTimer()
    }

  Column(
    modifier = Modifier.fillMaxSize().padding(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      listOf(1 to "1m", 5 to "5m", 10 to "10m", 25 to "25m", 45 to "45m").forEach { (mins, label)
        ->
        FilterChip(
          selected = timer.totalDurationMs == mins * 60_000L,
          onClick = { viewModel.setTimerPreset(mins) },
          label = { Text(label) },
        )
      }
    }

    ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        MetricProgressRing(
          progress = timer.progress,
          size = 210.dp,
          strokeWidth = 12.dp,
          activeColor =
            if (timer.hasCompleted) JaBoardStatusColors.HealthyGreen
            else MaterialTheme.colorScheme.primary,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = Formatters.formatTimer(timer.remainingMs),
              style = JaBoardMonoStyles.HeroNumeric,
              modifier = Modifier.testTag("timer_readout"),
            )
            Text(
              text =
                when {
                  timer.hasCompleted -> "Completed"
                  timer.isRunning -> "Running"
                  else -> "Paused"
                },
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Button(
            onClick = {
              if (!timer.isRunning &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !viewModel.hasNotificationPermission()
              ) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
              } else {
                viewModel.toggleTimer()
              }
            },
            modifier = Modifier.testTag("timer_start_pause_button"),
          ) {
            Icon(
              if (timer.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
              contentDescription = null,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (timer.isRunning) "Pause" else "Start")
          }
          FilledTonalButton(
            onClick = viewModel::resetTimer,
            modifier = Modifier.testTag("timer_reset_button"),
          ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reset")
          }
        }
      }
    }
  }
}

@Composable
private fun StopwatchPane(viewModel: ClockToolsViewModel) {
  val sw by viewModel.stopwatchState.collectAsStateWithLifecycle()

  Column(
    modifier = Modifier.fillMaxSize().padding(20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        Text(
          text = Formatters.formatStopwatch(sw.elapsedMs),
          style = JaBoardMonoStyles.HeroNumeric.copy(fontSize = MaterialTheme.typography.displayMedium.fontSize),
          modifier = Modifier.testTag("stopwatch_readout"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Button(
            onClick = viewModel::toggleStopwatch,
            modifier = Modifier.testTag("stopwatch_start_pause_button"),
          ) {
            Icon(
              if (sw.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
              contentDescription = null,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (sw.isRunning) "Pause" else "Start")
          }
          FilledTonalButton(
            onClick = viewModel::recordLap,
            enabled = sw.elapsedMs > 0L,
            modifier = Modifier.testTag("stopwatch_lap_button"),
          ) {
            Icon(Icons.Rounded.Flag, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Lap")
          }
          FilledTonalButton(
            onClick = viewModel::resetStopwatch,
            enabled = sw.elapsedMs > 0L || sw.lapsMs.isNotEmpty(),
            modifier = Modifier.testTag("stopwatch_reset_button"),
          ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
          }
        }
      }
    }

    if (sw.lapsMs.isNotEmpty()) {
      ExpressiveCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
        LazyColumn(
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          itemsIndexed(sw.lapsMs) { idx, lapTime ->
            val lapNumber = sw.lapsMs.size - idx
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text("Lap $lapNumber", style = MaterialTheme.typography.titleSmall)
              Text(Formatters.formatStopwatch(lapTime), style = JaBoardMonoStyles.MetricReadout)
            }
          }
        }
      }
    }
  }
}
