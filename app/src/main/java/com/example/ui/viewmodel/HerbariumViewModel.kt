package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HerbariumApp
import com.example.audio.BotanicalAudioSpectrum
import com.example.audio.BotanicalLibraryScanService
import com.example.audio.DetectedFolderSource
import com.example.data.model.AlbumPaletteColors
import com.example.data.model.AudioEffectsState
import com.example.data.model.AudioSourceSummary
import com.example.data.model.AudioTrack
import com.example.data.model.BotanicalCatalog
import com.example.data.model.BotanicalProgress
import com.example.data.model.BotanicalSpecies
import com.example.data.model.BotanicalSpeciesId
import com.example.data.model.EqualizerPreset
import com.example.data.model.LibraryCatalogData
import com.example.data.model.PlaybackState
import com.example.data.model.PlaylistWithTracks
import com.example.data.model.ThemeBackgroundGradients
import com.example.data.model.buildLibraryCatalogData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class HerbariumUiState(
  val isLoading: Boolean = true,
  val isScanningFolders: Boolean = false,
  val tracks: List<AudioTrack> = emptyList(),
  val filteredTracks: List<AudioTrack> = emptyList(),
  val libraryCatalog: LibraryCatalogData = LibraryCatalogData(),
  val availableGenres: List<String> = emptyList(),
  val searchQuery: String = "",
  val selectedGenre: String? = null,
  val selectedSourcePath: String? = null,
  val audioSources: List<AudioSourceSummary> = emptyList(),
  val detectedFolders: List<DetectedFolderSource> = emptyList(),
  val dominantDetectedFolder: DetectedFolderSource? = null,
  val playlists: List<PlaylistWithTracks> = emptyList(),
  val botanicalProgress: Map<BotanicalSpeciesId, BotanicalProgress> = emptyMap(),
  val playbackState: PlaybackState = PlaybackState(),
  val effectsState: AudioEffectsState = AudioEffectsState(),
  val albumPalette: AlbumPaletteColors = AlbumPaletteColors(),
  val themeGradients: ThemeBackgroundGradients = ThemeBackgroundGradients.Default,
  val unlockedCount: Int = 1,
)

private data class CatalogSnapshot(
  val isLoading: Boolean = true,
  val isScanningFolders: Boolean = false,
  val tracks: List<AudioTrack> = emptyList(),
  val filteredTracks: List<AudioTrack> = emptyList(),
  val libraryCatalog: LibraryCatalogData = LibraryCatalogData(),
  val availableGenres: List<String> = emptyList(),
  val searchQuery: String = "",
  val selectedGenre: String? = null,
  val selectedSourcePath: String? = null,
  val audioSources: List<AudioSourceSummary> = emptyList(),
  val detectedFolders: List<DetectedFolderSource> = emptyList(),
  val dominantDetectedFolder: DetectedFolderSource? = null,
  val playlists: List<PlaylistWithTracks> = emptyList(),
  val botanicalProgress: Map<BotanicalSpeciesId, BotanicalProgress> = emptyMap(),
  val unlockedCount: Int = 1,
)

class HerbariumViewModel(application: Application) : AndroidViewModel(application) {
  private val app = application as HerbariumApp
  private val repository = app.musicRepository
  private val prefsRepo = app.preferencesRepository
  private val playbackController = app.playbackController
  private val paletteService = app.albumPaletteService

  val audioSpectrum: StateFlow<BotanicalAudioSpectrum> = app.frequencyAnalyzer.spectrumState
  val playbackPositionMs: StateFlow<Long> = playbackController.positionMsFlow

  private val searchQuery = MutableStateFlow("")
  val searchQueryState: StateFlow<String> = searchQuery.asStateFlow()

  @OptIn(FlowPreview::class)
  private val debouncedSearchQuery = searchQuery
    .debounce { if (it.isBlank()) 0L else 150L }
    .distinctUntilChanged()

  private val selectedGenre = MutableStateFlow<String?>(null)
  private val selectedSourcePath = MutableStateFlow<String?>(null)
  private val detectedFolders = MutableStateFlow<List<DetectedFolderSource>>(emptyList())
  private val dominantDetectedFolder = MutableStateFlow<DetectedFolderSource?>(null)
  private val isScanningFolders = MutableStateFlow(false)
  private val isInitialized = MutableStateFlow(false)

  private val _unlockedBanner = MutableStateFlow<BotanicalSpecies?>(null)
  val unlockedBanner: StateFlow<BotanicalSpecies?> = _unlockedBanner.asStateFlow()

  private val _uiMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val uiMessages: SharedFlow<String> = _uiMessages.asSharedFlow()

  // Slow-changing catalog snapshot computed off the main thread; isolated from 250ms playback ticks
  private val catalogSnapshotFlow = combine(
    repository.tracksFlow,
    repository.playlistsFlow,
    repository.botanicalProgressFlow,
    debouncedSearchQuery,
    selectedGenre,
    selectedSourcePath,
    detectedFolders,
    dominantDetectedFolder,
    BotanicalLibraryScanService.scanStatus,
    isInitialized,
  ) { values ->
    @Suppress("UNCHECKED_CAST")
    val tracks = values[0] as List<AudioTrack>
    @Suppress("UNCHECKED_CAST")
    val playlists = values[1] as List<PlaylistWithTracks>
    @Suppress("UNCHECKED_CAST")
    val progress = values[2] as Map<BotanicalSpeciesId, BotanicalProgress>
    val query = values[3] as String
    val genre = values[4] as String?
    val sourcePath = values[5] as String?
    @Suppress("UNCHECKED_CAST")
    val detected = values[6] as List<DetectedFolderSource>
    val dominant = values[7] as DetectedFolderSource?
    val scanStatus = values[8] as com.example.audio.BotanicalScanStatus
    val scanning = scanStatus.isScanning || isScanningFolders.value
    val ready = values[9] as Boolean

    val genres = tracks.map { it.genre }.distinct().sorted()
    val sources = tracks
      .groupBy { it.sourcePath }
      .map { (path, groupTracks) ->
        AudioSourceSummary(
          folderName = groupTracks.first().sourceFolder,
          folderPath = path,
          trackCount = groupTracks.size,
          longTrackCount = groupTracks.count { it.durationMs > 90_000L },
          isPersistedFolder = !groupTracks.first().isBundled,
        )
      }
      .sortedWith(
        compareByDescending<AudioSourceSummary> { it.isPersistedFolder }
          .thenByDescending { it.longTrackCount }
          .thenByDescending { it.trackCount }
      )

    val filtered = tracks.filter { track ->
      val matchesQuery = query.isBlank() ||
        track.title.contains(query, ignoreCase = true) ||
        track.artist.contains(query, ignoreCase = true) ||
        track.album.contains(query, ignoreCase = true) ||
        track.genre.contains(query, ignoreCase = true) ||
        track.sourceFolder.contains(query, ignoreCase = true)
      val matchesGenre = genre == null || track.genre.equals(genre, ignoreCase = true)
      val matchesSource = sourcePath == null || track.sourcePath == sourcePath
      matchesQuery && matchesGenre && matchesSource
    }

    val libraryCatalog = buildLibraryCatalogData(filtered)

    CatalogSnapshot(
      isLoading = !ready,
      isScanningFolders = scanning,
      tracks = tracks,
      filteredTracks = filtered,
      libraryCatalog = libraryCatalog,
      availableGenres = genres,
      searchQuery = query,
      selectedGenre = genre,
      selectedSourcePath = sourcePath,
      audioSources = sources,
      detectedFolders = detected,
      dominantDetectedFolder = dominant,
      playlists = playlists,
      botanicalProgress = progress,
      unlockedCount = progress.values.count { it.isUnlocked }.coerceAtLeast(1),
    )
  }
    .distinctUntilChanged()
    .flowOn(Dispatchers.Default)

  val uiState: StateFlow<HerbariumUiState> = combine(
    catalogSnapshotFlow,
    playbackController.playbackState,
    prefsRepo.preferencesFlow,
    playbackController.sleepMinutesRemaining,
    paletteService.currentPalette,
    paletteService.backgroundGradients,
  ) { values ->
    val catalog = values[0] as CatalogSnapshot
    val playback = values[1] as PlaybackState
    val prefs = values[2] as AudioEffectsState
    val sleepMin = values[3] as Int?
    val palette = values[4] as AlbumPaletteColors
    val gradients = values[5] as ThemeBackgroundGradients

    HerbariumUiState(
      isLoading = catalog.isLoading,
      isScanningFolders = catalog.isScanningFolders,
      tracks = catalog.tracks,
      filteredTracks = catalog.filteredTracks,
      libraryCatalog = catalog.libraryCatalog,
      availableGenres = catalog.availableGenres,
      searchQuery = catalog.searchQuery,
      selectedGenre = catalog.selectedGenre,
      selectedSourcePath = catalog.selectedSourcePath,
      audioSources = catalog.audioSources,
      detectedFolders = catalog.detectedFolders,
      dominantDetectedFolder = catalog.dominantDetectedFolder,
      playlists = catalog.playlists,
      botanicalProgress = catalog.botanicalProgress,
      playbackState = playback,
      effectsState = prefs.copy(sleepTimerMinutesRemaining = sleepMin),
      albumPalette = palette,
      themeGradients = gradients,
      unlockedCount = catalog.unlockedCount,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = HerbariumUiState(),
  )

  private var initialPaletteLoaded = false
  private var pendingNeedFolderPicker: (() -> Unit)? = null

  init {
    viewModelScope.launch {
      repository.initializeLibraryIfNeeded()
      isInitialized.value = true
      // Trigger background Service scan for >90s audio files into "Botanical Library"
      BotanicalLibraryScanService.startScan(app)
    }

    viewModelScope.launch {
      BotanicalLibraryScanService.scanCompletedEvents.collectLatest { summary ->
        detectedFolders.value = summary.scanResult.allFolders
        dominantDetectedFolder.value = summary.scanResult.winningFolder

        val winner = summary.scanResult.winningFolder
        if (summary.triggeredManually) {
          if (winner != null && winner.qualifyingCount > 0) {
            selectedSourcePath.value = winner.folderKey
            val msg = if (summary.newlyImportedCount > 0 || summary.newlyCategorizedCount > 0) {
              "Botanical Library: ${summary.totalInBotanicalLibrary} pistas > 90 s (fuente «${winner.folderName}»)."
            } else {
              "Botanical Library actualizada (${summary.totalInBotanicalLibrary} pistas > 90 s)."
            }
            _uiMessages.tryEmit(msg)
          } else {
            val picker = pendingNeedFolderPicker
            pendingNeedFolderPicker = null
            if (picker != null) {
              picker()
            } else {
              val skipped = summary.scanResult.skippedShortCount
              _uiMessages.tryEmit(
                if (skipped > 0) {
                  "Se omitieron $skipped audios de 90 s o menos; sin audios > 90 s."
                } else {
                  "No se detectaron pistas > 90 s en las carpetas analizadas."
                }
              )
            }
          }
        }
      }
    }

    viewModelScope.launch {
      prefsRepo.preferencesFlow.collectLatest { prefs ->
        playbackController.setEqualizerEnabled(prefs.equalizerEnabled)
        playbackController.applyEqualizerBands(prefs.bandLevelsMb)
        playbackController.applyBassBoost(prefs.bassBoostStrength)
        playbackController.applyVirtualizer(prefs.virtualizerStrength)
        playbackController.setPlaybackSpeed(prefs.playbackSpeed)
        playbackController.setGaplessEnabled(prefs.gaplessEnabled)
        playbackController.setCrossfadeEnabled(prefs.crossfadeEnabled)
        paletteService.setDynamicGradientsEnabled(prefs.chameleonThemeEnabled)
      }
    }

    viewModelScope.launch {
      playbackController.trackTransitionEvents.collectLatest { track ->
        paletteService.updateForTrack(track)
        val unlocked = repository.recordTrackPlayed(track)
        if (unlocked != null) {
          _unlockedBanner.value = unlocked
        }
        if (playbackController.playbackState.value.gaplessTransitionsCount > 0) {
          repository.unlockSpeciesIfNeeded(
            BotanicalSpeciesId.GINKGO,
            "Desbloqueado tras una transición sin pausas (gapless)"
          )?.let { _unlockedBanner.value = it }
        }
      }
    }

    // Extract initial cover palette once when library finishes loading
    viewModelScope.launch {
      repository.tracksFlow.collectLatest { tracks ->
        if (!initialPaletteLoaded && tracks.isNotEmpty() && playbackController.playbackState.value.currentTrack == null) {
          initialPaletteLoaded = true
          paletteService.updateForTrack(tracks.first())
        }
      }
    }
  }

  fun onSearchQueryChange(query: String) {
    searchQuery.value = query
  }

  fun setVisualizerActive(active: Boolean) {
    app.frequencyAnalyzer.setVisualizationEnabled(active)
  }

  fun onSelectGenre(genre: String?) {
    selectedGenre.value = if (selectedGenre.value == genre) null else genre
  }

  fun onSelectSourcePath(sourcePath: String?) {
    selectedSourcePath.value = if (selectedSourcePath.value == sourcePath) null else sourcePath
  }

  fun dismissUnlockedBanner() {
    _unlockedBanner.value = null
  }

  /**
   * Launches the background scan service to scan storage for audio files longer than 90s
   * and categorize them into the 'Botanical Library' collection.
   */
  fun scanAndAutoImportFolder(treeUri: Uri? = null, onNeedFolderPicker: (() -> Unit)? = null) {
    if (isScanningFolders.value || BotanicalLibraryScanService.scanStatus.value.isScanning) return
    pendingNeedFolderPicker = if (treeUri == null) onNeedFolderPicker else null
    BotanicalLibraryScanService.startScan(
      context = app,
      treeUri = treeUri,
      triggeredManually = true,
      asForeground = false,
    )
  }

  fun importSpecificDetectedFolder(folder: DetectedFolderSource) {
    viewModelScope.launch {
      isScanningFolders.value = true
      try {
        val added = repository.importDetectedFolder(folder)
        selectedSourcePath.value = folder.folderKey
        val msg = if (added > 0) {
          "Fuente «${folder.folderName}»: $added pistas > 1:30 importadas."
        } else {
          "Fuente «${folder.folderName}» ya estaba sincronizada."
        }
        _uiMessages.tryEmit(msg)
      } finally {
        isScanningFolders.value = false
      }
    }
  }

  fun playTrackFromList(tracks: List<AudioTrack>, startIndex: Int, sourceName: String? = null) {
    playbackController.playQueue(tracks, startIndex, sourceName)
  }

  fun playPlaylist(playlist: PlaylistWithTracks, startIndex: Int = 0) {
    if (playlist.tracks.isEmpty()) {
      _uiMessages.tryEmit("La lista «${playlist.playlist.name}» aún no tiene pistas.")
      return
    }
    playbackController.playQueue(playlist.tracks, startIndex, playlist.playlist.name)
  }

  fun shuffleAndPlay(tracks: List<AudioTrack>, sourceName: String? = null) {
    if (tracks.isEmpty()) return
    val shuffled = tracks.shuffled()
    playbackController.playQueue(shuffled, 0, sourceName)
  }

  fun playNextInQueue(track: AudioTrack) {
    playbackController.playNextInQueue(track)
    _uiMessages.tryEmit("«${track.title}» sonará a continuación")
  }

  fun appendToQueue(track: AudioTrack) {
    playbackController.appendToQueue(track)
    _uiMessages.tryEmit("«${track.title}» añadida a la cola")
  }

  fun moveQueueItem(fromIndex: Int, toIndex: Int) =
    playbackController.moveQueueItem(fromIndex, toIndex)

  fun removeFromQueue(index: Int) =
    playbackController.removeFromQueue(index)

  fun jumpToQueueIndex(index: Int) =
    playbackController.jumpToQueueIndex(index)

  fun togglePlayPause() {
    val current = playbackController.playbackState.value
    if (current.currentTrack == null && uiState.value.tracks.isNotEmpty()) {
      playTrackFromList(uiState.value.tracks, 0, "Canciones")
    } else {
      playbackController.togglePlayPause()
    }
  }

  fun seekTo(ms: Long) = playbackController.seekTo(ms)
  fun next() = playbackController.next()
  fun previous() = playbackController.previous()
  fun toggleShuffle() = playbackController.toggleShuffle()
  fun cycleRepeatMode() = playbackController.cycleRepeatMode()

  fun toggleFavorite(track: AudioTrack) {
    viewModelScope.launch {
      val unlocked = repository.toggleFavorite(track.id)
      if (unlocked != null) {
        _unlockedBanner.value = unlocked
      }
    }
  }

  fun importLocalAudio(uris: List<Uri>) {
    if (uris.isEmpty()) return
    viewModelScope.launch {
      val added = repository.importLocalAudio(uris)
      val msg = if (added > 0) {
        "Se importaron $added pistas de audio con metadatos y carátula."
      } else {
        "Las pistas seleccionadas ya estaban en tu fonoteca."
      }
      _uiMessages.tryEmit(msg)
    }
  }

  fun createPlaylist(name: String, description: String, motif: BotanicalSpeciesId, initialTrackIds: List<Long>) {
    if (name.isBlank()) return
    viewModelScope.launch {
      val (_, unlocked) = repository.createPlaylist(name, description, motif, initialTrackIds)
      if (unlocked != null) _unlockedBanner.value = unlocked
      _uiMessages.tryEmit("Lista «$name» guardada en tu herbario.")
    }
  }

  fun updatePlaylist(playlistId: Long, name: String, description: String, motif: BotanicalSpeciesId) {
    if (name.isBlank()) return
    viewModelScope.launch {
      repository.updatePlaylist(playlistId, name, description, motif)
      _uiMessages.tryEmit("Lista «$name» actualizada.")
    }
  }

  fun deletePlaylist(playlistId: Long, name: String) {
    viewModelScope.launch {
      repository.deletePlaylist(playlistId)
      _uiMessages.tryEmit("Lista «$name» eliminada.")
    }
  }

  fun addTrackToPlaylist(playlistId: Long, playlistName: String, track: AudioTrack) {
    viewModelScope.launch {
      repository.addTrackToPlaylist(playlistId, track.id)
      _uiMessages.tryEmit("«${track.title}» añadida a «$playlistName».")
    }
  }

  fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
    viewModelScope.launch {
      repository.removeTrackFromPlaylist(playlistId, trackId)
    }
  }

  fun savePlaylistOrder(playlistId: Long, orderedTracks: List<AudioTrack>) {
    viewModelScope.launch {
      repository.reorderPlaylistTracks(playlistId, orderedTracks.map { it.id })
    }
  }

  fun toggleEqualizerEnabled(enabled: Boolean) {
    playbackController.setEqualizerEnabled(enabled)
    viewModelScope.launch { prefsRepo.saveEqualizerEnabled(enabled) }
  }

  fun selectEqualizerPreset(preset: EqualizerPreset) {
    viewModelScope.launch {
      prefsRepo.savePreset(preset)
      if (preset != EqualizerPreset.CUSTOM) {
        playbackController.applyEqualizerBands(preset.bandLevels)
        playbackController.applyBassBoost(preset.recommendedBassBoost)
        playbackController.applyVirtualizer(preset.recommendedVirtualizer)
      }
      if (preset == EqualizerPreset.WARM_ACOUSTIC ||
        preset == EqualizerPreset.CLASSICAL_HERBARIUM ||
        preset == EqualizerPreset.LOTUS_RESONANCE
      ) {
        repository.unlockSpeciesIfNeeded(
          BotanicalSpeciesId.LOTUS,
          "Desbloqueado al elegir el perfil botánico ${preset.label}"
        )?.let { _unlockedBanner.value = it }
      } else if (preset == EqualizerPreset.MONSTERA_CANOPY) {
        repository.unlockSpeciesIfNeeded(
          BotanicalSpeciesId.MONSTERA,
          "Desbloqueado al explorar el dosel acústico de Monstera"
        )?.let { _unlockedBanner.value = it }
      }
    }
  }

  fun updateBandLevel(bandIndex: Int, levelMb: Int) {
    val current = uiState.value.effectsState.bandLevelsMb.toMutableList()
    if (bandIndex !in current.indices) return
    current[bandIndex] = levelMb
    playbackController.applyEqualizerBands(current)
    viewModelScope.launch {
      prefsRepo.saveCustomBands(current)
      repository.unlockSpeciesIfNeeded(
        BotanicalSpeciesId.PASSIFLORA,
        "Desbloqueado al esculpir frecuencias en el ecualizador"
      )?.let { _unlockedBanner.value = it }
    }
  }

  fun resetEqualizerFlat() {
    val flat = listOf(0, 0, 0, 0, 0)
    playbackController.applyEqualizerBands(flat)
    viewModelScope.launch {
      prefsRepo.saveCustomBands(flat)
    }
  }

  fun updateBassBoost(strength: Int) {
    playbackController.applyBassBoost(strength)
    viewModelScope.launch { prefsRepo.saveBassBoost(strength) }
  }

  fun updateVirtualizer(strength: Int) {
    playbackController.applyVirtualizer(strength)
    viewModelScope.launch { prefsRepo.saveVirtualizer(strength) }
  }

  fun updatePlaybackSpeed(speed: Float) {
    playbackController.setPlaybackSpeed(speed)
    viewModelScope.launch { prefsRepo.savePlaybackSpeed(speed) }
  }

  fun toggleGapless(enabled: Boolean) {
    playbackController.setGaplessEnabled(enabled)
    viewModelScope.launch { prefsRepo.saveGaplessEnabled(enabled) }
  }

  fun toggleCrossfade(enabled: Boolean) {
    playbackController.setCrossfadeEnabled(enabled)
    viewModelScope.launch { prefsRepo.saveCrossfadeEnabled(enabled) }
  }

  fun toggleChameleonTheme(enabled: Boolean) {
    paletteService.setDynamicGradientsEnabled(enabled)
    viewModelScope.launch { prefsRepo.saveChameleonTheme(enabled) }
  }

  fun selectPlayerTheme(themeId: String) {
    viewModelScope.launch { prefsRepo.savePlayerThemeId(themeId) }
  }

  fun setSleepTimer(minutes: Int?) {
    playbackController.setSleepTimer(minutes)
    if (minutes != null && minutes > 0) {
      viewModelScope.launch {
        repository.unlockSpeciesIfNeeded(
          BotanicalSpeciesId.LAVENDER,
          "Desbloqueado al programar el temporizador de reposo ($minutes min)"
        )?.let { _unlockedBanner.value = it }
      }
      _uiMessages.tryEmit("Temporizador de reposo activo: $minutes min")
    } else {
      _uiMessages.tryEmit("Temporizador de reposo desactivado")
    }
  }

  fun previewSpeciesPalette(speciesId: BotanicalSpeciesId) {
    viewModelScope.launch {
      paletteService.updateForSpecies(speciesId)
    }
  }
}
