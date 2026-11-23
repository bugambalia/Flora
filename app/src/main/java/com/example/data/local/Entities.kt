package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.data.model.AudioTrack
import com.example.data.model.BotanicalProgress
import com.example.data.model.BotanicalSpeciesId
import com.example.data.model.Playlist
import com.example.data.model.PlaylistWithTracks

@Entity(tableName = "tracks")
data class TrackEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val uri: String,
  val title: String,
  val artist: String,
  val album: String,
  val genre: String,
  val durationMs: Long,
  val bitrateKbps: Int,
  val sampleRateHz: Int,
  val codec: String,
  val coverPath: String?,
  val speciesId: String,
  val playCount: Int = 0,
  val isFavorite: Boolean = false,
  val isBundled: Boolean = false,
  val sourceFolder: String = "Herbario",
  val sourcePath: String = "herbarium://bundled",
  val dateAdded: Long = System.currentTimeMillis(),
) {
  fun toDomain(): AudioTrack = AudioTrack(
    id = id,
    uri = uri,
    title = title,
    artist = artist,
    album = album,
    genre = genre,
    durationMs = durationMs,
    bitrateKbps = bitrateKbps,
    sampleRateHz = sampleRateHz,
    codec = codec,
    coverPath = coverPath,
    speciesId = BotanicalSpeciesId.fromId(speciesId),
    playCount = playCount,
    isFavorite = isFavorite,
    isBundled = isBundled,
    sourceFolder = sourceFolder,
    sourcePath = sourcePath,
  )
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val description: String,
  val speciesMotif: String,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
) {
  fun toDomain(): Playlist = Playlist(
    id = id,
    name = name,
    description = description,
    speciesMotif = BotanicalSpeciesId.fromId(speciesMotif),
    createdAt = createdAt,
    updatedAt = updatedAt,
  )
}

@Entity(
  tableName = "playlist_track_cross_ref",
  primaryKeys = ["playlistId", "trackId"],
  foreignKeys = [
    ForeignKey(
      entity = PlaylistEntity::class,
      parentColumns = ["id"],
      childColumns = ["playlistId"],
      onDelete = ForeignKey.CASCADE
    ),
    ForeignKey(
      entity = TrackEntity::class,
      parentColumns = ["id"],
      childColumns = ["trackId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [Index("playlistId"), Index("trackId")]
)
data class PlaylistTrackCrossRef(
  val playlistId: Long,
  val trackId: Long,
  val position: Int,
)

@Entity(tableName = "botanical_progress")
data class BotanicalProgressEntity(
  @PrimaryKey val speciesId: String,
  val isUnlocked: Boolean,
  val unlockedAt: Long?,
  val unlockReason: String?,
  val listenSeconds: Int = 0,
) {
  fun toDomain(): BotanicalProgress = BotanicalProgress(
    speciesId = BotanicalSpeciesId.fromId(speciesId),
    isUnlocked = isUnlocked,
    unlockedAt = unlockedAt,
    unlockReason = unlockReason,
    listenSeconds = listenSeconds,
  )
}

data class PlaylistEntryWithTrack(
  @Embedded val ref: PlaylistTrackCrossRef,
  @Relation(parentColumn = "trackId", entityColumn = "id")
  val track: TrackEntity,
)

data class PlaylistWithOrderedEntries(
  @Embedded val playlist: PlaylistEntity,
  @Relation(
    entity = PlaylistTrackCrossRef::class,
    parentColumn = "id",
    entityColumn = "playlistId"
  )
  val entries: List<PlaylistEntryWithTrack>,
) {
  fun toDomain(): PlaylistWithTracks = PlaylistWithTracks(
    playlist = playlist.toDomain(),
    tracks = entries.sortedBy { it.ref.position }.map { it.track.toDomain() }
  )
}
