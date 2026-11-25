package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppThemeMode
import com.example.ui.components.DottedRuleDivider
import com.example.ui.components.FloraMountedPlate
import com.example.ui.components.ParchmentBackground
import com.example.ui.theme.FloraChipShape
import com.example.ui.theme.FloraPlayerCream
import com.example.ui.theme.FloraStripShape
import com.example.ui.theme.LocalFloraColors
import com.example.ui.viewmodel.HerbariumUiState
import java.util.Locale

@Composable
fun SettingsOverlayScreen(
  uiState: HerbariumUiState,
  hasAudioPermission: Boolean,
  onRequestAudioPermission: () -> Unit,
  onTriggerManualScan: () -> Unit,
  onMinScanDurationChange: (Int) -> Unit,
  onPickFolderClick: () -> Unit,
  onImportFilesClick: () -> Unit,
  onThemeModeChange: (AppThemeMode) -> Unit,
  onChameleonToggle: (Boolean) -> Unit,
  onClearCoverCache: () -> Unit,
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)
  val colors = LocalFloraColors.current
  val effects = uiState.effectsState

  ParchmentBackground(chameleonEnabled = effects.chameleonThemeEnabled) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("settings_close_button")
        ) {
          Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Volver",
            tint = colors.ink,
            modifier = Modifier.size(28.dp)
          )
        }

        Text(
          text = "Ajustes",
          style = MaterialTheme.typography.displayLarge.copy(fontSize = 30.sp),
          color = colors.ink
        )

        Spacer(modifier = Modifier.size(48.dp))
      }

      // 1. Audio Permission Card
      FloraMountedPlate(
        shape = FloraStripShape,
        containerColor = colors.paperCard,
        borderColor = colors.borderInk.copy(alpha = 0.45f),
        shadowOffsetX = 0.dp,
        shadowOffsetY = 0.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Filled.Security, contentDescription = null, tint = colors.terracotta, modifier = Modifier.size(20.dp))
            Text("Permiso de audio del dispositivo", style = MaterialTheme.typography.titleMedium, color = colors.ink)
          }
          Text(
            text = if (hasAudioPermission) {
              "Concedido: Flora puede indexar música de tu almacenamiento automáticamente."
            } else {
              "Pendiente: concede permiso para descubrir carpetas de música en tu dispositivo."
            },
            style = MaterialTheme.typography.bodySmall,
            color = colors.metaGreen
          )
          if (!hasAudioPermission) {
            Button(
              onClick = onRequestAudioPermission,
              shape = FloraStripShape,
              colors = ButtonDefaults.buttonColors(containerColor = colors.cobalt, contentColor = FloraPlayerCream),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_request_permission_button")
            ) {
              Text("Conceder permiso de audio", style = MaterialTheme.typography.labelLarge)
            }
          }
        }
      }

      // 2. Botanical Library Scanning & Import Card
      FloraMountedPlate(
        shape = FloraStripShape,
        containerColor = colors.paperCard,
        borderColor = colors.borderInk.copy(alpha = 0.45f),
        shadowOffsetX = 0.dp,
        shadowOffsetY = 0.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = colors.terracotta, modifier = Modifier.size(20.dp))
            Text("Fonoteca y escaneo automático", style = MaterialTheme.typography.titleMedium, color = colors.ink)
          }

          Button(
            onClick = onTriggerManualScan,
            enabled = !uiState.isScanningFolders,
            shape = FloraStripShape,
            colors = ButtonDefaults.buttonColors(containerColor = colors.terracotta, contentColor = FloraPlayerCream),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("settings_rescan_button")
          ) {
            if (uiState.isScanningFolders) {
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = FloraPlayerCream
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Escaneando biblioteca...", style = MaterialTheme.typography.labelLarge)
            } else {
              Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Re-escanear ahora", style = MaterialTheme.typography.labelLarge)
            }
          }

          DottedRuleDivider(color = colors.borderInk.copy(alpha = 0.35f))

          Text(
            text = "Duración mínima para «Botanical Library»",
            style = MaterialTheme.typography.titleSmall,
            color = colors.ink
          )
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf(0, 30, 60, 90, 120).forEach { sec ->
              val label = if (sec == 0) "Todas" else "${sec}s"
              FilterChip(
                selected = effects.minScanDurationSec == sec,
                onClick = { onMinScanDurationChange(sec) },
                label = { Text(label) },
                shape = FloraChipShape,
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = colors.terracotta,
                  selectedLabelColor = FloraPlayerCream
                ),
                modifier = Modifier.testTag("scan_threshold_$sec")
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = onPickFolderClick,
              shape = FloraStripShape,
              border = BorderStroke(1.dp, colors.borderInk),
              modifier = Modifier
                .weight(1f)
                .testTag("settings_pick_folder_button")
            ) {
              Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.terracotta)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Elegir carpeta", style = MaterialTheme.typography.labelLarge, color = colors.ink)
            }
            OutlinedButton(
              onClick = onImportFilesClick,
              shape = FloraStripShape,
              border = BorderStroke(1.dp, colors.borderInk),
              modifier = Modifier
                .weight(1f)
                .testTag("settings_import_files_button")
            ) {
              Icon(Icons.Filled.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.cobalt)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Importar pistas", style = MaterialTheme.typography.labelLarge, color = colors.ink)
            }
          }
        }
      }

      // 3. Appearance & Theme Mode Card
      FloraMountedPlate(
        shape = FloraStripShape,
        containerColor = colors.paperCard,
        borderColor = colors.borderInk.copy(alpha = 0.45f),
        shadowOffsetX = 0.dp,
        shadowOffsetY = 0.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Filled.Palette, contentDescription = null, tint = colors.terracotta, modifier = Modifier.size(20.dp))
            Text("Apariencia del herbario", style = MaterialTheme.typography.titleMedium, color = colors.ink)
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            AppThemeMode.entries.forEach { mode ->
              FilterChip(
                selected = effects.themeMode == mode,
                onClick = { onThemeModeChange(mode) },
                label = { Text(mode.label) },
                shape = FloraChipShape,
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = colors.forest,
                  selectedLabelColor = FloraPlayerCream
                ),
                modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}")
              )
            }
          }

          DottedRuleDivider(color = colors.borderInk.copy(alpha = 0.35f))
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Paleta por carátula", style = MaterialTheme.typography.titleSmall, color = colors.ink)
              Text(
                "Adapta fondos y brillos al color dominante del álbum.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.metaGreen
              )
            }
            Switch(
              checked = effects.chameleonThemeEnabled,
              onCheckedChange = onChameleonToggle,
              colors = SwitchDefaults.colors(checkedTrackColor = colors.forest, checkedThumbColor = FloraPlayerCream)
            )
          }
        }
      }

      // 4. Cover Cache Management Card
      FloraMountedPlate(
        shape = FloraStripShape,
        containerColor = colors.paperCard,
        borderColor = colors.borderInk.copy(alpha = 0.45f),
        shadowOffsetX = 0.dp,
        shadowOffsetY = 0.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Filled.CleaningServices, contentDescription = null, tint = colors.terracotta, modifier = Modifier.size(20.dp))
              Text("Caché de carátulas", style = MaterialTheme.typography.titleMedium, color = colors.ink)
            }
            Text(
              text = formatCacheSizeMb(uiState.coverCacheBytes),
              style = MaterialTheme.typography.labelLarge,
              color = colors.terracotta
            )
          }

          OutlinedButton(
            onClick = onClearCoverCache,
            shape = FloraStripShape,
            border = BorderStroke(1.dp, colors.borderInk),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("settings_clear_cache_button")
          ) {
            Text("Limpiar caché de carátulas", style = MaterialTheme.typography.labelLarge, color = colors.ink)
          }
        }
      }

      Spacer(modifier = Modifier.size(16.dp))
    }
  }
}

private fun formatCacheSizeMb(bytes: Long): String {
  val mb = bytes / (1024.0 * 1024.0)
  return if (mb < 0.1 && bytes > 0L) {
    "${(bytes / 1024L).coerceAtLeast(1L)} KB"
  } else {
    String.format(Locale.getDefault(), "%.2f MB", mb)
  }
}
