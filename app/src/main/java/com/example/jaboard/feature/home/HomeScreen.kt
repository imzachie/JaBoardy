package com.example.jaboard.feature.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.jaboard.core.common.Formatters
import com.example.jaboard.core.database.CardSize
import com.example.jaboard.core.database.DashboardCardEntity
import com.example.jaboard.core.database.DashboardCardType
import com.example.jaboard.core.design.EmptyStateView
import com.example.jaboard.core.design.ExpressiveCard
import com.example.jaboard.core.design.JaBoardLargeTopBar
import com.example.jaboard.core.design.JaBoardMonoStyles
import com.example.jaboard.core.design.JaBoardStatusColors
import com.example.jaboard.feature.weather.weatherIconForCode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  viewModel: HomeViewModel,
  onOpenStudio: () -> Unit,
  onOpenWeather: () -> Unit,
  onOpenBattery: () -> Unit,
  onOpenMusic: () -> Unit,
  onOpenDeviceSection: (String) -> Unit,
  onOpenClockTab: (String) -> Unit,
  onOpenCalendar: () -> Unit,
  onOpenNotes: () -> Unit,
  onOpenTodo: () -> Unit,
  onOpenCalculator: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val lifecycleOwner = LocalLifecycleOwner.current
  val haptics = LocalHapticFeedback.current
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

  val calendarPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
      viewModel.refreshSystemIntegrations()
    }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.refreshSystemIntegrations()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  val gridState = rememberLazyGridState()
  var draggedIndex by remember { mutableIntStateOf(-1) }
  var dragOffsetX by remember { mutableStateOf(0f) }
  var dragOffsetY by remember { mutableStateOf(0f) }

  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      JaBoardLargeTopBar(
        title = "JaBoard",
        subtitle = "Long-press a card to drag & reorder",
        scrollBehavior = scrollBehavior,
        actions = {
          IconButton(
            onClick = onOpenStudio,
            modifier = Modifier.testTag("home_open_studio_button"),
          ) {
            Icon(
              Icons.Rounded.Tune,
              contentDescription = stringResource(R.string.cd_edit_dashboard),
            )
          }
        },
      )
    },
  ) { innerPadding ->
    if (uiState.cards.isEmpty()) {
      EmptyStateView(
        icon = Icons.Rounded.Tune,
        title = "All dashboard cards hidden",
        message = "Open Studio to toggle cards back on or apply a preset layout.",
        actionLabel = "Open Studio",
        onAction = onOpenStudio,
        modifier = Modifier.fillMaxSize().padding(innerPadding),
      )
    } else {
      BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        val columnCount =
          when {
            maxWidth >= 900.dp -> 4
            maxWidth >= 600.dp -> 3
            else -> 2
          }

        LazyVerticalGrid(
          state = gridState,
          columns = GridCells.Fixed(columnCount),
          contentPadding = PaddingValues(16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier =
            Modifier.fillMaxSize()
              .testTag("home_bento_grid")
              .pointerInput(uiState.cards) {
                detectDragGesturesAfterLongPress(
                  onDragStart = { offset ->
                    val hit =
                      gridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                        offset.x.toInt() in item.offset.x..(item.offset.x + item.size.width) &&
                          offset.y.toInt() in item.offset.y..(item.offset.y + item.size.height)
                      }
                    if (hit != null) {
                      draggedIndex = hit.index
                      dragOffsetX = 0f
                      dragOffsetY = 0f
                      haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                  },
                  onDrag = { change, dragAmount ->
                    change.consume()
                    dragOffsetX += dragAmount.x
                    dragOffsetY += dragAmount.y
                    val current =
                      gridState.layoutInfo.visibleItemsInfo.firstOrNull {
                        it.index == draggedIndex
                      }
                    if (current != null) {
                      val centerX = current.offset.x + current.size.width / 2f + dragOffsetX
                      val centerY = current.offset.y + current.size.height / 2f + dragOffsetY
                      val target =
                        gridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                          item.index != draggedIndex &&
                            centerX.toInt() in item.offset.x..(item.offset.x + item.size.width) &&
                            centerY.toInt() in item.offset.y..(item.offset.y + item.size.height)
                        }
                      if (target != null) {
                        viewModel.reorderVisibleCards(draggedIndex, target.index)
                        draggedIndex = target.index
                        dragOffsetX = 0f
                        dragOffsetY = 0f
                      }
                    }
                  },
                  onDragEnd = {
                    draggedIndex = -1
                    dragOffsetX = 0f
                    dragOffsetY = 0f
                  },
                  onDragCancel = {
                    draggedIndex = -1
                    dragOffsetX = 0f
                    dragOffsetY = 0f
                  },
                )
              },
        ) {
          itemsIndexed(
            items = uiState.cards,
            key = { _, card -> card.cardId },
            span = { _, card ->
              val size = CardSize.fromName(card.cardSize)
              val spanCols = if (size == CardSize.COMPACT) 1 else minOf(2, columnCount)
              GridItemSpan(spanCols)
            },
          ) { index, card ->
            val isDragging = index == draggedIndex
            val cardModifier =
              Modifier.graphicsLayer {
                if (isDragging) {
                  translationX = dragOffsetX
                  translationY = dragOffsetY
                  scaleX = 1.03f
                  scaleY = 1.03f
                  alpha = 0.92f
                }
              }

            BentoDashboardCard(
              card = card,
              uiState = uiState,
              modifier = cardModifier,
              onOpenWeather = onOpenWeather,
              onOpenBattery = onOpenBattery,
              onOpenMusic = onOpenMusic,
              onOpenDeviceSection = onOpenDeviceSection,
              onOpenClockTab = onOpenClockTab,
              onOpenCalendar = onOpenCalendar,
              onRequestCalendarPermission = {
                calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
              },
              onOpenNotes = onOpenNotes,
              onOpenTodo = onOpenTodo,
              onOpenCalculator = onOpenCalculator,
              onToggleMediaPlayPause = viewModel::toggleMediaPlayPause,
              onSkipNext = viewModel::skipNext,
              onSkipPrevious = viewModel::skipPrevious,
              onOpenNotificationAccess = viewModel::openNotificationAccessSettings,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun BentoDashboardCard(
  card: DashboardCardEntity,
  uiState: HomeUiState,
  modifier: Modifier = Modifier,
  onOpenWeather: () -> Unit,
  onOpenBattery: () -> Unit,
  onOpenMusic: () -> Unit,
  onOpenDeviceSection: (String) -> Unit,
  onOpenClockTab: (String) -> Unit,
  onOpenCalendar: () -> Unit,
  onRequestCalendarPermission: () -> Unit,
  onOpenNotes: () -> Unit,
  onOpenTodo: () -> Unit,
  onOpenCalculator: () -> Unit,
  onToggleMediaPlayPause: () -> Unit,
  onSkipNext: () -> Unit,
  onSkipPrevious: () -> Unit,
  onOpenNotificationAccess: () -> Unit,
) {
  val size = CardSize.fromName(card.cardSize)
  when (DashboardCardType.fromId(card.cardId)) {
    DashboardCardType.CLOCK ->
      ClockBentoCard(
        nowMillis = uiState.currentTimeMillis,
        showSeconds = uiState.showClockSeconds,
        showMillis = uiState.showClockMillis,
        use24Hour = uiState.use24HourClock,
        size = size,
        modifier = modifier.testTag("home_card_clock"),
        onClick = { onOpenClockTab("clock") },
      )
    DashboardCardType.WEATHER ->
      WeatherBentoCard(
        uiState = uiState,
        size = size,
        modifier = modifier.testTag("home_card_weather"),
        onClick = onOpenWeather,
      )
    DashboardCardType.BATTERY ->
      BatteryBentoCard(
        uiState = uiState,
        size = size,
        modifier = modifier.testTag("home_card_battery"),
        onClick = onOpenBattery,
      )
    DashboardCardType.NOW_PLAYING ->
      NowPlayingBentoCard(
        uiState = uiState,
        modifier = modifier.testTag("home_card_now_playing"),
        onClick = onOpenMusic,
        onTogglePlayPause = onToggleMediaPlayPause,
        onNext = onSkipNext,
        onPrevious = onSkipPrevious,
        onOpenNotificationAccess = onOpenNotificationAccess,
      )
    DashboardCardType.RAM ->
      RamBentoCard(
        uiState = uiState,
        size = size,
        modifier = modifier.testTag("home_card_ram"),
        onClick = { onOpenDeviceSection("ram") },
      )
    DashboardCardType.STORAGE ->
      StorageBentoCard(
        uiState = uiState,
        size = size,
        modifier = modifier.testTag("home_card_storage"),
        onClick = { onOpenDeviceSection("storage") },
      )
    DashboardCardType.QUICK_ACTIONS ->
      QuickActionsBentoCard(
        size = size,
        modifier = modifier.testTag("home_card_quick_actions"),
        onOpenNotes = onOpenNotes,
        onOpenTodo = onOpenTodo,
        onOpenCalculator = onOpenCalculator,
        onOpenTimer = { onOpenClockTab("timer") },
      )
    DashboardCardType.CALENDAR ->
      CalendarBentoCard(
        uiState = uiState,
        modifier = modifier.testTag("home_card_calendar"),
        onClick = onOpenCalendar,
        onRequestPermission = onRequestCalendarPermission,
      )
  }
}

@Composable
private fun ClockBentoCard(
  nowMillis: Long,
  showSeconds: Boolean,
  showMillis: Boolean,
  use24Hour: Boolean,
  size: CardSize,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val formattedTime =
    Formatters.formatClockTime(
      epochMillis = nowMillis,
      use24Hour = use24Hour,
      showSeconds = showSeconds,
      showMillis = showMillis,
    )
  val dateFmt = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

  val compactStyle =
    if (showMillis) JaBoardMonoStyles.MetricReadout.copy(fontSize = 14.sp, lineHeight = 18.sp)
    else if (!use24Hour) JaBoardMonoStyles.MetricReadout.copy(fontSize = 16.sp, lineHeight = 20.sp)
    else JaBoardMonoStyles.MetricReadout

  val wideStyle =
    when {
      showMillis && !use24Hour ->
        JaBoardMonoStyles.HeroNumeric.copy(fontSize = 23.sp, lineHeight = 28.sp)
      showMillis -> JaBoardMonoStyles.HeroNumeric.copy(fontSize = 26.sp, lineHeight = 32.sp)
      !use24Hour -> JaBoardMonoStyles.HeroNumeric.copy(fontSize = 30.sp, lineHeight = 36.sp)
      else -> JaBoardMonoStyles.HeroNumeric
    }

  ExpressiveCard(
    modifier = modifier.fillMaxWidth(),
    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
    onClick = onClick,
  ) {
    if (size == CardSize.COMPACT) {
      Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
          text = formattedTime,
          style = compactStyle,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.testTag("home_clock_time_text"),
        )
        Text(
          dateFmt.format(Date(nowMillis)),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      Row(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(
          modifier = Modifier.weight(1f).padding(end = 12.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = dateFmt.format(Date(nowMillis)),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = formattedTime,
            style = wideStyle,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("home_clock_time_text"),
          )
        }
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.7f),
        ) {
          Icon(
            imageVector = Icons.Rounded.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(12.dp).size(28.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun WeatherBentoCard(
  uiState: HomeUiState,
  size: CardSize,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val report = uiState.weatherReport
  ExpressiveCard(
    modifier = modifier.fillMaxWidth(),
    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
    onClick = onClick,
  ) {
    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      if (uiState.savedLocation == null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Rounded.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Weather Setup", style = MaterialTheme.typography.titleMedium)
        }
        Text(
          "Choose a city or allow approximate GPS to view live Open-Meteo forecasts.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilledTonalButton(onClick = onClick) { Text("Set Location") }
      } else if (report == null) {
        Text(uiState.savedLocation.cityName, style = MaterialTheme.typography.titleMedium)
        Text(
          "Tap to load latest forecast",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              report.cityName,
              style = MaterialTheme.typography.titleMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              report.description,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = weatherIconForCode(report.weatherCode),
              contentDescription = report.description,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(30.dp),
            )
            Text(
              Formatters.formatTemperature(report.tempC, uiState.temperatureUnit),
              style = MaterialTheme.typography.headlineLarge,
            )
          }
        }
        if (size == CardSize.EXPANDED && report.hourly.isNotEmpty()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            report.hourly.take(5).forEach { hr ->
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(hr.label, style = MaterialTheme.typography.labelSmall)
                Icon(
                  weatherIconForCode(hr.weatherCode),
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                  tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                  Formatters.formatShortTemp(hr.tempC, uiState.temperatureUnit),
                  style = MaterialTheme.typography.labelMedium,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun BatteryBentoCard(
  uiState: HomeUiState,
  size: CardSize,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val bat = uiState.battery
  val pct = bat?.levelPercent ?: 0
  val tint =
    when {
      bat?.isCharging == true -> JaBoardStatusColors.HealthyGreen
      pct <= 15 -> JaBoardStatusColors.CriticalRed
      else -> MaterialTheme.colorScheme.primary
    }

  ExpressiveCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          if (bat?.isCharging == true) Icons.Rounded.BatteryChargingFull
          else Icons.Rounded.BatteryFull,
          contentDescription = null,
          tint = tint,
        )
        Text(
          bat?.statusLabel ?: "Battery",
          style = MaterialTheme.typography.labelSmall,
          color = tint,
        )
      }
      Text("$pct%", style = JaBoardMonoStyles.HeroNumeric)
      LinearProgressIndicator(
        progress = { pct / 100f },
        modifier = Modifier.fillMaxWidth().height(7.dp),
        strokeCap = StrokeCap.Round,
        color = tint,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
      )
      if (size != CardSize.COMPACT && bat != null) {
        Text(
          "${bat.chargingSourceLabel} • ${bat.temperatureCelsius?.let { String.format(Locale.US, "%.1f°C", it) } ?: bat.healthLabel}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun RamBentoCard(
  uiState: HomeUiState,
  size: CardSize,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val ram = uiState.ram
  val pct = ((ram?.usedFraction ?: 0f) * 100).roundToInt()

  ExpressiveCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Rounded.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Text("RAM", style = MaterialTheme.typography.labelSmall)
      }
      Text("$pct%", style = JaBoardMonoStyles.HeroNumeric)
      LinearProgressIndicator(
        progress = { ram?.usedFraction ?: 0f },
        modifier = Modifier.fillMaxWidth().height(7.dp),
        strokeCap = StrokeCap.Round,
        color = MaterialTheme.colorScheme.secondary,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
      )
      if (ram != null) {
        Text(
          if (size == CardSize.COMPACT) "${Formatters.formatBytes(ram.availableBytes)} free"
          else "${Formatters.formatBytes(ram.usedBytes)} / ${Formatters.formatBytes(ram.totalBytes)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun StorageBentoCard(
  uiState: HomeUiState,
  size: CardSize,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val storage = uiState.storage
  val pct = ((storage?.usedFraction ?: 0f) * 100).roundToInt()

  ExpressiveCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          Icons.Rounded.SdStorage,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.tertiary,
        )
        Text("Storage", style = MaterialTheme.typography.labelSmall)
      }
      Text("$pct%", style = JaBoardMonoStyles.HeroNumeric)
      LinearProgressIndicator(
        progress = { storage?.usedFraction ?: 0f },
        modifier = Modifier.fillMaxWidth().height(7.dp),
        strokeCap = StrokeCap.Round,
        color = MaterialTheme.colorScheme.tertiary,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
      )
      if (storage != null) {
        Text(
          if (size == CardSize.COMPACT) "${Formatters.formatBytes(storage.availableBytes)} free"
          else "${Formatters.formatBytes(storage.usedBytes)} / ${Formatters.formatBytes(storage.totalBytes)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun NowPlayingBentoCard(
  uiState: HomeUiState,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onNext: () -> Unit,
  onPrevious: () -> Unit,
  onOpenNotificationAccess: () -> Unit,
) {
  val media = uiState.media
  val session = media.activeSession

  ExpressiveCard(
    modifier = modifier.fillMaxWidth(),
    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
    onClick = onClick,
  ) {
    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      if (!media.hasNotificationAccess) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Now Playing", style = MaterialTheme.typography.titleMedium)
            Text(
              "Enable Notification Access to control active music apps.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          FilledTonalButton(onClick = onOpenNotificationAccess) { Text("Enable") }
        }
      } else if (session == null) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
          )
          Column {
            Text("No media playing", style = MaterialTheme.typography.titleMedium)
            Text(
              "Start playback in any music app to control it here.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(54.dp),
          ) {
            if (session.albumArt != null) {
              Image(
                bitmap = session.albumArt.asImageBitmap(),
                contentDescription = session.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
              )
            } else {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null)
              }
            }
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              session.title,
              style = MaterialTheme.typography.titleMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              "${session.artist} • ${session.appLabel}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) {
              Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous")
            }
            FilledTonalIconButton(onClick = onTogglePlayPause) {
              Icon(
                if (session.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (session.isPlaying) "Pause" else "Play",
              )
            }
            IconButton(onClick = onNext) {
              Icon(Icons.Rounded.SkipNext, contentDescription = "Next")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun QuickActionsBentoCard(
  size: CardSize,
  modifier: Modifier = Modifier,
  onOpenNotes: () -> Unit,
  onOpenTodo: () -> Unit,
  onOpenCalculator: () -> Unit,
  onOpenTimer: () -> Unit,
) {
  ExpressiveCard(modifier = modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text("Quick Actions", style = MaterialTheme.typography.labelLarge)
      if (size == CardSize.COMPACT) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          QuickActionChip(
            icon = Icons.Rounded.Description,
            label = "Notes",
            modifier = Modifier.weight(1f),
            onClick = onOpenNotes,
          )
          QuickActionChip(
            icon = Icons.Rounded.CheckBox,
            label = "To-Do",
            modifier = Modifier.weight(1f),
            onClick = onOpenTodo,
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          QuickActionChip(
            icon = Icons.Rounded.Calculate,
            label = "Calc",
            modifier = Modifier.weight(1f),
            onClick = onOpenCalculator,
          )
          QuickActionChip(
            icon = Icons.Rounded.Timer,
            label = "Timer",
            modifier = Modifier.weight(1f),
            onClick = onOpenTimer,
          )
        }
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          QuickActionChip(
            icon = Icons.Rounded.Description,
            label = "Notes",
            modifier = Modifier.weight(1f),
            onClick = onOpenNotes,
          )
          QuickActionChip(
            icon = Icons.Rounded.CheckBox,
            label = "To-Do",
            modifier = Modifier.weight(1f),
            onClick = onOpenTodo,
          )
          QuickActionChip(
            icon = Icons.Rounded.Calculate,
            label = "Calc",
            modifier = Modifier.weight(1f),
            onClick = onOpenCalculator,
          )
          QuickActionChip(
            icon = Icons.Rounded.Timer,
            label = "Timer",
            modifier = Modifier.weight(1f),
            onClick = onOpenTimer,
          )
        }
      }
    }
  }
}

@Composable
private fun QuickActionChip(
  icon: ImageVector,
  label: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    modifier = modifier.height(50.dp),
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
      Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
  }
}

@Composable
private fun CalendarBentoCard(
  uiState: HomeUiState,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
  onRequestPermission: () -> Unit,
) {
  val timeFmt = remember { SimpleDateFormat("EEE h:mm a", Locale.getDefault()) }

  ExpressiveCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Rounded.CalendarMonth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Upcoming Events", style = MaterialTheme.typography.titleMedium)
        }
        Icon(
          Icons.Rounded.DragIndicator,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
          modifier = Modifier.size(18.dp),
        )
      }

      if (!uiState.hasCalendarPermission) {
        Text(
          "Calendar access is optional. Grant permission to see your upcoming schedule.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onRequestPermission) { Text("Allow Calendar") }
      } else if (uiState.upcomingEvents.isEmpty()) {
        Text(
          "No upcoming events in the next 7 days.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        uiState.upcomingEvents.take(3).forEach { ev ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              ev.title,
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              if (ev.allDay) "All day" else timeFmt.format(Date(ev.startMillis)),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}
