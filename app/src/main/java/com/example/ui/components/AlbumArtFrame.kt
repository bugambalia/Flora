package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AudioTrack
import com.example.data.model.BotanicalCatalog
import com.example.ui.theme.FloraBorderInk
import com.example.ui.theme.FloraPaperWarm
import com.example.ui.theme.FloraThumbShape
import com.example.ui.theme.LocalAlbumPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal object CoverArtCache {
  private val maxCacheKb: Int = (Runtime.getRuntime().maxMemory() / 1024L / 8L)
    .toInt()
    .coerceIn(4 * 1024, 24 * 1024)

  private val lru = object : LruCache<String, ImageBitmap>(maxCacheKb) {
    override fun sizeOf(key: String, value: ImageBitmap): Int =
      ((value.width.toLong() * value.height.toLong() * 4L) / 1024L).toInt().coerceAtLeast(1)
  }

  fun cacheKey(coverPath: String, targetSizePx: Int): String = "$coverPath@$targetSizePx"

  fun get(key: String): ImageBitmap? = lru.get(key)

  fun put(key: String, bitmap: ImageBitmap) {
    lru.put(key, bitmap)
  }

  fun clear() {
    lru.evictAll()
  }
}

internal fun calculateInSampleSize(
  rawWidth: Int,
  rawHeight: Int,
  reqWidth: Int,
  reqHeight: Int,
): Int {
  if (rawWidth <= 0 || rawHeight <= 0 || reqWidth <= 0 || reqHeight <= 0) return 1
  var inSampleSize = 1
  if (rawHeight > reqHeight || rawWidth > reqWidth) {
    val halfHeight = rawHeight / 2
    val halfWidth = rawWidth / 2
    while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
      inSampleSize *= 2
    }
  }
  return inSampleSize.coerceAtLeast(1)
}

internal fun decodeSampledImageBitmap(coverPath: String, targetSizePx: Int): ImageBitmap? {
  return runCatching {
    val file = File(coverPath)
    if (!file.exists()) return null

    val boundsOpts = BitmapFactory.Options().apply {
      inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(coverPath, boundsOpts)
    val sampleSize = calculateInSampleSize(
      rawWidth = boundsOpts.outWidth,
      rawHeight = boundsOpts.outHeight,
      reqWidth = targetSizePx,
      reqHeight = targetSizePx,
    )
    val decodeOpts = BitmapFactory.Options().apply {
      inSampleSize = sampleSize
    }
    BitmapFactory.decodeFile(coverPath, decodeOpts)?.asImageBitmap()
  }.getOrNull()
}

@Composable
fun AlbumArtFrame(
  track: AudioTrack?,
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 10.dp,
  customShape: Shape? = null,
  borderColor: Color = FloraBorderInk.copy(alpha = 0.72f),
  preferLiveBotanicalCanvas: Boolean = false,
) {
  val palette = LocalAlbumPalette.current
  val coverPath = track?.coverPath
  val isImportedCover = track != null && !track.isBundled && coverPath != null

  val shape = customShape ?: RoundedCornerShape(
    topStart = 2.dp,
    topEnd = cornerRadius,
    bottomEnd = 2.dp,
    bottomStart = 2.dp
  )

  BoxWithConstraints(
    modifier = modifier
      .clip(shape)
      .background(FloraPaperWarm)
      .border(1.dp, borderColor, shape)
  ) {
    val density = LocalDensity.current
    val targetSizePx = remember(constraints.maxWidth, constraints.maxHeight, density) {
      val maxDim = when {
        constraints.hasBoundedWidth && constraints.hasBoundedHeight -> maxOf(constraints.maxWidth, constraints.maxHeight)
        constraints.hasBoundedWidth -> constraints.maxWidth
        constraints.hasBoundedHeight -> constraints.maxHeight
        else -> with(density) { 128.dp.roundToPx() }
      }.coerceIn(64, 720)
      ((maxDim + 63) / 64) * 64
    }

    val cacheKey = if (!preferLiveBotanicalCanvas && isImportedCover && coverPath != null) {
      CoverArtCache.cacheKey(coverPath, targetSizePx)
    } else {
      null
    }
    val cachedBitmap = remember(cacheKey) {
      cacheKey?.let { CoverArtCache.get(it) }
    }

    val bitmap by produceState<ImageBitmap?>(
      initialValue = cachedBitmap,
      cacheKey,
      targetSizePx
    ) {
      if (cachedBitmap != null) {
        value = cachedBitmap
      } else if (cacheKey != null && coverPath != null) {
        value = withContext(Dispatchers.IO) {
          CoverArtCache.get(cacheKey) ?: decodeSampledImageBitmap(coverPath, targetSizePx)?.also { decoded ->
            CoverArtCache.put(cacheKey, decoded)
          }
        }
      } else {
        value = null
      }
    }

    when {
      preferLiveBotanicalCanvas && track != null -> {
        BotanicalSpecimenIllustration(
          speciesId = track.speciesId,
          accentOverride = palette.vibrant,
          modifier = Modifier
            .fillMaxSize()
            .padding(6.dp)
        )
      }
      bitmap != null -> {
        Image(
          bitmap = bitmap!!,
          contentDescription = "Carátula de ${track?.title ?: "álbum"}",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      }
      track != null -> {
        val species = BotanicalCatalog.getById(track.speciesId)
        Image(
          painter = painterResource(id = species.coverResId),
          contentDescription = "Lámina botánica de ${track.title}",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}

@Composable
fun FloraDrawablePlateImage(
  drawableResId: Int,
  contentDescription: String,
  modifier: Modifier = Modifier,
  shape: Shape = FloraThumbShape,
  borderColor: Color = FloraBorderInk,
  contentScale: ContentScale = ContentScale.Crop,
) {
  Image(
    painter = painterResource(id = drawableResId),
    contentDescription = contentDescription,
    contentScale = contentScale,
    modifier = modifier
      .clip(shape)
      .border(1.2.dp, borderColor, shape)
  )
}
