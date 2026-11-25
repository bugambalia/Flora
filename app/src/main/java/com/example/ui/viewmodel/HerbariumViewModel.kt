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
import com.example.data.local.SavedPlaybackSession
import com.example.data.model.AlbumGroupSummary
import com.example.data.model.AlbumPaletteColors
import com.example.data.model.AppThemeMode
import com.example.data.model.ArtistGroupSummary
import com.example.data.model.AudioEffectsState
import com.example.data.model.AudioSourceSummary
import com.example.data.model.AudioTrack
import com.example.data.model.BotanicalCatalog
import com.example.data.model.BotanicalProgress
import com.example.data.model.BotanicalSpecies
import com.example.data.model.BotanicalSpeciesId
import com.example.data.model.EqualizerPreset
import com.example.data.model.LibraryCatalogData
import com.example.data.model.LibraryFilterTab
import com.example.data.model.LibrarySortOrder
import com.example.data.model.PlaybackState
import com.example.data.model.PlaylistWithTracks
import com.example.data.model.ThemeBackgroundGradients
import com.example.data.model.buildLibraryCatalogData
import com.example.data.repository.PlaylistSerializer
import com.example.ui.components.CoverArtCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Immutable
data class HerbariumUiState(
  val isLoading: Boolean = true,
  val isScanningFolders: Boolean = false,
  val tracks: List<AudioTrack> = emptyList(),
  val filteredTracks: List<AudioTrack> = emptyList(),
  val libraryCatalog: LibraryCatalogData = LibraryCatalogData(),
  val albumGroups: List<AlbumGroupSummary> = emptyList(),
  val artistGroups: List<ArtistGroupSummary> = emptyList(),
  val availableGenres: List<String> = emptyList(),
  val searchQuery: String = "",
  val selectedGenre: String? = null,
  val selectedSourcePath: String? = null,
  val libraryTab: LibraryFilterTab = LibraryFilterTab.ALL,
  val librarySortOrder: LibrarySortOrder = LibrarySortOrder.TITLE_AZ,
  val audioSources: List<AudioSourceSummary> = emptyList(),
  val detectedFolders: List<DetectedFolderSource> = emptyList(),
  val dominantDetectedFolder: DetectedFolderSource? = null,
  val playlists: List<PlaylistWithTracks> = emptyList(),
  val botanicalProgress: Map<BotanicalSpeciesId, BotanicalProgress> = emptyMap(),
  val totalListenSeconds: Int = 0,
  val coverCacheBytes: Long = 0L,
  val playbackState: PlaybackState = PlaybackState(),
  val effectsState: AudioEffectsState = AudioEffectsState(),
  val albumPalette: AlbumPaletteColors = AlbumPaletteColors(),
  val themeGradients: ThemeBackgroundGradients = ThemeBackgroundGradients.Default,
  val unlockedCount: Int = 1,
)

private data class LibraryFilterParams(
  val query: String = "",
  val genre: String? = null,
  val sourcePath: String? = null,
  val tab: LibraryFilterTab = LibraryFilterTab.ALL,
  val sortOrder: LibrarySortOrder = LibrarySortOrder.TITLE_AZ,
)

private data class LibraryCatalogSnapshot(
  val tracks: List<AudioTrack> = emptyList(),
  val filteredTracks: List<AudioTrack> = emptyList(),
  val libraryCatalog: LibraryCatalogData = LibraryCatalogData(),
  val albumGroups: List<AlbumGroupSummary> = emptyList(),
  val artistGroups: List<ArtistGroupSummary> = emptyList(),
  val availableGenres: List<String> = emptyList(),
  val searchQuery: String = "",
  val selectedGenre: String? = null,
  val selectedSourcePath: String? = null,
  val libraryTab: LibraryFilterTab = LibraryFilterTab.ALL,
  val librarySortOrder: LibrarySortOrder = LibrarySortOrder.TITLE_AZ,
  val audioSources: List<AudioSourceSummary> = emptyList(),
)

private data class ProgressSnapshot(
  val botanicalProgress: Map<BotanicalSpeciesId, BotanicalProgress> = emptyMap(),
  val totalListenSeconds: Int = 0,
  val unlockedCount: Int = 1,
)

private data class ScanSnapshot(
  val isScanningFolders: Boolean = false,
  val detectedFolders: List<DetectedFolderSource> = emptyList(),
  val dominantDetectedFolder: DetectedFolderSource? = null,
)

private data class CatalogSnapshot(
  val isLoading: Boolean = true,
  val isScanningFolders: Boolean = false,
  val tracks: List<AudioTrack> = emptyList(),
  val filteredTracks: List<AudioTrack> = emptyList(),
  val libraryCatalog: LibraryCatalogData = LibraryCatalogData(),
  val albumGroups: List<AlbumGroupSummary> = emptyList(),
  val artistGroups: List<ArtistGroupSummary> = emptyList(),
  val availableGenres: List<String> = emptyList(),
  val searchQuery: String = "",
  val selectedGenre: String? = null,
  val selectedSourcePath: String? = null,
  val libraryTab: LibraryFilterTab = LibraryFilterTab.ALL,
  val librarySortOrder: LibrarySortOrder = LibrarySortOrder.TITLE_AZ,
  val audioSources: List<AudioSourceSummary> = emptyList(),
  val detectedFolders: List<DetectedFolderSource> = emptyList(),
  val dominantDetectedFolder: DetectedFolderSource? = null,
  val playlists: List<PlaylistWithTracks> = emptyList(),
  val botanicalProgress: Map<BotanicalSpeciesId, BotanicalProgress> = emptyMap(),
  val totalListenSeconds: Int = 0,
  val coverCacheBytes: Long = 0L,
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
  private val libraryTab = MutableStateFlow(LibraryFilterTab.ALL)
  private val librarySortOrder = MutableStateFlow(LibrarySortOrder.TITLE_AZ)
  private val detectedFolders = MutableStateFlow<List<DetectedFolderSource>>(emptyList())
  private val dominantDetectedFolder = MutableStateFlow<DetectedFolderSource?>(null)
  private val isScanningFolders = MutableStateFlow(false)
  private val isInitialized = MutableStateFlow(false)
  private val coverCacheBytes = MutableStateFlow(0L)

  private val _unlockedBanner = MutableStateFlow<BotanicalSpecies?>(null)
  val unlockedBanner: StateFlow<BotanicalSpecies?> = _unlockedBanner.asStateFlow()

  private val _uiMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val uiMessages: SharedFlow<String> = _uiMessages.asSharedFlow()

  private val filterParamsFlow = combine(
    debouncedSearchQuery,
    selectedGenre,
    selectedSourcePath,
    libraryTab,
    librarySortOrder,
  ) { query, genre, sourcePath, tab, sortOrder ->
    LibraryFilterParams(
      query = query,
      genre = genre,
      sourcePath = sourcePath,
      tab = tab,
      sortOrder = sortOrder,
    )
  }

  private val libraryCatalogSnapshotFlow = combine(
    repository.tracksFlow.distinctUntilChanged(),
    filterParamsFlow.distinctUntilChanged(),
  ) { tracks, params ->
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

    val baseFiltered = tracks.filter { track ->
      val matchesQuery = params.query.isBlank() ||
        track.title.contains(params.query, ignoreCase = true) ||
        track.artist.contains(params.query, ignoreCase = true) ||
        track.album.contains(params.query, ignoreCase = true) ||
        track.genre.contains(params.query, ignoreCase = true) ||
        track.sourceFolder.contains(params.query, ignoreCase = true)
      val matchesGenre = params.genre == null || track.genre.equals(params.genre, ignoreCase = true)
      val matchesSource = params.sourcePath == null || track.sourcePath == params.sourcePath
      matchesQuery && matchesGenre && matchesSource
    }

    val tabFiltered = when (params.tab) {
      LibraryFilterTab.FAVORITES -> baseFiltered.filter { it.isFavorite }
      else -> baseFiltered
    }

    val effectiveSortOrder = when (params.tab) {
      LibraryFilterTab.MOST_PLAYED -> LibrarySortOrder.MOST_PLAYED
      LibraryFilterTab.RECENT -> LibrarySortOrder.RECENT
      else -> params.sortOrder
    }

    val libraryCatalog = buildLibraryCatalogData(tabFiltered, effectiveSortOrder)

    val albumGroups = baseFiltered
      .groupBy { it.album.trim().ifBlank { "Sin álbum" } }
      .map { (albumName, groupTracks) ->
        val sortedGroup = groupTracks.sortedBy { it.title.lowercase() }
        AlbumGroupSummary(
          albumName = albumName,
          artistName = sortedGroup.first().artist,
          tracks = sortedGroup,
          coverTrack = sortedGroup.firstOrNull { !it.coverPath.isNullOrBlank() } ?: sortedGroup.first(),
        )
      }
      .sortedBy { it.albumName.lowercase() }

    val artistGroups = baseFiltered
      .groupBy { it.artist.trim().ifBlank { "Artista desconocido" } }
      .map { (artistName, groupTracks) ->
        val sortedGroup = groupTracks.sortedBy { it.title.lowercase() }
        ArtistGroupSummary(
          artistName = artistName,
          albumCount = sortedGroup.map { it.album.trim().lowercase() }.distinct().size,
          tracks = sortedGroup,
          coverTrack = sortedGroup.firstOrNull { !it.coverPath.isNullOrBlank() } ?: sortedGroup.first(),
        )
      }
      .sortedBy { it.artistName.lowercase() }

    LibraryCatalogSnapshot(
      tracks = tracks,
      filteredTracks = tabFiltered,
      libraryCatalog = libraryCatalog,
      albumGroups = albumGroups,
      artistGroups = artistGroups,
      availableGenres = genres,
      searchQuery = params.query,
      selectedGenre = params.genre,
      selectedSourcePath = params.sourcePath,
      libraryTab = params.tab,
      librarySortOrder = params.sortOrder,
      audioSources = sources,
    )
  }
    .distinctUntilChanged()
    .flowOn(Dispatchers.Default)

  private val progressSnapshotFlow = repository.botanicalProgressFlow
    .distinctUntilChanged()
    .map { progress ->
      ProgressSnapshot(
        botanicalProgress = progress,
        totalListenSeconds = progress.values.sumOf { it.listenSeconds },
        unlockedCount = progress.values.count { it.isUnlocked }.coerceAtLeast(1),
      )
    }.distinctUntilChanged()

  private val scanSnapshotFlow = combine(
    BotanicalLibraryScanService.scanStatus,
    isScanningFolders,
    detectedFolders,
    dominantDetectedFolder,
  ) { scanStatus, manualScanning, detected, dominant ->
    ScanSnapshot(
      isScanningFolders = scanStatus.isScanning || manualScanning,
      detectedFolders = detected,
      dominantDetectedFolder = dominant,
    )
  }.distinctUntilChanged()

  private val catalogSnapshotFlow = combine(
    libraryCatalogSnapshotFlow,
    repository.playlistsFlow.distinctUntilChanged(),
    progressSnapshotFlow,
    scanSnapshotFlow,
    coverCacheBytes,
    isInitialized,
  ) { values ->
    val lib = values[0] as LibraryCatalogSnapshot
    @Suppress("UNCHECKED_CAST")
    val playlists = values[1] as List<PlaylistWithTracks>
    val prog = values[2] as ProgressSnapshot
    val scan = values[3] as ScanSnapshot
    val cacheBytes = values[4] as Long
    val ready = values[5] as Boolean

    CatalogSnapshot(
      isLoading = !ready,
      isScanningFolders = scan.isScanningFolders,
      tracks = lib.tracks,
      filteredTracks = lib.filteredTracks,
      libraryCatalog = lib.libraryCatalog,
      albumGroups = lib.albumGroups,
      artistGroups = lib.artistGroups,
      availableGenres = lib.availableGenres,
      searchQuery = lib.searchQuery,
      selectedGenre = lib.selectedGenre,
      selectedSourcePath = lib.selectedSourcePath,
      libraryTab = lib.libraryTab,
      librarySortOrder = lib.librarySortOrder,
      audioSources = lib.audioSources,
      detectedFolders = scan.detectedFolders,
      dominantDetectedFolder = scan.dominantDetectedFolder,
      playlists = playlists,
      botanicalProgress = prog.botanicalProgress,
      totalListenSeconds = prog.totalListenSeconds,
      coverCacheBytes = cacheBytes,
      unlockedCount = prog.unlockedCount,
    )
  }.distinctUntilChanged()

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
      albumGroups = catalog.albumGroups,
      artistGroups = catalog.artistGroups,
      availableGenres = catalog.availableGenres,
      searchQuery = catalog.searchQuery,
      selectedGenre = catalog.selectedGenre,
      selectedSourcePath = catalog.selectedSourcePath,
      libraryTab = catalog.libraryTab,
      librarySortOrder = catalog.librarySortOrder,
      audioSources = catalog.audioSources,
      detectedFolders = catalog.detectedFolders,
      dominantDetectedFolder = catalog.dominantDetectedFolder,
      playlists = catalog.playlists,
      botanicalProgress = catalog.botanicalProgress,
      totalListenSeconds = catalog.totalListenSeconds,
      coverCacheBytes = catalog.coverCacheBytes,
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
  private var sessionRestored = false
  private var pendingNeedFolderPicker: (() -> Unit)? = null

  init {
    viewModelScope.launch {
      repository.initializeLibraryIfNeeded()
      isInitialized.value = true
      refreshCoverCacheSize()
      restoreLastPlaybackSession()
      delay(1800L)
      if (prefsRepo.shouldRunAutoScan()) {
        BotanicalLibraryScanService.startScan(app)
      }
    }

    viewModelScope.launch {
      playbackController.playbackErrors.collectLatest { err ->
        _uiMessages.tryEmit(err)
      }
    }

    viewModelScope.launch {
      BotanicalLibraryScanService.scanCompletedEvents.collectLatest { summary ->
        detectedFolders.value = summary.scanResult.allFolders
        dominantDetectedFolder.value = summary.scanResult.winningFolder

        val winner = summary.scanResult.winningFolder
        if (summary.triggeredManually) {
          val thresholdSec = prefsRepo.getMinScanDurationMs() / 1000L
          if (winner != null && winner.qualifyingCount > 0) {
            selectedSourcePath.value = winner.folderKey
            val msg = if (summary.newlyImportedCount > 0 || summary.newlyCategorizedCount > 0) {
              "Botanical Library: ${summary.totalInBotanicalLibrary} pistas > ${thresholdSec}s (fuente «${winner.folderName}»)."
            } else {
              "Botanical Library actualizada (${summary.totalInBotanicalLibrary} pistas > ${thresholdSec}s)."
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
                  "Se omitieron $skipped audios de ${thresholdSec}s o menos; sin audios > ${thresholdSec}s."
                } else {
                  "No se detectaron pistas > ${thresholdSec}s en las carpetas analizadas."
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
        saveCurrentPlaybackSession()
      }
    }

    // Track changes to library propagate to active queue and initial palette
    viewModelScope.launch {
      repository.tracksFlow.collectLatest { tracks ->
        playbackController.syncTracksUpdated(tracks)
        if (!initialPaletteLoaded && tracks.isNotEmpty() && playbackController.playbackState.value.currentTrack == null) {
          initialPaletteLoaded = true
          paletteService.updateForTrack(tracks.first())
        }
      }
    }

    // Active listening time accumulator + periodic session checkpoint
    viewModelScope.launch {
      var pendingSpecies: BotanicalSpeciesId? = null
      var pendingSeconds = 0
      var checkpointTicks = 0
      var wasPlaying = false

      while (isActive) {
        delay(1000L)
        val pb = playbackController.playbackState.value
        val activeTrack = pb.currentTrack
        if (pb.isPlaying && activeTrack != null) {
          wasPlaying = true
          val trackSpecies = activeTrack.speciesId
          if (pendingSpecies != null && pendingSpecies != trackSpecies && pendingSeconds > 0) {
            repository.addListenSeconds(pendingSpecies, pendingSeconds)
            pendingSeconds = 0
          }
          pendingSpecies = trackSpecies
          pendingSeconds++
          checkpointTicks++

          if (pendingSeconds >= 5) {
            repository.addListenSeconds(trackSpecies, pendingSeconds)
            pendingSeconds = 0
          }
          if (checkpointTicks >= 5) {
            checkpointTicks = 0
            saveCurrentPlaybackSession()
          }
        } else {
          if (pendingSpecies != null && pendingSeconds > 0) {
            repository.addListenSeconds(pendingSpecies, pendingSeconds)
            pendingSeconds = 0
          }
          if (wasPlaying) {
            wasPlaying = false
            checkpointTicks = 0
            saveCurrentPlaybackSession()
          }
        }
      }
    }
  }

  private suspend fun restoreLastPlaybackSession() {
    if (sessionRestored || playbackController.playbackState.value.currentTrack != null) return
    sessionRestored = true
    val saved = prefsRepo.loadSavedPlaybackSession()
    if (saved.queueTrackIds.isEmpty()) return

    val allTracks = repository.tracksFlow.first()
    if (allTracks.isEmpty()) return
    val byId = allTracks.associateBy { it.id }
    val restoredQueue = saved.queueTrackIds.mapNotNull { byId[it] }
    if (restoredQueue.isEmpty()) return

    val safeIdx = saved.currentIndex.coerceIn(0, restoredQueue.lastIndex)
    playbackController.prepareRestoredSession(
      tracks = restoredQueue,
      startIndex = safeIdx,
      positionMs = saved.positionMs,
      playlistName = saved.playlistName,
      shuffleEnabled = saved.shuffleEnabled,
      repeatMode = saved.repeatMode,
    )
    restoredQueue.getOrNull(safeIdx)?.let {
      initialPaletteLoaded = true
      paletteService.updateForTrack(it)
    }
  }

  fun saveCurrentPlaybackSession() {
    val pb = playbackController.playbackState.value
    val pos = playbackController.positionMsFlow.value
    viewModelScope.launch(Dispatchers.IO) {
      prefsRepo.savePlaybackSession(
        SavedPlaybackSession(
          queueTrackIds = pb.queue.map { it.id },
          currentIndex = pb.currentIndex.coerceAtLeast(0),
          positionMs = pos.coerceAtLeast(0L),
          playlistName = pb.activePlaylistName,
          shuffleEnabled = pb.shuffleEnabled,
          repeatMode = pb.repeatMode,
        )
      )
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

  fun onSelectLibraryTab(tab: LibraryFilterTab) {
    libraryTab.value = tab
  }

  fun onSelectSortOrder(order: LibrarySortOrder) {
    librarySortOrder.value = order
  }

  fun dismissUnlockedBanner() {
    _unlockedBanner.value = null
  }

  fun dismissPlaybackError() {
    playbackController.clearErrorMessage()
  }

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

  fun triggerManualLibraryScan() {
    if (isScanningFolders.value || BotanicalLibraryScanService.scanStatus.value.isScanning) return
    pendingNeedFolderPicker = null
    BotanicalLibraryScanService.startScan(
      context = app,
      treeUri = null,
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
          "Fuente «${folder.folderName}»: $added pistas importadas."
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
  fun toggleShuffle() {
    playbackController.toggleShuffle()
    saveCurrentPlaybackSession()
  }

  fun cycleRepeatMode() {
    playbackController.cycleRepeatMode()
    saveCurrentPlaybackSession()
  }

  fun toggleFavorite(track: AudioTrack) {
    viewModelScope.launch {
      val unlocked = repository.toggleFavorite(track.id)
      if (unlocked != null) {
        _unlockedBanner.value = unlocked
      }
    }
  }

  fun updateTrackMetadata(
    trackId: Long,
    title: String,
    artist: String,
    album: String,
    genre: String,
    coverPath: String?,
  ) {
    viewModelScope.launch {
      repository.updateTrackMetadata(trackId, title, artist, album, genre, coverPath)
      CoverArtCache.clear()
      _uiMessages.tryEmit("Metadatos de «${title.trim()}» actualizados.")
    }
  }

  fun updateTrackLyrics(trackId: Long, rawLyrics: String?) {
    viewModelScope.launch {
      repository.updateTrackLyrics(trackId, rawLyrics)
      _uiMessages.tryEmit(
        if (rawLyrics.isNullOrBlank()) "Letra personalizada eliminada."
        else "Letra guardada para la pista."
      )
    }
  }

  fun importLyricsFromUri(trackId: Long, uri: Uri) {
    viewModelScope.launch {
      val content = withContext(Dispatchers.IO) {
        runCatching {
          app.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
      }
      if (content.isNullOrBlank()) {
        _uiMessages.tryEmit("No se pudo leer el archivo de letras.")
      } else {
        repository.updateTrackLyrics(trackId, content)
        _uiMessages.tryEmit("Archivo .lrc importado correctamente.")
      }
    }
  }

  fun deleteTrack(track: AudioTrack) {
    viewModelScope.launch {
      repository.deleteTrack(track.id)
      _uiMessages.tryEmit("«${track.title}» eliminada de la biblioteca.")
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

  fun duplicatePlaylist(playlist: PlaylistWithTracks) {
    viewModelScope.launch {
      val (_, unlocked) = repository.duplicatePlaylist(playlist)
      if (unlocked != null) _unlockedBanner.value = unlocked
      _uiMessages.tryEmit("Colección «${playlist.playlist.name}» duplicada.")
    }
  }

  fun exportPlaylistToUri(playlist: PlaylistWithTracks, format: String, uri: Uri) {
    viewModelScope.launch {
      val payload = if (format.equals("json", ignoreCase = true)) {
        PlaylistSerializer.exportToJson(playlist)
      } else {
        PlaylistSerializer.exportToM3u(playlist)
      }
      val ok = withContext(Dispatchers.IO) {
        runCatching {
          app.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(payload) }
          true
        }.getOrDefault(false)
      }
      _uiMessages.tryEmit(
        if (ok) "Colección «${playlist.playlist.name}» exportada en formato ${format.uppercase()}."
        else "No se pudo guardar el archivo de playlist."
      )
    }
  }

  fun importPlaylistFromUri(uri: Uri) {
    viewModelScope.launch {
      val raw = withContext(Dispatchers.IO) {
        runCatching {
          app.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
      }
      if (raw.isNullOrBlank()) {
        _uiMessages.tryEmit("Archivo de playlist vacío o ilegible.")
        return@launch
      }
      val parsed = PlaylistSerializer.parseImport(raw)
      val libraryTracks = uiState.value.tracks
      val (_, matchedCount) = repository.importPlaylistData(parsed, libraryTracks)
      _uiMessages.tryEmit(
        "Playlist «${parsed.name}» importada ($matchedCount de ${parsed.trackRefs.size} pistas encontradas)."
      )
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

  fun setThemeMode(mode: AppThemeMode) {
    viewModelScope.launch { prefsRepo.saveThemeMode(mode) }
  }

  fun setMinScanDurationSec(seconds: Int) {
    viewModelScope.launch {
      prefsRepo.saveMinScanDurationSec(seconds)
      _uiMessages.tryEmit("Umbral de escaneo ajustado a ${seconds}s.")
    }
  }

  fun refreshCoverCacheSize() {
    viewModelScope.launch(Dispatchers.IO) {
      val dirs = listOf(
        File(app.filesDir, "imported_covers"),
        File(app.filesDir, "custom_covers"),
        File(app.filesDir, "botanical_covers"),
        app.cacheDir,
      )
      val bytes = dirs.sumOf { dir ->
        if (dir.exists()) dir.walkTopDown().filter { it.isFile }.sumOf { it.length() } else 0L
      }
      coverCacheBytes.value = bytes
    }
  }

  fun clearCoverCache() {
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        CoverArtCache.clear()
        File(app.filesDir, "imported_covers").deleteRecursively()
        File(app.filesDir, "botanical_covers").deleteRecursively()
        app.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
      }
      refreshCoverCacheSize()
      _uiMessages.tryEmit("Caché de carátulas liberada.")
    }
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
