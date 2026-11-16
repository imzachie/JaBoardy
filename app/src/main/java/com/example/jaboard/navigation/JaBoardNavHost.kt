package com.example.jaboard.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.jaboard.core.common.AppContainer
import com.example.jaboard.core.common.viewModelFactory
import com.example.jaboard.feature.battery.BatteryScreen
import com.example.jaboard.feature.battery.BatteryViewModel
import com.example.jaboard.feature.calculator.CalculatorScreen
import com.example.jaboard.feature.calculator.CalculatorViewModel
import com.example.jaboard.feature.calendar.CalendarScreen
import com.example.jaboard.feature.calendar.CalendarViewModel
import com.example.jaboard.feature.clock.ClockToolsScreen
import com.example.jaboard.feature.clock.ClockToolsViewModel
import com.example.jaboard.feature.device.DeviceScreen
import com.example.jaboard.feature.device.DeviceViewModel
import com.example.jaboard.feature.explore.ExploreScreen
import com.example.jaboard.feature.explore.UtilityDestinationId
import com.example.jaboard.feature.home.HomeScreen
import com.example.jaboard.feature.home.HomeViewModel
import com.example.jaboard.feature.music.MusicScreen
import com.example.jaboard.feature.music.MusicViewModel
import com.example.jaboard.feature.notes.NoteEditorScreen
import com.example.jaboard.feature.notes.NotesListScreen
import com.example.jaboard.feature.notes.NotesViewModel
import com.example.jaboard.feature.settings.LicensesScreen
import com.example.jaboard.feature.settings.SettingsScreen
import com.example.jaboard.feature.settings.SettingsViewModel
import com.example.jaboard.feature.studio.StudioScreen
import com.example.jaboard.feature.studio.StudioViewModel
import com.example.jaboard.feature.todo.TodoScreen
import com.example.jaboard.feature.todo.TodoViewModel
import com.example.jaboard.feature.compass.CompassScreen
import com.example.jaboard.feature.compass.CompassViewModel
import com.example.jaboard.feature.weather.WeatherScreen
import com.example.jaboard.feature.weather.WeatherViewModel

private data class TopLevelTab(
  val route: JaBoardRoute,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

private val TopLevelTabs =
  listOf(
    TopLevelTab(HomeRoute, "Home", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    TopLevelTab(
      ExploreRoute,
      "Explore",
      Icons.Rounded.Explore,
      Icons.Outlined.Explore,
      "nav_explore",
    ),
    TopLevelTab(StudioRoute, "Studio", Icons.Rounded.Tune, Icons.Outlined.Tune, "nav_studio"),
    TopLevelTab(
      SettingsRoute,
      "Settings",
      Icons.Rounded.Settings,
      Icons.Outlined.Settings,
      "nav_settings",
    ),
  )

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JaBoardAppShell(container: AppContainer) {
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDest = backStackEntry?.destination
  val appContext = LocalContext.current.applicationContext

  val showTopLevelChrome =
    currentDest == null ||
      TopLevelTabs.any { tab -> currentDest.hasRoute(tab.route::class) }

  fun navigateTopLevel(route: JaBoardRoute) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  val notesViewModel: NotesViewModel =
    viewModel(factory = viewModelFactory { NotesViewModel(container.notesRepository) })

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp

    Row(modifier = Modifier.fillMaxSize()) {
      if (useNavigationRail && showTopLevelChrome) {
        NavigationRail {
          Spacer(modifier = Modifier.weight(0.15f))
          TopLevelTabs.forEach { tab ->
            val selected = currentDest?.hasRoute(tab.route::class) == true
            NavigationRailItem(
              selected = selected,
              onClick = { navigateTopLevel(tab.route) },
              icon = {
                Icon(
                  imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                  contentDescription = tab.label,
                )
              },
              label = { Text(tab.label) },
              modifier = Modifier.testTag(tab.testTag),
            )
          }
          Spacer(modifier = Modifier.weight(1f))
        }
      }

      Scaffold(
        modifier = Modifier.weight(1f).fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
          if (!useNavigationRail && showTopLevelChrome) {
            NavigationBar {
              TopLevelTabs.forEach { tab ->
                val selected = currentDest?.hasRoute(tab.route::class) == true
                NavigationBarItem(
                  selected = selected,
                  onClick = { navigateTopLevel(tab.route) },
                  icon = {
                    Icon(
                      imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                      contentDescription = tab.label,
                    )
                  },
                  label = { Text(tab.label) },
                  modifier = Modifier.testTag(tab.testTag),
                )
              }
            }
          }
        },
      ) { innerPadding ->
        NavHost(
          navController = navController,
          startDestination = HomeRoute,
          enterTransition = { fadeIn() },
          exitTransition = { fadeOut() },
          modifier = Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
          composable<HomeRoute> {
            val homeVm: HomeViewModel =
              viewModel(
                factory =
                  viewModelFactory {
                    HomeViewModel(
                      dashboardRepository = container.dashboardRepository,
                      userPreferencesRepository = container.userPreferencesRepository,
                      weatherRepository = container.weatherRepository,
                      batteryMonitorRepository = container.batteryMonitorRepository,
                      deviceMonitorRepository = container.deviceMonitorRepository,
                      mediaSessionRepository = container.mediaSessionRepository,
                      calendarRepository = container.calendarRepository,
                    )
                  }
              )
            HomeScreen(
              viewModel = homeVm,
              onOpenStudio = { navigateTopLevel(StudioRoute) },
              onOpenWeather = { navController.navigate(WeatherRoute) },
              onOpenBattery = { navController.navigate(BatteryRoute) },
              onOpenMusic = { navController.navigate(MusicRoute("player")) },
              onOpenDeviceSection = { sec -> navController.navigate(DeviceMonitorRoute(sec)) },
              onOpenClockTab = { tab -> navController.navigate(ClockRoute(tab)) },
              onOpenCalendar = { navController.navigate(CalendarRoute) },
              onOpenNotes = { navController.navigate(NotesRoute) },
              onOpenTodo = { navController.navigate(TodoRoute) },
              onOpenCalculator = { navController.navigate(CalculatorRoute) },
            )
          }

          composable<ExploreRoute> {
            ExploreScreen(
              onOpenUtility = { util ->
                when (util) {
                  UtilityDestinationId.BATTERY -> navController.navigate(BatteryRoute)
                  UtilityDestinationId.RAM -> navController.navigate(DeviceMonitorRoute("ram"))
                  UtilityDestinationId.STORAGE ->
                    navController.navigate(DeviceMonitorRoute("storage"))
                  UtilityDestinationId.DEVICE_INFO ->
                    navController.navigate(DeviceMonitorRoute("device"))
                  UtilityDestinationId.NETWORK_INFO ->
                    navController.navigate(DeviceMonitorRoute("network"))
                  UtilityDestinationId.WEATHER -> navController.navigate(WeatherRoute)
                  UtilityDestinationId.CLOCK -> navController.navigate(ClockRoute("clock"))
                  UtilityDestinationId.CALENDAR -> navController.navigate(CalendarRoute)
                  UtilityDestinationId.NOTES -> navController.navigate(NotesRoute)
                  UtilityDestinationId.TODO -> navController.navigate(TodoRoute)
                  UtilityDestinationId.CALCULATOR -> navController.navigate(CalculatorRoute)
                  UtilityDestinationId.TIMER -> navController.navigate(ClockRoute("timer"))
                  UtilityDestinationId.STOPWATCH -> navController.navigate(ClockRoute("stopwatch"))
                  UtilityDestinationId.MUSIC_PLAYER -> navController.navigate(MusicRoute("player"))
                  UtilityDestinationId.MEDIA_CONTROLS ->
                    navController.navigate(MusicRoute("controls"))
                  UtilityDestinationId.COMPASS -> navController.navigate(CompassRoute)
                }
              }
            )
          }

          composable<StudioRoute> {
            val studioVm: StudioViewModel =
              viewModel(
                factory =
                  viewModelFactory {
                    StudioViewModel(
                      dashboardRepository = container.dashboardRepository,
                      userPreferencesRepository = container.userPreferencesRepository,
                    )
                  }
              )
            StudioScreen(viewModel = studioVm)
          }

          composable<SettingsRoute> {
            val settingsVm: SettingsViewModel =
              viewModel(
                factory =
                  viewModelFactory {
                    SettingsViewModel(
                      context = appContext,
                      userPreferencesRepository = container.userPreferencesRepository,
                      dashboardRepository = container.dashboardRepository,
                      weatherRepository = container.weatherRepository,
                      notesRepository = container.notesRepository,
                      todoRepository = container.todoRepository,
                      mediaSessionRepository = container.mediaSessionRepository,
                    )
                  }
              )
            SettingsScreen(
              viewModel = settingsVm,
              onOpenStudio = { navigateTopLevel(StudioRoute) },
              onOpenLicenses = { navController.navigate(LicensesRoute) },
            )
          }

          composable<BatteryRoute> {
            val vm: BatteryViewModel =
              viewModel(
                factory = viewModelFactory { BatteryViewModel(container.batteryMonitorRepository) }
              )
            BatteryScreen(viewModel = vm, onBack = { navController.popBackStack() })
          }

          composable<WeatherRoute> {
            val vm: WeatherViewModel =
              viewModel(
                factory =
                  viewModelFactory {
                    WeatherViewModel(
                      weatherRepository = container.weatherRepository,
                      userPreferencesRepository = container.userPreferencesRepository,
                      locationRepository = container.locationRepository,
                      weatherAiSummaryRepository = container.weatherAiSummaryRepository,
                    )
                  }
              )
            WeatherScreen(viewModel = vm, onBack = { navController.popBackStack() })
          }

          composable<CompassRoute> {
            val vm: CompassViewModel =
              viewModel(
                factory = viewModelFactory { CompassViewModel(container.locationRepository) }
              )
            CompassScreen(viewModel = vm, onBack = { navController.popBackStack() })
          }

          composable<MusicRoute> { entry ->
            val route = entry.toRoute<MusicRoute>()
            val vm: MusicViewModel =
              viewModel(
                factory =
                  viewModelFactory {
                    MusicViewModel(
                      mediaSessionRepository = container.mediaSessionRepository,
                      lrclibRepository = container.lrclibRepository,
                    )
                  }
              )
            MusicScreen(
              viewModel = vm,
              initialTab = route.initialTab,
              onBack = { navController.popBackStack() },
            )
          }

          composable<DeviceMonitorRoute> { entry ->
            val route = entry.toRoute<DeviceMonitorRoute>()
            val vm: DeviceViewModel =
              viewModel(
                factory = viewModelFactory { DeviceViewModel(container.deviceMonitorRepository) }
              )
            DeviceScreen(
              viewModel = vm,
              initialSection = route.initialSection,
              onBack = { navController.popBackStack() },
            )
          }

          composable<CalendarRoute> {
            val vm: CalendarViewModel =
              viewModel(
                factory = viewModelFactory { CalendarViewModel(container.calendarRepository) }
              )
            CalendarScreen(viewModel = vm, onBack = { navController.popBackStack() })
          }

          composable<NotesRoute> {
            NotesListScreen(
              viewModel = notesViewModel,
              onBack = { navController.popBackStack() },
              onOpenNote = { noteId -> navController.navigate(NoteEditorRoute(noteId)) },
            )
          }

          composable<NoteEditorRoute> { entry ->
            val route = entry.toRoute<NoteEditorRoute>()
            NoteEditorScreen(
              viewModel = notesViewModel,
              noteId = route.noteId,
              onBack = { navController.popBackStack() },
            )
          }

          composable<TodoRoute> {
            val vm: TodoViewModel =
              viewModel(factory = viewModelFactory { TodoViewModel(container.todoRepository) })
            TodoScreen(viewModel = vm, onBack = { navController.popBackStack() })
          }

          composable<CalculatorRoute> {
            val vm: CalculatorViewModel =
              viewModel(factory = viewModelFactory { CalculatorViewModel() })
            CalculatorScreen(viewModel = vm, onBack = { navController.popBackStack() })
          }

          composable<ClockRoute> { entry ->
            val route = entry.toRoute<ClockRoute>()
            val vm: ClockToolsViewModel =
              viewModel(
                factory =
                  viewModelFactory { ClockToolsViewModel(container.timerNotificationHelper) }
              )
            ClockToolsScreen(
              viewModel = vm,
              initialTab = route.initialTab,
              onBack = { navController.popBackStack() },
            )
          }

          composable<LicensesRoute> {
            LicensesScreen(onBack = { navController.popBackStack() })
          }
        }
      }
    }
  }
}
