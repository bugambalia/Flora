package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.model.AudioTrack
import com.example.data.model.BotanicalCatalog
import com.example.data.model.BotanicalSpeciesId
import com.example.data.model.PlaylistWithTracks
import com.example.ui.components.AlbumArtFrame
import com.example.ui.components.CreateOrEditPlaylistDialog
import com.example.ui.components.FloraDashedEmptyPlate
import com.example.ui.components.FloraFlowerGlyph
import com.example.ui.components.FloraMountedPlate
import com.example.ui.components.FloraScreenHeader
import com.example.ui.theme.FloraChipShape
import com.example.ui.theme.FloraForest
import com.example.ui.theme.FloraGold
import com.example.ui.theme.FloraHeroShape
import com.example.ui.theme.FloraPlayerCream
import com.example.ui.theme.FloraStripShape
import com.example.ui.theme.FloraThumbShape
import com.example.ui.theme.LocalFloraColors
import com.example.ui.viewmodel.HerbariumUiState
import kotlin.math.roundToInt

private val OliveMatting = Color(0xFF6B7552)

@Composable
fun PlaylistsScreen(
  uiState: HerbariumUiState,
  selectedPlaylistId: Long?,
  onSelectPlaylist: (Long?) -> Unit,
  onPlayPlaylist: (PlaylistWithTracks, Int) -> Unit,
  onShufflePlaylist: (PlaylistWithTracks) -> Unit,
  onCreatePlaylist: (String, String, BotanicalSpeciesId, List<Long>) -> Unit,
  onUpdatePlaylist: (Long, String, String, BotanicalSpeciesId) -> Unit,
  onDeletePlaylist: (Long, String) -> Unit,
  onAddTrackToPlaylist: (Long, String, AudioTrack) -> Unit,
  onRemoveTrackFromPlaylist: (Long, Long) -> Unit,
  onSavePlaylistOrder: (Long, List<AudioTrack>) -> Unit,
  onOpenHerbarium: () -> Unit,
  modifier: Modifier = Modifier,
  onDuplicatePlaylist: (PlaylistWithTracks) -> Unit = {},
  onExportPlaylist: (PlaylistWithTracks, String) -> Unit = { _, _ -> },
  onImportPlaylistClick: () -> Unit = {},
) {
  val colors = LocalFloraColors.current
  var showCreateDialog by remember { mutableStateOf(false) }
  var editingPlaylist by remember { mutableStateOf<PlaylistWithTracks?>(null) }
  var deletingPlaylist by remember { mutableStateOf<PlaylistWithTracks?>(null) }
  var playlistSearchQuery by remember { mutableStateOf("") }

  val selectedPlaylist = uiState.playlists.firstOrNull { it.playlist.id == selectedPlaylistId }
  val currentPlayingTrackId = uiState.playbackState.currentTrack?.id

  val filteredPlaylists = remember(uiState.playlists, playlistSearchQuery) {
    val q = playlistSearchQuery.trim()
    if (q.isEmpty()) {
      uiState.playlists
    } else {
      uiState.playlists.filter { item ->
        item.playlist.name.contains(q, ignoreCase = true) ||
          item.playlist.description.contains(q, ignoreCase = true) ||
          item.tracks.any { t ->
            t.title.contains(q, ignoreCase = true) || t.artist.contains(q, ignoreCase = true)
          }
      }
    }
  }

  if (selectedPlaylist != null) {
    BackHandler { onSelectPlaylist(null) }
    PlaylistDetailView(
      playlistWithTracks = selectedPlaylist,
      allLibraryTracks = uiState.tracks,
      currentPlayingTrackId = currentPlayingTrackId,
      onBack = { onSelectPlaylist(null) },
      onPlayFromIndex = { idx -> onPlayPlaylist(selectedPlaylist, idx) },
      onShuffle = { onShufflePlaylist(selectedPlaylist) },
      onEditMetadata = { editingPlaylist = selectedPlaylist },
      onDuplicate = { onDuplicatePlaylist(selectedPlaylist) },
      onExportFormat = { fmt -> onExportPlaylist(selectedPlaylist, fmt) },
      onDelete = { deletingPlaylist = selectedPlaylist },
      onAddTrack = { track ->
        onAddTrackToPlaylist(selectedPlaylist.playlist.id, selectedPlaylist.playlist.name, track)
      },
      onRemoveTrack = { track ->
        onRemoveTrackFromPlaylist(selectedPlaylist.playlist.id, track.id)
      },
      onSaveOrder = { ordered ->
        onSavePlaylistOrder(selectedPlaylist.playlist.id, ordered)
      },
      modifier = modifier
    )
  } else {
    LazyColumn(
      modifier = modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 156.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {
        FloraScreenHeader(
          title = "Playlists",
          sealText = "M",
          onSealClick = onOpenHerbarium,
          sealContentDescription = "Herbario",
          trailingExtra = {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              IconButton(
                onClick = onImportPlaylistClick,
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("import_playlist_button")
              ) {
                Icon(
                  Icons.Filled.FileUpload,
                  contentDescription = "Importar playlist",
                  tint = colors.terracotta
                )
              }
              OutlinedButton(
                onClick = { showCreateDialog = true },
                shape = FloraStripShape,
                border = BorderStroke(1.dp, colors.borderInk),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("create_playlist_button")
              ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.ink)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nueva", style = MaterialTheme.typography.labelLarge, color = colors.ink)
              }
            }
          }
        )
      }

      if (uiState.playlists.isNotEmpty()) {
        item {
          PlaylistSearchField(
            query = playlistSearchQuery,
            onQueryChange = { playlistSearchQuery = it },
            placeholder = "Buscar colección o pista incluida",
            testTag = "playlist_search_input"
          )
        }
      }

      if (filteredPlaylists.isEmpty()) {
        item {
          FloraDashedEmptyPlate(
            message = if (playlistSearchQuery.isBlank()) "Sin colecciones aún." else "Ninguna colección coincide con «$playlistSearchQuery»."
          )
        }
      } else {
        itemsIndexed(
          items = filteredPlaylists,
          key = { _, item -> item.playlist.id }
        ) { idx, item ->
          FloraCollectionEditorialBlock(
            folioNumber = idx + 1,
            item = item,
            currentPlayingTrackId = currentPlayingTrackId,
            onOpen = { onSelectPlaylist(item.playlist.id) },
            onPlayTrackAt = { trackIdx -> onPlayPlaylist(item, trackIdx) }
          )
        }
      }
    }
  }

  if (showCreateDialog) {
    CreateOrEditPlaylistDialog(
      onConfirm = { name, desc, motif ->
        onCreatePlaylist(name, desc, motif, emptyList())
        showCreateDialog = false
      },
      onDismiss = { showCreateDialog = false }
    )
  }

  editingPlaylist?.let { target ->
    CreateOrEditPlaylistDialog(
      initialName = target.playlist.name,
      initialDescription = target.playlist.description,
      initialMotif = target.playlist.speciesMotif,
      isEditing = true,
      onConfirm = { name, desc, motif ->
        onUpdatePlaylist(target.playlist.id, name, desc, motif)
        editingPlaylist = null
      },
      onDismiss = { editingPlaylist = null }
    )
  }

  deletingPlaylist?.let { target ->
    AlertDialog(
      onDismissRequest = { deletingPlaylist = null },
      containerColor = colors.paper,
      title = { Text("Eliminar colección", style = MaterialTheme.typography.headlineMedium, color = colors.ink) },
      text = {
        Text(
          "¿Eliminar «${target.playlist.name}»?",
          style = MaterialTheme.typography.bodyMedium,
          color = colors.ink
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (selectedPlaylistId == target.playlist.id) onSelectPlaylist(null)
            onDeletePlaylist(target.playlist.id, target.playlist.name)
            deletingPlaylist = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = colors.terracotta, contentColor = FloraPlayerCream)
        ) { Text("Eliminar") }
      },
      dismissButton = {
        TextButton(onClick = { deletingPlaylist = null }) { Text("Cancelar", color = colors.ink) }
      }
    )
  }
}

@Composable
private fun PlaylistSearchField(
  query: String,
  onQueryChange: (String) -> Unit,
  placeholder: String,
  testTag: String,
) {
  val colors = LocalFloraColors.current
  FloraMountedPlate(
    shape = FloraStripShape,
    containerColor = colors.paperCard,
    borderColor = colors.borderInk,
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
      FloraFlowerGlyph(modifier = Modifier.size(18.dp), color = colors.terracotta)
      Spacer(modifier = Modifier.width(10.dp))
      Box(modifier = Modifier.weight(1f)) {
        if (query.isEmpty()) {
          Text(
            text = placeholder,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.metaGreen
          )
        }
        BasicTextField(
          value = query,
          onValueChange = onQueryChange,
          singleLine = true,
          textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.ink),
          cursorBrush = SolidColor(colors.terracotta),
          modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
        )
      }
      if (query.isNotEmpty()) {
        Text(
          text = "Limpiar",
          style = MaterialTheme.typography.labelLarge,
          color = colors.terracotta,
          modifier = Modifier
            .clip(FloraChipShape)
            .clickable { onQueryChange("") }
            .padding(horizontal = 6.dp, vertical = 2.dp)
        )
      }
    }
  }
}

@Composable
private fun FloraCollectionEditorialBlock(
  folioNumber: Int,
  item: PlaylistWithTracks,
  currentPlayingTrackId: Long?,
  onOpen: () -> Unit,
  onPlayTrackAt: (Int) -> Unit,
) {
  val colors = LocalFloraColors.current
  val species = BotanicalCatalog.getById(item.playlist.speciesMotif)
  val useOliveMatting = folioNumber % 2 == 1

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    FloraMountedPlate(
      shape = FloraHeroShape,
      containerColor = if (useOliveMatting) OliveMatting else colors.paperCard,
      borderColor = colors.borderInk,
      goldHairline = true,
      shadowOffsetX = 4.dp,
      shadowOffsetY = 5.dp,
      shadowColor = colors.shadowPaper,
      onClick = onOpen,
      modifier = Modifier
        .fillMaxWidth()
        .height(210.dp)
        .testTag("playlist_card_${item.playlist.id}")
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        Image(
          painter = painterResource(id = species.coverResId),
          contentDescription = item.playlist.name,
          contentScale = if (useOliveMatting) ContentScale.Fit else ContentScale.Crop,
          modifier = Modifier
            .fillMaxSize()
            .padding(if (useOliveMatting) 8.dp else 4.dp)
            .clip(FloraHeroShape)
        )

        if (useOliveMatting) {
          FloraFlowerGlyph(
            color = FloraGold.copy(alpha = 0.45f),
            filled = true,
            centerColor = OliveMatting,
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(end = 22.dp, bottom = 30.dp)
              .size(28.dp)
          )
        }

        Surface(
          shape = FloraChipShape,
          color = FloraForest.copy(alpha = 0.94f),
          border = BorderStroke(1.dp, FloraGold.copy(alpha = 0.65f)),
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 14.dp, bottom = 14.dp)
        ) {
          Text(
            text = item.playlist.name.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = FloraPlayerCream,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
          )
        }
      }
    }

    if (item.tracks.isNotEmpty()) {
      Column(modifier = Modifier.fillMaxWidth()) {
        val visibleTracks = item.tracks.take(4)
        visibleTracks.forEachIndexed { trackIdx, track ->
          val isCurrent = track.id == currentPlayingTrackId
          CollectionTrackListRow(
            indexNumber = trackIdx + 1,
            track = track,
            isCurrent = isCurrent,
            onClick = { onPlayTrackAt(trackIdx) }
          )
          if (trackIdx < visibleTracks.lastIndex) {
            HorizontalDivider(thickness = 0.8.dp, color = colors.borderInk.copy(alpha = 0.14f))
          }
        }
        if (item.tracks.size > 4) {
          TextButton(
            onClick = onOpen,
            modifier = Modifier.align(Alignment.End)
          ) {
            Text(
              "Ver ${item.tracks.size} pistas",
              style = MaterialTheme.typography.labelLarge,
              color = colors.terracotta
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CollectionTrackListRow(
  indexNumber: Int,
  track: AudioTrack,
  isCurrent: Boolean,
  onClick: () -> Unit,
) {
  val colors = LocalFloraColors.current
  val bg = if (isCurrent) colors.currentHighlight else Color.Transparent
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(FloraStripShape)
      .background(bg)
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier.width(32.dp),
      contentAlignment = Alignment.CenterStart
    ) {
      if (isCurrent) {
        Icon(
          imageVector = Icons.Filled.MusicNote,
          contentDescription = "Reproduciendo",
          tint = colors.terracotta,
          modifier = Modifier.size(16.dp)
        )
      } else {
        Text(
          text = "%02d".format(indexNumber),
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
          color = colors.terracotta
        )
      }
    }

    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = track.title,
        style = MaterialTheme.typography.titleMedium,
        color = colors.ink,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = track.artist,
        style = MaterialTheme.typography.bodySmall,
        color = colors.trackMeta,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }

    Spacer(modifier = Modifier.width(8.dp))

    Text(
      text = formatDuration(track.durationMs),
      style = MaterialTheme.typography.bodySmall,
      color = colors.metaGreen
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistDetailView(
  playlistWithTracks: PlaylistWithTracks,
  allLibraryTracks: List<AudioTrack>,
  currentPlayingTrackId: Long?,
  onBack: () -> Unit,
  onPlayFromIndex: (Int) -> Unit,
  onShuffle: () -> Unit,
  onEditMetadata: () -> Unit,
  onDuplicate: () -> Unit,
  onExportFormat: (String) -> Unit,
  onDelete: () -> Unit,
  onAddTrack: (AudioTrack) -> Unit,
  onRemoveTrack: (AudioTrack) -> Unit,
  onSaveOrder: (List<AudioTrack>) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = LocalFloraColors.current
  val species = BotanicalCatalog.getById(playlistWithTracks.playlist.speciesMotif)
  val editableTracks = remember(playlistWithTracks.playlist.id) { mutableStateListOf<AudioTrack>() }
  LaunchedEffect(playlistWithTracks.tracks) {
    editableTracks.clear()
    editableTracks.addAll(playlistWithTracks.tracks)
  }

  var showAddTracksSheet by remember { mutableStateOf(false) }
  var showDetailMenu by remember { mutableStateOf(false) }
  var detailSearchQuery by remember { mutableStateOf("") }
  var draggedIndex by remember { mutableStateOf<Int?>(null) }
  var dragOffsetY by remember { mutableFloatStateOf(0f) }
  val rowHeightPx = with(LocalDensity.current) { 78.dp.toPx() }

  val filteredDetailTracks = remember(editableTracks.toList(), detailSearchQuery) {
    val q = detailSearchQuery.trim()
    if (q.isEmpty()) {
      editableTracks.mapIndexed { idx, t -> idx to t }
    } else {
      editableTracks.mapIndexedNotNull { idx, t ->
        if (t.title.contains(q, ignoreCase = true) || t.artist.contains(q, ignoreCase = true)) {
          idx to t
        } else null
      }
    }
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 156.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("playlist_detail_back")
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = colors.ink)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onEditMetadata, modifier = Modifier.minimumInteractiveComponentSize()) {
            Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = colors.ink)
          }
          Box {
            IconButton(
              onClick = { showDetailMenu = true },
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("playlist_detail_more")
            ) {
              Icon(Icons.Filled.MoreVert, contentDescription = "Más acciones", tint = colors.ink)
            }
            DropdownMenu(
              expanded = showDetailMenu,
              onDismissRequest = { showDetailMenu = false }
            ) {
              DropdownMenuItem(
                text = { Text("Duplicar colección") },
                leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                onClick = {
                  showDetailMenu = false
                  onDuplicate()
                }
              )
              DropdownMenuItem(
                text = { Text("Exportar .m3u") },
                leadingIcon = { Icon(Icons.Filled.FileDownload, contentDescription = null) },
                onClick = {
                  showDetailMenu = false
                  onExportFormat("m3u")
                }
              )
              DropdownMenuItem(
                text = { Text("Exportar .json") },
                leadingIcon = { Icon(Icons.Filled.FileDownload, contentDescription = null) },
                onClick = {
                  showDetailMenu = false
                  onExportFormat("json")
                }
              )
              DropdownMenuItem(
                text = { Text("Eliminar colección") },
                leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = colors.terracotta) },
                onClick = {
                  showDetailMenu = false
                  onDelete()
                }
              )
            }
          }
        }
      }
    }

    item {
      FloraMountedPlate(
        shape = FloraHeroShape,
        containerColor = colors.paperCard,
        borderColor = colors.borderInk,
        goldHairline = true,
        shadowOffsetX = 4.dp,
        shadowOffsetY = 5.dp,
        shadowColor = colors.shadowPaper,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Image(
            painter = painterResource(id = species.coverResId),
            contentDescription = playlistWithTracks.playlist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .size(80.dp)
              .clip(FloraThumbShape)
              .border(1.dp, colors.borderInk, FloraThumbShape)
          )
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              playlistWithTracks.playlist.name,
              style = MaterialTheme.typography.headlineMedium,
              color = colors.ink
            )
            if (playlistWithTracks.playlist.description.isNotBlank()) {
              Text(
                playlistWithTracks.playlist.description,
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = colors.metaGreen
              )
            }
            Text(
              "${editableTracks.size} pistas · ${formatDuration(playlistWithTracks.totalDurationMs)}",
              style = MaterialTheme.typography.labelSmall,
              color = colors.sage
            )
          }
        }
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = { if (editableTracks.isNotEmpty()) onPlayFromIndex(0) },
          enabled = editableTracks.isNotEmpty(),
          shape = FloraStripShape,
          colors = ButtonDefaults.buttonColors(
            containerColor = colors.cobalt,
            contentColor = FloraPlayerCream
          ),
          modifier = Modifier
            .weight(1f)
            .testTag("playlist_detail_play")
        ) {
          Icon(Icons.Filled.PlayArrow, contentDescription = null)
          Spacer(modifier = Modifier.width(4.dp))
          Text("Reproducir", style = MaterialTheme.typography.labelLarge)
        }
        OutlinedButton(
          onClick = onShuffle,
          enabled = editableTracks.isNotEmpty(),
          shape = FloraStripShape,
          border = BorderStroke(1.dp, colors.borderInk),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp), tint = colors.ink)
          Spacer(modifier = Modifier.width(4.dp))
          Text("Aleatorio", style = MaterialTheme.typography.labelLarge, color = colors.ink)
        }
        OutlinedButton(
          onClick = { showAddTracksSheet = true },
          shape = FloraChipShape,
          border = BorderStroke(1.dp, colors.borderInk),
          modifier = Modifier.testTag("playlist_add_tracks_button")
        ) {
          Icon(Icons.Filled.Add, contentDescription = "Añadir pistas", tint = colors.ink)
        }
      }
    }

    if (editableTracks.isNotEmpty()) {
      item {
        PlaylistSearchField(
          query = detailSearchQuery,
          onQueryChange = { detailSearchQuery = it },
          placeholder = "Filtrar canciones dentro de esta colección",
          testTag = "playlist_detail_search_input"
        )
      }
    }

    if (editableTracks.isEmpty()) {
      item {
        FloraDashedEmptyPlate(message = "Lista vacía.")
        Spacer(modifier = Modifier.height(8.dp))
        Button(
          onClick = { showAddTracksSheet = true },
          shape = FloraStripShape,
          colors = ButtonDefaults.buttonColors(containerColor = colors.cobalt, contentColor = FloraPlayerCream),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Añadir pistas", style = MaterialTheme.typography.labelLarge)
        }
      }
    } else if (filteredDetailTracks.isEmpty()) {
      item {
        FloraDashedEmptyPlate(message = "Sin pistas que coincidan con «$detailSearchQuery».")
      }
    } else {
      val allowReorder = detailSearchQuery.isBlank()
      itemsIndexed(
        items = filteredDetailTracks,
        key = { _, pair -> pair.second.id }
      ) { _, (originalIndex, track) ->
        val isDragging = allowReorder && draggedIndex == originalIndex
        val isCurrent = track.id == currentPlayingTrackId

        Surface(
          shape = FloraStripShape,
          color = if (isCurrent) colors.currentHighlight else colors.paperCard,
          border = BorderStroke(
            width = if (isDragging || isCurrent) 1.5.dp else 1.dp,
            color = if (isDragging || isCurrent) colors.terracotta else colors.borderInk.copy(alpha = 0.45f)
          ),
          shadowElevation = if (isDragging) 6.dp else 0.dp,
          modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 2f else 0f)
            .offset { IntOffset(0, if (isDragging) dragOffsetY.roundToInt() else 0) }
            .clickable { onPlayFromIndex(originalIndex) }
            .testTag("playlist_track_row_${track.id}")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            if (allowReorder) {
              Box(
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .pointerInput(editableTracks.size) {
                    detectDragGesturesAfterLongPress(
                      onDragStart = {
                        draggedIndex = originalIndex
                        dragOffsetY = 0f
                      },
                      onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetY += dragAmount.y
                        val cur = draggedIndex ?: return@detectDragGesturesAfterLongPress
                        val targetDelta = (dragOffsetY / rowHeightPx).roundToInt()
                        val targetIdx = (cur + targetDelta).coerceIn(0, editableTracks.lastIndex)
                        if (targetIdx != cur) {
                          val moved = editableTracks.removeAt(cur)
                          editableTracks.add(targetIdx, moved)
                          draggedIndex = targetIdx
                          dragOffsetY -= (targetIdx - cur) * rowHeightPx
                        }
                      },
                      onDragEnd = {
                        draggedIndex = null
                        dragOffsetY = 0f
                        onSaveOrder(editableTracks.toList())
                      },
                      onDragCancel = {
                        draggedIndex = null
                        dragOffsetY = 0f
                      }
                    )
                  },
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Filled.DragHandle,
                  contentDescription = "Reordenar",
                  tint = colors.metaGreen
                )
              }
            } else {
              Spacer(modifier = Modifier.width(8.dp))
            }

            AlbumArtFrame(
              track = track,
              customShape = FloraThumbShape,
              modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "%02d · %s".format(originalIndex + 1, track.title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                "${track.artist} · ${formatDuration(track.durationMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.trackMeta,
                maxLines = 1
              )
            }

            if (allowReorder) {
              IconButton(
                onClick = {
                  if (originalIndex > 0) {
                    val item = editableTracks.removeAt(originalIndex)
                    editableTracks.add(originalIndex - 1, item)
                    onSaveOrder(editableTracks.toList())
                  }
                },
                enabled = originalIndex > 0,
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("move_up_${track.id}")
              ) {
                Icon(Icons.Filled.ArrowUpward, contentDescription = "Subir", modifier = Modifier.size(18.dp), tint = colors.ink)
              }
              IconButton(
                onClick = {
                  if (originalIndex < editableTracks.lastIndex) {
                    val item = editableTracks.removeAt(originalIndex)
                    editableTracks.add(originalIndex + 1, item)
                    onSaveOrder(editableTracks.toList())
                  }
                },
                enabled = originalIndex < editableTracks.lastIndex,
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("move_down_${track.id}")
              ) {
                Icon(Icons.Filled.ArrowDownward, contentDescription = "Bajar", modifier = Modifier.size(18.dp), tint = colors.ink)
              }
            }
            IconButton(
              onClick = { onRemoveTrack(track) },
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("remove_from_playlist_${track.id}")
            ) {
              Icon(
                Icons.Filled.RemoveCircleOutline,
                contentDescription = "Quitar",
                tint = colors.terracotta,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }
    }
  }

  if (showAddTracksSheet) {
    val existingIds = editableTracks.map { it.id }.toSet()
    val candidates = allLibraryTracks.filter { it.id !in existingIds }
    val selectedIds = remember { mutableStateListOf<Long>() }

    ModalBottomSheet(
      onDismissRequest = { showAddTracksSheet = false },
      containerColor = colors.paper
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .navigationBarsPadding()
          .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text("Añadir pistas", style = MaterialTheme.typography.headlineMedium, color = colors.ink)
        if (candidates.isEmpty()) {
          Text(
            "Todas las pistas ya están incluidas.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.metaGreen
          )
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            itemsIndexed(candidates, key = { _, t -> t.id }) { _, track ->
              val checked = track.id in selectedIds
              Surface(
                shape = FloraStripShape,
                color = colors.paperWarm.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, colors.borderInk.copy(alpha = 0.4f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    if (checked) selectedIds.remove(track.id) else selectedIds.add(track.id)
                  }
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Checkbox(
                    checked = checked,
                    onCheckedChange = { isChecked ->
                      if (isChecked) selectedIds.add(track.id) else selectedIds.remove(track.id)
                    }
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      track.title,
                      style = MaterialTheme.typography.titleMedium,
                      color = colors.ink,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(track.artist, style = MaterialTheme.typography.bodySmall, color = colors.metaGreen)
                  }
                }
              }
            }
          }
          Button(
            onClick = {
              candidates.filter { it.id in selectedIds }.forEach(onAddTrack)
              showAddTracksSheet = false
            },
            enabled = selectedIds.isNotEmpty(),
            shape = FloraStripShape,
            colors = ButtonDefaults.buttonColors(containerColor = colors.cobalt, contentColor = FloraPlayerCream),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Añadir (${selectedIds.size})", style = MaterialTheme.typography.labelLarge)
          }
        }
      }
    }
  }
}
