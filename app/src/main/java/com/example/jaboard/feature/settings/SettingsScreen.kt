package com.example.jaboard.feature.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.jaboard.core.common.TemperatureUnit
import com.example.jaboard.core.design.ExpressiveCard
import com.example.jaboard.core.design.JaBoardLargeTopBar
import com.example.jaboard.core.design.JaBoardStatusColors
import com.example.jaboard.core.design.KeyValueRow
import com.example.jaboard.core.design.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel,
  onOpenStudio: () -> Unit,
  onOpenLicenses: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val lifecycleOwner = LocalLifecycleOwner.current
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  var showConfirmClearData by rememberSaveable { mutableStateOf(false) }

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
      viewModel.refreshPermissions()
    }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.refreshPermissions()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      JaBoardLargeTopBar(
        title = "Settings",
        subtitle = "Appearance, permissions & local privacy",
        scrollBehavior = scrollBehavior,
      )
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding).testTag("settings_list"),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      if (!uiState.statusBanner.isNullOrBlank()) {
        item {
          ExpressiveCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                uiState.statusBanner!!,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
              )
              TextButton(onClick = viewModel::dismissBanner) { Text("OK") }
            }
          }
        }
      }

      // Appearance
      item {
        ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            SectionTitle(Icons.Rounded.Palette, "Appearance")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
              ThemeMode.entries.forEachIndexed { idx, mode ->
                SegmentedButton(
                  selected = uiState.preferences.themeMode == mode,
                  onClick = { viewModel.setThemeMode(mode) },
                  shape = SegmentedButtonDefaults.itemShape(idx, ThemeMode.entries.size),
                ) {
                  Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                }
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("Dynamic Color", style = MaterialTheme.typography.titleSmall)
                Text(
                  "Material You wallpaper palette (Pixel Cobalt fallback)",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(
                checked = uiState.preferences.useDynamicColor,
                onCheckedChange = viewModel::setUseDynamicColor,
                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
              )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("Temperature unit", style = MaterialTheme.typography.titleSmall)
              SingleChoiceSegmentedButtonRow {
                TemperatureUnit.entries.forEachIndexed { idx, unit ->
                  SegmentedButton(
                    selected = uiState.preferences.temperatureUnit == unit,
                    onClick = { viewModel.setTemperatureUnit(unit) },
                    shape = SegmentedButtonDefaults.itemShape(idx, TemperatureUnit.entries.size),
                  ) {
                    Text(if (unit == TemperatureUnit.CELSIUS) "°C" else "°F")
                  }
                }
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("Clock format", style = MaterialTheme.typography.titleSmall)
              SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                  selected = !uiState.preferences.use24HourClock,
                  onClick = { viewModel.setUse24HourClock(false) },
                  shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) {
                  Text("12h")
                }
                SegmentedButton(
                  selected = uiState.preferences.use24HourClock,
                  onClick = { viewModel.setUse24HourClock(true) },
                  shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) {
                  Text("24h")
                }
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("Show seconds on Home clock", style = MaterialTheme.typography.titleSmall)
              Switch(
                checked = uiState.preferences.showClockSeconds,
                onCheckedChange = viewModel::setShowClockSeconds,
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("Show milliseconds on Home clock", style = MaterialTheme.typography.titleSmall)
              Switch(
                checked = uiState.preferences.showClockMillis,
                onCheckedChange = viewModel::setShowClockMillis,
              )
            }
          }
        }
      }

      // Dashboard Preferences
      item {
        ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(Icons.Rounded.Tune, "Dashboard Preferences")
            Row(
              modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenStudio).padding(vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("Customize in Studio", style = MaterialTheme.typography.titleSmall)
                Text(
                  "Current preset: ${uiState.preferences.activePreset.label}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clickable(onClick = viewModel::resetDashboardToDefault)
                  .padding(vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("Restore default Expressive Bento layout", style = MaterialTheme.typography.titleSmall)
              Text(
                "Reset",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }

      // Permissions
      item {
        ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            SectionTitle(Icons.Rounded.PrivacyTip, "Permissions")
            PermissionRow(
              icon = Icons.Rounded.LocationOn,
              title = "Approximate Location",
              subtitle = "Optional GPS city lookup for Weather",
              granted = uiState.permissions.locationGranted,
              onClick = {
                if (uiState.permissions.locationGranted) viewModel.openAppSettings()
                else permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
              },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            PermissionRow(
              icon = Icons.Rounded.CalendarMonth,
              title = "Read Calendar",
              subtitle = "Optional upcoming events widget",
              granted = uiState.permissions.calendarGranted,
              onClick = {
                if (uiState.permissions.calendarGranted) viewModel.openAppSettings()
                else permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
              },
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
              PermissionRow(
                icon = Icons.Rounded.Notifications,
                title = "Post Notifications",
                subtitle = "Alerts when countdown timers finish",
                granted = uiState.permissions.notificationsGranted,
                onClick = {
                  if (uiState.permissions.notificationsGranted) viewModel.openAppSettings()
                  else permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
              )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            PermissionRow(
              icon = Icons.Rounded.NotificationsActive,
              title = "Notification Listener Access",
              subtitle = "Required by Android for MediaSession playback controls",
              granted = uiState.permissions.notificationListenerGranted,
              onClick = viewModel::openNotificationListenerSettings,
            )
          }
        }
      }

      // Data and Privacy
      item {
        ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            SectionTitle(Icons.Rounded.Policy, "Data & Privacy")
            Text(
              "JaBoard operates offline-first with zero user accounts, zero analytics, and zero third-party ad SDKs. Notes, to-do lists, and dashboard settings remain strictly on your device.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clickable(onClick = viewModel::clearWeatherCacheAndLocation)
                  .padding(vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("Clear cached weather & saved city", style = MaterialTheme.typography.titleSmall)
              Text(
                "Clear",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clickable { showConfirmClearData = true }
                  .padding(vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                "Delete all local notes & to-do tasks",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
              )
              Icon(
                Icons.Rounded.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
              )
            }
          }
        }
      }

      // About & Licenses
      item {
        ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            SectionTitle(Icons.Rounded.Info, "About JaBoard")
            KeyValueRow("Application", "JaBoard")
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            KeyValueRow("Package ID", BuildConfig.APPLICATION_ID, monoValue = true)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            KeyValueRow(
              "Version",
              "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
              monoValue = true,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clickable(onClick = onOpenLicenses)
                  .padding(vertical = 8.dp)
                  .testTag("settings_licenses_row"),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("Open-source licenses", style = MaterialTheme.typography.titleSmall)
              Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
            }
          }
        }
      }
    }
  }

  if (showConfirmClearData) {
    AlertDialog(
      onDismissRequest = { showConfirmClearData = false },
      title = { Text("Delete all notes and tasks?") },
      text = {
        Text("This permanently removes all locally stored notes and to-do items from this device.")
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.clearAllPersonalProductivityData()
            showConfirmClearData = false
          }
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { showConfirmClearData = false }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun SectionTitle(icon: ImageVector, title: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.width(8.dp))
    Text(title, style = MaterialTheme.typography.titleMedium)
  }
}

@Composable
private fun PermissionRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  granted: Boolean,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.titleSmall)
      Text(
        subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Surface(
      shape = MaterialTheme.shapes.extraSmall,
      color =
        if (granted) JaBoardStatusColors.HealthyGreenContainer
        else MaterialTheme.colorScheme.surfaceContainerHighest,
      contentColor =
        if (granted) JaBoardStatusColors.HealthyGreen
        else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
      Text(
        text = if (granted) "Granted" else "Not Set",
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      )
    }
  }
}
