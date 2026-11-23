package com.example.audio

import android.content.Context
import android.content.Intent
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.TrackEntity
import com.example.data.model.BotanicalCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object LocalAudioImporter {

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
              fallbackCodec = candidate.codec,
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
              fallbackCodec = guessCodec(context, uri),
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
    fallbackCodec: String,
    sourceFolder: String,
    sourcePath: String,
    coverDir: File,
  ): TrackEntity {
    val retriever = MediaMetadataRetriever()
    var title = fallbackTitle ?: fallbackDisplayName
    var artist = fallbackArtist ?: "Artista Local"
    var album = fallbackAlbum ?: sourceFolder
    var genre = "Acústico"
    var durationMs = fallbackDurationMs
    var bitrateKbps = 192
    var sampleRateHz = 44100
    var coverPath: String? = null

    val coverFile = File(coverDir, "cover_${uri.toString().hashCode()}.jpg")
    if (coverFile.exists() && coverFile.length() > 512L) {
      coverPath = coverFile.absolutePath
    }

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
        ?.toIntOrNull() ?: 192000
      bitrateKbps = (bps / 1000).coerceAtLeast(64)

      retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
        ?.toIntOrNull()
        ?.takeIf { it > 0 }
        ?.let { sampleRateHz = it }

      if (coverPath == null) {
        retriever.embeddedPicture?.let { bytes ->
          FileOutputStream(coverFile).use { it.write(bytes) }
          coverPath = coverFile.absolutePath
        }
      }
    } catch (_: Exception) {
      // Graceful fallback
    } finally {
      runCatching { retriever.release() }
    }

    var codec = fallbackCodec
    if (codec == "AUDIO" || sampleRateHz == 44100) {
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
            codec = mime.removePrefix("audio/").uppercase()
            break
          }
        }
      } catch (_: Exception) {
        // Keep defaults
      } finally {
        runCatching { extractor.release() }
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
    return when {
      mime.contains("flac") -> "FLAC"
      mime.contains("mpeg") -> "MP3"
      mime.contains("mp4") || mime.contains("aac") -> "AAC"
      mime.contains("ogg") || mime.contains("vorbis") -> "OGG"
      mime.contains("wav") -> "WAV"
      else -> mime.substringAfter('/').uppercase()
    }
  }
}
