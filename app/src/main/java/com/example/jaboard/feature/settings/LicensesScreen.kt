package com.example.jaboard.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.jaboard.core.design.ExpressiveCard
import com.example.jaboard.core.design.JaBoardUtilityTopBar

private data class OpenSourceLibraryNotice(
  val name: String,
  val author: String,
  val license: String,
  val summary: String,
)

private val OpenSourceNotices =
  listOf(
    OpenSourceLibraryNotice(
      "AndroidX Jetpack (Compose, Room, DataStore, Navigation, Lifecycle)",
      "The Android Open Source Project",
      "Apache License 2.0",
      "Core Jetpack UI, local SQLite ORM, Preferences persistence, and type-safe navigation.",
    ),
    OpenSourceLibraryNotice(
      "Kotlin & Kotlinx Coroutines / Serialization",
      "JetBrains s.r.o.",
      "Apache License 2.0",
      "Language runtime, structured concurrency, Flow, and type-safe route serialization.",
    ),
    OpenSourceLibraryNotice(
      "Retrofit & OkHttp",
      "Square, Inc.",
      "Apache License 2.0",
      "HTTP client and REST interface layer used for Open-Meteo weather and city geocoding.",
    ),
    OpenSourceLibraryNotice(
      "Moshi",
      "Square, Inc.",
      "Apache License 2.0",
      "JSON serialization and code-generated adapters for weather forecast caching.",
    ),
    OpenSourceLibraryNotice(
      "Coil Compose",
      "Coil Contributors",
      "Apache License 2.0",
      "Image loading library for Jetpack Compose.",
    ),
    OpenSourceLibraryNotice(
      "Open-Meteo Weather & Geocoding Data",
      "Open-Meteo.com",
      "CC BY 4.0",
      "Open-source weather forecast and geocoding endpoints used without API key tracking.",
    ),
    OpenSourceLibraryNotice(
      "Google Fonts (Outfit, Plus Jakarta Sans, JetBrains Mono)",
      "Google Fonts & JetBrains",
      "SIL Open Font License 1.1",
      "Bundled display, body, and tabular monospace typography.",
    ),
  )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
  Scaffold(
    topBar = {
      JaBoardUtilityTopBar(
        title = "Open-Source Licenses",
        subtitle = "Libraries & data attributions",
        onBack = onBack,
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      items(OpenSourceNotices) { notice ->
        ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(notice.name, style = MaterialTheme.typography.titleMedium)
            Text(
              "${notice.author} • ${notice.license}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              notice.summary,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}
