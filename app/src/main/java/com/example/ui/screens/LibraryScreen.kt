package com.example.ui.screens

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.data.model.AudioTrack
import com.example.data.model.BotanicalSpeciesId
import com.example.ui.components.AlbumArtFrame
import com.example.ui.components.FloraDashedEmptyPlate
import com.example.ui.components.FloraFlowerGlyph
import com.example.ui.components.FloraMountedPlate
import com.example.ui.components.FloraScreenHeader
import com.example.ui.theme.FloraBorderInk
import com.example.ui.theme.FloraChipShape
import com.example.ui.theme.FloraForest
import com.example.ui.theme.FloraInk
import com.example.ui.theme.FloraMetaGreen
import com.example.ui.theme.FloraPaper
import com.example.ui.theme.FloraPaperCard
import com.example.ui.theme.FloraPaperWarm
import com.example.ui.theme.FloraPlateShape
import com.example.ui.theme.FloraPlayerCream
import com.example.ui.theme.FloraSage
import com.example.ui.theme.FloraShadowPaper
import com.example.ui.theme.FloraStripShape
import com.example.ui.theme.FloraTerracotta
import com.example.ui.theme.FloraThumbShape
import com.example.ui.theme.FloraTrackMeta
import com.example.ui.viewmodel.HerbariumUiState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private sealed interface LibraryRowItem {
  val key: String
  val sectionChar: Char

  data class SectionHeader(
    override val sectionChar: Char,
    val count: Int,
  ) : LibraryRowItem {
    override val key: String = "hdr_$sectionChar"
  }

  data class TrackRow(
    val track: AudioTrack,
    val flatIndex: Int,
    override val sectionChar: Char,
  ) : LibraryRowItem {
    override val key: String = "tr_${track.id}"
  }
}

private fun trackSectionChar(title: String): Char {
  val ch = title.trim().firstOrNull()?.uppercaseChar()
  return if (ch != null && ch in 'A'..'Z') ch else '#'
}

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
) {
  val currentTrackId = uiState.playbackState.currentTrack?.id
  var showSourcesDialog by remember { mutableStateOf(false) }

  val sortedTracks = remember(uiState.filteredTracks) {
    uiState.filteredTracks.sortedWith(
      compareBy<AudioTrack> { chSortPriority(trackSectionChar(it.title)) }
        .thenBy { it.title.trim().lowercase() }
    )
  }

  val availableLetters = remember(sortedTracks) {
    buildSet {
      sortedTracks.forEach { add(trackSectionChar(it.title)) }
    }
  }

  val (rowItems, letterToItemIndex, sectionTrackCounts) = remember(sortedTracks) {
    val rows = mutableListOf<LibraryRowItem>()
    val indexMap = mutableMapOf<Char, Int>()
    val grouped = sortedTracks.groupBy { trackSectionChar(it.title) }
    val counts = grouped.mapValues { it.value.size }
    var flatIdx = 0
    val orderedKeys = grouped.keys.sortedBy { chSortPriority(it) * 1000 + it.code }
    for (secChar in orderedKeys) {
      val tracksInSection = grouped[secChar].orEmpty()
      indexMap[secChar] = rows.size
      rows += LibraryRowItem.SectionHeader(sectionChar = secChar, count = tracksInSection.size)
      for (t in tracksInSection) {
        rows += LibraryRowItem.TrackRow(track = t, flatIndex = flatIdx, sectionChar = secChar)
        flatIdx++
      }
    }
    Triple(rows, indexMap, counts)
  }

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
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      FloraScreenHeader(
        title = "Canciones",
        sealText = "M",
        onSealClick = onOpenHerbarium,
        sealContentDescription = "Herbario"
      )

      // Search Field
      FloraMountedPlate(
        shape = FloraStripShape,
        containerColor = FloraPaperCard,
        borderColor = FloraInk,
        shadowOffsetX = 2.dp,
        shadowOffsetY = 3.dp,
        shadowColor = FloraShadowPaper,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          FloraFlowerGlyph(
            modifier = Modifier.size(20.dp),
            color = FloraTerracotta
          )
          Spacer(modifier = Modifier.width(10.dp))
          Box(modifier = Modifier.weight(1f)) {
            if (uiState.searchQuery.isEmpty()) {
              Text(
                text = "Buscar canción, artista o fuente",
                style = MaterialTheme.typography.bodyMedium,
                color = FloraMetaGreen
              )
            }
            BasicTextField(
              value = uiState.searchQuery,
              onValueChange = onSearchChange,
              singleLine = true,
              textStyle = MaterialTheme.typography.bodyMedium.copy(color = FloraInk),
              cursorBrush = SolidColor(FloraTerracotta),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("library_search_input")
            )
          }
          if (uiState.searchQuery.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Limpiar",
              style = MaterialTheme.typography.labelLarge,
              color = FloraTerracotta,
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

      // FUENTES (Detected & Imported Audio Folder Sources > 1m30s)
      Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "FUENTES",
            style = MaterialTheme.typography.labelMedium,
            color = FloraSage
          )
          if (uiState.detectedFolders.isNotEmpty() || uiState.audioSources.isNotEmpty()) {
            Text(
              text = "Ver fuentes (${maxOf(uiState.detectedFolders.size, uiState.audioSources.size)})",
              style = MaterialTheme.typography.labelLarge,
              color = FloraTerracotta,
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
            color = if (allSelected) FloraForest else FloraPaperCard,
            border = BorderStroke(
              1.dp,
              if (allSelected) FloraForest else FloraBorderInk.copy(alpha = 0.45f)
            ),
            modifier = Modifier
              .clip(FloraChipShape)
              .clickable { onSelectSourcePath(null) }
              .testTag("source_chip_all")
          ) {
            Text(
              text = "Todas (${uiState.tracks.size})",
              style = MaterialTheme.typography.labelLarge,
              color = if (allSelected) FloraPlayerCream else FloraInk,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
          }

          uiState.audioSources.forEach { source ->
            val selected = uiState.selectedSourcePath == source.folderPath
            Surface(
              shape = FloraChipShape,
              color = if (selected) FloraTerracotta else FloraPaperCard,
              border = BorderStroke(
                1.dp,
                if (selected) FloraTerracotta else FloraBorderInk.copy(alpha = 0.45f)
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
                  tint = if (selected) FloraPlayerCream else FloraTerracotta,
                  modifier = Modifier.size(14.dp)
                )
                Text(
                  text = "${source.folderName} (${source.trackCount})",
                  style = MaterialTheme.typography.labelLarge,
                  color = if (selected) FloraPlayerCream else FloraInk,
                  maxLines = 1
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Songs List + Vertical A-Z Fast Scrubber Rail (modern music app layout)
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
    ) {
      if (rowItems.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
          FloraDashedEmptyPlate(
            message = "Sin canciones coincidentes."
          )
        }
      } else {
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(start = 18.dp, end = 42.dp, top = 6.dp, bottom = 112.dp),
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
                  accentColor = FloraTerracotta,
                  onClick = { onPlayTrack(sortedTracks, item.flatIndex) },
                  onToggleFavorite = { onToggleFavorite(track) },
                  onPlayNext = { onPlayNext(track) },
                  onAddToQueue = { onAddToQueue(track) },
                  onAddToPlaylist = { onAddToPlaylistClick(track) },
                  onInspectSpecies = { onInspectSpecies(track.speciesId) }
                )
              }
            }
          }
        }

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
            color = FloraForest,
            border = BorderStroke(1.2.dp, FloraTerracotta),
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

  if (showSourcesDialog) {
    AlertDialog(
      onDismissRequest = { showSourcesDialog = false },
      containerColor = FloraPaper,
      title = {
        Text(
          text = "Fuentes detectadas (> 1:30)",
          style = MaterialTheme.typography.headlineMedium,
          color = FloraInk
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          if (uiState.detectedFolders.isNotEmpty()) {
            uiState.detectedFolders.forEach { folder ->
              val isWinner = folder.folderKey == uiState.dominantDetectedFolder?.folderKey
              Surface(
                shape = FloraStripShape,
                color = if (isWinner) FloraPaperWarm else FloraPaperCard,
                border = BorderStroke(
                  width = if (isWinner) 1.4.dp else 1.dp,
                  color = if (isWinner) FloraTerracotta else FloraBorderInk.copy(alpha = 0.4f)
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
                      color = FloraInk,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = "${folder.qualifyingCount} pistas > 1:30 · ${folder.totalAudioScanned} totales",
                      style = MaterialTheme.typography.bodySmall,
                      color = FloraMetaGreen
                    )
                  }
                  Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Importar ${folder.folderName}",
                    tint = FloraTerracotta
                  )
                }
              }
            }
          } else {
            uiState.audioSources.forEach { src ->
              Surface(
                shape = FloraStripShape,
                color = FloraPaperCard,
                border = BorderStroke(1.dp, FloraBorderInk.copy(alpha = 0.4f)),
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
                    Text(src.folderName, style = MaterialTheme.typography.titleSmall, color = FloraInk)
                    Text(
                      text = "${src.trackCount} canciones · ${src.folderPath}",
                      style = MaterialTheme.typography.bodySmall,
                      color = FloraMetaGreen,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            showSourcesDialog = false
            onPickRootFolderClick()
          }
        ) {
          Text("Elegir carpeta", color = FloraTerracotta)
        }
      },
      dismissButton = {
        TextButton(onClick = { showSourcesDialog = false }) {
          Text("Cerrar", color = FloraInk)
        }
      }
    )
  }
}

private fun chSortPriority(ch: Char): Int = if (ch == '#') 1 else 0

@Composable
private fun AlphabetSectionDivider(
  letter: Char,
  trackCount: Int,
) {
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
      color = FloraPaperWarm,
      border = BorderStroke(1.dp, FloraBorderInk.copy(alpha = 0.35f))
    ) {
      Text(
        text = letter.toString(),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = FloraTerracotta,
        modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp)
      )
    }
    HorizontalDivider(
      modifier = Modifier.weight(1f),
      thickness = 1.dp,
      color = FloraBorderInk.copy(alpha = 0.22f)
    )
    Text(
      text = "$trackCount",
      style = MaterialTheme.typography.labelSmall,
      color = FloraMetaGreen
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
      color = if (isScrubbing) FloraPaperWarm.copy(alpha = 0.96f) else FloraPaperCard.copy(alpha = 0.86f),
      border = BorderStroke(
        width = if (isScrubbing) 1.2.dp else 1.dp,
        color = if (isScrubbing) FloraTerracotta.copy(alpha = 0.65f) else FloraBorderInk.copy(alpha = 0.28f)
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
              .background(if (isSelected) FloraTerracotta else Color.Transparent)
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
                isAvailable -> FloraInk
                else -> FloraMetaGreen
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
) {
  var menuExpanded by remember { mutableStateOf(false) }
  val bgColor = if (isCurrent) Color(0xFFEBD9B3) else FloraPaperCard

  Surface(
    shape = FloraStripShape,
    color = bgColor,
    border = BorderStroke(
      width = if (isCurrent) 1.3.dp else 1.dp,
      color = if (isCurrent) accentColor else FloraBorderInk.copy(alpha = 0.42f)
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
          color = FloraInk,
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
          color = FloraTrackMeta,
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
          tint = if (track.isFavorite) accentColor else FloraMetaGreen
        )
      }

      Box {
        IconButton(
          onClick = { menuExpanded = true },
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("track_menu_${track.id}")
        ) {
          Icon(Icons.Filled.MoreVert, contentDescription = "Opciones", tint = FloraInk)
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
