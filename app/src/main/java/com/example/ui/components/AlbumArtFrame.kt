package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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

  val bitmap by produceState<ImageBitmap?>(initialValue = null, coverPath, preferLiveBotanicalCanvas, isImportedCover) {
    value = if (!preferLiveBotanicalCanvas && isImportedCover) {
      withContext(Dispatchers.IO) {
        runCatching {
          val file = File(coverPath)
          if (file.exists()) BitmapFactory.decodeFile(coverPath)?.asImageBitmap() else null
        }.getOrNull()
      }
    } else null
  }

  val shape = customShape ?: RoundedCornerShape(
    topStart = 2.dp,
    topEnd = cornerRadius,
    bottomEnd = 2.dp,
    bottomStart = 2.dp
  )
  Box(
    modifier = modifier
      .clip(shape)
      .background(FloraPaperWarm)
      .border(1.dp, borderColor, shape)
  ) {
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
