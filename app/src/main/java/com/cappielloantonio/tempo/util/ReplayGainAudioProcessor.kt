package com.cappielloantonio.tempo.util

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.pow

/**
 * An AudioProcessor that applies ReplayGain adjustment directly to
 * PCM samples.
 */
@androidx.media3.common.util.UnstableApi
class ReplayGainAudioProcessor : BaseAudioProcessor() {
    private var targetGainLinear = 1.0f
    private var pendingFlushGainLinear = 1.0f
    private var baselineGainLinear = 1.0f
    private var hasPendingFlushGain = false
    private var activeGainLinear = 1.0f
    private var rampFromGain = 1.0f
    private var rampToGain = 1.0f
    private var rampTotalFrames = 441
    private var rampFramesDone = 0
    private var ramping = false
    private var hasProcessedAnyInput = false
    private var endOfStreamPending = false
    private var configAfterEos = false
    fun setPendingGain(gainDb: Float) {
        pendingFlushGainLinear = dbToLinear(gainDb)
        hasPendingFlushGain = true
    }

    fun setGainImmediate(gainDb: Float) {
        val linear = dbToLinear(gainDb)
        targetGainLinear = linear
        baselineGainLinear = linear
        hasPendingFlushGain = false
        Log.d(TAG, "setGainImmediate: $gainDb dB -> linear=$linear")
    }

    fun clearPendingGain() {
        hasPendingFlushGain = false
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        val enc = inputAudioFormat.encoding
        if (enc != C.ENCODING_PCM_16BIT && enc != C.ENCODING_PCM_FLOAT) {
            return AudioFormat.NOT_SET
        }
        rampTotalFrames = maxOf(1, (inputAudioFormat.sampleRate * RAMP_DURATION_SECONDS).toInt())

        if (endOfStreamPending) {
            configAfterEos = true
        }
        return inputAudioFormat
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onFlush() {
        if (hasPendingFlushGain && hasProcessedAnyInput && endOfStreamPending && configAfterEos) {
            activeGainLinear = pendingFlushGainLinear
            targetGainLinear = pendingFlushGainLinear
            hasPendingFlushGain = false
            ramping = false
            Log.d(TAG, "onFlush: GAPLESS PROMOTION -> active/target=$activeGainLinear")
        } else if (hasPendingFlushGain && hasProcessedAnyInput && endOfStreamPending) {
            activeGainLinear = pendingFlushGainLinear
            targetGainLinear = pendingFlushGainLinear
            baselineGainLinear = pendingFlushGainLinear
            hasPendingFlushGain = false
            ramping = false
            Log.d(
                TAG,
                "onFlush: SAME-FORMAT GAPLESS PROMOTION -> active/target/baseline=$activeGainLinear"
            )
        } else {
            Log.d(
                TAG,
                "onFlush: SEEK/STARTUP branch, restoring to baseline=$baselineGainLinear (was active=$activeGainLinear)"
            )
            activeGainLinear = baselineGainLinear
            targetGainLinear = baselineGainLinear
            ramping = false
            hasPendingFlushGain = false
        }
        endOfStreamPending = false
        hasProcessedAnyInput = false
        configAfterEos = false
    }

    override fun onQueueEndOfStream() {
        endOfStreamPending = true
    }

    override fun onReset() {
        activeGainLinear = baselineGainLinear
        targetGainLinear = baselineGainLinear
        pendingFlushGainLinear = 1.0f
        hasPendingFlushGain = false
        endOfStreamPending = false
        configAfterEos = false
        ramping = false
        hasProcessedAnyInput = false
        Log.d(TAG, "onReset: gain reset to baseline=$baselineGainLinear")
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        hasProcessedAnyInput = true
        val target = targetGainLinear
        if (!ramping && abs(target - activeGainLinear) > 0.0001f) {
            Log.d(TAG, "queueInput: RAMP START active=$activeGainLinear -> target=$target")
            rampFromGain = activeGainLinear
            rampToGain = target
            rampFramesDone = 0
            ramping = true
        }
        if (!ramping && abs(activeGainLinear - 1.0f) < 0.0001f) {
            val output = replaceOutputBuffer(remaining)
            output.put(inputBuffer)
            output.flip()
            return
        }
        val output = replaceOutputBuffer(remaining)
        output.order(ByteOrder.nativeOrder())
        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
            process16Bit(inputBuffer, output)
        } else {
            processFloat(inputBuffer, output)
        }
        output.flip()
    }

    private fun process16Bit(`in`: ByteBuffer, out: ByteBuffer) {
        while (`in`.remaining() >= 2) {
            val gain = advanceGain()
            val sample = `in`.short
            var adjusted = sample * gain
            if (adjusted > Short.MAX_VALUE) adjusted = Short.MAX_VALUE.toFloat()
            else if (adjusted < Short.MIN_VALUE) adjusted = Short.MIN_VALUE.toFloat()
            out.putShort(adjusted.toInt().toShort())
        }
    }

    private fun processFloat(`in`: ByteBuffer, out: ByteBuffer) {
        while (`in`.remaining() >= 4) {
            val gain = advanceGain()
            out.putFloat(`in`.float * gain)
        }
    }

    private fun advanceGain(): Float {
        if (!ramping) return activeGainLinear
        val t = rampFramesDone.toFloat() / rampTotalFrames
        val gain = rampFromGain + (rampToGain - rampFromGain) * t
        rampFramesDone++
        if (rampFramesDone >= rampTotalFrames) {
            activeGainLinear = rampToGain
            ramping = false
            Log.d(TAG, "advanceGain: RAMP COMPLETE -> active=$activeGainLinear")
        }
        return gain
    }

    companion object {
        private const val TAG = "RGAudioProcessor"
        private const val RAMP_DURATION_SECONDS = 0.01f // 10 ms
        private fun dbToLinear(db: Float): Float {
            val constrainedDb = db.coerceIn(-60f, 15f)
            return 10.0f.pow(constrainedDb / 20.0f)
        }
    }
}
