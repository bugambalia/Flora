package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.example.data.model.AlbumPaletteColors
import com.example.data.model.AppThemeMode
import com.example.data.model.ThemeBackgroundGradients

val LocalAlbumPalette = compositionLocalOf { AlbumPaletteColors() }
val LocalThemeGradients = compositionLocalOf { ThemeBackgroundGradients.Default }
val LocalFloraColors = compositionLocalOf { LightFloraColors }

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
  themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  content: @Composable () -> Unit,
) {
  val isDark = when (themeMode) {
    AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    AppThemeMode.LIGHT -> false
    AppThemeMode.DARK -> true
  }
  val floraColors = if (isDark) DarkFloraColors else LightFloraColors

  val targetPrimary = if (chameleonEnabled) lerp(floraColors.forest, albumPalette.vibrant, 0.35f) else floraColors.forest
  val targetSecondary = if (chameleonEnabled) lerp(floraColors.terracotta, albumPalette.darkVibrant, 0.25f) else floraColors.terracotta
  val targetTertiary = floraColors.cobalt

  val animSpec = tween<Color>(durationMillis = 600)
  val animatedPrimary by animateColorAsState(targetPrimary, animSpec, label = "primaryColor")
  val animatedSecondary by animateColorAsState(targetSecondary, animSpec, label = "secondaryColor")

  val baseGradients = if (isDark) {
    val raw = if (chameleonEnabled) themeGradients else ThemeBackgroundGradients.Default
    raw.copy(
      parchmentTop = if (chameleonEnabled) lerp(DarkFloraColors.surfaceBgTop, raw.parchmentTop, 0.10f) else DarkFloraColors.surfaceBgTop,
      parchmentBottom = if (chameleonEnabled) lerp(DarkFloraColors.surfaceBgBottom, raw.parchmentBottom, 0.10f) else DarkFloraColors.surfaceBgBottom,
      parchmentRadialPrimary = raw.parchmentRadialPrimary.copy(alpha = 0.12f),
      parchmentRadialSecondary = raw.parchmentRadialSecondary.copy(alpha = 0.14f),
    )
  } else {
    if (chameleonEnabled) themeGradients else ThemeBackgroundGradients.Default
  }

  val animatedBg by animateColorAsState(
    targetValue = if (chameleonEnabled) baseGradients.parchmentTop else floraColors.paper,
    animationSpec = animSpec,
    label = "bgColor"
  )

  val animParchmentTop by animateColorAsState(baseGradients.parchmentTop, animSpec, label = "gParchmentTop")
  val animParchmentBottom by animateColorAsState(baseGradients.parchmentBottom, animSpec, label = "gParchmentBottom")
  val animRadialPrimary by animateColorAsState(baseGradients.parchmentRadialPrimary, animSpec, label = "gRadial1")
  val animRadialSecondary by animateColorAsState(baseGradients.parchmentRadialSecondary, animSpec, label = "gRadial2")
  val animHeroStart by animateColorAsState(baseGradients.heroGlowStart, animSpec, label = "gHero1")
  val animHeroEnd by animateColorAsState(baseGradients.heroGlowEnd, animSpec, label = "gHero2")
  val animPlayerTop by animateColorAsState(baseGradients.playerTop, animSpec, label = "gPlayerTop")
  val animPlayerMid by animateColorAsState(baseGradients.playerMiddle, animSpec, label = "gPlayerMid")
  val animPlayerBottom by animateColorAsState(baseGradients.playerBottom, animSpec, label = "gPlayerBottom")
  val animPlayerGlow by animateColorAsState(baseGradients.playerGlow, animSpec, label = "gPlayerGlow")
  val animMiniStart by animateColorAsState(baseGradients.miniPlayerStart, animSpec, label = "gMiniStart")
  val animMiniEnd by animateColorAsState(baseGradients.miniPlayerEnd, animSpec, label = "gMiniEnd")

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

  val colorScheme = if (isDark) {
    darkColorScheme(
      primary = animatedPrimary,
      onPrimary = FloraPlayerCream,
      primaryContainer = floraColors.sageLight,
      onPrimaryContainer = floraColors.ink,
      secondary = animatedSecondary,
      onSecondary = FloraPlayerCream,
      secondaryContainer = floraColors.paperWarm,
      onSecondaryContainer = floraColors.ink,
      tertiary = targetTertiary,
      onTertiary = FloraPlayerCream,
      background = animatedBg,
      onBackground = floraColors.ink,
      surface = floraColors.paper,
      onSurface = floraColors.ink,
      surfaceVariant = floraColors.paperWarm,
      onSurfaceVariant = floraColors.metaGreen,
      outline = floraColors.borderInk,
      outlineVariant = floraColors.borderInk.copy(alpha = 0.45f),
      error = floraColors.terracotta,
      onError = FloraPlayerCream,
    )
  } else {
    lightColorScheme(
      primary = animatedPrimary,
      onPrimary = FloraPlayerCream,
      primaryContainer = floraColors.sageLight,
      onPrimaryContainer = floraColors.ink,
      secondary = animatedSecondary,
      onSecondary = FloraPlayerCream,
      secondaryContainer = floraColors.paperWarm,
      onSecondaryContainer = floraColors.ink,
      tertiary = targetTertiary,
      onTertiary = FloraPlayerCream,
      background = animatedBg,
      onBackground = floraColors.ink,
      surface = floraColors.paper,
      onSurface = floraColors.ink,
      surfaceVariant = floraColors.paperWarm,
      onSurfaceVariant = floraColors.metaGreen,
      outline = floraColors.borderInk,
      outlineVariant = floraColors.borderInk.copy(alpha = 0.38f),
      error = floraColors.terracotta,
      onError = FloraPlayerCream,
    )
  }

  CompositionLocalProvider(
    LocalAlbumPalette provides albumPalette,
    LocalThemeGradients provides animatedGradients,
    LocalFloraColors provides floraColors,
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = BotanicalTypography,
      shapes = BotanicalShapes,
      content = content,
    )
  }
}
