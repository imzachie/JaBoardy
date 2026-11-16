# JaBoard

JaBoard is a standalone, offline-first Android dashboard and utility application inspired by Google Pixel and Material You. It brings device telemetry, Open-Meteo weather forecasts, MediaSession playback controls, calendar events, and productivity tools together in a customizable Material 3 Expressive Bento interface.

## Key Features

- **Home (Expressive Bento Dashboard)**: Mixed-size widget grid (`Compact`, `Wide`, `Expanded`) with long-press drag-and-drop rearrangement, quick actions, and live updates for Clock & Date, Weather, Battery, Now Playing, RAM, Storage, and Upcoming Calendar Events.
- **Explore (Utility Directory)**: Searchable, category-filtered directory (`Device`, `Lifestyle`, `Productivity`, `Media`) with 15 dedicated utility experiences.
- **Studio (Dashboard Customization)**: Drag-and-drop card reordering, accessible arrow reordering, per-card visibility and size toggles, 4 layout presets (*Expressive Bento*, *System Telemetry*, *Daily Focus*, *Minimalist*), theme mode (`System`, `Light`, `Dark`), Dynamic Color toggle, and one-tap reset.
- **Settings**: Appearance controls, temperature unit (`°C` / `°F`), live permission status inspector, local data management, version metadata, and open-source attributions.

## Dedicated Utilities

- **Device**: Battery Monitor (`BatteryManager`), RAM Monitor (`ActivityManager.MemoryInfo`), Storage Monitor (`StatFs`), Device Information (`Build` & SoC metadata), Network Information (`ConnectivityManager`).
- **Lifestyle**: Weather (Open-Meteo Forecast & Geocoding APIs with Room offline caching and optional coarse GPS), Analog/Digital Clock, Calendar (`CalendarContract`).
- **Productivity**: Notes (Room persistence, pinning, color tags, search, auto-save), To-Do Lists (Room persistence, priority & filtering), Calculator (standard + scientific shunting-yard engine), Countdown Timer, Stopwatch with laps.
- **Media**: Music Player & Active Media Controls via `MediaSessionManager` and `NotificationListenerService`.

## Architecture & Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3 + Navigation Compose (`@Serializable` type-safe routes)
- **State**: MVVM (`ViewModel` + `StateFlow` + `collectAsStateWithLifecycle`)
- **Local Persistence**: Room (`jaboard.db`) + Jetpack Preferences DataStore
- **Networking**: Retrofit + OkHttp + Moshi (Open-Meteo APIs, zero API key required)
- **Fonts**: Bundled Google Fonts (`Outfit`, `Plus Jakarta Sans`, `JetBrains Mono`)

## Build & Test Instructions

1. Ensure JDK 21 and Android SDK (API 36) are configured (`ANDROID_SDK_ROOT` set; no `local.properties` required when using environment SDK variables).
2. Build the debug APK:
   ```bash
   gradle :app:assembleDebug
   ```
   Output APK path: `app/build/outputs/apk/debug/app-debug.apk`
3. Run unit & Robolectric tests:
   ```bash
   gradle :app:testDebugUnitTest
   ```
4. Run Android Lint:
   ```bash
   gradle :app:lintDebug
   ```

## Permissions

All optional permissions include contextual in-app explanations and degrade gracefully when denied:
- `INTERNET` & `ACCESS_NETWORK_STATE`: Fetch Open-Meteo weather forecasts and city geocoding results.
- `ACCESS_COARSE_LOCATION` *(optional)*: Approximate location lookup for Weather; manual city search works without it.
- `READ_CALENDAR` *(optional)*: Display upcoming events on Home and the Calendar utility.
- `POST_NOTIFICATIONS` *(optional, Android 13+)*: Alert when a Countdown Timer completes.
- `BIND_NOTIFICATION_LISTENER_SERVICE` *(optional, user-enabled in system settings)*: Required by Android to read active `MediaSession` metadata and send Play/Pause/Skip/Seek commands.

## Known Limitations

- **CPU Utilization**: Modern Android restricts `/proc/stat` access for non-root apps; Device Information reports verified core count, SoC model (Android 12+), and ABIs rather than fabricated CPU percentages.
- **Battery Health / Charge Time**: Hardware diagnostics reflect only fields exposed by `BatteryManager` and `ACTION_BATTERY_CHANGED`; omitted OEM fields are shown honestly as not reported.
- **Media Controls**: Cross-app playback control requires the user to enable JaBoard under Android Notification Access settings.
