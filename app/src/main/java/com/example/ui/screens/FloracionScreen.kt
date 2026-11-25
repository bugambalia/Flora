package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.BotanicalCatalog
import com.example.data.model.BotanicalSpeciesId
import com.example.data.model.PlaylistWithTracks
import com.example.ui.components.FloraFlowerGlyph
import com.example.ui.components.FloraMountedPlate
import com.example.ui.components.FloraScreenHeader
import com.example.ui.theme.FloraCardTopRightShape
import com.example.ui.theme.FloraHeroShape
import com.example.ui.theme.FloraInk
import com.example.ui.theme.FloraPlayerCream
import com.example.ui.theme.FloraStripShape
import com.example.ui.theme.LocalFloraColors
import com.example.ui.theme.LocalThemeGradients
import com.example.ui.viewmodel.HerbariumUiState

@Composable
fun FloracionScreen(
  uiState: HerbariumUiState,
  onPlayHero: () -> Unit,
  onOpenPlaylist: (PlaylistWithTracks) -> Unit,
  onPlayPlaylist: (PlaylistWithTracks) -> Unit,
  onOpenExplore: () -> Unit,
  onOpenHerbarium: () -> Unit,
  modifier: Modifier = Modifier,
  onOpenSettings: () -> Unit = {},
) {
  val colors = LocalFloraColors.current
  val themeGradients = LocalThemeGradients.current
  val ecosystems = uiState.playlists.take(3)
  val featuredSpecies = BotanicalCatalog.getById(
    uiState.playbackState.currentTrack?.speciesId ?: BotanicalSpeciesId.CAMELLIA
  )

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 156.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp)
  ) {
    item {
      FloraScreenHeader(
        title = "Floración",
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
    }

    // Hero Plate
    item {
      FloraMountedPlate(
        shape = FloraHeroShape,
        containerColor = colors.paper,
        borderColor = colors.borderInk,
        shadowColor = colors.shadowPaper,
        goldHairline = true,
        shadowOffsetX = 5.dp,
        shadowOffsetY = 6.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.radialGradient(
                colors = listOf(
                  themeGradients.heroGlowStart,
                  themeGradients.heroGlowEnd,
                  Color.Transparent
                )
              )
            )
            .padding(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .weight(0.45f)
                .height(198.dp)
                .clip(FloraCardTopRightShape)
                .border(1.dp, colors.borderInk, FloraCardTopRightShape)
            ) {
              Image(
                painter = painterResource(id = R.drawable.flora_poppies_cobalt),
                contentDescription = featuredSpecies.scientificName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .align(Alignment.BottomCenter)
                  .background(
                    Brush.verticalGradient(
                      colors = listOf(Color.Transparent, FloraInk.copy(alpha = 0.85f))
                    )
                  )
                  .padding(horizontal = 8.dp, vertical = 8.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(6.dp)
                      .clip(CircleShape)
                      .background(colors.gold)
                  )
                  Text(
                    text = featuredSpecies.scientificName.substringBefore(" ("),
                    style = MaterialTheme.typography.bodySmall,
                    color = FloraPlayerCream,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }

            Column(
              modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight(),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "LÁMINA DEL DÍA",
                  style = MaterialTheme.typography.labelMedium,
                  color = colors.terracotta,
                  maxLines = 1
                )
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = Color.Transparent,
                  border = BorderStroke(1.dp, colors.terracotta.copy(alpha = 0.65f))
                ) {
                  Text(
                    text = "FL",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.terracotta,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              Text(
                text = "Flores que también cantan",
                style = MaterialTheme.typography.headlineLarge,
                color = colors.ink
              )

              Text(
                text = "Paisajes sonoros de flora endémica.",
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = colors.heroSub
              )

              Spacer(modifier = Modifier.height(4.dp))

              Button(
                onClick = onPlayHero,
                shape = RoundedCornerShape(topStart = 2.dp, topEnd = 14.dp, bottomEnd = 2.dp, bottomStart = 14.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = colors.cobalt,
                  contentColor = FloraPlayerCream
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("hero_play_now_button")
              ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "ESCUCHAR AHORA",
                  style = MaterialTheme.typography.labelLarge,
                  maxLines = 1
                )
              }
            }
          }
        }
      }
    }

    // Ecosistemas Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "Ecosistemas",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.ink
          )
          FloraFlowerGlyph(modifier = Modifier.size(18.dp), color = colors.terracotta)
        }
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onOpenExplore)
            .padding(horizontal = 4.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "VER TODO",
            style = MaterialTheme.typography.labelMedium,
            color = colors.terracotta
          )
          Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Ver todo",
            tint = colors.terracotta,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }

    // 3 Ecosystem Cards Row
    if (ecosystems.isNotEmpty()) {
      item {
        val accents = listOf(colors.terracotta, colors.cobalt, colors.gold)
        val drawables = listOf(
          R.drawable.flora_protea_rainforest,
          R.drawable.flora_cattleya_mosaic,
          R.drawable.flora_lotus_pond
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          ecosystems.forEachIndexed { idx, pl ->
            val cardAccent = accents[idx % accents.size]
            val artRes = drawables[idx % drawables.size]
            Column(
              modifier = Modifier
                .weight(1f)
                .clickable { onOpenPlaylist(pl) }
                .testTag("ecosystem_card_${pl.playlist.id}"),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .aspectRatio(1f)
                  .clip(FloraCardTopRightShape)
                  .border(1.5.dp, cardAccent, FloraCardTopRightShape)
              ) {
                Image(
                  painter = painterResource(id = artRes),
                  contentDescription = pl.playlist.name,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize()
                )
                Box(
                  modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(cardAccent)
                    .border(1.dp, FloraPlayerCream, CircleShape)
                    .clickable { onPlayPlaylist(pl) },
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Reproducir ${pl.playlist.name}",
                    tint = FloraPlayerCream,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
              Text(
                text = pl.playlist.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "${pl.tracks.size} canciones",
                style = MaterialTheme.typography.bodySmall,
                color = colors.metaGreen
              )
            }
          }
        }
      }
    }

    // Nota de Herbario Quote Strip
    item {
      FloraMountedPlate(
        shape = FloraStripShape,
        containerColor = colors.paperWarm,
        borderColor = colors.borderInk.copy(alpha = 0.45f),
        shadowOffsetX = 0.dp,
        shadowOffsetY = 0.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = "NOTA DE HERBARIO",
              style = MaterialTheme.typography.labelMedium,
              color = colors.terracotta
            )
            Text(
              text = "“Del azul mosaico de los Andes al oro de la selva húmeda.”",
              style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, fontSize = 14.sp),
              color = colors.quoteInk
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, colors.terracotta.copy(alpha = 0.55f)),
            modifier = Modifier.size(28.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              FloraFlowerGlyph(modifier = Modifier.size(14.dp), color = colors.terracotta)
            }
          }
        }
      }
    }
  }
}
