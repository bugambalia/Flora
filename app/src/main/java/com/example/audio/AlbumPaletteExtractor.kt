package com.example.audio

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.palette.graphics.Palette
import com.example.data.model.AlbumPaletteColors
import com.example.data.model.AudioTrack
import com.example.data.model.BotanicalCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object AlbumPaletteExtractor {
  private val cache = LruCache<String, AlbumPaletteColors>(32)

  suspend fun extractForTrack(track: AudioTrack): AlbumPaletteColors = withContext(Dispatchers.Default) {
    val cacheKey = "${track.id}_${track.coverPath ?: track.speciesId.id}"
    cache.get(cacheKey)?.let { return@withContext it }

    val species = BotanicalCatalog.getById(track.speciesId)
    val fallback = AlbumPaletteColors(
      dominant = species.primaryColor,
      vibrant = species.primaryColor,
      darkVibrant = species.secondaryColor,
      lightVibrant = species.primaryColor.copy(alpha = 0.35f),
      muted = species.foliageColor,
      darkMuted = species.secondaryColor,
      onAccent = if (species.primaryColor.luminance() > 0.45f) Color(0xFF1F1B16) else Color(0xFFFFFBF4),
    )

    val bitmap = track.coverPath?.let { decodeSampledBitmap(it, 180, 180) }
    if (bitmap == null) {
      cache.put(cacheKey, fallback)
      return@withContext fallback
    }

    val palette = Palette.from(bitmap)
      .maximumColorCount(24)
      .generate()
    bitmap.recycle()

    val dominant = palette.dominantSwatch?.rgb?.let { Color(it) } ?: fallback.dominant
    val vibrant = palette.vibrantSwatch?.rgb?.let { Color(it) }
      ?: palette.mutedSwatch?.rgb?.let { Color(it) }
      ?: fallback.vibrant
    val darkVibrant = palette.darkVibrantSwatch?.rgb?.let { Color(it) }
      ?: palette.darkMutedSwatch?.rgb?.let { Color(it) }
      ?: fallback.darkVibrant
    val lightVibrant = palette.lightVibrantSwatch?.rgb?.let { Color(it) }
      ?: palette.lightMutedSwatch?.rgb?.let { Color(it) }
      ?: fallback.lightVibrant
    val muted = palette.mutedSwatch?.rgb?.let { Color(it) } ?: fallback.muted
    val darkMuted = palette.darkMutedSwatch?.rgb?.let { Color(it) } ?: fallback.darkMuted

    val primaryAccent = if (vibrant.luminance() in 0.08f..0.78f) vibrant else dominant
    val onAccent = if (primaryAccent.luminance() > 0.45f) Color(0xFF1E1A15) else Color(0xFFFDF8EE)

    val result = AlbumPaletteColors(
      dominant = dominant,
      vibrant = primaryAccent,
      darkVibrant = darkVibrant,
      lightVibrant = lightVibrant,
      muted = muted,
      darkMuted = darkMuted,
      onAccent = onAccent,
    )
    cache.put(cacheKey, result)
    result
  }

  private fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
    val file = File(path)
    if (!file.exists()) return null
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, options)
    var inSampleSize = 1
    if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
      val halfH = options.outHeight / 2
      val halfW = options.outWidth / 2
      while (halfH / inSampleSize >= reqHeight && halfW / inSampleSize >= reqWidth) {
        inSampleSize *= 2
      }
    }
    return BitmapFactory.decodeFile(
      path,
      BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
    )
  }
}
