package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.ui.components.AddToPlaylistSheet
import com.example.ui.components.BotanicalSpecimenIllustration
import com.example.ui.components.CreateOrEditPlaylistDialog
import com.example.ui.components.EqualizerOverlayScreen
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.ParchmentBackground
import com.example.ui.components.UpNextQueueSheet
import com.example.ui.screens.FloracionScreen
import com.example.ui.screens.HerbariumScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.theme.FloraBorderInk
import com.example.ui.theme.FloraInk
import com.example.ui.theme.FloraMetaGreen
import com.example.ui.theme.FloraNavIdle
import com.example.ui.theme.FloraPaper
import com.example.ui.theme.FloraPaperCard
import com.example.ui.theme.FloraSageLight
import com.example.ui.theme.FloraStripShape
import com.example.ui.theme.FloraTerracotta
import com.example.ui.theme.HerbariumTheme
import com.example.ui.viewmodel.HerbariumViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.Serializable

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

  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDest = backStackEntry?.destination

  var showNowPlaying by rememberSaveable { mutableStateOf(false) }
  var showEqualizerOverlay by rememberSaveable { mutableStateOf(false) }
  var showHerbariumOverlay by rememberSaveable { mutableStateOf(false) }
  var showQueueSheet by rememberSaveable { mutableStateOf(false) }
  var selectedPlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }
  var inspectedSpeciesId by remember { mutableStateOf<BotanicalSpeciesId?>(null) }
  var trackPendingPlaylist by remember { mutableStateOf<AudioTrack?>(null) }
  var showCreatePlaylistModal by rememberSaveable { mutableStateOf(false) }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { }
  )
  LaunchedEffect(Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
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

  LaunchedEffect(Unit) {
    viewModel.uiMessages.collectLatest { message ->
      snackbarHostState.showSnackbar(message)
    }
  }

  HerbariumTheme(
    albumPalette = uiState.albumPalette,
    themeGradients = uiState.themeGradients,
    chameleonEnabled = uiState.effectsState.chameleonThemeEnabled,
  ) {
    val navigateToTab: (Any) -> Unit = { route ->
      showHerbariumOverlay = false
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
      showHerbariumOverlay = true
    }

    val openHerbariumAtlas: () -> Unit = {
      inspectedSpeciesId = null
      showHerbariumOverlay = true
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
              if (!isExpanded && !showNowPlaying && !showEqualizerOverlay && !showHerbariumOverlay) {
                Column(modifier = Modifier.fillMaxWidth()) {
                  AnimatedVisibility(
                    visible = uiState.playbackState.currentTrack != null,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                  ) {
                    MiniPlayerBar(
                      playbackState = uiState.playbackState,
                      onTogglePlayPause = viewModel::togglePlayPause,
                      onPrevious = viewModel::previous,
                      onNext = viewModel::next,
                      onToggleFavorite = {
                        uiState.playbackState.currentTrack?.let(viewModel::toggleFavorite)
                      },
                      onOpenNowPlaying = { showNowPlaying = true }
                    )
                  }

                  HorizontalDivider(thickness = 1.dp, color = FloraBorderInk.copy(alpha = 0.22f))
                  NavigationBar(
                    containerColor = FloraPaper.copy(alpha = 0.98f),
                    tonalElevation = 0.dp
                  ) {
                    val isHome = currentDest?.hasRoute<FloracionRoute>() == true || currentDest == null
                    val isExplore = currentDest?.hasRoute<LibraryRoute>() == true
                    val isCollection = currentDest?.hasRoute<PlaylistsRoute>() == true

                    val navColors = NavigationBarItemDefaults.colors(
                      selectedIconColor = FloraInk,
                      selectedTextColor = FloraInk,
                      unselectedIconColor = FloraNavIdle,
                      unselectedTextColor = FloraNavIdle,
                      indicatorColor = FloraSageLight
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
                  containerColor = FloraPaper.copy(alpha = 0.96f),
                  modifier = Modifier.fillMaxHeight()
                ) {
                  val isHome = currentDest?.hasRoute<FloracionRoute>() == true || currentDest == null
                  val isExplore = currentDest?.hasRoute<LibraryRoute>() == true
                  val isCollection = currentDest?.hasRoute<PlaylistsRoute>() == true

                  val railColors = NavigationRailItemDefaults.colors(
                    selectedIconColor = FloraInk,
                    selectedTextColor = FloraInk,
                    unselectedIconColor = FloraNavIdle,
                    unselectedTextColor = FloraNavIdle,
                    indicatorColor = FloraSageLight
                  )

                  Spacer(modifier = Modifier.weight(0.2f))
                  NavigationRailItem(
                    selected = !showHerbariumOverlay && isHome,
                    onClick = { navigateToTab(FloracionRoute) },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Floración") },
                    label = { Text("Floración") },
                    colors = railColors
                  )
                  NavigationRailItem(
                    selected = !showHerbariumOverlay && isExplore,
                    onClick = { navigateToTab(LibraryRoute) },
                    icon = { Icon(Icons.Filled.MusicNote, contentDescription = "Canciones") },
                    label = { Text("Canciones") },
                    colors = railColors
                  )
                  NavigationRailItem(
                    selected = !showHerbariumOverlay && isCollection,
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
                  Spacer(modifier = Modifier.weight(1f))
                }
              }

              Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (uiState.isLoading) {
                  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FloraTerracotta)
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
                        onOpenHerbarium = openHerbariumAtlas
                      )
                    }

                    composable<LibraryRoute> {
                      LibraryScreen(
                        uiState = uiState,
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
                        onOpenHerbarium = openHerbariumAtlas
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
                        onOpenHerbarium = openHerbariumAtlas
                      )
                    }
                  }
                }

                if (isExpanded && uiState.playbackState.currentTrack != null && !showNowPlaying && !showEqualizerOverlay && !showHerbariumOverlay) {
                  MiniPlayerBar(
                    playbackState = uiState.playbackState,
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
        visible = showNowPlaying,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
      ) {
        NowPlayingScreen(
          uiState = uiState,
          audioSpectrumFlow = viewModel.audioSpectrum,
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
          onSelectPlayerTheme = viewModel::selectPlayerTheme
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
  }
}

@Composable
private fun BotanicalBloomBanner(
  species: BotanicalSpecies,
  onViewPlate: () -> Unit,
  onDismiss: () -> Unit,
) {
  Surface(
    shape = FloraStripShape,
    color = FloraPaperCard,
    border = BorderStroke(1.2.dp, FloraBorderInk),
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
          color = FloraTerracotta
        )
        Text(species.commonName, style = MaterialTheme.typography.titleMedium, color = FloraInk)
        Text(
          species.scientificName,
          style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
          color = FloraMetaGreen
        )
      }
      TextButton(onClick = onViewPlate) {
        Text("Ver", color = FloraTerracotta, style = MaterialTheme.typography.labelLarge)
      }
      IconButton(onClick = onDismiss) {
        Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = FloraInk)
      }
    }
  }
}
