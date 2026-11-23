package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.luminance
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AlbumPaletteService
import com.example.audio.AudioFolderScanner
import com.example.audio.AudioFrequencyAnalyzer
import com.example.audio.BotanicalAudioSpectrum
import com.example.audio.CrossfadeAudioProcessor
import com.example.audio.ScannedAudioCandidate
import com.example.data.local.BotanicalAudioDatabase
import com.example.data.local.PlaylistEntity
import com.example.data.local.TrackEntity
import com.example.data.model.AudioTrack
import com.example.data.model.BotanicalCatalog
import com.example.data.model.BotanicalSpeciesId
import com.example.data.model.ThemeBackgroundGradients
import com.example.ui.components.BotanicalStemType
import com.example.ui.components.computeBotanicalStemKinematics
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Flora", appName)
  }

  @Test
  fun `genre maps to matching botanical species`() {
    assertEquals(BotanicalSpeciesId.MONSTERA, BotanicalCatalog.matchSpeciesForGenre("Ambient"))
    assertEquals(BotanicalSpeciesId.GINKGO, BotanicalCatalog.matchSpeciesForGenre("Jazz"))
    assertEquals(BotanicalSpeciesId.LAVENDER, BotanicalCatalog.matchSpeciesForGenre("Chill"))
    assertEquals(BotanicalSpeciesId.CAMELLIA, BotanicalCatalog.matchSpeciesForGenre("Piano"))
  }

  @Test
  fun `playlist creation and track reordering persists in Room`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, BotanicalAudioDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val dao = db.audioDao()

    val trackIds = dao.insertTracks(
      listOf(
        TrackEntity(
          uri = "file:///a.wav", title = "A", artist = "Art", album = "Alb",
          genre = "Folk", durationMs = 10000L, bitrateKbps = 705, sampleRateHz = 44100,
          codec = "PCM WAV", coverPath = null, speciesId = BotanicalSpeciesId.TIGER_LILY.id
        ),
        TrackEntity(
          uri = "file:///b.wav", title = "B", artist = "Art", album = "Alb",
          genre = "Jazz", durationMs = 12000L, bitrateKbps = 705, sampleRateHz = 44100,
          codec = "PCM WAV", coverPath = null, speciesId = BotanicalSpeciesId.GINKGO.id
        )
      )
    )

    val playlistId = dao.insertPlaylist(
      PlaylistEntity(name = "Mi Herbario", description = "Prueba", speciesMotif = BotanicalSpeciesId.FERN.id)
    )
    dao.addTrackToPlaylist(playlistId, trackIds[0])
    dao.addTrackToPlaylist(playlistId, trackIds[1])

    val initial = dao.observePlaylistsWithTracks().first().first().toDomain()
    assertEquals(listOf("A", "B"), initial.tracks.map { it.title })

    dao.replacePlaylistTrackOrder(playlistId, listOf(trackIds[1], trackIds[0]))
    val reordered = dao.observePlaylistsWithTracks().first().first().toDomain()
    assertEquals(listOf("B", "A"), reordered.tracks.map { it.title })
    assertTrue(reordered.totalDurationMs == 22000L)

    db.close()
  }

  @Test
  fun `botanical equalizer presets define 5 valid frequency bands`() {
    com.example.data.model.EqualizerPreset.entries.forEach { preset ->
      assertEquals(5, preset.bandLevels.size)
      assertTrue(preset.bandLevels.all { it in -1000..1000 })
      assertTrue(preset.recommendedBassBoost in 0..1000)
      assertTrue(preset.recommendedVirtualizer in 0..1000)
      assertTrue(preset.label.isNotBlank())
    }
  }

  @Test
  fun `palette service extracts vibrant colors from bitmap and builds theme gradients`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val service = AlbumPaletteService(context)

    // Create vibrant crimson & gold synthetic album cover bitmap
    val bmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(0xFFD92B3A.toInt())
    val accentPaint = Paint().apply { color = 0xFFF2B705.toInt() }
    canvas.drawCircle(60f, 60f, 24f, accentPaint)

    val extracted = service.extractFromBitmap(bmp, cacheKey = "test_crimson")
    bmp.recycle()

    // Dominant / vibrant color should reflect strong red channel over blue
    assertTrue(extracted.vibrant.red > extracted.vibrant.blue)

    val gradients = AlbumPaletteService.buildThemeGradients(extracted, dynamicEnabled = true)
    assertNotEquals(ThemeBackgroundGradients.Default, gradients)
    assertTrue(gradients.playerTop.luminance() <= 0.25f)
    assertTrue(gradients.playerBottom.luminance() <= 0.15f)
  }

  @Test
  fun `palette service updates reactive gradients on track update and toggle`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val service = AlbumPaletteService(context)

    val sampleTrack = AudioTrack(
      id = 99L,
      uri = "file:///nonexistent.wav",
      title = "Azul Andino",
      artist = "Taller",
      album = "Cordillera",
      genre = "Jazz",
      durationMs = 15000L,
      bitrateKbps = 705,
      sampleRateHz = 44100,
      codec = "PCM WAV",
      coverPath = null,
      speciesId = BotanicalSpeciesId.CAMELLIA,
      playCount = 0,
      isFavorite = false,
      isBundled = true
    )

    val palette = service.updateForTrack(sampleTrack, dynamicThemeEnabled = true)
    assertEquals(palette, service.currentPalette.value)
    assertNotEquals(ThemeBackgroundGradients.Default, service.backgroundGradients.value)

    service.setDynamicGradientsEnabled(false)
    assertEquals(ThemeBackgroundGradients.Default, service.backgroundGradients.value)

    service.setDynamicGradientsEnabled(true)
    assertNotEquals(ThemeBackgroundGradients.Default, service.backgroundGradients.value)
  }

  @Test
  fun `audio frequency analyzer separates low bass and high treble frequencies`() {
    val analyzer = AudioFrequencyAnalyzer()
    val sampleRate = 44100
    val sampleCount = 1024

    // Low bass sine wave (110 Hz)
    val bassSamples = ShortArray(sampleCount) { i ->
      (kotlin.math.sin(2.0 * kotlin.math.PI * 110.0 * i / sampleRate) * 24000.0).toInt().toShort()
    }
    val bassSpectrum = analyzer.processPcm16(bassSamples, sampleRateHz = sampleRate, channelCount = 1)
    assertTrue(bassSpectrum.isActive)
    assertEquals(BotanicalAudioSpectrum.BAND_COUNT, bassSpectrum.bands.size)
    assertTrue(bassSpectrum.bass > bassSpectrum.treble)

    // Reset before testing high frequency
    analyzer.onPlaybackPaused()
    assertEquals(BotanicalAudioSpectrum.Idle, analyzer.spectrumState.value)

    // High treble sine wave (5000 Hz)
    val trebleSamples = ShortArray(sampleCount) { i ->
      (kotlin.math.sin(2.0 * kotlin.math.PI * 5000.0 * i / sampleRate) * 24000.0).toInt().toShort()
    }
    val trebleSpectrum = analyzer.processPcm16(trebleSamples, sampleRateHz = sampleRate, channelCount = 1)
    assertTrue(trebleSpectrum.isActive)
    assertTrue(trebleSpectrum.treble > trebleSpectrum.bass)
  }

  @Test
  fun `crossfade audio processor blends outgoing tail and incoming audio with equal power curve`() {
    val processor = CrossfadeAudioProcessor()
    val outgoingChunk = ShortArray(2000) { 10000 }
    val preOutput = processor.processPcm16Array(outgoingChunk)
    assertEquals(10000.toShort(), preOutput[0])

    processor.armCrossfade(durationMs = 10L) // 44100 * 2 * 0.01 = 882 samples
    val incomingChunk = ShortArray(1200) { -10000 }
    val mixed = processor.processPcm16Array(incomingChunk)

    // Start of crossfade prioritizes outgoing tail
    assertEquals(10000.toShort(), mixed[0])
    // Midpoint blends equal-power outgoing (+10000) and incoming (-10000) near zero
    val midVal = kotlin.math.abs(mixed[441].toInt())
    assertTrue("Expected midpoint near 0, got $midVal", midVal < 150)
    // After 882-sample window completes, incoming audio plays clean
    assertEquals((-10000).toShort(), mixed[1000])
  }

  @Test
  fun `crossfade audio processor bypasses mixing when disabled`() {
    val processor = CrossfadeAudioProcessor()
    processor.processPcm16Array(ShortArray(2000) { 10000 })
    processor.isEnabled = false
    processor.armCrossfade(durationMs = 10L)

    val incoming = ShortArray(500) { -8000 }
    val output = processor.processPcm16Array(incoming)
    assertEquals((-8000).toShort(), output[0])
    assertEquals((-8000).toShort(), output[250])
  }

  @Test
  fun `botanical stem equalizer maps 5 bands to distinct plant species whose height and sway scale with gain`() {
    val mapped = (0..4).map { BotanicalStemType.forBandIndex(it) }
    assertEquals(5, mapped.distinct().size)

    val lowGain = computeBotanicalStemKinematics(
      normalizedGain = 0.1f,
      stemType = BotanicalStemType.LILY_STALK,
      swayClockRad = 1.2f,
      maxSwayPx = 40f,
      enabled = true,
      isPlaying = true,
    )
    val midGain = computeBotanicalStemKinematics(
      normalizedGain = 0.5f,
      stemType = BotanicalStemType.LILY_STALK,
      swayClockRad = 1.2f,
      maxSwayPx = 40f,
      enabled = true,
      isPlaying = true,
    )
    val highGain = computeBotanicalStemKinematics(
      normalizedGain = 0.95f,
      stemType = BotanicalStemType.LILY_STALK,
      swayClockRad = 1.2f,
      maxSwayPx = 40f,
      enabled = true,
      isPlaying = true,
    )

    assertTrue(lowGain.heightFraction < midGain.heightFraction)
    assertTrue(midGain.heightFraction < highGain.heightFraction)
    assertTrue(lowGain.swayAmplitudePx < midGain.swayAmplitudePx)
    assertTrue(midGain.swayAmplitudePx < highGain.swayAmplitudePx)
    assertTrue(lowGain.bloomOpenness < highGain.bloomOpenness)

    val disabled = computeBotanicalStemKinematics(
      normalizedGain = 0.95f,
      stemType = BotanicalStemType.LILY_STALK,
      swayClockRad = 1.2f,
      maxSwayPx = 40f,
      enabled = false,
      isPlaying = true,
    )
    assertTrue(disabled.swayAmplitudePx < highGain.swayAmplitudePx)
  }

  @Test
  fun `audio folder scanner picks folder with most tracks exceeding 90 seconds`() {
    val candidates = listOf(
      // Folder with many short clips (<= 1m30s) -> must be excluded
      ScannedAudioCandidate("file:///r/1.mp3", "r1", "Ringtones", "/storage/Ringtones", 25_000L),
      ScannedAudioCandidate("file:///r/2.mp3", "r2", "Ringtones", "/storage/Ringtones", 45_000L),
      ScannedAudioCandidate("file:///r/3.mp3", "r3", "Ringtones", "/storage/Ringtones", 89_999L),
      ScannedAudioCandidate("file:///r/4.mp3", "r4", "Ringtones", "/storage/Ringtones", 90_000L),
      // Secondary folder with 2 long tracks
      ScannedAudioCandidate("file:///p/1.mp3", "p1", "Podcasts", "/storage/Podcasts", 180_000L),
      ScannedAudioCandidate("file:///p/2.mp3", "p2", "Podcasts", "/storage/Podcasts", 210_000L),
      // Dominant folder with 4 tracks > 1m30s + 1 exact 90s intro
      ScannedAudioCandidate("file:///m/intro.flac", "intro", "Bosque", "/storage/Music/Bosque", 90_000L),
      ScannedAudioCandidate("file:///m/a.flac", "a", "Bosque", "/storage/Music/Bosque", 91_000L),
      ScannedAudioCandidate("file:///m/b.flac", "b", "Bosque", "/storage/Music/Bosque", 145_000L),
      ScannedAudioCandidate("file:///m/c.flac", "c", "Bosque", "/storage/Music/Bosque", 198_000L),
      ScannedAudioCandidate("file:///m/d.flac", "d", "Bosque", "/storage/Music/Bosque", 240_000L),
    )

    val result = AudioFolderScanner.selectDominantAudioFolder(candidates)
    assertEquals("Bosque", result.winningFolder?.folderName)
    assertEquals("/storage/Music/Bosque", result.winningFolder?.folderKey)
    assertEquals(4, result.winningFolder?.qualifyingCount)
    assertEquals(5, result.winningFolder?.totalAudioScanned)
    assertEquals(2, result.allFolders.size)
    assertEquals(listOf("Bosque", "Podcasts"), result.allFolders.map { it.folderName })
    assertEquals(5, result.skippedShortCount)
  }

  @Test
  fun `dao batch uri lookup and source folder fields persist correctly`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, BotanicalAudioDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val dao = db.audioDao()

    dao.insertTracks(
      listOf(
        TrackEntity(
          uri = "content://tree/bosque/1",
          title = "Sendero",
          artist = "Flora",
          album = "Bosque",
          genre = "Folk",
          durationMs = 150_000L,
          bitrateKbps = 320,
          sampleRateHz = 44100,
          codec = "FLAC",
          coverPath = null,
          speciesId = BotanicalSpeciesId.TIGER_LILY.id,
          isBundled = false,
          sourceFolder = "Bosque",
          sourcePath = "/storage/Music/Bosque",
        )
      )
    )

    val uris = dao.getAllTrackUris()
    assertEquals(listOf("content://tree/bosque/1"), uris)
    val track = dao.observeAllTracks().first().first().toDomain()
    assertEquals("Bosque", track.sourceFolder)
    assertEquals("/storage/Music/Bosque", track.sourcePath)
    db.close()
  }

  @Test
  fun `repository categorizes only tracks longer than 90 seconds into Botanical Library idempotently`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, BotanicalAudioDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val dao = db.audioDao()
    val repository = com.example.data.repository.MusicRepository(context, dao)

    dao.insertTracks(
      listOf(
        TrackEntity(
          uri = "file:///short_exact_90.wav",
          title = "Exacto 90s",
          artist = "Flora",
          album = "Muestras",
          genre = "Ambient",
          durationMs = 90_000L,
          bitrateKbps = 320,
          sampleRateHz = 44100,
          codec = "WAV",
          coverPath = null,
          speciesId = BotanicalSpeciesId.FERN.id,
          isBundled = false,
          sourceFolder = "Muestras",
          sourcePath = "/storage/Music/Muestras",
        ),
        TrackEntity(
          uri = "file:///long_91.flac",
          title = "Sendero Largo",
          artist = "Flora",
          album = "Bosque",
          genre = "Folk",
          durationMs = 91_000L,
          bitrateKbps = 900,
          sampleRateHz = 44100,
          codec = "FLAC",
          coverPath = null,
          speciesId = BotanicalSpeciesId.MONSTERA.id,
          isBundled = false,
          sourceFolder = "Bosque",
          sourcePath = "/storage/Music/Bosque",
        ),
        TrackEntity(
          uri = "file:///long_240.mp3",
          title = "Lluvia Nocturna",
          artist = "Flora",
          album = "Bosque",
          genre = "Jazz",
          durationMs = 240_000L,
          bitrateKbps = 320,
          sampleRateHz = 44100,
          codec = "MP3",
          coverPath = null,
          speciesId = BotanicalSpeciesId.GINKGO.id,
          isBundled = false,
          sourceFolder = "Bosque",
          sourcePath = "/storage/Music/Bosque",
        ),
      )
    )

    val (collectionId1, added1, total1) = repository.categorizeLongTracksIntoBotanicalLibrary()
    assertTrue(collectionId1 != null && collectionId1 > 0L)
    assertEquals(2, added1)
    assertEquals(2, total1)

    // Re-running categorization must be idempotent
    val (collectionId2, added2, total2) = repository.categorizeLongTracksIntoBotanicalLibrary()
    assertEquals(collectionId1, collectionId2)
    assertEquals(0, added2)
    assertEquals(2, total2)

    val playlists = dao.observePlaylistsWithTracks().first().map { it.toDomain() }
    val botanicalLibraries = playlists.filter {
      it.playlist.name == com.example.data.repository.MusicRepository.BOTANICAL_LIBRARY_COLLECTION_NAME
    }
    assertEquals(1, botanicalLibraries.size)
    assertEquals(
      setOf("Sendero Largo", "Lluvia Nocturna"),
      botanicalLibraries.first().tracks.map { it.title }.toSet()
    )

    db.close()
  }

  @Test
  fun `botanical library scan service initializes and handles scan intent`() {
    val controller = org.robolectric.Robolectric.buildService(
      com.example.audio.BotanicalLibraryScanService::class.java
    )
    val service = controller.create().get()
    val intent = android.content.Intent(
      ApplicationProvider.getApplicationContext(),
      com.example.audio.BotanicalLibraryScanService::class.java
    ).apply {
      action = com.example.audio.BotanicalLibraryScanService.ACTION_SCAN_STORAGE
    }
    val startResult = service.onStartCommand(intent, 0, 1)
    assertEquals(android.app.Service.START_NOT_STICKY, startResult)
    controller.destroy()
  }

  @get:org.junit.Rule
  val composeTestRule = androidx.compose.ui.test.junit4.createComposeRule()

  @Test
  fun `library screen displays vertical alphabet index rail and sorted section headers`() {
    val tracks = listOf(
      AudioTrack(
        id = 1L, uri = "file:///z.wav", title = "Zamba del Bosque", artist = "Flora",
        album = "Andes", genre = "Folk", durationMs = 120_000L, bitrateKbps = 320,
        sampleRateHz = 44100, codec = "WAV", coverPath = null,
        speciesId = BotanicalSpeciesId.FERN, playCount = 0, isFavorite = false, isBundled = true
      ),
      AudioTrack(
        id = 2L, uri = "file:///a.wav", title = "Alba Tropical", artist = "Flora",
        album = "Selva", genre = "Ambient", durationMs = 150_000L, bitrateKbps = 320,
        sampleRateHz = 44100, codec = "WAV", coverPath = null,
        speciesId = BotanicalSpeciesId.MONSTERA, playCount = 0, isFavorite = false, isBundled = true
      ),
    )

    composeTestRule.setContent {
      com.example.ui.theme.HerbariumTheme {
        com.example.ui.screens.LibraryScreen(
          uiState = com.example.ui.viewmodel.HerbariumUiState(
            isLoading = false,
            tracks = tracks,
            filteredTracks = tracks,
          ),
          onSearchChange = {},
          onSelectSourcePath = {},
          onPlayTrack = { _, _ -> },
          onPickRootFolderClick = {},
          onImportDetectedFolder = {},
          onToggleFavorite = {},
          onPlayNext = {},
          onAddToQueue = {},
          onAddToPlaylistClick = {},
          onInspectSpecies = {},
          onOpenHerbarium = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("import_audio_button")).assertDoesNotExist()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("pick_root_folder_button")).assertDoesNotExist()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("vertical_alphabet_index")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("alpha_all")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("alpha_A")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("alpha_Z")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("section_header_A")).assertExists()
    composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("section_header_Z")).assertExists()
  }

  @Test
  fun `audio frequency analyzer skips handleBuffer FFT when visualizer is disabled`() {
    val analyzer = AudioFrequencyAnalyzer()
    analyzer.audioBufferSink.flush(44100, 1, androidx.media3.common.C.ENCODING_PCM_16BIT)

    val sampleCount = 1024
    val byteBuffer = java.nio.ByteBuffer.allocate(sampleCount * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
    for (i in 0 until sampleCount) {
      val sample = (kotlin.math.sin(2.0 * kotlin.math.PI * 220.0 * i / 44100.0) * 24000.0).toInt().toShort()
      byteBuffer.putShort(sample)
    }
    byteBuffer.flip()

    // Disabled by default -> handleBuffer must stay Idle without computing FFT
    analyzer.audioBufferSink.handleBuffer(byteBuffer.duplicate())
    assertEquals(BotanicalAudioSpectrum.Idle, analyzer.spectrumState.value)

    // Enabled -> handleBuffer computes active spectrum
    analyzer.setVisualizationEnabled(true)
    analyzer.audioBufferSink.handleBuffer(byteBuffer.duplicate())
    assertTrue(analyzer.spectrumState.value.isActive)

    // Turning off resets spectrum back to Idle
    analyzer.setVisualizationEnabled(false)
    assertEquals(BotanicalAudioSpectrum.Idle, analyzer.spectrumState.value)
  }

  @Test
  fun `buildLibraryCatalogData sorts tracks alphabetically with symbol section at end`() {
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

    val catalog = com.example.data.model.buildLibraryCatalogData(tracks)
    assertEquals(listOf("Alba", "Orquídea", "99 Lotos"), catalog.sortedTracks.map { it.title })
    assertEquals(setOf('A', 'O', '#'), catalog.availableLetters)
    assertEquals(0, catalog.letterToItemIndex['A'])
    assertEquals(2, catalog.letterToItemIndex['O'])
    assertEquals(4, catalog.letterToItemIndex['#'])
    assertEquals(1, catalog.sectionTrackCounts['A'])
    assertEquals(1, catalog.sectionTrackCounts['O'])
    assertEquals(1, catalog.sectionTrackCounts['#'])
  }

  @Test
  fun `calculateInSampleSize downsamples oversized album art to power of two`() {
    assertEquals(1, com.example.ui.components.calculateInSampleSize(128, 128, 128, 128))
    assertEquals(8, com.example.ui.components.calculateInSampleSize(1200, 1200, 128, 128))
    assertEquals(2, com.example.ui.components.calculateInSampleSize(600, 600, 192, 192))
  }
}
