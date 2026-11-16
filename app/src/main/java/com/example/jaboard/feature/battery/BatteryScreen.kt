package com.example.jaboard.feature.battery

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jaboard.core.design.ExpressiveCard
import com.example.jaboard.core.design.JaBoardMonoStyles
import com.example.jaboard.core.design.JaBoardStatusColors
import com.example.jaboard.core.design.JaBoardUtilityTopBar
import com.example.jaboard.core.design.KeyValueRow
import com.example.jaboard.core.design.MetricProgressRing
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryScreen(viewModel: BatteryViewModel, onBack: () -> Unit) {
  val battery by viewModel.batteryState.collectAsStateWithLifecycle()

  val ringColor =
    when {
      battery.isCharging -> JaBoardStatusColors.HealthyGreen
      battery.levelPercent <= 15 -> JaBoardStatusColors.CriticalRed
      battery.levelPercent <= 30 -> JaBoardStatusColors.WarningAmber
      else -> MaterialTheme.colorScheme.primary
    }

  Scaffold(
    topBar = {
      JaBoardUtilityTopBar(
        title = "Battery Monitor",
        subtitle = "Live Android BatteryManager telemetry",
        onBack = onBack,
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding).testTag("battery_screen_list"),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        ExpressiveCard(
          modifier = Modifier.fillMaxWidth(),
          containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
          ) {
            MetricProgressRing(
              progress = battery.levelPercent / 100f,
              size = 116.dp,
              strokeWidth = 11.dp,
              activeColor = ringColor,
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  imageVector =
                    if (battery.isCharging) Icons.Rounded.BatteryChargingFull
                    else Icons.Rounded.BatteryFull,
                  contentDescription = null,
                  tint = ringColor,
                  modifier = Modifier.size(22.dp),
                )
                Text(
                  text = "${battery.levelPercent}%",
                  style = JaBoardMonoStyles.HeroNumeric.copy(fontWeight = FontWeight.Bold),
                )
              }
            }

            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Surface(
                shape = MaterialTheme.shapes.small,
                color = ringColor.copy(alpha = 0.14f),
                contentColor = ringColor,
              ) {
                Text(
                  text = battery.statusLabel,
                  style = MaterialTheme.typography.labelLarge,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }
              Text(
                text = battery.chargingSourceLabel,
                style = MaterialTheme.typography.headlineSmall,
              )
              Text(
                text =
                  if (battery.isCharging) "Receiving power from ${battery.chargingSourceLabel}"
                  else "Discharging on internal cell",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          BatteryMiniTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.DeviceThermostat,
            label = "Temperature",
            value =
              battery.temperatureCelsius?.let {
                String.format(Locale.US, "%.1f °C", it)
              } ?: "Unavailable",
            tint = MaterialTheme.colorScheme.tertiary,
          )
          BatteryMiniTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.ElectricBolt,
            label = "Voltage",
            value = battery.voltageMilliVolts?.let { "$it mV" } ?: "Unavailable",
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          BatteryMiniTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.HealthAndSafety,
            label = "Health",
            value = battery.healthLabel,
            tint = JaBoardStatusColors.HealthyGreen,
          )
          BatteryMiniTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.Memory,
            label = "Chemistry",
            value = battery.technology ?: "Not reported",
            tint = MaterialTheme.colorScheme.secondary,
          )
        }
      }

      item {
        ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Rounded.Power,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Detailed Cell Diagnostics", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            KeyValueRow("Charging state", battery.statusLabel)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            KeyValueRow("Power source", battery.chargingSourceLabel)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            KeyValueRow(
              "Live current",
              battery.currentMicroAmps?.let { "${it / 1000} mA" } ?: "Not exposed by device",
              monoValue = true,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            KeyValueRow(
              "Voltage",
              battery.voltageMilliVolts?.let {
                String.format(Locale.US, "%.3f V (%d mV)", it / 1000f, it)
              } ?: "Not reported",
              monoValue = true,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            KeyValueRow(
              "Temperature",
              battery.temperatureCelsius?.let { String.format(Locale.US, "%.1f °C", it) }
                ?: "Not reported",
              monoValue = true,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            KeyValueRow("Health status", battery.healthLabel)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            KeyValueRow("Battery technology", battery.technology ?: "Not reported")
          }
        }
      }

      item {
        ExpressiveCard(
          modifier = Modifier.fillMaxWidth(),
          containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Bolt,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text =
                "All metrics above are read directly from Android's BatteryManager and system broadcast intents. Values omitted by your device hardware are shown as not reported.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun BatteryMiniTile(
  icon: ImageVector,
  label: String,
  value: String,
  tint: Color,
  modifier: Modifier = Modifier,
) {
  ExpressiveCard(modifier = modifier) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(imageVector = icon, contentDescription = null, tint = tint)
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = value,
        style = JaBoardMonoStyles.MetricReadout,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}
