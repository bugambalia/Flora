package com.example.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer

class AudioEffectsController {
  private var equalizer: Equalizer? = null
  private var bassBoost: BassBoost? = null
  private var virtualizer: Virtualizer? = null
  private var currentSessionId: Int = 0
  private var isEqEnabled: Boolean = true
  private var bandLevels: List<Int> = listOf(120, 60, 0, 140, 220)
  private var bassStrength: Int = 240
  private var virtualizerStrength: Int = 180

  fun attachToAudioSession(audioSessionId: Int) {
    if (audioSessionId == 0 || audioSessionId == currentSessionId) return
    release()
    currentSessionId = audioSessionId
    runCatching {
      equalizer = Equalizer(0, audioSessionId).apply {
        enabled = isEqEnabled
      }
      applyBands(bandLevels)
    }
    runCatching {
      bassBoost = BassBoost(0, audioSessionId).apply {
        enabled = isEqEnabled
        if (strengthSupported) {
          setStrength(bassStrength.coerceIn(0, 1000).toShort())
        }
      }
    }
    runCatching {
      virtualizer = Virtualizer(0, audioSessionId).apply {
        enabled = isEqEnabled
        if (strengthSupported) {
          setStrength(virtualizerStrength.coerceIn(0, 1000).toShort())
        }
      }
    }
  }

  fun setEqualizerEnabled(enabled: Boolean) {
    isEqEnabled = enabled
    runCatching { equalizer?.enabled = enabled }
    runCatching { bassBoost?.enabled = enabled }
    runCatching { virtualizer?.enabled = enabled }
  }

  fun applyBands(levelsMb: List<Int>) {
    bandLevels = levelsMb
    val eq = equalizer ?: return
    runCatching {
      val numBands = eq.numberOfBands.toInt()
      val range = eq.bandLevelRange
      val minLvl = range[0].toInt()
      val maxLvl = range[1].toInt()
      for (band in 0 until numBands) {
        val target = levelsMb.getOrElse(band) { 0 }.coerceIn(minLvl, maxLvl)
        eq.setBandLevel(band.toShort(), target.toShort())
      }
    }
  }

  fun applyBassBoost(strength: Int) {
    bassStrength = strength.coerceIn(0, 1000)
    runCatching {
      bassBoost?.let { bb ->
        if (bb.strengthSupported) {
          bb.setStrength(bassStrength.toShort())
        }
      }
    }
  }

  fun applyVirtualizer(strength: Int) {
    virtualizerStrength = strength.coerceIn(0, 1000)
    runCatching {
      virtualizer?.let { virt ->
        if (virt.strengthSupported) {
          virt.setStrength(virtualizerStrength.toShort())
        }
      }
    }
  }

  fun release() {
    runCatching { equalizer?.release() }
    runCatching { bassBoost?.release() }
    runCatching { virtualizer?.release() }
    equalizer = null
    bassBoost = null
    virtualizer = null
    currentSessionId = 0
  }
}
