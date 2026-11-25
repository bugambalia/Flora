package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BotanicalAudioDao {
  @Query("SELECT * FROM tracks ORDER BY isBundled DESC, dateAdded DESC")
  fun observeAllTracks(): Flow<List<TrackEntity>>

  @Query("SELECT COUNT(*) FROM tracks")
  suspend fun getTrackCount(): Int

  @Query("SELECT uri FROM tracks")
  suspend fun getAllTrackUris(): List<String>

  @Query("SELECT * FROM tracks WHERE durationMs > :minDurationMs")
  suspend fun getTracksLongerThan(minDurationMs: Long): List<TrackEntity>

  @Query("SELECT * FROM tracks WHERE uri = :uri LIMIT 1")
  suspend fun getTrackByUri(uri: String): TrackEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTrack(track: TrackEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTracks(tracks: List<TrackEntity>): List<Long>

  @Query("UPDATE tracks SET isFavorite = NOT isFavorite WHERE id = :trackId")
  suspend fun toggleFavorite(trackId: Long)

  @Query("SELECT COUNT(*) FROM tracks WHERE isFavorite = 1")
  suspend fun getFavoriteCount(): Int

  @Query("UPDATE tracks SET playCount = playCount + 1 WHERE id = :trackId")
  suspend fun incrementPlayCount(trackId: Long)

  @Query(
    """
    UPDATE tracks
    SET title = :title,
        artist = :artist,
        album = :album,
        genre = :genre,
        speciesId = :speciesId,
        coverPath = :coverPath
    WHERE id = :trackId
    """
  )
  suspend fun updateTrackMetadata(
    trackId: Long,
    title: String,
    artist: String,
    album: String,
    genre: String,
    speciesId: String,
    coverPath: String?,
  )

  @Query("UPDATE tracks SET lyrics = :lyrics WHERE id = :trackId")
  suspend fun updateTrackLyrics(trackId: Long, lyrics: String?)

  @Query("DELETE FROM tracks WHERE id = :trackId")
  suspend fun deleteTrack(trackId: Long)

  @Transaction
  @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
  fun observePlaylistsWithTracks(): Flow<List<PlaylistWithOrderedEntries>>

  @Query("SELECT * FROM playlists WHERE name = :name LIMIT 1")
  suspend fun getPlaylistByName(name: String): PlaylistEntity?

  @Query("SELECT trackId FROM playlist_track_cross_ref WHERE playlistId = :playlistId")
  suspend fun getTrackIdsInPlaylist(playlistId: Long): List<Long>

  @Query("SELECT COUNT(*) FROM playlists")
  suspend fun getPlaylistCount(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPlaylist(playlist: PlaylistEntity): Long

  @Update
  suspend fun updatePlaylist(playlist: PlaylistEntity)

  @Query("DELETE FROM playlists WHERE id = :playlistId")
  suspend fun deletePlaylistById(playlistId: Long)

  @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_track_cross_ref WHERE playlistId = :playlistId")
  suspend fun getMaxPositionInPlaylist(playlistId: Long): Int

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertPlaylistCrossRef(crossRef: PlaylistTrackCrossRef): Long

  @Query("DELETE FROM playlist_track_cross_ref WHERE playlistId = :playlistId AND trackId = :trackId")
  suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)

  @Query("DELETE FROM playlist_track_cross_ref WHERE playlistId = :playlistId")
  suspend fun clearPlaylistTracks(playlistId: Long)

  @Query("UPDATE playlists SET updatedAt = :timestamp WHERE id = :playlistId")
  suspend fun touchPlaylist(playlistId: Long, timestamp: Long = System.currentTimeMillis())

  @Transaction
  suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
    val nextPos = getMaxPositionInPlaylist(playlistId) + 1
    insertPlaylistCrossRef(PlaylistTrackCrossRef(playlistId, trackId, nextPos))
    touchPlaylist(playlistId)
  }

  @Transaction
  suspend fun replacePlaylistTrackOrder(playlistId: Long, orderedTrackIds: List<Long>) {
    clearPlaylistTracks(playlistId)
    orderedTrackIds.forEachIndexed { index, trackId ->
      insertPlaylistCrossRef(PlaylistTrackCrossRef(playlistId, trackId, index))
    }
    touchPlaylist(playlistId)
  }

  @Query("SELECT * FROM botanical_progress")
  fun observeBotanicalProgress(): Flow<List<BotanicalProgressEntity>>

  @Query("SELECT * FROM botanical_progress WHERE speciesId = :speciesId LIMIT 1")
  suspend fun getBotanicalProgress(speciesId: String): BotanicalProgressEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertBotanicalProgress(progress: BotanicalProgressEntity)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertInitialBotanicalProgress(items: List<BotanicalProgressEntity>)

  @Query("UPDATE botanical_progress SET listenSeconds = listenSeconds + :deltaSeconds WHERE speciesId = :speciesId")
  suspend fun addListenSeconds(speciesId: String, deltaSeconds: Int)
}
