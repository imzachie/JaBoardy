package com.example.jaboard.core.system

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JaBoardMediaListenerService : NotificationListenerService() {
  override fun onListenerConnected() {
    super.onListenerConnected()
    MediaSessionHolder.onListenerConnectedChanged(true)
  }

  override fun onListenerDisconnected() {
    super.onListenerDisconnected()
    MediaSessionHolder.onListenerConnectedChanged(false)
  }
}

object MediaSessionHolder {
  private val _listenerConnected = MutableStateFlow(false)
  val listenerConnected: StateFlow<Boolean> = _listenerConnected.asStateFlow()

  fun onListenerConnectedChanged(connected: Boolean) {
    _listenerConnected.value = connected
  }
}

data class ActiveMediaSessionInfo(
  val packageName: String,
  val appLabel: String,
  val title: String,
  val artist: String,
  val album: String,
  val albumArt: Bitmap?,
  val isPlaying: Boolean,
  val playbackStateLabel: String,
  val durationMs: Long,
  val positionMs: Long,
)

data class MediaSessionsUiState(
  val hasNotificationAccess: Boolean = false,
  val sessions: List<ActiveMediaSessionInfo> = emptyList(),
  val selectedPackageName: String? = null,
) {
  val activeSession: ActiveMediaSessionInfo?
    get() =
      sessions.firstOrNull { it.packageName == selectedPackageName }
        ?: sessions.firstOrNull { it.isPlaying }
        ?: sessions.firstOrNull()
}

class MediaSessionRepository(private val context: Context) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val componentName = ComponentName(context, JaBoardMediaListenerService::class.java)
  private val sessionManager =
    context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

  private val _state = MutableStateFlow(MediaSessionsUiState())
  val state: StateFlow<MediaSessionsUiState> = _state.asStateFlow()

  private var activeControllers: List<MediaController> = emptyList()
  private val controllerCallbacks = mutableMapOf<MediaController, MediaController.Callback>()
  private var sessionListenerRegistered = false
  private var progressTickerJob: Job? = null

  private val activeSessionsChangedListener =
    MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
      bindControllers(controllers.orEmpty())
    }

  init {
    refreshState()
  }

  fun hasNotificationListenerAccess(): Boolean {
    return NotificationManagerCompat.getEnabledListenerPackages(context)
      .contains(context.packageName)
  }

  fun openNotificationAccessSettings() {
    val intent =
      Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    context.startActivity(intent)
  }

  fun refreshState() {
    val hasAccess = hasNotificationListenerAccess()
    if (!hasAccess) {
      clearControllers()
      _state.value = MediaSessionsUiState(hasNotificationAccess = false, sessions = emptyList())
      return
    }

    runCatching {
      if (!sessionListenerRegistered) {
        sessionManager.addOnActiveSessionsChangedListener(activeSessionsChangedListener, componentName)
        sessionListenerRegistered = true
      }
      val controllers = sessionManager.getActiveSessions(componentName)
      bindControllers(controllers)
    }.onFailure {
      clearControllers()
      _state.value = MediaSessionsUiState(hasNotificationAccess = hasAccess, sessions = emptyList())
    }
  }

  fun selectSession(packageName: String) {
    _state.value = _state.value.copy(selectedPackageName = packageName)
  }

  fun togglePlayPause() {
    val controller = findTargetController() ?: return
    val isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING
    if (isPlaying) {
      controller.transportControls.pause()
    } else {
      controller.transportControls.play()
    }
  }

  fun skipToNext() {
    findTargetController()?.transportControls?.skipToNext()
  }

  fun skipToPrevious() {
    findTargetController()?.transportControls?.skipToPrevious()
  }

  fun seekTo(positionMs: Long) {
    findTargetController()?.transportControls?.seekTo(positionMs.coerceAtLeast(0L))
  }

  private fun findTargetController(): MediaController? {
    val targetPkg = _state.value.activeSession?.packageName
    return activeControllers.firstOrNull { it.packageName == targetPkg }
      ?: activeControllers.firstOrNull()
  }

  private fun bindControllers(controllers: List<MediaController>) {
    clearControllers()
    activeControllers = controllers
    controllers.forEach { controller ->
      val cb =
        object : MediaController.Callback() {
          override fun onPlaybackStateChanged(state: PlaybackState?) {
            emitCurrentSessions()
          }

          override fun onMetadataChanged(metadata: MediaMetadata?) {
            emitCurrentSessions()
          }
        }
      controller.registerCallback(cb)
      controllerCallbacks[controller] = cb
    }
    emitCurrentSessions()
    ensureProgressTicker()
  }

  private fun clearControllers() {
    controllerCallbacks.forEach { (controller, cb) ->
      runCatching { controller.unregisterCallback(cb) }
    }
    controllerCallbacks.clear()
    activeControllers = emptyList()
    progressTickerJob?.cancel()
    progressTickerJob = null
  }

  private fun ensureProgressTicker() {
    progressTickerJob?.cancel()
    if (activeControllers.any { it.playbackState?.state == PlaybackState.STATE_PLAYING }) {
      progressTickerJob =
        scope.launch {
          while (true) {
            delay(1000L)
            emitCurrentSessions()
          }
        }
    }
  }

  private fun emitCurrentSessions() {
    val infos = activeControllers.map { toSessionInfo(it) }
    val currentSelected = _state.value.selectedPackageName
    val resolvedSelected =
      if (infos.any { it.packageName == currentSelected }) currentSelected
      else infos.firstOrNull { it.isPlaying }?.packageName ?: infos.firstOrNull()?.packageName

    _state.value =
      MediaSessionsUiState(
        hasNotificationAccess = hasNotificationListenerAccess(),
        sessions = infos,
        selectedPackageName = resolvedSelected,
      )
  }

  private fun toSessionInfo(controller: MediaController): ActiveMediaSessionInfo {
    val meta = controller.metadata
    val pb = controller.playbackState
    val pkg = controller.packageName
    val appLabel =
      runCatching {
        val ai = context.packageManager.getApplicationInfo(pkg, 0)
        context.packageManager.getApplicationLabel(ai).toString()
      }.getOrDefault(pkg)

    val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() } ?: "Unknown Track"
    val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() } ?: appLabel
    val album = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty()
    val art =
      meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
    val duration = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0L) ?: 0L
    val stateCode = pb?.state ?: PlaybackState.STATE_NONE
    val isPlaying = stateCode == PlaybackState.STATE_PLAYING
    val stateLabel =
      when (stateCode) {
        PlaybackState.STATE_PLAYING -> "Playing"
        PlaybackState.STATE_PAUSED -> "Paused"
        PlaybackState.STATE_BUFFERING -> "Buffering"
        PlaybackState.STATE_STOPPED -> "Stopped"
        else -> "Idle"
      }

    val rawPos = pb?.position ?: 0L
    val computedPos =
      if (isPlaying && pb != null && pb.lastPositionUpdateTime > 0L) {
        val delta = android.os.SystemClock.elapsedRealtime() - pb.lastPositionUpdateTime
        (rawPos + (delta * pb.playbackSpeed).toLong()).coerceIn(0L, duration.takeIf { it > 0 } ?: Long.MAX_VALUE)
      } else {
        rawPos.coerceAtLeast(0L)
      }

    return ActiveMediaSessionInfo(
      packageName = pkg,
      appLabel = appLabel,
      title = title,
      artist = artist,
      album = album,
      albumArt = art,
      isPlaying = isPlaying,
      playbackStateLabel = stateLabel,
      durationMs = duration,
      positionMs = computedPos,
    )
  }
}
