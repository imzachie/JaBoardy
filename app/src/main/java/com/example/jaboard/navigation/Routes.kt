package com.example.jaboard.navigation

import kotlinx.serialization.Serializable

sealed interface JaBoardRoute

// Top-level destinations
@Serializable data object HomeRoute : JaBoardRoute

@Serializable data object ExploreRoute : JaBoardRoute

@Serializable data object StudioRoute : JaBoardRoute

@Serializable data object SettingsRoute : JaBoardRoute

// Dedicated Utility destinations
@Serializable data object BatteryRoute : JaBoardRoute

@Serializable data object WeatherRoute : JaBoardRoute

@Serializable data class MusicRoute(val initialTab: String = "player") : JaBoardRoute

@Serializable data class DeviceMonitorRoute(val initialSection: String = "ram") : JaBoardRoute

@Serializable data object CalendarRoute : JaBoardRoute

@Serializable data object NotesRoute : JaBoardRoute

@Serializable data class NoteEditorRoute(val noteId: Long = -1L) : JaBoardRoute

@Serializable data object TodoRoute : JaBoardRoute

@Serializable data object CalculatorRoute : JaBoardRoute

@Serializable data object CompassRoute : JaBoardRoute

@Serializable data class ClockRoute(val initialTab: String = "clock") : JaBoardRoute

@Serializable data object LicensesRoute : JaBoardRoute
