package com.example.audio

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.HerbariumApp
import com.example.MainActivity
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class BotanicalPlaybackService : MediaSessionService() {
  private var mediaSession: MediaSession? = null
  private var playerRef: ExoPlayer? = null
  private var crossfadeProcessorRef: CrossfadeAudioProcessor? = null
  private val effectsController = AudioEffectsController()
  private var analyzerRef: AudioFrequencyAnalyzer? = null

  @OptIn(UnstableApi::class)
  override fun onCreate() {
    super.onCreate()

    val analyzer = (application as? HerbariumApp)?.frequencyAnalyzer ?: AudioFrequencyAnalyzer()
    analyzerRef = analyzer
    val crossfadeProcessor = (application as? HerbariumApp)?.crossfadeAudioProcessor
      ?: CrossfadeAudioProcessor()
    crossfadeProcessorRef = crossfadeProcessor
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
    playerRef = player

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

    val sessionCallback = object : MediaSession.Callback {
      override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
      ): MediaSession.ConnectionResult {
        val base = super.onConnect(session, controller)
        if (controller.packageName != packageName) {
          return base
        }
        val commands = base.availableSessionCommands.buildUpon().apply {
          CUSTOM_COMMANDS.forEach { action ->
            add(SessionCommand(action, Bundle.EMPTY))
          }
        }.build()
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
          .setAvailableSessionCommands(commands)
          .setAvailablePlayerCommands(base.availablePlayerCommands)
          .build()
      }

      override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
      ): ListenableFuture<SessionResult> {
        if (controller.packageName != packageName) {
          return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_PERMISSION_DENIED))
        }
        when (customCommand.customAction) {
          COMMAND_SET_GAPLESS -> {
            val enabled = args.getBoolean(EXTRA_ENABLED, true)
            playerRef?.pauseAtEndOfMediaItems = !enabled
          }
          COMMAND_SET_CROSSFADE -> {
            crossfadeProcessorRef?.isEnabled = args.getBoolean(EXTRA_ENABLED, true)
          }
          COMMAND_ARM_CROSSFADE -> {
            val durationMs = args.getLong(EXTRA_DURATION_MS, CrossfadeAudioProcessor.DEFAULT_CROSSFADE_MS)
            crossfadeProcessorRef?.armCrossfade(durationMs)
          }
          COMMAND_SET_EQ_ENABLED -> {
            effectsController.setEqualizerEnabled(args.getBoolean(EXTRA_ENABLED, true))
          }
          COMMAND_SET_EQ_BANDS -> {
            args.getIntArray(EXTRA_BANDS)?.let { bands ->
              effectsController.applyBands(bands.toList())
            }
          }
          COMMAND_SET_BASS_BOOST -> {
            effectsController.applyBassBoost(args.getInt(EXTRA_STRENGTH, 0))
          }
          COMMAND_SET_VIRTUALIZER -> {
            effectsController.applyVirtualizer(args.getInt(EXTRA_STRENGTH, 0))
          }
          else -> {
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
          }
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
      }
    }

    mediaSession = MediaSession.Builder(this, player)
      .setSessionActivity(openAppIntent)
      .setCallback(sessionCallback)
      .build()
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
    crossfadeProcessorRef = null
    playerRef = null
    effectsController.release()
    mediaSession?.run {
      player.release()
      release()
      mediaSession = null
    }
    super.onDestroy()
  }

  companion object {
    const val COMMAND_SET_GAPLESS = "com.example.audio.SET_GAPLESS"
    const val COMMAND_SET_CROSSFADE = "com.example.audio.SET_CROSSFADE"
    const val COMMAND_ARM_CROSSFADE = "com.example.audio.ARM_CROSSFADE"
    const val COMMAND_SET_EQ_ENABLED = "com.example.audio.SET_EQ_ENABLED"
    const val COMMAND_SET_EQ_BANDS = "com.example.audio.SET_EQ_BANDS"
    const val COMMAND_SET_BASS_BOOST = "com.example.audio.SET_BASS_BOOST"
    const val COMMAND_SET_VIRTUALIZER = "com.example.audio.SET_VIRTUALIZER"

    const val EXTRA_ENABLED = "extra_enabled"
    const val EXTRA_DURATION_MS = "extra_duration_ms"
    const val EXTRA_BANDS = "extra_bands"
    const val EXTRA_STRENGTH = "extra_strength"

    private val CUSTOM_COMMANDS = listOf(
      COMMAND_SET_GAPLESS,
      COMMAND_SET_CROSSFADE,
      COMMAND_ARM_CROSSFADE,
      COMMAND_SET_EQ_ENABLED,
      COMMAND_SET_EQ_BANDS,
      COMMAND_SET_BASS_BOOST,
      COMMAND_SET_VIRTUALIZER,
    )
  }
}
