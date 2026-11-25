package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppThemeMode
import com.example.data.model.AudioEffectsState
import com.example.data.model.EqualizerPreset
import com.example.data.model.RepeatModeOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.audioDataStore: DataStore<Preferences> by preferencesDataStore(name = "herbarium_audio_prefs")

data class SavedPlaybackSession(
  val queueTrackIds: List<Long> = emptyList(),
  val currentIndex: Int = 0,
  val positionMs: Long = 0L,
  val playlistName: String? = null,
  val shuffleEnabled: Boolean = false,
  val repeatMode: RepeatModeOption = RepeatModeOption.OFF,
)

class AudioPreferencesRepository(private val context: Context) {
  private object Keys {
    val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
    val EQ_PRESET = stringPreferencesKey("eq_preset")
    val EQ_BANDS = stringPreferencesKey("eq_bands")
    val BASS_BOOST = intPreferencesKey("bass_boost")
    val VIRTUALIZER = intPreferencesKey("virtualizer_strength")
    val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
    val GAPLESS_ENABLED = booleanPreferencesKey("gapless_enabled")
    val CROSSFADE_ENABLED = booleanPreferencesKey("crossfade_enabled")
    val CHAMELEON_THEME = booleanPreferencesKey("chameleon_theme")
    val PLAYER_THEME_ID = stringPreferencesKey("player_theme_id")
    val THEME_MODE = stringPreferencesKey("app_theme_mode")
    val MIN_SCAN_DURATION_SEC = intPreferencesKey("min_scan_duration_sec")

    val SESSION_QUEUE_IDS = stringPreferencesKey("session_queue_ids")
    val SESSION_CURRENT_INDEX = intPreferencesKey("session_current_index")
    val SESSION_POSITION_MS = longPreferencesKey("session_position_ms")
    val SESSION_PLAYLIST_NAME = stringPreferencesKey("session_playlist_name")
    val SESSION_SHUFFLE = booleanPreferencesKey("session_shuffle")
    val SESSION_REPEAT = stringPreferencesKey("session_repeat")
    val LAST_AUTO_SCAN_MS = longPreferencesKey("last_auto_scan_ms")
    val LAST_SCAN_THRESHOLD_SEC = intPreferencesKey("last_scan_threshold_sec")
  }

  val preferencesFlow: Flow<AudioEffectsState> = context.audioDataStore.data.map { prefs ->
    val presetName = prefs[Keys.EQ_PRESET] ?: EqualizerPreset.BOTANICAL_NATURAL.name
    val preset = runCatching { EqualizerPreset.valueOf(presetName) }
      .getOrDefault(EqualizerPreset.BOTANICAL_NATURAL)
    val savedBands = prefs[Keys.EQ_BANDS]
      ?.split(",")
      ?.mapNotNull { it.toIntOrNull() }
      ?.takeIf { it.size == 5 }
      ?: preset.bandLevels
    AudioEffectsState(
      equalizerEnabled = prefs[Keys.EQ_ENABLED] ?: true,
      preset = preset,
      bandLevelsMb = savedBands,
      bassBoostStrength = prefs[Keys.BASS_BOOST] ?: preset.recommendedBassBoost,
      virtualizerStrength = prefs[Keys.VIRTUALIZER] ?: preset.recommendedVirtualizer,
      playbackSpeed = prefs[Keys.PLAYBACK_SPEED] ?: 1.0f,
      gaplessEnabled = prefs[Keys.GAPLESS_ENABLED] ?: true,
      crossfadeEnabled = prefs[Keys.CROSSFADE_ENABLED] ?: true,
      chameleonThemeEnabled = prefs[Keys.CHAMELEON_THEME] ?: true,
      playerThemeId = prefs[Keys.PLAYER_THEME_ID] ?: "AUTO",
      themeMode = AppThemeMode.fromName(prefs[Keys.THEME_MODE]),
      minScanDurationSec = (prefs[Keys.MIN_SCAN_DURATION_SEC] ?: 90).coerceIn(0, 600),
    )
  }

  suspend fun getMinScanDurationMs(): Long {
    val secs = context.audioDataStore.data.first()[Keys.MIN_SCAN_DURATION_SEC] ?: 90
    return secs.coerceIn(0, 600) * 1000L
  }

  suspend fun shouldRunAutoScan(cooldownMs: Long = 6L * 3600_000L): Boolean {
    val prefs = context.audioDataStore.data.first()
    val lastScanMs = prefs[Keys.LAST_AUTO_SCAN_MS] ?: 0L
    if (lastScanMs <= 0L) return true
    val currentThreshold = (prefs[Keys.MIN_SCAN_DURATION_SEC] ?: 90).coerceIn(0, 600)
    val lastThreshold = prefs[Keys.LAST_SCAN_THRESHOLD_SEC] ?: -1
    if (currentThreshold != lastThreshold) return true
    val elapsed = System.currentTimeMillis() - lastScanMs
    return elapsed >= cooldownMs || elapsed < 0L
  }

  suspend fun recordAutoScanCompleted(minScanDurationSec: Int) {
    context.audioDataStore.edit { prefs ->
      prefs[Keys.LAST_AUTO_SCAN_MS] = System.currentTimeMillis()
      prefs[Keys.LAST_SCAN_THRESHOLD_SEC] = minScanDurationSec.coerceIn(0, 600)
    }
  }

  suspend fun loadSavedPlaybackSession(): SavedPlaybackSession {
    val prefs = context.audioDataStore.data.first()
    val ids = prefs[Keys.SESSION_QUEUE_IDS]
      ?.split(",")
      ?.mapNotNull { it.toLongOrNull() }
      .orEmpty()
    val repeat = runCatching {
      RepeatModeOption.valueOf(prefs[Keys.SESSION_REPEAT] ?: RepeatModeOption.OFF.name)
    }.getOrDefault(RepeatModeOption.OFF)
    return SavedPlaybackSession(
      queueTrackIds = ids,
      currentIndex = (prefs[Keys.SESSION_CURRENT_INDEX] ?: 0).coerceAtLeast(0),
      positionMs = (prefs[Keys.SESSION_POSITION_MS] ?: 0L).coerceAtLeast(0L),
      playlistName = prefs[Keys.SESSION_PLAYLIST_NAME]?.takeIf { it.isNotBlank() },
      shuffleEnabled = prefs[Keys.SESSION_SHUFFLE] ?: false,
      repeatMode = repeat,
    )
  }

  suspend fun savePlaybackSession(session: SavedPlaybackSession) {
    context.audioDataStore.edit { prefs ->
      if (session.queueTrackIds.isEmpty()) {
        prefs.remove(Keys.SESSION_QUEUE_IDS)
        prefs.remove(Keys.SESSION_CURRENT_INDEX)
        prefs.remove(Keys.SESSION_POSITION_MS)
        prefs.remove(Keys.SESSION_PLAYLIST_NAME)
      } else {
        prefs[Keys.SESSION_QUEUE_IDS] = session.queueTrackIds.joinToString(",")
        prefs[Keys.SESSION_CURRENT_INDEX] = session.currentIndex.coerceAtLeast(0)
        prefs[Keys.SESSION_POSITION_MS] = session.positionMs.coerceAtLeast(0L)
        if (session.playlistName.isNullOrBlank()) {
          prefs.remove(Keys.SESSION_PLAYLIST_NAME)
        } else {
          prefs[Keys.SESSION_PLAYLIST_NAME] = session.playlistName
        }
      }
      prefs[Keys.SESSION_SHUFFLE] = session.shuffleEnabled
      prefs[Keys.SESSION_REPEAT] = session.repeatMode.name
    }
  }

  suspend fun saveEqualizerEnabled(enabled: Boolean) {
    context.audioDataStore.edit { prefs -> prefs[Keys.EQ_ENABLED] = enabled }
  }

  suspend fun savePreset(preset: EqualizerPreset) {
    context.audioDataStore.edit { prefs ->
      prefs[Keys.EQ_PRESET] = preset.name
      if (preset != EqualizerPreset.CUSTOM) {
        prefs[Keys.EQ_BANDS] = preset.bandLevels.joinToString(",")
        prefs[Keys.BASS_BOOST] = preset.recommendedBassBoost
        prefs[Keys.VIRTUALIZER] = preset.recommendedVirtualizer
      }
    }
  }

  suspend fun saveCustomBands(bands: List<Int>) {
    context.audioDataStore.edit { prefs ->
      prefs[Keys.EQ_PRESET] = EqualizerPreset.CUSTOM.name
      prefs[Keys.EQ_BANDS] = bands.joinToString(",")
    }
  }

  suspend fun saveBassBoost(strength: Int) {
    context.audioDataStore.edit { prefs -> prefs[Keys.BASS_BOOST] = strength }
  }

  suspend fun saveVirtualizer(strength: Int) {
    context.audioDataStore.edit { prefs -> prefs[Keys.VIRTUALIZER] = strength }
  }

  suspend fun savePlaybackSpeed(speed: Float) {
    context.audioDataStore.edit { prefs -> prefs[Keys.PLAYBACK_SPEED] = speed }
  }

  suspend fun saveGaplessEnabled(enabled: Boolean) {
    context.audioDataStore.edit { prefs -> prefs[Keys.GAPLESS_ENABLED] = enabled }
  }

  suspend fun saveCrossfadeEnabled(enabled: Boolean) {
    context.audioDataStore.edit { prefs -> prefs[Keys.CROSSFADE_ENABLED] = enabled }
  }

  suspend fun saveChameleonTheme(enabled: Boolean) {
    context.audioDataStore.edit { prefs -> prefs[Keys.CHAMELEON_THEME] = enabled }
  }

  suspend fun savePlayerThemeId(themeId: String) {
    context.audioDataStore.edit { prefs -> prefs[Keys.PLAYER_THEME_ID] = themeId }
  }

  suspend fun saveThemeMode(themeMode: AppThemeMode) {
    context.audioDataStore.edit { prefs -> prefs[Keys.THEME_MODE] = themeMode.name }
  }

  suspend fun saveMinScanDurationSec(seconds: Int) {
    context.audioDataStore.edit { prefs -> prefs[Keys.MIN_SCAN_DURATION_SEC] = seconds.coerceIn(0, 600) }
  }
}
