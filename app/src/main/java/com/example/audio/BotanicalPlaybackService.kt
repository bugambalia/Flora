package com.example.audio

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.HerbariumApp
import com.example.MainActivity

class BotanicalPlaybackService : MediaSessionService() {
  private var mediaSession: MediaSession? = null
  val effectsController = AudioEffectsController()
  private var analyzerRef: AudioFrequencyAnalyzer? = null

  @OptIn(UnstableApi::class)
  override fun onCreate() {
    super.onCreate()

    val analyzer = (application as? HerbariumApp)?.frequencyAnalyzer ?: AudioFrequencyAnalyzer()
    analyzerRef = analyzer
    val crossfadeProcessor = (application as? HerbariumApp)?.crossfadeAudioProcessor
      ?: CrossfadeAudioProcessor()
    activeCrossfadeBridge = crossfadeProcessor
    val teeAudioProcessor = TeeAudioProcessor(analyzer.audioBufferSink)

    val renderersFactory = object : DefaultRenderersFactory(this) {
      override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
      ): AudioSink {
        return DefaultAudioSink.Builder(context)
          .setEnableFloatOutput(enableFloatOutput)
          .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
          .setAudioProcessors(arrayOf(crossfadeProcessor, teeAudioProcessor))
          .build()
      }
    }

    // Lightweight audio-optimized LoadControl to minimize RAM and CPU usage
    val loadControl = DefaultLoadControl.Builder()
      .setBufferDurationsMs(
        /* minBufferMs = */ 5_000,
        /* maxBufferMs = */ 20_000,
        /* bufferForPlaybackMs = */ 1_000,
        /* bufferForPlaybackAfterRebufferMs = */ 2_000
      )
      .setTargetBufferBytes(4 * 1024 * 1024)
      .setPrioritizeTimeOverSizeThresholds(true)
      .build()

    val audioAttributes = AudioAttributes.Builder()
      .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
      .setUsage(C.USAGE_MEDIA)
      .build()

    val player = ExoPlayer.Builder(this, renderersFactory)
      .setLoadControl(loadControl)
      .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
      .setHandleAudioBecomingNoisy(true)
      .setWakeMode(C.WAKE_MODE_LOCAL)
      .build()
      .apply {
        pauseAtEndOfMediaItems = false // True gapless playback across consecutive queue items
      }

    player.addListener(object : Player.Listener {
      override fun onAudioSessionIdChanged(audioSessionId: Int) {
        effectsController.attachToAudioSession(audioSessionId)
      }

      override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (!isPlaying) {
          analyzer.onPlaybackPaused()
        }
      }
    })
    if (player.audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
      effectsController.attachToAudioSession(player.audioSessionId)
    }

    val openAppIntent = PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    mediaSession = MediaSession.Builder(this, player)
      .setSessionActivity(openAppIntent)
      .build()

    activeEffectsBridge = effectsController
    activePlayerBridge = player
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
    mediaSession

  override fun onTaskRemoved(rootIntent: Intent?) {
    val player = mediaSession?.player
    if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
      stopSelf()
    }
  }

  override fun onDestroy() {
    analyzerRef?.onPlaybackPaused()
    analyzerRef = null
    activeCrossfadeBridge = null
    activeEffectsBridge = null
    activePlayerBridge = null
    effectsController.release()
    mediaSession?.run {
      player.release()
      release()
      mediaSession = null
    }
    super.onDestroy()
  }

  companion object {
    @Volatile
    var activeEffectsBridge: AudioEffectsController? = null

    @Volatile
    var activePlayerBridge: ExoPlayer? = null

    @Volatile
    var activeCrossfadeBridge: CrossfadeAudioProcessor? = null

    fun setGaplessPlayback(enabled: Boolean) {
      activePlayerBridge?.pauseAtEndOfMediaItems = !enabled
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
      activeCrossfadeBridge?.isEnabled = enabled
    }

    fun armCrossfade(durationMs: Long = CrossfadeAudioProcessor.DEFAULT_CROSSFADE_MS) {
      activeCrossfadeBridge?.armCrossfade(durationMs)
    }
  }
}
