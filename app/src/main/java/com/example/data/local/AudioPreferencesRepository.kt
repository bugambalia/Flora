package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AudioEffectsState
import com.example.data.model.EqualizerPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.audioDataStore: DataStore<Preferences> by preferencesDataStore(name = "herbarium_audio_prefs")

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
    )
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
}
