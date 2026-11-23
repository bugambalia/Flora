package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.BotanicalAudioSpectrum
import com.example.data.model.AudioTrack
import com.example.data.model.BotanicalCatalog
import com.example.data.model.BotanicalLyricsCatalog
import com.example.data.model.BotanicalPlayerTheme
import com.example.data.model.BotanicalSpeciesId
import com.example.data.model.RepeatModeOption
import com.example.ui.components.AlbumArtFrame
import com.example.ui.components.BotanicalDissolveArtworkLayer
import com.example.ui.components.BotanicalFrequencyVisualizer
import com.example.ui.components.BotanicalSpecimenIllustration
import com.example.ui.components.FloraFlowerGlyph
import com.example.ui.theme.FloraChipShape
import com.example.ui.theme.FloraDarkMeta
import com.example.ui.theme.FloraGold
import com.example.ui.theme.FloraGoldSoft
import com.example.ui.theme.FloraInk
import com.example.ui.theme.FloraLyricIdle
import com.example.ui.theme.FloraPlayerCream
import com.example.ui.theme.FloraTerracotta
import com.example.ui.theme.LocalAlbumPalette
import com.example.ui.theme.LocalThemeGradients
import com.example.ui.viewmodel.HerbariumUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private enum class PlayerTabMode { MUSICA, LETRAS }

@Composable
fun NowPlayingScreen(
  uiState: HerbariumUiState,
  audioSpectrumFlow: StateFlow<BotanicalAudioSpectrum> = MutableStateFlow(BotanicalAudioSpectrum.Idle),
  onClose: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onSeekTo: (Long) -> Unit,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  onToggleShuffle: () -> Unit,
  onCycleRepeat: () -> Unit,
  onToggleFavorite: (AudioTrack) -> Unit,
  onOpenEqualizer: () -> Unit,
  onOpenQueue: () -> Unit,
  onAddToPlaylist: (AudioTrack) -> Unit,
  onInspectSpecies: (BotanicalSpeciesId) -> Unit,
  onSelectPlayerTheme: (String) -> Unit,
) {
  BackHandler(onBack = onClose)

  val playback = uiState.playbackState
  val track = playback.currentTrack ?: uiState.tracks.firstOrNull()
  val species = BotanicalCatalog.getById(track?.speciesId ?: BotanicalSpeciesId.TIGER_LILY)
  val albumPalette = LocalAlbumPalette.current
  val themeGradients = LocalThemeGradients.current
  val chameleonOn = uiState.effectsState.chameleonThemeEnabled

  val activeTheme = remember(uiState.effectsState.playerThemeId, species.id) {
    if (uiState.effectsState.playerThemeId == "AUTO") {
      BotanicalPlayerTheme.forSpecies(species.id)
    } else {
      BotanicalPlayerTheme.fromId(uiState.effectsState.playerThemeId)
        ?: BotanicalPlayerTheme.forSpecies(species.id)
    }
  }

  val isAutoTheme = uiState.effectsState.playerThemeId == "AUTO"
  val baseForest by animateColorAsState(
    targetValue = if (chameleonOn && isAutoTheme) {
      lerp(themeGradients.playerTop, activeTheme.baseForestColor, 0.25f)
    } else {
      activeTheme.baseForestColor
    },
    animationSpec = tween(550),
    label = "playerBaseForest"
  )
  val midForest by animateColorAsState(
    targetValue = if (chameleonOn && isAutoTheme) {
      lerp(themeGradients.playerMiddle, activeTheme.deepForestColor, 0.25f)
    } else {
      lerp(activeTheme.baseForestColor, activeTheme.deepForestColor, 0.5f)
    },
    animationSpec = tween(550),
    label = "playerMidForest"
  )
  val deepForest by animateColorAsState(
    targetValue = if (chameleonOn && isAutoTheme) {
      lerp(themeGradients.playerBottom, activeTheme.deepForestColor, 0.22f)
    } else {
      activeTheme.deepForestColor
    },
    animationSpec = tween(550),
    label = "playerDeepForest"
  )
  val glowColor by animateColorAsState(
    targetValue = if (chameleonOn) lerp(themeGradients.playerGlow, albumPalette.vibrant, 0.45f) else activeTheme.glowColor,
    animationSpec = tween(550),
    label = "playerGlow"
  )

  var tabMode by remember { mutableStateOf(PlayerTabMode.MUSICA) }
  var showThemeGallery by remember { mutableStateOf(false) }
  var showLiveBotanicalDisc by remember { mutableStateOf(false) }
  var showTrackMenu by remember { mutableStateOf(false) }
  var isDraggingSlider by remember { mutableStateOf(false) }
  var sliderDragPosition by remember { mutableFloatStateOf(0f) }

  val durationMs = playback.durationMs.coerceAtLeast(1L)
  val currentPositionMs = if (isDraggingSlider) {
    sliderDragPosition.toLong()
  } else {
    playback.positionMs.coerceIn(0L, durationMs)
  }
  val progressFraction = (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

  val verses = remember(track?.id, track?.speciesId) {
    BotanicalLyricsCatalog.getLyricsForTrack(track)
  }
  val activeVerseIndex = remember(progressFraction, verses.size) {
    if (verses.isEmpty()) 0
    else (progressFraction * verses.size).toInt().coerceIn(0, verses.lastIndex)
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Brush.verticalGradient(listOf(baseForest, midForest, deepForest)))
  ) {
    Image(
      painter = painterResource(id = activeTheme.backdropResId),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .fillMaxSize()
        .alpha(0.22f)
    )

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(
              baseForest.copy(alpha = 0.62f),
              midForest.copy(alpha = 0.74f),
              deepForest.copy(alpha = 0.90f)
            )
          )
        )
    )
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.radialGradient(
            colors = listOf(glowColor.copy(alpha = 0.30f), Color.Transparent)
          )
        )
    )

    BotanicalSpecimenIllustration(
      speciesId = species.id,
      lockedSketchMode = true,
      modifier = Modifier
        .align(Alignment.Center)
        .size(310.dp)
        .alpha(0.08f)
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Top Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onClose,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("now_playing_close")
        ) {
          Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = "Minimizar",
            tint = FloraPlayerCream
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "Música",
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = if (tabMode == PlayerTabMode.MUSICA) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (tabMode == PlayerTabMode.MUSICA) FloraGoldSoft else FloraLyricIdle,
            modifier = Modifier
              .clip(FloraChipShape)
              .clickable { tabMode = PlayerTabMode.MUSICA }
              .padding(horizontal = 10.dp, vertical = 10.dp)
              .testTag("player_tab_musica")
          )
          Box(
            modifier = Modifier
              .width(1.dp)
              .height(14.dp)
              .background(FloraLyricIdle.copy(alpha = 0.45f))
          )
          Text(
            text = "Letras",
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = if (tabMode == PlayerTabMode.LETRAS) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (tabMode == PlayerTabMode.LETRAS) FloraGoldSoft else FloraLyricIdle,
            modifier = Modifier
              .clip(FloraChipShape)
              .clickable { tabMode = PlayerTabMode.LETRAS }
              .padding(horizontal = 10.dp, vertical = 10.dp)
              .testTag("player_tab_letras")
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = { showThemeGallery = !showThemeGallery },
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("now_playing_theme_picker")
          ) {
            Icon(
              Icons.Filled.Palette,
              contentDescription = "Temas",
              tint = if (showThemeGallery) FloraGoldSoft else FloraPlayerCream
            )
          }
          IconButton(
            onClick = onOpenEqualizer,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("now_playing_open_eq")
          ) {
            Icon(
              Icons.Filled.Tune,
              contentDescription = "Ecualizador",
              tint = FloraPlayerCream
            )
          }
        }
      }

      AnimatedVisibility(
        visible = showThemeGallery,
        enter = expandVertically(),
        exit = shrinkVertically()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          val isAuto = uiState.effectsState.playerThemeId == "AUTO"
          ThemeAutoSwatchChip(
            isSelected = isAuto,
            onClick = { onSelectPlayerTheme("AUTO") }
          )
          BotanicalPlayerTheme.entries.forEach { theme ->
            val selected = uiState.effectsState.playerThemeId == theme.id
            ThemePaintingSwatchChip(
              theme = theme,
              isSelected = selected,
              onClick = { onSelectPlayerTheme(theme.id) }
            )
          }
        }
      }

      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
        if (tabMode == PlayerTabMode.MUSICA) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Spacer(modifier = Modifier.height(8.dp))

            RadialBotanicalDisc(
              track = track,
              speciesId = species.id,
              isPlaying = playback.isPlaying,
              showLiveDrawing = showLiveBotanicalDisc,
              audioSpectrumFlow = audioSpectrumFlow,
              onToggleDiscMode = { showLiveBotanicalDisc = !showLiveBotanicalDisc }
            )

            Spacer(modifier = Modifier.height(30.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Text(
                  text = track?.title ?: "Sin pista",
                  style = MaterialTheme.typography.headlineLarge,
                  color = FloraPlayerCream,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "${track?.artist ?: "Flora"} · ${species.scientificName.substringBefore(" (")}",
                  style = MaterialTheme.typography.bodyMedium,
                  color = FloraDarkMeta,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = { track?.let(onToggleFavorite) },
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("now_playing_favorite")
                ) {
                  val fav = track?.isFavorite == true
                  FloraFlowerGlyph(
                    color = if (fav) FloraGoldSoft else FloraDarkMeta,
                    filled = fav,
                    centerColor = deepForest,
                    modifier = Modifier.size(22.dp)
                  )
                }

                Box {
                  IconButton(
                    onClick = { showTrackMenu = true },
                    modifier = Modifier
                      .minimumInteractiveComponentSize()
                      .testTag("now_playing_more")
                  ) {
                    Icon(
                      Icons.Filled.MoreVert,
                      contentDescription = "Más opciones",
                      tint = FloraPlayerCream
                    )
                  }
                  DropdownMenu(
                    expanded = showTrackMenu,
                    onDismissRequest = { showTrackMenu = false }
                  ) {
                    DropdownMenuItem(
                      text = { Text("Cola") },
                      leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                      onClick = {
                        showTrackMenu = false
                        onOpenQueue()
                      }
                    )
                    DropdownMenuItem(
                      text = { Text("Añadir a colección") },
                      leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                      onClick = {
                        showTrackMenu = false
                        track?.let(onAddToPlaylist)
                      }
                    )
                    DropdownMenuItem(
                      text = { Text("Ver planta") },
                      leadingIcon = { Icon(Icons.Filled.LocalFlorist, contentDescription = null) },
                      onClick = {
                        showTrackMenu = false
                        onInspectSpecies(species.id)
                      }
                    )
                  }
                }
              }
            }
          }
        } else {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
          ) {
            verses.forEachIndexed { idx, line ->
              val isActive = idx == activeVerseIndex
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(FloraChipShape)
                  .clickable {
                    val seekTarget = (durationMs * idx / verses.size.coerceAtLeast(1))
                    onSeekTo(seekTarget)
                  }
                  .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.Top
              ) {
                Box(
                  modifier = Modifier
                    .padding(top = 11.dp, end = 10.dp)
                    .size(width = 4.dp, height = if (isActive) 18.dp else 6.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isActive) FloraGoldSoft else Color.Transparent)
                )
                Text(
                  text = line,
                  style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 29.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                  ),
                  color = if (isActive) FloraPlayerCream else FloraLyricIdle.copy(alpha = 0.84f)
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Slider(
        value = currentPositionMs.toFloat(),
        onValueChange = {
          isDraggingSlider = true
          sliderDragPosition = it
        },
        onValueChangeFinished = {
          onSeekTo(sliderDragPosition.toLong())
          isDraggingSlider = false
        },
        valueRange = 0f..durationMs.toFloat(),
        colors = SliderDefaults.colors(
          thumbColor = FloraPlayerCream,
          activeTrackColor = FloraGoldSoft,
          inactiveTrackColor = Color.White.copy(alpha = 0.22f)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .semantics {
            contentDescription = "${formatDuration(currentPositionMs)} de ${formatDuration(durationMs)}"
          }
          .testTag("now_playing_seekbar")
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = formatDuration(currentPositionMs),
          style = MaterialTheme.typography.bodySmall,
          color = FloraDarkMeta
        )
        Text(
          text = formatDuration(durationMs),
          style = MaterialTheme.typography.bodySmall,
          color = FloraDarkMeta
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onToggleShuffle,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("now_playing_shuffle")
        ) {
          Icon(
            Icons.Filled.Shuffle,
            contentDescription = "Aleatorio",
            tint = if (playback.shuffleEnabled) FloraGoldSoft else FloraDarkMeta
          )
        }

        IconButton(
          onClick = onPrevious,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("now_playing_previous")
        ) {
          Icon(
            Icons.Filled.SkipPrevious,
            contentDescription = "Anterior",
            tint = FloraPlayerCream,
            modifier = Modifier.size(32.dp)
          )
        }

        Surface(
          shape = CircleShape,
          color = FloraGoldSoft,
          border = BorderStroke(1.5.dp, FloraPlayerCream),
          modifier = Modifier
            .size(66.dp)
            .clip(CircleShape)
            .clickable(onClick = onTogglePlayPause)
            .testTag("now_playing_play_pause")
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
              contentDescription = if (playback.isPlaying) "Pausar" else "Reproducir",
              tint = FloraInk,
              modifier = Modifier.size(32.dp)
            )
          }
        }

        IconButton(
          onClick = onNext,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("now_playing_next")
        ) {
          Icon(
            Icons.Filled.SkipNext,
            contentDescription = "Siguiente",
            tint = FloraPlayerCream,
            modifier = Modifier.size(32.dp)
          )
        }

        IconButton(
          onClick = onCycleRepeat,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("now_playing_repeat")
        ) {
          Icon(
            imageVector = if (playback.repeatMode == RepeatModeOption.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
            contentDescription = "Repetir",
            tint = if (playback.repeatMode != RepeatModeOption.OFF) FloraGoldSoft else FloraDarkMeta
          )
        }
      }
    }
  }
}

@Composable
private fun RadialBotanicalDisc(
  track: AudioTrack?,
  speciesId: BotanicalSpeciesId,
  isPlaying: Boolean,
  showLiveDrawing: Boolean,
  audioSpectrumFlow: StateFlow<BotanicalAudioSpectrum>,
  onToggleDiscMode: () -> Unit,
) {
  val spectrum by audioSpectrumFlow.collectAsStateWithLifecycle()
  var previousTrack by remember { mutableStateOf<AudioTrack?>(null) }
  var outgoingTrack by remember { mutableStateOf<AudioTrack?>(null) }
  val dissolveAnim = remember { Animatable(1f) }

  LaunchedEffect(track?.id) {
    val prev = previousTrack
    if (prev != null && track != null && prev.id != track.id) {
      outgoingTrack = prev
      previousTrack = track
      dissolveAnim.snapTo(0f)
      dissolveAnim.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
      )
      outgoingTrack = null
    } else {
      previousTrack = track
      outgoingTrack = null
      dissolveAnim.snapTo(1f)
    }
  }

  val dissolveProgress = dissolveAnim.value
  val isDissolving = dissolveProgress < 0.995f

  Box(
    modifier = Modifier
      .size(286.dp)
      .clip(CircleShape)
      .clickable(onClick = onToggleDiscMode)
      .testTag("now_playing_disc"),
    contentAlignment = Alignment.Center
  ) {
    BotanicalFrequencyVisualizer(
      spectrum = spectrum,
      isPlaying = isPlaying,
      innerRadiusFraction = 192f / 286f,
      dissolveProgress = dissolveProgress,
      isDissolving = isDissolving,
      modifier = Modifier.fillMaxSize()
    )

    Box(
      modifier = Modifier
        .size(192.dp)
        .clip(CircleShape)
        .border(3.dp, FloraPlayerCream, CircleShape)
        .padding(4.dp)
        .clip(CircleShape)
        .border(1.5.dp, FloraGold.copy(alpha = 0.85f), CircleShape)
    ) {
      BotanicalDissolveArtworkLayer(
        outgoingTrack = outgoingTrack,
        incomingTrack = track,
        speciesId = speciesId,
        dissolveProgress = dissolveProgress,
        isDissolving = isDissolving,
        showLiveDrawing = showLiveDrawing,
        modifier = Modifier.fillMaxSize()
      )
    }
  }
}

@Composable
private fun ThemeAutoSwatchChip(
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    shape = FloraChipShape,
    color = if (isSelected) FloraTerracotta else Color.Black.copy(alpha = 0.30f),
    border = BorderStroke(1.dp, if (isSelected) FloraGoldSoft else FloraGoldSoft.copy(alpha = 0.45f)),
    modifier = Modifier
      .clip(FloraChipShape)
      .clickable(onClick = onClick)
      .testTag("player_theme_auto")
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      FloraFlowerGlyph(
        color = FloraGoldSoft,
        filled = isSelected,
        centerColor = FloraTerracotta,
        modifier = Modifier.size(16.dp)
      )
      Text(
        text = "Auto",
        style = MaterialTheme.typography.labelLarge,
        color = FloraPlayerCream
      )
    }
  }
}

@Composable
private fun ThemePaintingSwatchChip(
  theme: BotanicalPlayerTheme,
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    shape = FloraChipShape,
    color = if (isSelected) FloraTerracotta else Color.Black.copy(alpha = 0.30f),
    border = BorderStroke(
      width = if (isSelected) 1.5.dp else 1.dp,
      color = if (isSelected) FloraGoldSoft else FloraPlayerCream.copy(alpha = 0.35f)
    ),
    modifier = Modifier
      .clip(FloraChipShape)
      .clickable(onClick = onClick)
      .testTag("player_theme_${theme.id}")
  ) {
    Row(
      modifier = Modifier.padding(start = 6.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Image(
        painter = painterResource(id = theme.backdropResId),
        contentDescription = theme.displayName,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .size(26.dp)
          .clip(CircleShape)
          .border(1.dp, FloraGoldSoft, CircleShape)
      )
      Text(
        text = theme.displayName,
        style = MaterialTheme.typography.bodySmall.copy(
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        ),
        color = FloraPlayerCream,
        maxLines = 1
      )
    }
  }
}
