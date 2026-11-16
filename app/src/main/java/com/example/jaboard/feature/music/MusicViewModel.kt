package com.example.jaboard.feature.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jaboard.core.network.LrclibRepository
import com.example.jaboard.core.network.ParsedLyricLine
import com.example.jaboard.core.network.ParsedLyrics
import com.example.jaboard.core.system.MediaSessionRepository
import com.example.jaboard.core.system.MediaSessionsUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LyricsUiState(
  val isLoading: Boolean = false,
  val lyrics: ParsedLyrics? = null,
  val error: String? = null,
  val activeLineIndex: Int = -1,
  val isLyricsVisible: Boolean = false,
)

class MusicViewModel(
  private val mediaSessionRepository: MediaSessionRepository,
  private val lrclibRepository: LrclibRepository? = null,
) : ViewModel() {

  val uiState: StateFlow<MediaSessionsUiState> = mediaSessionRepository.state

  private val _rawLyrics = MutableStateFlow<ParsedLyrics?>(null)
  private val _isLoadingLyrics = MutableStateFlow(false)
  private val _lyricsError = MutableStateFlow<String?>(null)
  private val _isLyricsVisible = MutableStateFlow(false)

  private var lastFetchedTrack: String? = null
  private var fetchJob: Job? = null

  val lyricsState: StateFlow<LyricsUiState> =
    combine(
      _rawLyrics,
      _isLoadingLyrics,
      _lyricsError,
      _isLyricsVisible,
      mediaSessionRepository.state,
    ) { lyrics, loading, error, visible, mediaState ->
      val pos = mediaState.activeSession?.positionMs ?: 0L
      val lines = lyrics?.syncedLines.orEmpty()
      val activeIndex =
        if (lines.isEmpty()) -1
        else lines.indexOfLast { it.timestampMs <= pos }.coerceAtLeast(0)

      LyricsUiState(
        isLoading = loading,
        lyrics = lyrics,
        error = error,
        activeLineIndex = activeIndex,
        isLyricsVisible = visible,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = LyricsUiState(),
    )

  fun refresh() {
    mediaSessionRepository.refreshState()
    checkAutoFetchLyrics()
  }

  fun toggleLyricsVisibility() {
    val next = !_isLyricsVisible.value
    _isLyricsVisible.value = next
    if (next && _rawLyrics.value == null) {
      fetchLyricsForCurrentTrack()
    }
  }

  fun closeLyrics() {
    _isLyricsVisible.value = false
  }

  fun fetchLyricsForCurrentTrack() {
    val session = uiState.value.activeSession ?: run {
      _lyricsError.value = "No active track playing to fetch lyrics for."
      return
    }
    val title = session.title
    val artist = session.artist

    if (title.isBlank()) {
      _lyricsError.value = "Track title is missing."
      return
    }

    val trackKey = "$title - $artist"
    lastFetchedTrack = trackKey

    fetchJob?.cancel()
    fetchJob = viewModelScope.launch {
      _isLoadingLyrics.value = true
      _lyricsError.value = null

      val repo = lrclibRepository ?: LrclibRepository()
      val result = repo.fetchLyrics(
        trackName = title,
        artistName = artist,
        durationMs = session.durationMs.takeIf { it > 0 },
      )

      result.onSuccess { parsed ->
        _rawLyrics.value = parsed
        _lyricsError.value = null
      }.onFailure { err ->
        _rawLyrics.value = null
        _lyricsError.value = err.message ?: "Lyrics not found"
      }

      _isLoadingLyrics.value = false
    }
  }

  private fun checkAutoFetchLyrics() {
    val session = uiState.value.activeSession ?: return
    val trackKey = "${session.title} - ${session.artist}"
    if (_isLyricsVisible.value && trackKey != lastFetchedTrack) {
      fetchLyricsForCurrentTrack()
    }
  }

  fun seekToLine(line: ParsedLyricLine) {
    seekTo(line.timestampMs)
  }

  fun openNotificationAccessSettings() = mediaSessionRepository.openNotificationAccessSettings()

  fun selectSession(packageName: String) = mediaSessionRepository.selectSession(packageName)

  fun togglePlayPause() = mediaSessionRepository.togglePlayPause()

  fun skipToNext() {
    mediaSessionRepository.skipToNext()
    _rawLyrics.value = null
    _lyricsError.value = null
  }

  fun skipToPrevious() {
    mediaSessionRepository.skipToPrevious()
    _rawLyrics.value = null
    _lyricsError.value = null
  }

  fun seekTo(positionMs: Long) = mediaSessionRepository.seekTo(positionMs)
}
