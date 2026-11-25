package com.example

import androidx.media3.common.C
import com.example.audio.AudioFrequencyAnalyzer
import com.example.audio.BotanicalAudioSpectrum
import com.example.audio.LocalAudioImporter
import com.example.audio.LyricsParser
import com.example.data.model.AudioTrack
import com.example.data.model.BotanicalSpeciesId
import com.example.data.model.LibrarySortOrder
import com.example.data.model.Playlist
import com.example.data.model.PlaylistWithTracks
import com.example.data.model.buildLibraryCatalogData
import com.example.data.model.formatListenDuration
import com.example.data.repository.PlaylistSerializer
import com.example.ui.components.calculateInSampleSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
  fun buildLibraryCatalogDataSupportsAllSortOrders() {
    val tracks = listOf(
      AudioTrack(
        id = 1L, uri = "file:///1.wav", title = "Brisa", artist = "Zeta",
        album = "Montaña", genre = "Folk", durationMs = 90_000L, bitrateKbps = 320,
        sampleRateHz = 44100, codec = "WAV", coverPath = null,
        speciesId = BotanicalSpeciesId.LOTUS, playCount = 1, isFavorite = false, isBundled = true, dateAdded = 100L
      ),
      AudioTrack(
        id = 2L, uri = "file:///2.wav", title = "Alba", artist = "Alfa",
        album = "Bosque", genre = "Jazz", durationMs = 240_000L, bitrateKbps = 320,
        sampleRateHz = 44100, codec = "WAV", coverPath = null,
        speciesId = BotanicalSpeciesId.LAVENDER, playCount = 8, isFavorite = false, isBundled = true, dateAdded = 300L
      ),
      AudioTrack(
        id = 3L, uri = "file:///3.wav", title = "Céfiro", artist = "Beta",
        album = "Río", genre = "Ambient", durationMs = 180_000L, bitrateKbps = 320,
        sampleRateHz = 44100, codec = "WAV", coverPath = null,
        speciesId = BotanicalSpeciesId.MONSTERA, playCount = 3, isFavorite = false, isBundled = true, dateAdded = 200L
      ),
    )

    val byDuration = buildLibraryCatalogData(tracks, LibrarySortOrder.DURATION)
    assertFalse(byDuration.usesAlphabetSections)
    assertEquals(listOf("Alba", "Céfiro", "Brisa"), byDuration.sortedTracks.map { it.title })

    val byMostPlayed = buildLibraryCatalogData(tracks, LibrarySortOrder.MOST_PLAYED)
    assertEquals(listOf(8, 3, 1), byMostPlayed.sortedTracks.map { it.playCount })

    val byArtist = buildLibraryCatalogData(tracks, LibrarySortOrder.ARTIST)
    assertTrue(byArtist.usesAlphabetSections)
    assertEquals(listOf("Alfa", "Beta", "Zeta"), byArtist.sortedTracks.map { it.artist })
  }

  @Test
  fun lyricsParserParsesLrcTimestampsAndFindsActiveLine() {
    val lrc = """
      [ti:Bosque de Niebla]
      [00:05.00] Primer verso del alba
      [00:12.50] Segundo verso bajo la lluvia
      [00:25.10] Tercer verso en el herbario
    """.trimIndent()

    val parsed = LyricsParser.parseLyricsText(lrc, sourceLabel = "Archivo .lrc")
    assertTrue(parsed.isSynced)
    assertEquals(3, parsed.lines.size)
    assertEquals(5_000L, parsed.lines[0].timestampMs)
    assertEquals(12_500L, parsed.lines[1].timestampMs)
    assertEquals(25_100L, parsed.lines[2].timestampMs)

    assertEquals(0, LyricsParser.findActiveLineIndex(parsed, 1_000L, 60_000L))
    assertEquals(1, LyricsParser.findActiveLineIndex(parsed, 14_000L, 60_000L))
    assertEquals(2, LyricsParser.findActiveLineIndex(parsed, 30_000L, 60_000L))
  }

  @Test
  fun playlistSerializerExportsAndMatchesM3uAndJson() {
    val tracks = listOf(
      AudioTrack(
        id = 10L, uri = "file:///music/alba.flac", title = "Alba", artist = "Trio",
        album = "Selva", genre = "Folk", durationMs = 185_000L, bitrateKbps = 900,
        sampleRateHz = 44100, codec = "FLAC", coverPath = null,
        speciesId = BotanicalSpeciesId.FERN, playCount = 0, isFavorite = false, isBundled = false
      ),
      AudioTrack(
        id = 20L, uri = "file:///music/loto.mp3", title = "Loto Dorado", artist = "Ribera",
        album = "Ciénaga", genre = "Bossa", durationMs = 210_000L, bitrateKbps = 320,
        sampleRateHz = 44100, codec = "MP3", coverPath = null,
        speciesId = BotanicalSpeciesId.LOTUS, playCount = 0, isFavorite = false, isBundled = false
      )
    )
    val playlist = PlaylistWithTracks(
      playlist = Playlist(
        id = 1L, name = "Sendero Botánico", description = "Colección",
        speciesMotif = BotanicalSpeciesId.LOTUS, createdAt = 1L, updatedAt = 1L
      ),
      tracks = tracks
    )

    val m3u = PlaylistSerializer.exportToM3u(playlist)
    val parsedM3u = PlaylistSerializer.parseImport(m3u)
    assertEquals("Sendero Botánico", parsedM3u.name)
    assertEquals(2, PlaylistSerializer.matchTracksInLibrary(parsedM3u.trackRefs, tracks).size)

    val json = PlaylistSerializer.exportToJson(playlist)
    val parsedJson = PlaylistSerializer.parseImport(json)
    assertEquals("Sendero Botánico", parsedJson.name)
    assertEquals(BotanicalSpeciesId.LOTUS, parsedJson.motif)
    assertEquals(listOf(10L, 20L), PlaylistSerializer.matchTracksInLibrary(parsedJson.trackRefs, tracks).map { it.id })
  }

  @Test
  fun formatListenDurationFormatsSecondsMinutesAndHours() {
    assertEquals("45s", formatListenDuration(45))
    assertEquals("2m 5s", formatListenDuration(125))
    assertEquals("1h 2m", formatListenDuration(3725))
  }

  @Test
  fun calculateInSampleSizeComputesPowerOfTwo() {
    assertEquals(1, calculateInSampleSize(128, 128, 128, 128))
    assertEquals(8, calculateInSampleSize(1200, 1200, 128, 128))
    assertEquals(2, calculateInSampleSize(600, 600, 192, 192))
  }

  @Test
  fun precomputedFftSeparatesBassAndTrebleAccurately() {
    val analyzer = AudioFrequencyAnalyzer()
    val sampleRate = 44100
    val sampleCount = 1024

    val bassSamples = ShortArray(sampleCount) { i ->
      (sin(2.0 * PI * 110.0 * i / sampleRate) * 24000.0).toInt().toShort()
    }
    val bassSpectrum = analyzer.processPcm16(bassSamples, sampleRateHz = sampleRate, channelCount = 1)
    assertTrue(bassSpectrum.isActive)
    assertEquals(BotanicalAudioSpectrum.BAND_COUNT, bassSpectrum.bands.size)
    assertTrue(bassSpectrum.bass > bassSpectrum.treble)

    analyzer.onPlaybackPaused()
    val trebleSamples = ShortArray(sampleCount) { i ->
      (sin(2.0 * PI * 5000.0 * i / sampleRate) * 24000.0).toInt().toShort()
    }
    val trebleSpectrum = analyzer.processPcm16(trebleSamples, sampleRateHz = sampleRate, channelCount = 1)
    assertTrue(trebleSpectrum.isActive)
    assertTrue(trebleSpectrum.treble > trebleSpectrum.bass)
  }

  @Test
  fun coverFileNameUsesCollisionResistantSha256Hash() {
    val uriA = "content://media/external/audio/Aa"
    val uriB = "content://media/external/audio/BB"
    // Verify 32-bit hashCode collides while SHA-256 cover filename does not
    assertEquals(uriA.hashCode(), uriB.hashCode())
    val nameA = LocalAudioImporter.coverFileNameForUri(uriA)
    val nameB = LocalAudioImporter.coverFileNameForUri(uriB)
    assertEquals(nameA, LocalAudioImporter.coverFileNameForUri(uriA))
    assertNotEquals(nameA, nameB)
    assertTrue(nameA.matches(Regex("cover_[0-9a-f]{32}\\.jpg")))
  }
}
