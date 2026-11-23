package com.example

import androidx.media3.common.C
import com.example.audio.AudioFrequencyAnalyzer
import com.example.audio.BotanicalAudioSpectrum
import com.example.data.model.AudioTrack
import com.example.data.model.BotanicalSpeciesId
import com.example.data.model.buildLibraryCatalogData
import com.example.ui.components.calculateInSampleSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun fftGatingSkipsWhenVisualizerDisabled() {
    val analyzer = AudioFrequencyAnalyzer()
    analyzer.audioBufferSink.flush(44100, 1, C.ENCODING_PCM_16BIT)

    val sampleCount = 1024
    val byteBuffer = ByteBuffer.allocate(sampleCount * 2).order(ByteOrder.LITTLE_ENDIAN)
    for (i in 0 until sampleCount) {
      val sample = (sin(2.0 * PI * 220.0 * i / 44100.0) * 24000.0).toInt().toShort()
      byteBuffer.putShort(sample)
    }
    byteBuffer.flip()

    analyzer.audioBufferSink.handleBuffer(byteBuffer.duplicate())
    assertEquals(BotanicalAudioSpectrum.Idle, analyzer.spectrumState.value)

    analyzer.setVisualizationEnabled(true)
    analyzer.audioBufferSink.handleBuffer(byteBuffer.duplicate())
    assertTrue(analyzer.spectrumState.value.isActive)

    analyzer.setVisualizationEnabled(false)
    assertEquals(BotanicalAudioSpectrum.Idle, analyzer.spectrumState.value)
  }

  @Test
  fun buildLibraryCatalogDataSortsAndIndexesAlphabetically() {
    val tracks = listOf(
      AudioTrack(
        id = 1L, uri = "file:///1.wav", title = "99 Lotos", artist = "A",
        album = "X", genre = "Folk", durationMs = 120_000L, bitrateKbps = 320,
        sampleRateHz = 44100, codec = "WAV", coverPath = null,
        speciesId = BotanicalSpeciesId.LOTUS, playCount = 0, isFavorite = false, isBundled = true
      ),
      AudioTrack(
        id = 2L, uri = "file:///2.wav", title = "Orquídea", artist = "B",
        album = "X", genre = "Jazz", durationMs = 120_000L, bitrateKbps = 320,
        sampleRateHz = 44100, codec = "WAV", coverPath = null,
        speciesId = BotanicalSpeciesId.LAVENDER, playCount = 0, isFavorite = false, isBundled = true
      ),
      AudioTrack(
        id = 3L, uri = "file:///3.wav", title = "Alba", artist = "C",
        album = "X", genre = "Ambient", durationMs = 120_000L, bitrateKbps = 320,
        sampleRateHz = 44100, codec = "WAV", coverPath = null,
        speciesId = BotanicalSpeciesId.MONSTERA, playCount = 0, isFavorite = false, isBundled = true
      ),
    )

    val catalog = buildLibraryCatalogData(tracks)
    assertEquals(listOf("Alba", "Orquídea", "99 Lotos"), catalog.sortedTracks.map { it.title })
    assertEquals(setOf('A', 'O', '#'), catalog.availableLetters)
    assertEquals(0, catalog.letterToItemIndex['A'])
    assertEquals(2, catalog.letterToItemIndex['O'])
    assertEquals(4, catalog.letterToItemIndex['#'])
  }

  @Test
  fun calculateInSampleSizeComputesPowerOfTwo() {
    assertEquals(1, calculateInSampleSize(128, 128, 128, 128))
    assertEquals(8, calculateInSampleSize(1200, 1200, 128, 128))
    assertEquals(2, calculateInSampleSize(600, 600, 192, 192))
  }
}
