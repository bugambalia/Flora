package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CormorantDisplayFont
import com.example.ui.theme.FloraBorderInk
import com.example.ui.theme.FloraForest
import com.example.ui.theme.FloraGold
import com.example.ui.theme.FloraGoldSoft
import com.example.ui.theme.FloraInk
import com.example.ui.theme.FloraPaper
import com.example.ui.theme.FloraPlateShape
import com.example.ui.theme.FloraSage
import com.example.ui.theme.FloraShadowPaper
import com.example.ui.theme.FloraTerracotta
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Minimal 5-petal botanical blossom glyph used inline next to screen titles and inside seals.
 */
@Composable
fun FloraFlowerGlyph(
  modifier: Modifier = Modifier,
  color: Color = FloraTerracotta,
  filled: Boolean = false,
  centerColor: Color = FloraGold,
) {
  Canvas(modifier = modifier) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension * 0.25f
    val dist = size.minDimension * 0.23f
    val strokeW = (size.minDimension * 0.085f).coerceIn(1.4f, 3.2f)

    for (i in 0 until 5) {
      val angle = (i * 72.0 - 90.0) * PI / 180.0
      val px = cx + (cos(angle) * dist).toFloat()
      val py = cy + (sin(angle) * dist).toFloat()
      if (filled) {
        drawCircle(color = color, radius = r, center = Offset(px, py))
      } else {
        drawCircle(
          color = color,
          radius = r,
          center = Offset(px, py),
          style = Stroke(width = strokeW)
        )
      }
    }
    drawCircle(
      color = if (filled) FloraForest else centerColor,
      radius = size.minDimension * 0.11f,
      center = Offset(cx, cy)
    )
  }
}

/**
 * Mounted botanical plate container matching DESIGN.md §1 & §6:
 * asymmetric shape, crisp 1dp border-ink, optional inner gold hairline, and hard offset shadow.
 */
@Composable
fun FloraMountedPlate(
  modifier: Modifier = Modifier,
  shape: Shape = FloraPlateShape,
  containerColor: Color = FloraPaper,
  borderColor: Color = FloraBorderInk,
  goldHairline: Boolean = false,
  shadowOffsetX: Dp = 4.dp,
  shadowOffsetY: Dp = 5.dp,
  shadowColor: Color = FloraShadowPaper,
  onClick: (() -> Unit)? = null,
  content: @Composable BoxScope.() -> Unit,
) {
  val density = LocalDensity.current
  val sx = with(density) { shadowOffsetX.toPx() }
  val sy = with(density) { shadowOffsetY.toPx() }

  Box(
    modifier = modifier
      .drawBehind {
        if (sx > 0f || sy > 0f) {
          val outline = shape.createOutline(size, layoutDirection, this)
          val path = Path().apply { addOutline(outline) }
          translate(left = sx, top = sy) {
            drawPath(path = path, color = shadowColor)
          }
        }
      }
      .clip(shape)
      .background(containerColor)
      .border(BorderStroke(1.dp, borderColor), shape)
      .then(
        if (goldHairline) {
          Modifier.padding(3.dp).border(BorderStroke(0.8.dp, FloraGold.copy(alpha = 0.45f)), shape)
        } else {
          Modifier
        }
      )
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    content = content
  )
}

/**
 * Top Flora Screen Header matching screenshots 01, 02, 05:
 * Kicker ("FLORA · HERBARIO MUSICAL"), italic Cormorant title + terracotta flower glyph,
 * circular dark-forest monogram seal on the right ("M" / opens Herbario Atlas), and crisp hairline divider.
 */
@Composable
fun FloraScreenHeader(
  title: String,
  onSealClick: () -> Unit,
  modifier: Modifier = Modifier,
  kicker: String = "FLORA · HERBARIO MUSICAL",
  sealText: String = "H",
  sealContentDescription: String = "Abrir Herbario Anatómico",
  trailingExtra: (@Composable () -> Unit)? = null,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = kicker,
          style = MaterialTheme.typography.labelMedium,
          color = FloraSage,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = title,
            style = MaterialTheme.typography.displayLarge,
            color = FloraInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          FloraFlowerGlyph(
            modifier = Modifier.size(20.dp),
            color = FloraTerracotta,
            filled = false
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        trailingExtra?.invoke()
        // Monogram / Herbario Atlas Circular Seal with double ring
        Box(
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(46.dp)
            .clip(CircleShape)
            .border(1.dp, FloraBorderInk.copy(alpha = 0.35f), CircleShape)
            .padding(3.dp)
            .clip(CircleShape)
            .background(FloraForest)
            .border(1.dp, FloraGold.copy(alpha = 0.75f), CircleShape)
            .clickable(onClick = onSealClick)
            .semantics { contentDescription = sealContentDescription }
            .testTag("flora_header_herbarium_seal"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = sealText,
            fontFamily = CormorantDisplayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = FloraGoldSoft
          )
        }
      }
    }
    HorizontalDivider(thickness = 1.dp, color = FloraBorderInk.copy(alpha = 0.22f))
  }
}

@Composable
fun SpecimenHeaderPlate(
  title: String,
  latinSubtitle: String,
  fieldDescription: String,
  leftMetaLine: String,
  accentColor: Color,
  modifier: Modifier = Modifier,
  trailingAction: (@Composable () -> Unit)? = null,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Top
    ) {
      Column(modifier = Modifier.weight(1.15f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp, lineHeight = 35.sp),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = leftMetaLine,
          style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.End
      ) {
        Text(
          text = latinSubtitle,
          style = MaterialTheme.typography.titleSmall.copy(fontStyle = FontStyle.Italic),
          color = accentColor,
          textAlign = TextAlign.End,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = fieldDescription,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.End,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis
        )
        if (trailingAction != null) {
          Spacer(modifier = Modifier.height(6.dp))
          trailingAction()
        }
      }
    }
    DottedRuleDivider(color = MaterialTheme.colorScheme.outline)
  }
}

@Composable
fun DottedRuleDivider(
  modifier: Modifier = Modifier,
  color: Color = MaterialTheme.colorScheme.outline,
) {
  Canvas(
    modifier = modifier
      .fillMaxWidth()
      .height(10.dp)
  ) {
    val y = size.height / 2f
    val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
    drawLine(
      color = color.copy(alpha = 0.65f),
      start = Offset(4f, y),
      end = Offset(size.width - 4f, y),
      strokeWidth = 1.4f,
      pathEffect = dash
    )
    drawCircle(color = color.copy(alpha = 0.8f), radius = 2.2f, center = Offset(4f, y))
    drawCircle(color = color.copy(alpha = 0.8f), radius = 2.2f, center = Offset(size.width - 4f, y))
  }
}

enum class MarginaliaKind { UNIAXIAL_BRANCH, SYMPODIAL_TWIG, SEED_SECTION, SPROUT }

@Composable
fun MarginaliaSketch(
  kind: MarginaliaKind,
  modifier: Modifier = Modifier,
  inkColor: Color = FloraInk.copy(alpha = 0.78f),
  tintColor: Color = FloraTerracotta.copy(alpha = 0.45f),
) {
  Canvas(modifier = modifier.padding(2.dp)) {
    val w = size.width
    val h = size.height
    val stroke = (w.coerceAtMost(h) * 0.045f).coerceIn(1.4f, 3f)

    when (kind) {
      MarginaliaKind.UNIAXIAL_BRANCH -> {
        drawLine(inkColor, Offset(w * 0.5f, h * 0.92f), Offset(w * 0.5f, h * 0.14f), strokeWidth = stroke * 1.4f, cap = StrokeCap.Round)
        listOf(0.70f to -1f, 0.50f to 1f, 0.36f to -1f, 0.25f to 1f).forEach { (yf, dir) ->
          drawLine(
            inkColor,
            Offset(w * 0.5f, h * yf),
            Offset(w * (0.5f + dir * 0.34f), h * (yf - 0.16f)),
            strokeWidth = stroke,
            cap = StrokeCap.Round
          )
        }
      }
      MarginaliaKind.SYMPODIAL_TWIG -> {
        val twig = Path().apply {
          moveTo(w * 0.42f, h * 0.92f)
          lineTo(w * 0.56f, h * 0.62f)
          lineTo(w * 0.40f, h * 0.36f)
          lineTo(w * 0.58f, h * 0.12f)
        }
        drawPath(twig, inkColor, style = Stroke(stroke * 1.3f, cap = StrokeCap.Round))
        drawLine(inkColor, Offset(w * 0.56f, h * 0.62f), Offset(w * 0.78f, h * 0.48f), strokeWidth = stroke * 0.9f, cap = StrokeCap.Round)
        drawLine(inkColor, Offset(w * 0.40f, h * 0.36f), Offset(w * 0.20f, h * 0.24f), strokeWidth = stroke * 0.9f, cap = StrokeCap.Round)
      }
      MarginaliaKind.SEED_SECTION -> {
        drawOval(tintColor, Offset(w * 0.22f, h * 0.14f), Size(w * 0.56f, h * 0.72f))
        drawOval(inkColor, Offset(w * 0.22f, h * 0.14f), Size(w * 0.56f, h * 0.72f), style = Stroke(stroke))
        drawOval(inkColor.copy(alpha = 0.65f), Offset(w * 0.34f, h * 0.26f), Size(w * 0.32f, h * 0.48f), style = Stroke(stroke * 0.8f))
        drawCircle(inkColor, radius = stroke * 1.2f, center = Offset(w * 0.50f, h * 0.46f))
      }
      MarginaliaKind.SPROUT -> {
        val stem = Path().apply {
          moveTo(w * 0.50f, h * 0.76f)
          cubicTo(w * 0.42f, h * 0.44f, w * 0.66f, h * 0.18f, w * 0.74f, h * 0.42f)
        }
        drawPath(stem, inkColor, style = Stroke(stroke * 1.2f, cap = StrokeCap.Round))
        drawOval(tintColor, Offset(w * 0.64f, h * 0.38f), Size(w * 0.20f, h * 0.14f))
        drawOval(inkColor, Offset(w * 0.64f, h * 0.38f), Size(w * 0.20f, h * 0.14f), style = Stroke(stroke * 0.8f))
        listOf(-0.18f, 0f, 0.16f).forEach { dx ->
          drawLine(inkColor, Offset(w * 0.50f, h * 0.76f), Offset(w * (0.50f + dx), h * 0.92f), strokeWidth = stroke * 0.8f)
        }
      }
    }
  }
}

@Composable
fun FloraDashedEmptyPlate(
  message: String,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .drawBehind {
        val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
        val outline = FloraPlateShape.createOutline(size, layoutDirection, this)
        val path = Path().apply { addOutline(outline) }
        drawPath(
          path = path,
          color = FloraBorderInk.copy(alpha = 0.32f),
          style = Stroke(width = 1.2.dp.toPx(), pathEffect = dash)
        )
      }
      .padding(horizontal = 24.dp, vertical = 32.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      FloraFlowerGlyph(
        modifier = Modifier.size(28.dp),
        color = FloraTerracotta.copy(alpha = 0.75f)
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
        color = FloraInk.copy(alpha = 0.85f),
        textAlign = TextAlign.Center
      )
    }
  }
}
