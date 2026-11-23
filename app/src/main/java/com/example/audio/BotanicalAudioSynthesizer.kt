package com.example.audio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import com.example.data.local.TrackEntity
import com.example.data.model.BotanicalCatalog
import com.example.data.model.BotanicalSpecies
import com.example.data.model.BotanicalSpeciesId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

object BotanicalAudioSynthesizer {

  private data class SeedComposition(
    val fileName: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val speciesId: BotanicalSpeciesId,
    val freqs: List<Double>,
    val tempoBpm: Double,
    val timbre: String,
    val durationSec: Int = 10,
  )

  private val seeds = listOf(
    SeedComposition(
      fileName = "lilium_preludio.wav",
      title = "Preludio del Lirio Tigre",
      artist = "Cuarteto del Alba",
      album = "Láminas de Japón",
      genre = "Folk",
      speciesId = BotanicalSpeciesId.TIGER_LILY,
      freqs = listOf(293.66, 369.99, 440.00, 587.33, 440.00, 329.63),
      tempoBpm = 76.0,
      timbre = "pluck"
    ),
    SeedComposition(
      fileName = "monstera_dosel.wav",
      title = "Luz entre Fenestraciones",
      artist = "Taller Umbría",
      album = "Sotobosque Tropical",
      genre = "Ambient",
      speciesId = BotanicalSpeciesId.MONSTERA,
      freqs = listOf(174.61, 220.00, 261.63, 329.63, 392.00, 261.63),
      tempoBpm = 56.0,
      timbre = "pad"
    ),
    SeedComposition(
      fileName = "ginkgo_otono.wav",
      title = "Abanicos de Tianmu",
      artist = "Trío Salisburia",
      album = "Oro Dicotómico",
      genre = "Jazz",
      speciesId = BotanicalSpeciesId.GINKGO,
      freqs = listOf(220.00, 261.63, 329.63, 392.00, 493.88, 349.23),
      tempoBpm = 84.0,
      timbre = "rhodes"
    ),
    SeedComposition(
      fileName = "lavanda_432hz.wav",
      title = "Brisa de Provenza (432 Hz)",
      artist = "Jardín Nocturno",
      album = "Destilación Silvestre",
      genre = "Chill",
      speciesId = BotanicalSpeciesId.LAVENDER,
      freqs = listOf(216.0, 272.14, 324.0, 432.0, 324.0, 256.87),
      tempoBpm = 52.0,
      timbre = "pad"
    ),
    SeedComposition(
      fileName = "loto_alba.wav",
      title = "Gotas sobre el Loto",
      artist = "Ensamble Ribera",
      album = "Estanque Sereno",
      genre = "Bossa",
      speciesId = BotanicalSpeciesId.LOTUS,
      freqs = listOf(246.94, 311.13, 369.99, 440.00, 493.88, 369.99),
      tempoBpm = 92.0,
      timbre = "pluck"
    ),
    SeedComposition(
      fileName = "helecho_espiral.wav",
      title = "Espiral de Báculo",
      artist = "Bosque Atlántico",
      album = "Frondas Húmedas",
      genre = "Indie",
      speciesId = BotanicalSpeciesId.FERN,
      freqs = listOf(196.00, 246.94, 293.66, 392.00, 329.63, 246.94),
      tempoBpm = 96.0,
      timbre = "pluck"
    ),
    SeedComposition(
      fileName = "pasiflora_pulso.wav",
      title = "Corona de Filamentos",
      artist = "Andes Sonoro",
      album = "Trópico Vivo",
      genre = "Latina",
      speciesId = BotanicalSpeciesId.PASSIFLORA,
      freqs = listOf(261.63, 329.63, 392.00, 523.25, 440.00, 349.23),
      tempoBpm = 104.0,
      timbre = "marimba"
    ),
    SeedComposition(
      fileName = "camelia_nocturno.wav",
      title = "Nocturno en Pétalos Céreos",
      artist = "Clara Solís",
      album = "Flores de Invierno",
      genre = "Piano",
      speciesId = BotanicalSpeciesId.CAMELLIA,
      freqs = listOf(220.00, 261.63, 329.63, 440.00, 392.00, 293.66),
      tempoBpm = 66.0,
      timbre = "rhodes"
    )
  )

  suspend fun ensureSeedTracksGenerated(context: Context): List<TrackEntity> =
    withContext(Dispatchers.Default) {
      val audioDir = File(context.filesDir, "botanical_audio").apply { mkdirs() }
      val coverDir = File(context.filesDir, "botanical_covers").apply { mkdirs() }

      coroutineScope {
        seeds.map { seed ->
          async(Dispatchers.Default) {
            val audioFile = File(audioDir, seed.fileName)
            if (!audioFile.exists() || audioFile.length() < 4096L) {
              writeWavFile(audioFile, seed)
            }
            val coverFile = File(coverDir, "${seed.speciesId.id}_flora.jpg")
            if (!coverFile.exists() || coverFile.length() < 1024L) {
              val species = BotanicalCatalog.getById(seed.speciesId)
              saveDrawableAsCoverFile(context, coverFile, species)
            }

            TrackEntity(
              uri = audioFile.toURI().toString(),
              title = seed.title,
              artist = seed.artist,
              album = seed.album,
              genre = seed.genre,
              durationMs = seed.durationSec * 1000L,
              bitrateKbps = 705,
              sampleRateHz = 44100,
              codec = "PCM WAV",
              coverPath = coverFile.absolutePath,
              speciesId = seed.speciesId.id,
              isBundled = true,
              sourceFolder = "Herbario",
              sourcePath = "herbarium://bundled",
            )
          }
        }.awaitAll()
      }
    }

  private fun writeWavFile(file: File, seed: SeedComposition) {
    val sampleRate = 44100
    val totalSamples = sampleRate * seed.durationSec
    val dataBytes = totalSamples * 2
    val beatSec = 60.0 / seed.tempoBpm

    BufferedOutputStream(FileOutputStream(file), 32768).use { out ->
      // RIFF/WAV header (44 bytes)
      out.write("RIFF".toByteArray(Charsets.US_ASCII))
      writeIntLE(out, 36 + dataBytes)
      out.write("WAVE".toByteArray(Charsets.US_ASCII))
      out.write("fmt ".toByteArray(Charsets.US_ASCII))
      writeIntLE(out, 16)
      writeShortLE(out, 1) // PCM
      writeShortLE(out, 1) // Mono
      writeIntLE(out, sampleRate)
      writeIntLE(out, sampleRate * 2)
      writeShortLE(out, 2)
      writeShortLE(out, 16)
      out.write("data".toByteArray(Charsets.US_ASCII))
      writeIntLE(out, dataBytes)

      val chunk = ByteArray(16384)
      var chunkPos = 0

      for (i in 0 until totalSamples) {
        val t = i.toDouble() / sampleRate
        val noteFloat = t / beatSec
        val noteIdx = noteFloat.toInt()
        val noteTime = (noteFloat - noteIdx) * beatSec
        val f1 = seed.freqs[noteIdx % seed.freqs.size]
        val fRoot = seed.freqs[0] * 0.5

        val signal = when (seed.timbre) {
          "pad" -> {
            val env = sin(PI * (noteTime / beatSec)).coerceAtLeast(0.0)
            val pad = 0.45 * sin(2 * PI * f1 * t) +
              0.25 * sin(2 * PI * (f1 * 1.5) * t) +
              0.20 * sin(2 * PI * fRoot * t) +
              0.10 * sin(2 * PI * (f1 * 2.0) * t)
            pad * (0.55 + 0.45 * env)
          }
          "marimba" -> {
            val decay = exp(-noteTime * 7.5)
            val strike = 0.65 * sin(2 * PI * f1 * noteTime) +
              0.25 * sin(2 * PI * f1 * 3.98 * noteTime) * exp(-noteTime * 18.0) +
              0.20 * sin(2 * PI * fRoot * t)
            strike * (0.25 + 0.75 * decay)
          }
          "rhodes" -> {
            val decay = exp(-noteTime * 2.8)
            val bell = sin(2 * PI * f1 * 2.0 * noteTime) * exp(-noteTime * 11.0) * 0.25
            val body = 0.60 * sin(2 * PI * f1 * noteTime) +
              0.22 * sin(2 * PI * fRoot * t) + bell
            body * (0.2 + 0.8 * decay)
          }
          else -> { // pluck
            val decay = exp(-noteTime * 4.2)
            val harmonics = 0.58 * sin(2 * PI * f1 * noteTime) +
              0.22 * sin(2 * PI * f1 * 2.0 * noteTime) * exp(-noteTime * 7.0) +
              0.12 * sin(2 * PI * f1 * 3.0 * noteTime) * exp(-noteTime * 12.0) +
              0.20 * sin(2 * PI * fRoot * t)
            harmonics * (0.18 + 0.82 * decay)
          }
        }

        // Smooth fade-in and fade-out for clean gapless transitions
        val fadeIn = (t / 0.12).coerceIn(0.0, 1.0)
        val fadeOut = ((seed.durationSec - t) / 0.18).coerceIn(0.0, 1.0)
        val sampleVal = (signal * fadeIn * fadeOut * 11500.0).toInt().coerceIn(-32767, 32767)

        chunk[chunkPos++] = (sampleVal and 0xFF).toByte()
        chunk[chunkPos++] = ((sampleVal ushr 8) and 0xFF).toByte()
        if (chunkPos == chunk.size) {
          out.write(chunk, 0, chunkPos)
          chunkPos = 0
        }
      }
      if (chunkPos > 0) {
        out.write(chunk, 0, chunkPos)
      }
    }
  }

  private fun writeIntLE(out: BufferedOutputStream, value: Int) {
    out.write(value and 0xFF)
    out.write((value ushr 8) and 0xFF)
    out.write((value ushr 16) and 0xFF)
    out.write((value ushr 24) and 0xFF)
  }

  private fun writeShortLE(out: BufferedOutputStream, value: Int) {
    out.write(value and 0xFF)
    out.write((value ushr 8) and 0xFF)
  }

  private fun saveDrawableAsCoverFile(context: Context, file: File, species: BotanicalSpecies) {
    val decoded = BitmapFactory.decodeResource(context.resources, species.coverResId)
    if (decoded != null) {
      val scaled = Bitmap.createScaledBitmap(decoded, 420, 420, true)
      FileOutputStream(file).use { out ->
        scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
      }
      if (scaled !== decoded) scaled.recycle()
      decoded.recycle()
      return
    }
    generateBotanicalCoverBitmap(file, species, species.commonName)
  }

  private fun generateBotanicalCoverBitmap(file: File, species: BotanicalSpecies, title: String) {
    val size = 420
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val primaryInt = species.primaryColor.toArgbInt()
    val secondaryInt = species.secondaryColor.toArgbInt()
    val foliageInt = species.foliageColor.toArgbInt()

    // Vintage parchment gradient tinted subtly with species botanical pigment
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = LinearGradient(
        0f, 0f, size.toFloat(), size.toFloat(),
        intArrayOf(0xFFF3EBDD.toInt(), blendColors(0xFFEDE1CE.toInt(), primaryInt, 0.20f)),
        null,
        Shader.TileMode.CLAMP
      )
    }
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

    // Soft botanical pigment wash behind specimen
    val washPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = RadialGradient(
        size * 0.52f, size * 0.48f, size * 0.44f,
        intArrayOf(withAlpha(primaryInt, 90), withAlpha(foliageInt, 45), 0x00000000),
        floatArrayOf(0f, 0.55f, 1f),
        Shader.TileMode.CLAMP
      )
    }
    canvas.drawCircle(size * 0.52f, size * 0.48f, size * 0.44f, washPaint)

    // Double botanical plate border
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 2.5f
      color = 0xFF3A3127.toInt()
    }
    canvas.drawRect(16f, 16f, size - 16f, size - 16f, borderPaint)
    borderPaint.strokeWidth = 1f
    borderPaint.alpha = 140
    canvas.drawRect(24f, 24f, size - 24f, size - 24f, borderPaint)

    // Stylized botanical centerpiece on bitmap for authentic Palette extraction
    val petalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = primaryInt
      style = Paint.Style.FILL
    }
    val petalAccent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = secondaryInt
      style = Paint.Style.FILL
    }
    val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = foliageInt
      style = Paint.Style.FILL
    }
    val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = 0xFF28211A.toInt()
      style = Paint.Style.STROKE
      strokeWidth = 3f
      strokeCap = Paint.Cap.ROUND
    }

    val cx = size * 0.5f
    val cy = size * 0.5f

    // Stem
    val stem = Path().apply {
      moveTo(cx, size * 0.82f)
      quadTo(cx - 18f, cy + 40f, cx, cy - 10f)
    }
    canvas.drawPath(stem, inkPaint.apply { strokeWidth = 5f })

    // Foliage leaves
    for (dir in listOf(-1f, 1f)) {
      val leaf = Path().apply {
        moveTo(cx - 4f, cy + 68f)
        quadTo(cx + dir * 90f, cy + 12f, cx + dir * 105f, cy + 72f)
        quadTo(cx + dir * 45f, cy + 94f, cx - 4f, cy + 68f)
      }
      canvas.drawPath(leaf, leafPaint)
      canvas.drawPath(leaf, inkPaint.apply { strokeWidth = 2.5f })
    }

    // Radial botanical petals
    val petalCount = if (species.id == BotanicalSpeciesId.GINKGO) 3 else 6
    for (i in 0 until petalCount) {
      val angleDeg = (360f / petalCount) * i - 90f
      canvas.save()
      canvas.rotate(angleDeg, cx, cy - 26f)
      val petal = Path().apply {
        moveTo(cx, cy - 26f)
        cubicTo(cx - 32f, cy - 82f, cx - 22f, cy - 132f, cx, cy - 142f)
        cubicTo(cx + 22f, cy - 132f, cx + 32f, cy - 82f, cx, cy - 26f)
      }
      canvas.drawPath(petal, if (i % 2 == 0) petalPaint else petalAccent)
      canvas.drawPath(petal, inkPaint.apply { strokeWidth = 2.5f })
      canvas.restore()
    }

    // Center stamen crown
    canvas.drawCircle(cx, cy - 26f, 14f, petalAccent)
    canvas.drawCircle(cx, cy - 26f, 14f, inkPaint)

    // Plate title caption at bottom
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = 0xFF28211A.toInt()
      textSize = 18f
      typeface = Typeface.SERIF
      textAlign = Paint.Align.CENTER
    }
    canvas.drawText(species.scientificName, cx, size - 36f, textPaint)

    FileOutputStream(file).use { out ->
      bmp.compress(Bitmap.CompressFormat.PNG, 92, out)
    }
    bmp.recycle()
  }

  private fun androidx.compose.ui.graphics.Color.toArgbInt(): Int {
    val a = (alpha * 255).toInt() and 0xFF
    val r = (red * 255).toInt() and 0xFF
    val g = (green * 255).toInt() and 0xFF
    val b = (blue * 255).toInt() and 0xFF
    return (a shl 24) or (r shl 16) or (g shl 8) or b
  }

  private fun withAlpha(color: Int, alpha: Int): Int =
    (alpha shl 24) or (color and 0x00FFFFFF)

  private fun blendColors(c1: Int, c2: Int, ratio: Float): Int {
    val inv = 1f - ratio
    val r = (((c1 shr 16) and 0xFF) * inv + ((c2 shr 16) and 0xFF) * ratio).toInt()
    val g = (((c1 shr 8) and 0xFF) * inv + ((c2 shr 8) and 0xFF) * ratio).toInt()
    val b = ((c1 and 0xFF) * inv + (c2 and 0xFF) * ratio).toInt()
    return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
  }
}
