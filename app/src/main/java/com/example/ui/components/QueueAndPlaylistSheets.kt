package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AudioTrack
import com.example.data.model.BotanicalCatalog
import com.example.data.model.BotanicalSpeciesId
import com.example.data.model.PlaybackState
import com.example.data.model.PlaylistWithTracks
import com.example.ui.theme.FloraBorderInk
import com.example.ui.theme.FloraChipShape
import com.example.ui.theme.FloraCobalt
import com.example.ui.theme.FloraInk
import com.example.ui.theme.FloraMetaGreen
import com.example.ui.theme.FloraPaper
import com.example.ui.theme.FloraPaperCard
import com.example.ui.theme.FloraPlayerCream
import com.example.ui.theme.FloraStripShape
import com.example.ui.theme.FloraTerracotta
import com.example.ui.theme.FloraThumbShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpNextQueueSheet(
  playbackState: PlaybackState,
  onJumpToIndex: (Int) -> Unit,
  onMoveItem: (Int, Int) -> Unit,
  onRemoveItem: (Int) -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = FloraPaper
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .navigationBarsPadding()
        .padding(bottom = 20.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = FloraTerracotta)
        Text("Cola (${playbackState.queue.size})", style = MaterialTheme.typography.headlineMedium, color = FloraInk)
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (playbackState.queue.isEmpty()) {
        FloraDashedEmptyPlate(message = "Cola vacía.")
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          itemsIndexed(
            items = playbackState.queue,
            key = { idx, item -> "${idx}_${item.id}" }
          ) { index, track ->
            val isCurrent = index == playbackState.currentIndex
            Surface(
              shape = FloraStripShape,
              color = if (isCurrent) Color(0xFFEBD9B3) else FloraPaperCard,
              border = BorderStroke(
                width = 1.dp,
                color = if (isCurrent) FloraTerracotta else FloraBorderInk.copy(alpha = 0.45f)
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onJumpToIndex(index) }
                .testTag("queue_item_$index")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                AlbumArtFrame(
                  track = track,
                  customShape = FloraThumbShape,
                  modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isCurrent) {
                      Icon(Icons.Filled.GraphicEq, contentDescription = "Sonando", tint = FloraTerracotta, modifier = Modifier.size(16.dp))
                    }
                    Text(
                      track.title,
                      style = MaterialTheme.typography.titleMedium,
                      color = FloraInk,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                  Text(
                    track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = FloraMetaGreen,
                    maxLines = 1
                  )
                }
                IconButton(
                  onClick = { if (index > 0) onMoveItem(index, index - 1) },
                  enabled = index > 0,
                  modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                  Icon(Icons.Filled.ArrowUpward, contentDescription = "Subir")
                }
                IconButton(
                  onClick = { if (index < playbackState.queue.lastIndex) onMoveItem(index, index + 1) },
                  enabled = index < playbackState.queue.lastIndex,
                  modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                  Icon(Icons.Filled.ArrowDownward, contentDescription = "Bajar")
                }
                if (playbackState.queue.size > 1) {
                  IconButton(
                    onClick = { onRemoveItem(index) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                  ) {
                    Icon(Icons.Filled.Close, contentDescription = "Quitar")
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
  track: AudioTrack,
  playlists: List<PlaylistWithTracks>,
  onAddTrackToPlaylist: (PlaylistWithTracks) -> Unit,
  onRequestNewPlaylist: () -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = FloraPaper
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .navigationBarsPadding()
        .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, tint = FloraTerracotta)
        Text("Colección", style = MaterialTheme.typography.headlineMedium, color = FloraInk)
      }

      OutlinedButton(
        onClick = {
          onDismiss()
          onRequestNewPlaylist()
        },
        shape = FloraStripShape,
        border = BorderStroke(1.dp, FloraBorderInk),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("create_playlist_from_sheet")
      ) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Nueva colección", style = MaterialTheme.typography.labelLarge)
      }

      playlists.forEach { item ->
        val alreadyIn = item.tracks.any { it.id == track.id }
        Surface(
          shape = FloraStripShape,
          color = FloraPaperCard,
          border = BorderStroke(1.dp, FloraBorderInk.copy(alpha = 0.45f)),
          modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !alreadyIn) {
              onAddTrackToPlaylist(item)
              onDismiss()
            }
            .testTag("pick_playlist_${item.playlist.id}")
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            BotanicalSpecimenIllustration(
              speciesId = item.playlist.speciesMotif,
              modifier = Modifier.size(38.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(item.playlist.name, style = MaterialTheme.typography.titleMedium, color = FloraInk)
              Text(
                "${item.tracks.size} pistas",
                style = MaterialTheme.typography.bodySmall,
                color = FloraMetaGreen
              )
            }
            Text(
              text = if (alreadyIn) "Incluida" else "Añadir",
              style = MaterialTheme.typography.labelLarge,
              color = if (alreadyIn) FloraMetaGreen else FloraTerracotta
            )
          }
        }
      }
    }
  }
}

@Composable
fun CreateOrEditPlaylistDialog(
  initialName: String = "",
  initialDescription: String = "",
  initialMotif: BotanicalSpeciesId = BotanicalSpeciesId.TIGER_LILY,
  isEditing: Boolean = false,
  onConfirm: (String, String, BotanicalSpeciesId) -> Unit,
  onDismiss: () -> Unit,
) {
  var name by remember { mutableStateOf(initialName) }
  var description by remember { mutableStateOf(initialDescription) }
  var motif by remember { mutableStateOf(initialMotif) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = FloraPaper,
    title = {
      Text(
        text = if (isEditing) "Editar colección" else "Nueva colección",
        style = MaterialTheme.typography.headlineMedium,
        color = FloraInk
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Nombre") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("playlist_name_input")
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Nota") },
          maxLines = 2,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("playlist_desc_input")
        )
        Text("Emblema", style = MaterialTheme.typography.labelLarge, color = FloraInk)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          BotanicalCatalog.allSpecies.forEach { species ->
            FilterChip(
              selected = motif == species.id,
              onClick = { motif = species.id },
              label = { Text(species.commonName) },
              shape = FloraChipShape,
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = FloraTerracotta,
                selectedLabelColor = FloraPlayerCream
              ),
              leadingIcon = {
                BotanicalSpecimenIllustration(
                  speciesId = species.id,
                  modifier = Modifier.size(20.dp)
                )
              }
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { if (name.isNotBlank()) onConfirm(name, description, motif) },
        enabled = name.isNotBlank(),
        shape = FloraStripShape,
        colors = ButtonDefaults.buttonColors(containerColor = FloraCobalt, contentColor = FloraPlayerCream),
        modifier = Modifier.testTag("confirm_playlist_button")
      ) {
        Text(if (isEditing) "Guardar" else "Crear", style = MaterialTheme.typography.labelLarge)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancelar", color = FloraInk) }
    }
  )
}
