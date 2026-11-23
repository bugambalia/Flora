package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnatomicalCallout
import com.example.data.model.BotanicalCatalog
import com.example.data.model.BotanicalSpeciesId
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun BotanicalSpecimenIllustration(
  speciesId: BotanicalSpeciesId,
  modifier: Modifier = Modifier,
  lockedSketchMode: Boolean = false,
  accentOverride: Color? = null,
  showCallouts: Boolean = false,
  selectedCalloutNumber: Int? = null,
  onSelectCallout: ((AnatomicalCallout) -> Unit)? = null,
) {
  val species = BotanicalCatalog.getById(speciesId)
  val inkColor = MaterialTheme.colorScheme.onBackground.copy(
    alpha = if (lockedSketchMode) 0.36f else 0.88f
  )
  val petalPrimary = if (lockedSketchMode) {
    inkColor.copy(alpha = 0.08f)
  } else {
    accentOverride ?: species.primaryColor
  }
  val petalSecondary = if (lockedSketchMode) {
    inkColor.copy(alpha = 0.05f)
  } else {
    species.secondaryColor
  }
  val leafColor = if (lockedSketchMode) {
    inkColor.copy(alpha = 0.07f)
  } else {
    species.foliageColor
  }
  val surfaceBg = MaterialTheme.colorScheme.surface
  val strokeScale by animateFloatAsState(
    targetValue = if (lockedSketchMode) 0.7f else 1f,
    animationSpec = tween(400),
    label = "strokeScale"
  )

  BoxWithConstraints(
    modifier = modifier.semantics {
      contentDescription = "Ilustración botánica de ${species.commonName} (${species.scientificName})"
    }
  ) {
    val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
    val heightPx = with(LocalDensity.current) { maxHeight.toPx() }

    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      val stroke = (w.coerceAtMost(h) * 0.009f).coerceIn(1.5f, 4.2f) * strokeScale

      when (speciesId) {
        BotanicalSpeciesId.TIGER_LILY ->
          drawTigerLily(w, h, stroke, inkColor, petalPrimary, petalSecondary, leafColor, lockedSketchMode)
        BotanicalSpeciesId.MONSTERA ->
          drawMonstera(w, h, stroke, inkColor, petalPrimary, leafColor, surfaceBg, lockedSketchMode)
        BotanicalSpeciesId.GINKGO ->
          drawGinkgo(w, h, stroke, inkColor, petalPrimary, petalSecondary, leafColor, lockedSketchMode)
        BotanicalSpeciesId.LAVENDER ->
          drawLavender(w, h, stroke, inkColor, petalPrimary, petalSecondary, leafColor, lockedSketchMode)
        BotanicalSpeciesId.LOTUS ->
          drawLotus(w, h, stroke, inkColor, petalPrimary, petalSecondary, leafColor, lockedSketchMode)
        BotanicalSpeciesId.FERN ->
          drawFern(w, h, stroke, inkColor, petalPrimary, leafColor, lockedSketchMode)
        BotanicalSpeciesId.PASSIFLORA ->
          drawPassiflora(w, h, stroke, inkColor, petalPrimary, petalSecondary, leafColor, lockedSketchMode)
        BotanicalSpeciesId.CAMELLIA ->
          drawCamellia(w, h, stroke, inkColor, petalPrimary, petalSecondary, leafColor, lockedSketchMode)
      }

      if (showCallouts && !lockedSketchMode) {
        val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        species.callouts.forEach { callout ->
          val px = callout.xFraction * w
          val py = callout.yFraction * h
          val anchorX = if (callout.labelOnLeft) (px - w * 0.12f).coerceAtLeast(w * 0.08f)
          else (px + w * 0.12f).coerceAtMost(w * 0.92f)
          val isSelected = selectedCalloutNumber == callout.number
          drawLine(
            color = if (isSelected) petalPrimary else inkColor.copy(alpha = 0.55f),
            start = Offset(px, py),
            end = Offset(anchorX, py - h * 0.02f),
            strokeWidth = if (isSelected) stroke * 0.95f else stroke * 0.65f,
            pathEffect = dash
          )
          drawCircle(
            color = if (isSelected) petalPrimary else inkColor,
            radius = stroke * 1.4f,
            center = Offset(px, py)
          )
        }
      }
    }

    if (showCallouts && !lockedSketchMode && onSelectCallout != null) {
      val pinSizeDp = 28.dp
      val pinHalfPx = with(LocalDensity.current) { (pinSizeDp / 2).toPx() }
      species.callouts.forEach { callout ->
        val px = callout.xFraction * widthPx
        val py = callout.yFraction * heightPx
        val anchorX = if (callout.labelOnLeft) (px - widthPx * 0.12f).coerceAtLeast(widthPx * 0.08f)
        else (px + widthPx * 0.12f).coerceAtMost(widthPx * 0.92f)
        val anchorY = py - heightPx * 0.02f
        val isSelected = selectedCalloutNumber == callout.number

        Box(
          modifier = Modifier
            .offset {
              IntOffset(
                (anchorX - pinHalfPx).roundToInt(),
                (anchorY - pinHalfPx).roundToInt()
              )
            }
            .size(48.dp)
            .clip(CircleShape)
            .clickable { onSelectCallout(callout) }
            .testTag("callout_pin_${species.id.id}_${callout.number}"),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .size(pinSizeDp)
              .clip(CircleShape)
              .background(if (isSelected) petalPrimary else MaterialTheme.colorScheme.surface)
              .border(
                width = 1.5.dp,
                color = if (isSelected) petalSecondary else inkColor,
                shape = CircleShape
              ),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = callout.number.toString(),
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
              ),
              color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }
  }
}

private fun DrawScope.drawTigerLily(
  w: Float,
  h: Float,
  stroke: Float,
  ink: Color,
  petalPrimary: Color,
  petalSecondary: Color,
  leaf: Color,
  sketch: Boolean,
) {
  val cx = w * 0.50f
  val baseY = h * 0.80f

  // Fibrous roots at crown
  val rootOffsets = listOf(-0.12f, -0.07f, -0.03f, 0.02f, 0.07f, 0.13f)
  rootOffsets.forEachIndexed { idx, dx ->
    val rPath = Path().apply {
      moveTo(cx, baseY)
      quadraticTo(
        cx + w * dx * 0.7f,
        baseY + h * (0.06f + (idx % 2) * 0.03f),
        cx + w * dx,
        baseY + h * (0.12f + (idx % 3) * 0.02f)
      )
    }
    drawPath(rPath, ink.copy(alpha = 0.75f), style = Stroke(stroke * 0.9f, cap = StrokeCap.Round))
  }

  // Main stem
  val stemPath = Path().apply {
    moveTo(cx, baseY)
    cubicTo(cx - w * 0.04f, h * 0.62f, cx + w * 0.02f, h * 0.45f, cx - w * 0.04f, h * 0.24f)
  }
  drawPath(stemPath, leaf, style = Stroke(stroke * 2.1f, cap = StrokeCap.Round))
  drawPath(stemPath, ink, style = Stroke(stroke * 1.1f, cap = StrokeCap.Round))

  // Lanceolate leaves along stem
  val leafSpecs = listOf(
    Triple(0.70f, -1f, 0.24f),
    Triple(0.64f, 1f, 0.27f),
    Triple(0.55f, -1f, 0.21f),
    Triple(0.49f, 1f, 0.20f),
  )
  leafSpecs.forEach { (yFrac, dir, len) ->
    val sy = h * yFrac
    val sx = cx - w * 0.01f
    val lPath = Path().apply {
      moveTo(sx, sy)
      cubicTo(sx + dir * w * len * 0.5f, sy - h * 0.08f, sx + dir * w * len, sy - h * 0.02f, sx + dir * w * len * 1.08f, sy + h * 0.04f)
      cubicTo(sx + dir * w * len * 0.6f, sy + h * 0.03f, sx + dir * w * len * 0.25f, sy + h * 0.02f, sx, sy + h * 0.015f)
      close()
    }
    drawPath(lPath, leaf)
    drawPath(lPath, ink, style = Stroke(stroke * 0.9f, join = StrokeJoin.Round))
    drawLine(
      ink.copy(alpha = 0.5f),
      Offset(sx, sy + h * 0.005f),
      Offset(sx + dir * w * len * 0.82f, sy + h * 0.01f),
      strokeWidth = stroke * 0.45f
    )
  }

  // Axillary dark bulbils on stem
  if (!sketch) {
    listOf(0.50f, 0.57f).forEach { by ->
      drawCircle(petalSecondary.copy(alpha = 0.9f), radius = stroke * 2.0f, center = Offset(cx - w * 0.01f, h * by))
      drawCircle(ink, radius = stroke * 2.0f, center = Offset(cx - w * 0.01f, h * by), style = Stroke(stroke * 0.6f))
    }
  }

  // Secondary unopened bud on right branch
  val budOrigin = Offset(cx - w * 0.02f, h * 0.34f)
  val budTip = Offset(cx + w * 0.16f, h * 0.19f)
  drawLine(leaf, budOrigin, budTip, strokeWidth = stroke * 1.3f, cap = StrokeCap.Round)
  drawLine(ink, budOrigin, budTip, strokeWidth = stroke * 0.8f, cap = StrokeCap.Round)
  val budPath = Path().apply {
    moveTo(budTip.x - w * 0.02f, budTip.y + h * 0.02f)
    cubicTo(budTip.x + w * 0.03f, budTip.y - h * 0.07f, budTip.x + w * 0.08f, budTip.y - h * 0.08f, budTip.x + w * 0.08f, budTip.y - h * 0.03f)
    cubicTo(budTip.x + w * 0.07f, budTip.y + h * 0.02f, budTip.x + w * 0.02f, budTip.y + h * 0.04f, budTip.x - w * 0.02f, budTip.y + h * 0.02f)
    close()
  }
  drawPath(budPath, petalPrimary)
  drawPath(budPath, ink, style = Stroke(stroke * 0.85f))

  // Main Tiger Lily Blossom (6 recurved speckled tepals)
  val flowerCenter = Offset(cx - w * 0.09f, h * 0.30f)
  val angles = listOf(-145f, -95f, -45f, 15f, 70f, 130f)
  angles.forEachIndexed { idx, deg ->
    rotate(deg, pivot = flowerCenter) {
      val pLen = w * 0.18f
      val pWid = w * 0.046f
      val tepal = Path().apply {
        moveTo(flowerCenter.x, flowerCenter.y)
        cubicTo(
          flowerCenter.x + pLen * 0.35f, flowerCenter.y - pWid,
          flowerCenter.x + pLen * 0.82f, flowerCenter.y - pWid * 0.7f,
          flowerCenter.x + pLen, flowerCenter.y + pWid * 0.25f
        )
        cubicTo(
          flowerCenter.x + pLen * 0.72f, flowerCenter.y + pWid * 0.9f,
          flowerCenter.x + pLen * 0.30f, flowerCenter.y + pWid * 0.8f,
          flowerCenter.x, flowerCenter.y
        )
        close()
      }
      drawPath(tepal, if (idx % 2 == 0) petalPrimary else petalPrimary.copy(alpha = 0.86f))
      drawPath(tepal, ink, style = Stroke(stroke * 0.95f, join = StrokeJoin.Round))
      drawLine(
        ink.copy(alpha = 0.45f),
        flowerCenter,
        Offset(flowerCenter.x + pLen * 0.78f, flowerCenter.y + pWid * 0.08f),
        strokeWidth = stroke * 0.45f
      )
      // Tiger lily spots
      if (!sketch) {
        listOf(0.32f, 0.46f, 0.56f, 0.40f).forEachIndexed { sIdx, f ->
          val sy = if (sIdx % 2 == 0) -pWid * 0.25f else pWid * 0.22f
          drawCircle(
            color = ink.copy(alpha = 0.78f),
            radius = stroke * 0.75f,
            center = Offset(flowerCenter.x + pLen * f, flowerCenter.y + sy)
          )
        }
      }
    }
  }

  // Protruding stamens & anthers + central pistil
  val stamenAngles = listOf(-170f, -152f, -135f, -118f, -100f)
  stamenAngles.forEach { deg ->
    val rad = deg * PI / 180.0
    val ex = flowerCenter.x + (w * 0.14f * cos(rad)).toFloat()
    val ey = flowerCenter.y + (w * 0.14f * sin(rad)).toFloat()
    drawLine(ink, flowerCenter, Offset(ex, ey), strokeWidth = stroke * 0.6f, cap = StrokeCap.Round)
    drawOval(
      color = petalSecondary,
      topLeft = Offset(ex - stroke * 1.8f, ey - stroke * 0.9f),
      size = Size(stroke * 3.6f, stroke * 1.8f)
    )
    drawOval(
      color = ink,
      topLeft = Offset(ex - stroke * 1.8f, ey - stroke * 0.9f),
      size = Size(stroke * 3.6f, stroke * 1.8f),
      style = Stroke(stroke * 0.55f)
    )
  }
}

private fun DrawScope.drawMonstera(
  w: Float,
  h: Float,
  stroke: Float,
  ink: Color,
  accent: Color,
  leaf: Color,
  surfaceBg: Color,
  sketch: Boolean,
) {
  val cx = w * 0.50f
  // Aerial roots descending
  listOf(-0.07f, 0.03f, 0.09f).forEachIndexed { idx, dx ->
    val root = Path().apply {
      moveTo(cx + w * 0.02f, h * 0.66f)
      cubicTo(cx + w * dx, h * 0.74f, cx + w * dx * 1.4f, h * 0.80f, cx + w * dx * 1.1f, h * (0.88f + idx * 0.02f))
    }
    drawPath(root, ink.copy(alpha = 0.7f), style = Stroke(stroke * 1.0f, cap = StrokeCap.Round))
  }

  // Petiole stem
  val petiole = Path().apply {
    moveTo(cx + w * 0.04f, h * 0.82f)
    quadraticTo(cx - w * 0.04f, h * 0.60f, cx, h * 0.42f)
  }
  drawPath(petiole, leaf, style = Stroke(stroke * 2.5f, cap = StrokeCap.Round))
  drawPath(petiole, ink, style = Stroke(stroke * 1.1f, cap = StrokeCap.Round))

  // Main Monstera leaf heart silhouette with notched lobes
  val leafCenter = Offset(cx - w * 0.02f, h * 0.36f)
  val lamina = Path().apply {
    moveTo(leafCenter.x, leafCenter.y - h * 0.21f)
    cubicTo(leafCenter.x - w * 0.26f, leafCenter.y - h * 0.19f, leafCenter.x - w * 0.30f, leafCenter.y + h * 0.08f, leafCenter.x - w * 0.14f, leafCenter.y + h * 0.16f)
    quadraticTo(leafCenter.x - w * 0.05f, leafCenter.y + h * 0.18f, leafCenter.x, leafCenter.y + h * 0.10f)
    quadraticTo(leafCenter.x + w * 0.07f, leafCenter.y + h * 0.19f, leafCenter.x + w * 0.17f, leafCenter.y + h * 0.15f)
    cubicTo(leafCenter.x + w * 0.30f, leafCenter.y + h * 0.06f, leafCenter.x + w * 0.26f, leafCenter.y - h * 0.19f, leafCenter.x, leafCenter.y - h * 0.21f)
    close()
  }
  drawPath(lamina, leaf)
  drawPath(lamina, ink, style = Stroke(stroke * 1.1f, join = StrokeJoin.Round))

  // Midrib & lateral veins
  drawLine(
    ink.copy(alpha = 0.65f),
    Offset(leafCenter.x, leafCenter.y - h * 0.18f),
    Offset(leafCenter.x, leafCenter.y + h * 0.10f),
    strokeWidth = stroke * 1.2f
  )
  listOf(-0.12f, -0.05f, 0.02f).forEach { yOff ->
    drawLine(ink.copy(alpha = 0.45f), Offset(leafCenter.x, leafCenter.y + h * yOff), Offset(leafCenter.x - w * 0.20f, leafCenter.y + h * (yOff - 0.04f)), stroke * 0.6f)
    drawLine(ink.copy(alpha = 0.45f), Offset(leafCenter.x, leafCenter.y + h * yOff), Offset(leafCenter.x + w * 0.20f, leafCenter.y + h * (yOff - 0.04f)), stroke * 0.6f)
  }

  // Elliptical Fenestrations (characteristic holes)
  val holeColor = if (sketch) Color.Transparent else surfaceBg
  listOf(
    Offset(leafCenter.x - w * 0.11f, leafCenter.y - h * 0.08f),
    Offset(leafCenter.x - w * 0.14f, leafCenter.y + h * 0.02f),
    Offset(leafCenter.x + w * 0.11f, leafCenter.y - h * 0.07f),
    Offset(leafCenter.x + w * 0.13f, leafCenter.y + h * 0.03f),
  ).forEach { hole ->
    if (!sketch) {
      drawOval(holeColor, Offset(hole.x - w * 0.035f, hole.y - h * 0.018f), Size(w * 0.07f, h * 0.036f))
    }
    drawOval(ink, Offset(hole.x - w * 0.035f, hole.y - h * 0.018f), Size(w * 0.07f, h * 0.036f), style = Stroke(stroke * 0.8f))
  }

  // Spadix inflorescence on right
  val spathe = Path().apply {
    moveTo(cx + w * 0.04f, h * 0.59f)
    quadraticTo(cx + w * 0.22f, h * 0.50f, cx + w * 0.18f, h * 0.37f)
    quadraticTo(cx + w * 0.09f, h * 0.42f, cx + w * 0.04f, h * 0.59f)
    close()
  }
  drawPath(spathe, accent.copy(alpha = 0.28f))
  drawPath(spathe, ink, style = Stroke(stroke * 0.9f))
}

private fun DrawScope.drawGinkgo(
  w: Float,
  h: Float,
  stroke: Float,
  ink: Color,
  goldPrimary: Color,
  goldSecondary: Color,
  leaf: Color,
  sketch: Boolean,
) {
  val branchStart = Offset(w * 0.45f, h * 0.84f)
  val branchMid = Offset(w * 0.50f, h * 0.54f)
  val branchTop = Offset(w * 0.54f, h * 0.34f)
  val twig = Path().apply {
    moveTo(branchStart.x, branchStart.y)
    quadraticTo(w * 0.46f, h * 0.68f, branchMid.x, branchMid.y)
    lineTo(branchTop.x, branchTop.y)
  }
  drawPath(twig, goldSecondary.copy(alpha = if (sketch) 0.15f else 0.7f), style = Stroke(stroke * 3.0f, cap = StrokeCap.Round))
  drawPath(twig, ink, style = Stroke(stroke * 1.2f, cap = StrokeCap.Round))

  // Fan-shaped Ginkgo leaves clustered at brachyblast
  val fans = listOf(
    Triple(Offset(w * 0.48f, h * 0.48f), -42f, 0.23f),
    Triple(Offset(w * 0.52f, h * 0.42f), 5f, 0.26f),
    Triple(Offset(w * 0.56f, h * 0.46f), 46f, 0.21f),
  )
  fans.forEachIndexed { idx, (origin, angle, sizeFrac) ->
    rotate(angle, pivot = origin) {
      val r = w * sizeFrac
      drawLine(ink, origin, Offset(origin.x, origin.y - r * 0.45f), strokeWidth = stroke * 0.9f)
      val apex = Offset(origin.x, origin.y - r * 0.45f)
      val fanPath = Path().apply {
        moveTo(apex.x, apex.y)
        lineTo(apex.x - r * 0.72f, apex.y - r * 0.78f)
        cubicTo(apex.x - r * 0.40f, apex.y - r * 1.05f, apex.x - r * 0.12f, apex.y - r * 1.02f, apex.x - r * 0.04f, apex.y - r * 0.78f)
        lineTo(apex.x, apex.y - r * 0.68f)
        lineTo(apex.x + r * 0.04f, apex.y - r * 0.78f)
        cubicTo(apex.x + r * 0.12f, apex.y - r * 1.02f, apex.x + r * 0.40f, apex.y - r * 1.05f, apex.x + r * 0.72f, apex.y - r * 0.78f)
        close()
      }
      drawPath(fanPath, if (idx == 1) goldPrimary else leaf.copy(alpha = 0.9f))
      drawPath(fanPath, ink, style = Stroke(stroke * 0.95f, join = StrokeJoin.Round))
      // Dichotomous radiating veins
      for (v in -4..4) {
        val vx = apex.x + v * r * 0.13f
        val vy = apex.y - r * 0.85f
        drawLine(ink.copy(alpha = 0.42f), apex, Offset(vx, vy), strokeWidth = stroke * 0.4f)
      }
    }
  }

  // Fleshy golden seed near lower branch
  val seedCenter = Offset(w * 0.63f, h * 0.69f)
  drawLine(ink, Offset(w * 0.50f, h * 0.62f), seedCenter, strokeWidth = stroke * 0.7f)
  drawOval(goldPrimary, Offset(seedCenter.x - w * 0.04f, seedCenter.y - h * 0.03f), Size(w * 0.08f, h * 0.06f))
  drawOval(ink, Offset(seedCenter.x - w * 0.04f, seedCenter.y - h * 0.03f), Size(w * 0.08f, h * 0.06f), style = Stroke(stroke * 0.85f))
}

private fun DrawScope.drawLavender(
  w: Float,
  h: Float,
  stroke: Float,
  ink: Color,
  violetPrimary: Color,
  violetDark: Color,
  leaf: Color,
  sketch: Boolean,
) {
  val stems = listOf(-0.08f, 0.0f, 0.09f)
  stems.forEachIndexed { idx, dx ->
    val topY = h * (0.20f + idx * 0.03f)
    val startX = w * (0.50f + dx * 0.4f)
    val endX = w * (0.50f + dx * 1.6f)
    drawLine(leaf, Offset(startX, h * 0.82f), Offset(endX, topY), strokeWidth = stroke * 1.4f, cap = StrokeCap.Round)
    drawLine(ink, Offset(startX, h * 0.82f), Offset(endX, topY), strokeWidth = stroke * 0.75f, cap = StrokeCap.Round)

    // Linear silver-green leaves lower down
    listOf(0.55f, 0.63f, 0.71f).forEach { lf ->
      val lx = startX + (endX - startX) * (1f - lf)
      val ly = h * lf
      for (dir in listOf(-1f, 1f)) {
        val leafPath = Path().apply {
          moveTo(lx, ly)
          quadraticTo(lx + dir * w * 0.07f, ly - h * 0.04f, lx + dir * w * 0.09f, ly - h * 0.08f)
          quadraticTo(lx + dir * w * 0.04f, ly - h * 0.03f, lx, ly + h * 0.01f)
          close()
        }
        drawPath(leafPath, leaf)
        drawPath(leafPath, ink, style = Stroke(stroke * 0.65f))
      }
    }

    // Verticillaster flower whorls along top spike
    for (tier in 0..6) {
      val frac = tier / 6f
      val fy = topY + h * 0.22f * frac
      val fx = endX - (endX - startX) * (frac * 0.25f)
      for (side in listOf(-1f, 0f, 1f)) {
        val bx = fx + side * w * 0.022f
        val by = fy + kotlin.math.abs(side) * h * 0.006f
        drawOval(
          color = if (tier % 2 == 0) violetPrimary else violetDark,
          topLeft = Offset(bx - w * 0.018f, by - h * 0.014f),
          size = Size(w * 0.036f, h * 0.028f)
        )
        drawOval(
          color = ink,
          topLeft = Offset(bx - w * 0.018f, by - h * 0.014f),
          size = Size(w * 0.036f, h * 0.028f),
          style = Stroke(stroke * 0.55f)
        )
      }
    }
  }
}

private fun DrawScope.drawLotus(
  w: Float,
  h: Float,
  stroke: Float,
  ink: Color,
  rosePrimary: Color,
  roseDark: Color,
  leaf: Color,
  sketch: Boolean,
) {
  val cx = w * 0.50f
  // Submerged rhizome at bottom
  drawOval(
    color = if (sketch) Color.Transparent else leaf.copy(alpha = 0.6f),
    topLeft = Offset(cx - w * 0.18f, h * 0.80f),
    size = Size(w * 0.36f, h * 0.07f)
  )
  drawOval(ink, Offset(cx - w * 0.18f, h * 0.80f), Size(w * 0.36f, h * 0.07f), style = Stroke(stroke * 0.9f))

  // Stems
  drawLine(leaf, Offset(cx - w * 0.03f, h * 0.80f), Offset(cx, h * 0.38f), strokeWidth = stroke * 2f)
  drawLine(ink, Offset(cx - w * 0.03f, h * 0.80f), Offset(cx, h * 0.38f), strokeWidth = stroke * 0.9f)
  drawLine(leaf, Offset(cx + w * 0.04f, h * 0.80f), Offset(cx + w * 0.18f, h * 0.58f), strokeWidth = stroke * 1.6f)
  drawLine(ink, Offset(cx + w * 0.04f, h * 0.80f), Offset(cx + w * 0.18f, h * 0.58f), strokeWidth = stroke * 0.8f)

  // Peltate circular floating leaf on right
  val padCenter = Offset(cx + w * 0.18f, h * 0.58f)
  drawOval(leaf, Offset(padCenter.x - w * 0.15f, padCenter.y - h * 0.05f), Size(w * 0.30f, h * 0.10f))
  drawOval(ink, Offset(padCenter.x - w * 0.15f, padCenter.y - h * 0.05f), Size(w * 0.30f, h * 0.10f), style = Stroke(stroke * 0.9f))

  // Layered Lotus petals
  val fc = Offset(cx, h * 0.37f)
  val petalAngles = listOf(-65f, -40f, -18f, 18f, 40f, 65f, 0f)
  petalAngles.forEachIndexed { idx, deg ->
    rotate(deg, pivot = fc) {
      val len = h * (if (idx == 6) 0.19f else 0.17f)
      val wid = w * 0.065f
      val p = Path().apply {
        moveTo(fc.x, fc.y)
        cubicTo(fc.x - wid, fc.y - len * 0.45f, fc.x - wid * 0.7f, fc.y - len * 0.88f, fc.x, fc.y - len)
        cubicTo(fc.x + wid * 0.7f, fc.y - len * 0.88f, fc.x + wid, fc.y - len * 0.45f, fc.x, fc.y)
        close()
      }
      drawPath(p, if (idx % 2 == 0) rosePrimary else roseDark.copy(alpha = 0.85f))
      drawPath(p, ink, style = Stroke(stroke * 0.9f))
    }
  }
  // Golden receptacle top
  drawOval(
    color = if (sketch) Color.Transparent else Color(0xFFD9A944),
    topLeft = Offset(fc.x - w * 0.05f, fc.y - h * 0.13f),
    size = Size(w * 0.10f, h * 0.035f)
  )
  drawOval(ink, Offset(fc.x - w * 0.05f, fc.y - h * 0.13f), Size(w * 0.10f, h * 0.035f), style = Stroke(stroke * 0.8f))
}

private fun DrawScope.drawFern(
  w: Float,
  h: Float,
  stroke: Float,
  ink: Color,
  fernPrimary: Color,
  leaf: Color,
  sketch: Boolean,
) {
  val cx = w * 0.52f
  val rachis = Path().apply {
    moveTo(cx - w * 0.03f, h * 0.84f)
    cubicTo(cx - w * 0.05f, h * 0.60f, cx + w * 0.03f, h * 0.38f, cx, h * 0.16f)
  }
  drawPath(rachis, fernPrimary, style = Stroke(stroke * 2.0f, cap = StrokeCap.Round))
  drawPath(rachis, ink, style = Stroke(stroke * 1.0f, cap = StrokeCap.Round))

  // Coiled Fibonacci fiddlehead crozier on left
  val crozierBase = Offset(cx - w * 0.03f, h * 0.72f)
  val crozierStem = Path().apply {
    moveTo(crozierBase.x, crozierBase.y)
    quadraticTo(cx - w * 0.16f, h * 0.56f, cx - w * 0.15f, h * 0.43f)
    cubicTo(cx - w * 0.14f, h * 0.36f, cx - w * 0.07f, h * 0.37f, cx - w * 0.08f, h * 0.42f)
    cubicTo(cx - w * 0.09f, h * 0.45f, cx - w * 0.12f, h * 0.44f, cx - w * 0.11f, h * 0.41f)
  }
  drawPath(crozierStem, ink, style = Stroke(stroke * 1.2f, cap = StrokeCap.Round))

  // Pinnate frond segments
  for (i in 0..8) {
    val t = i / 8f
    val py = h * (0.68f - t * 0.47f)
    val span = w * (0.22f * (1f - t * 0.72f))
    for (dir in listOf(-1f, 1f)) {
      val pinna = Path().apply {
        moveTo(cx, py)
        cubicTo(cx + dir * span * 0.45f, py - h * 0.03f, cx + dir * span * 0.85f, py - h * 0.025f, cx + dir * span, py - h * 0.045f)
        cubicTo(cx + dir * span * 0.7f, py + h * 0.015f, cx + dir * span * 0.35f, py + h * 0.015f, cx, py + h * 0.01f)
        close()
      }
      drawPath(pinna, if (i % 2 == 0) leaf else fernPrimary)
      drawPath(pinna, ink, style = Stroke(stroke * 0.7f, join = StrokeJoin.Round))
      if (!sketch && i in 2..6) {
        drawCircle(ink.copy(alpha = 0.55f), radius = stroke * 0.7f, center = Offset(cx + dir * span * 0.5f, py - h * 0.012f))
      }
    }
  }
}

private fun DrawScope.drawPassiflora(
  w: Float,
  h: Float,
  stroke: Float,
  ink: Color,
  violetPrimary: Color,
  violetDark: Color,
  leaf: Color,
  sketch: Boolean,
) {
  val cx = w * 0.50f
  val fc = Offset(cx, h * 0.36f)

  // Vine stem & helical tendril
  val vine = Path().apply {
    moveTo(cx + w * 0.05f, h * 0.84f)
    cubicTo(cx - w * 0.06f, h * 0.68f, cx + w * 0.04f, h * 0.52f, fc.x, fc.y)
  }
  drawPath(vine, leaf, style = Stroke(stroke * 1.9f, cap = StrokeCap.Round))
  drawPath(vine, ink, style = Stroke(stroke * 0.9f))

  // Coiled tendril
  val tendril = Path().apply {
    moveTo(cx - w * 0.01f, h * 0.64f)
    cubicTo(cx - w * 0.18f, h * 0.62f, cx - w * 0.18f, h * 0.68f, cx - w * 0.12f, h * 0.69f)
    cubicTo(cx - w * 0.08f, h * 0.70f, cx - w * 0.16f, h * 0.74f, cx - w * 0.20f, h * 0.72f)
  }
  drawPath(tendril, ink, style = Stroke(stroke * 0.85f, cap = StrokeCap.Round))

  // 10 Sepals & petals behind corona
  for (i in 0 until 10) {
    rotate(i * 36f, pivot = fc) {
      val petal = Path().apply {
        moveTo(fc.x, fc.y)
        cubicTo(fc.x - w * 0.04f, fc.y - h * 0.10f, fc.x - w * 0.03f, fc.y - h * 0.17f, fc.x, fc.y - h * 0.18f)
        cubicTo(fc.x + w * 0.03f, fc.y - h * 0.17f, fc.x + w * 0.04f, fc.y - h * 0.10f, fc.x, fc.y)
        close()
      }
      drawPath(petal, if (i % 2 == 0) violetPrimary.copy(alpha = 0.24f) else leaf.copy(alpha = 0.35f))
      drawPath(petal, ink, style = Stroke(stroke * 0.8f))
    }
  }

  // Radial corona filaments
  for (i in 0 until 24) {
    val rad = (i * 15.0) * PI / 180.0
    val len = w * 0.17f
    val ex = fc.x + (len * cos(rad)).toFloat()
    val ey = fc.y + (len * sin(rad)).toFloat()
    drawLine(if (i % 2 == 0) violetPrimary else violetDark, fc, Offset(ex, ey), strokeWidth = stroke * 0.8f, cap = StrokeCap.Round)
  }

  // 3-armed stigma crown
  listOf(-150f, -90f, -30f).forEach { deg ->
    val rad = deg * PI / 180.0
    val ex = fc.x + (w * 0.075f * cos(rad)).toFloat()
    val ey = fc.y + (w * 0.075f * sin(rad)).toFloat()
    drawLine(ink, fc, Offset(ex, ey), strokeWidth = stroke * 1.1f)
    drawCircle(violetDark, radius = stroke * 2.0f, center = Offset(ex, ey))
    drawCircle(ink, radius = stroke * 2.0f, center = Offset(ex, ey), style = Stroke(stroke * 0.6f))
  }
}

private fun DrawScope.drawCamellia(
  w: Float,
  h: Float,
  stroke: Float,
  ink: Color,
  crimsonPrimary: Color,
  crimsonDark: Color,
  leaf: Color,
  sketch: Boolean,
) {
  val cx = w * 0.50f
  val fc = Offset(cx, h * 0.34f)
  drawLine(leaf, Offset(cx - w * 0.03f, h * 0.84f), fc, strokeWidth = stroke * 2.2f, cap = StrokeCap.Round)
  drawLine(ink, Offset(cx - w * 0.03f, h * 0.84f), fc, strokeWidth = stroke * 1.0f)

  // Serrated glossy leaves
  listOf(-1f to 0.58f, 1f to 0.64f).forEach { (dir, yFrac) ->
    val sy = h * yFrac
    val lPath = Path().apply {
      moveTo(cx, sy)
      cubicTo(cx + dir * w * 0.14f, sy - h * 0.07f, cx + dir * w * 0.24f, sy - h * 0.02f, cx + dir * w * 0.25f, sy + h * 0.04f)
      cubicTo(cx + dir * w * 0.15f, sy + h * 0.07f, cx + dir * w * 0.05f, sy + h * 0.04f, cx, sy + h * 0.01f)
      close()
    }
    drawPath(lPath, leaf)
    drawPath(lPath, ink, style = Stroke(stroke * 0.95f))
  }

  // Concentric imbricated Camellia petals
  val rings = listOf(
    Triple(8, 0.16f, crimsonDark),
    Triple(6, 0.11f, crimsonPrimary),
    Triple(5, 0.065f, crimsonPrimary.copy(alpha = 0.92f)),
  )
  rings.forEachIndexed { rIdx, (count, radiusFrac, color) ->
    for (i in 0 until count) {
      val deg = i * (360f / count) + rIdx * 20f
      rotate(deg, pivot = fc) {
        val r = w * radiusFrac
        drawOval(
          color = color,
          topLeft = Offset(fc.x - r * 0.55f, fc.y - r * 1.15f),
          size = Size(r * 1.1f, r * 1.05f)
        )
        drawOval(
          color = ink,
          topLeft = Offset(fc.x - r * 0.55f, fc.y - r * 1.15f),
          size = Size(r * 1.1f, r * 1.05f),
          style = Stroke(stroke * 0.8f)
        )
      }
    }
  }
  // Golden stamen heart
  drawCircle(if (sketch) Color.Transparent else Color(0xFFDDA93E), radius = w * 0.032f, center = fc)
  drawCircle(ink, radius = w * 0.032f, center = fc, style = Stroke(stroke * 0.7f))
}
