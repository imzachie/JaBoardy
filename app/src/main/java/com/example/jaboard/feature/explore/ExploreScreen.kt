package com.example.jaboard.feature.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.jaboard.core.design.EmptyStateView
import com.example.jaboard.core.design.ExpressiveCard
import com.example.jaboard.core.design.JaBoardLargeTopBar

enum class UtilityCategory(val label: String) {
  ALL("All"),
  DEVICE("Device"),
  LIFESTYLE("Lifestyle"),
  PRODUCTIVITY("Productivity"),
  MEDIA("Media"),
}

enum class UtilityDestinationId {
  BATTERY,
  RAM,
  STORAGE,
  DEVICE_INFO,
  NETWORK_INFO,
  WEATHER,
  CLOCK,
  CALENDAR,
  NOTES,
  TODO,
  CALCULATOR,
  TIMER,
  STOPWATCH,
  MUSIC_PLAYER,
  MEDIA_CONTROLS,
  COMPASS,
}

private data class UtilityCatalogItem(
  val id: UtilityDestinationId,
  val title: String,
  val subtitle: String,
  val category: UtilityCategory,
  val icon: ImageVector,
)

private val UtilityCatalog =
  listOf(
    UtilityCatalogItem(
      UtilityDestinationId.BATTERY,
      "Battery Monitor",
      "Charge, temp, voltage & health",
      UtilityCategory.DEVICE,
      Icons.Rounded.BatteryFull,
    ),
    UtilityCatalogItem(
      UtilityDestinationId.RAM,
      "RAM Monitor",
      "Live active & free memory",
      UtilityCategory.DEVICE,
      Icons.Rounded.Memory,
    ),
    UtilityCatalogItem(
      UtilityDestinationId.STORAGE,
      "Storage Monitor",
      "Flash capacity & free space",
      UtilityCategory.DEVICE,
      Icons.Rounded.SdStorage,
    ),
    UtilityCatalogItem(
      UtilityDestinationId.DEVICE_INFO,
      "Device Information",
      "Hardware, Android OS & SoC",
      UtilityCategory.DEVICE,
      Icons.Rounded.PhoneAndroid,
    ),
    UtilityCatalogItem(
      UtilityDestinationId.NETWORK_INFO,
      "Network Information",
      "Transport, metered & bandwidth",
      UtilityCategory.DEVICE,
      Icons.Rounded.Wifi,
    ),
    UtilityCatalogItem(
      UtilityDestinationId.COMPASS,
      "Compass",
      "Advanced heading, altitude & GPS status",
      UtilityCategory.DEVICE,
      Icons.Rounded.Explore,
    ),
    UtilityCatalogItem(
      UtilityDestinationId.WEATHER,
      "Weather",
      "Hourly & 7-day Open-Meteo forecast",
      UtilityCategory.LIFESTYLE,
      Icons.Rounded.WbSunny,
    ),
    UtilityCatalogItem(
      UtilityDestinationId.CLOCK,
      "Clock",
      "Analog & digital world time",
      UtilityCategory.LIFESTYLE,
      Icons.Rounded.Schedule,
    ),
    UtilityCatalogItem(
      UtilityDestinationId.CALENDAR,
      "Calendar",
      "Upcoming device schedule",
      UtilityCategory.LIFESTYLE,
      Icons.Rounded.CalendarMonth,
    ),
    UtilityCatalogItem(
      UtilityDestinationId.NOTES,
      "Notes",
      "Pinned & auto-saved local notes",
      UtilityCategory.PRODUCTIVITY,
      Icons.Rounded.Description,
    ),
    UtilityCatalogItem(
      UtilityDestinationId.TODO,
      "To-do Lists",
      "Prioritized offline task lists",
      UtilityCategory.PRODUCTIVITY,
      Icons.Rounded.CheckBox,
    ),
    UtilityCatalogItem(
      UtilityDestinationId.CALCULATOR,
      "Calculator",
      "Standard & scientific math",
      UtilityCategory.PRODUCTIVITY,
      Icons.Rounded.Calculate,
    ),
    UtilityCatalogItem(
      UtilityDestinationId.TIMER,
      "Timer",
      "Countdown ring & alert",
      UtilityCategory.PRODUCTIVITY,
      Icons.Rounded.HourglassBottom,
    ),
    UtilityCatalogItem(
      UtilityDestinationId.STOPWATCH,
      "Stopwatch",
      "Precision lap timing",
      UtilityCategory.PRODUCTIVITY,
      Icons.Rounded.Timer,
    ),
    UtilityCatalogItem(
      UtilityDestinationId.MUSIC_PLAYER,
      "Music Player",
      "Album art, seek & playback",
      UtilityCategory.MEDIA,
      Icons.Rounded.MusicNote,
    ),
    UtilityCatalogItem(
      UtilityDestinationId.MEDIA_CONTROLS,
      "Media Controls",
      "Manage active media sessions",
      UtilityCategory.MEDIA,
      Icons.Rounded.Tune,
    ),
  )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(onOpenUtility: (UtilityDestinationId) -> Unit) {
  var searchQuery by rememberSaveable { mutableStateOf("") }
  var selectedCategory by rememberSaveable { mutableStateOf(UtilityCategory.ALL) }
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

  val filteredItems =
    remember(searchQuery, selectedCategory) {
      val q = searchQuery.trim()
      UtilityCatalog.filter { item ->
        val catOk = selectedCategory == UtilityCategory.ALL || item.category == selectedCategory
        val queryOk =
          q.isEmpty() ||
            item.title.contains(q, ignoreCase = true) ||
            item.subtitle.contains(q, ignoreCase = true) ||
            item.category.label.contains(q, ignoreCase = true)
        catOk && queryOk
      }
    }

  val grouped = remember(filteredItems) { filteredItems.groupBy { it.category } }

  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      JaBoardLargeTopBar(
        title = "Explore",
        subtitle = "15 dedicated native utilities",
        scrollBehavior = scrollBehavior,
      )
    },
  ) { innerPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("explore_search_input"),
        placeholder = { Text("Search utilities...") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(
                Icons.Rounded.Clear,
                contentDescription = stringResource(R.string.cd_clear_search),
              )
            }
          }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
      )

      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(UtilityCategory.entries) { category ->
          FilterChip(
            selected = selectedCategory == category,
            onClick = { selectedCategory = category },
            label = { Text(category.label) },
            modifier = Modifier.testTag("explore_chip_${category.name.lowercase()}"),
          )
        }
      }

      if (filteredItems.isEmpty()) {
        EmptyStateView(
          icon = Icons.Rounded.SearchOff,
          title = "No utilities found",
          message = "Try clearing your search filter or picking All categories.",
          actionLabel = "Show All",
          onAction = {
            searchQuery = ""
            selectedCategory = UtilityCategory.ALL
          },
          modifier = Modifier.weight(1f),
        )
      } else {
        LazyVerticalGrid(
          columns = GridCells.Adaptive(minSize = 162.dp),
          contentPadding = PaddingValues(16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxSize().testTag("explore_grid"),
        ) {
          grouped.forEach { (category, utilities) ->
            item(
              key = "cat_${category.name}",
              span = { GridItemSpan(maxLineSpan) },
            ) {
              Text(
                text = category.label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
              )
            }
            items(utilities, key = { it.id.name }) { item ->
              UtilityTileCard(item = item, onClick = { onOpenUtility(item.id) })
            }
          }
        }
      }
    }
  }
}

@Composable
private fun UtilityTileCard(item: UtilityCatalogItem, onClick: () -> Unit) {
  ExpressiveCard(
    modifier = Modifier.fillMaxWidth().testTag("explore_item_${item.id.name.lowercase()}"),
    onClick = onClick,
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      ) {
        Icon(
          imageVector = item.icon,
          contentDescription = null,
          modifier = Modifier.padding(10.dp).size(24.dp),
        )
      }
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(item.title, style = MaterialTheme.typography.titleMedium)
        Text(
          item.subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
        )
      }
    }
  }
}
