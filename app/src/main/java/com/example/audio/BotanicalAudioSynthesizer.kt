package com.example.audio

import android.content.Context
import com.example.data.local.TrackEntity
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

      coroutineScope {
        seeds.map { seed ->
          async(Dispatchers.Default) {
            val audioFile = File(audioDir, seed.fileName)
            if (!audioFile.exists() || audioFile.length() < 4096L) {
              writeWavFile(audioFile, seed)
            }

            // Bundled tracks render artwork directly from drawable resources (no redundant disk JPEGs)
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
              coverPath = null,
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
}
