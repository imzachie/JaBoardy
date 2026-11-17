package com.example.jaboard.feature.music

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jaboard.core.common.Formatters
import com.example.jaboard.core.design.EmptyStateView
import com.example.jaboard.core.design.ExpressiveCard
import com.example.jaboard.core.design.JaBoardMonoStyles
import com.example.jaboard.core.design.JaBoardUtilityTopBar
import com.example.jaboard.core.design.PermissionPromptCard
import com.example.jaboard.core.network.ParsedLyricLine
import com.example.jaboard.core.system.ActiveMediaSessionInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
  viewModel: MusicViewModel,
  initialTab: String,
  onBack: () -> Unit,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val lyricsState by viewModel.lyricsState.collectAsStateWithLifecycle()
  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  var selectedTab by rememberSaveable(initialTab) {
    mutableIntStateOf(if (initialTab.equals("controls", ignoreCase = true)) 1 else 0)
  }

  Scaffold(
    topBar = {
      JaBoardUtilityTopBar(
        title = if (selectedTab == 0) "Music Player" else "Media Controls",
        subtitle = state.activeSession?.appLabel ?: "Android MediaSession",
        onBack = onBack,
        actions = {
          IconButton(
            onClick = viewModel::refresh,
            modifier = Modifier.testTag("music_refresh_button"),
          ) {
            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh sessions")
          }
        },
      )
    },
  ) { innerPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      PrimaryTabRow(selectedTabIndex = selectedTab) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("Now Playing") },
          modifier = Modifier.testTag("music_tab_player"),
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("Active Sessions") },
          modifier = Modifier.testTag("music_tab_sessions"),
        )
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        if (!state.hasNotificationAccess) {
          item {
            PermissionPromptCard(
              icon = Icons.Rounded.NotificationsActive,
              title = "Notification access enables live media controls",
              description =
                "Android exposes active media playback sessions (track info, album art, play/pause, and skip controls) only to apps granted Notification Listener access. JaBoard never reads or stores personal notification messages.",
              actionLabel = "Grant Notification Access",
              onAction = viewModel::openNotificationAccessSettings,
              actionTestTag = "music_grant_access_button",
            )
          }
        } else if (state.sessions.isEmpty()) {
          item {
            EmptyStateView(
              icon = Icons.Rounded.MusicNote,
              title = "No active media session",
              message =
                "Start playing audio in Spotify, YouTube Music, Pocket Casts, or any Android media app to control playback here.",
              actionLabel = "Check Again",
              onAction = viewModel::refresh,
            )
          }
        } else {
          if (state.sessions.size > 1) {
            item {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.sessions, key = { it.packageName }) { session ->
                  FilterChip(
                    selected = session.packageName == state.activeSession?.packageName,
                    onClick = { viewModel.selectSession(session.packageName) },
                    label = { Text(session.appLabel) },
                  )
                }
              }
            }
          }

          val active = state.activeSession
          if (active != null && selectedTab == 0) {
            item {
              FullMediaPlayerCard(
                session = active,
                onPrevious = viewModel::skipToPrevious,
                onPlayPause = viewModel::togglePlayPause,
                onNext = viewModel::skipToNext,
                onSeek = viewModel::seekTo,
                onOpenLyrics = viewModel::toggleLyricsVisibility,
              )
            }
          }

          if (selectedTab == 1 || active == null) {
            items(state.sessions, key = { it.packageName }) { session ->
              CompactSessionControlCard(
                session = session,
                isSelected = session.packageName == state.activeSession?.packageName,
                onSelect = { viewModel.selectSession(session.packageName) },
                onPrevious = {
                  viewModel.selectSession(session.packageName)
                  viewModel.skipToPrevious()
                },
                onPlayPause = {
                  viewModel.selectSession(session.packageName)
                  viewModel.togglePlayPause()
                },
                onNext = {
                  viewModel.selectSession(session.packageName)
                  viewModel.skipToNext()
                },
                onOpenLyrics = {
                  viewModel.selectSession(session.packageName)
                  viewModel.toggleLyricsVisibility()
                },
              )
            }
          }
        }
      }
    }
  }

  if (lyricsState.isLyricsVisible) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = viewModel::closeLyrics,
      sheetState = sheetState,
    ) {
      LyricsBottomSheetContent(
        session = state.activeSession,
        lyricsState = lyricsState,
        onRetry = viewModel::fetchLyricsForCurrentTrack,
        onSeekToLine = viewModel::seekToLine,
      )
    }
  }
}

@Composable
private fun FullMediaPlayerCard(
  session: ActiveMediaSessionInfo,
  onPrevious: () -> Unit,
  onPlayPause: () -> Unit,
  onNext: () -> Unit,
  onSeek: (Long) -> Unit,
  onOpenLyrics: () -> Unit,
) {
  ExpressiveCard(modifier = Modifier.fillMaxWidth().testTag("music_full_card")) {
    Column(
      modifier = Modifier.padding(22.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(0.74f).aspectRatio(1f),
      ) {
        if (session.albumArt != null) {
          Image(
            bitmap = session.albumArt.asImageBitmap(),
            contentDescription = "${session.title} album art",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)),
          )
        } else {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.Album,
              contentDescription = null,
              modifier = Modifier.size(84.dp),
              tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            )
          }
        }
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
          Text(
            text = "${session.appLabel} • ${session.playbackStateLabel}",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = session.title.ifBlank { "Unknown Title" },
          style = MaterialTheme.typography.headlineSmall,
          textAlign = TextAlign.Center,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text =
            listOf(session.artist, session.album)
              .filter { it.isNotBlank() }
              .joinToString(" • ")
              .ifBlank { "Unknown Artist" },
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      if (session.durationMs > 0L) {
        Column(modifier = Modifier.fillMaxWidth()) {
          val progress =
            (session.positionMs.toFloat() / session.durationMs.toFloat()).coerceIn(0f, 1f)
          Slider(
            value = progress,
            onValueChange = { frac -> onSeek((frac * session.durationMs).toLong()) },
            modifier = Modifier.fillMaxWidth().testTag("music_playback_slider"),
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(Formatters.formatMediaTime(session.positionMs), style = JaBoardMonoStyles.SpecValue)
            Text(Formatters.formatMediaTime(session.durationMs), style = JaBoardMonoStyles.SpecValue)
          }
        }
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        FilledTonalIconButton(
          onClick = onPrevious,
          modifier = Modifier.size(56.dp).testTag("music_prev_button"),
        ) {
          Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous track")
        }
        FilledIconButton(
          onClick = onPlayPause,
          modifier = Modifier.size(72.dp).testTag("music_play_pause_button"),
        ) {
          Icon(
            imageVector = if (session.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (session.isPlaying) "Pause" else "Play",
            modifier = Modifier.size(36.dp),
          )
        }
        FilledTonalIconButton(
          onClick = onNext,
          modifier = Modifier.size(56.dp).testTag("music_next_button"),
        ) {
          Icon(Icons.Rounded.SkipNext, contentDescription = "Next track")
        }
      }

      // LRCLIB Lyrics Launcher Button
      FilledTonalButton(
        onClick = onOpenLyrics,
        modifier = Modifier.fillMaxWidth().testTag("music_lyrics_button"),
      ) {
        Icon(Icons.Rounded.Lyrics, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Lyrics (LRCLIB)")
      }
    }
  }
}

@Composable
private fun CompactSessionControlCard(
  session: ActiveMediaSessionInfo,
  isSelected: Boolean,
  onSelect: () -> Unit,
  onPrevious: () -> Unit,
  onPlayPause: () -> Unit,
  onNext: () -> Unit,
  onOpenLyrics: () -> Unit,
) {
  ExpressiveCard(
    modifier = Modifier.fillMaxWidth(),
    containerColor =
      if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
      else MaterialTheme.colorScheme.surfaceContainerLow,
    onClick = onSelect,
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.size(56.dp),
      ) {
        if (session.albumArt != null) {
          Image(
            bitmap = session.albumArt.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null)
          }
        }
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          session.appLabel,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          session.title.ifBlank { "Unknown Title" },
          style = MaterialTheme.typography.titleSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          session.artist.ifBlank { "Unknown Artist" },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onOpenLyrics) {
          Icon(Icons.Rounded.Lyrics, contentDescription = "Lyrics")
        }
        IconButton(onClick = onPrevious) {
          Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous")
        }
        FilledTonalIconButton(onClick = onPlayPause) {
          Icon(
            if (session.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (session.isPlaying) "Pause" else "Play",
          )
        }
        IconButton(onClick = onNext) {
          Icon(Icons.Rounded.SkipNext, contentDescription = "Next")
        }
      }
    }
  }
}

@Composable
private fun LyricsBottomSheetContent(
  session: ActiveMediaSessionInfo?,
  lyricsState: LyricsUiState,
  onRetry: () -> Unit,
  onSeekToLine: (ParsedLyricLine) -> Unit,
) {
  val listState = rememberLazyListState()

  LaunchedEffect(lyricsState.activeLineIndex) {
    if (lyricsState.activeLineIndex >= 0 && !listState.isScrollInProgress) {
      val target = (lyricsState.activeLineIndex - 2).coerceAtLeast(0)
      listState.animateScrollToItem(target)
    }
  }

  Column(
    modifier = Modifier.fillMaxWidth().height(520.dp).padding(horizontal = 24.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = session?.title ?: "Track Lyrics",
          style = MaterialTheme.typography.titleLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = session?.artist ?: "LRCLIB Lyrics",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
      ) {
        Text(
          text = when {
            lyricsState.lyrics?.syncedLines?.isNotEmpty() == true -> "Synced LRCLIB"
            lyricsState.lyrics?.isInstrumental == true -> "Instrumental"
            lyricsState.lyrics?.plainLyrics != null -> "Plain LRCLIB"
            else -> "LRCLIB"
          },
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
      }
    }

    when {
      lyricsState.isLoading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            CircularProgressIndicator(modifier = Modifier.size(36.dp))
            Text("Searching LRCLIB lyrics...", style = MaterialTheme.typography.bodyMedium)
          }
        }
      }

      lyricsState.error != null -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              Icons.Rounded.GraphicEq,
              contentDescription = null,
              modifier = Modifier.size(48.dp),
              tint = MaterialTheme.colorScheme.outline,
            )
            Text(
              lyricsState.error,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
            OutlinedButton(onClick = onRetry) {
              Text("Retry Search")
            }
          }
        }
      }

      lyricsState.lyrics != null -> {
        val lyrics = lyricsState.lyrics
        if (lyrics.isInstrumental) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
              "🎵 This track is instrumental (no lyrics).",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else if (lyrics.syncedLines.isNotEmpty()) {
          LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
          ) {
            itemsIndexed(lyrics.syncedLines) { index, line ->
              val isActive = index == lyricsState.activeLineIndex
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { onSeekToLine(line) },
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = line.text.ifBlank { "♪" },
                    style = if (isActive) {
                      MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 18.sp,
                      )
                    } else {
                      MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                      )
                    },
                    modifier = Modifier.weight(1f),
                  )
                  if (isActive) {
                    Text(
                      text = Formatters.formatMediaTime(line.timestampMs),
                      style = JaBoardMonoStyles.SpecValue,
                      color = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.padding(start = 8.dp),
                    )
                  }
                }
              }
            }
          }
        } else if (!lyrics.plainLyrics.isNullOrBlank()) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(vertical = 12.dp),
          ) {
            Text(
              text = lyrics.plainLyrics,
              style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        } else {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No lyrics content found for this track.")
          }
        }
      }

      else -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          OutlinedButton(onClick = onRetry) {
            Text("Fetch Lyrics")
          }
        }
      }
    }
  }
}
