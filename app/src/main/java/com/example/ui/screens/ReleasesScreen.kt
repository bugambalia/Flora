package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.model.ReleaseCatalog
import com.example.data.model.ReleaseChangeGroup
import com.example.data.model.ReleaseChangeType
import com.example.data.model.ReleaseEntry
import com.example.ui.components.DottedRuleDivider
import com.example.ui.components.FloraFlowerGlyph
import com.example.ui.components.FloraMountedPlate
import com.example.ui.components.ParchmentBackground
import com.example.ui.theme.FloraChipShape
import com.example.ui.theme.FloraPlayerCream
import com.example.ui.theme.FloraPlateShape
import com.example.ui.theme.FloraStripShape
import com.example.ui.theme.LocalFloraColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReleasesOverlayScreen(
  chameleonEnabled: Boolean,
  onOpenExternalUrl: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)
  val colors = LocalFloraColors.current
  var selectedFilter by remember { mutableStateOf<ReleaseChangeType?>(null) }

  val filteredReleases = remember(selectedFilter) {
    val filter = selectedFilter
    if (filter == null) {
      ReleaseCatalog.entries
    } else {
      ReleaseCatalog.entries.mapNotNull { entry ->
        val matchingGroups = entry.groups.filter { it.type == filter }
        if (matchingGroups.isEmpty()) null else entry.copy(groups = matchingGroups)
      }
    }
  }

  ParchmentBackground(chameleonEnabled = chameleonEnabled) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("releases_close_button")
          ) {
            Icon(
              Icons.AutoMirrored.Filled.KeyboardArrowLeft,
              contentDescription = "Volver a Ajustes",
              tint = colors.ink,
              modifier = Modifier.size(28.dp)
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "Releases & Cambios",
              style = MaterialTheme.typography.displayLarge.copy(fontSize = 27.sp),
              color = colors.ink
            )
            FloraFlowerGlyph(
              modifier = Modifier.size(20.dp),
              color = colors.terracotta,
              centerColor = colors.gold
            )
          }

          Spacer(modifier = Modifier.size(48.dp))
        }
      }

      // Installed build & GitHub portal links card
      item {
        FloraMountedPlate(
          shape = FloraPlateShape,
          containerColor = colors.paperCard,
          borderColor = colors.borderInk,
          goldHairline = true,
          shadowOffsetX = 3.dp,
          shadowOffsetY = 4.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "VERSIÓN INSTALADA",
                  style = MaterialTheme.typography.labelMedium,
                  color = colors.terracotta
                )
                Text(
                  text = "Flora v${BuildConfig.VERSION_NAME}",
                  style = MaterialTheme.typography.headlineMedium,
                  color = colors.ink
                )
                Text(
                  text = "Build ${BuildConfig.VERSION_CODE} · ${BuildConfig.APPLICATION_ID}",
                  style = MaterialTheme.typography.labelSmall,
                  color = colors.metaGreen
                )
              }
              Surface(
                shape = FloraChipShape,
                color = colors.sageLight,
                border = BorderStroke(1.dp, colors.borderInk.copy(alpha = 0.5f))
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = colors.forest,
                    modifier = Modifier.size(15.dp)
                  )
                  Text(
                    text = "100% Local",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.ink
                  )
                }
              }
            }

            DottedRuleDivider(color = colors.borderInk.copy(alpha = 0.35f))

            Text(
              text = "Publicación y descargas en GitHub",
              style = MaterialTheme.typography.titleSmall,
              color = colors.ink
            )

            Button(
              onClick = { onOpenExternalUrl(ReleaseCatalog.GITHUB_PAGES_URL) },
              shape = FloraStripShape,
              colors = ButtonDefaults.buttonColors(
                containerColor = colors.terracotta,
                contentColor = FloraPlayerCream
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("releases_open_pages_button")
            ) {
              Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Abrir página de Releases (GitHub Pages)", style = MaterialTheme.typography.labelLarge)
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = { onOpenExternalUrl(ReleaseCatalog.GITHUB_RELEASES_URL) },
                shape = FloraStripShape,
                border = BorderStroke(1.dp, colors.borderInk),
                modifier = Modifier
                  .weight(1f)
                  .testTag("releases_open_github_button")
              ) {
                Icon(Icons.Filled.NewReleases, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.forest)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Releases APK", style = MaterialTheme.typography.labelLarge, color = colors.ink)
              }

              OutlinedButton(
                onClick = { onOpenExternalUrl(ReleaseCatalog.GITHUB_CHANGELOG_URL) },
                shape = FloraStripShape,
                border = BorderStroke(1.dp, colors.borderInk),
                modifier = Modifier
                  .weight(1f)
                  .testTag("releases_open_changelog_button")
              ) {
                Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.cobalt)
                Spacer(modifier = Modifier.width(6.dp))
                Text("CHANGELOG", style = MaterialTheme.typography.labelLarge, color = colors.ink)
              }
            }
          }
        }
      }

      // Filter row
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = selectedFilter == null,
            onClick = { selectedFilter = null },
            label = { Text("Todas") },
            shape = FloraChipShape,
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = colors.forest,
              selectedLabelColor = FloraPlayerCream
            ),
            modifier = Modifier.testTag("release_filter_all")
          )
          ReleaseChangeType.entries.forEach { type ->
            FilterChip(
              selected = selectedFilter == type,
              onClick = { selectedFilter = type },
              label = { Text(type.label) },
              shape = FloraChipShape,
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = colors.terracotta,
                selectedLabelColor = FloraPlayerCream
              ),
              modifier = Modifier.testTag("release_filter_${type.name.lowercase()}")
            )
          }
        }
      }

      // Version history cards
      items(filteredReleases, key = { it.tag }) { entry ->
        ReleaseHistoryCard(
          entry = entry,
          onOpenRelease = {
            val targetUrl = if (entry.tag.endsWith("-dev")) {
              ReleaseCatalog.GITHUB_CHANGELOG_URL
            } else {
              "${ReleaseCatalog.GITHUB_RELEASES_URL}/tag/${entry.tag}"
            }
            onOpenExternalUrl(targetUrl)
          }
        )
      }

      item {
        Spacer(modifier = Modifier.size(16.dp))
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReleaseHistoryCard(
  entry: ReleaseEntry,
  onOpenRelease: () -> Unit,
) {
  val colors = LocalFloraColors.current
  FloraMountedPlate(
    shape = FloraStripShape,
    containerColor = colors.paperCard,
    borderColor = colors.borderInk.copy(alpha = 0.55f),
    shadowOffsetX = 0.dp,
    shadowOffsetY = 0.dp,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("release_card_${entry.tag}")
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
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
          Surface(
            shape = FloraChipShape,
            color = if (entry.isLatest) colors.terracotta else colors.sageLight
          ) {
            Text(
              text = entry.tag,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = if (entry.isLatest) FloraPlayerCream else colors.ink,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }
          if (entry.isLatest) {
            Surface(
              shape = FloraChipShape,
              color = colors.forest
            ) {
              Text(
                text = "PUBLICADA",
                style = MaterialTheme.typography.labelSmall,
                color = FloraPlayerCream,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }

        Text(
          text = entry.date,
          style = MaterialTheme.typography.labelSmall,
          color = colors.metaGreen
        )
      }

      Text(
        text = entry.title,
        style = MaterialTheme.typography.titleLarge,
        color = colors.ink
      )

      Text(
        text = entry.summary,
        style = MaterialTheme.typography.bodySmall,
        color = colors.metaGreen
      )

      DottedRuleDivider(color = colors.borderInk.copy(alpha = 0.35f))

      entry.groups.forEach { group ->
        ReleaseChangeGroupBlock(group = group)
      }

      if (entry.artifacts.isNotEmpty()) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          entry.artifacts.forEach { artifact ->
            Box(
              modifier = Modifier
                .background(colors.paper, FloraChipShape)
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = artifact,
                style = MaterialTheme.typography.labelSmall,
                color = colors.cobalt
              )
            }
          }
        }
      }

      OutlinedButton(
        onClick = onOpenRelease,
        shape = FloraStripShape,
        border = BorderStroke(1.dp, colors.borderInk.copy(alpha = 0.65f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(
          Icons.AutoMirrored.Filled.OpenInNew,
          contentDescription = null,
          tint = colors.terracotta,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (entry.tag.endsWith("-dev")) "Ver notas en CHANGELOG.md" else "Ver ${entry.tag} en GitHub",
          style = MaterialTheme.typography.labelLarge,
          color = colors.ink
        )
      }
    }
  }
}

@Composable
private fun ReleaseChangeGroupBlock(group: ReleaseChangeGroup) {
  val colors = LocalFloraColors.current
  val accentColor = when (group.type) {
    ReleaseChangeType.ADDED -> colors.forest
    ReleaseChangeType.IMPROVED -> colors.cobalt
    ReleaseChangeType.FIXED -> colors.terracotta
    ReleaseChangeType.TECHNICAL -> colors.sage
  }

  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Surface(
      shape = FloraChipShape,
      color = accentColor.copy(alpha = 0.14f)
    ) {
      Text(
        text = group.type.label.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = accentColor,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
      )
    }

    group.items.forEach { item ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = "•",
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
          color = accentColor
        )
        Text(
          text = item,
          style = MaterialTheme.typography.bodySmall,
          color = colors.ink
        )
      }
    }
  }
}
