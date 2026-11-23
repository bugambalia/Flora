package com.example.audio

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.model.AudioTrack
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

  private val _sleepMinutesRemaining = MutableStateFlow<Int?>(null)
  val sleepMinutesRemaining: StateFlow<Int?> = _sleepMinutesRemaining.asStateFlow()

  private val _trackTransitionEvents = MutableSharedFlow<AudioTrack>(extraBufferCapacity = 8)
  val trackTransitionEvents: SharedFlow<AudioTrack> = _trackTransitionEvents.asSharedFlow()

  private var pendingSpeed: Float = 1.0f
  private var pendingGapless: Boolean = true
  private var pendingCrossfade: Boolean = true
  private var pendingEqEnabled: Boolean = true
  private var pendingBands: List<Int> = listOf(120, 60, 0, 140, 220)
  private var pendingBass: Int = 240
  private var pendingVirtualizer: Int = 180
  private var lastAutoCrossfadeTrackId: Long? = null

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
        syncStateFromPlayer(controller)
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
        _playbackState.update { it.copy(isPlaying = isPlaying) }
        if (isPlaying) startProgressLoop() else stopProgressLoop()
      }

      override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val currentIdx = controller.currentMediaItemIndex
        val track = _playbackState.value.queue.getOrNull(currentIdx)
        val wasGapless = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
        _playbackState.update {
          it.copy(
            currentIndex = currentIdx,
            currentTrack = track,
            positionMs = 0L,
            durationMs = controller.duration.takeIf { d -> d > 0 } ?: track?.durationMs ?: 0L,
            gaplessTransitionsCount = if (wasGapless) it.gaplessTransitionsCount + 1 else it.gaplessTransitionsCount
          )
        }
        if (track != null) {
          _trackTransitionEvents.tryEmit(track)
        }
      }

      override fun onPlaybackStateChanged(playbackState: Int) {
        syncStateFromPlayer(controller)
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
    _playbackState.update {
      it.copy(
        currentIndex = idx,
        currentTrack = track ?: it.currentTrack,
        isPlaying = controller.isPlaying,
        positionMs = controller.currentPosition.coerceAtLeast(0L),
        durationMs = controller.duration.takeIf { d -> d > 0 } ?: track?.durationMs ?: it.durationMs,
        bufferedPercentage = controller.bufferedPercentage,
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
            val dur = c.duration.takeIf { d -> d > 0 } ?: _playbackState.value.currentTrack?.durationMs ?: 0L
            _playbackState.update { state ->
              state.copy(
                positionMs = pos,
                durationMs = dur,
                bufferedPercentage = c.bufferedPercentage
              )
            }
            val currentId = _playbackState.value.currentTrack?.id
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
              BotanicalPlaybackService.armCrossfade(CrossfadeAudioProcessor.DEFAULT_CROSSFADE_MS)
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

  fun playQueue(tracks: List<AudioTrack>, startIndex: Int = 0, playlistName: String? = null) {
    if (tracks.isEmpty()) return
    val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
    val mediaItems = tracks.map { it.toMediaItem() }
    val selected = tracks[safeIndex]
    lastAutoCrossfadeTrackId = null

    if (pendingCrossfade && mediaController?.isPlaying == true) {
      BotanicalPlaybackService.armCrossfade(CrossfadeAudioProcessor.DEFAULT_CROSSFADE_MS)
    }

    _playbackState.update {
      it.copy(
        queue = tracks,
        currentIndex = safeIndex,
        currentTrack = selected,
        activePlaylistName = playlistName,
        positionMs = 0L,
        durationMs = selected.durationMs,
      )
    }
    _trackTransitionEvents.tryEmit(selected)

    val controller = mediaController
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
    if (pendingCrossfade && mediaController?.isPlaying == true) {
      BotanicalPlaybackService.armCrossfade(CrossfadeAudioProcessor.DEFAULT_CROSSFADE_MS)
    }
    _playbackState.update { it.copy(currentIndex = index, currentTrack = track, positionMs = 0L) }
    mediaController?.let { c ->
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
    _playbackState.update { it.copy(positionMs = clamped) }
    mediaController?.seekTo(clamped)
  }

  fun next() {
    mediaController?.let { c ->
      if (pendingCrossfade && c.isPlaying) {
        BotanicalPlaybackService.armCrossfade(CrossfadeAudioProcessor.DEFAULT_CROSSFADE_MS)
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
          BotanicalPlaybackService.armCrossfade(CrossfadeAudioProcessor.DEFAULT_CROSSFADE_MS)
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
    BotanicalPlaybackService.setGaplessPlayback(enabled)
  }

  fun setCrossfadeEnabled(enabled: Boolean) {
    pendingCrossfade = enabled
    BotanicalPlaybackService.setCrossfadeEnabled(enabled)
  }

  fun setEqualizerEnabled(enabled: Boolean) {
    pendingEqEnabled = enabled
    BotanicalPlaybackService.activeEffectsBridge?.setEqualizerEnabled(enabled)
  }

  fun applyEqualizerBands(bandsMb: List<Int>) {
    pendingBands = bandsMb
    BotanicalPlaybackService.activeEffectsBridge?.applyBands(bandsMb)
  }

  fun applyBassBoost(strength: Int) {
    pendingBass = strength
    BotanicalPlaybackService.activeEffectsBridge?.applyBassBoost(strength)
  }

  fun applyVirtualizer(strength: Int) {
    pendingVirtualizer = strength
    BotanicalPlaybackService.activeEffectsBridge?.applyVirtualizer(strength)
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

  private fun applyPendingAudioSettings(controller: MediaController) {
    controller.setPlaybackSpeed(pendingSpeed)
    BotanicalPlaybackService.setGaplessPlayback(pendingGapless)
    BotanicalPlaybackService.setCrossfadeEnabled(pendingCrossfade)
    BotanicalPlaybackService.activeEffectsBridge?.setEqualizerEnabled(pendingEqEnabled)
    BotanicalPlaybackService.activeEffectsBridge?.applyBands(pendingBands)
    BotanicalPlaybackService.activeEffectsBridge?.applyBassBoost(pendingBass)
    BotanicalPlaybackService.activeEffectsBridge?.applyVirtualizer(pendingVirtualizer)
  }

  private fun AudioTrack.toMediaItem(): MediaItem {
    val artworkUri = coverPath?.let { path ->
      val f = File(path)
      if (f.exists()) Uri.fromFile(f) else null
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
