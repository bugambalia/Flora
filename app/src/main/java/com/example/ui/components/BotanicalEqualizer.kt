package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioEffectsState
import com.example.data.model.BotanicalCatalog
import com.example.data.model.EqualizerPreset
import com.example.ui.theme.FloraBorderInk
import com.example.ui.theme.FloraCobalt
import com.example.ui.theme.FloraDarkMeta
import com.example.ui.theme.FloraForest
import com.example.ui.theme.FloraGold
import com.example.ui.theme.FloraGoldSoft
import com.example.ui.theme.FloraHeroShape
import com.example.ui.theme.FloraInk
import com.example.ui.theme.FloraPaperCard
import com.example.ui.theme.FloraPaperWarm
import com.example.ui.theme.FloraPlateShape
import com.example.ui.theme.FloraPlayerCream
import com.example.ui.theme.FloraSage
import com.example.ui.theme.FloraSageLight
import com.example.ui.theme.FloraShadowPaper
import com.example.ui.theme.FloraStripShape
import com.example.ui.theme.FloraTerracotta
import com.example.ui.theme.LocalFloraColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class BotanicalStemType(
  val speciesLabel: String,
  val frequencyLabel: String,
  val swayFrequencyMultiplier: Float,
  val phaseOffsetRad: Float,
) {
  FERN_RHIZOME("Helecho", "60", 1.0f, 0.0f),
  CALATHEA_PETIOLE("Calatea", "230", 1.0f, 1.15f),
  LILY_STALK("Lirio", "910", 2.0f, 2.35f),
  LAVENDER_SPIKE("Espiga", "3.6K", 2.0f, 3.60f),
  PAPYRUS_PLUME("Papiro", "14K", 3.0f, 4.90f);

  companion object {
    fun forBandIndex(index: Int): BotanicalStemType =
      entries.getOrElse(index.coerceIn(0, entries.lastIndex)) { LILY_STALK }
  }
}

data class BotanicalStemKinematics(
  val heightFraction: Float,
  val swayAmplitudePx: Float,
  val bloomOpenness: Float,
  val tipSwayOffsetX: Float,
  val midBendOffsetX: Float,
)

/**
 * Computes deterministic height, sway intensity, and botanical openness from a
 * normalized frequency band gain [0f..1f] and the continuous sway phase [0..2π].
 */
fun computeBotanicalStemKinematics(
  normalizedGain: Float,
  stemType: BotanicalStemType,
  swayClockRad: Float,
  maxSwayPx: Float,
  enabled: Boolean,
  isPlaying: Boolean,
): BotanicalStemKinematics {
  val g = normalizedGain.coerceIn(0f, 1f)
  val heightFraction = 0.12f + g * 0.88f
  val activityScale = when {
    !enabled -> 0.08f
    !isPlaying -> 0.42f
    else -> 1.0f
  }
  val swayAmplitudePx = maxSwayPx * (0.06f + g * 0.94f) * activityScale
  val bloomOpenness = 0.22f + g * 0.78f
  val primaryWave = sin(swayClockRad * stemType.swayFrequencyMultiplier + stemType.phaseOffsetRad)
  val secondaryHarmonic = sin(
    swayClockRad * (stemType.swayFrequencyMultiplier + 1.0f) + stemType.phaseOffsetRad * 0.65f
  ) * 0.18f
  val tipSwayOffsetX = swayAmplitudePx * (primaryWave + secondaryHarmonic)
  val midBendOffsetX = swayAmplitudePx * 0.46f * primaryWave
  return BotanicalStemKinematics(
    heightFraction = heightFraction,
    swayAmplitudePx = swayAmplitudePx,
    bloomOpenness = bloomOpenness,
    tipSwayOffsetX = tipSwayOffsetX,
    midBendOffsetX = midBendOffsetX,
  )
}

@Composable
fun BotanicalEqualizerPanel(
  effectsState: AudioEffectsState,
  onSelectPreset: (EqualizerPreset) -> Unit,
  onBandLevelChange: (Int, Int) -> Unit,
  onResetFlat: () -> Unit,
  onBassBoostChange: (Int) -> Unit,
  onVirtualizerChange: (Int) -> Unit,
  modifier: Modifier = Modifier,
  isPlaying: Boolean = true,
) {
  val enabled = effectsState.equalizerEnabled
  val animatedBandNorms = effectsState.bandLevelsMb.mapIndexed { idx, mb ->
    val target = ((mb - effectsState.minBandLevelMb).toFloat() /
      (effectsState.maxBandLevelMb - effectsState.minBandLevelMb)).coerceIn(0f, 1f)
    animateFloatAsState(
      targetValue = target,
      animationSpec = spring(dampingRatio = 0.74f, stiffness = 320f),
      label = "eqBandNorm_$idx"
    ).value
  }

  val swayTransition = rememberInfiniteTransition(label = "botanicalStemSway")
  val swayClockRad by swayTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2.0 * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 4800, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "stemSwayClock"
  )

  val density = LocalDensity.current
  val maxSwayPx = remember(density) { with(density) { 15.dp.toPx() } }
  val stemKinematicsList = animatedBandNorms.mapIndexed { idx, norm ->
    computeBotanicalStemKinematics(
      normalizedGain = norm,
      stemType = BotanicalStemType.forBandIndex(idx),
      swayClockRad = swayClockRad,
      maxSwayPx = maxSwayPx,
      enabled = enabled,
      isPlaying = isPlaying,
    )
  }

  val primary4Presets = listOf(
    EqualizerPreset.CUSTOM,
    EqualizerPreset.BOTANICAL_NATURAL,
    EqualizerPreset.DEEP_ROOTS,
    EqualizerPreset.FOREST_CLARITY
  )
  val specializedPresets = EqualizerPreset.entries.filterNot { it in primary4Presets }
  val colors = LocalFloraColors.current

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Dark Forest Living Botanical Stem Equalizer Conservatory
    FloraMountedPlate(
      shape = FloraHeroShape,
      containerColor = FloraForest,
      borderColor = FloraBorderInk,
      goldHairline = true,
      shadowOffsetX = 4.dp,
      shadowOffsetY = 5.dp,
      shadowColor = FloraShadowPaper,
      modifier = Modifier
        .fillMaxWidth()
        .alpha(if (enabled) 1f else 0.45f)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(Brush.linearGradient(listOf(FloraForest, FloraInk)))
          .padding(horizontal = 14.dp, vertical = 18.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(232.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(
              modifier = Modifier
                .height(204.dp)
                .padding(end = 6.dp),
              verticalArrangement = Arrangement.SpaceBetween,
              horizontalAlignment = Alignment.End
            ) {
              Text("+10", style = MaterialTheme.typography.labelSmall, color = FloraDarkMeta)
              Text("0", style = MaterialTheme.typography.labelSmall, color = FloraDarkMeta)
              Text("-10", style = MaterialTheme.typography.labelSmall, color = FloraDarkMeta)
            }

            Box(
              modifier = Modifier
                .weight(1f)
                .height(228.dp)
            ) {
              // Conservatory soil bed, 0 dB horizon line, and golden pollen curve across swaying tips
              Canvas(modifier = Modifier.fillMaxSize()) {
                val topPad = 18.dp.toPx()
                val botPad = size.height - 14.dp.toPx()
                val usableH = (botPad - topPad).coerceAtLeast(1f)
                val midY = topPad + usableH * 0.5f

                // 0 dB dashed horizon rule
                drawLine(
                  color = FloraGold.copy(alpha = 0.22f),
                  start = Offset(0f, midY),
                  end = Offset(size.width, midY),
                  strokeWidth = 1.dp.toPx(),
                  pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                )

                // Root soil ground line
                drawLine(
                  color = FloraSage.copy(alpha = 0.55f),
                  start = Offset(4.dp.toPx(), botPad),
                  end = Offset(size.width - 4.dp.toPx(), botPad),
                  strokeWidth = 2.dp.toPx(),
                  cap = StrokeCap.Round
                )

                val tipPoints = stemKinematicsList.mapIndexed { idx, kin ->
                  val step = size.width / stemKinematicsList.size.coerceAtLeast(1)
                  val baseX = step * idx + step / 2f
                  val tipY = botPad - kin.heightFraction * usableH
                  val tipX = baseX + kin.tipSwayOffsetX
                  Offset(tipX, tipY)
                }

                if (tipPoints.size >= 2) {
                  val vinePath = Path().apply {
                    moveTo(tipPoints.first().x, tipPoints.first().y)
                    for (i in 0 until tipPoints.lastIndex) {
                      val p0 = tipPoints[i]
                      val p1 = tipPoints[i + 1]
                      val cx = (p0.x + p1.x) / 2f
                      cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                  }
                  drawPath(
                    path = vinePath,
                    color = FloraGold.copy(alpha = 0.26f),
                    style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                  )
                }
              }

              Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly
              ) {
                effectsState.bandLevelsMb.forEachIndexed { index, levelMb ->
                  val stemType = BotanicalStemType.forBandIndex(index)
                  val kinematics = stemKinematicsList.getOrElse(index) {
                    computeBotanicalStemKinematics(
                      normalizedGain = 0.5f,
                      stemType = stemType,
                      swayClockRad = swayClockRad,
                      maxSwayPx = maxSwayPx,
                      enabled = enabled,
                      isPlaying = isPlaying,
                    )
                  }
                  BotanicalStemFader(
                    stemType = stemType,
                    levelMb = levelMb,
                    kinematics = kinematics,
                    minMb = effectsState.minBandLevelMb,
                    maxMb = effectsState.maxBandLevelMb,
                    enabled = enabled,
                    onLevelChange = { newMb -> onBandLevelChange(index, newMb) },
                    modifier = Modifier
                      .weight(1f)
                      .testTag("eq_band_slider_$index")
                  )
                }
              }
            }
          }

          // Frequency, live gain readout & plant stem label row
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 28.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
          ) {
            effectsState.bandLevelsMb.forEachIndexed { index, levelMb ->
              val stemType = BotanicalStemType.forBandIndex(index)
              val dbVal = levelMb / 100f
              val dbLabel = String.format("%+.1f", dbVal)
              Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
              ) {
                Text(
                  text = stemType.frequencyLabel,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                  ),
                  color = FloraPlayerCream
                )
                Text(
                  text = stemType.speciesLabel,
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                  color = FloraDarkMeta.copy(alpha = 0.8f),
                  maxLines = 1
                )
                Text(
                  text = dbLabel,
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                  color = when {
                    levelMb > 30 -> FloraGoldSoft
                    levelMb < -30 -> FloraDarkMeta.copy(alpha = 0.72f)
                    else -> FloraDarkMeta
                  }
                )
              }
            }
          }
        }
      }
    }

    // 2x2 Asymmetric Preset Grid
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      primary4Presets.chunked(2).forEach { rowPresets ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          rowPresets.forEach { preset ->
            val selected = effectsState.preset == preset
            val bgColor by animateColorAsState(
              targetValue = if (selected) colors.terracotta else colors.paperWarm,
              animationSpec = tween(220),
              label = "presetBg_${preset.name}"
            )
            Surface(
              shape = FloraPlateShape,
              color = bgColor,
              border = BorderStroke(
                1.dp,
                if (selected) colors.terracotta else colors.borderInk.copy(alpha = 0.35f)
              ),
              modifier = Modifier
                .weight(1f)
                .height(54.dp)
                .clip(FloraPlateShape)
                .clickable(enabled = enabled) { onSelectPreset(preset) }
                .testTag("eq_preset_${preset.name.lowercase()}")
            ) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
              ) {
                Text(
                  text = preset.label,
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                  ),
                  color = if (selected) FloraPlayerCream else colors.ink,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }
    }

    // Botanical Presets Strip + 0 dB Reset
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        modifier = Modifier
          .weight(1f)
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        specializedPresets.forEach { preset ->
          val isSelected = effectsState.preset == preset
          val species = BotanicalCatalog.getById(preset.speciesId)
          Surface(
            shape = FloraStripShape,
            color = if (isSelected) colors.terracotta else colors.paperCard,
            border = BorderStroke(
              width = if (isSelected) 1.5.dp else 1.dp,
              color = if (isSelected) colors.terracotta else colors.borderInk.copy(alpha = 0.45f)
            ),
            modifier = Modifier
              .clip(FloraStripShape)
              .clickable(enabled = enabled) { onSelectPreset(preset) }
              .testTag("eq_preset_${preset.name.lowercase()}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              BotanicalSpecimenIllustration(
                speciesId = species.id,
                modifier = Modifier.size(24.dp)
              )
              Text(
                text = preset.label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isSelected) FloraPlayerCream else colors.ink,
                maxLines = 1
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.width(6.dp))
      TextButton(
        onClick = onResetFlat,
        enabled = enabled,
        modifier = Modifier.testTag("eq_reset_flat_button")
      ) {
        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.terracotta)
        Spacer(modifier = Modifier.width(4.dp))
        Text("0 dB", style = MaterialTheme.typography.labelLarge, color = colors.terracotta)
      }
    }

    // Bass Boost & 3D Virtualizer
    FloraMountedPlate(
      shape = FloraStripShape,
      containerColor = colors.paperCard,
      borderColor = colors.borderInk.copy(alpha = 0.45f),
      shadowOffsetX = 0.dp,
      shadowOffsetY = 0.dp,
      modifier = Modifier
        .fillMaxWidth()
        .alpha(if (enabled) 1f else 0.55f)
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Graves", style = MaterialTheme.typography.titleSmall, color = colors.ink)
          Text(
            "${effectsState.bassBoostStrength / 10}%",
            style = MaterialTheme.typography.labelLarge,
            color = colors.terracotta
          )
        }
        Slider(
          value = effectsState.bassBoostStrength.toFloat(),
          onValueChange = { onBassBoostChange(it.roundToInt()) },
          enabled = enabled,
          valueRange = 0f..1000f,
          colors = SliderDefaults.colors(
            thumbColor = colors.terracotta,
            activeTrackColor = colors.terracotta,
            inactiveTrackColor = colors.borderInk.copy(alpha = 0.2f)
          ),
          modifier = Modifier.testTag("bass_boost_slider")
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Espacial 3D", style = MaterialTheme.typography.titleSmall, color = colors.ink)
          Text(
            "${effectsState.virtualizerStrength / 10}%",
            style = MaterialTheme.typography.labelLarge,
            color = colors.forest
          )
        }
        Slider(
          value = effectsState.virtualizerStrength.toFloat(),
          onValueChange = { onVirtualizerChange(it.roundToInt()) },
          enabled = enabled,
          valueRange = 0f..1000f,
          colors = SliderDefaults.colors(
            thumbColor = colors.forest,
            activeTrackColor = colors.forest,
            inactiveTrackColor = colors.borderInk.copy(alpha = 0.2f)
          ),
          modifier = Modifier.testTag("virtualizer_slider")
        )
      }
    }
  }
}

@Composable
private fun BotanicalStemFader(
  stemType: BotanicalStemType,
  levelMb: Int,
  kinematics: BotanicalStemKinematics,
  minMb: Int,
  maxMb: Int,
  enabled: Boolean,
  onLevelChange: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val currentMin by rememberUpdatedState(minMb)
  val currentMax by rememberUpdatedState(maxMb)
  val currentCallback by rememberUpdatedState(onLevelChange)
  val dbText = String.format("%+.1f", levelMb / 100f)
  val reusablePath = remember { Path() }
  val leafPath = remember { Path() }

  Box(
    modifier = modifier
      .height(228.dp)
      .semantics {
        contentDescription = "Tallo ${stemType.speciesLabel} (${stemType.frequencyLabel} Hz) a $dbText decibelios"
      }
      .pointerInput(enabled) {
        if (!enabled) return@pointerInput
        detectTapGestures { offset ->
          val topPad = 18.dp.toPx()
          val botPad = size.height - 14.dp.toPx()
          val usable = (botPad - topPad).coerceAtLeast(1f)
          val norm = (1f - ((offset.y - topPad) / usable)).coerceIn(0f, 1f)
          val mb = (currentMin + norm * (currentMax - currentMin)).roundToInt()
          currentCallback(mb)
        }
      }
      .pointerInput(enabled) {
        if (!enabled) return@pointerInput
        detectVerticalDragGestures { change, _ ->
          change.consume()
          val topPad = 18.dp.toPx()
          val botPad = size.height - 14.dp.toPx()
          val usable = (botPad - topPad).coerceAtLeast(1f)
          val norm = (1f - ((change.position.y - topPad) / usable)).coerceIn(0f, 1f)
          val mb = (currentMin + norm * (currentMax - currentMin)).roundToInt()
          currentCallback(mb)
        }
      }
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = size.width / 2f
      val topPad = 18.dp.toPx()
      val botPad = size.height - 14.dp.toPx()
      val usableH = (botPad - topPad).coerceAtLeast(1f)

      val tipY = botPad - kinematics.heightFraction * usableH
      val tipX = cx + kinematics.tipSwayOffsetX
      val p0 = Offset(cx, botPad)
      val p1 = Offset(cx + kinematics.midBendOffsetX * 0.4f, botPad - (botPad - tipY) * 0.36f)
      val p2 = Offset(cx + kinematics.midBendOffsetX + kinematics.tipSwayOffsetX * 0.45f, botPad - (botPad - tipY) * 0.72f)
      val p3 = Offset(tipX, tipY)

      // Faint botanical measurement stem guide
      drawLine(
        color = Color.White.copy(alpha = 0.08f),
        start = Offset(cx, topPad),
        end = Offset(cx, botPad),
        strokeWidth = 1.5.dp.toPx(),
        cap = StrokeCap.Round
      )

      // Root node anchored in soil
      drawCircle(
        color = FloraSage.copy(alpha = 0.85f),
        radius = 4.dp.toPx(),
        center = p0
      )

      when (stemType) {
        BotanicalStemType.FERN_RHIZOME -> drawFernStem(
          stemPath = reusablePath,
          leafPath = leafPath,
          p0 = p0, p1 = p1, p2 = p2, p3 = p3,
          openness = kinematics.bloomOpenness,
          swayOffset = kinematics.tipSwayOffsetX,
        )
        BotanicalStemType.CALATHEA_PETIOLE -> drawCalatheaStem(
          stemPath = reusablePath,
          leafPath = leafPath,
          p0 = p0, p1 = p1, p2 = p2, p3 = p3,
          openness = kinematics.bloomOpenness,
        )
        BotanicalStemType.LILY_STALK -> drawLilyStem(
          stemPath = reusablePath,
          leafPath = leafPath,
          p0 = p0, p1 = p1, p2 = p2, p3 = p3,
          openness = kinematics.bloomOpenness,
        )
        BotanicalStemType.LAVENDER_SPIKE -> drawLavenderSpikeStem(
          stemPath = reusablePath,
          leafPath = leafPath,
          p0 = p0, p1 = p1, p2 = p2, p3 = p3,
          openness = kinematics.bloomOpenness,
        )
        BotanicalStemType.PAPYRUS_PLUME -> drawPapyrusStem(
          stemPath = reusablePath,
          p0 = p0, p1 = p1, p2 = p2, p3 = p3,
          openness = kinematics.bloomOpenness,
          swayOffset = kinematics.tipSwayOffsetX,
        )
      }
    }
  }
}

private fun evalCubicBezier(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
  val u = 1f - t
  val tt = t * t
  val uu = u * u
  val uuu = uu * u
  val ttt = tt * t
  val x = uuu * p0.x + 3f * uu * t * p1.x + 3f * u * tt * p2.x + ttt * p3.x
  val y = uuu * p0.y + 3f * uu * t * p1.y + 3f * u * tt * p2.y + ttt * p3.y
  return Offset(x, y)
}

private fun evalCubicTangentAngle(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Float {
  val u = 1f - t
  val dx = 3f * u * u * (p1.x - p0.x) + 6f * u * t * (p2.x - p1.x) + 3f * t * t * (p3.x - p2.x)
  val dy = 3f * u * u * (p1.y - p0.y) + 6f * u * t * (p2.y - p1.y) + 3f * t * t * (p3.y - p2.y)
  return kotlin.math.atan2(dy, dx)
}

private fun DrawScope.drawStemSpine(
  stemPath: Path,
  p0: Offset,
  p1: Offset,
  p2: Offset,
  p3: Offset,
  color: Color,
  strokeWidthPx: Float,
) {
  stemPath.reset()
  stemPath.moveTo(p0.x, p0.y)
  stemPath.cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
  drawPath(
    path = stemPath,
    color = color,
    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
  )
}

private fun DrawScope.drawCurvedStemLeaf(
  leafPath: Path,
  origin: Offset,
  angleRad: Float,
  lengthPx: Float,
  widthPx: Float,
  fillColor: Color,
  strokeColor: Color,
) {
  val ux = cos(angleRad)
  val uy = sin(angleRad)
  val px = -uy
  val py = ux
  val tip = Offset(origin.x + ux * lengthPx, origin.y + uy * lengthPx)
  val c1 = Offset(origin.x + ux * lengthPx * 0.45f + px * widthPx, origin.y + uy * lengthPx * 0.45f + py * widthPx)
  val c2 = Offset(origin.x + ux * lengthPx * 0.45f - px * widthPx, origin.y + uy * lengthPx * 0.45f - py * widthPx)

  leafPath.reset()
  leafPath.moveTo(origin.x, origin.y)
  leafPath.quadraticTo(c1.x, c1.y, tip.x, tip.y)
  leafPath.quadraticTo(c2.x, c2.y, origin.x, origin.y)
  leafPath.close()

  drawPath(path = leafPath, color = fillColor)
  drawPath(path = leafPath, color = strokeColor, style = Stroke(width = 1.dp.toPx()))
  drawLine(
    color = strokeColor.copy(alpha = 0.65f),
    start = origin,
    end = Offset(origin.x + ux * lengthPx * 0.78f, origin.y + uy * lengthPx * 0.78f),
    strokeWidth = 0.8.dp.toPx(),
    cap = StrokeCap.Round
  )
}

/** 60 Hz — Sturdy woody fern culm with alternating pinnae and a coiled fiddlehead crown. */
private fun DrawScope.drawFernStem(
  stemPath: Path,
  leafPath: Path,
  p0: Offset,
  p1: Offset,
  p2: Offset,
  p3: Offset,
  openness: Float,
  swayOffset: Float,
) {
  val stemColor = lerp(FloraSage, FloraGold, 0.28f)
  drawStemSpine(stemPath, p0, p1, p2, p3, stemColor, 3.8.dp.toPx())

  val frondCount = 5
  for (i in 0 until frondCount) {
    val t = 0.24f + i * 0.14f
    val pt = evalCubicBezier(p0, p1, p2, p3, t)
    val tangent = evalCubicTangentAngle(p0, p1, p2, p3, t)
    val side = if (i % 2 == 0) 1f else -1f
    val frondLen = (9.dp.toPx() + openness * 9.dp.toPx()) * (1.05f - i * 0.11f)
    val leafAngle = tangent + side * (1.05f - openness * 0.25f)
    drawCurvedStemLeaf(
      leafPath = leafPath,
      origin = pt,
      angleRad = leafAngle,
      lengthPx = frondLen,
      widthPx = frondLen * 0.34f,
      fillColor = lerp(FloraSage, FloraSageLight, 0.22f + openness * 0.18f).copy(alpha = 0.90f),
      strokeColor = FloraGoldSoft.copy(alpha = 0.72f),
    )
  }

  // Unfurling golden fiddlehead crozier at tip
  val curlRadius = 4.5.dp.toPx() + openness * 3.5.dp.toPx()
  val curlCenter = Offset(p3.x + (if (swayOffset >= 0f) 2f else -2f), p3.y - curlRadius * 0.3f)
  drawCircle(
    color = FloraGoldSoft,
    radius = curlRadius,
    center = curlCenter,
    style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
  )
  drawCircle(
    color = FloraPlayerCream,
    radius = 2.8.dp.toPx() + openness * 1.4.dp.toPx(),
    center = curlCenter
  )
}

/** 230 Hz — Lush broad-leaf Calathea petiole with veined foliage and warm spathe bud. */
private fun DrawScope.drawCalatheaStem(
  stemPath: Path,
  leafPath: Path,
  p0: Offset,
  p1: Offset,
  p2: Offset,
  p3: Offset,
  openness: Float,
) {
  val stemColor = lerp(FloraSageLight, FloraSage, 0.55f)
  drawStemSpine(stemPath, p0, p1, p2, p3, stemColor, 3.0.dp.toPx())

  val nodes = listOf(0.32f to -1f, 0.56f to 1f, 0.76f to -1f)
  nodes.forEachIndexed { idx, (t, side) ->
    val pt = evalCubicBezier(p0, p1, p2, p3, t)
    val tangent = evalCubicTangentAngle(p0, p1, p2, p3, t)
    val len = (11.dp.toPx() + openness * 9.5.dp.toPx()) * (1f - idx * 0.12f)
    drawCurvedStemLeaf(
      leafPath = leafPath,
      origin = pt,
      angleRad = tangent + side * (0.95f - openness * 0.20f),
      lengthPx = len,
      widthPx = len * 0.52f,
      fillColor = lerp(FloraSage, FloraGoldSoft, idx * 0.12f + openness * 0.14f).copy(alpha = 0.92f),
      strokeColor = FloraPlayerCream.copy(alpha = 0.78f),
    )
  }

  // Broad crown leaf / terracotta spathe bud
  val budLen = 8.dp.toPx() + openness * 6.dp.toPx()
  val tipAngle = evalCubicTangentAngle(p0, p1, p2, p3, 1f)
  drawCurvedStemLeaf(
    leafPath = leafPath,
    origin = p3,
    angleRad = tipAngle,
    lengthPx = budLen,
    widthPx = budLen * 0.56f,
    fillColor = lerp(FloraTerracotta, FloraGoldSoft, 0.35f + openness * 0.25f),
    strokeColor = FloraPlayerCream,
  )
}

/** 910 Hz — Upright flowering lily stalk whose multi-petal crown opens wider with gain. */
private fun DrawScope.drawLilyStem(
  stemPath: Path,
  leafPath: Path,
  p0: Offset,
  p1: Offset,
  p2: Offset,
  p3: Offset,
  openness: Float,
) {
  drawStemSpine(stemPath, p0, p1, p2, p3, FloraSageLight.copy(alpha = 0.88f), 2.5.dp.toPx())

  // Opposite slender lance leaves
  val leafNodes = listOf(0.36f to -1f, 0.38f to 1f, 0.64f to -1f, 0.66f to 1f)
  leafNodes.forEach { (t, side) ->
    val pt = evalCubicBezier(p0, p1, p2, p3, t)
    val tangent = evalCubicTangentAngle(p0, p1, p2, p3, t)
    val len = 8.dp.toPx() + openness * 6.dp.toPx()
    drawCurvedStemLeaf(
      leafPath = leafPath,
      origin = pt,
      angleRad = tangent + side * 0.78f,
      lengthPx = len,
      widthPx = len * 0.26f,
      fillColor = FloraSage.copy(alpha = 0.92f),
      strokeColor = FloraGoldSoft.copy(alpha = 0.7f),
    )
  }

  // 5-petal radial lily blossom opening with gain
  val petalCount = 5
  val petalLen = 6.dp.toPx() + openness * 7.5.dp.toPx()
  val tipTangent = evalCubicTangentAngle(p0, p1, p2, p3, 1f)
  val spreadSpan = 0.75f + openness * 1.45f
  for (i in 0 until petalCount) {
    val fraction = if (petalCount > 1) (i.toFloat() / (petalCount - 1)) - 0.5f else 0f
    val petalAngle = tipTangent + fraction * spreadSpan * 2f
    drawCurvedStemLeaf(
      leafPath = leafPath,
      origin = p3,
      angleRad = petalAngle,
      lengthPx = petalLen,
      widthPx = petalLen * 0.40f,
      fillColor = lerp(FloraTerracotta, FloraGoldSoft, (i % 2) * 0.35f + openness * 0.25f),
      strokeColor = FloraPlayerCream.copy(alpha = 0.90f),
    )
  }
  drawCircle(
    color = FloraPlayerCream,
    radius = 3.2.dp.toPx() + openness * 1.5.dp.toPx(),
    center = p3
  )
}

/** 3.6K Hz — Slender lavender/bellflower raceme spike lined with ascending floret clusters. */
private fun DrawScope.drawLavenderSpikeStem(
  stemPath: Path,
  leafPath: Path,
  p0: Offset,
  p1: Offset,
  p2: Offset,
  p3: Offset,
  openness: Float,
) {
  drawStemSpine(stemPath, p0, p1, p2, p3, FloraGoldSoft.copy(alpha = 0.85f), 2.1.dp.toPx())

  // Basal slender leaves
  listOf(-1f, 1f).forEach { side ->
    val pt = evalCubicBezier(p0, p1, p2, p3, 0.24f)
    val tangent = evalCubicTangentAngle(p0, p1, p2, p3, 0.24f)
    drawCurvedStemLeaf(
      leafPath = leafPath,
      origin = pt,
      angleRad = tangent + side * 0.60f,
      lengthPx = 10.dp.toPx() + openness * 4.dp.toPx(),
      widthPx = 2.4.dp.toPx(),
      fillColor = FloraSage,
      strokeColor = FloraSageLight.copy(alpha = 0.7f),
    )
  }

  // Paired floret buds ascending upper half of the spike
  val floretLevels = listOf(0.50f, 0.62f, 0.74f, 0.86f, 0.96f)
  floretLevels.forEachIndexed { idx, t ->
    val pt = evalCubicBezier(p0, p1, p2, p3, t)
    val tangent = evalCubicTangentAngle(p0, p1, p2, p3, t)
    val nx = -sin(tangent)
    val ny = cos(tangent)
    val budR = (2.1.dp.toPx() + openness * 1.7.dp.toPx()) * (1.05f - idx * 0.10f)
    val offsetDist = 2.6.dp.toPx() + openness * 1.6.dp.toPx()
    val budColor = lerp(FloraCobalt, FloraPlayerCream, 0.45f + idx * 0.08f)

    drawCircle(
      color = budColor,
      radius = budR,
      center = Offset(pt.x + nx * offsetDist, pt.y + ny * offsetDist)
    )
    drawCircle(
      color = budColor,
      radius = budR,
      center = Offset(pt.x - nx * offsetDist, pt.y - ny * offsetDist)
    )
  }

  // Crown apex bud
  drawCircle(
    color = FloraGoldSoft,
    radius = 3.5.dp.toPx() + openness * 1.5.dp.toPx(),
    center = p3
  )
  drawCircle(
    color = FloraPlayerCream,
    radius = 1.6.dp.toPx(),
    center = p3
  )
}

/** 14K Hz — Delicate papyrus culm crowned with fanning feathery filament rays & pollen tips. */
private fun DrawScope.drawPapyrusStem(
  stemPath: Path,
  p0: Offset,
  p1: Offset,
  p2: Offset,
  p3: Offset,
  openness: Float,
  swayOffset: Float,
) {
  drawStemSpine(stemPath, p0, p1, p2, p3, FloraGoldSoft.copy(alpha = 0.92f), 1.6.dp.toPx())

  // Radiating papyrus umbel filaments fanning outward with gain
  val rayCount = 9
  val rayLength = 7.dp.toPx() + openness * 10.dp.toPx()
  val tipTangent = evalCubicTangentAngle(p0, p1, p2, p3, 1f)
  val fanSpan = 1.25f + openness * 1.35f
  for (i in 0 until rayCount) {
    val normPos = (i.toFloat() / (rayCount - 1)) - 0.5f
    val rayAngle = tipTangent + normPos * fanSpan * 2f + (swayOffset * 0.012f)
    val endPt = Offset(
      p3.x + cos(rayAngle) * rayLength,
      p3.y + sin(rayAngle) * rayLength
    )
    val ctrlPt = Offset(
      p3.x + cos(rayAngle) * rayLength * 0.52f,
      p3.y + sin(rayAngle) * rayLength * 0.42f - 2.dp.toPx()
    )
    stemPath.reset()
    stemPath.moveTo(p3.x, p3.y)
    stemPath.quadraticTo(ctrlPt.x, ctrlPt.y, endPt.x, endPt.y)
    drawPath(
      path = stemPath,
      color = lerp(FloraGoldSoft, FloraPlayerCream, 0.35f).copy(alpha = 0.88f),
      style = Stroke(width = 1.1.dp.toPx(), cap = StrokeCap.Round)
    )
    drawCircle(
      color = FloraPlayerCream.copy(alpha = 0.65f + openness * 0.35f),
      radius = 1.4.dp.toPx() + openness * 1.1.dp.toPx(),
      center = endPt
    )
  }

  drawCircle(
    color = FloraGoldSoft,
    radius = 3.2.dp.toPx(),
    center = p3
  )
  drawCircle(
    color = FloraInk,
    radius = 3.2.dp.toPx(),
    center = p3,
    style = Stroke(1.dp.toPx())
  )
}
