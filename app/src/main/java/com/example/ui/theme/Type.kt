package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val CormorantDisplayFont = FontFamily(
  Font(R.font.cormorant_garamond, FontWeight.Normal),
  Font(R.font.cormorant_garamond, FontWeight.SemiBold),
  Font(R.font.cormorant_garamond, FontWeight.Bold),
  Font(R.font.cormorant_garamond, FontWeight.Bold, FontStyle.Italic),
)

val FrauncesSerifFont = FontFamily(
  Font(R.font.fraunces, FontWeight.Normal),
  Font(R.font.fraunces, FontWeight.Medium),
  Font(R.font.fraunces, FontWeight.SemiBold),
  Font(R.font.fraunces, FontWeight.Normal, FontStyle.Italic),
)

val DmSansUiFont = FontFamily(
  Font(R.font.dm_sans, FontWeight.Normal),
  Font(R.font.dm_sans, FontWeight.Medium),
  Font(R.font.dm_sans, FontWeight.SemiBold),
  Font(R.font.dm_sans, FontWeight.Bold),
)

val SpecimenMonoFont = FontFamily(
  Font(R.font.jetbrains_mono, FontWeight.Normal),
  Font(R.font.jetbrains_mono, FontWeight.Medium),
)

val BotanicalTypography = Typography(
  // Screen titles ("Floración", "Explorar", "Colección", "Ecualizador")
  displayLarge = TextStyle(
    fontFamily = CormorantDisplayFont,
    fontWeight = FontWeight.Bold,
    fontStyle = FontStyle.Italic,
    fontSize = 32.sp,
    lineHeight = 36.sp,
    letterSpacing = (-0.3).sp,
  ),
  // Hero headline & Full Player Track Title
  headlineLarge = TextStyle(
    fontFamily = CormorantDisplayFont,
    fontWeight = FontWeight.Bold,
    fontStyle = FontStyle.Italic,
    fontSize = 27.sp,
    lineHeight = 31.sp,
  ),
  // Section headings ("Ecosistemas", modals)
  headlineMedium = TextStyle(
    fontFamily = CormorantDisplayFont,
    fontWeight = FontWeight.Bold,
    fontStyle = FontStyle.Italic,
    fontSize = 24.sp,
    lineHeight = 28.sp,
  ),
  // Lyrics & feature card titles
  titleLarge = TextStyle(
    fontFamily = FrauncesSerifFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 19.sp,
    lineHeight = 25.sp,
  ),
  // Track titles in lists & cards
  titleMedium = TextStyle(
    fontFamily = FrauncesSerifFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    lineHeight = 20.sp,
  ),
  // Collection names, EQ preset buttons
  titleSmall = TextStyle(
    fontFamily = FrauncesSerifFont,
    fontWeight = FontWeight.Medium,
    fontSize = 13.5.sp,
    lineHeight = 18.sp,
  ),
  // Editorial quotes & empty state italic serif
  bodyLarge = TextStyle(
    fontFamily = FrauncesSerifFont,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp,
  ),
  // Standard DM Sans UI body
  bodyMedium = TextStyle(
    fontFamily = DmSansUiFont,
    fontWeight = FontWeight.Normal,
    fontSize = 13.5.sp,
    lineHeight = 19.sp,
  ),
  // Meta text, artist sublines, counts
  bodySmall = TextStyle(
    fontFamily = DmSansUiFont,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp,
  ),
  // Buttons, chips, primary action labels
  labelLarge = TextStyle(
    fontFamily = DmSansUiFont,
    fontWeight = FontWeight.Bold,
    fontSize = 12.5.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.6.sp,
  ),
  // Kickers & uppercase eyebrows ("FLORA · HERBARIO MUSICAL", "LÁMINA DEL DÍA")
  labelMedium = TextStyle(
    fontFamily = DmSansUiFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 10.5.sp,
    lineHeight = 14.sp,
    letterSpacing = 1.8.sp,
  ),
  // Compact tabular / EQ scale / technical metrics
  labelSmall = TextStyle(
    fontFamily = SpecimenMonoFont,
    fontWeight = FontWeight.Medium,
    fontSize = 10.5.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.3.sp,
  ),
)
