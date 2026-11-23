package com.example.ui.components

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioEffectsState
import com.example.data.model.EqualizerPreset
import com.example.data.model.PlaybackState
import com.example.ui.theme.FloraBorderInk
import com.example.ui.theme.FloraChipShape
import com.example.ui.theme.FloraForest
import com.example.ui.theme.FloraInk
import com.example.ui.theme.FloraMetaGreen
import com.example.ui.theme.FloraPaperCard
import com.example.ui.theme.FloraPaperWarm
import com.example.ui.theme.FloraPlayerCream
import com.example.ui.theme.FloraStripShape
import com.example.ui.theme.FloraTerracotta
import com.example.ui.theme.FloraThumbShape

@Composable
fun EqualizerOverlayScreen(
  effectsState: AudioEffectsState,
  playbackState: PlaybackState,
  onTogglePlayPause: () -> Unit,
  onToggleEqualizerEnabled: (Boolean) -> Unit,
  onSelectPreset: (EqualizerPreset) -> Unit,
  onBandLevelChange: (Int, Int) -> Unit,
  onResetEqualizerFlat: () -> Unit,
  onBassBoostChange: (Int) -> Unit,
  onVirtualizerChange: (Int) -> Unit,
  onSpeedChange: (Float) -> Unit,
  onGaplessToggle: (Boolean) -> Unit,
  onCrossfadeToggle: (Boolean) -> Unit,
  onChameleonToggle: (Boolean) -> Unit,
  onSleepTimerSelect: (Int?) -> Unit,
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)
  val track = playbackState.currentTrack

  ParchmentBackground(chameleonEnabled = effectsState.chameleonThemeEnabled) {
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
            .testTag("eq_close_button")
        ) {
          Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Volver",
            tint = FloraInk,
            modifier = Modifier.size(28.dp)
          )
        }

        Text(
          text = "Ecualizador",
          style = MaterialTheme.typography.displayLarge.copy(fontSize = 30.sp),
          color = FloraInk
        )

        Switch(
          checked = effectsState.equalizerEnabled,
          onCheckedChange = onToggleEqualizerEnabled,
          colors = SwitchDefaults.colors(
            checkedThumbColor = FloraPlayerCream,
            checkedTrackColor = FloraForest,
            uncheckedThumbColor = FloraInk,
            uncheckedTrackColor = FloraPaperWarm
          ),
          modifier = Modifier.testTag("eq_master_switch")
        )
      }

      if (track != null) {
        FloraMountedPlate(
          shape = FloraStripShape,
          containerColor = FloraPaperWarm,
          borderColor = FloraBorderInk.copy(alpha = 0.45f),
          shadowOffsetX = 0.dp,
          shadowOffsetY = 0.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            AlbumArtFrame(
              track = track,
              customShape = FloraThumbShape,
              borderColor = FloraBorderInk.copy(alpha = 0.65f),
              modifier = Modifier.size(46.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                color = FloraInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = FloraMetaGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            IconButton(
              onClick = onTogglePlayPause,
              modifier = Modifier.minimumInteractiveComponentSize()
            ) {
              Icon(
                imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playbackState.isPlaying) "Pausar" else "Reproducir",
                tint = FloraInk
              )
            }
          }
        }
      }

      BotanicalEqualizerPanel(
        effectsState = effectsState,
        onSelectPreset = onSelectPreset,
        onBandLevelChange = onBandLevelChange,
        onResetFlat = onResetEqualizerFlat,
        onBassBoostChange = onBassBoostChange,
        onVirtualizerChange = onVirtualizerChange,
        isPlaying = playbackState.isPlaying,
      )

      // Gapless & Palette switches (concise labels only)
      FloraMountedPlate(
        shape = FloraStripShape,
        containerColor = FloraPaperCard,
        borderColor = FloraBorderInk.copy(alpha = 0.45f),
        shadowOffsetX = 0.dp,
        shadowOffsetY = 0.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Sin pausas (Gapless)", style = MaterialTheme.typography.titleSmall, color = FloraInk)
            Switch(
              checked = effectsState.gaplessEnabled,
              onCheckedChange = onGaplessToggle,
              colors = SwitchDefaults.colors(checkedTrackColor = FloraForest, checkedThumbColor = FloraPlayerCream),
              modifier = Modifier.testTag("switch_gapless")
            )
          }
          DottedRuleDivider(color = FloraBorderInk.copy(alpha = 0.4f))
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Fundido cruzado", style = MaterialTheme.typography.titleSmall, color = FloraInk)
            Switch(
              checked = effectsState.crossfadeEnabled,
              onCheckedChange = onCrossfadeToggle,
              colors = SwitchDefaults.colors(checkedTrackColor = FloraForest, checkedThumbColor = FloraPlayerCream),
              modifier = Modifier.testTag("switch_crossfade")
            )
          }
          DottedRuleDivider(color = FloraBorderInk.copy(alpha = 0.4f))
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Paleta por carátula", style = MaterialTheme.typography.titleSmall, color = FloraInk)
            Switch(
              checked = effectsState.chameleonThemeEnabled,
              onCheckedChange = onChameleonToggle,
              colors = SwitchDefaults.colors(checkedTrackColor = FloraForest, checkedThumbColor = FloraPlayerCream),
              modifier = Modifier.testTag("switch_chameleon")
            )
          }
        }
      }

      // Speed
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(Icons.Filled.Speed, contentDescription = null, tint = FloraTerracotta, modifier = Modifier.size(18.dp))
        Text("Velocidad", style = MaterialTheme.typography.titleSmall, color = FloraInk)
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { spd ->
          FilterChip(
            selected = kotlin.math.abs(effectsState.playbackSpeed - spd) < 0.05f,
            onClick = { onSpeedChange(spd) },
            label = { Text("${spd}x", style = MaterialTheme.typography.labelLarge) },
            shape = FloraChipShape,
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = FloraTerracotta,
              selectedLabelColor = FloraPlayerCream
            ),
            modifier = Modifier.testTag("speed_chip_$spd")
          )
        }
      }

      // Sleep Timer
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(Icons.Filled.Timer, contentDescription = null, tint = FloraTerracotta, modifier = Modifier.size(18.dp))
        Text(
          text = effectsState.sleepTimerMinutesRemaining?.let {
            "Temporizador ($it min)"
          } ?: "Temporizador",
          style = MaterialTheme.typography.titleSmall,
          color = FloraInk
        )
      }
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterChip(
          selected = effectsState.sleepTimerMinutesRemaining == null,
          onClick = { onSleepTimerSelect(null) },
          label = { Text("No") },
          shape = FloraChipShape,
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = FloraTerracotta,
            selectedLabelColor = FloraPlayerCream
          )
        )
        listOf(15, 30, 45, 60).forEach { min ->
          FilterChip(
            selected = effectsState.sleepTimerMinutesRemaining == min,
            onClick = { onSleepTimerSelect(min) },
            label = { Text("$min min") },
            shape = FloraChipShape,
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = FloraTerracotta,
              selectedLabelColor = FloraPlayerCream
            ),
            modifier = Modifier.testTag("sleep_timer_$min")
          )
        }
      }

      Spacer(modifier = Modifier.size(16.dp))
    }
  }
}
