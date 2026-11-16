package com.example.jaboard.feature.battery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jaboard.core.system.BatteryMonitorRepository
import com.example.jaboard.core.system.BatterySnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class BatteryViewModel(batteryMonitorRepository: BatteryMonitorRepository) : ViewModel() {
  val batteryState: StateFlow<BatterySnapshot> =
    batteryMonitorRepository
      .observeBattery()
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = batteryMonitorRepository.readCurrentSnapshot(),
      )
}
