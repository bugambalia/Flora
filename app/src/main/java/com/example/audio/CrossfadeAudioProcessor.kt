package com.example.audio

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * ExoPlayer AudioProcessor that maintains a short circular PCM-16 tail buffer of the
 * active track and performs an equal-power cosine/sine cross-fade overlap mix with
 * incoming track audio whenever a track transition is armed.
 */
@OptIn(UnstableApi::class)
class CrossfadeAudioProcessor : BaseAudioProcessor() {

  private val maxBufferSeconds = 2
  private var tailRing = ShortArray(44100 * 2 * maxBufferSeconds)
  private var tailWriteIdx = 0
  private var tailFilledCount = 0

  private var outgoingSamples = ShortArray(0)
  private var outgoingLength = 0
  private var crossfadeSampleCursor = 0
  private var crossfadeTotalSamples = 0

  @Volatile
  var isEnabled: Boolean = true

  @Volatile
  private var isCrossfading: Boolean = false

  @Volatile
  private var armedDurationMs: Long = 0L

  private val lock = Any()

  fun armCrossfade(durationMs: Long = DEFAULT_CROSSFADE_MS) {
    if (!isEnabled || durationMs <= 0L) return
    synchronized(lock) {
      val sr = inputAudioFormat.sampleRate.takeIf { it > 0 } ?: 44100
      val ch = inputAudioFormat.channelCount.takeIf { it > 0 } ?: 2
      val targetSamples = ((durationMs * sr / 1000L) * ch).toInt()
        .coerceIn(ch, tailRing.size)

      val available = tailFilledCount.coerceAtMost(targetSamples)
      if (available <= 0) {
        armedDurationMs = durationMs
        isCrossfading = false
        return
      }

      if (outgoingSamples.size < available) {
        outgoingSamples = ShortArray(available)
      }
      val startIdx = (tailWriteIdx - available + tailRing.size) % tailRing.size
      for (i in 0 until available) {
        outgoingSamples[i] = tailRing[(startIdx + i) % tailRing.size]
      }
      outgoingLength = available
      crossfadeTotalSamples = available
      crossfadeSampleCursor = 0
      armedDurationMs = 0L
      isCrossfading = true
    }
  }

  /**
   * Pure PCM-16 helper for deterministic mixing verification in tests and direct calls.
   */
  fun processPcm16Array(input: ShortArray): ShortArray {
    val output = ShortArray(input.size)
    synchronized(lock) {
      val ringCap = tailRing.size
      for (i in input.indices) {
        val inSample = input[i]
        tailRing[tailWriteIdx] = inSample
        tailWriteIdx = (tailWriteIdx + 1) % ringCap
        if (tailFilledCount < ringCap) tailFilledCount++

        val outSample: Short = if (isEnabled && isCrossfading && crossfadeTotalSamples > 0) {
          val t = (crossfadeSampleCursor.toFloat() / crossfadeTotalSamples.toFloat()).coerceIn(0f, 1f)
          val inGain = sin(t * (PI / 2.0)).toFloat()
          val outGain = cos(t * (PI / 2.0)).toFloat()
          val tailVal = if (crossfadeSampleCursor < outgoingLength) outgoingSamples[crossfadeSampleCursor] else 0
          crossfadeSampleCursor++
          if (crossfadeSampleCursor >= crossfadeTotalSamples) {
            isCrossfading = false
          }
          (inSample * inGain + tailVal * outGain)
            .roundToInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            .toShort()
        } else {
          inSample
        }
        output[i] = outSample
      }
    }
    return output
  }

  override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
    if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
      return AudioProcessor.AudioFormat.NOT_SET
    }
    synchronized(lock) {
      val requiredCap = (inputAudioFormat.sampleRate.coerceAtLeast(8000) *
        inputAudioFormat.channelCount.coerceAtLeast(1) *
        maxBufferSeconds)
      if (tailRing.size != requiredCap) {
        tailRing = ShortArray(requiredCap)
        tailWriteIdx = 0
        tailFilledCount = 0
      }
    }
    return inputAudioFormat
  }

  override fun queueInput(inputBuffer: ByteBuffer) {
    val remaining = inputBuffer.remaining()
    if (remaining <= 0) return

    val outBuffer = replaceOutputBuffer(remaining)
    outBuffer.order(ByteOrder.LITTLE_ENDIAN)
    val inView = inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
    val sampleCount = remaining / 2

    synchronized(lock) {
      val ringCap = tailRing.size
      for (i in 0 until sampleCount) {
        val inSample = inView.short
        tailRing[tailWriteIdx] = inSample
        tailWriteIdx = (tailWriteIdx + 1) % ringCap
        if (tailFilledCount < ringCap) tailFilledCount++

        val mixed: Short = if (isEnabled && isCrossfading && crossfadeTotalSamples > 0) {
          val t = (crossfadeSampleCursor.toFloat() / crossfadeTotalSamples.toFloat()).coerceIn(0f, 1f)
          val inGain = sin(t * (PI / 2.0)).toFloat()
          val outGain = cos(t * (PI / 2.0)).toFloat()
          val tailVal = if (crossfadeSampleCursor < outgoingLength) outgoingSamples[crossfadeSampleCursor] else 0
          crossfadeSampleCursor++
          if (crossfadeSampleCursor >= crossfadeTotalSamples) {
            isCrossfading = false
          }
          (inSample * inGain + tailVal * outGain)
            .roundToInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            .toShort()
        } else {
          inSample
        }
        outBuffer.putShort(mixed)
      }
    }
    // Consume any odd trailing byte if present
    if (inView.hasRemaining()) {
      outBuffer.put(inView.get())
    }
    outBuffer.flip()
  }

  override fun onFlush() {
    // Preserve armed crossfade across seek/track transition flush so outgoing tail blends smoothly
  }

  override fun onReset() {
    synchronized(lock) {
      tailWriteIdx = 0
      tailFilledCount = 0
      outgoingLength = 0
      crossfadeSampleCursor = 0
      crossfadeTotalSamples = 0
      isCrossfading = false
    }
  }

  companion object {
    const val DEFAULT_CROSSFADE_MS = 1500L
  }
}
