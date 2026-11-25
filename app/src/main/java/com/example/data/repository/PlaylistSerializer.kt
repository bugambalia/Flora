package com.example.data.repository

import com.example.data.model.AudioTrack
import com.example.data.model.BotanicalSpeciesId
import com.example.data.model.PlaylistWithTracks
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ExportedPlaylistTrackRef(
  val uri: String,
  val title: String,
  val artist: String,
  val album: String = "",
  val durationMs: Long = 0L,
)

@Serializable
data class ExportedPlaylistPayload(
  val name: String,
  val description: String = "",
  val speciesMotif: String = BotanicalSpeciesId.TIGER_LILY.id,
  val tracks: List<ExportedPlaylistTrackRef> = emptyList(),
)

data class ImportedPlaylistData(
  val name: String,
  val description: String,
  val motif: BotanicalSpeciesId,
  val trackRefs: List<ExportedPlaylistTrackRef>,
)

object PlaylistSerializer {

  private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  fun exportToM3u(playlist: PlaylistWithTracks): String {
    val sb = StringBuilder()
    sb.appendLine("#EXTM3U")
    sb.appendLine("#PLAYLIST:${playlist.playlist.name}")
    playlist.tracks.forEach { track ->
      val seconds = (track.durationMs / 1000L).coerceAtLeast(0L)
      sb.appendLine("#EXTINF:$seconds,${track.artist} - ${track.title}")
      sb.appendLine(track.uri)
    }
    return sb.toString()
  }

  fun exportToJson(playlist: PlaylistWithTracks): String {
    val payload = ExportedPlaylistPayload(
      name = playlist.playlist.name,
      description = playlist.playlist.description,
      speciesMotif = playlist.playlist.speciesMotif.id,
      tracks = playlist.tracks.map { track ->
        ExportedPlaylistTrackRef(
          uri = track.uri,
          title = track.title,
          artist = track.artist,
          album = track.album,
          durationMs = track.durationMs,
        )
      }
    )
    return json.encodeToString(payload)
  }

  fun parseImport(raw: String, fallbackName: String = "Lista importada"): ImportedPlaylistData {
    val trimmed = raw.trim()
    if (trimmed.startsWith("{")) {
      runCatching {
        val payload = json.decodeFromString<ExportedPlaylistPayload>(trimmed)
        return ImportedPlaylistData(
          name = payload.name.ifBlank { fallbackName },
          description = payload.description,
          motif = BotanicalSpeciesId.fromId(payload.speciesMotif),
          trackRefs = payload.tracks,
        )
      }
    }

    var playlistName = fallbackName
    val refs = mutableListOf<ExportedPlaylistTrackRef>()
    var pendingTitle = ""
    var pendingArtist = ""
    var pendingDuration = 0L

    trimmed.lines().forEach { rawLine ->
      val line = rawLine.trim()
      if (line.isEmpty()) return@forEach
      when {
        line.startsWith("#EXTM3U") -> Unit
        line.startsWith("#PLAYLIST:") -> {
          val parsed = line.removePrefix("#PLAYLIST:").trim()
          if (parsed.isNotBlank()) playlistName = parsed
        }
        line.startsWith("#EXTINF:") -> {
          val body = line.removePrefix("#EXTINF:")
          val secPart = body.substringBefore(',').trim().toLongOrNull() ?: 0L
          pendingDuration = secPart * 1000L
          val label = body.substringAfter(',', "").trim()
          if (label.contains(" - ")) {
            pendingArtist = label.substringBefore(" - ").trim()
            pendingTitle = label.substringAfter(" - ").trim()
          } else {
            pendingTitle = label
            pendingArtist = ""
          }
        }
        line.startsWith("#") -> Unit
        else -> {
          val fileTitle = line.substringAfterLast('/').substringBeforeLast('.')
          refs += ExportedPlaylistTrackRef(
            uri = line,
            title = pendingTitle.ifBlank { fileTitle },
            artist = pendingArtist,
            durationMs = pendingDuration,
          )
          pendingTitle = ""
          pendingArtist = ""
          pendingDuration = 0L
        }
      }
    }

    return ImportedPlaylistData(
      name = playlistName,
      description = "Importada desde archivo",
      motif = BotanicalSpeciesId.FERN,
      trackRefs = refs,
    )
  }

  fun matchTracksInLibrary(
    refs: List<ExportedPlaylistTrackRef>,
    libraryTracks: List<AudioTrack>,
  ): List<AudioTrack> {
    if (refs.isEmpty() || libraryTracks.isEmpty()) return emptyList()
    val byUri = libraryTracks.associateBy { it.uri }
    val matched = mutableListOf<AudioTrack>()
    val seenIds = HashSet<Long>()

    for (ref in refs) {
      val exact = byUri[ref.uri]
      val candidate = exact ?: libraryTracks.firstOrNull { track ->
        val titleMatches = ref.title.isNotBlank() &&
          track.title.equals(ref.title, ignoreCase = true)
        val artistMatches = ref.artist.isBlank() ||
          track.artist.equals(ref.artist, ignoreCase = true)
        val fileTailMatches = ref.uri.isNotBlank() &&
          track.uri.substringAfterLast('/').equals(ref.uri.substringAfterLast('/'), ignoreCase = true)
        fileTailMatches || (titleMatches && artistMatches)
      }
      if (candidate != null && seenIds.add(candidate.id)) {
        matched += candidate
      }
    }
    return matched
  }
}
