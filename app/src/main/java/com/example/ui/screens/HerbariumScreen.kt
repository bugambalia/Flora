package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnatomicalCallout
import com.example.data.model.BotanicalCatalog
import com.example.data.model.BotanicalSpecies
import com.example.data.model.BotanicalSpeciesId
import com.example.ui.components.BotanicalSpecimenIllustration
import com.example.ui.components.DottedRuleDivider
import com.example.ui.components.FloraMountedPlate
import com.example.ui.components.FloraScreenHeader
import com.example.ui.components.MarginaliaKind
import com.example.ui.components.MarginaliaSketch
import com.example.ui.theme.FloraBorderInk
import com.example.ui.theme.FloraChipShape
import com.example.ui.theme.FloraCobalt
import com.example.ui.theme.FloraGold
import com.example.ui.theme.FloraHeroShape
import com.example.ui.theme.FloraInk
import com.example.ui.theme.FloraMetaGreen
import com.example.ui.theme.FloraPaper
import com.example.ui.theme.FloraPaperCard
import com.example.ui.theme.FloraPaperWarm
import com.example.ui.theme.FloraPlateShape
import com.example.ui.theme.FloraPlayerCream
import com.example.ui.theme.FloraShadowPaper
import com.example.ui.theme.FloraStripShape
import com.example.ui.theme.FloraTerracotta
import com.example.ui.theme.FloraThumbShape
import com.example.ui.viewmodel.HerbariumUiState

private enum class HerbariumFilter { ALL, UNLOCKED, LOCKED }

@Composable
fun HerbariumScreen(
  uiState: HerbariumUiState,
  inspectedSpeciesId: BotanicalSpeciesId?,
  onInspectSpecies: (BotanicalSpeciesId?) -> Unit,
  onPlayGenreForSpecies: (BotanicalSpecies) -> Unit,
  onPreviewPalette: (BotanicalSpeciesId) -> Unit,
  onBack: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  var filter by remember { mutableStateOf(HerbariumFilter.ALL) }

  val filteredSpecies = remember(uiState.botanicalProgress, filter) {
    BotanicalCatalog.allSpecies.filter { species ->
      val unlocked = uiState.botanicalProgress[species.id]?.isUnlocked == true
      when (filter) {
        HerbariumFilter.ALL -> true
        HerbariumFilter.UNLOCKED -> unlocked
        HerbariumFilter.LOCKED -> !unlocked
      }
    }
  }

  val inspectedSpecies = inspectedSpeciesId?.let { BotanicalCatalog.getById(it) }
  if (inspectedSpecies != null) {
    val plateNumber = (BotanicalCatalog.allSpecies.indexOfFirst { it.id == inspectedSpecies.id }.coerceAtLeast(0)) + 1
    val isUnlocked = uiState.botanicalProgress[inspectedSpecies.id]?.isUnlocked == true
    BackHandler { onInspectSpecies(null) }
    BotanicalSpecimenFolioView(
      plateNumber = plateNumber,
      species = inspectedSpecies,
      isUnlocked = isUnlocked,
      onBack = { onInspectSpecies(null) },
      onPlayAssociatedGenre = { onPlayGenreForSpecies(inspectedSpecies) },
      onApplyPalette = { onPreviewPalette(inspectedSpecies.id) },
      modifier = modifier.statusBarsPadding()
    )
    return
  }

  if (onBack != null) {
    BackHandler(onBack = onBack)
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .then(if (onBack != null) Modifier.statusBarsPadding() else Modifier)
  ) {
    val columns = if (maxWidth >= 700.dp) 3 else 2

    LazyVerticalGrid(
      columns = GridCells.Fixed(columns),
      contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 156.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      item(span = { GridItemSpan(columns) }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          FloraScreenHeader(
            title = "Herbario",
            sealText = "${uiState.unlockedCount}/${BotanicalCatalog.allSpecies.size}",
            onSealClick = { onBack?.invoke() },
            sealContentDescription = "Volver",
            trailingExtra = if (onBack != null) {
              {
                IconButton(
                  onClick = onBack,
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("herbarium_back_button")
                ) {
                  Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Volver",
                    tint = FloraInk,
                    modifier = Modifier.size(28.dp)
                  )
                }
              }
            } else null
          )

          LinearProgressIndicator(
            progress = { uiState.unlockedCount.toFloat() / BotanicalCatalog.allSpecies.size },
            modifier = Modifier
              .fillMaxWidth()
              .height(5.dp),
            color = FloraTerracotta,
            trackColor = FloraBorderInk.copy(alpha = 0.2f),
            strokeCap = StrokeCap.Butt
          )

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = filter == HerbariumFilter.ALL,
              onClick = { filter = HerbariumFilter.ALL },
              label = { Text("Todas") },
              shape = FloraChipShape,
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = FloraTerracotta,
                selectedLabelColor = FloraPlayerCream
              )
            )
            FilterChip(
              selected = filter == HerbariumFilter.UNLOCKED,
              onClick = { filter = HerbariumFilter.UNLOCKED },
              label = { Text("Florecidas (${uiState.unlockedCount})") },
              shape = FloraChipShape,
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = FloraTerracotta,
                selectedLabelColor = FloraPlayerCream
              )
            )
            FilterChip(
              selected = filter == HerbariumFilter.LOCKED,
              onClick = { filter = HerbariumFilter.LOCKED },
              label = { Text("Latentes") },
              shape = FloraChipShape,
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = FloraTerracotta,
                selectedLabelColor = FloraPlayerCream
              )
            )
          }
        }
      }

      itemsIndexed(filteredSpecies, key = { _, it -> it.id.id }) { idx, species ->
        val plateIdx = BotanicalCatalog.allSpecies.indexOfFirst { it.id == species.id }.coerceAtLeast(idx) + 1
        val progress = uiState.botanicalProgress[species.id]
        val isUnlocked = progress?.isUnlocked == true
        SpecimenGridCard(
          plateNumber = plateIdx,
          species = species,
          isUnlocked = isUnlocked,
          onClick = { onInspectSpecies(species.id) }
        )
      }
    }
  }
}

@Composable
private fun SpecimenGridCard(
  plateNumber: Int,
  species: BotanicalSpecies,
  isUnlocked: Boolean,
  onClick: () -> Unit,
) {
  val tint = if (isUnlocked) {
    lerp(FloraPaperCard, species.primaryColor, 0.06f)
  } else {
    FloraPaperWarm.copy(alpha = 0.65f)
  }

  FloraMountedPlate(
    shape = FloraPlateShape,
    containerColor = tint,
    borderColor = if (isUnlocked) FloraBorderInk else FloraBorderInk.copy(alpha = 0.35f),
    goldHairline = isUnlocked,
    shadowOffsetX = 3.dp,
    shadowOffsetY = 4.dp,
    shadowColor = FloraShadowPaper,
    onClick = onClick,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("specimen_card_${species.id.id}")
  ) {
    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "LÁM. %02d".format(plateNumber),
          style = MaterialTheme.typography.labelSmall,
          color = if (isUnlocked) FloraTerracotta else FloraMetaGreen
        )
        if (!isUnlocked) {
          Icon(
            Icons.Filled.Lock,
            contentDescription = "Latente",
            modifier = Modifier.size(13.dp),
            tint = FloraMetaGreen
          )
        } else {
          Text(
            text = species.associatedGenres.first().uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = FloraMetaGreen
          )
        }
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(0.95f)
          .clip(FloraThumbShape)
          .background(FloraPaper)
          .border(1.dp, FloraBorderInk.copy(alpha = 0.5f), FloraThumbShape)
      ) {
        BotanicalSpecimenIllustration(
          speciesId = species.id,
          lockedSketchMode = !isUnlocked,
          modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
        )
      }

      Text(
        text = species.commonName,
        style = MaterialTheme.typography.titleMedium,
        color = FloraInk,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = species.scientificName,
        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
        color = FloraMetaGreen,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      if (!isUnlocked) {
        Text(
          text = species.unlockConditionLabel,
          style = MaterialTheme.typography.labelSmall,
          color = FloraMetaGreen,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
private fun BotanicalSpecimenFolioView(
  plateNumber: Int,
  species: BotanicalSpecies,
  isUnlocked: Boolean,
  onBack: () -> Unit,
  onPlayAssociatedGenre: () -> Unit,
  onApplyPalette: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedCallout by remember(species.id) {
    mutableStateOf<AnatomicalCallout?>(species.callouts.firstOrNull())
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 18.dp, vertical = 10.dp)
      .navigationBarsPadding()
      .padding(bottom = 132.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        "LÁMINA %02d · ${species.family.uppercase()}".format(plateNumber),
        style = MaterialTheme.typography.labelMedium,
        color = FloraTerracotta
      )
      IconButton(
        onClick = onBack,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("close_specimen_folio")
      ) {
        Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = FloraInk)
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Top
    ) {
      Column(modifier = Modifier.weight(1.15f)) {
        Text(
          species.commonName,
          style = MaterialTheme.typography.displayLarge.copy(fontSize = 34.sp, lineHeight = 36.sp),
          color = FloraInk,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          species.coordinates,
          style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
          color = FloraMetaGreen
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
        Text(
          species.scientificName,
          style = MaterialTheme.typography.titleSmall.copy(fontStyle = FontStyle.Italic),
          color = FloraTerracotta,
          textAlign = TextAlign.End
        )
        Text(
          species.nativeRegion,
          style = MaterialTheme.typography.bodySmall,
          color = FloraMetaGreen,
          textAlign = TextAlign.End
        )
      }
    }

    DottedRuleDivider(color = FloraBorderInk)

    FloraMountedPlate(
      shape = FloraHeroShape,
      containerColor = FloraPaperCard,
      borderColor = FloraBorderInk,
      goldHairline = true,
      shadowOffsetX = 4.dp,
      shadowOffsetY = 5.dp,
      shadowColor = FloraShadowPaper
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top
        ) {
          Column(
            modifier = Modifier.width(106.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            Text(species.soilPh, style = MaterialTheme.typography.labelSmall, color = FloraInk)
            Text(species.heightSpec, style = MaterialTheme.typography.labelSmall, color = FloraInk)
            Text(species.tempSpec, style = MaterialTheme.typography.labelSmall, color = FloraInk)
            Text(species.humiditySpec, style = MaterialTheme.typography.labelSmall, color = FloraInk)
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MarginaliaSketch(
              kind = MarginaliaKind.UNIAXIAL_BRANCH,
              tintColor = species.foliageColor.copy(alpha = 0.55f),
              modifier = Modifier.size(36.dp)
            )
            MarginaliaSketch(
              kind = MarginaliaKind.SEED_SECTION,
              tintColor = species.primaryColor.copy(alpha = 0.45f),
              modifier = Modifier.size(36.dp)
            )
          }
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(FloraThumbShape)
            .background(FloraPaper)
            .border(1.dp, FloraBorderInk.copy(alpha = 0.65f), FloraThumbShape)
            .padding(8.dp)
        ) {
          BotanicalSpecimenIllustration(
            speciesId = species.id,
            lockedSketchMode = !isUnlocked,
            showCallouts = isUnlocked,
            selectedCalloutNumber = selectedCallout?.number,
            onSelectCallout = { selectedCallout = it },
            modifier = Modifier.fillMaxSize()
          )
          if (!isUnlocked) {
            Surface(
              shape = FloraStripShape,
              color = FloraPaper.copy(alpha = 0.95f),
              border = BorderStroke(1.dp, FloraBorderInk),
              modifier = Modifier.align(Alignment.Center)
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = FloraTerracotta)
                Text(
                  species.unlockConditionLabel,
                  style = MaterialTheme.typography.bodySmall,
                  color = FloraInk,
                  textAlign = TextAlign.Center
                )
              }
            }
          }
        }

        Text(
          species.associatedGenres.joinToString(" · "),
          style = MaterialTheme.typography.labelSmall,
          color = FloraTerracotta
        )
      }
    }

    if (isUnlocked && selectedCallout != null) {
      val c = selectedCallout!!
      FloraMountedPlate(
        shape = FloraStripShape,
        containerColor = FloraPaperWarm,
        borderColor = FloraGold,
        shadowOffsetX = 0.dp,
        shadowOffsetY = 0.dp
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = "${c.number}. ${c.label.uppercase()} — ${c.latinTerm}",
            style = MaterialTheme.typography.labelMedium,
            color = FloraTerracotta
          )
          Text(c.description, style = MaterialTheme.typography.bodyMedium, color = FloraInk)
        }
      }
    }

    FloraMountedPlate(
      shape = FloraStripShape,
      containerColor = FloraPaperCard,
      borderColor = FloraBorderInk.copy(alpha = 0.55f),
      shadowOffsetX = 2.dp,
      shadowOffsetY = 3.dp
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(species.description, style = MaterialTheme.typography.bodyLarge, color = FloraInk)
        DottedRuleDivider(color = FloraBorderInk.copy(alpha = 0.45f))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(Icons.Filled.LocalFlorist, contentDescription = null, tint = FloraTerracotta, modifier = Modifier.size(18.dp))
          Text(
            species.audioConnectionNote,
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
            color = FloraMetaGreen
          )
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Button(
        onClick = onPlayAssociatedGenre,
        shape = FloraStripShape,
        colors = ButtonDefaults.buttonColors(containerColor = FloraCobalt, contentColor = FloraPlayerCream),
        modifier = Modifier
          .weight(1f)
          .testTag("play_species_genre_${species.id.id}")
      ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null)
        Spacer(modifier = Modifier.width(6.dp))
        Text(species.associatedGenres.first(), style = MaterialTheme.typography.labelLarge)
      }
      OutlinedButton(
        onClick = onApplyPalette,
        shape = FloraStripShape,
        border = BorderStroke(1.dp, FloraBorderInk),
        modifier = Modifier.weight(1f)
      ) {
        Icon(Icons.Filled.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Paleta", style = MaterialTheme.typography.labelLarge)
      }
    }
  }
}
