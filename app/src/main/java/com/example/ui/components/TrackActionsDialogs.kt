package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AudioTrack
import com.example.data.model.BotanicalCatalog
import com.example.ui.screens.formatDuration
import com.example.ui.theme.FloraPlayerCream
import com.example.ui.theme.FloraStripShape
import com.example.ui.theme.FloraThumbShape
import com.example.ui.theme.LocalFloraColors

@Composable
fun TrackTechnicalDetailsDialog(
  track: AudioTrack,
  onDismiss: () -> Unit,
) {
  val colors = LocalFloraColors.current
  val species = BotanicalCatalog.getById(track.speciesId)

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = colors.paper,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Icon(Icons.Filled.AudioFile, contentDescription = null, tint = colors.terracotta)
        Text(
          text = "Ficha técnica",
          style = MaterialTheme.typography.headlineMedium,
          color = colors.ink
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = track.title,
          style = MaterialTheme.typography.titleMedium,
          color = colors.ink,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = "${track.artist} · ${track.album}",
          style = MaterialTheme.typography.bodySmall,
          color = colors.metaGreen
        )
        DottedRuleDivider(color = colors.borderInk.copy(alpha = 0.4f))

        TechnicalDetailRow("Códec", track.codec.ifBlank { "PCM" })
        TechnicalDetailRow("Tasa de bits", if (track.bitrateKbps > 0) "${track.bitrateKbps} kbps" else "Sin pérdida / Variable")
        TechnicalDetailRow("Frecuencia", "${track.sampleRateHz} Hz")
        TechnicalDetailRow("Duración", formatDuration(track.durationMs))
        TechnicalDetailRow("Género", track.genre)
        TechnicalDetailRow("Especie botánica", "${species.commonName} (${species.scientificName.substringBefore(" (")})")
        TechnicalDetailRow("Reproducciones", "${track.playCount}")
        TechnicalDetailRow("Carpeta / Fuente", track.sourceFolder)
        TechnicalDetailRow("Ubicación", track.sourcePath)
        TechnicalDetailRow("URI", track.uri)
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        shape = FloraStripShape,
        colors = ButtonDefaults.buttonColors(containerColor = colors.cobalt, contentColor = FloraPlayerCream),
        modifier = Modifier.testTag("details_close_button")
      ) {
        Text("Cerrar", style = MaterialTheme.typography.labelLarge)
      }
    }
  )
}

@Composable
private fun TechnicalDetailRow(label: String, value: String) {
  val colors = LocalFloraColors.current
  Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
    Text(
      text = label.uppercase(),
      style = MaterialTheme.typography.labelSmall,
      color = colors.sage
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      color = colors.ink,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
fun EditTrackMetadataDialog(
  track: AudioTrack,
  selectedCoverPath: String?,
  onPickCoverImage: () -> Unit,
  onClearCustomCover: () -> Unit,
  onSave: (title: String, artist: String, album: String, genre: String, coverPath: String?) -> Unit,
  onDismiss: () -> Unit,
) {
  val colors = LocalFloraColors.current
  var title by remember(track.id) { mutableStateOf(track.title) }
  var artist by remember(track.id) { mutableStateOf(track.artist) }
  var album by remember(track.id) { mutableStateOf(track.album) }
  var genre by remember(track.id) { mutableStateOf(track.genre) }
  val previewTrack = remember(track, selectedCoverPath) { track.copy(coverPath = selectedCoverPath) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = colors.paper,
    title = {
      Text(
        text = "Editar etiquetas",
        style = MaterialTheme.typography.headlineMedium,
        color = colors.ink
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Surface(
          shape = FloraStripShape,
          color = colors.paperCard,
          border = BorderStroke(1.dp, colors.borderInk.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            AlbumArtFrame(
              track = previewTrack,
              customShape = FloraThumbShape,
              modifier = Modifier.size(56.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              OutlinedButton(
                onClick = onPickCoverImage,
                shape = FloraStripShape,
                border = BorderStroke(1.dp, colors.borderInk),
                modifier = Modifier.testTag("pick_custom_cover_button")
              ) {
                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.terracotta)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Elegir carátula", style = MaterialTheme.typography.labelLarge, color = colors.ink)
              }
              if (selectedCoverPath != null) {
                TextButton(onClick = onClearCustomCover) {
                  Text("Restaurar carátula botánica", style = MaterialTheme.typography.labelSmall, color = colors.metaGreen)
                }
              }
            }
          }
        }

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Título") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_track_title_input")
        )
        OutlinedTextField(
          value = artist,
          onValueChange = { artist = it },
          label = { Text("Artista") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_track_artist_input")
        )
        OutlinedTextField(
          value = album,
          onValueChange = { album = it },
          label = { Text("Álbum") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_track_album_input")
        )
        OutlinedTextField(
          value = genre,
          onValueChange = { genre = it },
          label = { Text("Género") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_track_genre_input")
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(title, artist, album, genre, selectedCoverPath) },
        enabled = title.isNotBlank(),
        shape = FloraStripShape,
        colors = ButtonDefaults.buttonColors(containerColor = colors.cobalt, contentColor = FloraPlayerCream),
        modifier = Modifier.testTag("save_track_metadata_button")
      ) {
        Text("Guardar", style = MaterialTheme.typography.labelLarge)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancelar", color = colors.ink) }
    }
  )
}

@Composable
fun EditLyricsDialog(
  track: AudioTrack,
  initialLyrics: String,
  onImportLrcFile: () -> Unit,
  onSave: (String?) -> Unit,
  onDismiss: () -> Unit,
) {
  val colors = LocalFloraColors.current
  var lyricsText by remember(track.id, initialLyrics) { mutableStateOf(initialLyrics) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = colors.paper,
    title = {
      Text(
        text = "Letra de la pista",
        style = MaterialTheme.typography.headlineMedium,
        color = colors.ink
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Admite formato sincronizado LRC ([00:15.20] Verso) o texto plano.",
          style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
          color = colors.metaGreen
        )
        OutlinedButton(
          onClick = onImportLrcFile,
          shape = FloraStripShape,
          border = BorderStroke(1.dp, colors.borderInk),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("import_lrc_dialog_button")
        ) {
          Icon(Icons.Filled.UploadFile, contentDescription = null, tint = colors.terracotta, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Importar archivo .lrc", style = MaterialTheme.typography.labelLarge, color = colors.ink)
        }
        OutlinedTextField(
          value = lyricsText,
          onValueChange = { lyricsText = it },
          label = { Text("Letra / marcas [mm:ss.xx]") },
          minLines = 6,
          maxLines = 10,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .testTag("lyrics_editor_input")
        )
        if (lyricsText.isNotBlank()) {
          TextButton(onClick = { lyricsText = "" }) {
            Text("Vaciar letra personalizada", color = colors.terracotta)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(lyricsText.takeIf { it.isNotBlank() }) },
        shape = FloraStripShape,
        colors = ButtonDefaults.buttonColors(containerColor = colors.cobalt, contentColor = FloraPlayerCream),
        modifier = Modifier.testTag("save_lyrics_button")
      ) {
        Text("Guardar", style = MaterialTheme.typography.labelLarge)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancelar", color = colors.ink) }
    }
  )
}

@Composable
fun DeleteTrackConfirmDialog(
  track: AudioTrack,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  val colors = LocalFloraColors.current
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = colors.paper,
    title = {
      Text(
        text = "Eliminar canción",
        style = MaterialTheme.typography.headlineMedium,
        color = colors.ink
      )
    },
    text = {
      Text(
        text = "¿Quitar «${track.title}» de la fonoteca de Flora? El archivo original del dispositivo no se borrará.",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.ink
      )
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        shape = FloraStripShape,
        colors = ButtonDefaults.buttonColors(containerColor = colors.terracotta, contentColor = FloraPlayerCream),
        modifier = Modifier.testTag("confirm_delete_track_button")
      ) {
        Text("Eliminar", style = MaterialTheme.typography.labelLarge)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancelar", color = colors.ink) }
    }
  )
}
