package com.example.jaboard.core.system

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import java.io.File
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

data class RamSnapshot(
  val totalBytes: Long,
  val availableBytes: Long,
  val usedBytes: Long,
  val usedFraction: Float,
  val isLowMemory: Boolean,
)

data class CpuSnapshot(
  val usagePercent: Float, // 0-100
  val load1Min: Float,
  val load5Min: Float,
  val load15Min: Float,
  val coreCount: Int,
  val frequencyMhz: Int,
  val maxFrequencyMhz: Int,
  val temperatureCelsius: Float?,
  val governor: String?,
)

data class StorageSnapshot(
  val totalBytes: Long,
  val availableBytes: Long,
  val usedBytes: Long,
  val usedFraction: Float,
)

data class DeviceHardwareInfo(
  val manufacturer: String,
  val model: String,
  val brand: String,
  val deviceCodeName: String,
  val androidVersion: String,
  val sdkInt: Int,
  val securityPatch: String?,
  val processorCores: Int,
  val supportedAbis: String,
  val socModel: String?,
  val hardwareName: String,
  val boardName: String,
)

data class WifiDetails(
  val ssid: String,
  val bssid: String?,
  val rssiDbm: Int,
  val signalPercent: Int,
  val signalLevel: String,
  val linkSpeedMbps: Int,
  val frequencyMhz: Int,
  val frequencyBand: String,
  val wifiStandard: String,
)

data class IpConfigDetails(
  val ipv4Address: String?,
  val ipv6Address: String?,
  val gatewayAddress: String?,
  val dnsServers: List<String>,
  val interfaceName: String?,
)

data class NetworkSnapshot(
  val isConnected: Boolean,
  val isValidated: Boolean,
  val transportType: String,
  val isMetered: Boolean,
  val downstreamKbps: Int?,
  val upstreamKbps: Int?,
  val wifiDetails: WifiDetails? = null,
  val ipDetails: IpConfigDetails? = null,
)

class DeviceMonitorRepository(private val context: Context) {

  private val wifiManager: WifiManager? =
    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

  fun observeRam(intervalMs: Long = 2500L): Flow<RamSnapshot> = flow {
    while (true) {
      emit(readRamSnapshot())
      delay(intervalMs)
    }
  }

  fun readRamSnapshot(): RamSnapshot {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    am.getMemoryInfo(memInfo)
    val total = memInfo.totalMem.coerceAtLeast(1L)
    val avail = memInfo.availMem.coerceIn(0L, total)
    val used = (total - avail).coerceAtLeast(0L)
    return RamSnapshot(
      totalBytes = total,
      availableBytes = avail,
      usedBytes = used,
      usedFraction = (used.toFloat() / total.toFloat()).coerceIn(0f, 1f),
      isLowMemory = memInfo.lowMemory,
    )
  }

  fun observeStorage(intervalMs: Long = 10_000L): Flow<StorageSnapshot> = flow {
    while (true) {
      emit(readStorageSnapshot())
      delay(intervalMs)
    }
  }

  fun readStorageSnapshot(): StorageSnapshot {
    val path: File = Environment.getDataDirectory()
    val stat = StatFs(path.path)
    val total = stat.totalBytes.coerceAtLeast(1L)
    val avail = stat.availableBytes.coerceIn(0L, total)
    val used = (total - avail).coerceAtLeast(0L)
    return StorageSnapshot(
      totalBytes = total,
      availableBytes = avail,
      usedBytes = used,
      usedFraction = (used.toFloat() / total.toFloat()).coerceIn(0f, 1f),
    )
  }

  fun observeCpu(intervalMs: Long = 1000L): Flow<CpuSnapshot> = flow {
    while (true) {
      emit(readCpuSnapshot())
      delay(intervalMs)
    }
  }

  fun readCpuSnapshot(): CpuSnapshot {
    val coreCount = Runtime.getRuntime().availableProcessors()
    var usagePercent = 0f
    var load1Min = 0f
    var load5Min = 0f
    var load15Min = 0f
    var frequencyMhz = 0
    var maxFrequencyMhz = 0
    var temperatureCelsius: Float? = null
    var governor: String? = null

    // Try to read /proc/stat for CPU usage (limited on modern Android)
    try {
      val statFile = File("/proc/stat")
      if (statFile.exists()) {
        val lines = statFile.readText().lines()
        val cpuLine = lines.firstOrNull { it.startsWith("cpu ") }
        if (cpuLine != null) {
          val parts = cpuLine.split("\\s+")
          if (parts.size >= 8) {
            val user = parts[1].toLong()
            val nice = parts[2].toLong()
            val system = parts[3].toLong()
            val idle = parts[4].toLong()
            val iowait = parts[5].toLong()
            val irq = parts[6].toLong()
            val softirq = parts[7].toLong()

            val total = user + nice + system + idle + iowait + irq + softirq
            val nonIdle = user + nice + system + irq + softirq + iowait

            // Store for next calculation (using a simple approach)
            // For real-time usage, we'd need two samples, but this gives a rough estimate
            if (total > 0) {
              usagePercent = ((nonIdle.toFloat() / total.toFloat()) * 100f).coerceIn(0f, 100f)
            }
          }
        }
      }
    } catch (e: Exception) {
      // /proc/stat access restricted on modern Android
    }

    // Try to read load average
    try {
      val loadFile = File("/proc/loadavg")
      if (loadFile.exists()) {
        val loadText = loadFile.readText().trim()
        val loadParts = loadText.split("\\s+")
        if (loadParts.size >= 3) {
          load1Min = loadParts[0].toFloat()
          load5Min = loadParts[1].toFloat()
          load15Min = loadParts[2].toFloat()
        }
      }
    } catch (e: Exception) {
      // loadavg may not be accessible
    }

    // Try to read CPU frequency
    try {
      val cpuInfoFile = File("/proc/cpuinfo")
      if (cpuInfoFile.exists()) {
        val lines = cpuInfoFile.readText().lines()
        for (line in lines) {
          if (line.startsWith("cpu MHz") || line.startsWith("clock")) {
            val parts = line.split(":")
            if (parts.size >= 2) {
              frequencyMhz = parts[1].trim().toFloat().toInt()
            }
          }
        }
      }
    } catch (e: Exception) {
      // cpuinfo may be restricted
    }

    // Try to read max frequency from sysfs
    try {
      val maxFreqFile = File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
      if (maxFreqFile.exists()) {
        maxFrequencyMhz = (maxFreqFile.readText().trim().toLong() / 1000).toInt() // kHz to MHz
      }
    } catch (e: Exception) {
      // Not accessible
    }

    // Try to read thermal zone for temperature
    try {
      val thermalFiles = File("/sys/class/thermal").listFiles()
      thermalFiles?.forEach { zone ->
        val typeFile = File(zone, "type")
        val tempFile = File(zone, "temp")
        if (typeFile.exists() && tempFile.exists()) {
          val type = typeFile.readText().trim().lowercase()
          if (type.contains("cpu") || type.contains("therm")) {
            val temp = tempFile.readText().trim().toLong() / 1000f // millidegrees to Celsius
            if (temperatureCelsius == null || temp > temperatureCelsius!!) {
              temperatureCelsius = temp
            }
          }
        }
      }
    } catch (e: Exception) {
      // Thermal zones may not be accessible
    }

    // Try to read governor
    try {
      val governorFile = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
      if (governorFile.exists()) {
        governor = governorFile.readText().trim()
      }
    } catch (e: Exception) {
      // Governor not accessible
    }

    return CpuSnapshot(
      usagePercent = usagePercent,
      load1Min = load1Min,
      load5Min = load5Min,
      load15Min = load15Min,
      coreCount = coreCount,
      frequencyMhz = frequencyMhz,
      maxFrequencyMhz = maxFrequencyMhz,
      temperatureCelsius = temperatureCelsius,
      governor = governor,
    )
  }

  fun readDeviceHardwareInfo(): DeviceHardwareInfo {
    val soc =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Build.SOC_MODEL.takeIf { it.isNotBlank() && it != Build.UNKNOWN }
      } else {
        null
      }
    return DeviceHardwareInfo(
      manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
      model = Build.MODEL,
      brand = Build.BRAND.replaceFirstChar { it.uppercase() },
      deviceCodeName = Build.DEVICE,
      androidVersion = Build.VERSION.RELEASE,
      sdkInt = Build.VERSION.SDK_INT,
      securityPatch = Build.VERSION.SECURITY_PATCH.takeIf { it.isNotBlank() },
      processorCores = Runtime.getRuntime().availableProcessors(),
      supportedAbis = Build.SUPPORTED_ABIS.joinToString(", "),
      socModel = soc,
      hardwareName = Build.HARDWARE,
      boardName = Build.BOARD,
    )
  }

  fun observeNetwork(): Flow<NetworkSnapshot> = callbackFlow {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    trySend(readNetworkSnapshot(cm))

    val callback =
      object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
          trySend(readNetworkSnapshot(cm))
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
          trySend(readNetworkSnapshot(cm))
        }

        override fun onLost(network: Network) {
          trySend(readNetworkSnapshot(cm))
        }
      }

    runCatching { cm.registerDefaultNetworkCallback(callback) }
    awaitClose { runCatching { cm.unregisterNetworkCallback(callback) } }
  }

  fun readNetworkSnapshot(
    cm: ConnectivityManager =
      context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
  ): NetworkSnapshot {
    val active = cm.activeNetwork ?: return disconnectedNetwork()
    val caps = cm.getNetworkCapabilities(active) ?: return disconnectedNetwork()

    val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    val transport =
      when {
        isWifi -> "Wi-Fi"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth"
        else -> "Other"
      }

    val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    val metered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

    val wifiDetails = if (isWifi) extractWifiDetails(caps) else null
    val ipDetails = extractIpDetails(cm, active)

    return NetworkSnapshot(
      isConnected = hasInternet,
      isValidated = validated,
      transportType = transport,
      isMetered = metered,
      downstreamKbps = caps.linkDownstreamBandwidthKbps.takeIf { it > 0 },
      upstreamKbps = caps.linkUpstreamBandwidthKbps.takeIf { it > 0 },
      wifiDetails = wifiDetails,
      ipDetails = ipDetails,
    )
  }

  suspend fun testPingLatency(host: String = "8.8.8.8", port: Int = 53, timeoutMs: Int = 3000): Result<Long> =
    withContext(Dispatchers.IO) {
      runCatching {
        val socketAddress = InetSocketAddress(host, port)
        val socket = Socket()
        val start = SystemClock.elapsedRealtime()
        socket.connect(socketAddress, timeoutMs)
        val latency = SystemClock.elapsedRealtime() - start
        socket.close()
        latency
      }
    }

  @Suppress("DEPRECATION")
  private fun extractWifiDetails(caps: NetworkCapabilities): WifiDetails? {
    val wifiInfo: WifiInfo? =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        (caps.transportInfo as? WifiInfo) ?: runCatching { wifiManager?.connectionInfo }.getOrNull()
      } else {
        runCatching { wifiManager?.connectionInfo }.getOrNull()
      }

    if (wifiInfo == null) return null

    val rawSsid = wifiInfo.ssid.orEmpty().removePrefix("\"").removeSuffix("\"")
    val cleanSsid = if (rawSsid.isBlank() || rawSsid == "<unknown ssid>") "Connected Wi-Fi" else rawSsid
    val bssid = wifiInfo.bssid?.takeIf { it.isNotBlank() && it != "02:00:00:00:00:00" }
    val rssi = wifiInfo.rssi
    val signalPct = ((rssi + 100) * 2).coerceIn(0, 100)
    val rating = when {
      signalPct >= 80 -> "Excellent"
      signalPct >= 60 -> "Good"
      signalPct >= 40 -> "Fair"
      else -> "Weak"
    }

    val speed = wifiInfo.linkSpeed.coerceAtLeast(0)
    val freq = wifiInfo.frequency
    val band = when (freq) {
      in 2400..2500 -> "2.4 GHz"
      in 4900..5900 -> "5 GHz"
      in 5925..7125 -> "6 GHz"
      else -> if (freq > 0) "${freq} MHz" else "Standard"
    }

    val standard =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        when (wifiInfo.wifiStandard) {
          4 -> "Wi-Fi 4 (802.11n)"
          5 -> "Wi-Fi 5 (802.11ac)"
          6 -> "Wi-Fi 6 (802.11ax)"
          7 -> "Wi-Fi 6E"
          8 -> "Wi-Fi 7 (802.11be)"
          else -> if (band.contains("5 GHz") || band.contains("6 GHz")) "Wi-Fi 5/6" else "Wi-Fi"
        }
      } else {
        if (band.contains("5 GHz")) "Wi-Fi 5 (802.11ac)" else "Wi-Fi"
      }

    return WifiDetails(
      ssid = cleanSsid,
      bssid = bssid,
      rssiDbm = rssi,
      signalPercent = signalPct,
      signalLevel = rating,
      linkSpeedMbps = speed,
      frequencyMhz = freq,
      frequencyBand = band,
      wifiStandard = standard,
    )
  }

  private fun extractIpDetails(cm: ConnectivityManager, activeNetwork: Network): IpConfigDetails {
    val linkProps = cm.getLinkProperties(activeNetwork)
    val iface = linkProps?.interfaceName
    val linkAddresses = linkProps?.linkAddresses.orEmpty()
    val ipv4 = linkAddresses.mapNotNull { it.address as? Inet4Address }.firstOrNull()?.hostAddress
    val ipv6 = linkAddresses.mapNotNull { it.address as? Inet6Address }.firstOrNull()?.hostAddress
    val gateway = linkProps?.routes?.firstOrNull { it.isDefaultRoute && it.gateway != null }?.gateway?.hostAddress
    val dns = linkProps?.dnsServers?.mapNotNull { it.hostAddress }.orEmpty()

    return IpConfigDetails(
      ipv4Address = ipv4,
      ipv6Address = ipv6,
      gatewayAddress = gateway,
      dnsServers = dns,
      interfaceName = iface,
    )
  }

  private fun disconnectedNetwork() =
    NetworkSnapshot(
      isConnected = false,
      isValidated = false,
      transportType = "Offline",
      isMetered = false,
      downstreamKbps = null,
      upstreamKbps = null,
      wifiDetails = null,
      ipDetails = null,
    )
}
