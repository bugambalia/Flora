package com.example

import android.app.Application
import com.example.audio.AlbumPaletteService
import com.example.audio.AudioFrequencyAnalyzer
import com.example.audio.CrossfadeAudioProcessor
import com.example.audio.PlaybackController
import com.example.data.local.AudioPreferencesRepository
import com.example.data.local.BotanicalAudioDatabase
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class HerbariumApp : Application() {
  val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  val database: BotanicalAudioDatabase by lazy { BotanicalAudioDatabase.getInstance(this) }
  val albumPaletteService: AlbumPaletteService by lazy { AlbumPaletteService(this) }
  val frequencyAnalyzer: AudioFrequencyAnalyzer by lazy { AudioFrequencyAnalyzer() }
  val crossfadeAudioProcessor: CrossfadeAudioProcessor by lazy { CrossfadeAudioProcessor() }
  val musicRepository: MusicRepository by lazy {
    MusicRepository(this, database.audioDao(), albumPaletteService)
  }
  val preferencesRepository: AudioPreferencesRepository by lazy { AudioPreferencesRepository(this) }
  val playbackController: PlaybackController by lazy { PlaybackController(this) }
}
