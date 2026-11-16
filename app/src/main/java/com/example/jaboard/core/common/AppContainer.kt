package com.example.jaboard.core.common

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.jaboard.core.database.DashboardRepository
import com.example.jaboard.core.database.JaBoardDatabase
import com.example.jaboard.core.database.NotesRepository
import com.example.jaboard.core.database.TodoRepository
import com.example.jaboard.core.datastore.UserPreferencesRepository
import com.example.jaboard.core.network.WeatherRepository
import com.example.jaboard.core.system.BatteryMonitorRepository
import com.example.jaboard.core.system.CalendarRepository
import com.example.jaboard.core.system.DeviceMonitorRepository
import com.example.jaboard.core.system.LocationRepository
import com.example.jaboard.core.system.MediaSessionRepository
import com.example.jaboard.core.system.TimerNotificationHelper

class AppContainer(context: Context) {
  private val appContext = context.applicationContext

  val database: JaBoardDatabase = JaBoardDatabase.getInstance(appContext)
  val userPreferencesRepository = UserPreferencesRepository(appContext)
  val dashboardRepository = DashboardRepository(database.dashboardDao())
  val notesRepository = NotesRepository(database.noteDao())
  val todoRepository = TodoRepository(database.todoDao())
  val weatherRepository = WeatherRepository(database.weatherCacheDao())
  val weatherAiSummaryRepository = com.example.jaboard.core.network.WeatherAiSummaryRepository(appContext)

  val batteryMonitorRepository = BatteryMonitorRepository(appContext)
  val deviceMonitorRepository = DeviceMonitorRepository(appContext)
  val locationRepository = LocationRepository(appContext)
  val calendarRepository = CalendarRepository(appContext)
  val mediaSessionRepository = MediaSessionRepository(appContext)
  val lrclibRepository = com.example.jaboard.core.network.LrclibRepository()
  val timerNotificationHelper = TimerNotificationHelper(appContext)
}

fun <T : ViewModel> viewModelFactory(initializer: () -> T): ViewModelProvider.Factory =
  object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = initializer() as VM
  }
