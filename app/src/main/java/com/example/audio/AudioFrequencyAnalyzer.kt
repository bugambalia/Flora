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
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

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
 * Computes 12 logarithmic frequency bands using a Hann-windowed Cooley-Tukey FFT with
 * precomputed twiddle/bit-reversal/bin tables to avoid hot-path allocations.
 */
class AudioFrequencyAnalyzer {
  private val _spectrumState = MutableStateFlow(BotanicalAudioSpectrum.Idle)
  val spectrumState: StateFlow<BotanicalAudioSpectrum> = _spectrumState.asStateFlow()

  private val fftSize = 512
  private val log2N = 9
  private val sampleRing = FloatArray(fftSize)
  private var ringIndex = 0
  private var samplesAccumulated = 0

  private val hannWindow = FloatArray(fftSize) { i ->
    (0.5 * (1.0 - cos(2.0 * PI * i / (fftSize - 1)))).toFloat()
  }
  private val bitReversed = IntArray(fftSize) { i ->
    var x = i
    var r = 0
    for (b in 0 until log2N) {
      r = (r shl 1) or (x and 1)
      x = x ushr 1
    }
    r
  }

  // Precomputed twiddle tables per radix-2 stage [0 until log2N]
  private val stageCosTables = Array(log2N) { stage ->
    val halfLen = 1 shl stage
    val len = halfLen shl 1
    val step = -2.0 * PI / len
    FloatArray(halfLen) { k -> cos(step * k).toFloat() }
  }
  private val stageSinTables = Array(log2N) { stage ->
    val halfLen = 1 shl stage
    val len = halfLen shl 1
    val step = -2.0 * PI / len
    FloatArray(halfLen) { k -> sin(step * k).toFloat() }
  }

  private val fftReal = FloatArray(fftSize)
  private val fftImag = FloatArray(fftSize)
  private val smoothedBands = FloatArray(BotanicalAudioSpectrum.BAND_COUNT)

  private var cachedBinSampleRate = -1
  private val bandBinStart = IntArray(BotanicalAudioSpectrum.BAND_COUNT)
  private val bandBinEnd = IntArray(BotanicalAudioSpectrum.BAND_COUNT)
  private val ln10 = ln(10f)
  private val magNormDivisor = fftSize / 4f

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

        val invChannelsScale = 1f / (32768f * channels)
        if (channels == 1) {
          for (frame in 0 until totalFrames) {
            sampleRing[ringIndex] = copy.short * invChannelsScale
            ringIndex = (ringIndex + 1) and (fftSize - 1)
          }
        } else if (channels == 2) {
          for (frame in 0 until totalFrames) {
            val l = copy.short
            val r = copy.short
            sampleRing[ringIndex] = (l + r) * invChannelsScale
            ringIndex = (ringIndex + 1) and (fftSize - 1)
          }
        } else {
          for (frame in 0 until totalFrames) {
            var monoSum = 0
            for (ch in 0 until channels) {
              monoSum += copy.short
            }
            sampleRing[ringIndex] = monoSum * invChannelsScale
            ringIndex = (ringIndex + 1) and (fftSize - 1)
          }
        }
        samplesAccumulated += totalFrames

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
    val invChannelsScale = 1f / (32768f * channels)
    for (f in 0 until frames) {
      var monoSum = 0
      val base = f * channels
      for (ch in 0 until channels) {
        monoSum += samples[base + ch]
      }
      sampleRing[ringIndex] = monoSum * invChannelsScale
      ringIndex = (ringIndex + 1) and (fftSize - 1)
      samplesAccumulated++
    }
    return computeAndPublishSpectrum()
  }

  fun onPlaybackPaused() {
    smoothedBands.fill(0f)
    samplesAccumulated = 0
    _spectrumState.value = BotanicalAudioSpectrum.Idle
  }

  private fun ensureBandBinsForSampleRate(sr: Int) {
    if (cachedBinSampleRate == sr) return
    cachedBinSampleRate = sr
    val nyquist = sr / 2f
    val maxBin = fftSize / 2 - 1
    val minFreq = 45f
    val maxFreq = nyquist.coerceAtMost(12000f).coerceAtLeast(minFreq * 2f)
    val freqRatio = maxFreq / minFreq
    val bandCount = BotanicalAudioSpectrum.BAND_COUNT

    for (b in 0 until bandCount) {
      val t0 = b.toFloat() / bandCount
      val t1 = (b + 1).toFloat() / bandCount
      val f0 = minFreq * freqRatio.pow(t0)
      val f1 = minFreq * freqRatio.pow(t1)

      val bin0 = ((f0 * fftSize) / sr).toInt().coerceIn(1, maxBin)
      val bin1 = ((f1 * fftSize) / sr).toInt().coerceIn(bin0, maxBin)
      bandBinStart[b] = bin0
      bandBinEnd[b] = bin1
    }
  }

  private fun computeAndPublishSpectrum(): BotanicalAudioSpectrum {
    val sr = sampleRateHz
    ensureBandBinsForSampleRate(sr)

    val mask = fftSize - 1
    for (i in 0 until fftSize) {
      val idx = (ringIndex + i) and mask
      val rev = bitReversed[i]
      fftReal[rev] = sampleRing[idx] * hannWindow[i]
      fftImag[rev] = 0f
    }

    fftInPlacePresorted(fftReal, fftImag)

    val bandCount = BotanicalAudioSpectrum.BAND_COUNT
    var bassSum = 0f
    var midSum = 0f
    var trebleSum = 0f
    var totalSum = 0f

    for (b in 0 until bandCount) {
      val bin0 = bandBinStart[b]
      val bin1 = bandBinEnd[b]

      var peakMagSq = 0f
      for (bin in bin0..bin1) {
        val re = fftReal[bin]
        val im = fftImag[bin]
        val magSq = re * re + im * im
        if (magSq > peakMagSq) peakMagSq = magSq
      }

      val peakMag = sqrt(peakMagSq) / magNormDivisor
      val db = 20f * ln(peakMag + 1e-5f) / ln10
      val normalized = ((db + 48f) / 42f).coerceIn(0f, 1f)

      val prev = smoothedBands[b]
      val alpha = if (normalized > prev) 0.58f else 0.24f
      val smoothed = prev + (normalized - prev) * alpha
      smoothedBands[b] = smoothed

      totalSum += smoothed
      when {
        b < 3 -> bassSum += smoothed
        b < 8 -> midSum += smoothed
        else -> trebleSum += smoothed
      }
    }

    val bass = (bassSum / 3f).coerceIn(0f, 1f)
    val mid = (midSum / 5f).coerceIn(0f, 1f)
    val treble = (trebleSum / 4f).coerceIn(0f, 1f)
    val overall = (totalSum / bandCount).coerceIn(0f, 1f)

    val spectrum = BotanicalAudioSpectrum(
      bands = smoothedBands.toList(),
      bass = bass,
      mid = mid,
      treble = treble,
      overall = overall,
      isActive = overall > 0.015f,
    )
    _spectrumState.value = spectrum
    return spectrum
  }

  private fun fftInPlacePresorted(real: FloatArray, imag: FloatArray) {
    val n = real.size
    for (stage in 0 until log2N) {
      val halfLen = 1 shl stage
      val len = halfLen shl 1
      val cosTable = stageCosTables[stage]
      val sinTable = stageSinTables[stage]
      var i = 0
      while (i < n) {
        for (k in 0 until halfLen) {
          val evenIdx = i + k
          val oddIdx = evenIdx + halfLen
          val wReal = cosTable[k]
          val wImag = sinTable[k]
          val oddR = real[oddIdx] * wReal - imag[oddIdx] * wImag
          val oddI = real[oddIdx] * wImag + imag[oddIdx] * wReal
          real[oddIdx] = real[evenIdx] - oddR
          imag[oddIdx] = imag[evenIdx] - oddI
          real[evenIdx] = real[evenIdx] + oddR
          imag[evenIdx] = imag[evenIdx] + oddI
        }
        i += len
      }
    }
  }
}
