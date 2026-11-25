package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.audio.AlbumPaletteService
import com.example.audio.AudioFolderScanner
import com.example.audio.BotanicalAudioSynthesizer
import com.example.audio.DetectedFolderSource
import com.example.audio.DominantFolderScanResult
import com.example.audio.LocalAudioImporter
import com.example.data.local.BotanicalAudioDao
import com.example.data.local.BotanicalProgressEntity
import com.example.data.local.PlaylistEntity
import com.example.data.model.AlbumPaletteColors
import com.example.data.model.AudioTrack
import com.example.data.model.BotanicalCatalog
import com.example.data.model.BotanicalProgress
import com.example.data.model.BotanicalSpecies
import com.example.data.model.BotanicalSpeciesId
import com.example.data.model.PlaylistWithTracks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class BotanicalLibraryScanSummary(
  val scanResult: DominantFolderScanResult,
  val newlyImportedCount: Int,
  val newlyCategorizedCount: Int,
  val totalInBotanicalLibrary: Int,
  val collectionId: Long?,
  val triggeredManually: Boolean = false,
)

class MusicRepository(
  private val context: Context,
  private val dao: BotanicalAudioDao,
  private val paletteService: AlbumPaletteService = AlbumPaletteService(context),
) {
  val tracksFlow: Flow<List<AudioTrack>> = dao.observeAllTracks().map { list ->
    list.map { it.toDomain() }
  }

  val playlistsFlow: Flow<List<PlaylistWithTracks>> = dao.observePlaylistsWithTracks().map { list ->
    list.map { it.toDomain() }
  }

  val botanicalProgressFlow: Flow<Map<BotanicalSpeciesId, BotanicalProgress>> =
    dao.observeBotanicalProgress().map { items ->
      items.associate { item ->
        val domain = item.toDomain()
        domain.speciesId to domain
      }
    }

  suspend fun initializeLibraryIfNeeded() = withContext(Dispatchers.IO) {
    // Ensure all 8 botanical progress rows exist (Tiger Lily unlocked initially)
    val initialProgress = BotanicalCatalog.allSpecies.mapIndexed { idx, species ->
      BotanicalProgressEntity(
        speciesId = species.id.id,
        isUnlocked = idx == 0,
        unlockedAt = if (idx == 0) System.currentTimeMillis() else null,
        unlockReason = if (idx == 0) "Espécimen inicial del herbario" else null,
        listenSeconds = 0
      )
    }
    dao.insertInitialBotanicalProgress(initialProgress)

    if (dao.getTrackCount() == 0) {
      val seeds = BotanicalAudioSynthesizer.ensureSeedTracksGenerated(context)
      val insertedIds = dao.insertTracks(seeds)

      if (dao.getPlaylistCount() == 0 && insertedIds.size >= 8) {
        val selvaId = dao.insertPlaylist(
          PlaylistEntity(
            name = "Selva húmeda",
            description = "Heliconia rostrata • Paisajes cálidos de bosque tropical",
            speciesMotif = BotanicalSpeciesId.PASSIFLORA.id,
          )
        )
        listOf(0, 5, 4, 6).forEach { idx ->
          dao.addTrackToPlaylist(selvaId, insertedIds[idx])
        }

        val andinoId = dao.insertPlaylist(
          PlaylistEntity(
            name = "Bosque andino",
            description = "Cattleya azul mosaico • Niebla de altura y cordillera",
            speciesMotif = BotanicalSpeciesId.LAVENDER.id,
          )
        )
        listOf(3, 1, 7).forEach { idx ->
          dao.addTrackToPlaylist(andinoId, insertedIds[idx])
        }

        val cienagaId = dao.insertPlaylist(
          PlaylistEntity(
            name = "Ciénaga dorada",
            description = "Loto de agua • Resonancias serenas de ribera",
            speciesMotif = BotanicalSpeciesId.LOTUS.id,
          )
        )
        listOf(4, 2, 0).forEach { idx ->
          dao.addTrackToPlaylist(cienagaId, insertedIds[idx])
        }
      }
    }
  }

  suspend fun scanAccessibleFolders(
    extraTreeUri: Uri? = null,
    minDurationMs: Long = AudioFolderScanner.MIN_LONG_AUDIO_DURATION_MS,
  ): DominantFolderScanResult {
    if (extraTreeUri != null) {
      runCatching {
        context.contentResolver.takePersistableUriPermission(
          extraTreeUri,
          Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
      }
    }
    return AudioFolderScanner.scanAllAccessibleSources(
      context = context,
      extraTreeUris = listOfNotNull(extraTreeUri),
      minDurationMs = minDurationMs,
    )
  }

  suspend fun scanAndCategorizeIntoBotanicalLibrary(
    extraTreeUri: Uri? = null,
    triggeredManually: Boolean = false,
    minDurationMs: Long = AudioFolderScanner.MIN_LONG_AUDIO_DURATION_MS,
  ): BotanicalLibraryScanSummary = withContext(Dispatchers.IO) {
    val scanResult = scanAccessibleFolders(extraTreeUri, minDurationMs)
    var newlyImported = 0

    if (scanResult.allFolders.isNotEmpty()) {
      val existingUris = dao.getAllTrackUris().toHashSet()
      val seenInBatch = HashSet(existingUris)
      val entitiesToInsert = mutableListOf<com.example.data.local.TrackEntity>()

      for (folder in scanResult.allFolders) {
        val imported = LocalAudioImporter.importScannedFolder(
          context = context,
          folder = folder,
          existingUris = seenInBatch,
        )
        imported.forEach { entity ->
          if (seenInBatch.add(entity.uri)) {
            entitiesToInsert += entity
          }
        }
      }

      if (entitiesToInsert.isNotEmpty()) {
        dao.insertTracks(entitiesToInsert)
        newlyImported = entitiesToInsert.size
      }
    }

    val (collectionId, newlyCategorized, totalInLibrary) = categorizeLongTracksIntoBotanicalLibrary(minDurationMs)
    BotanicalLibraryScanSummary(
      scanResult = scanResult,
      newlyImportedCount = newlyImported,
      newlyCategorizedCount = newlyCategorized,
      totalInBotanicalLibrary = totalInLibrary,
      collectionId = collectionId,
      triggeredManually = triggeredManually,
    )
  }

  suspend fun categorizeLongTracksIntoBotanicalLibrary(
    minDurationMs: Long = AudioFolderScanner.MIN_LONG_AUDIO_DURATION_MS,
  ): Triple<Long?, Int, Int> = withContext(Dispatchers.IO) {
    val longTracks = dao.getTracksLongerThan(minDurationMs)
    if (longTracks.isEmpty()) {
      val existing = dao.getPlaylistByName(BOTANICAL_LIBRARY_COLLECTION_NAME)
      val existingCount = existing?.let { dao.getTrackIdsInPlaylist(it.id).size } ?: 0
      return@withContext Triple(existing?.id, 0, existingCount)
    }

    val existingPlaylist = dao.getPlaylistByName(BOTANICAL_LIBRARY_COLLECTION_NAME)
    val collectionId = existingPlaylist?.id ?: dao.insertPlaylist(
      PlaylistEntity(
        name = BOTANICAL_LIBRARY_COLLECTION_NAME,
        description = "Audios mayores a ${minDurationMs / 1000L} s detectados automáticamente",
        speciesMotif = BotanicalSpeciesId.MONSTERA.id,
      )
    )

    val currentIds = dao.getTrackIdsInPlaylist(collectionId).toHashSet()
    var addedToCollection = 0
    for (track in longTracks) {
      if (track.id !in currentIds) {
        dao.addTrackToPlaylist(collectionId, track.id)
        currentIds.add(track.id)
        addedToCollection++
      }
    }
    Triple(collectionId, addedToCollection, currentIds.size)
  }

  suspend fun importDetectedFolder(folder: DetectedFolderSource): Int = withContext(Dispatchers.IO) {
    val existingUris = dao.getAllTrackUris().toHashSet()
    val entities = LocalAudioImporter.importScannedFolder(
      context = context,
      folder = folder,
      existingUris = existingUris,
    )
    if (entities.isEmpty()) {
      categorizeLongTracksIntoBotanicalLibrary()
      return@withContext 0
    }
    dao.insertTracks(entities)
    categorizeLongTracksIntoBotanicalLibrary()
    entities.size
  }

  suspend fun importLocalAudio(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
    val existingUris = dao.getAllTrackUris().toHashSet()
    val entities = LocalAudioImporter.importUris(
      context = context,
      uris = uris,
      existingUris = existingUris,
    )
    if (entities.isEmpty()) return@withContext 0
    dao.insertTracks(entities)
    categorizeLongTracksIntoBotanicalLibrary()
    entities.size
  }

  companion object {
    const val BOTANICAL_LIBRARY_COLLECTION_NAME = "Botanical Library"
  }

  suspend fun toggleFavorite(trackId: Long): BotanicalSpecies? {
    dao.toggleFavorite(trackId)
    val favCount = dao.getFavoriteCount()
    if (favCount >= 2) {
      return unlockSpeciesIfNeeded(
        BotanicalSpeciesId.CAMELLIA,
        "Desbloqueado al reunir $favCount pistas favoritas"
      )
    }
    return null
  }

  suspend fun updateTrackMetadata(
    trackId: Long,
    title: String,
    artist: String,
    album: String,
    genre: String,
    coverPath: String?,
  ): BotanicalSpeciesId = withContext(Dispatchers.IO) {
    val cleanGenre = genre.trim().ifBlank { "Acústico" }
    val matchedSpecies = BotanicalCatalog.matchSpeciesForGenre(cleanGenre)
    dao.updateTrackMetadata(
      trackId = trackId,
      title = title.trim().ifBlank { "Pista sin título" },
      artist = artist.trim().ifBlank { "Artista desconocido" },
      album = album.trim().ifBlank { "Sin álbum" },
      genre = cleanGenre,
      speciesId = matchedSpecies.id,
      coverPath = coverPath,
    )
    matchedSpecies
  }

  suspend fun updateTrackLyrics(trackId: Long, rawLyrics: String?) = withContext(Dispatchers.IO) {
    dao.updateTrackLyrics(trackId, rawLyrics?.trim()?.takeIf { it.isNotEmpty() })
  }

  suspend fun deleteTrack(trackId: Long) = withContext(Dispatchers.IO) {
    dao.deleteTrack(trackId)
  }

  suspend fun addListenSeconds(speciesId: BotanicalSpeciesId, deltaSeconds: Int) {
    if (deltaSeconds <= 0) return
    withContext(Dispatchers.IO) {
      dao.addListenSeconds(speciesId.id, deltaSeconds)
    }
  }

  suspend fun createPlaylist(
    name: String,
    description: String,
    motif: BotanicalSpeciesId,
    initialTrackIds: List<Long> = emptyList(),
  ): Pair<Long, BotanicalSpecies?> {
    val id = dao.insertPlaylist(
      PlaylistEntity(
        name = name.trim(),
        description = description.trim(),
        speciesMotif = motif.id,
      )
    )
    initialTrackIds.forEach { trackId ->
      dao.addTrackToPlaylist(id, trackId)
    }
    val unlocked = unlockSpeciesIfNeeded(
      BotanicalSpeciesId.FERN,
      "Desbloqueado al curar la lista «$name»"
    )
    return id to unlocked
  }

  suspend fun duplicatePlaylist(source: PlaylistWithTracks): Pair<Long, BotanicalSpecies?> {
    val copyName = "${source.playlist.name} (copia)"
    return createPlaylist(
      name = copyName,
      description = source.playlist.description,
      motif = source.playlist.speciesMotif,
      initialTrackIds = source.tracks.map { it.id },
    )
  }

  suspend fun importPlaylistData(
    data: ImportedPlaylistData,
    libraryTracks: List<AudioTrack>,
  ): Pair<Long, Int> {
    val matched = PlaylistSerializer.matchTracksInLibrary(data.trackRefs, libraryTracks)
    val (id, _) = createPlaylist(
      name = data.name,
      description = data.description,
      motif = data.motif,
      initialTrackIds = matched.map { it.id },
    )
    return id to matched.size
  }

  suspend fun updatePlaylist(playlistId: Long, name: String, description: String, motif: BotanicalSpeciesId) {
    dao.updatePlaylist(
      PlaylistEntity(
        id = playlistId,
        name = name.trim(),
        description = description.trim(),
        speciesMotif = motif.id,
        updatedAt = System.currentTimeMillis()
      )
    )
  }

  suspend fun deletePlaylist(playlistId: Long) {
    dao.deletePlaylistById(playlistId)
  }

  suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
    dao.addTrackToPlaylist(playlistId, trackId)
  }

  suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
    dao.removeTrackFromPlaylist(playlistId, trackId)
  }

  suspend fun reorderPlaylistTracks(playlistId: Long, orderedTrackIds: List<Long>) {
    dao.replacePlaylistTrackOrder(playlistId, orderedTrackIds)
  }

  suspend fun recordTrackPlayed(track: AudioTrack): BotanicalSpecies? {
    dao.incrementPlayCount(track.id)
    val matchedByGenre = BotanicalCatalog.matchSpeciesForGenre(track.genre)
    val targetSpecies = if (track.speciesId != BotanicalSpeciesId.TIGER_LILY) track.speciesId else matchedByGenre
    return unlockSpeciesIfNeeded(
      targetSpecies,
      "Desbloqueado al escuchar «${track.title}» (${track.genre})"
    )
  }

  suspend fun unlockSpeciesIfNeeded(speciesId: BotanicalSpeciesId, reason: String): BotanicalSpecies? {
    val existing = dao.getBotanicalProgress(speciesId.id)
    if (existing?.isUnlocked == true) return null
    dao.upsertBotanicalProgress(
      BotanicalProgressEntity(
        speciesId = speciesId.id,
        isUnlocked = true,
        unlockedAt = System.currentTimeMillis(),
        unlockReason = reason,
        listenSeconds = existing?.listenSeconds ?: 0
      )
    )
    return BotanicalCatalog.getById(speciesId)
  }

  suspend fun extractAlbumPalette(track: AudioTrack): AlbumPaletteColors =
    paletteService.extractForTrack(track)
}
