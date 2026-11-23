package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PlaybackState
import com.example.ui.theme.FloraDarkMeta
import com.example.ui.theme.FloraForestDeep
import com.example.ui.theme.FloraForestSoft
import com.example.ui.theme.FloraGold
import com.example.ui.theme.FloraGoldSoft
import com.example.ui.theme.FloraPlayerCream
import com.example.ui.theme.FloraTerracotta
import com.example.ui.theme.FloraThumbShape
import com.example.ui.theme.LocalAlbumPalette
import com.example.ui.theme.LocalThemeGradients
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private val MiniPlayerShape = RoundedCornerShape(
  topStart = 3.dp,
  topEnd = 19.dp,
  bottomEnd = 3.dp,
  bottomStart = 19.dp
)

@Composable
fun MiniPlayerBar(
  playbackState: PlaybackState,
  onTogglePlayPause: () -> Unit,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  onToggleFavorite: () -> Unit,
  onOpenNowPlaying: () -> Unit,
  modifier: Modifier = Modifier,
  positionMsFlow: StateFlow<Long> = MutableStateFlow(playbackState.positionMs),
) {
  val track = playbackState.currentTrack ?: return
  val albumPalette = LocalAlbumPalette.current
  val themeGradients = LocalThemeGradients.current
  val barAccent by animateColorAsState(
    targetValue = lerp(FloraGold, albumPalette.vibrant, 0.35f),
    animationSpec = tween(550),
    label = "miniPlayerAccent"
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 14.dp, vertical = 6.dp)
      .clip(MiniPlayerShape)
      .background(
        brush = Brush.linearGradient(
          colors = listOf(themeGradients.miniPlayerStart, themeGradients.miniPlayerEnd)
        )
      )
      .border(BorderStroke(1.dp, FloraGold.copy(alpha = 0.85f)), MiniPlayerShape)
      .clickable(onClick = onOpenNowPlaying)
      .testTag("mini_player_bar")
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 10.dp, end = 6.dp, top = 10.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        AlbumArtFrame(
          track = track,
          customShape = FloraThumbShape,
          borderColor = FloraGold.copy(alpha = 0.7f),
          modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = track.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = FloraPlayerCream
          )
          Text(
            text = track.artist,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = FloraDarkMeta
          )
        }

        // 1. Favorite Flower Glyph (clean gold-soft when liked, no glow blur)
        IconButton(
          onClick = onToggleFavorite,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(44.dp)
            .testTag("mini_player_favorite")
        ) {
          FloraFlowerGlyph(
            color = if (track.isFavorite) FloraGoldSoft else FloraDarkMeta,
            filled = track.isFavorite,
            centerColor = FloraForestDeep,
            modifier = Modifier.size(18.dp)
          )
        }

        // 2. Previous
        IconButton(
          onClick = onPrevious,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(44.dp)
            .testTag("mini_player_previous")
        ) {
          Icon(
            imageVector = Icons.Filled.SkipPrevious,
            contentDescription = "Pista anterior",
            tint = FloraPlayerCream,
            modifier = Modifier.size(22.dp)
          )
        }

        // 3. Gold-ringed Circular Play / Pause
        Box(
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(44.dp)
            .clickable(onClick = onTogglePlayPause)
            .testTag("mini_player_play_pause"),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(FloraForestDeep.copy(alpha = 0.65f))
              .border(1.2.dp, FloraGoldSoft.copy(alpha = 0.85f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
              contentDescription = if (playbackState.isPlaying) "Pausar" else "Reproducir",
              tint = FloraPlayerCream,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        // 4. Next
        IconButton(
          onClick = onNext,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(44.dp)
            .testTag("mini_player_next")
        ) {
          Icon(
            imageVector = Icons.Filled.SkipNext,
            contentDescription = "Siguiente pista",
            tint = FloraPlayerCream,
            modifier = Modifier.size(22.dp)
          )
        }
      }

      // Clipped 3dp progress bar inside the asymmetric container (isolated 250ms recomposition scope)
      MiniPlayerProgressBar(
        positionMsFlow = positionMsFlow,
        durationMs = playbackState.durationMs,
        barAccent = barAccent,
      )
    }
  }
}

@Composable
private fun MiniPlayerProgressBar(
  positionMsFlow: StateFlow<Long>,
  durationMs: Long,
  barAccent: Color,
) {
  val positionMs by positionMsFlow.collectAsStateWithLifecycle()
  val progress = if (durationMs > 0L) {
    (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
  } else 0f

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(3.dp)
      .background(Color.White.copy(alpha = 0.14f))
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth(progress)
        .height(3.dp)
        .background(
          Brush.horizontalGradient(
            listOf(FloraTerracotta, barAccent)
          )
        )
    )
  }
}
