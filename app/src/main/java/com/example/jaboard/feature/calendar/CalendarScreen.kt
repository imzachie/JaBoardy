package com.example.jaboard.feature.calendar

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.jaboard.core.design.EmptyStateView
import com.example.jaboard.core.design.ExpressiveCard
import com.example.jaboard.core.design.JaBoardUtilityTopBar
import com.example.jaboard.core.design.PermissionPromptCard
import com.example.jaboard.core.system.CalendarEventItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel, onBack: () -> Unit) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
      viewModel.refresh()
    }

  val groupedEvents =
    remember(uiState.events) {
      val dayFormatter = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
      uiState.events.groupBy { dayFormatter.format(Date(it.startMillis)) }
    }

  Scaffold(
    topBar = {
      JaBoardUtilityTopBar(
        title = "Calendar",
        subtitle = "Next 14 days",
        onBack = onBack,
        actions = {
          IconButton(onClick = viewModel::refresh) {
            Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.cd_refresh))
          }
        },
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (uiState.isLoading) {
        item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
      }

      if (!uiState.hasPermission) {
        item {
          PermissionPromptCard(
            icon = Icons.Rounded.CalendarMonth,
            title = "Show your upcoming schedule",
            description =
              "Grant read-only calendar access to view your upcoming meetings and reminders directly inside JaBoard. Calendar access is completely optional.",
            actionLabel = "Allow Calendar Access",
            onAction = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) },
            actionTestTag = "calendar_grant_button",
          )
        }
      } else if (!uiState.isLoading && uiState.events.isEmpty()) {
        item {
          EmptyStateView(
            icon = Icons.Rounded.EventAvailable,
            title = "Your schedule is clear",
            message = "No upcoming events found in your device calendars for the next 14 days.",
            actionLabel = "Refresh",
            onAction = viewModel::refresh,
          )
        }
      } else {
        groupedEvents.forEach { (dayLabel, dayEvents) ->
          item(key = "header_$dayLabel") {
            Text(
              text = dayLabel,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
            )
          }
          items(dayEvents, key = { "${it.id}_${it.startMillis}" }) { event ->
            CalendarEventRowCard(event)
          }
        }
      }
    }
  }
}

@Composable
private fun CalendarEventRowCard(event: CalendarEventItem) {
  val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
  val timeText =
    if (event.allDay) "All day"
    else "${timeFmt.format(Date(event.startMillis))} – ${timeFmt.format(Date(event.endMillis))}"
  val dotColor =
    if (event.calendarColor != 0) Color(event.calendarColor)
    else MaterialTheme.colorScheme.primary

  ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(dotColor))
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(event.title, style = MaterialTheme.typography.titleMedium)
        Text(
          timeText,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!event.location.isNullOrBlank()) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Rounded.Place,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              event.location,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}
