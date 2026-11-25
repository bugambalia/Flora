package com.example.audio

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.example.data.model.AudioTrack
import com.example.data.model.BotanicalCatalog
import com.example.data.model.PlaybackState
import com.example.data.model.RepeatModeOption
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class PlaybackController(private val context: Context) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var controllerFuture: ListenableFuture<MediaController>? = null
  private var mediaController: MediaController? = null
  private var progressJob: Job? = null
  private var sleepTimerJob: Job? = null

  private val _playbackState = MutableStateFlow(PlaybackState())
  val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

  private val _positionMsFlow = MutableStateFlow(0L)
  val positionMsFlow: StateFlow<Long> = _positionMsFlow.asStateFlow()

  private val _sleepMinutesRemaining = MutableStateFlow<Int?>(null)
  val sleepMinutesRemaining: StateFlow<Int?> = _sleepMinutesRemaining.asStateFlow()

  private val _trackTransitionEvents = MutableSharedFlow<AudioTrack>(extraBufferCapacity = 8)
  val trackTransitionEvents: SharedFlow<AudioTrack> = _trackTransitionEvents.asSharedFlow()

  private val _playbackErrors = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val playbackErrors: SharedFlow<String> = _playbackErrors.asSharedFlow()

  private var pendingSpeed: Float = 1.0f
  private var pendingGapless: Boolean = true
  private var pendingCrossfade: Boolean = true
  private var pendingEqEnabled: Boolean = true
  private var pendingBands: List<Int> = listOf(120, 60, 0, 140, 220)
  private var pendingBass: Int = 240
  private var pendingVirtualizer: Int = 180
  private var lastAutoCrossfadeTrackId: Long? = null
  private var pendingRestoredStartPositionMs: Long? = null

  fun connect() {
    if (controllerFuture != null) return
    val token = SessionToken(context, ComponentName(context, BotanicalPlaybackService::class.java))
    val future = MediaController.Builder(context, token).buildAsync()
    controllerFuture = future
    future.addListener({
      runCatching {
        val controller = future.get()
        mediaController = controller
        attachListener(controller)
        applyPendingAudioSettings(controller)
        val restoredQueue = _playbackState.value.queue
        if (controller.mediaItemCount == 0 && restoredQueue.isNotEmpty()) {
          val safeIdx = _playbackState.value.currentIndex.coerceIn(0, restoredQueue.lastIndex)
          val startPos = pendingRestoredStartPositionMs ?: _positionMsFlow.value
          controller.shuffleModeEnabled = _playbackState.value.shuffleEnabled
          controller.repeatMode = when (_playbackState.value.repeatMode) {
            RepeatModeOption.OFF -> Player.REPEAT_MODE_OFF
            RepeatModeOption.ALL -> Player.REPEAT_MODE_ALL
            RepeatModeOption.ONE -> Player.REPEAT_MODE_ONE
          }
          controller.setMediaItems(restoredQueue.map { it.toMediaItem() }, safeIdx, startPos)
          controller.prepare()
          pendingRestoredStartPositionMs = null
        } else {
          syncStateFromPlayer(controller)
        }
      }
    }, ContextCompat.getMainExecutor(context))
  }

  fun disconnect() {
    progressJob?.cancel()
    controllerFuture?.let { MediaController.releaseFuture(it) }
    controllerFuture = null
    mediaController = null
  }

  private fun attachListener(controller: MediaController) {
    controller.addListener(object : Player.Listener {
      override fun onIsPlayingChanged(isPlaying: Boolean) {
        _playbackState.update {
          it.copy(
            isPlaying = isPlaying,
            errorMessage = if (isPlaying) null else it.errorMessage,
          )
        }
        if (isPlaying) startProgressLoop() else stopProgressLoop()
      }

      override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val currentIdx = controller.currentMediaItemIndex
        val track = _playbackState.value.queue.getOrNull(currentIdx)
        val wasGapless = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
        _positionMsFlow.value = 0L
        _playbackState.update {
          it.copy(
            currentIndex = currentIdx,
            currentTrack = track,
            positionMs = 0L,
            durationMs = controller.duration.takeIf { d -> d > 0 } ?: track?.durationMs ?: 0L,
            gaplessTransitionsCount = if (wasGapless) it.gaplessTransitionsCount + 1 else it.gaplessTransitionsCount,
            errorMessage = null,
          )
        }
        if (track != null) {
          _trackTransitionEvents.tryEmit(track)
        }
      }

      override fun onPlaybackStateChanged(playbackState: Int) {
        syncStateFromPlayer(controller)
      }

      override fun onPlayerError(error: PlaybackException) {
        val trackName = _playbackState.value.currentTrack?.title ?: "la pista"
        val msg = when (error.errorCode) {
          PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
            "No se encontró el archivo de «$trackName»."
          PlaybackException.ERROR_CODE_IO_NO_PERMISSION ->
            "Permiso revocado para reproducir «$trackName». Vuelve a seleccionar la carpeta."
          PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
          PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
          PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
          PlaybackException.ERROR_CODE_DECODING_FAILED ->
            "Formato no compatible o archivo dañado en «$trackName»."
          else ->
            "No se pudo reproducir «$trackName» (${error.errorCodeName})."
        }
        stopProgressLoop()
        _playbackState.update {
          it.copy(
            isPlaying = false,
            isBuffering = false,
            errorMessage = msg,
          )
        }
        _playbackErrors.tryEmit(msg)
      }

      override fun onRepeatModeChanged(repeatMode: Int) {
        _playbackState.update {
          it.copy(
            repeatMode = when (repeatMode) {
              Player.REPEAT_MODE_ALL -> RepeatModeOption.ALL
              Player.REPEAT_MODE_ONE -> RepeatModeOption.ONE
              else -> RepeatModeOption.OFF
            }
          )
        }
      }

      override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        _playbackState.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
      }
    })
  }

  private fun syncStateFromPlayer(controller: MediaController) {
    val idx = controller.currentMediaItemIndex
    val track = _playbackState.value.queue.getOrNull(idx)
    val pos = controller.currentPosition.coerceAtLeast(0L)
    val buffering = controller.playbackState == Player.STATE_BUFFERING
    if (_playbackState.value.queue.isNotEmpty() || controller.mediaItemCount > 0) {
      _positionMsFlow.value = pos
    }
    _playbackState.update {
      it.copy(
        currentIndex = if (idx >= 0) idx else it.currentIndex,
        currentTrack = track ?: it.currentTrack,
        isPlaying = controller.isPlaying,
        isBuffering = buffering,
        durationMs = controller.duration.takeIf { d -> d > 0 } ?: track?.durationMs ?: it.durationMs,
        bufferedPercentage = controller.bufferedPercentage.coerceIn(0, 100),
      )
    }
    if (controller.isPlaying) startProgressLoop() else stopProgressLoop()
  }

  private fun startProgressLoop() {
    if (progressJob?.isActive == true) return
    progressJob = scope.launch {
      while (isActive) {
        mediaController?.let { c ->
          if (c.isPlaying) {
            val pos = c.currentPosition.coerceAtLeast(0L)
            val state = _playbackState.value
            val dur = c.duration.takeIf { d -> d > 0 } ?: state.currentTrack?.durationMs ?: 0L
            _positionMsFlow.value = pos
            val bufPct = c.bufferedPercentage.coerceIn(0, 100)
            if ((dur > 0L && dur != state.durationMs) || bufPct != state.bufferedPercentage) {
              _playbackState.update {
                it.copy(
                  durationMs = if (dur > 0L) dur else it.durationMs,
                  bufferedPercentage = bufPct,
                )
              }
            }
            val currentId = state.currentTrack?.id
            val remaining = dur - pos
            if (pendingCrossfade &&
              dur > 4_000L &&
              remaining in 1L..CrossfadeAudioProcessor.DEFAULT_CROSSFADE_MS &&
              c.hasNextMediaItem() &&
              c.repeatMode != Player.REPEAT_MODE_ONE &&
              currentId != null &&
              currentId != lastAutoCrossfadeTrackId
            ) {
              lastAutoCrossfadeTrackId = currentId
              armCrossfadeOnSession(c)
              c.seekToNextMediaItem()
            }
          }
        }
        delay(250L)
      }
    }
  }

  private fun stopProgressLoop() {
    progressJob?.cancel()
    progressJob = null
  }

  fun clearErrorMessage() {
    _playbackState.update { it.copy(errorMessage = null) }
  }

  fun prepareRestoredSession(
    tracks: List<AudioTrack>,
    startIndex: Int,
    positionMs: Long,
    playlistName: String?,
    shuffleEnabled: Boolean,
    repeatMode: RepeatModeOption,
  ) {
    if (tracks.isEmpty()) return
    val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
    val selected = tracks[safeIndex]
    val clampedPos = positionMs.coerceIn(0L, selected.durationMs.coerceAtLeast(1L))
    pendingRestoredStartPositionMs = clampedPos
    _positionMsFlow.value = clampedPos
    _playbackState.update {
      it.copy(
        queue = tracks,
        currentIndex = safeIndex,
        currentTrack = selected,
        activePlaylistName = playlistName,
        positionMs = clampedPos,
        durationMs = selected.durationMs,
        shuffleEnabled = shuffleEnabled,
        repeatMode = repeatMode,
        isPlaying = false,
        isBuffering = false,
        errorMessage = null,
      )
    }

    val controller = mediaController
    if (controller != null && controller.mediaItemCount == 0 && !controller.isPlaying) {
      controller.shuffleModeEnabled = shuffleEnabled
      controller.repeatMode = when (repeatMode) {
        RepeatModeOption.OFF -> Player.REPEAT_MODE_OFF
        RepeatModeOption.ALL -> Player.REPEAT_MODE_ALL
        RepeatModeOption.ONE -> Player.REPEAT_MODE_ONE
      }
      controller.setMediaItems(tracks.map { it.toMediaItem() }, safeIndex, clampedPos)
      controller.prepare()
      pendingRestoredStartPositionMs = null
    }
  }

  fun syncTracksUpdated(latestTracks: List<AudioTrack>) {
    val byId = latestTracks.associateBy { it.id }
    val state = _playbackState.value
    if (state.queue.isEmpty()) return
    val updatedQueue = state.queue.mapNotNull { byId[it.id] }
    if (updatedQueue == state.queue) return
    if (updatedQueue.isEmpty()) {
      mediaController?.stop()
      mediaController?.clearMediaItems()
      _positionMsFlow.value = 0L
      _playbackState.update { PlaybackState() }
      return
    }
    val currentId = state.currentTrack?.id
    val newIdx = updatedQueue.indexOfFirst { it.id == currentId }
      .takeIf { it >= 0 }
      ?: state.currentIndex.coerceIn(0, updatedQueue.lastIndex)
    _playbackState.update {
      it.copy(
        queue = updatedQueue,
        currentIndex = newIdx,
        currentTrack = updatedQueue.getOrNull(newIdx),
      )
    }
  }

  fun playQueue(tracks: List<AudioTrack>, startIndex: Int = 0, playlistName: String? = null) {
    if (tracks.isEmpty()) return
    val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
    val mediaItems = tracks.map { it.toMediaItem() }
    val selected = tracks[safeIndex]
    lastAutoCrossfadeTrackId = null
    pendingRestoredStartPositionMs = null

    val controller = mediaController
    if (pendingCrossfade && controller?.isPlaying == true) {
      armCrossfadeOnSession(controller)
    }

    _positionMsFlow.value = 0L
    _playbackState.update {
      it.copy(
        queue = tracks,
        currentIndex = safeIndex,
        currentTrack = selected,
        activePlaylistName = playlistName,
        positionMs = 0L,
        durationMs = selected.durationMs,
        errorMessage = null,
      )
    }
    _trackTransitionEvents.tryEmit(selected)

    if (controller != null) {
      controller.setMediaItems(mediaItems, safeIndex, 0L)
      controller.prepare()
      controller.play()
    } else {
      connect()
    }
  }

  fun playNextInQueue(track: AudioTrack) {
    val controller = mediaController
    val currentQueue = _playbackState.value.queue.toMutableList()
    if (currentQueue.isEmpty()) {
      playQueue(listOf(track), 0)
      return
    }
    val insertPos = (_playbackState.value.currentIndex + 1).coerceIn(0, currentQueue.size)
    currentQueue.add(insertPos, track)
    _playbackState.update { it.copy(queue = currentQueue) }
    controller?.addMediaItem(insertPos, track.toMediaItem())
  }

  fun appendToQueue(track: AudioTrack) {
    val controller = mediaController
    val currentQueue = _playbackState.value.queue.toMutableList()
    if (currentQueue.isEmpty()) {
      playQueue(listOf(track), 0)
      return
    }
    currentQueue.add(track)
    _playbackState.update { it.copy(queue = currentQueue) }
    controller?.addMediaItem(track.toMediaItem())
  }

  fun removeFromQueue(index: Int) {
    val currentQueue = _playbackState.value.queue.toMutableList()
    if (index !in currentQueue.indices || currentQueue.size <= 1) return
    currentQueue.removeAt(index)
    val oldIdx = _playbackState.value.currentIndex
    val newIdx = when {
      index < oldIdx -> oldIdx - 1
      index == oldIdx -> oldIdx.coerceAtMost(currentQueue.lastIndex)
      else -> oldIdx
    }
    _playbackState.update {
      it.copy(
        queue = currentQueue,
        currentIndex = newIdx,
        currentTrack = currentQueue.getOrNull(newIdx)
      )
    }
    mediaController?.removeMediaItem(index)
  }

  fun moveQueueItem(fromIndex: Int, toIndex: Int) {
    val currentQueue = _playbackState.value.queue.toMutableList()
    if (fromIndex !in currentQueue.indices || toIndex !in currentQueue.indices || fromIndex == toIndex) return
    val item = currentQueue.removeAt(fromIndex)
    currentQueue.add(toIndex, item)
    val oldIdx = _playbackState.value.currentIndex
    val newIdx = when (oldIdx) {
      fromIndex -> toIndex
      in (fromIndex + 1)..toIndex -> oldIdx - 1
      in toIndex until fromIndex -> oldIdx + 1
      else -> oldIdx
    }
    _playbackState.update { it.copy(queue = currentQueue, currentIndex = newIdx) }
    mediaController?.moveMediaItem(fromIndex, toIndex)
  }

  fun jumpToQueueIndex(index: Int) {
    val track = _playbackState.value.queue.getOrNull(index) ?: return
    val controller = mediaController
    if (pendingCrossfade && controller?.isPlaying == true) {
      armCrossfadeOnSession(controller)
    }
    _positionMsFlow.value = 0L
    _playbackState.update { it.copy(currentIndex = index, currentTrack = track, positionMs = 0L) }
    controller?.let { c ->
      c.seekToDefaultPosition(index)
      c.play()
    }
  }

  fun togglePlayPause() {
    val controller = mediaController ?: return
    if (controller.isPlaying) {
      controller.pause()
    } else {
      if (controller.mediaItemCount == 0 && _playbackState.value.queue.isNotEmpty()) {
        playQueue(_playbackState.value.queue, _playbackState.value.currentIndex.coerceAtLeast(0))
      } else {
        controller.play()
      }
    }
  }

  fun seekTo(positionMs: Long) {
    val clamped = positionMs.coerceAtLeast(0L)
    _positionMsFlow.value = clamped
    mediaController?.seekTo(clamped)
  }

  fun next() {
    mediaController?.let { c ->
      if (pendingCrossfade && c.isPlaying) {
        armCrossfadeOnSession(c)
      }
      if (c.hasNextMediaItem()) {
        c.seekToNextMediaItem()
        c.play()
      } else if (_playbackState.value.queue.isNotEmpty()) {
        c.seekToDefaultPosition(0)
        c.play()
      }
    }
  }

  fun previous() {
    mediaController?.let { c ->
      if (c.currentPosition > 3500L) {
        c.seekTo(0L)
      } else if (c.hasPreviousMediaItem()) {
        if (pendingCrossfade && c.isPlaying) {
          armCrossfadeOnSession(c)
        }
        c.seekToPreviousMediaItem()
        c.play()
      } else {
        c.seekTo(0L)
      }
    }
  }

  fun toggleShuffle() {
    val next = !_playbackState.value.shuffleEnabled
    _playbackState.update { it.copy(shuffleEnabled = next) }
    mediaController?.shuffleModeEnabled = next
  }

  fun cycleRepeatMode() {
    val nextMode = when (_playbackState.value.repeatMode) {
      RepeatModeOption.OFF -> RepeatModeOption.ALL
      RepeatModeOption.ALL -> RepeatModeOption.ONE
      RepeatModeOption.ONE -> RepeatModeOption.OFF
    }
    _playbackState.update { it.copy(repeatMode = nextMode) }
    mediaController?.repeatMode = when (nextMode) {
      RepeatModeOption.OFF -> Player.REPEAT_MODE_OFF
      RepeatModeOption.ALL -> Player.REPEAT_MODE_ALL
      RepeatModeOption.ONE -> Player.REPEAT_MODE_ONE
    }
  }

  fun setPlaybackSpeed(speed: Float) {
    pendingSpeed = speed.coerceIn(0.5f, 2.0f)
    mediaController?.setPlaybackSpeed(pendingSpeed)
  }

  fun setGaplessEnabled(enabled: Boolean) {
    pendingGapless = enabled
    sendCommand(BotanicalPlaybackService.COMMAND_SET_GAPLESS) {
      putBoolean(BotanicalPlaybackService.EXTRA_ENABLED, enabled)
    }
  }

  fun setCrossfadeEnabled(enabled: Boolean) {
    pendingCrossfade = enabled
    sendCommand(BotanicalPlaybackService.COMMAND_SET_CROSSFADE) {
      putBoolean(BotanicalPlaybackService.EXTRA_ENABLED, enabled)
    }
  }

  fun setEqualizerEnabled(enabled: Boolean) {
    pendingEqEnabled = enabled
    sendCommand(BotanicalPlaybackService.COMMAND_SET_EQ_ENABLED) {
      putBoolean(BotanicalPlaybackService.EXTRA_ENABLED, enabled)
    }
  }

  fun applyEqualizerBands(bandsMb: List<Int>) {
    pendingBands = bandsMb
    sendCommand(BotanicalPlaybackService.COMMAND_SET_EQ_BANDS) {
      putIntArray(BotanicalPlaybackService.EXTRA_BANDS, bandsMb.toIntArray())
    }
  }

  fun applyBassBoost(strength: Int) {
    pendingBass = strength
    sendCommand(BotanicalPlaybackService.COMMAND_SET_BASS_BOOST) {
      putInt(BotanicalPlaybackService.EXTRA_STRENGTH, strength)
    }
  }

  fun applyVirtualizer(strength: Int) {
    pendingVirtualizer = strength
    sendCommand(BotanicalPlaybackService.COMMAND_SET_VIRTUALIZER) {
      putInt(BotanicalPlaybackService.EXTRA_STRENGTH, strength)
    }
  }

  fun setSleepTimer(minutes: Int?) {
    sleepTimerJob?.cancel()
    if (minutes == null || minutes <= 0) {
      _sleepMinutesRemaining.value = null
      return
    }
    _sleepMinutesRemaining.value = minutes
    sleepTimerJob = scope.launch {
      var remaining = minutes
      while (remaining > 0 && isActive) {
        delay(60_000L)
        remaining -= 1
        _sleepMinutesRemaining.value = if (remaining > 0) remaining else null
      }
      if (isActive) {
        mediaController?.pause()
      }
    }
  }

  private fun armCrossfadeOnSession(
    controller: MediaController,
    durationMs: Long = CrossfadeAudioProcessor.DEFAULT_CROSSFADE_MS,
  ) {
    sendCommand(BotanicalPlaybackService.COMMAND_ARM_CROSSFADE, controller) {
      putLong(BotanicalPlaybackService.EXTRA_DURATION_MS, durationMs)
    }
  }

  private inline fun sendCommand(
    action: String,
    targetController: MediaController? = mediaController,
    fillArgs: Bundle.() -> Unit,
  ) {
    val controller = targetController ?: return
    val args = Bundle().apply(fillArgs)
    runCatching {
      controller.sendCustomCommand(SessionCommand(action, Bundle.EMPTY), args)
    }
  }

  private fun applyPendingAudioSettings(controller: MediaController) {
    controller.setPlaybackSpeed(pendingSpeed)
    sendCommand(BotanicalPlaybackService.COMMAND_SET_GAPLESS, controller) {
      putBoolean(BotanicalPlaybackService.EXTRA_ENABLED, pendingGapless)
    }
    sendCommand(BotanicalPlaybackService.COMMAND_SET_CROSSFADE, controller) {
      putBoolean(BotanicalPlaybackService.EXTRA_ENABLED, pendingCrossfade)
    }
    sendCommand(BotanicalPlaybackService.COMMAND_SET_EQ_ENABLED, controller) {
      putBoolean(BotanicalPlaybackService.EXTRA_ENABLED, pendingEqEnabled)
    }
    sendCommand(BotanicalPlaybackService.COMMAND_SET_EQ_BANDS, controller) {
      putIntArray(BotanicalPlaybackService.EXTRA_BANDS, pendingBands.toIntArray())
    }
    sendCommand(BotanicalPlaybackService.COMMAND_SET_BASS_BOOST, controller) {
      putInt(BotanicalPlaybackService.EXTRA_STRENGTH, pendingBass)
    }
    sendCommand(BotanicalPlaybackService.COMMAND_SET_VIRTUALIZER, controller) {
      putInt(BotanicalPlaybackService.EXTRA_STRENGTH, pendingVirtualizer)
    }
  }

  private fun AudioTrack.toMediaItem(): MediaItem {
    val artworkUri = when {
      !coverPath.isNullOrBlank() -> {
        if (coverPath.startsWith("content://") || coverPath.startsWith("file://")) {
          Uri.parse(coverPath)
        } else {
          val f = File(coverPath)
          if (f.exists()) Uri.fromFile(f) else null
        }
      }
      isBundled -> {
        val species = BotanicalCatalog.getById(speciesId)
        Uri.parse("android.resource://${context.packageName}/${species.coverResId}")
      }
      else -> null
    }
    val metadata = MediaMetadata.Builder()
      .setTitle(title)
      .setArtist(artist)
      .setAlbumTitle(album)
      .setGenre(genre)
      .setArtworkUri(artworkUri)
      .build()
    return MediaItem.Builder()
      .setMediaId(id.toString())
      .setUri(Uri.parse(uri))
      .setMediaMetadata(metadata)
      .build()
  }
}
