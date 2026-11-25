package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.DetectedFolderSource
import com.example.data.model.AlbumGroupSummary
import com.example.data.model.ArtistGroupSummary
import com.example.data.model.AudioTrack
import com.example.data.model.BotanicalSpeciesId
import com.example.data.model.LibraryFilterTab
import com.example.data.model.LibraryRowItem
import com.example.data.model.LibrarySortOrder
import com.example.data.model.buildLibraryCatalogData
import com.example.data.model.chSortPriority
import com.example.ui.components.AlbumArtFrame
import com.example.ui.components.FloraDashedEmptyPlate
import com.example.ui.components.FloraFlowerGlyph
import com.example.ui.components.FloraMountedPlate
import com.example.ui.components.FloraScreenHeader
import com.example.ui.theme.FloraChipShape
import com.example.ui.theme.FloraPlateShape
import com.example.ui.theme.FloraPlayerCream
import com.example.ui.theme.FloraStripShape
import com.example.ui.theme.FloraThumbShape
import com.example.ui.theme.LocalFloraColors
import com.example.ui.viewmodel.HerbariumUiState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LibraryScreen(
  uiState: HerbariumUiState,
  onSearchChange: (String) -> Unit,
  onSelectSourcePath: (String?) -> Unit,
  onPlayTrack: (List<AudioTrack>, Int) -> Unit,
  onPickRootFolderClick: () -> Unit,
  onImportDetectedFolder: (DetectedFolderSource) -> Unit,
  onToggleFavorite: (AudioTrack) -> Unit,
  onPlayNext: (AudioTrack) -> Unit,
  onAddToQueue: (AudioTrack) -> Unit,
  onAddToPlaylistClick: (AudioTrack) -> Unit,
  onInspectSpecies: (BotanicalSpeciesId) -> Unit,
  onOpenHerbarium: () -> Unit,
  modifier: Modifier = Modifier,
  searchQuery: String = uiState.searchQuery,
  selectedGenre: String? = uiState.selectedGenre,
  onSelectGenre: (String?) -> Unit = {},
  onSelectLibraryTab: (LibraryFilterTab) -> Unit = {},
  onSelectSortOrder: (LibrarySortOrder) -> Unit = {},
  onImportFilesClick: () -> Unit = {},
  hasAudioPermission: Boolean = true,
  onRequestAudioPermission: () -> Unit = {},
  onShowTrackDetails: (AudioTrack) -> Unit = {},
  onEditTrackMetadata: (AudioTrack) -> Unit = {},
  onShareTrack: (AudioTrack) -> Unit = {},
  onDeleteTrack: (AudioTrack) -> Unit = {},
  onOpenSettings: () -> Unit = {},
) {
  val colors = LocalFloraColors.current
  val currentTrackId = uiState.playbackState.currentTrack?.id
  var showSourcesDialog by remember { mutableStateOf(false) }
  var showSortMenu by remember { mutableStateOf(false) }
  var selectedAlbumName by remember { mutableStateOf<String?>(null) }
  var selectedArtistName by remember { mutableStateOf<String?>(null) }

  val activeAlbum = remember(uiState.albumGroups, selectedAlbumName) {
    selectedAlbumName?.let { name -> uiState.albumGroups.firstOrNull { it.albumName == name } }
  }
  val activeArtist = remember(uiState.artistGroups, selectedArtistName) {
    selectedArtistName?.let { name -> uiState.artistGroups.firstOrNull { it.artistName == name } }
  }

  if (activeAlbum != null && uiState.libraryTab == LibraryFilterTab.ALBUMS) {
    BackHandler { selectedAlbumName = null }
  } else if (activeArtist != null && uiState.libraryTab == LibraryFilterTab.ARTISTS) {
    BackHandler { selectedArtistName = null }
  }

  // Precomputed on Dispatchers.Default in HerbariumViewModel; fallback keeps direct test states working
  val catalog = remember(uiState.libraryCatalog, uiState.filteredTracks, uiState.librarySortOrder) {
    if (uiState.libraryCatalog.rowItems.isNotEmpty() || uiState.filteredTracks.isEmpty()) {
      uiState.libraryCatalog
    } else {
      buildLibraryCatalogData(uiState.filteredTracks, uiState.librarySortOrder)
    }
  }
  val sortedTracks = catalog.sortedTracks
  val availableLetters = catalog.availableLetters
  val rowItems = catalog.rowItems
  val letterToItemIndex = catalog.letterToItemIndex
  val sectionTrackCounts = catalog.sectionTrackCounts

  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()

  val visibleLetter by remember(rowItems) {
    derivedStateOf {
      val idx = listState.firstVisibleItemIndex
      rowItems.getOrNull(idx)?.sectionChar
    }
  }

  var scrubbedLetter by remember { mutableStateOf<Char?>(null) }
  var isScrubbing by remember { mutableStateOf(false) }
  var scrubPointerY by remember { mutableFloatStateOf(0f) }

  val activeRailLetter = if (isScrubbing) scrubbedLetter else visibleLetter

  fun resolveScrollTarget(target: Char?): Int {
    if (target == null || rowItems.isEmpty()) return 0
    letterToItemIndex[target]?.let { return it }
    val orderedAvailable = letterToItemIndex.keys.sortedBy { chSortPriority(it) * 1000 + it.code }
    val targetWeight = chSortPriority(target) * 1000 + target.code
    val nextAvailable = orderedAvailable.firstOrNull { chSortPriority(it) * 1000 + it.code >= targetWeight }
      ?: orderedAvailable.lastOrNull()
    return nextAvailable?.let { letterToItemIndex[it] } ?: 0
  }

  Column(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 18.dp, end = 18.dp, top = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      FloraScreenHeader(
        title = "Canciones",
        sealText = "M",
        onSealClick = onOpenHerbarium,
        sealContentDescription = "Herbario",
        trailingExtra = {
          IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("open_settings_button")
          ) {
            Icon(
              Icons.Filled.Settings,
              contentDescription = "Ajustes",
              tint = colors.ink
            )
          }
        }
      )

      // Search Field
      FloraMountedPlate(
        shape = FloraStripShape,
        containerColor = colors.paperCard,
        borderColor = colors.ink,
        shadowOffsetX = 2.dp,
        shadowOffsetY = 3.dp,
        shadowColor = colors.shadowPaper,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          FloraFlowerGlyph(
            modifier = Modifier.size(20.dp),
            color = colors.terracotta
          )
          Spacer(modifier = Modifier.width(10.dp))
          Box(modifier = Modifier.weight(1f)) {
            if (searchQuery.isEmpty()) {
              Text(
                text = "Buscar canción, artista o álbum",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.metaGreen
              )
            }
            BasicTextField(
              value = searchQuery,
              onValueChange = onSearchChange,
              singleLine = true,
              textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.ink),
              cursorBrush = SolidColor(colors.terracotta),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("library_search_input")
            )
          }
          if (searchQuery.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Limpiar",
              style = MaterialTheme.typography.labelLarge,
              color = colors.terracotta,
              modifier = Modifier
                .clip(FloraChipShape)
                .clickable {
                  onSearchChange("")
                  coroutineScope.launch { listState.animateScrollToItem(0) }
                }
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .testTag("clear_search_button")
            )
          }
        }
      }

      // Filter Tabs + Sort Order Menu
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(
          modifier = Modifier
            .weight(1f)
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          LibraryFilterTab.entries.forEach { tab ->
            val selected = uiState.libraryTab == tab
            Surface(
              shape = FloraChipShape,
              color = if (selected) colors.terracotta else colors.paperCard,
              border = BorderStroke(
                1.dp,
                if (selected) colors.terracotta else colors.borderInk.copy(alpha = 0.42f)
              ),
              modifier = Modifier
                .clip(FloraChipShape)
                .clickable {
                  selectedAlbumName = null
                  selectedArtistName = null
                  onSelectLibraryTab(tab)
                }
                .testTag("library_tab_${tab.name.lowercase()}")
            ) {
              Text(
                text = tab.label,
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) FloraPlayerCream else colors.ink,
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
              )
            }
          }
        }

        if (uiState.libraryTab == LibraryFilterTab.ALL || uiState.libraryTab == LibraryFilterTab.FAVORITES) {
          Box {
            Surface(
              shape = FloraChipShape,
              color = colors.paperWarm,
              border = BorderStroke(1.dp, colors.borderInk.copy(alpha = 0.5f)),
              modifier = Modifier
                .clip(FloraChipShape)
                .clickable { showSortMenu = true }
                .testTag("library_sort_button")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  Icons.AutoMirrored.Filled.Sort,
                  contentDescription = "Ordenar",
                  tint = colors.terracotta,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = uiState.librarySortOrder.label,
                  style = MaterialTheme.typography.labelSmall,
                  color = colors.ink,
                  maxLines = 1
                )
              }
            }
            DropdownMenu(
              expanded = showSortMenu,
              onDismissRequest = { showSortMenu = false }
            ) {
              LibrarySortOrder.entries.forEach { order ->
                DropdownMenuItem(
                  text = {
                    Text(
                      text = order.label,
                      fontWeight = if (order == uiState.librarySortOrder) FontWeight.Bold else FontWeight.Normal,
                      color = if (order == uiState.librarySortOrder) colors.terracotta else colors.ink
                    )
                  },
                  onClick = {
                    showSortMenu = false
                    onSelectSortOrder(order)
                  }
                )
              }
            }
          }
        }
      }

      // FUENTES & GÉNEROS
      Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "FUENTES",
            style = MaterialTheme.typography.labelMedium,
            color = colors.sage
          )
          if (uiState.detectedFolders.isNotEmpty() || uiState.audioSources.isNotEmpty() || !hasAudioPermission) {
            Text(
              text = "Ver fuentes (${maxOf(uiState.detectedFolders.size, uiState.audioSources.size)})",
              style = MaterialTheme.typography.labelLarge,
              color = colors.terracotta,
              modifier = Modifier
                .clip(FloraChipShape)
                .clickable { showSourcesDialog = true }
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .testTag("view_audio_sources_button")
            )
          }
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          val allSelected = uiState.selectedSourcePath == null
          Surface(
            shape = FloraChipShape,
            color = if (allSelected) colors.forest else colors.paperCard,
            border = BorderStroke(
              1.dp,
              if (allSelected) colors.forest else colors.borderInk.copy(alpha = 0.45f)
            ),
            modifier = Modifier
              .clip(FloraChipShape)
              .clickable { onSelectSourcePath(null) }
              .testTag("source_chip_all")
          ) {
            Text(
              text = "Todas (${uiState.tracks.size})",
              style = MaterialTheme.typography.labelLarge,
              color = if (allSelected) FloraPlayerCream else colors.ink,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
          }

          uiState.audioSources.forEach { source ->
            val selected = uiState.selectedSourcePath == source.folderPath
            Surface(
              shape = FloraChipShape,
              color = if (selected) colors.terracotta else colors.paperCard,
              border = BorderStroke(
                1.dp,
                if (selected) colors.terracotta else colors.borderInk.copy(alpha = 0.45f)
              ),
              modifier = Modifier
                .clip(FloraChipShape)
                .clickable { onSelectSourcePath(source.folderPath) }
                .testTag("source_chip_${source.folderName}")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
              ) {
                Icon(
                  Icons.Filled.FolderOpen,
                  contentDescription = null,
                  tint = if (selected) FloraPlayerCream else colors.terracotta,
                  modifier = Modifier.size(14.dp)
                )
                Text(
                  text = "${source.folderName} (${source.trackCount})",
                  style = MaterialTheme.typography.labelLarge,
                  color = if (selected) FloraPlayerCream else colors.ink,
                  maxLines = 1
                )
              }
            }
          }
        }

        if (uiState.availableGenres.size > 1) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            val noGenreSelected = selectedGenre == null
            Surface(
              shape = FloraChipShape,
              color = if (noGenreSelected) colors.cobalt else colors.paperCard,
              border = BorderStroke(
                1.dp,
                if (noGenreSelected) colors.cobalt else colors.borderInk.copy(alpha = 0.35f)
              ),
              modifier = Modifier
                .clip(FloraChipShape)
                .clickable { onSelectGenre(null) }
                .testTag("genre_chip_all")
            ) {
              Text(
                text = "Todos los géneros",
                style = MaterialTheme.typography.labelSmall,
                color = if (noGenreSelected) FloraPlayerCream else colors.ink,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
              )
            }
            uiState.availableGenres.forEach { genre ->
              val isGenreSelected = selectedGenre.equals(genre, ignoreCase = true)
              Surface(
                shape = FloraChipShape,
                color = if (isGenreSelected) colors.cobalt else colors.paperCard,
                border = BorderStroke(
                  1.dp,
                  if (isGenreSelected) colors.cobalt else colors.borderInk.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                  .clip(FloraChipShape)
                  .clickable { onSelectGenre(genre) }
                  .testTag("genre_chip_$genre")
              ) {
                Text(
                  text = genre,
                  style = MaterialTheme.typography.labelSmall,
                  color = if (isGenreSelected) FloraPlayerCream else colors.ink,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Main Content Area (Albums / Artists / Songs + Vertical A-Z Rail)
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
    ) {
      when {
        uiState.libraryTab == LibraryFilterTab.ALBUMS -> {
          if (activeAlbum != null) {
            GroupTracksDetailView(
              headerTitle = activeAlbum.albumName,
              headerSubtitle = "${activeAlbum.artistName} · ${activeAlbum.tracks.size} pistas · ${formatDuration(activeAlbum.totalDurationMs)}",
              coverTrack = activeAlbum.coverTrack,
              tracks = activeAlbum.tracks,
              currentTrackId = currentTrackId,
              isPlaying = uiState.playbackState.isPlaying,
              onBack = { selectedAlbumName = null },
              onPlayTrack = onPlayTrack,
              onToggleFavorite = onToggleFavorite,
              onPlayNext = onPlayNext,
              onAddToQueue = onAddToQueue,
              onAddToPlaylistClick = onAddToPlaylistClick,
              onInspectSpecies = onInspectSpecies,
              onShowTrackDetails = onShowTrackDetails,
              onEditTrackMetadata = onEditTrackMetadata,
              onShareTrack = onShareTrack,
              onDeleteTrack = onDeleteTrack,
            )
          } else {
            AlbumGroupListView(
              albums = uiState.albumGroups,
              onOpenAlbum = { selectedAlbumName = it.albumName },
              onPlayAlbum = { onPlayTrack(it.tracks, 0) }
            )
          }
        }

        uiState.libraryTab == LibraryFilterTab.ARTISTS -> {
          if (activeArtist != null) {
            GroupTracksDetailView(
              headerTitle = activeArtist.artistName,
              headerSubtitle = "${activeArtist.albumCount} álbumes · ${activeArtist.tracks.size} pistas · ${formatDuration(activeArtist.totalDurationMs)}",
              coverTrack = activeArtist.coverTrack,
              tracks = activeArtist.tracks,
              currentTrackId = currentTrackId,
              isPlaying = uiState.playbackState.isPlaying,
              onBack = { selectedArtistName = null },
              onPlayTrack = onPlayTrack,
              onToggleFavorite = onToggleFavorite,
              onPlayNext = onPlayNext,
              onAddToQueue = onAddToQueue,
              onAddToPlaylistClick = onAddToPlaylistClick,
              onInspectSpecies = onInspectSpecies,
              onShowTrackDetails = onShowTrackDetails,
              onEditTrackMetadata = onEditTrackMetadata,
              onShareTrack = onShareTrack,
              onDeleteTrack = onDeleteTrack,
            )
          } else {
            ArtistGroupListView(
              artists = uiState.artistGroups,
              onOpenArtist = { selectedArtistName = it.artistName },
              onPlayArtist = { onPlayTrack(it.tracks, 0) }
            )
          }
        }

        rowItems.isEmpty() -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 18.dp, vertical = 16.dp)
          ) {
            FloraDashedEmptyPlate(
              message = "Sin canciones coincidentes."
            )
          }
        }

        else -> {
          val showAlphabetRail = catalog.usesAlphabetSections
          LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
              start = 18.dp,
              end = if (showAlphabetRail) 42.dp else 18.dp,
              top = 6.dp,
              bottom = 112.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(
              items = rowItems,
              key = { it.key }
            ) { item ->
              when (item) {
                is LibraryRowItem.SectionHeader -> {
                  AlphabetSectionDivider(
                    letter = item.sectionChar,
                    trackCount = item.count
                  )
                }
                is LibraryRowItem.TrackRow -> {
                  val track = item.track
                  val isPlayingTrack = track.id == currentTrackId
                  TrackRowCard(
                    catalogNumber = item.flatIndex + 1,
                    track = track,
                    isCurrent = isPlayingTrack,
                    isPlaying = isPlayingTrack && uiState.playbackState.isPlaying,
                    accentColor = colors.terracotta,
                    onClick = { onPlayTrack(sortedTracks, item.flatIndex) },
                    onToggleFavorite = { onToggleFavorite(track) },
                    onPlayNext = { onPlayNext(track) },
                    onAddToQueue = { onAddToQueue(track) },
                    onAddToPlaylist = { onAddToPlaylistClick(track) },
                    onInspectSpecies = { onInspectSpecies(track.speciesId) },
                    onShowDetails = { onShowTrackDetails(track) },
                    onEditMetadata = { onEditTrackMetadata(track) },
                    onShare = { onShareTrack(track) },
                    onDelete = { onDeleteTrack(track) }
                  )
                }
              }
            }
          }

          if (showAlphabetRail) {
            VerticalAlphabetIndexRail(
              activeLetter = activeRailLetter,
              availableLetters = availableLetters,
              isScrubbing = isScrubbing,
              onScrubStartOrMove = { letter, yPosPx, isTap ->
                isScrubbing = true
                scrubPointerY = yPosPx
                if (scrubbedLetter != letter || isTap) {
                  scrubbedLetter = letter
                  val targetIndex = resolveScrollTarget(letter)
                  coroutineScope.launch {
                    if (isTap) {
                      listState.animateScrollToItem(targetIndex)
                    } else {
                      listState.scrollToItem(targetIndex)
                    }
                  }
                }
              },
              onScrubEnd = {
                isScrubbing = false
              },
              modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = 4.dp, bottom = 14.dp, end = 6.dp)
            )

            // Floating Botanical Scrubber Bubble beside finger
            val density = LocalDensity.current
            androidx.compose.animation.AnimatedVisibility(
              visible = isScrubbing,
              enter = fadeIn() + scaleIn(initialScale = 0.85f),
              exit = fadeOut() + scaleOut(targetScale = 0.85f),
              modifier = Modifier
                .align(Alignment.TopEnd)
                .offset {
                  val bubbleHalfPx = with(density) { 26.dp.roundToPx() }
                  val endOffsetPx = with(density) { (-46).dp.roundToPx() }
                  IntOffset(
                    x = endOffsetPx,
                    y = (scrubPointerY.roundToInt() - bubbleHalfPx).coerceAtLeast(0)
                  )
                }
            ) {
              val displayChar = scrubbedLetter ?: '•'
              val count = if (scrubbedLetter == null) sortedTracks.size else (sectionTrackCounts[displayChar] ?: 0)
              Surface(
                shape = FloraPlateShape,
                color = colors.forest,
                border = BorderStroke(1.2.dp, colors.terracotta),
                shadowElevation = 6.dp,
                modifier = Modifier.testTag("alphabet_scrub_bubble")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Text(
                    text = displayChar.toString(),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = FloraPlayerCream
                  )
                  Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelLarge,
                    color = FloraPlayerCream.copy(alpha = 0.78f)
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (showSourcesDialog) {
    AlertDialog(
      onDismissRequest = { showSourcesDialog = false },
      containerColor = colors.paper,
      title = {
        Text(
          text = "Fuentes de fonoteca",
          style = MaterialTheme.typography.headlineMedium,
          color = colors.ink
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          if (!hasAudioPermission) {
            Surface(
              shape = FloraStripShape,
              color = colors.paperWarm,
              border = BorderStroke(1.dp, colors.terracotta),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(Icons.Filled.Security, contentDescription = null, tint = colors.terracotta, modifier = Modifier.size(16.dp))
                  Text("Permiso de audio pendiente", style = MaterialTheme.typography.titleSmall, color = colors.ink)
                }
                Text(
                  "Autoriza el acceso al audio para detectar tus carpetas automáticamente.",
                  style = MaterialTheme.typography.bodySmall,
                  color = colors.metaGreen
                )
                Button(
                  onClick = {
                    showSourcesDialog = false
                    onRequestAudioPermission()
                  },
                  shape = FloraStripShape,
                  colors = ButtonDefaults.buttonColors(containerColor = colors.cobalt, contentColor = FloraPlayerCream)
                ) {
                  Text("Conceder permiso", style = MaterialTheme.typography.labelLarge)
                }
              }
            }
          }

          if (uiState.detectedFolders.isNotEmpty()) {
            uiState.detectedFolders.forEach { folder ->
              val isWinner = folder.folderKey == uiState.dominantDetectedFolder?.folderKey
              Surface(
                shape = FloraStripShape,
                color = if (isWinner) colors.paperWarm else colors.paperCard,
                border = BorderStroke(
                  width = if (isWinner) 1.4.dp else 1.dp,
                  color = if (isWinner) colors.terracotta else colors.borderInk.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(FloraStripShape)
                  .clickable {
                    onImportDetectedFolder(folder)
                    showSourcesDialog = false
                  }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = folder.folderName,
                      style = MaterialTheme.typography.titleSmall,
                      color = colors.ink,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = "${folder.qualifyingCount} pistas > ${uiState.effectsState.minScanDurationSec}s · ${folder.totalAudioScanned} totales",
                      style = MaterialTheme.typography.bodySmall,
                      color = colors.metaGreen
                    )
                  }
                  Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Importar ${folder.folderName}",
                    tint = colors.terracotta
                  )
                }
              }
            }
          } else {
            uiState.audioSources.forEach { src ->
              Surface(
                shape = FloraStripShape,
                color = colors.paperCard,
                border = BorderStroke(1.dp, colors.borderInk.copy(alpha = 0.4f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(FloraStripShape)
                  .clickable {
                    onSelectSourcePath(src.folderPath)
                    showSourcesDialog = false
                  }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(src.folderName, style = MaterialTheme.typography.titleSmall, color = colors.ink)
                    Text(
                      text = "${src.trackCount} canciones · ${src.folderPath}",
                      style = MaterialTheme.typography.bodySmall,
                      color = colors.metaGreen,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = {
                showSourcesDialog = false
                onPickRootFolderClick()
              },
              shape = FloraStripShape,
              border = BorderStroke(1.dp, colors.borderInk),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.terracotta)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Carpeta", style = MaterialTheme.typography.labelLarge, color = colors.ink)
            }
            OutlinedButton(
              onClick = {
                showSourcesDialog = false
                onImportFilesClick()
              },
              shape = FloraStripShape,
              border = BorderStroke(1.dp, colors.borderInk),
              modifier = Modifier
                .weight(1f)
                .testTag("sources_dialog_import_files_button")
            ) {
              Icon(Icons.Filled.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.cobalt)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Archivos", style = MaterialTheme.typography.labelLarge, color = colors.ink)
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showSourcesDialog = false }) {
          Text("Cerrar", color = colors.ink)
        }
      }
    )
  }
}

@Composable
private fun AlbumGroupListView(
  albums: List<AlbumGroupSummary>,
  onOpenAlbum: (AlbumGroupSummary) -> Unit,
  onPlayAlbum: (AlbumGroupSummary) -> Unit,
) {
  val colors = LocalFloraColors.current
  if (albums.isEmpty()) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
      FloraDashedEmptyPlate(message = "Sin álbumes en esta selección.")
    }
    return
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 112.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    items(albums, key = { it.albumName }) { album ->
      FloraMountedPlate(
        shape = FloraStripShape,
        containerColor = colors.paperCard,
        borderColor = colors.borderInk.copy(alpha = 0.45f),
        shadowOffsetX = 2.dp,
        shadowOffsetY = 3.dp,
        shadowColor = colors.shadowPaper,
        onClick = { onOpenAlbum(album) },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("album_card_${album.albumName}")
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          AlbumArtFrame(
            track = album.coverTrack,
            customShape = FloraThumbShape,
            modifier = Modifier.size(54.dp)
          )
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
              Icon(Icons.Filled.Album, contentDescription = null, tint = colors.terracotta, modifier = Modifier.size(15.dp))
              Text(
                text = album.albumName,
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Text(
              text = "${album.artistName} · ${album.tracks.size} canciones · ${formatDuration(album.totalDurationMs)}",
              style = MaterialTheme.typography.bodySmall,
              color = colors.metaGreen,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          IconButton(
            onClick = { onPlayAlbum(album) },
            modifier = Modifier.minimumInteractiveComponentSize()
          ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Reproducir álbum", tint = colors.terracotta)
          }
        }
      }
    }
  }
}

@Composable
private fun ArtistGroupListView(
  artists: List<ArtistGroupSummary>,
  onOpenArtist: (ArtistGroupSummary) -> Unit,
  onPlayArtist: (ArtistGroupSummary) -> Unit,
) {
  val colors = LocalFloraColors.current
  if (artists.isEmpty()) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
      FloraDashedEmptyPlate(message = "Sin artistas en esta selección.")
    }
    return
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 112.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    items(artists, key = { it.artistName }) { artist ->
      FloraMountedPlate(
        shape = FloraStripShape,
        containerColor = colors.paperCard,
        borderColor = colors.borderInk.copy(alpha = 0.45f),
        shadowOffsetX = 2.dp,
        shadowOffsetY = 3.dp,
        shadowColor = colors.shadowPaper,
        onClick = { onOpenArtist(artist) },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("artist_card_${artist.artistName}")
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          AlbumArtFrame(
            track = artist.coverTrack,
            customShape = CircleShape,
            modifier = Modifier.size(54.dp)
          )
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
              Icon(Icons.Filled.Person, contentDescription = null, tint = colors.cobalt, modifier = Modifier.size(15.dp))
              Text(
                text = artist.artistName,
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Text(
              text = "${artist.albumCount} álbumes · ${artist.tracks.size} canciones · ${formatDuration(artist.totalDurationMs)}",
              style = MaterialTheme.typography.bodySmall,
              color = colors.metaGreen,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          IconButton(
            onClick = { onPlayArtist(artist) },
            modifier = Modifier.minimumInteractiveComponentSize()
          ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Reproducir artista", tint = colors.terracotta)
          }
        }
      }
    }
  }
}

@Composable
private fun GroupTracksDetailView(
  headerTitle: String,
  headerSubtitle: String,
  coverTrack: AudioTrack,
  tracks: List<AudioTrack>,
  currentTrackId: Long?,
  isPlaying: Boolean,
  onBack: () -> Unit,
  onPlayTrack: (List<AudioTrack>, Int) -> Unit,
  onToggleFavorite: (AudioTrack) -> Unit,
  onPlayNext: (AudioTrack) -> Unit,
  onAddToQueue: (AudioTrack) -> Unit,
  onAddToPlaylistClick: (AudioTrack) -> Unit,
  onInspectSpecies: (BotanicalSpeciesId) -> Unit,
  onShowTrackDetails: (AudioTrack) -> Unit,
  onEditTrackMetadata: (AudioTrack) -> Unit,
  onShareTrack: (AudioTrack) -> Unit,
  onDeleteTrack: (AudioTrack) -> Unit,
) {
  val colors = LocalFloraColors.current
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 112.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    item {
      FloraMountedPlate(
        shape = FloraPlateShape,
        containerColor = colors.paperCard,
        borderColor = colors.borderInk.copy(alpha = 0.6f),
        shadowOffsetX = 3.dp,
        shadowOffsetY = 4.dp,
        shadowColor = colors.shadowPaper,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            IconButton(
              onClick = onBack,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("group_detail_back_button")
            ) {
              Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Volver", tint = colors.ink)
            }
            AlbumArtFrame(
              track = coverTrack,
              customShape = FloraThumbShape,
              modifier = Modifier.size(56.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = headerTitle,
                style = MaterialTheme.typography.titleLarge,
                color = colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = headerSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.metaGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = { if (tracks.isNotEmpty()) onPlayTrack(tracks, 0) },
              shape = FloraStripShape,
              colors = ButtonDefaults.buttonColors(containerColor = colors.cobalt, contentColor = FloraPlayerCream),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Filled.PlayArrow, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Reproducir", style = MaterialTheme.typography.labelLarge)
            }
            OutlinedButton(
              onClick = { if (tracks.isNotEmpty()) onPlayTrack(tracks.shuffled(), 0) },
              shape = FloraStripShape,
              border = BorderStroke(1.dp, colors.borderInk),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Filled.Shuffle, contentDescription = null, tint = colors.terracotta)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Aleatorio", style = MaterialTheme.typography.labelLarge, color = colors.ink)
            }
          }
        }
      }
    }

    itemsIndexed(tracks, key = { _, t -> "grp_${t.id}" }) { idx, track ->
      val isPlayingTrack = track.id == currentTrackId
      TrackRowCard(
        catalogNumber = idx + 1,
        track = track,
        isCurrent = isPlayingTrack,
        isPlaying = isPlayingTrack && isPlaying,
        accentColor = colors.terracotta,
        onClick = { onPlayTrack(tracks, idx) },
        onToggleFavorite = { onToggleFavorite(track) },
        onPlayNext = { onPlayNext(track) },
        onAddToQueue = { onAddToQueue(track) },
        onAddToPlaylist = { onAddToPlaylistClick(track) },
        onInspectSpecies = { onInspectSpecies(track.speciesId) },
        onShowDetails = { onShowTrackDetails(track) },
        onEditMetadata = { onEditTrackMetadata(track) },
        onShare = { onShareTrack(track) },
        onDelete = { onDeleteTrack(track) }
      )
    }
  }
}

@Composable
private fun AlphabetSectionDivider(
  letter: Char,
  trackCount: Int,
) {
  val colors = LocalFloraColors.current
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 6.dp, bottom = 2.dp)
      .testTag("section_header_$letter"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Surface(
      shape = FloraChipShape,
      color = colors.paperWarm,
      border = BorderStroke(1.dp, colors.borderInk.copy(alpha = 0.35f))
    ) {
      Text(
        text = letter.toString(),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = colors.terracotta,
        modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp)
      )
    }
    HorizontalDivider(
      modifier = Modifier.weight(1f),
      thickness = 1.dp,
      color = colors.borderInk.copy(alpha = 0.22f)
    )
    Text(
      text = "$trackCount",
      style = MaterialTheme.typography.labelSmall,
      color = colors.metaGreen
    )
  }
}

@Composable
private fun VerticalAlphabetIndexRail(
  activeLetter: Char?,
  availableLetters: Set<Char>,
  isScrubbing: Boolean,
  onScrubStartOrMove: (letter: Char?, yPosPx: Float, isTap: Boolean) -> Unit,
  onScrubEnd: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = LocalFloraColors.current
  val railItems: List<Char?> = remember(availableLetters) {
    buildList {
      add(null) // Top jump ("Todo")
      ('A'..'Z').forEach { add(it) }
      if ('#' in availableLetters) add('#')
    }
  }
  val haptic = LocalHapticFeedback.current
  var lastEmittedIdx by remember { mutableStateOf(-1) }

  BoxWithConstraints(modifier = modifier.width(28.dp)) {
    val compactRail = maxHeight < 430.dp
    val letterFontSize = if (compactRail) 9.sp else 10.5.sp

    Surface(
      shape = RoundedCornerShape(14.dp),
      color = if (isScrubbing) colors.paperWarm.copy(alpha = 0.96f) else colors.paperCard.copy(alpha = 0.86f),
      border = BorderStroke(
        width = if (isScrubbing) 1.2.dp else 1.dp,
        color = if (isScrubbing) colors.terracotta.copy(alpha = 0.65f) else colors.borderInk.copy(alpha = 0.28f)
      ),
      modifier = Modifier
        .fillMaxSize()
        .testTag("vertical_alphabet_index")
        .pointerInput(railItems) {
          awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val totalHeight = size.height.coerceAtLeast(1)
            val startIdx = ((down.position.y / totalHeight) * railItems.size)
              .toInt()
              .coerceIn(0, railItems.lastIndex)
            lastEmittedIdx = startIdx
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onScrubStartOrMove(railItems[startIdx], down.position.y.coerceIn(0f, totalHeight.toFloat()), true)

            var pointerId = down.id
            while (true) {
              val event = awaitPointerEvent()
              val change = event.changes.firstOrNull { it.id == pointerId } ?: event.changes.firstOrNull()
              if (change == null || !change.pressed) {
                break
              }
              pointerId = change.id
              change.consume()
              val clampedY = change.position.y.coerceIn(0f, totalHeight.toFloat())
              val idx = ((clampedY / totalHeight) * railItems.size)
                .toInt()
                .coerceIn(0, railItems.lastIndex)
              if (idx != lastEmittedIdx) {
                lastEmittedIdx = idx
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              }
              onScrubStartOrMove(railItems[idx], clampedY, false)
            }
            lastEmittedIdx = -1
            onScrubEnd()
          }
        }
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
      ) {
        railItems.forEach { ch ->
          val isAll = ch == null
          val isAvailable = isAll || ch in availableLetters
          val isSelected = activeLetter == ch && (!isAll || isScrubbing)

          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .padding(horizontal = 3.dp)
              .clip(CircleShape)
              .background(if (isSelected) colors.terracotta else Color.Transparent)
              .alpha(if (isAvailable || isSelected) 1f else 0.34f)
              .testTag(if (isAll) "alpha_all" else "alpha_$ch"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = if (isAll) "•" else ch.toString(),
              fontSize = letterFontSize,
              lineHeight = letterFontSize,
              fontWeight = if (isSelected || isAvailable) FontWeight.Bold else FontWeight.Medium,
              color = when {
                isSelected -> FloraPlayerCream
                isAvailable -> colors.ink
                else -> colors.metaGreen
              },
              maxLines = 1
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TrackRowCard(
  catalogNumber: Int,
  track: AudioTrack,
  isCurrent: Boolean,
  isPlaying: Boolean,
  accentColor: Color,
  onClick: () -> Unit,
  onToggleFavorite: () -> Unit,
  onPlayNext: () -> Unit,
  onAddToQueue: () -> Unit,
  onAddToPlaylist: () -> Unit,
  onInspectSpecies: () -> Unit,
  onShowDetails: () -> Unit,
  onEditMetadata: () -> Unit,
  onShare: () -> Unit,
  onDelete: () -> Unit,
) {
  val colors = LocalFloraColors.current
  var menuExpanded by remember { mutableStateOf(false) }
  val bgColor = if (isCurrent) colors.currentHighlight else colors.paperCard

  Surface(
    shape = FloraStripShape,
    color = bgColor,
    border = BorderStroke(
      width = if (isCurrent) 1.3.dp else 1.dp,
      color = if (isCurrent) accentColor else colors.borderInk.copy(alpha = 0.42f)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clip(FloraStripShape)
      .clickable(onClick = onClick)
      .testTag("track_card_${track.id}")
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "%02d".format(catalogNumber),
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = accentColor,
        modifier = Modifier.width(26.dp)
      )

      Box(contentAlignment = Alignment.Center) {
        AlbumArtFrame(
          track = track,
          customShape = FloraThumbShape,
          modifier = Modifier.size(48.dp)
        )
        if (isPlaying) {
          Box(
            modifier = Modifier
              .size(22.dp)
              .clip(CircleShape)
              .background(accentColor.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              Icons.Filled.GraphicEq,
              contentDescription = "Reproduciendo",
              tint = FloraPlayerCream,
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = track.title,
          style = MaterialTheme.typography.titleMedium,
          color = colors.ink,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = if (track.isBundled) {
            "${track.artist} · ${formatDuration(track.durationMs)}"
          } else {
            "${track.artist} · ${track.sourceFolder} · ${formatDuration(track.durationMs)}"
          },
          style = MaterialTheme.typography.bodySmall,
          color = colors.trackMeta,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      IconButton(
        onClick = onToggleFavorite,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("fav_button_${track.id}")
      ) {
        Icon(
          imageVector = if (track.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
          contentDescription = if (track.isFavorite) "Quitar de favoritos" else "Favorito",
          tint = if (track.isFavorite) accentColor else colors.metaGreen
        )
      }

      Box {
        IconButton(
          onClick = { menuExpanded = true },
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("track_menu_${track.id}")
        ) {
          Icon(Icons.Filled.MoreVert, contentDescription = "Opciones", tint = colors.ink)
        }
        DropdownMenu(
          expanded = menuExpanded,
          onDismissRequest = { menuExpanded = false }
        ) {
          DropdownMenuItem(
            text = { Text("Reproducir siguiente") },
            leadingIcon = { Icon(Icons.Filled.QueuePlayNext, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onPlayNext()
            }
          )
          DropdownMenuItem(
            text = { Text("Añadir a la cola") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onAddToQueue()
            }
          )
          DropdownMenuItem(
            text = { Text("Añadir a playlist") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onAddToPlaylist()
            }
          )
          DropdownMenuItem(
            text = { Text("Ver planta") },
            leadingIcon = { Icon(Icons.Filled.LocalFlorist, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onInspectSpecies()
            }
          )
          HorizontalDivider()
          DropdownMenuItem(
            text = { Text("Ficha técnica") },
            leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onShowDetails()
            }
          )
          DropdownMenuItem(
            text = { Text("Editar etiquetas") },
            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onEditMetadata()
            }
          )
          DropdownMenuItem(
            text = { Text("Compartir") },
            leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onShare()
            }
          )
          DropdownMenuItem(
            text = { Text("Eliminar", color = colors.terracotta) },
            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = colors.terracotta) },
            onClick = {
              menuExpanded = false
              onDelete()
            }
          )
        }
      }
    }
  }
}

fun formatDuration(ms: Long): String {
  val totalSec = (ms / 1000L).coerceAtLeast(0L)
  val min = totalSec / 60L
  val sec = totalSec % 60L
  return "%d:%02d".format(min, sec)
}
