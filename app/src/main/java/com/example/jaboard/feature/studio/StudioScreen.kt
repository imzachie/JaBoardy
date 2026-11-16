package com.example.jaboard.feature.studio

import android.os.Build
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jaboard.core.common.Formatters
import com.example.jaboard.core.database.CardSize
import com.example.jaboard.core.database.DashboardCardEntity
import com.example.jaboard.core.database.DashboardCardType
import com.example.jaboard.core.database.DashboardPreset
import com.example.jaboard.core.design.ExpressiveCard
import com.example.jaboard.core.design.JaBoardLargeTopBar
import com.example.jaboard.core.design.JaBoardMonoStyles
import com.example.jaboard.core.design.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(viewModel: StudioViewModel) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  val listState = rememberLazyListState()
  val haptics = LocalHapticFeedback.current

  var draggedCardIndex by remember { mutableIntStateOf(-1) }
  var dragOffsetY by remember { mutableFloatStateOf(0f) }

  // Header items before the card items inside LazyColumn = 3 (Theme, Presets, Section Header)
  val cardListStartOffset = 3

  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      JaBoardLargeTopBar(
        title = "Studio",
        subtitle = "Customize dashboard layout & Material You theme",
        scrollBehavior = scrollBehavior,
        actions = {
          IconButton(
            onClick = viewModel::restoreDefaults,
            modifier = Modifier.testTag("studio_reset_button"),
          ) {
            Icon(Icons.Rounded.RestartAlt, contentDescription = "Restore default layout")
          }
        },
      )
    },
  ) { innerPadding ->
    LazyColumn(
      state = listState,
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .testTag("studio_list")
          .pointerInput(uiState.cards) {
            detectDragGesturesAfterLongPress(
              onDragStart = { offset ->
                val hit =
                  listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                    offset.y.toInt() in item.offset..(item.offset + item.size)
                  }
                val cardIdx = (hit?.index ?: -1) - cardListStartOffset
                if (cardIdx in uiState.cards.indices) {
                  draggedCardIndex = cardIdx
                  dragOffsetY = 0f
                  haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
              },
              onDrag = { change, dragAmount ->
                if (draggedCardIndex < 0) return@detectDragGesturesAfterLongPress
                change.consume()
                dragOffsetY += dragAmount.y
                val currentItem =
                  listState.layoutInfo.visibleItemsInfo.firstOrNull {
                    it.index == draggedCardIndex + cardListStartOffset
                  }
                if (currentItem != null) {
                  val centerY = currentItem.offset + currentItem.size / 2f + dragOffsetY
                  val targetItem =
                    listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                      val targetCardIdx = item.index - cardListStartOffset
                      targetCardIdx in uiState.cards.indices &&
                        targetCardIdx != draggedCardIndex &&
                        centerY.toInt() in item.offset..(item.offset + item.size)
                    }
                  if (targetItem != null) {
                    val targetCardIdx = targetItem.index - cardListStartOffset
                    viewModel.moveCard(draggedCardIndex, targetCardIdx)
                    draggedCardIndex = targetCardIdx
                    dragOffsetY = 0f
                  }
                }
              },
              onDragEnd = {
                draggedCardIndex = -1
                dragOffsetY = 0f
              },
              onDragCancel = {
                draggedCardIndex = -1
                dragOffsetY = 0f
              },
            )
          },
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // 0: Theme & Dynamic Color card
      item(key = "studio_theme") {
        ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text("Theme & Dynamic Color", style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
              ThemeMode.entries.forEachIndexed { idx, mode ->
                SegmentedButton(
                  selected = uiState.preferences.themeMode == mode,
                  onClick = { viewModel.setThemeMode(mode) },
                  shape = SegmentedButtonDefaults.itemShape(idx, ThemeMode.entries.size),
                  modifier = Modifier.testTag("studio_theme_${mode.name.lowercase()}"),
                ) {
                  Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                }
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("Material You Dynamic Color", style = MaterialTheme.typography.titleSmall)
                Text(
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    "Adapts colors from your system wallpaper (Pixel Cobalt when off)"
                  else "Requires Android 12+. Using Pixel Cobalt palette.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(
                checked = uiState.preferences.useDynamicColor,
                onCheckedChange = viewModel::setUseDynamicColor,
                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                modifier = Modifier.testTag("studio_dynamic_color_switch"),
              )
            }
          }
        }
      }

      // 1: Layout Presets
      item(key = "studio_presets") {
        ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text("Layout Presets", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(DashboardPreset.entries) { preset ->
                FilterChip(
                  selected = uiState.preferences.activePreset == preset,
                  onClick = { viewModel.applyPreset(preset) },
                  label = { Text(preset.label) },
                  modifier = Modifier.testTag("studio_preset_${preset.name.lowercase()}"),
                )
              }
            }
            Text(
              uiState.preferences.activePreset.description,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // 2: Section Header (moved up since Clock Format removed - now in Settings)
      item(key = "studio_cards_header") {
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text("Dashboard Widgets", style = MaterialTheme.typography.titleMedium)
            Text(
              "Long-press to drag or use arrows to reorder",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // 3..N: Dashboard cards
      itemsIndexed(uiState.cards, key = { _, card -> card.cardId }) { index, card ->
        val isDragging = index == draggedCardIndex
        StudioWidgetConfigCard(
          card = card,
          canMoveUp = index > 0,
          canMoveDown = index < uiState.cards.lastIndex,
          modifier =
            Modifier.graphicsLayer {
              if (isDragging) {
                translationY = dragOffsetY
                scaleX = 1.02f
                scaleY = 1.02f
                alpha = 0.92f
              }
            },
          onMoveUp = { viewModel.moveCard(index, index - 1) },
          onMoveDown = { viewModel.moveCard(index, index + 1) },
          onToggleVisible = { visible -> viewModel.toggleVisibility(card.cardId, visible) },
          onSelectSize = { size -> viewModel.setCardSize(card.cardId, size) },
        )
      }

      // Footer Reset button
      item(key = "studio_footer_reset") {
        OutlinedButton(
          onClick = viewModel::restoreDefaults,
          modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        ) {
          Icon(Icons.Rounded.RestartAlt, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Restore Default Layout")
        }
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

@Composable
private fun StudioWidgetConfigCard(
  card: DashboardCardEntity,
  canMoveUp: Boolean,
  canMoveDown: Boolean,
  modifier: Modifier = Modifier,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
  onToggleVisible: (Boolean) -> Unit,
  onSelectSize: (CardSize) -> Unit,
) {
  val meta = DashboardCardType.fromId(card.cardId)
  val currentSize = CardSize.fromName(card.cardSize)

  ExpressiveCard(modifier = modifier.fillMaxWidth().testTag("studio_card_${card.cardId}")) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          Icons.Rounded.DragHandle,
          contentDescription = "Drag handle",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(meta.defaultTitle, style = MaterialTheme.typography.titleMedium)
          Text(
            meta.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
          Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up")
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
          Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down")
        }
        Switch(
          checked = card.isVisible,
          onCheckedChange = onToggleVisible,
          modifier = Modifier.testTag("studio_toggle_${card.cardId}"),
        )
      }

      if (card.isVisible && meta.supportedSizes.size > 1) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          meta.supportedSizes.forEachIndexed { idx, size ->
            SegmentedButton(
              selected = currentSize == size,
              onClick = { onSelectSize(size) },
              shape = SegmentedButtonDefaults.itemShape(idx, meta.supportedSizes.size),
              modifier = Modifier.testTag("studio_size_${card.cardId}_${size.name.lowercase()}"),
            ) {
              Text(size.label)
            }
          }
        }
      }
    }
  }
}
