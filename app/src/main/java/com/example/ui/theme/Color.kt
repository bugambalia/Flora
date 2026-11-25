package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Flora — Herbario Musical Official Design Tokens
val FloraInk = Color(0xFF173026)
val FloraForest = Color(0xFF204235)
val FloraForestSoft = Color(0xFF264C3D)
val FloraForestDeep = Color(0xFF1D3C30)

val FloraSage = Color(0xFF58614A)
val FloraSageLight = Color(0xFFDCE2D2)
val FloraShellBg = Color(0xFFD2DAC8)

val FloraPaper = Color(0xFFF6EFE0)
val FloraPaperWarm = Color(0xFFEBE1CA)
val FloraPaperCard = Color(0xFFF9F4E8)
val FloraShadowPaper = Color(0xFFCFD6C3)

val FloraTerracotta = Color(0xFFB8442C)
val FloraTerracottaDark = Color(0xFF9B341F)
val FloraCobalt = Color(0xFF23479C)
val FloraCobaltDeep = Color(0xFF183270)

val FloraGold = Color(0xFFD5A342)
val FloraGoldSoft = Color(0xFFE6C678)
val FloraBorderInk = Color(0xFF274437)

// Validated WCAG AA Secondary Text Pairs on #F6EFE0 Paper
val FloraMetaGreen = Color(0xFF5A675C)
val FloraTrackMeta = Color(0xFF5B695E)
val FloraNavIdle = Color(0xFF5D6A5F)
val FloraQuoteInk = Color(0xFF3F5246)
val FloraHeroSub = Color(0xFF4F5F54)

// Dark Player Surfaces
val FloraPlayerCream = Color(0xFFFFFAF0)
val FloraLyricIdle = Color(0xFFC6D5CB)
val FloraDarkMeta = Color(0xFFC2D1C5)

@Immutable
data class FloraThemeColors(
  val isDark: Boolean,
  val ink: Color,
  val paper: Color,
  val paperWarm: Color,
  val paperCard: Color,
  val shadowPaper: Color,
  val borderInk: Color,
  val metaGreen: Color,
  val trackMeta: Color,
  val navIdle: Color,
  val quoteInk: Color,
  val heroSub: Color,
  val sage: Color,
  val sageLight: Color,
  val terracotta: Color,
  val cobalt: Color,
  val forest: Color,
  val gold: Color,
  val goldSoft: Color,
  val currentHighlight: Color,
  val surfaceBgTop: Color,
  val surfaceBgBottom: Color,
)

val LightFloraColors = FloraThemeColors(
  isDark = false,
  ink = FloraInk,
  paper = FloraPaper,
  paperWarm = FloraPaperWarm,
  paperCard = FloraPaperCard,
  shadowPaper = FloraShadowPaper,
  borderInk = FloraBorderInk,
  metaGreen = FloraMetaGreen,
  trackMeta = FloraTrackMeta,
  navIdle = FloraNavIdle,
  quoteInk = FloraQuoteInk,
  heroSub = FloraHeroSub,
  sage = FloraSage,
  sageLight = FloraSageLight,
  terracotta = FloraTerracotta,
  cobalt = FloraCobalt,
  forest = FloraForest,
  gold = FloraGold,
  goldSoft = FloraGoldSoft,
  currentHighlight = Color(0xFFEBD9B3),
  surfaceBgTop = FloraPaper,
  surfaceBgBottom = FloraPaperWarm,
)

val DarkFloraColors = FloraThemeColors(
  isDark = true,
  ink = Color(0xFFF3EBDA),
  paper = Color(0xFF131F19),
  paperWarm = Color(0xFF1D2E25),
  paperCard = Color(0xFF1A2921),
  shadowPaper = Color(0xFF0A120E),
  borderInk = Color(0xFF506B5C),
  metaGreen = Color(0xFFA9BBA9),
  trackMeta = Color(0xFFB3C4B4),
  navIdle = Color(0xFF9FB0A1),
  quoteInk = Color(0xFFD5E1D4),
  heroSub = Color(0xFFC4D1C4),
  sage = Color(0xFF8FA68B),
  sageLight = Color(0xFF284435),
  terracotta = Color(0xFFE06D53),
  cobalt = Color(0xFF5D84DF),
  forest = Color(0xFF234738),
  gold = Color(0xFFE2B455),
  goldSoft = Color(0xFFEBD18C),
  currentHighlight = Color(0xFF2D3C2C),
  surfaceBgTop = Color(0xFF121E18),
  surfaceBgBottom = Color(0xFF0C1611),
)

