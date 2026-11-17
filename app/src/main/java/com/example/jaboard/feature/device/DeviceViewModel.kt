package com.example.jaboard.feature.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jaboard.core.system.DeviceHardwareInfo
import com.example.jaboard.core.system.DeviceMonitorRepository
import com.example.jaboard.core.system.NetworkSnapshot
import com.example.jaboard.core.system.RamSnapshot
import com.example.jaboard.core.system.StorageSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DeviceViewModel(private val deviceMonitorRepository: DeviceMonitorRepository) : ViewModel() {

  val ramState: StateFlow<RamSnapshot> =
    deviceMonitorRepository
      .observeRam()
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = deviceMonitorRepository.readRamSnapshot(),
      )

  val storageState: StateFlow<StorageSnapshot> =
    deviceMonitorRepository
      .observeStorage()
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = deviceMonitorRepository.readStorageSnapshot(),
      )

  val networkState: StateFlow<NetworkSnapshot> =
    deviceMonitorRepository
      .observeNetwork()
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = deviceMonitorRepository.readNetworkSnapshot(),
      )

  val hardwareInfo: DeviceHardwareInfo = deviceMonitorRepository.readDeviceHardwareInfo()

  private val _pingLatencyMs = MutableStateFlow<Long?>(null)
  val pingLatencyMs: StateFlow<Long?> = _pingLatencyMs.asStateFlow()

  private val _isPinging = MutableStateFlow(false)
  val isPinging: StateFlow<Boolean> = _isPinging.asStateFlow()

  private val _pingError = MutableStateFlow<String?>(null)
  val pingError: StateFlow<String?> = _pingError.asStateFlow()

  fun runPingTest() {
    if (_isPinging.value) return
    viewModelScope.launch {
      _isPinging.value = true
      _pingError.value = null
      val result = deviceMonitorRepository.testPingLatency()
      result
        .onSuccess { ms ->
          _pingLatencyMs.value = ms
          _pingError.value = null
        }
        .onFailure { err ->
          _pingLatencyMs.value = null
          _pingError.value = err.message ?: "Failed to ping gateway/DNS"
        }
      _isPinging.value = false
    }
  }
}
