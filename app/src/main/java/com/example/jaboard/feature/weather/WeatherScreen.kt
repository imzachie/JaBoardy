package com.example.jaboard.feature.weather

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.EditLocationAlt
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.jaboard.core.common.Formatters
import com.example.jaboard.core.common.TemperatureUnit
import com.example.jaboard.core.design.ExpressiveCard
import com.example.jaboard.core.design.JaBoardMonoStyles
import com.example.jaboard.core.design.JaBoardStatusColors
import com.example.jaboard.core.design.JaBoardUtilityTopBar
import com.example.jaboard.core.network.DailyForecastItem
import com.example.jaboard.core.network.HourlyForecastItem
import com.example.jaboard.core.network.WeatherReport
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(viewModel: WeatherViewModel, onBack: () -> Unit) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var showLocationPicker by rememberSaveable { mutableStateOf(false) }

  val locationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        viewModel.useCurrentDeviceLocation()
        showLocationPicker = false
      }
    }

  LaunchedEffect(uiState.savedLocation?.key) {
    if (uiState.savedLocation != null && uiState.report == null) {
      viewModel.refreshIfConfigured()
    }
  }

  // Automatically trigger AI summary generation when report is loaded
  LaunchedEffect(uiState.report?.fetchedAt) {
    if (uiState.report != null && uiState.aiSummary == null && !uiState.isGeneratingAiSummary) {
      viewModel.generateAiSummary()
    }
  }

  Scaffold(
    topBar = {
      JaBoardUtilityTopBar(
        title = "Weather",
        subtitle = uiState.savedLocation?.cityName ?: "Location not configured",
        onBack = onBack,
        actions = {
          TextButton(onClick = viewModel::toggleTemperatureUnit) {
            Text(
              if (uiState.temperatureUnit == TemperatureUnit.CELSIUS) "°C" else "°F",
              fontWeight = FontWeight.Bold,
            )
          }
          IconButton(
            onClick = { showLocationPicker = true },
            modifier = Modifier.testTag("weather_change_location_button"),
          ) {
            Icon(Icons.Rounded.EditLocationAlt, contentDescription = "Change city")
          }
          if (uiState.savedLocation != null) {
            IconButton(
              onClick = viewModel::refreshIfConfigured,
              modifier = Modifier.testTag("weather_refresh_button"),
            ) {
              Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.cd_refresh))
            }
          }
        },
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      if (uiState.isLoading || uiState.isResolvingGps) {
        item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
      }

      if (!uiState.errorMessage.isNullOrBlank()) {
        item {
          ExpressiveCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
          ) {
            Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = uiState.errorMessage!!,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
              )
              if (uiState.savedLocation != null) {
                TextButton(onClick = viewModel::refreshIfConfigured) { Text("Retry") }
              }
            }
          }
        }
      }

      if (uiState.savedLocation == null) {
        item {
          WeatherLocationSetupCard(
            onSearchCity = { showLocationPicker = true },
            onUseGps = {
              if (viewModel.hasLocationPermission()) {
                viewModel.useCurrentDeviceLocation()
              } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
              }
            },
          )
        }
      } else if (uiState.report != null) {
        val report = uiState.report!!

        // 1. Pixel Weather Hero Card
        item {
          PixelWeatherHeroCard(report = report, unit = uiState.temperatureUnit)
        }

        // 2. Pixel Weather AI Summary Card (Gemini)
        item {
          PixelWeatherAiSummaryCard(
            summary = uiState.aiSummary,
            isGenerating = uiState.isGeneratingAiSummary,
            onRegenerate = viewModel::generateAiSummary,
          )
        }

        // 3. Hourly Forecast (24-Hour Carousel)
        if (report.hourly.isNotEmpty()) {
          item {
            PixelHourlyForecastSection(report = report, unit = uiState.temperatureUnit)
          }
        }

        // 4. 10-Day Daily Forecast with Range Bars
        if (report.daily.isNotEmpty()) {
          item {
            Pixel10DayForecastSection(report = report, unit = uiState.temperatureUnit)
          }
        }

        // 5. Pixel Weather Bento Grid Tiles
        item {
          PixelWeatherBentoGrid(report = report, unit = uiState.temperatureUnit)
        }
      } else if (!uiState.isLoading) {
        item {
          ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Text("Forecast unavailable offline", style = MaterialTheme.typography.titleMedium)
              Text(
                "No cached forecast is saved for ${uiState.savedLocation?.cityName} yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Button(onClick = viewModel::refreshIfConfigured) { Text("Fetch Forecast") }
            }
          }
        }
      }
    }
  }

  if (showLocationPicker) {
    ModalBottomSheet(onDismissRequest = { showLocationPicker = false }) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text("Choose Weather Location", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
          value = uiState.searchQuery,
          onValueChange = viewModel::onSearchQueryChanged,
          modifier = Modifier.fillMaxWidth().testTag("weather_city_search_input"),
          placeholder = { Text("Search city (e.g. Kuala Lumpur, Tokyo, London)") },
          leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
          trailingIcon = {
            if (uiState.searchQuery.isNotEmpty()) {
              IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.cd_clear_search))
              }
            }
          },
          singleLine = true,
        )

        FilledTonalButton(
          onClick = {
            if (viewModel.hasLocationPermission()) {
              viewModel.useCurrentDeviceLocation()
              showLocationPicker = false
            } else {
              locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
          },
          modifier = Modifier.fillMaxWidth().testTag("weather_use_gps_button"),
        ) {
          Icon(Icons.Rounded.MyLocation, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Use approximate device location")
        }

        if (uiState.isSearchingCities) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
          ) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
          }
        } else {
          uiState.searchResults.forEach { place ->
            Surface(
              modifier =
                Modifier.fillMaxWidth().clickable {
                  viewModel.selectCity(place)
                  showLocationPicker = false
                },
              shape = MaterialTheme.shapes.small,
              color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  Icons.Rounded.LocationOn,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(place.name, style = MaterialTheme.typography.titleSmall)
                  Text(
                    listOfNotNull(place.admin1, place.country).joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

@Composable
fun WeatherLocationSetupCard(
  onSearchCity: () -> Unit,
  onUseGps: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ExpressiveCard(
    modifier = modifier.fillMaxWidth(),
    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
  ) {
    Column(
      modifier = Modifier.padding(22.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          Icons.Rounded.LocationOn,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Set your weather location", style = MaterialTheme.typography.titleLarge)
      }
      Text(
        "Search for any city manually or grant approximate location access to fetch real Open-Meteo forecasts. Cached offline automatically.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = onSearchCity, modifier = Modifier.testTag("weather_setup_search_button")) {
          Text("Choose City")
        }
        FilledTonalButton(
          onClick = onUseGps,
          modifier = Modifier.testTag("weather_setup_gps_button"),
        ) {
          Text("Use GPS")
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 1. Pixel Weather Hero Card
// -------------------------------------------------------------
@Composable
private fun PixelWeatherHeroCard(report: WeatherReport, unit: TemperatureUnit) {
  val today = report.daily.firstOrNull()

  ExpressiveCard(
    modifier = Modifier.fillMaxWidth(),
    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
  ) {
    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = report.cityName,
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
          )
          if (report.country.isNotBlank()) {
            Text(
              text = report.country,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Icon(
          imageVector = weatherIconForCode(report.weatherCode),
          contentDescription = report.description,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(56.dp),
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Text(
          text = Formatters.formatTemperature(report.tempC, unit),
          style = MaterialTheme.typography.displayLarge.copy(fontSize = 68.sp, fontWeight = FontWeight.Bold),
        )
        Column(modifier = Modifier.padding(bottom = 10.dp)) {
          Text(
            text = report.description,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
          )
          Text(
            text = "Feels like ${Formatters.formatTemperature(report.feelsLikeC, unit)}" +
              (today?.let {
                " • H ${Formatters.formatShortTemp(it.maxTempC, unit)} / L ${Formatters.formatShortTemp(it.minTempC, unit)}"
              }.orEmpty()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Text(
        text = "Updated ${Formatters.formatRelativeTimestamp(report.fetchedAt)}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

// -------------------------------------------------------------
// 2. Pixel Weather AI Summary Card (Gemini)
// -------------------------------------------------------------
@Composable
private fun PixelWeatherAiSummaryCard(
  summary: String?,
  isGenerating: Boolean,
  onRegenerate: () -> Unit,
) {
  ExpressiveCard(
    modifier = Modifier.fillMaxWidth().testTag("weather_ai_summary_card"),
    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
  ) {
    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
          ) {
            Icon(
              imageVector = Icons.Rounded.AutoAwesome,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(6.dp).size(18.dp),
            )
          }
          Column {
            Text(
              text = "AI Weather Summary",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Text(
              text = "Powered by Gemini",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }

        IconButton(
          onClick = onRegenerate,
          enabled = !isGenerating,
          modifier = Modifier.testTag("weather_ai_regenerate_button"),
        ) {
          if (isGenerating) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
          } else {
            Icon(
              imageVector = Icons.Rounded.Refresh,
              contentDescription = "Regenerate AI summary",
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      AnimatedVisibility(
        visible = isGenerating,
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)))
          Text(
            text = "Analyzing conditions with Gemini AI...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (!summary.isNullOrBlank() && !isGenerating) {
        Text(
          text = summary,
          style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
          color = MaterialTheme.colorScheme.onSurface,
        )
      } else if (!isGenerating) {
        Text(
          text = "Tap refresh to generate an AI summary of today's conditions.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

// -------------------------------------------------------------
// 3. Hourly Forecast Section
// -------------------------------------------------------------
@Composable
private fun PixelHourlyForecastSection(report: WeatherReport, unit: TemperatureUnit) {
  ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(vertical = 18.dp)) {
      Text(
        "Hourly Forecast",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(horizontal = 20.dp),
      )
      Spacer(modifier = Modifier.height(12.dp))
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(report.hourly) { item ->
          val isNow = item.label == "Now"
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (isNow) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
          ) {
            Column(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = item.label,
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = if (isNow) FontWeight.Bold else FontWeight.Normal,
                ),
              )
              Icon(
                imageVector = weatherIconForCode(item.weatherCode),
                contentDescription = null,
                tint = if (isNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
              )
              Text(
                text = Formatters.formatShortTemp(item.tempC, unit),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              )
              if (item.rainProbability > 0) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                ) {
                  Text(
                    text = "${item.rainProbability}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
              } else {
                Spacer(modifier = Modifier.height(18.dp))
              }
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 4. 10-Day Daily Forecast with Pixel Range Bars
// -------------------------------------------------------------
@Composable
private fun Pixel10DayForecastSection(report: WeatherReport, unit: TemperatureUnit) {
  val daily = report.daily
  val globalMin = daily.minOfOrNull { it.minTempC } ?: 0.0
  val globalMax = daily.maxOfOrNull { it.maxTempC } ?: 35.0
  val rangeSpan = (globalMax - globalMin).coerceAtLeast(1.0)

  ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text(
        "${daily.size}-Day Outlook",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      )
      Spacer(modifier = Modifier.height(4.dp))

      daily.forEachIndexed { idx, day ->
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          // Day label
          Text(
            text = day.dayLabel,
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Medium,
            ),
            modifier = Modifier.width(68.dp),
          )

          // Condition icon + rain prob
          Row(
            modifier = Modifier.width(52.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = weatherIconForCode(day.weatherCode),
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
            if (day.rainProbability > 15) {
              Text(
                text = "${day.rainProbability}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
              )
            }
          }

          // Min Temp
          Text(
            text = Formatters.formatShortTemp(day.minTempC, unit),
            style = JaBoardMonoStyles.SpecValue,
            modifier = Modifier.width(36.dp),
          )

          // Pixel Temperature Range Bar
          val startFrac = ((day.minTempC - globalMin) / rangeSpan).toFloat().coerceIn(0f, 1f)
          val endFrac = ((day.maxTempC - globalMin) / rangeSpan).toFloat().coerceIn(0f, 1f)

          Box(
            modifier = Modifier
              .weight(1f)
              .height(8.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(MaterialTheme.colorScheme.surfaceContainerHighest),
          ) {
            // Colored range pill
            Box(
              modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(endFrac)
                .padding(start = ((startFrac * 100).coerceAtMost(90f)).dp) // visual representation
                .clip(RoundedCornerShape(4.dp))
                .background(
                  Brush.horizontalGradient(
                    colors = listOf(
                      Color(0xFF64B5F6),
                      Color(0xFFFFB74D),
                    ),
                  )
                ),
            )
          }

          // Max Temp
          Text(
            text = Formatters.formatShortTemp(day.maxTempC, unit),
            style = JaBoardMonoStyles.SpecValue.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.width(36.dp),
          )
        }

        if (idx < daily.lastIndex) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 5. Pixel Weather Bento Grid Tiles
// -------------------------------------------------------------
@Composable
private fun PixelWeatherBentoGrid(report: WeatherReport, unit: TemperatureUnit) {
  val uv = report.uvIndexNow ?: 0.0
  val uvRating = when {
    uv < 3.0 -> "Low" to JaBoardStatusColors.HealthyGreen
    uv < 6.0 -> "Moderate" to JaBoardStatusColors.WarningAmber
    uv < 8.0 -> "High" to Color(0xFFFF9800)
    uv < 11.0 -> "Very High" to MaterialTheme.colorScheme.error
    else -> "Extreme" to Color(0xFF9C27B0)
  }

  val windDeg = report.windDirectionDeg ?: 0
  val windCardinal = windDirectionCardinal(windDeg)

  val dewPointC = report.tempC - ((100 - report.humidityPercent) / 5.0)

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(
      text = "Current Conditions",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
    )

    // Row 1: UV Index & Wind
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // UV Index Tile
      PixelBentoTile(
        modifier = Modifier.weight(1f),
        icon = Icons.Rounded.WbSunny,
        title = "UV Index",
        value = String.format("%.1f", uv),
        subtitle = uvRating.first,
        accentColor = uvRating.second,
      ) {
        LinearProgressIndicator(
          progress = { (uv / 12.0).toFloat().coerceIn(0f, 1f) },
          modifier = Modifier.fillMaxWidth().height(6.dp),
          strokeCap = StrokeCap.Round,
          color = uvRating.second,
          trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
      }

      // Wind Tile
      PixelBentoTile(
        modifier = Modifier.weight(1f),
        icon = Icons.Rounded.Air,
        title = "Wind",
        value = "${report.windSpeedKmh.roundToInt()} km/h",
        subtitle = "$windCardinal (${windDeg}°)",
        accentColor = MaterialTheme.colorScheme.primary,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Navigation,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp).rotate(windDeg.toFloat()),
          )
          Text(
            text = "Wind Direction",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    // Row 2: Humidity & Precipitation
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Humidity Tile
      PixelBentoTile(
        modifier = Modifier.weight(1f),
        icon = Icons.Rounded.WaterDrop,
        title = "Humidity",
        value = "${report.humidityPercent}%",
        subtitle = "Dew point ${Formatters.formatShortTemp(dewPointC, unit)}",
        accentColor = MaterialTheme.colorScheme.tertiary,
      )

      // Rain Probability Tile
      PixelBentoTile(
        modifier = Modifier.weight(1f),
        icon = Icons.Rounded.Umbrella,
        title = "Precipitation",
        value = report.rainProbabilityNow?.let { "$it%" } ?: "0%",
        subtitle = if ((report.rainProbabilityNow ?: 0) >= 40) "Rain likely" else "No rain expected",
        accentColor = MaterialTheme.colorScheme.tertiary,
      )
    }

    // Row 3: Visibility & Pressure
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Visibility Tile
      PixelBentoTile(
        modifier = Modifier.weight(1f),
        icon = Icons.Rounded.Visibility,
        title = "Visibility",
        value = report.visibilityKm?.let { String.format("%.1f km", it) } ?: "10 km",
        subtitle = if ((report.visibilityKm ?: 10.0) >= 9.0) "Clear visibility" else "Reduced visibility",
        accentColor = MaterialTheme.colorScheme.secondary,
      )

      // Pressure Tile
      PixelBentoTile(
        modifier = Modifier.weight(1f),
        icon = Icons.Rounded.Compress,
        title = "Pressure",
        value = "${report.surfacePressureHpa?.roundToInt() ?: 1013} hPa",
        subtitle = "Normal atmospheric",
        accentColor = MaterialTheme.colorScheme.secondary,
      )
    }

    // Row 4: Sunrise & Sunset
    if (!report.sunriseToday.isNullOrBlank() || !report.sunsetToday.isNullOrBlank()) {
      ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          horizontalArrangement = Arrangement.SpaceAround,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.WbSunny,
              contentDescription = null,
              tint = Color(0xFFFFB74D),
              modifier = Modifier.size(28.dp),
            )
            Column {
              Text("Sunrise", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(report.sunriseToday ?: "--:--", style = JaBoardMonoStyles.SpecValue.copy(fontWeight = FontWeight.Bold))
            }
          }

          HorizontalDivider(
            modifier = Modifier.height(32.dp).width(1.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.WbTwilight,
              contentDescription = null,
              tint = Color(0xFFFF7043),
              modifier = Modifier.size(28.dp),
            )
            Column {
              Text("Sunset", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(report.sunsetToday ?: "--:--", style = JaBoardMonoStyles.SpecValue.copy(fontWeight = FontWeight.Bold))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PixelBentoTile(
  icon: ImageVector,
  title: String,
  value: String,
  subtitle: String,
  accentColor: Color,
  modifier: Modifier = Modifier,
  content: (@Composable () -> Unit)? = null,
) {
  ExpressiveCard(modifier = modifier) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Text(
        text = value,
        style = JaBoardMonoStyles.MetricReadout.copy(fontWeight = FontWeight.Bold),
      )

      if (content != null) {
        content()
      }

      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

fun windDirectionCardinal(degrees: Int): String {
  val normalized = (degrees % 360 + 360) % 360
  return when {
    normalized >= 338 || normalized < 23 -> "N"
    normalized < 68 -> "NE"
    normalized < 113 -> "E"
    normalized < 158 -> "SE"
    normalized < 203 -> "S"
    normalized < 248 -> "SW"
    normalized < 293 -> "W"
    else -> "NW"
  }
}

fun weatherIconForCode(code: Int): ImageVector =
  when (code) {
    0, 1 -> Icons.Rounded.WbSunny
    2 -> Icons.Rounded.WbCloudy
    3, 45, 48 -> Icons.Rounded.Cloud
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> Icons.Rounded.Umbrella
    71, 73, 75, 77, 85, 86 -> Icons.Rounded.AcUnit
    95, 96, 99 -> Icons.Rounded.Thunderstorm
    else -> Icons.Rounded.WbCloudy
  }
