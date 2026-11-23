package com.example.audio

import androidx.annotation.OptIn
import androidx.compose.runtime.Immutable
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin

@Immutable
data class BotanicalAudioSpectrum(
  val bands: List<Float> = List(BAND_COUNT) { 0f },
  val bass: Float = 0f,
  val mid: Float = 0f,
  val treble: Float = 0f,
  val overall: Float = 0f,
  val isActive: Boolean = false,
) {
  companion object {
    const val BAND_COUNT = 12
    val Idle = BotanicalAudioSpectrum()
  }
}

/**
 * Real-time PCM audio frequency analyzer wired directly into ExoPlayer's TeeAudioProcessor.
 * Computes 12 logarithmic frequency bands using a Hann-windowed Cooley-Tukey FFT without
 * requiring RECORD_AUDIO permissions.
 */
class AudioFrequencyAnalyzer {
  private val _spectrumState = MutableStateFlow(BotanicalAudioSpectrum.Idle)
  val spectrumState: StateFlow<BotanicalAudioSpectrum> = _spectrumState.asStateFlow()

  private val fftSize = 512
  private val sampleRing = FloatArray(fftSize)
  private var ringIndex = 0
  private var samplesAccumulated = 0

  private val hannWindow = FloatArray(fftSize) { i ->
    (0.5 * (1.0 - cos(2.0 * PI * i / (fftSize - 1)))).toFloat()
  }
  private val fftReal = FloatArray(fftSize)
  private val fftImag = FloatArray(fftSize)
  private val smoothedBands = FloatArray(BotanicalAudioSpectrum.BAND_COUNT)

  @Volatile
  private var sampleRateHz: Int = 44100

  @Volatile
  private var channelCount: Int = 2

  @Volatile
  private var encoding: Int = C.ENCODING_PCM_16BIT

  @Volatile
  private var lastEmitTimeMs: Long = 0L

  @Volatile
  private var visualizationEnabled: Boolean = false

  fun setVisualizationEnabled(enabled: Boolean) {
    if (visualizationEnabled == enabled) return
    visualizationEnabled = enabled
    if (!enabled) {
      onPlaybackPaused()
    }
  }

  fun isVisualizationEnabled(): Boolean = visualizationEnabled

  @OptIn(UnstableApi::class)
  val audioBufferSink: TeeAudioProcessor.AudioBufferSink =
    object : TeeAudioProcessor.AudioBufferSink {
      override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        this@AudioFrequencyAnalyzer.sampleRateHz = sampleRateHz.coerceAtLeast(8000)
        this@AudioFrequencyAnalyzer.channelCount = channelCount.coerceAtLeast(1)
        this@AudioFrequencyAnalyzer.encoding = encoding
        ringIndex = 0
        samplesAccumulated = 0
      }

      override fun handleBuffer(buffer: ByteBuffer) {
        if (!visualizationEnabled || encoding != C.ENCODING_PCM_16BIT) return
        val now = System.currentTimeMillis()
        val copy = buffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
        val channels = channelCount.coerceAtLeast(1)
        val frameBytes = channels * 2
        val totalFrames = copy.remaining() / frameBytes
        if (totalFrames <= 0) return

        for (frame in 0 until totalFrames) {
          var monoSum = 0f
          for (ch in 0 until channels) {
            if (copy.remaining() >= 2) {
              monoSum += copy.short / 32768f
            }
          }
          sampleRing[ringIndex] = monoSum / channels
          ringIndex = (ringIndex + 1) % fftSize
          samplesAccumulated++
        }

        if (samplesAccumulated >= fftSize / 2 && now - lastEmitTimeMs >= 24L) {
          lastEmitTimeMs = now
          computeAndPublishSpectrum()
        }
      }
    }

  /**
   * Direct PCM-16 processing entry point (also used for deterministic unit tests).
   */
  fun processPcm16(
    samples: ShortArray,
    sampleRateHz: Int = 44100,
    channelCount: Int = 1,
  ): BotanicalAudioSpectrum {
    this.sampleRateHz = sampleRateHz.coerceAtLeast(8000)
    this.channelCount = channelCount.coerceAtLeast(1)
    val channels = this.channelCount
    val frames = samples.size / channels
    for (f in 0 until frames) {
      var monoSum = 0f
      val base = f * channels
      for (ch in 0 until channels) {
        monoSum += samples[base + ch] / 32768f
      }
      sampleRing[ringIndex] = monoSum / channels
      ringIndex = (ringIndex + 1) % fftSize
      samplesAccumulated++
    }
    return computeAndPublishSpectrum()
  }

  fun onPlaybackPaused() {
    smoothedBands.fill(0f)
    samplesAccumulated = 0
    _spectrumState.value = BotanicalAudioSpectrum.Idle
  }

  private fun computeAndPublishSpectrum(): BotanicalAudioSpectrum {
    for (i in 0 until fftSize) {
      val idx = (ringIndex + i) % fftSize
      fftReal[i] = sampleRing[idx] * hannWindow[i]
      fftImag[i] = 0f
    }

    fftInPlace(fftReal, fftImag)

    val nyquist = sampleRateHz / 2f
    val maxBin = fftSize / 2 - 1
    val minFreq = 45f
    val maxFreq = nyquist.coerceAtMost(12000f).coerceAtLeast(minFreq * 2f)
    val bandCount = BotanicalAudioSpectrum.BAND_COUNT

    for (b in 0 until bandCount) {
      val t0 = b.toFloat() / bandCount
      val t1 = (b + 1).toFloat() / bandCount
      val f0 = minFreq * (maxFreq / minFreq).pow(t0)
      val f1 = minFreq * (maxFreq / minFreq).pow(t1)

      val bin0 = ((f0 * fftSize) / sampleRateHz).toInt().coerceIn(1, maxBin)
      val bin1 = ((f1 * fftSize) / sampleRateHz).toInt().coerceIn(bin0, maxBin)

      var peakMag = 0f
      for (bin in bin0..bin1) {
        val mag = hypot(fftReal[bin], fftImag[bin]) / (fftSize / 4f)
        if (mag > peakMag) peakMag = mag
      }

      // Convert magnitude to perceptual normalized [0..1] energy
      val db = 20f * ln((peakMag + 1e-5f).toDouble()).toFloat() / ln(10.0).toFloat()
      val normalized = ((db + 48f) / 42f).coerceIn(0f, 1f)

      val prev = smoothedBands[b]
      val alpha = if (normalized > prev) 0.58f else 0.24f
      smoothedBands[b] = prev + (normalized - prev) * alpha
    }

    val bandsList = smoothedBands.toList()
    val bass = (bandsList.take(3).average().toFloat()).coerceIn(0f, 1f)
    val mid = (bandsList.subList(3, 8).average().toFloat()).coerceIn(0f, 1f)
    val treble = (bandsList.subList(8, 12).average().toFloat()).coerceIn(0f, 1f)
    val overall = (bandsList.average().toFloat()).coerceIn(0f, 1f)

    val spectrum = BotanicalAudioSpectrum(
      bands = bandsList,
      bass = bass,
      mid = mid,
      treble = treble,
      overall = overall,
      isActive = overall > 0.015f,
    )
    _spectrumState.value = spectrum
    return spectrum
  }

  private fun fftInPlace(real: FloatArray, imag: FloatArray) {
    val n = real.size
    var j = 0
    for (i in 1 until n) {
      var bit = n shr 1
      while (j and bit != 0) {
        j = j xor bit
        bit = bit shr 1
      }
      j = j xor bit
      if (i < j) {
        val tr = real[i]
        real[i] = real[j]
        real[j] = tr
        val ti = imag[i]
        imag[i] = imag[j]
        imag[j] = ti
      }
    }

    var len = 2
    while (len <= n) {
      val halfLen = len / 2
      val angle = -2.0 * PI / len
      val wLenReal = cos(angle).toFloat()
      val wLenImag = sin(angle).toFloat()
      var i = 0
      while (i < n) {
        var wReal = 1f
        var wImag = 0f
        for (k in 0 until halfLen) {
          val evenIdx = i + k
          val oddIdx = evenIdx + halfLen
          val oddR = real[oddIdx] * wReal - imag[oddIdx] * wImag
          val oddI = real[oddIdx] * wImag + imag[oddIdx] * wReal
          real[oddIdx] = real[evenIdx] - oddR
          imag[oddIdx] = imag[evenIdx] - oddI
          real[evenIdx] = real[evenIdx] + oddR
          imag[evenIdx] = imag[evenIdx] + oddI

          val nextWReal = wReal * wLenReal - wImag * wLenImag
          wImag = wReal * wLenImag + wImag * wLenReal
          wReal = nextWReal
        }
        i += len
      }
      len = len shl 1
    }
  }
}
