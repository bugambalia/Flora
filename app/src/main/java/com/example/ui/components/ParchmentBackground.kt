package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.FloraGold
import com.example.ui.theme.FloraInk
import com.example.ui.theme.FloraSage
import com.example.ui.theme.LocalAlbumPalette
import com.example.ui.theme.LocalFloraColors
import com.example.ui.theme.LocalThemeGradients

@Composable
fun ParchmentBackground(
  modifier: Modifier = Modifier,
  chameleonEnabled: Boolean = true,
  showSubtleDotGrid: Boolean = false,
  content: @Composable BoxScope.() -> Unit,
) {
  val palette = LocalAlbumPalette.current
  val gradients = LocalThemeGradients.current
  val floraColors = LocalFloraColors.current
  val baseBg = floraColors.paper

  Box(
    modifier = modifier
      .fillMaxSize()
      .drawWithCache {
        val bgBrush = if (chameleonEnabled) {
          Brush.verticalGradient(
            colors = listOf(
              gradients.parchmentTop,
              lerp(gradients.parchmentTop, gradients.parchmentBottom, 0.52f),
              gradients.parchmentBottom
            )
          )
        } else {
          Brush.verticalGradient(colors = listOf(baseBg, floraColors.surfaceBgBottom))
        }

        val primaryRadial = Brush.radialGradient(
          colors = listOf(
            if (chameleonEnabled) gradients.parchmentRadialSecondary else floraColors.gold.copy(alpha = 0.06f),
            Color.Transparent
          ),
          center = Offset(size.width * 0.16f, size.height * 0.14f),
          radius = size.maxDimension * 0.52f
        )

        val secondaryRadial = if (chameleonEnabled) {
          Brush.radialGradient(
            colors = listOf(
              gradients.parchmentRadialPrimary,
              palette.muted.copy(alpha = 0.06f),
              Color.Transparent
            ),
            center = Offset(size.width * 0.84f, size.height * 0.24f),
            radius = size.maxDimension * 0.62f
          )
        } else {
          null
        }

        val stemPath = Path().apply {
          moveTo(size.width * 0.92f, size.height * 0.94f)
          cubicTo(
            size.width * 0.84f,
            size.height * 0.80f,
            size.width * 0.96f,
            size.height * 0.66f,
            size.width * 0.86f,
            size.height * 0.55f
          )
        }
        val stemStroke = Stroke(width = 1.5.dp.toPx())
        val stemColor = floraColors.sage.copy(alpha = 0.09f)
        val dotColor = floraColors.ink.copy(alpha = 0.08f)
        val dotSpacing = 22.dp.toPx()

        onDrawBehind {
          drawRect(brush = bgBrush)
          drawRect(brush = primaryRadial)
          secondaryRadial?.let { drawRect(brush = it) }

          if (showSubtleDotGrid) {
            var y = dotSpacing
            while (y < size.height) {
              var x = dotSpacing
              while (x < size.width) {
                drawCircle(
                  color = dotColor,
                  radius = 1.1f,
                  center = Offset(x, y)
                )
                x += dotSpacing
              }
              y += dotSpacing
            }
          }

          drawPath(
            path = stemPath,
            color = stemColor,
            style = stemStroke
          )
        }
      }
  ) {
    content()
  }
}
