package com.example.audio

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.util.ArrayDeque

@Immutable
data class ScannedAudioCandidate(
  val uri: String,
  val displayName: String,
  val folderName: String,
  val folderKey: String,
  val durationMs: Long,
  val title: String? = null,
  val artist: String? = null,
  val album: String? = null,
  val genre: String? = null,
  val bitrateKbps: Int = 192,
  val sampleRateHz: Int = 44100,
  val codec: String = "AUDIO",
)

@Immutable
data class DetectedFolderSource(
  val folderName: String,
  val folderKey: String,
  val qualifyingTracks: List<ScannedAudioCandidate>,
  val totalAudioScanned: Int,
) {
  val qualifyingCount: Int get() = qualifyingTracks.size
}

@Immutable
data class DominantFolderScanResult(
  val winningFolder: DetectedFolderSource?,
  val allFolders: List<DetectedFolderSource>,
  val totalFilesScanned: Int,
  val skippedShortCount: Int,
)

object AudioFolderScanner {

  const val MIN_LONG_AUDIO_DURATION_MS: Long = 90_000L // 1 minute 30 seconds

  private val audioExtensions = setOf(
    "mp3", "flac", "wav", "m4a", "aac", "ogg", "opus", "wma", "alac", "aiff", "m4b"
  )

  private val ignoredDirectoryNames = setOf(
    "botanical_audio", "botanical_covers", "imported_covers", ".thumbnails", ".cache", "Android"
  )

  /**
   * Groups scanned audio candidates by parent folder, filters strictly to files whose
   * duration exceeds [minDurationMs] (> 1m 30s), and picks the folder containing the
   * largest number of qualifying audio tracks.
   */
  fun selectDominantAudioFolder(
    candidates: List<ScannedAudioCandidate>,
    minDurationMs: Long = MIN_LONG_AUDIO_DURATION_MS,
  ): DominantFolderScanResult {
    if (candidates.isEmpty()) {
      return DominantFolderScanResult(
        winningFolder = null,
        allFolders = emptyList(),
        totalFilesScanned = 0,
        skippedShortCount = 0,
      )
    }

    val deduplicated = candidates.distinctBy { it.uri }
    val groupedByFolder = deduplicated.groupBy { it.folderKey }
    val folders = groupedByFolder.mapNotNull { (folderKey, items) ->
      val qualifying = items.filter { it.durationMs > minDurationMs }
      if (qualifying.isEmpty()) return@mapNotNull null
      val displayName = qualifying.first().folderName.ifBlank { folderKey }
      DetectedFolderSource(
        folderName = displayName,
        folderKey = folderKey,
        qualifyingTracks = qualifying.sortedBy { (it.title ?: it.displayName).lowercase() },
        totalAudioScanned = items.size,
      )
    }.sortedWith(
      compareByDescending<DetectedFolderSource> { it.qualifyingCount }
        .thenByDescending { it.qualifyingTracks.sumOf { t -> t.durationMs } }
        .thenBy { it.folderName.lowercase() }
    )

    val totalQualifying = folders.sumOf { it.qualifyingCount }
    return DominantFolderScanResult(
      winningFolder = folders.firstOrNull(),
      allFolders = folders,
      totalFilesScanned = deduplicated.size,
      skippedShortCount = (deduplicated.size - totalQualifying).coerceAtLeast(0),
    )
  }

  /**
   * Scans persisted SAF folder permissions, non-privileged MediaStore entries, and
   * readable Music directories to find the dominant folder with the most >1m30s tracks.
   */
  suspend fun scanAllAccessibleSources(
    context: Context,
    extraTreeUris: List<Uri> = emptyList(),
    minDurationMs: Long = MIN_LONG_AUDIO_DURATION_MS,
  ): DominantFolderScanResult = withContext(Dispatchers.IO) {
    val treeUris = buildList {
      addAll(extraTreeUris)
      context.contentResolver.persistedUriPermissions
        .filter { it.isReadPermission }
        .forEach { perm ->
          if (perm.uri !in this) add(perm.uri)
        }
    }

    val collected = coroutineScope {
      val safDeferred = treeUris.map { uri ->
        async { collectCandidatesFromSafTree(context, uri) }
      }
      val mediaStoreDeferred = async { collectCandidatesFromMediaStore(context, minDurationMs) }
      val musicDirDeferred = async {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        if (dir != null && dir.exists()) collectCandidatesFromDirectory(dir) else emptyList()
      }

      buildList {
        safDeferred.awaitAll().forEach { addAll(it) }
        addAll(mediaStoreDeferred.await())
        addAll(musicDirDeferred.await())
      }
    }

    selectDominantAudioFolder(collected, minDurationMs)
  }

  suspend fun scanSafTree(
    context: Context,
    treeUri: Uri,
    minDurationMs: Long = MIN_LONG_AUDIO_DURATION_MS,
  ): DominantFolderScanResult = withContext(Dispatchers.IO) {
    val candidates = collectCandidatesFromSafTree(context, treeUri)
    selectDominantAudioFolder(candidates, minDurationMs)
  }

  suspend fun scanFileDirectoryTree(
    rootDir: File,
    minDurationMs: Long = MIN_LONG_AUDIO_DURATION_MS,
  ): DominantFolderScanResult = withContext(Dispatchers.IO) {
    val candidates = collectCandidatesFromDirectory(rootDir)
    selectDominantAudioFolder(candidates, minDurationMs)
  }

  private data class RawDocumentEntry(
    val uri: Uri,
    val displayName: String,
    val folderName: String,
    val folderKey: String,
    val mimeType: String,
  )

  private suspend fun collectCandidatesFromSafTree(
    context: Context,
    treeUri: Uri,
  ): List<ScannedAudioCandidate> = coroutineScope {
    val resolver = context.contentResolver
    val rootDocId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
      ?: return@coroutineScope emptyList()

    val rootName = rootDocId.substringAfterLast(':').substringAfterLast('/').ifBlank { "Carpeta" }
    val rawEntries = mutableListOf<RawDocumentEntry>()
    val queue = ArrayDeque<Triple<String, String, String>>() // docId, folderName, folderKey
    queue.add(Triple(rootDocId, rootName, "${treeUri}#$rootDocId"))

    val projection = arrayOf(
      DocumentsContract.Document.COLUMN_DOCUMENT_ID,
      DocumentsContract.Document.COLUMN_DISPLAY_NAME,
      DocumentsContract.Document.COLUMN_MIME_TYPE,
      DocumentsContract.Document.COLUMN_SIZE,
    )

    var visitedDirs = 0
    val maxDirs = 240

    while (queue.isNotEmpty() && visitedDirs < maxDirs) {
      val (currentDocId, folderName, folderKey) = queue.removeFirst()
      visitedDirs++

      val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, currentDocId)
      runCatching {
        resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
          val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
          val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
          val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
          val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

          while (cursor.moveToNext()) {
            val childId = if (idCol >= 0) cursor.getString(idCol) ?: continue else continue
            val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "Audio" else "Audio"
            val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "" else ""
            val sizeBytes = if (sizeCol >= 0 && !cursor.isNull(sizeCol)) cursor.getLong(sizeCol) else -1L

            if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
              if (name !in ignoredDirectoryNames && !name.startsWith(".")) {
                queue.add(Triple(childId, name, "${treeUri}#$childId"))
              }
            } else if (isLikelyAudioFile(name, mime) && (sizeBytes < 0L || sizeBytes >= 64_000L)) {
              val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
              rawEntries += RawDocumentEntry(
                uri = docUri,
                displayName = name,
                folderName = folderName,
                folderKey = folderKey,
                mimeType = mime,
              )
            }
          }
        }
      }
    }

    if (rawEntries.isEmpty()) return@coroutineScope emptyList()

    val semaphore = Semaphore(4)
    rawEntries.map { entry ->
      async(Dispatchers.IO) {
        semaphore.withPermit {
          val durationMs = probeUriDurationMs(context, entry.uri)
          ScannedAudioCandidate(
            uri = entry.uri.toString(),
            displayName = entry.displayName.substringBeforeLast('.'),
            folderName = entry.folderName,
            folderKey = entry.folderKey,
            durationMs = durationMs,
            codec = guessCodecFromNameOrMime(entry.displayName, entry.mimeType),
          )
        }
      }
    }.awaitAll()
  }

  private suspend fun collectCandidatesFromDirectory(rootDir: File): List<ScannedAudioCandidate> =
    coroutineScope {
      if (!rootDir.exists() || !rootDir.isDirectory) return@coroutineScope emptyList()
      val files = mutableListOf<File>()
      val queue = ArrayDeque<File>()
      queue.add(rootDir)

      var visitedDirs = 0
      while (queue.isNotEmpty() && visitedDirs < 240) {
        val dir = queue.removeFirst()
        visitedDirs++
        val children = dir.listFiles() ?: continue
        for (child in children) {
          if (child.isDirectory) {
            if (child.name !in ignoredDirectoryNames && !child.name.startsWith(".")) {
              queue.add(child)
            }
          } else if (child.isFile && isLikelyAudioFile(child.name, "") && child.length() >= 4_096L) {
            files += child
          }
        }
      }

      if (files.isEmpty()) return@coroutineScope emptyList()

      val semaphore = Semaphore(4)
      files.map { file ->
        async(Dispatchers.IO) {
          semaphore.withPermit {
            val parent = file.parentFile
            val folderName = parent?.name?.takeIf { it.isNotBlank() } ?: rootDir.name
            val folderKey = parent?.absolutePath ?: rootDir.absolutePath
            val durationMs = probeFileDurationMs(file)
            ScannedAudioCandidate(
              uri = file.toURI().toString(),
              displayName = file.nameWithoutExtension,
              folderName = folderName,
              folderKey = folderKey,
              durationMs = durationMs,
              codec = guessCodecFromNameOrMime(file.name, ""),
            )
          }
        }
      }.awaitAll()
    }

  private fun collectCandidatesFromMediaStore(
    context: Context,
    minDurationMs: Long,
  ): List<ScannedAudioCandidate> {
    val results = mutableListOf<ScannedAudioCandidate>()
    val projection = buildList {
      add(MediaStore.Audio.Media._ID)
      add(MediaStore.Audio.Media.DISPLAY_NAME)
      add(MediaStore.Audio.Media.TITLE)
      add(MediaStore.Audio.Media.ARTIST)
      add(MediaStore.Audio.Media.ALBUM)
      add(MediaStore.Audio.Media.DURATION)
      add(MediaStore.Audio.Media.MIME_TYPE)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        add(MediaStore.Audio.Media.RELATIVE_PATH)
        add(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
      }
    }.toTypedArray()

    runCatching {
      context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        "${MediaStore.Audio.Media.DURATION} > ?",
        arrayOf(minDurationMs.toString()),
        "${MediaStore.Audio.Media.DATE_ADDED} DESC"
      )?.use { cursor ->
        val idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
        val nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
        val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
        val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
        val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
        val durCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
        val mimeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
        val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
        } else -1
        val bucketCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          cursor.getColumnIndex(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
        } else -1

        while (cursor.moveToNext()) {
          if (idCol < 0) continue
          val id = cursor.getLong(idCol)
          val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
          val displayName = (if (nameCol >= 0) cursor.getString(nameCol) else null) ?: "Pista"
          val title = (if (titleCol >= 0) cursor.getString(titleCol) else null)?.takeIf { it.isNotBlank() }
          val artist = (if (artistCol >= 0) cursor.getString(artistCol) else null)?.takeIf { it.isNotBlank() && it != "<unknown>" }
          val album = (if (albumCol >= 0) cursor.getString(albumCol) else null)?.takeIf { it.isNotBlank() }
          val duration = if (durCol >= 0) cursor.getLong(durCol) else 0L
          val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "" else ""
          val relPath = if (relPathCol >= 0) cursor.getString(relPathCol) ?: "" else ""
          val bucket = if (bucketCol >= 0) cursor.getString(bucketCol) ?: "" else ""
          val folderName = bucket.ifBlank {
            relPath.trimEnd('/').substringAfterLast('/').ifBlank { "MediaStore" }
          }
          val folderKey = relPath.ifBlank { "mediastore://$folderName" }

          results += ScannedAudioCandidate(
            uri = contentUri.toString(),
            displayName = displayName.substringBeforeLast('.'),
            folderName = folderName,
            folderKey = folderKey,
            durationMs = duration,
            title = title,
            artist = artist,
            album = album,
            codec = guessCodecFromNameOrMime(displayName, mime),
          )
        }
      }
    }
    return results
  }

  private fun probeUriDurationMs(context: Context, uri: Uri): Long {
    val retriever = MediaMetadataRetriever()
    return try {
      retriever.setDataSource(context, uri)
      retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    } catch (_: Exception) {
      0L
    } finally {
      runCatching { retriever.release() }
    }
  }

  private fun probeFileDurationMs(file: File): Long {
    val retriever = MediaMetadataRetriever()
    return try {
      retriever.setDataSource(file.absolutePath)
      retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    } catch (_: Exception) {
      0L
    } finally {
      runCatching { retriever.release() }
    }
  }

  internal fun isLikelyAudioFile(name: String, mime: String): Boolean {
    if (mime.startsWith("audio/")) return true
    val ext = name.substringAfterLast('.', "").lowercase()
    return ext in audioExtensions
  }

  private fun guessCodecFromNameOrMime(name: String, mime: String): String {
    val ext = name.substringAfterLast('.', "").uppercase()
    if (ext.isNotBlank() && ext != name.uppercase()) return ext
    return when {
      mime.contains("flac") -> "FLAC"
      mime.contains("mpeg") -> "MP3"
      mime.contains("mp4") || mime.contains("aac") -> "AAC"
      mime.contains("ogg") || mime.contains("vorbis") -> "OGG"
      mime.contains("wav") -> "WAV"
      mime.contains("/") -> mime.substringAfter('/').uppercase()
      else -> "AUDIO"
    }
  }
}
