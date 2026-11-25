package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.AudioTrack
import com.example.data.model.BotanicalSpecies
import com.example.data.model.BotanicalSpeciesId
import com.example.data.model.PlaylistWithTracks
import com.example.ui.components.AddToPlaylistSheet
import com.example.ui.components.BotanicalSpecimenIllustration
import com.example.ui.components.CreateOrEditPlaylistDialog
import com.example.ui.components.DeleteTrackConfirmDialog
import com.example.ui.components.EditLyricsDialog
import com.example.ui.components.EditTrackMetadataDialog
import com.example.ui.components.EqualizerOverlayScreen
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.ParchmentBackground
import com.example.ui.components.TrackTechnicalDetailsDialog
import com.example.ui.components.UpNextQueueSheet
import com.example.ui.screens.FloracionScreen
import com.example.ui.screens.HerbariumScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.SettingsOverlayScreen
import com.example.ui.theme.FloraStripShape
import com.example.ui.theme.HerbariumTheme
import com.example.ui.theme.LocalFloraColors
import com.example.ui.viewmodel.HerbariumViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File

@Serializable object FloracionRoute
@Serializable object LibraryRoute
@Serializable object PlaylistsRoute

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: HerbariumViewModel = viewModel()
      HerbariumRootApp(viewModel = viewModel)
    }
  }

  override fun onStart() {
    super.onStart()
    (application as HerbariumApp).playbackController.connect()
  }

  override fun onStop() {
    super.onStop()
    (application as HerbariumApp).playbackController.disconnect()
  }
}

@Composable
private fun HerbariumRootApp(viewModel: HerbariumViewModel) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val unlockedBanner by viewModel.unlockedBanner.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val coroutineScope = rememberCoroutineScope()

  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDest = backStackEntry?.destination

  var showNowPlaying by rememberSaveable { mutableStateOf(false) }
  var showEqualizerOverlay by rememberSaveable { mutableStateOf(false) }
  var showHerbariumOverlay by rememberSaveable { mutableStateOf(false) }
  var showSettingsOverlay by rememberSaveable { mutableStateOf(false) }
  var showQueueSheet by rememberSaveable { mutableStateOf(false) }
  var selectedPlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }
  var inspectedSpeciesId by remember { mutableStateOf<BotanicalSpeciesId?>(null) }
  var trackPendingPlaylist by remember { mutableStateOf<AudioTrack?>(null) }
  var showCreatePlaylistModal by rememberSaveable { mutableStateOf(false) }

  // Track action dialog states
  var trackForDetails by remember { mutableStateOf<AudioTrack?>(null) }
  var trackForEditMetadata by remember { mutableStateOf<AudioTrack?>(null) }
  var pendingEditCoverPath by remember { mutableStateOf<String?>(null) }
  var trackForEditLyrics by remember { mutableStateOf<Pair<AudioTrack, String>?>(null) }
  var trackPendingLrcImport by remember { mutableStateOf<AudioTrack?>(null) }
  var trackForDelete by remember { mutableStateOf<AudioTrack?>(null) }
  var playlistPendingExport by remember { mutableStateOf<PlaylistWithTracks?>(null) }

  val audioPermission = remember {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      Manifest.permission.READ_MEDIA_AUDIO
    } else {
      Manifest.permission.READ_EXTERNAL_STORAGE
    }
  }
  var hasAudioPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
    )
  }

  val startupPermissionsLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
    onResult = { results ->
      val audioGranted = results[audioPermission] == true ||
        ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
      hasAudioPermission = audioGranted
      if (audioGranted) {
        viewModel.scanAndAutoImportFolder()
      }
    }
  )

  val audioPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { granted ->
      hasAudioPermission = granted
      if (granted) {
        viewModel.scanAndAutoImportFolder()
      }
    }
  )

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> {
          hasAudioPermission = ContextCompat.checkSelfPermission(
            context,
            audioPermission
          ) == PackageManager.PERMISSION_GRANTED
        }
        Lifecycle.Event.ON_STOP -> {
          viewModel.saveCurrentPlaybackSession()
        }
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  LaunchedEffect(Unit) {
    val needed = buildList {
      if (ContextCompat.checkSelfPermission(context, audioPermission) != PackageManager.PERMISSION_GRANTED) {
        add(audioPermission)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
      ) {
        add(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
    if (needed.isNotEmpty()) {
      startupPermissionsLauncher.launch(needed.toTypedArray())
    }
  }

  val folderTreePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree(),
    onResult = { treeUri ->
      if (treeUri != null) {
        viewModel.scanAndAutoImportFolder(treeUri = treeUri)
      }
    }
  )

  val importAudioFilesLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments(),
    onResult = { uris ->
      if (uris.isNotEmpty()) {
        viewModel.importLocalAudio(uris)
      }
    }
  )

  val coverPhotoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
    onResult = { uri ->
      if (uri != null) {
        coroutineScope.launch {
          val copiedPath = copyImageToCustomCovers(context, uri)
          if (copiedPath != null) {
            pendingEditCoverPath = copiedPath
          } else {
            snackbarHostState.showSnackbar("No se pudo cargar la imagen seleccionada.")
          }
        }
      }
    }
  )

  val importLrcLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
    onResult = { uri ->
      val targetTrack = trackPendingLrcImport
      trackPendingLrcImport = null
      if (uri != null && targetTrack != null) {
        viewModel.importLyricsFromUri(targetTrack.id, uri)
        trackForEditLyrics = null
      }
    }
  )

  val importPlaylistLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
    onResult = { uri ->
      if (uri != null) {
        viewModel.importPlaylistFromUri(uri)
      }
    }
  )

  val exportM3uLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("audio/x-mpegurl"),
    onResult = { uri ->
      val pl = playlistPendingExport
      playlistPendingExport = null
      if (uri != null && pl != null) {
        viewModel.exportPlaylistToUri(pl, "m3u", uri)
      }
    }
  )

  val exportJsonLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json"),
    onResult = { uri ->
      val pl = playlistPendingExport
      playlistPendingExport = null
      if (uri != null && pl != null) {
        viewModel.exportPlaylistToUri(pl, "json", uri)
      }
    }
  )

  LaunchedEffect(Unit) {
    viewModel.uiMessages.collectLatest { message ->
      snackbarHostState.showSnackbar(message)
    }
  }

  LaunchedEffect(showNowPlaying) {
    viewModel.setVisualizerActive(showNowPlaying)
  }

  HerbariumTheme(
    albumPalette = uiState.albumPalette,
    themeGradients = uiState.themeGradients,
    chameleonEnabled = uiState.effectsState.chameleonThemeEnabled,
    themeMode = uiState.effectsState.themeMode,
  ) {
    val colors = LocalFloraColors.current

    val navigateToTab: (Any) -> Unit = { route ->
      showHerbariumOverlay = false
      showSettingsOverlay = false
      navController.navigate(route) {
        popUpTo<FloracionRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
      }
    }

    val openSpeciesFolio: (BotanicalSpeciesId) -> Unit = { id ->
      inspectedSpeciesId = id
      showNowPlaying = false
      showEqualizerOverlay = false
      showSettingsOverlay = false
      showHerbariumOverlay = true
    }

    val openHerbariumAtlas: () -> Unit = {
      inspectedSpeciesId = null
      showSettingsOverlay = false
      showHerbariumOverlay = true
    }

    val openSettings: () -> Unit = {
      viewModel.refreshCoverCacheSize()
      showSettingsOverlay = true
    }

    val openEditTrackMetadata: (AudioTrack) -> Unit = { track ->
      pendingEditCoverPath = track.coverPath
      trackForEditMetadata = track
    }

    val shareTrack: (AudioTrack) -> Unit = { track ->
      val shared = shareAudioTrack(context, track)
      if (!shared) {
        coroutineScope.launch {
          snackbarHostState.showSnackbar("No se encontró una app disponible para compartir esta pista.")
        }
      }
    }

    Box(modifier = Modifier.fillMaxSize()) {
      ParchmentBackground(chameleonEnabled = uiState.effectsState.chameleonThemeEnabled) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
          val isExpanded = maxWidth >= 700.dp

          Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
              if (!isExpanded && !showNowPlaying && !showEqualizerOverlay && !showHerbariumOverlay && !showSettingsOverlay) {
                Column(modifier = Modifier.fillMaxWidth()) {
                  AnimatedVisibility(
                    visible = uiState.playbackState.currentTrack != null,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                  ) {
                    MiniPlayerBar(
                      playbackState = uiState.playbackState,
                      positionMsFlow = viewModel.playbackPositionMs,
                      onTogglePlayPause = viewModel::togglePlayPause,
                      onPrevious = viewModel::previous,
                      onNext = viewModel::next,
                      onToggleFavorite = {
                        uiState.playbackState.currentTrack?.let(viewModel::toggleFavorite)
                      },
                      onOpenNowPlaying = { showNowPlaying = true }
                    )
                  }

                  HorizontalDivider(thickness = 1.dp, color = colors.borderInk.copy(alpha = 0.22f))
                  NavigationBar(
                    containerColor = colors.paper.copy(alpha = 0.98f),
                    tonalElevation = 0.dp
                  ) {
                    val isHome = currentDest?.hasRoute<FloracionRoute>() == true || currentDest == null
                    val isExplore = currentDest?.hasRoute<LibraryRoute>() == true
                    val isCollection = currentDest?.hasRoute<PlaylistsRoute>() == true

                    val navColors = NavigationBarItemDefaults.colors(
                      selectedIconColor = colors.ink,
                      selectedTextColor = colors.ink,
                      unselectedIconColor = colors.navIdle,
                      unselectedTextColor = colors.navIdle,
                      indicatorColor = colors.sageLight
                    )

                    NavigationBarItem(
                      selected = isHome,
                      onClick = { navigateToTab(FloracionRoute) },
                      icon = {
                        Icon(
                          if (isHome) Icons.Filled.Home else Icons.Outlined.Home,
                          contentDescription = "Floración"
                        )
                      },
                      label = {
                        Text(
                          "Floración",
                          fontWeight = if (isHome) FontWeight.Bold else FontWeight.Medium
                        )
                      },
                      colors = navColors,
                      modifier = Modifier.testTag("nav_floracion")
                    )
                    NavigationBarItem(
                      selected = isExplore,
                      onClick = { navigateToTab(LibraryRoute) },
                      icon = {
                        Icon(
                          if (isExplore) Icons.Filled.MusicNote else Icons.Outlined.MusicNote,
                          contentDescription = "Canciones"
                        )
                      },
                      label = {
                        Text(
                          "Canciones",
                          fontWeight = if (isExplore) FontWeight.Bold else FontWeight.Medium
                        )
                      },
                      colors = navColors,
                      modifier = Modifier.testTag("nav_library")
                    )
                    NavigationBarItem(
                      selected = isCollection,
                      onClick = { navigateToTab(PlaylistsRoute) },
                      icon = {
                        Icon(
                          if (isCollection) Icons.AutoMirrored.Filled.QueueMusic else Icons.AutoMirrored.Outlined.QueueMusic,
                          contentDescription = "Playlists"
                        )
                      },
                      label = {
                        Text(
                          "Playlists",
                          fontWeight = if (isCollection) FontWeight.Bold else FontWeight.Medium
                        )
                      },
                      colors = navColors,
                      modifier = Modifier.testTag("nav_playlists")
                    )
                  }
                }
              }
            }
          ) { innerPadding ->
            Row(
              modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
            ) {
              if (isExpanded) {
                NavigationRail(
                  containerColor = colors.paper.copy(alpha = 0.96f),
                  modifier = Modifier.fillMaxHeight()
                ) {
                  val isHome = currentDest?.hasRoute<FloracionRoute>() == true || currentDest == null
                  val isExplore = currentDest?.hasRoute<LibraryRoute>() == true
                  val isCollection = currentDest?.hasRoute<PlaylistsRoute>() == true

                  val railColors = NavigationRailItemDefaults.colors(
                    selectedIconColor = colors.ink,
                    selectedTextColor = colors.ink,
                    unselectedIconColor = colors.navIdle,
                    unselectedTextColor = colors.navIdle,
                    indicatorColor = colors.sageLight
                  )

                  Spacer(modifier = Modifier.weight(0.2f))
                  NavigationRailItem(
                    selected = !showHerbariumOverlay && !showSettingsOverlay && isHome,
                    onClick = { navigateToTab(FloracionRoute) },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Floración") },
                    label = { Text("Floración") },
                    colors = railColors
                  )
                  NavigationRailItem(
                    selected = !showHerbariumOverlay && !showSettingsOverlay && isExplore,
                    onClick = { navigateToTab(LibraryRoute) },
                    icon = { Icon(Icons.Filled.MusicNote, contentDescription = "Canciones") },
                    label = { Text("Canciones") },
                    colors = railColors
                  )
                  NavigationRailItem(
                    selected = !showHerbariumOverlay && !showSettingsOverlay && isCollection,
                    onClick = { navigateToTab(PlaylistsRoute) },
                    icon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Playlists") },
                    label = { Text("Playlists") },
                    colors = railColors
                  )
                  NavigationRailItem(
                    selected = showHerbariumOverlay,
                    onClick = openHerbariumAtlas,
                    icon = { Icon(Icons.Filled.LocalFlorist, contentDescription = "Herbario") },
                    label = { Text("Herbario") },
                    colors = railColors
                  )
                  NavigationRailItem(
                    selected = showSettingsOverlay,
                    onClick = openSettings,
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Ajustes") },
                    label = { Text("Ajustes") },
                    colors = railColors
                  )
                  Spacer(modifier = Modifier.weight(1f))
                }
              }

              Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (uiState.isLoading) {
                  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.terracotta)
                  }
                } else {
                  NavHost(
                    navController = navController,
                    startDestination = FloracionRoute,
                    modifier = Modifier.fillMaxSize()
                  ) {
                    composable<FloracionRoute> {
                      FloracionScreen(
                        uiState = uiState,
                        onPlayHero = {
                          val current = uiState.playbackState.currentTrack
                          if (current != null) {
                            viewModel.togglePlayPause()
                          } else if (uiState.tracks.isNotEmpty()) {
                            viewModel.playTrackFromList(uiState.tracks, 0, "Lámina del Día")
                          }
                          showNowPlaying = true
                        },
                        onOpenPlaylist = { pl ->
                          selectedPlaylistId = pl.playlist.id
                          navigateToTab(PlaylistsRoute)
                        },
                        onPlayPlaylist = { pl ->
                          viewModel.playPlaylist(pl, 0)
                        },
                        onOpenExplore = { navigateToTab(LibraryRoute) },
                        onOpenHerbarium = openHerbariumAtlas,
                        onOpenSettings = openSettings,
                      )
                    }

                    composable<LibraryRoute> {
                      val liveSearchQuery by viewModel.searchQueryState.collectAsStateWithLifecycle()
                      LibraryScreen(
                        uiState = uiState,
                        searchQuery = liveSearchQuery,
                        onSearchChange = viewModel::onSearchQueryChange,
                        onSelectSourcePath = viewModel::onSelectSourcePath,
                        onPlayTrack = { list, idx ->
                          viewModel.playTrackFromList(list, idx, "Canciones")
                        },
                        onPickRootFolderClick = {
                          folderTreePickerLauncher.launch(null)
                        },
                        onImportDetectedFolder = viewModel::importSpecificDetectedFolder,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onPlayNext = viewModel::playNextInQueue,
                        onAddToQueue = viewModel::appendToQueue,
                        onAddToPlaylistClick = { track -> trackPendingPlaylist = track },
                        onInspectSpecies = openSpeciesFolio,
                        onOpenHerbarium = openHerbariumAtlas,
                        selectedGenre = uiState.selectedGenre,
                        onSelectGenre = viewModel::onSelectGenre,
                        onSelectLibraryTab = viewModel::onSelectLibraryTab,
                        onSelectSortOrder = viewModel::onSelectSortOrder,
                        onImportFilesClick = { importAudioFilesLauncher.launch(arrayOf("audio/*")) },
                        hasAudioPermission = hasAudioPermission,
                        onRequestAudioPermission = { audioPermissionLauncher.launch(audioPermission) },
                        onShowTrackDetails = { trackForDetails = it },
                        onEditTrackMetadata = openEditTrackMetadata,
                        onShareTrack = shareTrack,
                        onDeleteTrack = { trackForDelete = it },
                        onOpenSettings = openSettings,
                      )
                    }

                    composable<PlaylistsRoute> {
                      PlaylistsScreen(
                        uiState = uiState,
                        selectedPlaylistId = selectedPlaylistId,
                        onSelectPlaylist = { selectedPlaylistId = it },
                        onPlayPlaylist = { pl, idx -> viewModel.playPlaylist(pl, idx) },
                        onShufflePlaylist = { pl ->
                          viewModel.shuffleAndPlay(pl.tracks, pl.playlist.name)
                        },
                        onCreatePlaylist = viewModel::createPlaylist,
                        onUpdatePlaylist = viewModel::updatePlaylist,
                        onDeletePlaylist = viewModel::deletePlaylist,
                        onAddTrackToPlaylist = viewModel::addTrackToPlaylist,
                        onRemoveTrackFromPlaylist = viewModel::removeTrackFromPlaylist,
                        onSavePlaylistOrder = viewModel::savePlaylistOrder,
                        onOpenHerbarium = openHerbariumAtlas,
                        onDuplicatePlaylist = viewModel::duplicatePlaylist,
                        onExportPlaylist = { pl, fmt ->
                          playlistPendingExport = pl
                          val safeFileBase = pl.playlist.name.replace(Regex("[^A-Za-z0-9._-]"), "_")
                          if (fmt.equals("json", ignoreCase = true)) {
                            exportJsonLauncher.launch("$safeFileBase.json")
                          } else {
                            exportM3uLauncher.launch("$safeFileBase.m3u")
                          }
                        },
                        onImportPlaylistClick = {
                          importPlaylistLauncher.launch(arrayOf("*/*"))
                        },
                      )
                    }
                  }
                }

                if (isExpanded && uiState.playbackState.currentTrack != null && !showNowPlaying && !showEqualizerOverlay && !showHerbariumOverlay && !showSettingsOverlay) {
                  MiniPlayerBar(
                    playbackState = uiState.playbackState,
                    positionMsFlow = viewModel.playbackPositionMs,
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onPrevious = viewModel::previous,
                    onNext = viewModel::next,
                    onToggleFavorite = {
                      uiState.playbackState.currentTrack?.let(viewModel::toggleFavorite)
                    },
                    onOpenNowPlaying = { showNowPlaying = true },
                    modifier = Modifier
                      .align(Alignment.BottomCenter)
                      .padding(bottom = 12.dp)
                  )
                }
              }
            }
          }
        }
      }

      AnimatedVisibility(
        visible = showHerbariumOverlay,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
      ) {
        ParchmentBackground(chameleonEnabled = uiState.effectsState.chameleonThemeEnabled) {
          HerbariumScreen(
            uiState = uiState,
            inspectedSpeciesId = inspectedSpeciesId,
            onInspectSpecies = { inspectedSpeciesId = it },
            onPlayGenreForSpecies = { species ->
              val matching = uiState.tracks.filter {
                it.speciesId == species.id ||
                  species.associatedGenres.any { g -> it.genre.contains(g, ignoreCase = true) }
              }
              if (matching.isNotEmpty()) {
                viewModel.playTrackFromList(matching, 0, species.commonName)
                showHerbariumOverlay = false
                showNowPlaying = true
              } else if (uiState.tracks.isNotEmpty()) {
                viewModel.playTrackFromList(uiState.tracks, 0, "Fonoteca")
              }
            },
            onPreviewPalette = viewModel::previewSpeciesPalette,
            onBack = { showHerbariumOverlay = false }
          )
        }
      }

      AnimatedVisibility(
        visible = showSettingsOverlay,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
      ) {
        SettingsOverlayScreen(
          uiState = uiState,
          hasAudioPermission = hasAudioPermission,
          onRequestAudioPermission = { audioPermissionLauncher.launch(audioPermission) },
          onTriggerManualScan = viewModel::triggerManualLibraryScan,
          onMinScanDurationChange = viewModel::setMinScanDurationSec,
          onPickFolderClick = { folderTreePickerLauncher.launch(null) },
          onImportFilesClick = { importAudioFilesLauncher.launch(arrayOf("audio/*")) },
          onThemeModeChange = viewModel::setThemeMode,
          onChameleonToggle = viewModel::toggleChameleonTheme,
          onClearCoverCache = viewModel::clearCoverCache,
          onDismiss = { showSettingsOverlay = false }
        )
      }

      AnimatedVisibility(
        visible = showNowPlaying,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
      ) {
        NowPlayingScreen(
          uiState = uiState,
          audioSpectrumFlow = viewModel.audioSpectrum,
          positionMsFlow = viewModel.playbackPositionMs,
          onClose = { showNowPlaying = false },
          onTogglePlayPause = viewModel::togglePlayPause,
          onSeekTo = viewModel::seekTo,
          onPrevious = viewModel::previous,
          onNext = viewModel::next,
          onToggleShuffle = viewModel::toggleShuffle,
          onCycleRepeat = viewModel::cycleRepeatMode,
          onToggleFavorite = viewModel::toggleFavorite,
          onOpenEqualizer = { showEqualizerOverlay = true },
          onOpenQueue = { showQueueSheet = true },
          onAddToPlaylist = { track -> trackPendingPlaylist = track },
          onInspectSpecies = openSpeciesFolio,
          onSelectPlayerTheme = viewModel::selectPlayerTheme,
          onDismissPlaybackError = viewModel::dismissPlaybackError,
          onShowTrackDetails = { trackForDetails = it },
          onEditTrackMetadata = openEditTrackMetadata,
          onShareTrack = shareTrack,
          onEditLyrics = { track, seedText -> trackForEditLyrics = track to seedText },
          onImportLrcFile = { track ->
            trackPendingLrcImport = track
            importLrcLauncher.launch(arrayOf("*/*"))
          },
        )
      }

      AnimatedVisibility(
        visible = showEqualizerOverlay,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
      ) {
        EqualizerOverlayScreen(
          effectsState = uiState.effectsState,
          playbackState = uiState.playbackState,
          onTogglePlayPause = viewModel::togglePlayPause,
          onToggleEqualizerEnabled = viewModel::toggleEqualizerEnabled,
          onSelectPreset = viewModel::selectEqualizerPreset,
          onBandLevelChange = viewModel::updateBandLevel,
          onResetEqualizerFlat = viewModel::resetEqualizerFlat,
          onBassBoostChange = viewModel::updateBassBoost,
          onVirtualizerChange = viewModel::updateVirtualizer,
          onSpeedChange = viewModel::updatePlaybackSpeed,
          onGaplessToggle = viewModel::toggleGapless,
          onCrossfadeToggle = viewModel::toggleCrossfade,
          onChameleonToggle = viewModel::toggleChameleonTheme,
          onSleepTimerSelect = viewModel::setSleepTimer,
          onDismiss = { showEqualizerOverlay = false }
        )
      }

      AnimatedVisibility(
        visible = unlockedBanner != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = Modifier
          .align(Alignment.TopCenter)
          .statusBarsPadding()
          .padding(horizontal = 16.dp, vertical = 10.dp)
      ) {
        unlockedBanner?.let { species ->
          BotanicalBloomBanner(
            species = species,
            onViewPlate = {
              viewModel.dismissUnlockedBanner()
              openSpeciesFolio(species.id)
            },
            onDismiss = viewModel::dismissUnlockedBanner
          )
        }
      }
    }

    if (showQueueSheet) {
      UpNextQueueSheet(
        playbackState = uiState.playbackState,
        onJumpToIndex = viewModel::jumpToQueueIndex,
        onMoveItem = viewModel::moveQueueItem,
        onRemoveItem = viewModel::removeFromQueue,
        onDismiss = { showQueueSheet = false }
      )
    }

    trackPendingPlaylist?.let { track ->
      AddToPlaylistSheet(
        track = track,
        playlists = uiState.playlists,
        onAddTrackToPlaylist = { pl ->
          viewModel.addTrackToPlaylist(pl.playlist.id, pl.playlist.name, track)
        },
        onRequestNewPlaylist = { showCreatePlaylistModal = true },
        onDismiss = { trackPendingPlaylist = null }
      )
    }

    if (showCreatePlaylistModal) {
      CreateOrEditPlaylistDialog(
        onConfirm = { name, desc, motif ->
          viewModel.createPlaylist(name, desc, motif, emptyList())
          showCreatePlaylistModal = false
        },
        onDismiss = { showCreatePlaylistModal = false }
      )
    }

    trackForDetails?.let { track ->
      TrackTechnicalDetailsDialog(
        track = track,
        onDismiss = { trackForDetails = null }
      )
    }

    trackForEditMetadata?.let { track ->
      EditTrackMetadataDialog(
        track = track,
        selectedCoverPath = pendingEditCoverPath,
        onPickCoverImage = {
          coverPhotoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          )
        },
        onClearCustomCover = { pendingEditCoverPath = null },
        onSave = { title, artist, album, genre, coverPath ->
          viewModel.updateTrackMetadata(track.id, title, artist, album, genre, coverPath)
          trackForEditMetadata = null
        },
        onDismiss = { trackForEditMetadata = null }
      )
    }

    trackForEditLyrics?.let { (track, seedText) ->
      EditLyricsDialog(
        track = track,
        initialLyrics = seedText,
        onImportLrcFile = {
          trackPendingLrcImport = track
          importLrcLauncher.launch(arrayOf("*/*"))
        },
        onSave = { rawLyrics ->
          viewModel.updateTrackLyrics(track.id, rawLyrics)
          trackForEditLyrics = null
        },
        onDismiss = { trackForEditLyrics = null }
      )
    }

    trackForDelete?.let { track ->
      DeleteTrackConfirmDialog(
        track = track,
        onConfirm = {
          viewModel.deleteTrack(track)
          trackForDelete = null
        },
        onDismiss = { trackForDelete = null }
      )
    }
  }
}

private suspend fun copyImageToCustomCovers(context: Context, uri: Uri): String? =
  withContext(Dispatchers.IO) {
    runCatching {
      val maxDimPx = 720
      val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use { input ->
        android.graphics.BitmapFactory.decodeStream(input, null, bounds)
      }
      if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

      val sampleSize = com.example.ui.components.calculateInSampleSize(
        rawWidth = bounds.outWidth,
        rawHeight = bounds.outHeight,
        reqWidth = maxDimPx,
        reqHeight = maxDimPx,
      )
      val decoded = context.contentResolver.openInputStream(uri)?.use { input ->
        android.graphics.BitmapFactory.decodeStream(
          input,
          null,
          android.graphics.BitmapFactory.Options().apply { inSampleSize = sampleSize }
        )
      } ?: return@withContext null

      val maxSide = maxOf(decoded.width, decoded.height)
      val scaled = if (maxSide > maxDimPx) {
        val ratio = maxDimPx.toFloat() / maxSide.toFloat()
        val targetW = (decoded.width * ratio).toInt().coerceAtLeast(1)
        val targetH = (decoded.height * ratio).toInt().coerceAtLeast(1)
        android.graphics.Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
      } else {
        decoded
      }

      val dir = File(context.filesDir, "custom_covers").apply { mkdirs() }
      val destFile = File(dir, "cover_${System.currentTimeMillis()}.jpg")
      try {
        destFile.outputStream().use { output ->
          scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, output)
        }
      } finally {
        if (scaled !== decoded) scaled.recycle()
        decoded.recycle()
      }
      destFile.absolutePath
    }.getOrNull()
  }

private fun shareAudioTrack(context: Context, track: AudioTrack): Boolean {
  return runCatching {
    val parsed = Uri.parse(track.uri)
    val shareUri = if (parsed.scheme == "file" && !parsed.path.isNullOrBlank()) {
      FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        File(parsed.path!!)
      )
    } else {
      parsed
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "audio/*"
      putExtra(Intent.EXTRA_STREAM, shareUri)
      putExtra(Intent.EXTRA_SUBJECT, track.title)
      putExtra(Intent.EXTRA_TEXT, "${track.title} — ${track.artist}")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(intent, "Compartir canción")
    context.startActivity(chooser)
    true
  }.getOrDefault(false)
}

@Composable
private fun BotanicalBloomBanner(
  species: BotanicalSpecies,
  onViewPlate: () -> Unit,
  onDismiss: () -> Unit,
) {
  val colors = LocalFloraColors.current
  Surface(
    shape = FloraStripShape,
    color = colors.paperCard,
    border = BorderStroke(1.2.dp, colors.borderInk),
    shadowElevation = 6.dp,
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onViewPlate)
      .testTag("botanical_bloom_banner")
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      BotanicalSpecimenIllustration(
        speciesId = species.id,
        modifier = Modifier.size(44.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          "FLORECIDA",
          style = MaterialTheme.typography.labelMedium,
          color = colors.terracotta
        )
        Text(species.commonName, style = MaterialTheme.typography.titleMedium, color = colors.ink)
        Text(
          species.scientificName,
          style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
          color = colors.metaGreen
        )
      }
      TextButton(onClick = onViewPlate) {
        Text("Ver", color = colors.terracotta, style = MaterialTheme.typography.labelLarge)
      }
      IconButton(onClick = onDismiss) {
        Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = colors.ink)
      }
    }
  }
}
