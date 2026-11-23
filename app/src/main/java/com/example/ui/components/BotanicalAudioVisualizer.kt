package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.audio.BotanicalAudioSpectrum
import com.example.data.model.AudioTrack
import com.example.data.model.BotanicalSpeciesId
import com.example.ui.theme.FloraGold
import com.example.ui.theme.FloraGoldSoft
import com.example.ui.theme.FloraInk
import com.example.ui.theme.FloraPlayerCream
import com.example.ui.theme.FloraSage
import com.example.ui.theme.LocalAlbumPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom Canvas-based botanical audio visualizer that draws animated organic
 * leaves, layered petals, veins, and pollen stamens around the central disc,
 * pulsing and growing in sync with real-time FFT audio frequency bands and
 * performing a wither-and-bloom dissolve animation during track transitions.
 */
@Composable
fun BotanicalFrequencyVisualizer(
  spectrum: BotanicalAudioSpectrum,
  isPlaying: Boolean,
  modifier: Modifier = Modifier,
  innerRadiusFraction: Float = 0.66f,
  dissolveProgress: Float = 1f,
  isDissolving: Boolean = false,
) {
  val palette = LocalAlbumPalette.current
  val infiniteTransition = rememberInfiniteTransition(label = "botanicalVisualizerDrift")

  val driftAngleDeg by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 36000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "visualizerDrift"
  )

  val idleBreath by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2600, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "visualizerBreath"
  )

  val bandCount = BotanicalAudioSpectrum.BAND_COUNT
  val animatedBands = List(bandCount) { idx ->
    val raw = if (isPlaying) {
      val bandVal = spectrum.bands.getOrElse(idx) { 0f }
      // Combine live PCM FFT energy with subtle breath floor when audio starts
      (bandVal.coerceIn(0f, 1f) + if (!spectrum.isActive) 0.08f * idleBreath else 0f).coerceIn(0f, 1f)
    } else {
      0.04f + 0.03f * idleBreath
    }
    val anim by animateFloatAsState(
      targetValue = raw,
      animationSpec = tween(durationMillis = 85),
      label = "band_$idx"
    )
    anim
  }

  val animatedBass by animateFloatAsState(
    targetValue = if (isPlaying) spectrum.bass else 0.04f,
    animationSpec = tween(durationMillis = 90),
    label = "bassEnergy"
  )
  val animatedTreble by animateFloatAsState(
    targetValue = if (isPlaying) spectrum.treble else 0.03f,
    animationSpec = tween(durationMillis = 90),
    label = "trebleEnergy"
  )

  val reusablePath = remember { Path() }
  val veinPath = remember { Path() }

  val leafBaseColor = lerp(FloraSage, palette.muted, 0.50f)
  val leafHighlightColor = lerp(FloraGoldSoft, palette.lightVibrant, 0.45f)
  val petalPrimaryColor = lerp(palette.vibrant, FloraGoldSoft, 0.30f)
  val petalSecondaryColor = lerp(palette.lightVibrant, FloraPlayerCream, 0.35f)
  val rotationOffset = if (isPlaying) driftAngleDeg else 0f

  val t = dissolveProgress.coerceIn(0f, 1f)
  // Wither peaks during [0.0 .. 0.50] as outgoing artwork dissolves
  val witherFactor = if (isDissolving && t < 0.52f) {
    sin((t / 0.52f) * PI).toFloat().coerceIn(0f, 1f)
  } else 0f
  // Bloom surges during [0.42 .. 1.00] as incoming artwork emerges
  val bloomFactor = if (isDissolving && t >= 0.42f) {
    sin(((t - 0.42f) / 0.58f) * PI).toFloat().coerceIn(0f, 1f)
  } else 0f

  val leafGrowthScale = (1f - 0.54f * witherFactor + 0.34f * bloomFactor).coerceIn(0.38f, 1.42f)
  val petalGrowthScale = (1f - 0.62f * witherFactor + 0.42f * bloomFactor).coerceIn(0.30f, 1.48f)
  val droopTwistDeg = witherFactor * 16f - bloomFactor * 5f

  Canvas(
    modifier = modifier
      .semantics {
        contentDescription = "Visualizador botánico reactivo a frecuencias de audio"
      }
      .testTag("botanical_audio_visualizer")
  ) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val maxR = size.minDimension / 2f
    val calyxR = maxR * innerRadiusFraction
    val maxReach = maxR - calyxR

    // Subtle bass + bloom-reactive aura ring around calyx
    drawCircle(
      color = lerp(petalPrimaryColor, FloraGold, witherFactor * 0.4f)
        .copy(alpha = (0.14f + animatedBass * 0.24f + bloomFactor * 0.16f - witherFactor * 0.08f).coerceIn(0.05f, 0.6f)),
      radius = calyxR + maxReach * (0.10f + animatedBass * 0.22f + bloomFactor * 0.18f - witherFactor * 0.06f),
      center = Offset(cx, cy),
      style = Stroke(width = 1.4f + animatedBass * 2.2f + bloomFactor * 1.4f)
    )

    // 1. Outer Botanical Foliage Ring: 12 organic curved leaves pulsing with frequency bands
    for (i in 0 until bandCount) {
      val bandEnergy = animatedBands[i]
      val baseAngleDeg = (i * (360f / bandCount)) + rotationOffset + droopTwistDeg * (if (i % 2 == 0) 1f else -1f)
      val swayDeg = sin((idleBreath * PI + i * 0.85)).toFloat() * (2.5f + bandEnergy * 6.5f)
      val angleRad = ((baseAngleDeg + swayDeg) * PI / 180.0).toFloat()

      val leafLength = maxReach * (0.34f + bandEnergy * 0.62f + animatedBass * 0.10f) * leafGrowthScale
      val leafWidth = leafLength * (0.34f + bandEnergy * 0.14f) * (1f - 0.25f * witherFactor + 0.12f * bloomFactor)
      val startR = calyxR - maxReach * (0.04f + witherFactor * 0.06f)

      val witheredLeafFill = lerp(
        lerp(leafBaseColor, leafHighlightColor, bandEnergy * 0.65f),
        FloraGold,
        witherFactor * 0.50f
      )
      drawOrganicLeaf(
        leafPath = reusablePath,
        veinPath = veinPath,
        cx = cx,
        cy = cy,
        startR = startR,
        length = leafLength,
        halfWidth = leafWidth,
        angleRad = angleRad,
        tiltSign = if (i % 2 == 0) 1f else -1f,
        fillColor = witheredLeafFill.copy(
          alpha = ((0.48f + bandEnergy * 0.44f) * (1f - 0.35f * witherFactor + 0.12f * bloomFactor)).coerceIn(0.20f, 0.96f)
        ),
        strokeColor = FloraGoldSoft.copy(alpha = (0.42f + bandEnergy * 0.48f - witherFactor * 0.15f).coerceIn(0.20f, 0.92f)),
        veinColor = FloraPlayerCream.copy(alpha = (0.32f + bandEnergy * 0.45f - witherFactor * 0.12f).coerceIn(0.15f, 0.85f)),
        strokeWidth = (1.3f + bandEnergy * 1.4f)
      )
    }

    // 2. Inner Botanical Petal Corolla: 12 offset petals blooming and withering between tracks
    val petalStepDeg = 360f / bandCount
    for (i in 0 until bandCount) {
      val pairedBand = (animatedBands[i] * 0.6f + animatedBands[(i + 1) % bandCount] * 0.4f).coerceIn(0f, 1f)
      val petalAngleDeg = (i * petalStepDeg) + (petalStepDeg * 0.5f) + rotationOffset * 0.85f - droopTwistDeg * 0.7f
      val petalAngleRad = (petalAngleDeg * PI / 180.0).toFloat()

      val petalLength = maxReach * (0.26f + pairedBand * 0.56f + animatedBass * 0.12f) * petalGrowthScale
      val petalHalfWidth = petalLength * (0.40f + pairedBand * 0.16f) * (1f - 0.28f * witherFactor + 0.18f * bloomFactor)
      val startR = calyxR - maxReach * (0.06f + witherFactor * 0.05f)

      val activePetalFill = lerp(
        lerp(petalPrimaryColor, petalSecondaryColor, pairedBand * 0.65f + bloomFactor * 0.25f),
        FloraGoldSoft,
        witherFactor * 0.45f
      )
      drawOrganicPetal(
        petalPath = reusablePath,
        veinPath = veinPath,
        cx = cx,
        cy = cy,
        startR = startR,
        length = petalLength,
        halfWidth = petalHalfWidth,
        angleRad = petalAngleRad,
        fillColor = activePetalFill.copy(
          alpha = ((0.45f + pairedBand * 0.47f) * (1f - 0.40f * witherFactor + 0.18f * bloomFactor)).coerceIn(0.18f, 0.96f)
        ),
        outlineColor = FloraPlayerCream.copy(alpha = (0.50f + pairedBand * 0.42f - witherFactor * 0.20f).coerceIn(0.22f, 0.95f)),
        veinColor = FloraGoldSoft.copy(alpha = (0.40f + pairedBand * 0.45f).coerceIn(0.22f, 0.90f)),
        strokeWidth = 1.2f + pairedBand * 1.3f
      )
    }

    // 3. Slender Botanical Stamens & Pollen Anthers shimmering with treble + bloom burst
    val stamenCount = bandCount * 2
    for (s in 0 until stamenCount) {
      val bandEnergy = animatedBands[s % bandCount]
      val shimmer = (bandEnergy * 0.65f + animatedTreble * 0.35f + bloomFactor * 0.32f - witherFactor * 0.20f)
        .coerceIn(0f, 1f)
      val angleDeg = (s * (360f / stamenCount)) + (360f / (stamenCount * 2f)) + rotationOffset * 1.1f
      val angleRad = (angleDeg * PI / 180.0).toFloat()

      val stemStart = calyxR + maxReach * 0.02f
      val stemEnd = calyxR + maxReach * (0.16f + shimmer * 0.46f) * (1f - 0.35f * witherFactor + 0.22f * bloomFactor)
      val ux = cos(angleRad)
      val uy = sin(angleRad)
      val px = -uy
      val py = ux

      val curveOffset = maxReach * 0.045f * (if (s % 2 == 0) 1f else -1f) * (0.5f + shimmer)
      val startPt = Offset(cx + ux * stemStart, cy + uy * stemStart)
      val midR = (stemStart + stemEnd) * 0.55f
      val ctrlPt = Offset(cx + ux * midR + px * curveOffset, cy + uy * midR + py * curveOffset)
      val tipPt = Offset(cx + ux * stemEnd, cy + uy * stemEnd)

      reusablePath.reset()
      reusablePath.moveTo(startPt.x, startPt.y)
      reusablePath.quadraticTo(ctrlPt.x, ctrlPt.y, tipPt.x, tipPt.y)

      drawPath(
        path = reusablePath,
        color = FloraGoldSoft.copy(alpha = (0.35f + shimmer * 0.55f - witherFactor * 0.18f).coerceIn(0.18f, 0.94f)),
        style = Stroke(width = 1.1f + shimmer * 0.9f, cap = StrokeCap.Round)
      )

      // Golden pollen anther droplet at tip
      drawCircle(
        color = FloraPlayerCream.copy(alpha = (0.55f + shimmer * 0.45f - witherFactor * 0.22f).coerceIn(0.25f, 1f)),
        radius = (1.8f + shimmer * 2.6f) * (1f + bloomFactor * 0.35f),
        center = tipPt
      )
    }

    // 4. Shedding Botanical Petals & Pollen Spores drifting outward during dissolve transition
    if (isDissolving && t in 0.04f..0.92f) {
      val driftProgress = ((t - 0.04f) / 0.88f).coerceIn(0f, 1f)
      val particleAlpha = sin(driftProgress * PI).toFloat().coerceIn(0f, 1f) * 0.88f
      for (p in 0 until 12) {
        val seedAngleDeg = p * 30f + driftProgress * 42f * (if (p % 2 == 0) 1f else -1f)
        val pAngleRad = (seedAngleDeg * PI / 180.0).toFloat()
        val travelR = calyxR + maxReach * (0.18f + driftProgress * 0.85f)
        val px = cx + cos(pAngleRad) * travelR
        val py = cy + sin(pAngleRad) * travelR
        val shedLen = maxReach * (0.14f - driftProgress * 0.06f).coerceAtLeast(0.04f)

        drawOrganicPetal(
          petalPath = reusablePath,
          veinPath = veinPath,
          cx = px,
          cy = py,
          startR = 0f,
          length = shedLen,
          halfWidth = shedLen * 0.48f,
          angleRad = pAngleRad + driftProgress * 1.4f,
          fillColor = lerp(FloraGoldSoft, petalPrimaryColor, driftProgress).copy(alpha = particleAlpha * 0.72f),
          outlineColor = FloraPlayerCream.copy(alpha = particleAlpha * 0.85f),
          veinColor = FloraGold.copy(alpha = particleAlpha * 0.6f),
          strokeWidth = 1f
        )
      }
    }
  }
}

/**
 * Circular artwork container that performs a botanical dissolve cross-fade between
 * [outgoingTrack] and [incomingTrack] using a blooming/withering floral petal veil.
 */
@Composable
fun BotanicalDissolveArtworkLayer(
  outgoingTrack: AudioTrack?,
  incomingTrack: AudioTrack?,
  speciesId: BotanicalSpeciesId,
  dissolveProgress: Float,
  isDissolving: Boolean,
  showLiveDrawing: Boolean,
  modifier: Modifier = Modifier,
) {
  val t = dissolveProgress.coerceIn(0f, 1f)
  val outgoingAlpha = if (isDissolving && outgoingTrack != null) {
    (1f - ((t - 0.16f) / 0.40f).coerceIn(0f, 1f))
  } else 0f
  val outgoingScale = if (isDissolving && outgoingTrack != null) {
    1f - 0.10f * ((t / 0.55f).coerceIn(0f, 1f))
  } else 1f

  val incomingAlpha = if (isDissolving && outgoingTrack != null) {
    ((t - 0.36f) / 0.46f).coerceIn(0f, 1f)
  } else 1f
  val incomingScale = if (isDissolving && outgoingTrack != null) {
    0.90f + 0.10f * ((t - 0.36f) / 0.64f).coerceIn(0f, 1f)
  } else 1f

  val palette = LocalAlbumPalette.current
  val petalMaskPath = remember { Path() }
  val veinMaskPath = remember { Path() }

  Box(
    modifier = modifier
      .clip(CircleShape)
      .testTag("botanical_dissolve_artwork"),
    contentAlignment = Alignment.Center
  ) {
    // Outgoing album artwork (fading & contracting during wither phase)
    if (isDissolving && outgoingTrack != null && outgoingAlpha > 0.01f) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer {
            alpha = outgoingAlpha
            scaleX = outgoingScale
            scaleY = outgoingScale
          }
      ) {
        if (showLiveDrawing) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color(0xFFF6EFE0)),
            contentAlignment = Alignment.Center
          ) {
            BotanicalSpecimenIllustration(
              speciesId = outgoingTrack.speciesId,
              modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
            )
          }
        } else {
          AlbumArtFrame(
            track = outgoingTrack,
            customShape = CircleShape,
            borderColor = Color.Transparent,
            modifier = Modifier.fillMaxSize()
          )
        }
      }
    }

    // Incoming album artwork (blooming in during anthesis phase)
    Box(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          alpha = incomingAlpha
          scaleX = incomingScale
          scaleY = incomingScale
        }
    ) {
      if (showLiveDrawing) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6EFE0)),
          contentAlignment = Alignment.Center
        ) {
          BotanicalSpecimenIllustration(
            speciesId = speciesId,
            modifier = Modifier
              .fillMaxSize()
              .padding(14.dp)
          )
        }
      } else {
        AlbumArtFrame(
          track = incomingTrack,
          customShape = CircleShape,
          borderColor = Color.Transparent,
          modifier = Modifier.fillMaxSize()
        )
      }
    }

    // Botanical Petal Dissolve Veil: petals close inward to dissolve old cover, then unfurl outward to reveal new cover
    if (isDissolving && t in 0.01f..0.98f) {
      val veilIntensity = sin(t * PI).toFloat().coerceIn(0f, 1f)
      val closingPhase = if (t < 0.48f) (t / 0.48f).coerceIn(0f, 1f) else 1f - ((t - 0.48f) / 0.52f).coerceIn(0f, 1f)
      Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension / 2f

        // Soft botanical radial mist at midpoint of cross-fade
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              lerp(FloraGoldSoft, palette.vibrant, 0.35f).copy(alpha = veilIntensity * 0.58f),
              FloraInk.copy(alpha = veilIntensity * 0.34f),
              Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = radius * (0.45f + veilIntensity * 0.65f)
          ),
          radius = radius,
          center = Offset(cx, cy)
        )

        val petalCount = 10
        val sweepSpinDeg = t * 52f
        for (i in 0 until petalCount) {
          val angleRad = ((i * (360f / petalCount) + sweepSpinDeg) * PI / 180.0).toFloat()
          val petalLen = radius * (0.28f + closingPhase * 0.78f)
          val startR = radius * (0.94f - closingPhase * 0.88f)
          val inwardAngle = if (t < 0.48f) angleRad + PI.toFloat() else angleRad
          val originX = cx + cos(angleRad) * startR
          val originY = cy + sin(angleRad) * startR

          drawOrganicPetal(
            petalPath = petalMaskPath,
            veinPath = veinMaskPath,
            cx = originX,
            cy = originY,
            startR = 0f,
            length = petalLen,
            halfWidth = petalLen * 0.44f,
            angleRad = inwardAngle,
            fillColor = lerp(FloraGoldSoft, palette.lightVibrant, 0.45f)
              .copy(alpha = (veilIntensity * 0.72f).coerceIn(0f, 0.82f)),
            outlineColor = FloraPlayerCream.copy(alpha = (veilIntensity * 0.85f).coerceIn(0f, 0.9f)),
            veinColor = FloraGold.copy(alpha = (veilIntensity * 0.7f).coerceIn(0f, 0.85f)),
            strokeWidth = 1.4f
          )
        }
      }
    }
  }
}

private fun DrawScope.drawOrganicLeaf(
  leafPath: Path,
  veinPath: Path,
  cx: Float,
  cy: Float,
  startR: Float,
  length: Float,
  halfWidth: Float,
  angleRad: Float,
  tiltSign: Float,
  fillColor: Color,
  strokeColor: Color,
  veinColor: Color,
  strokeWidth: Float,
) {
  val ux = cos(angleRad)
  val uy = sin(angleRad)
  val px = -uy
  val py = ux

  val baseX = cx + ux * startR
  val baseY = cy + uy * startR

  val tipR = startR + length
  val tipBend = halfWidth * 0.24f * tiltSign
  val tipX = cx + ux * tipR + px * tipBend
  val tipY = cy + uy * tipR + py * tipBend

  val bellyR1 = startR + length * 0.34f
  val bellyR2 = startR + length * 0.72f

  val rightCtrl1X = cx + ux * bellyR1 + px * (halfWidth * 1.05f)
  val rightCtrl1Y = cy + uy * bellyR1 + py * (halfWidth * 1.05f)
  val rightCtrl2X = cx + ux * bellyR2 + px * (halfWidth * 0.55f + tipBend * 0.5f)
  val rightCtrl2Y = cy + uy * bellyR2 + py * (halfWidth * 0.55f + tipBend * 0.5f)

  val leftCtrl2X = cx + ux * bellyR2 - px * (halfWidth * 0.48f - tipBend * 0.5f)
  val leftCtrl2Y = cy + uy * bellyR2 - py * (halfWidth * 0.48f - tipBend * 0.5f)
  val leftCtrl1X = cx + ux * bellyR1 - px * (halfWidth * 0.92f)
  val leftCtrl1Y = cy + uy * bellyR1 - py * (halfWidth * 0.92f)

  leafPath.reset()
  leafPath.moveTo(baseX, baseY)
  leafPath.cubicTo(rightCtrl1X, rightCtrl1Y, rightCtrl2X, rightCtrl2Y, tipX, tipY)
  leafPath.cubicTo(leftCtrl2X, leftCtrl2Y, leftCtrl1X, leftCtrl1Y, baseX, baseY)
  leafPath.close()

  drawPath(path = leafPath, color = fillColor)
  drawPath(
    path = leafPath,
    color = strokeColor,
    style = Stroke(width = strokeWidth, join = StrokeJoin.Round)
  )

  // Central curved botanical midrib vein
  val veinEndR = startR + length * 0.84f
  val veinMidR = startR + length * 0.45f
  veinPath.reset()
  veinPath.moveTo(baseX, baseY)
  veinPath.quadraticTo(
    cx + ux * veinMidR + px * (tipBend * 0.35f),
    cy + uy * veinMidR + py * (tipBend * 0.35f),
    cx + ux * veinEndR + px * (tipBend * 0.82f),
    cy + uy * veinEndR + py * (tipBend * 0.82f)
  )
  drawPath(
    path = veinPath,
    color = veinColor,
    style = Stroke(width = strokeWidth * 0.65f, cap = StrokeCap.Round)
  )
}

private fun DrawScope.drawOrganicPetal(
  petalPath: Path,
  veinPath: Path,
  cx: Float,
  cy: Float,
  startR: Float,
  length: Float,
  halfWidth: Float,
  angleRad: Float,
  fillColor: Color,
  outlineColor: Color,
  veinColor: Color,
  strokeWidth: Float,
) {
  val ux = cos(angleRad)
  val uy = sin(angleRad)
  val px = -uy
  val py = ux

  val baseX = cx + ux * startR
  val baseY = cy + uy * startR

  val tipR = startR + length
  val tipX = cx + ux * tipR
  val tipY = cy + uy * tipR

  val lobeR1 = startR + length * 0.42f
  val lobeR2 = startR + length * 0.88f

  petalPath.reset()
  petalPath.moveTo(baseX, baseY)
  petalPath.cubicTo(
    cx + ux * lobeR1 + px * (halfWidth * 0.90f),
    cy + uy * lobeR1 + py * (halfWidth * 0.90f),
    cx + ux * lobeR2 + px * (halfWidth * 0.78f),
    cy + uy * lobeR2 + py * (halfWidth * 0.78f),
    tipX,
    tipY
  )
  petalPath.cubicTo(
    cx + ux * lobeR2 - px * (halfWidth * 0.78f),
    cy + uy * lobeR2 - py * (halfWidth * 0.78f),
    cx + ux * lobeR1 - px * (halfWidth * 0.90f),
    cy + uy * lobeR1 - py * (halfWidth * 0.90f),
    baseX,
    baseY
  )
  petalPath.close()

  drawPath(path = petalPath, color = fillColor)
  drawPath(
    path = petalPath,
    color = outlineColor,
    style = Stroke(width = strokeWidth, join = StrokeJoin.Round)
  )

  val veinEndR = startR + length * 0.72f
  veinPath.reset()
  veinPath.moveTo(baseX, baseY)
  veinPath.lineTo(cx + ux * veinEndR, cy + uy * veinEndR)
  drawPath(
    path = veinPath,
    color = veinColor,
    style = Stroke(width = strokeWidth * 0.55f, cap = StrokeCap.Round)
  )
}
