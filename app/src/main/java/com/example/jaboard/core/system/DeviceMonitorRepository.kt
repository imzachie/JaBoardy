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
