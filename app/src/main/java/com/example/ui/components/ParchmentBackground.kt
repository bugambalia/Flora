package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
  val baseBg = MaterialTheme.colorScheme.background
  val bgBrush = if (chameleonEnabled) {
    Brush.verticalGradient(
      colors = listOf(
        gradients.parchmentTop,
        lerp(gradients.parchmentTop, gradients.parchmentBottom, 0.52f),
        gradients.parchmentBottom
      )
    )
  } else {
    Brush.verticalGradient(colors = listOf(baseBg, baseBg))
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(bgBrush)
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      // Soft radial warmth matching Flora printed herbarium paper
      drawRect(
        brush = Brush.radialGradient(
          colors = listOf(
            if (chameleonEnabled) gradients.parchmentRadialSecondary else FloraGold.copy(alpha = 0.06f),
            Color.Transparent
          ),
          center = Offset(size.width * 0.16f, size.height * 0.14f),
          radius = size.maxDimension * 0.52f
        )
      )

      if (chameleonEnabled) {
        drawRect(
          brush = Brush.radialGradient(
            colors = listOf(
              gradients.parchmentRadialPrimary,
              palette.muted.copy(alpha = 0.06f),
              Color.Transparent
            ),
            center = Offset(size.width * 0.84f, size.height * 0.24f),
            radius = size.maxDimension * 0.62f
          )
        )
      }

      if (showSubtleDotGrid) {
        val spacing = 22.dp.toPx()
        var y = spacing
        while (y < size.height) {
          var x = spacing
          while (x < size.width) {
            drawCircle(
              color = FloraInk.copy(alpha = 0.08f),
              radius = 1.1f,
              center = Offset(x, y)
            )
            x += spacing
          }
          y += spacing
        }
      }

      // Faint botanical stem silhouette in lower-right margin (non-intrusive paper watermark)
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
      drawPath(
        path = stemPath,
        color = FloraSage.copy(alpha = 0.07f),
        style = Stroke(width = 1.5.dp.toPx())
      )
    }
    content()
  }
}
