package com.example.audio

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import com.example.data.model.AudioTrack
import com.example.data.model.BotanicalLyricsCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset

@Immutable
data class LyricsLine(
  val timestampMs: Long?,
  val text: String,
)

@Immutable
data class ParsedLyrics(
  val lines: List<LyricsLine>,
  val isSynced: Boolean,
  val isBotanicalFallback: Boolean,
  val sourceLabel: String,
)

object LyricsParser {

  private val lrcTagRegex = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

  fun parseLyricsText(
    raw: String,
    sourceLabel: String = "Letra personalizada",
    isBotanicalFallback: Boolean = false,
  ): ParsedLyrics {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) {
      return ParsedLyrics(
        lines = emptyList(),
        isSynced = false,
        isBotanicalFallback = isBotanicalFallback,
        sourceLabel = sourceLabel,
      )
    }

    val syncedLines = mutableListOf<LyricsLine>()
    val plainLines = mutableListOf<LyricsLine>()

    trimmed.lines().forEach { rawLine ->
      val line = rawLine.trim()
      if (line.isEmpty()) return@forEach

      val matches = lrcTagRegex.findAll(line).toList()
      if (matches.isNotEmpty()) {
        val text = lrcTagRegex.replace(line, "").trim()
        if (text.isNotEmpty()) {
          matches.forEach { m ->
            val min = m.groupValues[1].toLongOrNull() ?: 0L
            val sec = m.groupValues[2].toLongOrNull() ?: 0L
            val fracRaw = m.groupValues[3]
            val millis = when (fracRaw.length) {
              0 -> 0L
              1 -> (fracRaw.toLongOrNull() ?: 0L) * 100L
              2 -> (fracRaw.toLongOrNull() ?: 0L) * 10L
              else -> fracRaw.take(3).toLongOrNull() ?: 0L
            }
            val ts = (min * 60_000L) + (sec * 1_000L) + millis
            syncedLines += LyricsLine(timestampMs = ts, text = text)
          }
        }
      } else if (!line.startsWith("[ti:") && !line.startsWith("[ar:") &&
        !line.startsWith("[al:") && !line.startsWith("[by:") && !line.startsWith("[offset:")
      ) {
        plainLines += LyricsLine(timestampMs = null, text = line)
      }
    }

    return if (syncedLines.isNotEmpty()) {
      ParsedLyrics(
        lines = syncedLines.sortedBy { it.timestampMs ?: 0L },
        isSynced = true,
        isBotanicalFallback = isBotanicalFallback,
        sourceLabel = "$sourceLabel · LRC",
      )
    } else {
      ParsedLyrics(
        lines = plainLines,
        isSynced = false,
        isBotanicalFallback = isBotanicalFallback,
        sourceLabel = sourceLabel,
      )
    }
  }

  fun findActiveLineIndex(
    parsed: ParsedLyrics,
    positionMs: Long,
    durationMs: Long,
  ): Int {
    if (parsed.lines.isEmpty()) return 0
    if (parsed.isSynced) {
      var active = 0
      for (i in parsed.lines.indices) {
        val ts = parsed.lines[i].timestampMs ?: continue
        if (ts <= positionMs + 150L) {
          active = i
        } else {
          break
        }
      }
      return active
    }
    val safeDur = durationMs.coerceAtLeast(1L)
    val frac = (positionMs.coerceIn(0L, safeDur).toFloat() / safeDur.toFloat()).coerceIn(0f, 1f)
    return (frac * parsed.lines.size).toInt().coerceIn(0, parsed.lines.lastIndex)
  }

  suspend fun resolveTrackLyrics(context: Context, track: AudioTrack?): ParsedLyrics =
    withContext(Dispatchers.IO) {
      if (track == null) {
        val verses = BotanicalLyricsCatalog.getLyricsForTrack(null)
        return@withContext ParsedLyrics(
          lines = verses.map { LyricsLine(null, it) },
          isSynced = false,
          isBotanicalFallback = true,
          sourceLabel = "Versos del Herbario",
        )
      }

      // 1. User-saved or imported lyrics stored in Room
      val stored = track.lyrics?.trim()
      if (!stored.isNullOrEmpty()) {
        return@withContext parseLyricsText(
          raw = stored,
          sourceLabel = "Letra guardada",
          isBotanicalFallback = false,
        )
      }

      // 2. Sidecar .lrc file next to file:// audio
      val parsedUri = runCatching { Uri.parse(track.uri) }.getOrNull()
      if (parsedUri?.scheme == "file" && parsedUri.path != null) {
        val audioFile = File(parsedUri.path!!)
        val sidecarLrc = File(audioFile.parentFile, audioFile.nameWithoutExtension + ".lrc")
        if (sidecarLrc.exists() && sidecarLrc.isFile && sidecarLrc.length() in 4..524_288) {
          val lrcText = runCatching { sidecarLrc.readText() }.getOrNull()
          if (!lrcText.isNullOrBlank()) {
            return@withContext parseLyricsText(
              raw = lrcText,
              sourceLabel = "Archivo .lrc",
              isBotanicalFallback = false,
            )
          }
        }
      }

      // 3. Embedded lyrics tag (ID3v2 USLT, MP4 ©lyr, or FLAC/Ogg LYRICS comment)
      if (!track.isBundled && parsedUri != null) {
        val embedded = extractEmbeddedLyrics(context, parsedUri)
        if (!embedded.isNullOrBlank()) {
          return@withContext parseLyricsText(
            raw = embedded,
            sourceLabel = "Letra incrustada",
            isBotanicalFallback = false,
          )
        }
      }

      // 4. Botanical fallback verses
      val fallback = BotanicalLyricsCatalog.getLyricsForTrack(track)
      ParsedLyrics(
        lines = fallback.map { LyricsLine(null, it) },
        isSynced = false,
        isBotanicalFallback = true,
        sourceLabel = "Versos del Herbario (sin letra propia)",
      )
    }

  private fun extractEmbeddedLyrics(context: Context, uri: Uri): String? {
    val headerBytes = readBoundedHeaderBytes(context, uri, maxBytes = 256 * 1024) ?: return null
    if (headerBytes.size < 16) return null

    // Check ID3v2 header
    if (headerBytes[0] == 'I'.code.toByte() &&
      headerBytes[1] == 'D'.code.toByte() &&
      headerBytes[2] == '3'.code.toByte()
    ) {
      val majorVersion = headerBytes[3].toInt() and 0xFF
      val tagSize = syncSafeInt(headerBytes, 6).coerceAtMost(headerBytes.size - 10)
      parseId3Uslt(headerBytes, majorVersion, 10 + tagSize)?.let { return it }
    }

    // Check Vorbis / FLAC LYRICS= or UNSYNCEDLYRICS= UTF-8 tag or MP4 ©lyr atom inside header
    return extractVorbisOrMp4Lyrics(headerBytes)
  }

  private fun readBoundedHeaderBytes(context: Context, uri: Uri, maxBytes: Int): ByteArray? {
    return runCatching {
      val input = if (uri.scheme == "file" && uri.path != null) {
        File(uri.path!!).inputStream()
      } else {
        context.contentResolver.openInputStream(uri)
      } ?: return null
      input.use { stream ->
        val buffer = ByteArray(maxBytes)
        var totalRead = 0
        while (totalRead < maxBytes) {
          val n = stream.read(buffer, totalRead, maxBytes - totalRead)
          if (n <= 0) break
          totalRead += n
        }
        if (totalRead == 0) null else buffer.copyOf(totalRead)
      }
    }.getOrNull()
  }

  private fun syncSafeInt(bytes: ByteArray, offset: Int): Int {
    if (offset + 3 >= bytes.size) return 0
    return ((bytes[offset].toInt() and 0x7F) shl 21) or
      ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
      ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
      (bytes[offset + 3].toInt() and 0x7F)
  }

  private fun bigEndianInt(bytes: ByteArray, offset: Int): Int {
    if (offset + 3 >= bytes.size) return 0
    return ((bytes[offset].toInt() and 0xFF) shl 24) or
      ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
      ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
      (bytes[offset + 3].toInt() and 0xFF)
  }

  private fun parseId3Uslt(bytes: ByteArray, majorVersion: Int, limit: Int): String? {
    if (majorVersion !in 3..4) return null
    var pos = 10
    val safeLimit = limit.coerceAtMost(bytes.size)
    while (pos + 10 <= safeLimit) {
      val frameId = String(bytes, pos, 4, Charsets.ISO_8859_1)
      if (frameId.all { it.code == 0 }) break
      val frameSize = if (majorVersion == 4) syncSafeInt(bytes, pos + 4) else bigEndianInt(bytes, pos + 4)
      if (frameSize <= 0 || pos + 10 + frameSize > safeLimit) break
      if (frameId == "USLT") {
        val payloadStart = pos + 10
        val encoding = bytes[payloadStart].toInt() and 0xFF
        // Skip 1 byte encoding + 3 bytes language, then find null-terminated content descriptor
        var textOffset = payloadStart + 4
        if (textOffset >= payloadStart + frameSize) return null
        val charset: Charset = when (encoding) {
          1 -> Charsets.UTF_16
          2 -> Charsets.UTF_16BE
          3 -> Charsets.UTF_8
          else -> Charsets.ISO_8859_1
        }
        val isTwoByte = encoding == 1 || encoding == 2
        if (isTwoByte) {
          while (textOffset + 1 < payloadStart + frameSize) {
            if (bytes[textOffset] == 0.toByte() && bytes[textOffset + 1] == 0.toByte()) {
              textOffset += 2
              break
            }
            textOffset += 2
          }
        } else {
          while (textOffset < payloadStart + frameSize) {
            if (bytes[textOffset] == 0.toByte()) {
              textOffset += 1
              break
            }
            textOffset++
          }
        }
        val len = (payloadStart + frameSize) - textOffset
        if (len > 0) {
          val decoded = String(bytes, textOffset, len, charset).trim('\u0000', ' ', '\r', '\n')
          if (decoded.isNotBlank()) return decoded
        }
      }
      pos += 10 + frameSize
    }
    return null
  }

  private fun extractVorbisOrMp4Lyrics(bytes: ByteArray): String? {
    // Vorbis comment keys: LYRICS= or UNSYNCEDLYRICS=
    val ascii = String(bytes, Charsets.ISO_8859_1)
    for (key in listOf("LYRICS=", "UNSYNCEDLYRICS=", "lyrics=")) {
      val idx = ascii.indexOf(key)
      if (idx >= 4) {
        val valStart = idx + key.length
        // Little-endian comment length precedes the key
        val len = (bytes[idx - 4].toInt() and 0xFF) or
          ((bytes[idx - 3].toInt() and 0xFF) shl 8) or
          ((bytes[idx - 2].toInt() and 0xFF) shl 16) or
          ((bytes[idx - 1].toInt() and 0xFF) shl 24)
        val valueLen = len - key.length
        if (valueLen in 2..65_536 && valStart + valueLen <= bytes.size) {
          val decoded = String(bytes, valStart, valueLen, Charsets.UTF_8).trim()
          if (decoded.isNotBlank()) return decoded
        }
      }
    }

    // MP4 ©lyr atom: [0xA9, 'l', 'y', 'r'] followed by 'data' sub-atom
    val marker = byteArrayOf(0xA9.toByte(), 'l'.code.toByte(), 'y'.code.toByte(), 'r'.code.toByte())
    val lyrIdx = indexOfBytes(bytes, marker)
    if (lyrIdx >= 4 && lyrIdx + 20 < bytes.size) {
      val atomSize = bigEndianInt(bytes, lyrIdx - 4)
      if (atomSize in 24..65_536 && lyrIdx - 4 + atomSize <= bytes.size) {
        val dataIdx = indexOfBytes(bytes, "data".toByteArray(Charsets.ISO_8859_1), lyrIdx, lyrIdx - 4 + atomSize)
        if (dataIdx >= 4 && dataIdx + 12 < bytes.size) {
          val dataAtomSize = bigEndianInt(bytes, dataIdx - 4)
          val textStart = dataIdx + 12
          val textLen = dataAtomSize - 16
          if (textLen > 1 && textStart + textLen <= bytes.size) {
            val decoded = String(bytes, textStart, textLen, Charsets.UTF_8).trim()
            if (decoded.isNotBlank()) return decoded
          }
        }
      }
    }
    return null
  }

  private fun indexOfBytes(
    haystack: ByteArray,
    needle: ByteArray,
    start: Int = 0,
    end: Int = haystack.size,
  ): Int {
    val limit = (end - needle.size).coerceAtMost(haystack.size - needle.size)
    for (i in start.coerceAtLeast(0)..limit) {
      var match = true
      for (j in needle.indices) {
        if (haystack[i + j] != needle[j]) {
          match = false
          break
        }
      }
      if (match) return i
    }
    return -1
  }
}
