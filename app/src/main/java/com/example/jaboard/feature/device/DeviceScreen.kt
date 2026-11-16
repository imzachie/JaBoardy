package com.example.jaboard.feature.device

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NetworkPing
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jaboard.core.common.Formatters
import com.example.jaboard.core.design.ExpressiveCard
import com.example.jaboard.core.design.JaBoardMonoStyles
import com.example.jaboard.core.design.JaBoardStatusColors
import com.example.jaboard.core.design.JaBoardUtilityTopBar
import com.example.jaboard.core.design.KeyValueRow
import com.example.jaboard.core.design.MetricProgressRing
import com.example.jaboard.core.system.DeviceHardwareInfo
import com.example.jaboard.core.system.CpuSnapshot
import com.example.jaboard.core.system.NetworkSnapshot
import com.example.jaboard.core.system.RamSnapshot
import com.example.jaboard.core.system.StorageSnapshot
import com.example.jaboard.core.system.WifiDetails
import kotlin.math.roundToInt

val DeviceSections = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
  listOf(
    DeviceSection.RAM,
    DeviceSection.STORAGE,
    DeviceSection.CPU,
    DeviceSection.DEVICE,
    DeviceSection.NETWORK,
  )
} else {
  listOf(
    DeviceSection.RAM,
    DeviceSection.STORAGE,
    DeviceSection.DEVICE,
    DeviceSection.NETWORK,
  )
}

enum class DeviceSection(val key: String, val label: String) {
  RAM("ram", "RAM"),
  STORAGE("storage", "Storage"),
  CPU("cpu", "CPU"),
  DEVICE("device", "Device Info"),
  NETWORK("network", "Network / Wi-Fi");

  companion object {
    fun indexForKey(key: String): Int =
      entries.indexOfFirst { it.key.equals(key, ignoreCase = true) }.coerceAtLeast(0)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
  viewModel: DeviceViewModel,
  initialSection: String,
  onBack: () -> Unit,
) {
  val ram by viewModel.ramState.collectAsStateWithLifecycle()
  val storage by viewModel.storageState.collectAsStateWithLifecycle()
  val cpu by viewModel.cpuState.collectAsStateWithLifecycle()
  val network by viewModel.networkState.collectAsStateWithLifecycle()
  val pingLatency by viewModel.pingLatencyMs.collectAsStateWithLifecycle()
  val isPinging by viewModel.isPinging.collectAsStateWithLifecycle()
  val pingError by viewModel.pingError.collectAsStateWithLifecycle()
  val hw = viewModel.hardwareInfo

  val sections = remember { DeviceSections }

  var selectedTabIndex by rememberSaveable(initialSection) {
    mutableIntStateOf(sections.indexOfFirst { it.key == initialSection }.coerceAtLeast(0))
  }

  val currentSection = sections[selectedTabIndex]

  Scaffold(
    topBar = {
      JaBoardUtilityTopBar(
        title =
          when (currentSection) {
            DeviceSection.RAM -> "RAM Monitor"
            DeviceSection.STORAGE -> "Storage Monitor"
            DeviceSection.CPU -> "CPU Monitor"
            DeviceSection.DEVICE -> "Device Information"
            DeviceSection.NETWORK -> "Network & Wi-Fi"
            else -> "Device"
          },
        subtitle = "${hw.manufacturer} ${hw.model}",
        onBack = onBack,
      )
    }
  ) { innerPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      PrimaryScrollableTabRow(selectedTabIndex = selectedTabIndex, edgePadding = 16.dp) {
        sections.forEachIndexed { idx, section ->
          Tab(
            selected = selectedTabIndex == idx,
            onClick = { selectedTabIndex = idx },
            text = { Text(section.label) },
            modifier = Modifier.testTag("device_tab_${section.key}"),
          )
        }
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        when (currentSection) {
          DeviceSection.RAM -> {
            item { RamDetailSection(ram) }
          }
          DeviceSection.STORAGE -> {
            item { StorageDetailSection(storage) }
          }
          DeviceSection.CPU -> {
            item { CpuDetailSection(cpu) }
          }
          DeviceSection.DEVICE -> {
            item { HardwareDetailSection(hw) }
          }
          DeviceSection.NETWORK -> {
            item {
              NetworkDetailSection(
                net = network,
                pingMs = pingLatency,
                isPinging = isPinging,
                pingError = pingError,
                onRunPing = viewModel::runPingTest,
              )
            }
          }
          else -> {
            item { /* Unknown section */ }
          }
        }
      }
    }
  }
}

@Composable
private fun RamDetailSection(ram: RamSnapshot) {
  val pct = (ram.usedFraction * 100).roundToInt()
  val ringColor =
    when {
      ram.usedFraction >= 0.90f -> MaterialTheme.colorScheme.error
      ram.usedFraction >= 0.75f -> JaBoardStatusColors.WarningAmber
      else -> JaBoardStatusColors.HealthyGreen
    }

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        MetricProgressRing(
          progress = ram.usedFraction,
          size = 112.dp,
          strokeWidth = 10.dp,
          activeColor = ringColor,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$pct%", style = JaBoardMonoStyles.HeroNumeric)
            Text("USED", style = MaterialTheme.typography.labelSmall)
          }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.Memory,
              contentDescription = null,
              tint = ringColor,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("System Memory", style = MaterialTheme.typography.titleMedium)
          }
          Text(
            text = "${Formatters.formatBytes(ram.usedBytes)} / ${Formatters.formatBytes(ram.totalBytes)}",
            style = JaBoardMonoStyles.MetricReadout,
          )
          Text(
            text = "${Formatters.formatBytes(ram.availableBytes)} free right now",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text("Memory Breakdown", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(10.dp))
        KeyValueRow("Total RAM", Formatters.formatBytes(ram.totalBytes), monoValue = true)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        KeyValueRow("Used RAM", Formatters.formatBytes(ram.usedBytes), monoValue = true)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        KeyValueRow("Available RAM", Formatters.formatBytes(ram.availableBytes), monoValue = true)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        KeyValueRow("Low memory state", if (ram.isLowMemory) "Low Memory Warning" else "Normal")
      }
    }
  }
}

@Composable
private fun StorageDetailSection(storage: StorageSnapshot) {
  val pct = (storage.usedFraction * 100).roundToInt()
  val animProgress by
    animateFloatAsState(
      targetValue = storage.usedFraction,
      animationSpec = spring(dampingRatio = 0.85f, stiffness = 320f),
      label = "storage_bar",
    )

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
      Column(
        modifier = Modifier.padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.SdStorage,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Internal Storage", style = MaterialTheme.typography.titleLarge)
          }
          Text("$pct% used", style = JaBoardMonoStyles.MetricReadout)
        }

        LinearProgressIndicator(
          progress = { animProgress },
          modifier = Modifier.fillMaxWidth().height(12.dp),
          strokeCap = StrokeCap.Round,
          color = MaterialTheme.colorScheme.tertiary,
          trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "${Formatters.formatBytes(storage.usedBytes)} used",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "${Formatters.formatBytes(storage.availableBytes)} free",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text("Storage Breakdown", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(10.dp))
        KeyValueRow("Total Storage", Formatters.formatBytes(storage.totalBytes), monoValue = true)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        KeyValueRow("Used Space", Formatters.formatBytes(storage.usedBytes), monoValue = true)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        KeyValueRow("Free Space", Formatters.formatBytes(storage.availableBytes), monoValue = true)
      }
    }
  }
}

@Composable
private fun CpuDetailSection(cpu: CpuSnapshot) {
  val usagePct = cpu.usagePercent.roundToInt()
  val usageColor = when {
    cpu.usagePercent >= 80f -> MaterialTheme.colorScheme.error
    cpu.usagePercent >= 60f -> JaBoardStatusColors.WarningAmber
    else -> JaBoardStatusColors.HealthyGreen
  }

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
      Column(
        modifier = Modifier.padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.Speed,
              contentDescription = null,
              tint = usageColor,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("CPU Usage", style = MaterialTheme.typography.titleLarge)
          }
          Text("$usagePct%", style = JaBoardMonoStyles.HeroNumeric.copy(color = usageColor))
        }

        LinearProgressIndicator(
          progress = { cpu.usagePercent / 100f },
          modifier = Modifier.fillMaxWidth().height(12.dp),
          strokeCap = StrokeCap.Round,
          color = usageColor,
          trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column {
            Text("Load (1m / 5m / 15m)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${cpu.load1Min} / ${cpu.load5Min} / ${cpu.load15Min}", style = JaBoardMonoStyles.SpecValue)
          }
          if (cpu.frequencyMhz > 0) {
            Column(horizontalAlignment = Alignment.End) {
              Text("Frequency", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("${cpu.frequencyMhz} MHz / ${cpu.maxFrequencyMhz} MHz max", style = JaBoardMonoStyles.SpecValue)
            }
          }
        }

        cpu.temperatureCelsius?.let { temp ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("Temperature", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(String.format("%.1f°C", temp), style = JaBoardMonoStyles.SpecValue)
          }
        }

        cpu.governor?.let { gov ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("Governor", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(gov, style = JaBoardMonoStyles.SpecValue)
          }
        }
      }
    }

    ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text("CPU Details", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(10.dp))
        KeyValueRow("Cores", "${cpu.coreCount} logical cores", monoValue = true)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        KeyValueRow("Current Usage", "$usagePct%", monoValue = true)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        KeyValueRow("Load (1m / 5m / 15m)", "${cpu.load1Min} / ${cpu.load5Min} / ${cpu.load15Min}", monoValue = true)
        if (cpu.frequencyMhz > 0) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
          KeyValueRow("Current Frequency", "${cpu.frequencyMhz} MHz", monoValue = true)
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
          KeyValueRow("Max Frequency", "${cpu.maxFrequencyMhz} MHz", monoValue = true)
        }
        cpu.temperatureCelsius?.let { temp ->
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
          KeyValueRow("Temperature", String.format("%.1f°C", temp), monoValue = true)
        }
        cpu.governor?.let { gov ->
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
          KeyValueRow("Governor", gov)
        }
      }
    }
  }
}

@Composable
private fun HardwareDetailSection(hw: DeviceHardwareInfo) {
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
      Column(
        modifier = Modifier.padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = "${hw.brand} ${hw.model}",
          style = MaterialTheme.typography.headlineMedium,
        )
        Text(
          text = "Android ${hw.androidVersion} • API Level ${hw.sdkInt}",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = "${hw.processorCores} Cores • ${hw.supportedAbis}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text("System & Hardware", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(10.dp))
        KeyValueRow("Manufacturer", hw.manufacturer)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        KeyValueRow("Model", hw.model)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        KeyValueRow("Brand / Codename", "${hw.brand} (${hw.deviceCodeName})")
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        KeyValueRow("Android version", "${hw.androidVersion} (SDK ${hw.sdkInt})", monoValue = true)
        if (!hw.securityPatch.isNullOrBlank()) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
          KeyValueRow("Security patch", hw.securityPatch, monoValue = true)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        KeyValueRow("CPU cores", "${hw.processorCores} logical cores", monoValue = true)
        if (!hw.socModel.isNullOrBlank()) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
          KeyValueRow("SoC model", hw.socModel)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        KeyValueRow("Hardware / Board", "${hw.hardwareName} / ${hw.boardName}", monoValue = true)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        KeyValueRow("Supported ABIs", hw.supportedAbis, monoValue = true)
      }
    }

    ExpressiveCard(
      modifier = Modifier.fillMaxWidth(),
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Rounded.Info,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text =
            "Modern Android restricts direct kernel /proc/stat CPU utilization access for standalone apps without root, so JaBoard reports verified core count and architecture instead of synthetic CPU percentages.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NetworkDetailSection(
  net: NetworkSnapshot,
  pingMs: Long?,
  isPinging: Boolean,
  pingError: String?,
  onRunPing: () -> Unit,
) {
  val statusColor =
    if (net.isConnected && net.isValidated) JaBoardStatusColors.HealthyGreen
    else if (net.isConnected) JaBoardStatusColors.WarningAmber
    else MaterialTheme.colorScheme.error

  val wifi = net.wifiDetails
  val ip = net.ipDetails

  val headerIcon =
    when {
      !net.isConnected -> Icons.Rounded.WifiOff
      net.transportType == "Cellular" -> Icons.Rounded.CellTower
      net.transportType == "Ethernet" -> Icons.Rounded.Lan
      else -> Icons.Rounded.Wifi
    }

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    // 1. Connection Header & Wi-Fi Signal Card
    ExpressiveCard(
      modifier = Modifier.fillMaxWidth(),
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
      Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = statusColor.copy(alpha = 0.16f),
            contentColor = statusColor,
          ) {
            Icon(
              imageVector = headerIcon,
              contentDescription = null,
              modifier = Modifier.padding(14.dp).size(32.dp),
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (wifi != null) wifi.ssid else if (net.isConnected) net.transportType else "Offline",
              style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )
            Text(
              text =
                when {
                  !net.isConnected -> "No active network connection"
                  net.isValidated -> "Connected • Internet validated"
                  else -> "Connected • Validation pending"
                },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          if (wifi != null) {
            Surface(
              shape = MaterialTheme.shapes.small,
              color = statusColor.copy(alpha = 0.18f),
              contentColor = statusColor,
            ) {
              Text(
                text = "${wifi.signalPercent}%",
                style = JaBoardMonoStyles.SpecValue.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
          }
        }

        // Wi-Fi RSSI signal bar
        if (wifi != null) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                "Signal Strength (${wifi.signalLevel})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                "${wifi.rssiDbm} dBm",
                style = JaBoardMonoStyles.SpecValue,
              )
            }
            LinearProgressIndicator(
              progress = { (wifi.signalPercent / 100f).coerceIn(0f, 1f) },
              modifier = Modifier.fillMaxWidth().height(8.dp),
              strokeCap = StrokeCap.Round,
              color = statusColor,
              trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
          }
        }
      }
    }

    // 2. Wi-Fi Spec Badges & Link Capabilities (if Wi-Fi active)
    if (wifi != null) {
      ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          Text("Wi-Fi Specifications", style = MaterialTheme.typography.titleMedium)

          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            NetworkChip(icon = Icons.Rounded.Wifi, label = "Standard", value = wifi.wifiStandard)
            NetworkChip(icon = Icons.Rounded.Sensors, label = "Band", value = wifi.frequencyBand)
            NetworkChip(
              icon = Icons.Rounded.Speed,
              label = "Link Speed",
              value = if (wifi.linkSpeedMbps > 0) "${wifi.linkSpeedMbps} Mbps" else "Negotiating",
            )
            if (wifi.frequencyMhz > 0) {
              NetworkChip(icon = Icons.Rounded.Router, label = "Channel Freq", value = "${wifi.frequencyMhz} MHz")
            }
          }

          if (!wifi.bssid.isNullOrBlank()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            KeyValueRow("Access Point BSSID", wifi.bssid, monoValue = true)
          }
        }
      }
    }

    // 3. Network Latency & Ping Diagnostics Card
    ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Rounded.Speed,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text("Latency & Ping Diagnostics", style = MaterialTheme.typography.titleMedium)
              Text("Google DNS (8.8.8.8:53)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }

          FilledTonalButton(
            onClick = onRunPing,
            enabled = !isPinging && net.isConnected,
            modifier = Modifier.testTag("network_ping_test_button"),
          ) {
            if (isPinging) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Testing...")
            } else {
              Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Run Ping")
            }
          }
        }

        if (pingMs != null) {
          val latencyColor = when {
            pingMs < 45L -> JaBoardStatusColors.HealthyGreen
            pingMs < 110L -> JaBoardStatusColors.WarningAmber
            else -> MaterialTheme.colorScheme.error
          }
          val latencyDesc = when {
            pingMs < 45L -> "Fast • Excellent for real-time gaming & streaming"
            pingMs < 110L -> "Moderate • Suitable for general browsing & calls"
            else -> "High Latency • Possible network congestion"
          }

          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              Text(
                text = "${pingMs}ms",
                style = JaBoardMonoStyles.HeroNumeric.copy(color = latencyColor),
              )
              Column {
                Text(
                  text = "Round-Trip Latency",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                  text = latencyDesc,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        } else if (pingError != null) {
          Text(
            text = "Ping error: $pingError",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        } else {
          Text(
            text = "Tap 'Run Ping' to measure live round-trip network response time.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    // 4. IP, Routing & Interface Configuration
    ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text("IP & Routing Configuration", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(10.dp))

        if (ip?.interfaceName != null) {
          KeyValueRow("Network interface", ip.interfaceName, monoValue = true)
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        }

        KeyValueRow("Local IPv4 address", ip?.ipv4Address ?: "Unavailable", monoValue = true)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

        if (ip?.ipv6Address != null) {
          KeyValueRow("Local IPv6 address", ip.ipv6Address, monoValue = true)
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        }

        KeyValueRow("Default gateway", ip?.gatewayAddress ?: "Automatic", monoValue = true)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

        val dnsList = ip?.dnsServers.orEmpty()
        KeyValueRow(
          "DNS servers",
          if (dnsList.isNotEmpty()) dnsList.joinToString(", ") else "System Default",
          monoValue = true,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

        KeyValueRow("Metered state", if (net.isMetered) "Metered Connection" else "Unmetered (Broadband)")
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

        KeyValueRow("Internet validated", if (net.isValidated) "Yes (Fully online)" else "No / Pending")

        if (net.downstreamKbps != null) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
          KeyValueRow(
            "Downstream bandwidth",
            "${(net.downstreamKbps / 1000.0).roundToInt()} Mbps",
            monoValue = true,
          )
        }

        if (net.upstreamKbps != null) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
          KeyValueRow(
            "Upstream bandwidth",
            "${(net.upstreamKbps / 1000.0).roundToInt()} Mbps",
            monoValue = true,
          )
        }
      }
    }
  }
}

@Composable
private fun NetworkChip(icon: ImageVector, label: String, value: String) {
  Surface(
    shape = MaterialTheme.shapes.small,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.primary,
      )
      Column {
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = value,
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
        )
      }
    }
  }
}
