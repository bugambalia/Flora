package com.example.audio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.example.data.model.AlbumPaletteColors
import com.example.data.model.AudioTrack
import com.example.data.model.BotanicalCatalog
import com.example.data.model.BotanicalSpeciesId
import com.example.data.model.ThemeBackgroundGradients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Service using androidx.palette.graphics.Palette to extract vibrant and harmonious
 * swatches from album artwork (cached cover files, content URIs, embedded audio art,
 * or botanical plate drawables) and publish reactive UI theme background gradients.
 */
class AlbumPaletteService(context: Context) {
  private val appContext = context.applicationContext
  private val cache = LruCache<String, AlbumPaletteColors>(48)

  private val _currentPalette = MutableStateFlow(AlbumPaletteColors())
  val currentPalette: StateFlow<AlbumPaletteColors> = _currentPalette.asStateFlow()

  private val _backgroundGradients = MutableStateFlow(ThemeBackgroundGradients.Default)
  val backgroundGradients: StateFlow<ThemeBackgroundGradients> = _backgroundGradients.asStateFlow()

  @Volatile
  private var dynamicGradientsEnabled: Boolean = true

  suspend fun updateForTrack(
    track: AudioTrack,
    dynamicThemeEnabled: Boolean = dynamicGradientsEnabled,
  ): AlbumPaletteColors {
    dynamicGradientsEnabled = dynamicThemeEnabled
    val extracted = extractForTrack(track)
    _currentPalette.value = extracted
    _backgroundGradients.value = buildThemeGradients(extracted, dynamicGradientsEnabled)
    return extracted
  }

  suspend fun updateForSpecies(
    speciesId: BotanicalSpeciesId,
    dynamicThemeEnabled: Boolean = dynamicGradientsEnabled,
  ): AlbumPaletteColors {
    dynamicGradientsEnabled = dynamicThemeEnabled
    val species = BotanicalCatalog.getById(speciesId)
    val fallback = speciesFallbackPalette(speciesId)
    val extracted = extractFromDrawable(
      resId = species.coverResId,
      cacheKey = "species_${speciesId.id}",
      fallback = fallback
    )
    _currentPalette.value = extracted
    _backgroundGradients.value = buildThemeGradients(extracted, dynamicGradientsEnabled)
    return extracted
  }

  fun setDynamicGradientsEnabled(enabled: Boolean) {
    if (dynamicGradientsEnabled == enabled) return
    dynamicGradientsEnabled = enabled
    _backgroundGradients.value = buildThemeGradients(_currentPalette.value, enabled)
  }

  suspend fun extractForTrack(track: AudioTrack): AlbumPaletteColors =
    withContext(Dispatchers.IO) {
      val cacheKey = "${track.id}_${track.coverPath ?: track.uri}_${track.speciesId.id}"
      synchronized(cache) { cache.get(cacheKey) }?.let { return@withContext it }

      val fallback = speciesFallbackPalette(track.speciesId)
      val species = BotanicalCatalog.getById(track.speciesId)

      val bitmap = decodeTrackArtworkBitmap(track, species.coverResId)
      if (bitmap == null) {
        synchronized(cache) { cache.put(cacheKey, fallback) }
        return@withContext fallback
      }

      try {
        extractFromBitmap(bitmap = bitmap, cacheKey = cacheKey, fallback = fallback)
      } finally {
        if (!bitmap.isRecycled) {
          bitmap.recycle()
        }
      }
    }

  suspend fun extractFromDrawable(
    @DrawableRes resId: Int,
    cacheKey: String = "drawable_$resId",
    fallback: AlbumPaletteColors = AlbumPaletteColors(),
  ): AlbumPaletteColors = withContext(Dispatchers.IO) {
    synchronized(cache) { cache.get(cacheKey) }?.let { return@withContext it }
    val bitmap = decodeSampledResource(resId, 192, 192) ?: return@withContext fallback
    try {
      extractFromBitmap(bitmap = bitmap, cacheKey = cacheKey, fallback = fallback)
    } finally {
      if (!bitmap.isRecycled) {
        bitmap.recycle()
      }
    }
  }

  suspend fun extractFromBitmap(
    bitmap: Bitmap,
    cacheKey: String? = null,
    fallback: AlbumPaletteColors = AlbumPaletteColors(),
  ): AlbumPaletteColors = withContext(Dispatchers.Default) {
    if (cacheKey != null) {
      synchronized(cache) { cache.get(cacheKey) }?.let { return@withContext it }
    }

    val palette = Palette.from(bitmap)
      .maximumColorCount(24)
      .generate()

    val result = paletteToColors(palette, fallback)
    if (cacheKey != null) {
      synchronized(cache) { cache.put(cacheKey, result) }
    }
    result
  }

  private fun paletteToColors(
    palette: Palette,
    fallback: AlbumPaletteColors,
  ): AlbumPaletteColors {
    val dominant = palette.dominantSwatch?.rgb?.let(::Color) ?: fallback.dominant

    val chromaticFallbackSwatch = palette.swatches
      .filter { it.hsl[1] >= 0.22f && it.hsl[2] in 0.16f..0.82f }
      .maxByOrNull { it.population * (0.5f + it.hsl[1]) }

    val vibrant = palette.vibrantSwatch?.rgb?.let(::Color)
      ?: chromaticFallbackSwatch?.rgb?.let(::Color)
      ?: palette.lightVibrantSwatch?.rgb?.let(::Color)
      ?: palette.darkVibrantSwatch?.rgb?.let(::Color)
      ?: palette.mutedSwatch?.rgb?.let(::Color)
      ?: fallback.vibrant

    val darkVibrant = palette.darkVibrantSwatch?.rgb?.let(::Color)
      ?: palette.darkMutedSwatch?.rgb?.let(::Color)
      ?: darkenColor(vibrant, 0.58f)

    val lightVibrant = palette.lightVibrantSwatch?.rgb?.let(::Color)
      ?: palette.lightMutedSwatch?.rgb?.let(::Color)
      ?: lightenColor(vibrant, 0.45f)

    val muted = palette.mutedSwatch?.rgb?.let(::Color)
      ?: palette.dominantSwatch?.rgb?.let(::Color)
      ?: fallback.muted

    val darkMuted = palette.darkMutedSwatch?.rgb?.let(::Color)
      ?: darkenColor(dominant, 0.65f)

    val primaryAccent = if (vibrant.luminance() in 0.06f..0.80f) vibrant else dominant
    val onAccent = if (primaryAccent.luminance() > 0.45f) {
      Color(0xFF1E1A15)
    } else {
      Color(0xFFFDF8EE)
    }

    return AlbumPaletteColors(
      dominant = dominant,
      vibrant = primaryAccent,
      darkVibrant = darkVibrant,
      lightVibrant = lightVibrant,
      muted = muted,
      darkMuted = darkMuted,
      onAccent = onAccent,
    )
  }

  private fun decodeTrackArtworkBitmap(track: AudioTrack, @DrawableRes fallbackCoverResId: Int): Bitmap? {
    val coverPath = track.coverPath
    if (!coverPath.isNullOrBlank()) {
      if (coverPath.startsWith("content://") || coverPath.startsWith("file://")) {
        decodeSampledUri(Uri.parse(coverPath), 192, 192)?.let { return it }
      } else {
        decodeSampledFile(coverPath, 192, 192)?.let { return it }
      }
    }

    if (!track.isBundled && track.uri.isNotBlank()) {
      extractEmbeddedCoverBitmap(track.uri)?.let { return it }
    }

    return decodeSampledResource(fallbackCoverResId, 192, 192)
  }

  private fun extractEmbeddedCoverBitmap(uriString: String): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
      val uri = Uri.parse(uriString)
      if (uri.scheme == "file" && uri.path != null) {
        retriever.setDataSource(uri.path)
      } else {
        retriever.setDataSource(appContext, uri)
      }
      val bytes = retriever.embeddedPicture ?: return null
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
      val sample = calculateInSampleSize(bounds, 192, 192)
      BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sample }
      )
    } catch (_: Exception) {
      null
    } finally {
      runCatching { retriever.release() }
    }
  }

  private fun decodeSampledFile(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
    val file = File(path)
    if (!file.exists()) return null
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) return null
    return BitmapFactory.decodeFile(
      path,
      BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
      }
    )
  }

  private fun decodeSampledUri(uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    appContext.contentResolver.openInputStream(uri)?.use { stream ->
      BitmapFactory.decodeStream(stream, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
    val sample = calculateInSampleSize(bounds, reqWidth, reqHeight)
    appContext.contentResolver.openInputStream(uri)?.use { stream ->
      BitmapFactory.decodeStream(
        stream,
        null,
        BitmapFactory.Options().apply { inSampleSize = sample }
      )
    }
  }.getOrNull()

  private fun decodeSampledResource(@DrawableRes resId: Int, reqWidth: Int, reqHeight: Int): Bitmap? = runCatching {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeResource(appContext.resources, resId, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) return@runCatching null
    BitmapFactory.decodeResource(
      appContext.resources,
      resId,
      BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
      }
    )
  }.getOrNull()

  private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int,
  ): Int {
    var inSampleSize = 1
    if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
      val halfH = options.outHeight / 2
      val halfW = options.outWidth / 2
      while (halfH / inSampleSize >= reqHeight && halfW / inSampleSize >= reqWidth) {
        inSampleSize *= 2
      }
    }
    return inSampleSize
  }

  companion object {
    private val PaperBaseTop = Color(0xFFF6EFE0)
    private val PaperBaseBottom = Color(0xFFEBE1CA)
    private val DeepNocturneShadow = Color(0xFF0E1B16)

    fun speciesFallbackPalette(speciesId: BotanicalSpeciesId): AlbumPaletteColors {
      val species = BotanicalCatalog.getById(speciesId)
      return AlbumPaletteColors(
        dominant = species.primaryColor,
        vibrant = species.primaryColor,
        darkVibrant = species.secondaryColor,
        lightVibrant = lerp(PaperBaseTop, species.primaryColor, 0.35f),
        muted = species.foliageColor,
        darkMuted = species.secondaryColor,
        onAccent = if (species.primaryColor.luminance() > 0.45f) Color(0xFF1F1B16) else Color(0xFFFFFBF4),
      )
    }

    fun buildThemeGradients(
      palette: AlbumPaletteColors,
      dynamicEnabled: Boolean,
    ): ThemeBackgroundGradients {
      if (!dynamicEnabled) return ThemeBackgroundGradients.Default

      val lightWash = lerp(PaperBaseTop, palette.lightVibrant, 0.24f)
      val warmBaseWash = lerp(PaperBaseBottom, palette.muted, 0.16f)

      val richTopBase = ensureDarkSurface(lerp(palette.darkVibrant, palette.vibrant, 0.25f), maxLuminance = 0.14f)
      val richMidBase = ensureDarkSurface(lerp(palette.darkMuted, palette.dominant, 0.32f), maxLuminance = 0.10f)
      val richBottomBase = ensureDarkSurface(lerp(palette.darkMuted, DeepNocturneShadow, 0.52f), maxLuminance = 0.06f)

      val miniStart = ensureDarkSurface(lerp(palette.darkMuted, palette.dominant, 0.35f), maxLuminance = 0.13f)
      val miniEnd = ensureDarkSurface(lerp(palette.darkVibrant, palette.vibrant, 0.30f), maxLuminance = 0.17f)

      return ThemeBackgroundGradients(
        parchmentTop = lightWash,
        parchmentBottom = warmBaseWash,
        parchmentRadialPrimary = palette.vibrant.copy(alpha = 0.15f),
        parchmentRadialSecondary = palette.lightVibrant.copy(alpha = 0.22f),
        heroGlowStart = palette.vibrant.copy(alpha = 0.20f),
        heroGlowEnd = palette.lightVibrant.copy(alpha = 0.16f),
        playerTop = richTopBase,
        playerMiddle = richMidBase,
        playerBottom = richBottomBase,
        playerGlow = palette.vibrant,
        miniPlayerStart = miniStart,
        miniPlayerEnd = miniEnd,
      )
    }

    private fun ensureDarkSurface(color: Color, maxLuminance: Float): Color {
      val opaque = color.copy(alpha = 1f)
      if (opaque.luminance() <= maxLuminance) return opaque
      val hsl = FloatArray(3)
      ColorUtils.colorToHSL(opaque.toArgb(), hsl)
      hsl[2] = hsl[2].coerceAtMost(0.24f)
      val toned = Color(ColorUtils.HSLToColor(hsl))
      return if (toned.luminance() <= maxLuminance) toned else lerp(toned, DeepNocturneShadow, 0.45f)
    }

    private fun darkenColor(color: Color, factor: Float): Color =
      lerp(color.copy(alpha = 1f), DeepNocturneShadow, factor.coerceIn(0f, 1f))

    private fun lightenColor(color: Color, factor: Float): Color =
      lerp(color.copy(alpha = 1f), PaperBaseTop, factor.coerceIn(0f, 1f))
  }
}
