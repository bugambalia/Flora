package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.example.data.model.AlbumPaletteColors
import com.example.data.model.ThemeBackgroundGradients

val LocalAlbumPalette = compositionLocalOf { AlbumPaletteColors() }
val LocalThemeGradients = compositionLocalOf { ThemeBackgroundGradients.Default }

// Asymmetric Botanical Plate Corner Language (DESIGN.md §6)
val FloraPlateShape = RoundedCornerShape(
  topStart = 3.dp,
  topEnd = 18.dp,
  bottomEnd = 3.dp,
  bottomStart = 18.dp
)

val FloraHeroShape = RoundedCornerShape(
  topStart = 4.dp,
  topEnd = 24.dp,
  bottomEnd = 4.dp,
  bottomStart = 4.dp
)

val FloraStripShape = RoundedCornerShape(
  topStart = 2.dp,
  topEnd = 16.dp,
  bottomEnd = 2.dp,
  bottomStart = 16.dp
)

val FloraCardTopRightShape = RoundedCornerShape(
  topStart = 3.dp,
  topEnd = 18.dp,
  bottomEnd = 3.dp,
  bottomStart = 3.dp
)

val FloraThumbShape = RoundedCornerShape(
  topStart = 1.dp,
  topEnd = 11.dp,
  bottomEnd = 1.dp,
  bottomStart = 1.dp
)

val FloraChipShape = RoundedCornerShape(
  topStart = 3.dp,
  topEnd = 13.dp,
  bottomEnd = 3.dp,
  bottomStart = 13.dp
)

private val BotanicalShapes = Shapes(
  small = FloraThumbShape,
  medium = FloraStripShape,
  large = FloraPlateShape,
  extraLarge = FloraHeroShape,
)

@Composable
fun HerbariumTheme(
  albumPalette: AlbumPaletteColors = AlbumPaletteColors(),
  themeGradients: ThemeBackgroundGradients = ThemeBackgroundGradients.Default,
  chameleonEnabled: Boolean = true,
  content: @Composable () -> Unit,
) {
  val targetPrimary = if (chameleonEnabled) lerp(FloraForest, albumPalette.vibrant, 0.35f) else FloraForest
  val targetSecondary = if (chameleonEnabled) lerp(FloraTerracotta, albumPalette.darkVibrant, 0.25f) else FloraTerracotta
  val targetTertiary = FloraCobalt

  val animSpec = tween<Color>(durationMillis = 600)
  val animatedPrimary by animateColorAsState(targetPrimary, animSpec, label = "primaryColor")
  val animatedSecondary by animateColorAsState(targetSecondary, animSpec, label = "secondaryColor")

  val animatedBg by animateColorAsState(
    targetValue = if (chameleonEnabled) themeGradients.parchmentTop else FloraPaper,
    animationSpec = animSpec,
    label = "bgColor"
  )

  val targetGradients = if (chameleonEnabled) themeGradients else ThemeBackgroundGradients.Default
  val animParchmentTop by animateColorAsState(targetGradients.parchmentTop, animSpec, label = "gParchmentTop")
  val animParchmentBottom by animateColorAsState(targetGradients.parchmentBottom, animSpec, label = "gParchmentBottom")
  val animRadialPrimary by animateColorAsState(targetGradients.parchmentRadialPrimary, animSpec, label = "gRadial1")
  val animRadialSecondary by animateColorAsState(targetGradients.parchmentRadialSecondary, animSpec, label = "gRadial2")
  val animHeroStart by animateColorAsState(targetGradients.heroGlowStart, animSpec, label = "gHero1")
  val animHeroEnd by animateColorAsState(targetGradients.heroGlowEnd, animSpec, label = "gHero2")
  val animPlayerTop by animateColorAsState(targetGradients.playerTop, animSpec, label = "gPlayerTop")
  val animPlayerMid by animateColorAsState(targetGradients.playerMiddle, animSpec, label = "gPlayerMid")
  val animPlayerBottom by animateColorAsState(targetGradients.playerBottom, animSpec, label = "gPlayerBottom")
  val animPlayerGlow by animateColorAsState(targetGradients.playerGlow, animSpec, label = "gPlayerGlow")
  val animMiniStart by animateColorAsState(targetGradients.miniPlayerStart, animSpec, label = "gMiniStart")
  val animMiniEnd by animateColorAsState(targetGradients.miniPlayerEnd, animSpec, label = "gMiniEnd")

  val animatedGradients = ThemeBackgroundGradients(
    parchmentTop = animParchmentTop,
    parchmentBottom = animParchmentBottom,
    parchmentRadialPrimary = animRadialPrimary,
    parchmentRadialSecondary = animRadialSecondary,
    heroGlowStart = animHeroStart,
    heroGlowEnd = animHeroEnd,
    playerTop = animPlayerTop,
    playerMiddle = animPlayerMid,
    playerBottom = animPlayerBottom,
    playerGlow = animPlayerGlow,
    miniPlayerStart = animMiniStart,
    miniPlayerEnd = animMiniEnd,
  )

  val colorScheme = lightColorScheme(
    primary = animatedPrimary,
    onPrimary = FloraPlayerCream,
    primaryContainer = FloraSageLight,
    onPrimaryContainer = FloraInk,
    secondary = animatedSecondary,
    onSecondary = FloraPlayerCream,
    secondaryContainer = FloraPaperWarm,
    onSecondaryContainer = FloraInk,
    tertiary = targetTertiary,
    onTertiary = FloraPlayerCream,
    background = animatedBg,
    onBackground = FloraInk,
    surface = FloraPaper,
    onSurface = FloraInk,
    surfaceVariant = FloraPaperWarm,
    onSurfaceVariant = FloraMetaGreen,
    outline = FloraBorderInk,
    outlineVariant = FloraBorderInk.copy(alpha = 0.38f),
    error = FloraTerracotta,
    onError = FloraPlayerCream,
  )

  CompositionLocalProvider(
    LocalAlbumPalette provides albumPalette,
    LocalThemeGradients provides animatedGradients,
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = BotanicalTypography,
      shapes = BotanicalShapes,
      content = content,
    )
  }
}
