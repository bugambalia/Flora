package com.example.audio

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.TrackEntity
import com.example.data.model.BotanicalCatalog
import com.example.ui.components.calculateInSampleSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.roundToInt

object LocalAudioImporter {

  private const val MAX_COVER_DIMENSION_PX = 640
  private const val COVER_JPEG_QUALITY = 85

  internal fun coverFileNameForUri(uriString: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
      .digest(uriString.toByteArray(Charsets.UTF_8))
    val hex = buildString(32) {
      for (i in 0 until 16) {
        val b = digest[i].toInt() and 0xFF
        append("0123456789abcdef"[b ushr 4])
        append("0123456789abcdef"[b and 0x0F])
      }
    }
    return "cover_$hex.jpg"
  }

  suspend fun importScannedFolder(
    context: Context,
    folder: DetectedFolderSource,
    existingUris: Set<String> = emptySet(),
  ): List<TrackEntity> = withContext(Dispatchers.IO) {
    val coverDir = File(context.filesDir, "imported_covers").apply { mkdirs() }
    val pending = folder.qualifyingTracks.filter { it.uri !in existingUris }
    if (pending.isEmpty()) return@withContext emptyList()

    val semaphore = Semaphore(4)
    coroutineScope {
      pending.map { candidate ->
        async(Dispatchers.IO) {
          semaphore.withPermit {
            extractTrackEntity(
              context = context,
              uri = Uri.parse(candidate.uri),
              fallbackDisplayName = candidate.displayName,
              fallbackDurationMs = candidate.durationMs,
              fallbackTitle = candidate.title,
              fallbackArtist = candidate.artist,
              fallbackAlbum = candidate.album,
              fallbackGenre = candidate.genre,
              fallbackBitrateKbps = candidate.bitrateKbps,
              fallbackSampleRateHz = candidate.sampleRateHz,
              fallbackCodec = candidate.codec,
              fallbackAlbumId = candidate.albumId,
              skipHeavyExtraction = candidate.hasCompleteMediaStoreMetadata,
              sourceFolder = folder.folderName,
              sourcePath = folder.folderKey,
              coverDir = coverDir,
            )
          }
        }
      }.awaitAll()
    }
  }

  suspend fun importUris(
    context: Context,
    uris: List<Uri>,
    existingUris: Set<String> = emptySet(),
    sourceFolder: String = "Importación Manual",
    sourcePath: String = "manual://picker",
  ): List<TrackEntity> = withContext(Dispatchers.IO) {
    val coverDir = File(context.filesDir, "imported_covers").apply { mkdirs() }
    val pendingUris = uris.filter { it.toString() !in existingUris }
    if (pendingUris.isEmpty()) return@withContext emptyList()

    val semaphore = Semaphore(4)
    coroutineScope {
      pendingUris.map { uri ->
        async(Dispatchers.IO) {
          semaphore.withPermit {
            runCatching {
              context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
              )
            }
            val displayName = queryDisplayName(context, uri)
              ?.substringBeforeLast('.')
              ?: "Pista Local"

            extractTrackEntity(
              context = context,
              uri = uri,
              fallbackDisplayName = displayName,
              fallbackDurationMs = 0L,
              fallbackTitle = null,
              fallbackArtist = null,
              fallbackAlbum = null,
              fallbackGenre = null,
              fallbackBitrateKbps = 192,
              fallbackSampleRateHz = 44100,
              fallbackCodec = guessCodec(context, uri),
              fallbackAlbumId = null,
              skipHeavyExtraction = false,
              sourceFolder = sourceFolder,
              sourcePath = sourcePath,
              coverDir = coverDir,
            )
          }
        }
      }.awaitAll()
    }
  }

  private fun extractTrackEntity(
    context: Context,
    uri: Uri,
    fallbackDisplayName: String,
    fallbackDurationMs: Long,
    fallbackTitle: String?,
    fallbackArtist: String?,
    fallbackAlbum: String?,
    fallbackGenre: String?,
    fallbackBitrateKbps: Int,
    fallbackSampleRateHz: Int,
    fallbackCodec: String,
    fallbackAlbumId: Long?,
    skipHeavyExtraction: Boolean,
    sourceFolder: String,
    sourcePath: String,
    coverDir: File,
  ): TrackEntity {
    var title = fallbackTitle ?: fallbackDisplayName
    var artist = fallbackArtist ?: "Artista Local"
    var album = fallbackAlbum ?: sourceFolder
    var genre = fallbackGenre ?: "Acústico"
    var durationMs = fallbackDurationMs
    var bitrateKbps = fallbackBitrateKbps
    var sampleRateHz = fallbackSampleRateHz
    var codec = fallbackCodec
    var coverPath: String? = null

    val coverFile = File(coverDir, coverFileNameForUri(uri.toString()))
    if (coverFile.exists() && coverFile.length() > 512L) {
      coverPath = coverFile.absolutePath
    } else if (fallbackAlbumId != null && fallbackAlbumId > 0L) {
      coverPath = "content://media/external/audio/albumart/$fallbackAlbumId"
    }

    if (!skipHeavyExtraction) {
      var foundSampleRateFromRetriever = false
      val retriever = MediaMetadataRetriever()
      try {
        if (uri.scheme == "file") {
          retriever.setDataSource(uri.path)
        } else {
          retriever.setDataSource(context, uri)
        }
        title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
          ?.takeIf { it.isNotBlank() } ?: title
        artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
          ?.takeIf { it.isNotBlank() } ?: artist
        album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
          ?.takeIf { it.isNotBlank() } ?: album
        genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
          ?.takeIf { it.isNotBlank() } ?: genre
        if (durationMs <= 0L) {
          durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: 0L
        }
        val bps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
          ?.toIntOrNull() ?: (fallbackBitrateKbps * 1000)
        bitrateKbps = (bps / 1000).coerceAtLeast(64)

        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
          ?.toIntOrNull()
          ?.takeIf { it > 0 }
          ?.let {
            sampleRateHz = it
            foundSampleRateFromRetriever = true
          }

        if (codec == "AUDIO") {
          retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            ?.takeIf { it.isNotBlank() }
            ?.let { mime -> codec = mimeToCodecLabel(mime) }
        }

        if (coverPath == null || coverPath.startsWith("content://")) {
          retriever.embeddedPicture?.let { bytes ->
            if (saveDownscaledCoverBytes(bytes, coverFile)) {
              coverPath = coverFile.absolutePath
            }
          }
        }
      } catch (_: Exception) {
        // Graceful fallback
      } finally {
        runCatching { retriever.release() }
      }

      if (codec == "AUDIO" || !foundSampleRateFromRetriever) {
        val extractor = MediaExtractor()
        try {
          if (uri.scheme == "file" && uri.path != null) {
            extractor.setDataSource(uri.path!!)
          } else {
            extractor.setDataSource(context, uri, null)
          }
          for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
              if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                sampleRateHz = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
              }
              if (codec == "AUDIO") {
                codec = mimeToCodecLabel(mime)
              }
              break
            }
          }
        } catch (_: Exception) {
          // Keep defaults
        } finally {
          runCatching { extractor.release() }
        }
      }
    }

    val species = BotanicalCatalog.matchSpeciesForGenre(genre)
    return TrackEntity(
      uri = uri.toString(),
      title = title,
      artist = artist,
      album = album,
      genre = genre,
      durationMs = durationMs.coerceAtLeast(1000L),
      bitrateKbps = bitrateKbps,
      sampleRateHz = sampleRateHz,
      codec = codec,
      coverPath = coverPath,
      speciesId = species.id,
      isBundled = false,
      sourceFolder = sourceFolder,
      sourcePath = sourcePath,
    )
  }

  private fun saveDownscaledCoverBytes(bytes: ByteArray, destFile: File): Boolean {
    return runCatching {
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
      if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching false

      val sampleSize = calculateInSampleSize(
        rawWidth = bounds.outWidth,
        rawHeight = bounds.outHeight,
        reqWidth = MAX_COVER_DIMENSION_PX,
        reqHeight = MAX_COVER_DIMENSION_PX,
      )
      val decoded = BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sampleSize }
      ) ?: return@runCatching false

      val maxSide = maxOf(decoded.width, decoded.height)
      val outputBitmap = if (maxSide > MAX_COVER_DIMENSION_PX) {
        val scale = MAX_COVER_DIMENSION_PX.toFloat() / maxSide.toFloat()
        val targetW = (decoded.width * scale).roundToInt().coerceAtLeast(1)
        val targetH = (decoded.height * scale).roundToInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
      } else {
        decoded
      }

      try {
        FileOutputStream(destFile).use { out ->
          outputBitmap.compress(Bitmap.CompressFormat.JPEG, COVER_JPEG_QUALITY, out)
        }
      } finally {
        if (outputBitmap !== decoded) outputBitmap.recycle()
        decoded.recycle()
      }
      true
    }.getOrDefault(false)
  }

  private fun queryDisplayName(context: Context, uri: Uri): String? {
    return runCatching {
      context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
          if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) cursor.getString(idx) else null
          } else null
        }
    }.getOrNull()
  }

  private fun guessCodec(context: Context, uri: Uri): String {
    val mime = context.contentResolver.getType(uri) ?: return "AUDIO"
    return mimeToCodecLabel(mime)
  }

  private fun mimeToCodecLabel(mime: String): String = when {
    mime.contains("flac", ignoreCase = true) -> "FLAC"
    mime.contains("mpeg", ignoreCase = true) -> "MP3"
    mime.contains("mp4", ignoreCase = true) || mime.contains("aac", ignoreCase = true) -> "AAC"
    mime.contains("ogg", ignoreCase = true) || mime.contains("vorbis", ignoreCase = true) -> "OGG"
    mime.contains("opus", ignoreCase = true) -> "OPUS"
    mime.contains("wav", ignoreCase = true) -> "WAV"
    else -> mime.substringAfter('/').uppercase()
  }
}
