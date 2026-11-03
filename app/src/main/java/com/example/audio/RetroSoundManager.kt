package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Procedural 8-bit retro sound synthesizer and haptic feedback manager.
 * Zero external audio assets required; produces instant, authentic arcade sound effects.
 */
class RetroSoundManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val sampleRate = 22050

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var isSoundEnabled: Boolean = true
    var isHapticsEnabled: Boolean = true

    private fun playTone(
        startFreq: Double,
        endFreq: Double,
        durationMs: Int,
        waveType: WaveType = WaveType.SQUARE,
        volume: Double = 0.5
    ) {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)
                var phase = 0.0

                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val currentFreq = startFreq + (endFreq - startFreq) * progress
                    val phaseIncrement = 2.0 * PI * currentFreq / sampleRate
                    phase += phaseIncrement

                    // Fade out envelope at the end to prevent clicking
                    val envelope = if (progress > 0.8) (1.0 - progress) / 0.2 else 1.0

                    val sampleValue = when (waveType) {
                        WaveType.SQUARE -> if (sin(phase) >= 0) 1.0 else -1.0
                        WaveType.TRIANGLE -> (2.0 / PI) * kotlin.math.asin(sin(phase))
                        WaveType.SINE -> sin(phase)
                        WaveType.NOISE -> (Math.random() * 2.0 - 1.0)
                    }

                    buffer[i] = (sampleValue * envelope * volume * Short.MAX_VALUE).toInt().toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                // Let track play then release
                Thread.sleep(durationMs.toLong() + 50)
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {
                // Ignore audio errors gracefully
            }
        }
    }

    private fun vibrate(durationMs: Long, amplitude: Int = 120) {
        if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val clampedAmp = amplitude.coerceIn(1, 255)
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, clampedAmp))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    fun playJump() {
        playTone(startFreq = 160.0, endFreq = 420.0, durationMs = 120, waveType = WaveType.SQUARE, volume = 0.35)
        vibrate(20, 90)
    }

    fun playWallJump() {
        playTone(startFreq = 380.0, endFreq = 680.0, durationMs = 90, waveType = WaveType.TRIANGLE, volume = 0.45)
        vibrate(25, 140)
    }

    fun playDash() {
        playTone(startFreq = 500.0, endFreq = 150.0, durationMs = 110, waveType = WaveType.NOISE, volume = 0.4)
        vibrate(35, 160)
    }

    fun playCoin() {
        scope.launch {
            if (!isSoundEnabled) return@launch
            playTone(startFreq = 987.77, endFreq = 987.77, durationMs = 70, waveType = WaveType.SINE, volume = 0.5)
            Thread.sleep(70)
            playTone(startFreq = 1318.51, endFreq = 1318.51, durationMs = 160, waveType = WaveType.SINE, volume = 0.5)
        }
        vibrate(18, 100)
    }

    fun playSpring() {
        playTone(startFreq = 220.0, endFreq = 620.0, durationMs = 200, waveType = WaveType.TRIANGLE, volume = 0.5)
        vibrate(40, 200)
    }

    fun playCheckpoint() {
        scope.launch {
            if (!isSoundEnabled) return@launch
            val notes = listOf(523.25, 659.25, 783.99, 1046.50) // C5, E5, G5, C6
            for (freq in notes) {
                playTone(startFreq = freq, endFreq = freq, durationMs = 80, waveType = WaveType.TRIANGLE, volume = 0.4)
                Thread.sleep(75)
            }
        }
        vibrate(60, 180)
    }

    fun playDeath() {
        playTone(startFreq = 350.0, endFreq = 90.0, durationMs = 350, waveType = WaveType.SQUARE, volume = 0.5)
        vibrate(100, 230)
    }

    fun playLevelClear() {
        scope.launch {
            if (!isSoundEnabled) return@launch
            val fanfare = listOf(
                Pair(523.25, 120), // C5
                Pair(659.25, 120), // E5
                Pair(783.99, 120), // G5
                Pair(1046.50, 280), // C6
                Pair(783.99, 100), // G5
                Pair(1046.50, 400) // C6 long
            )
            for ((freq, dur) in fanfare) {
                playTone(startFreq = freq, endFreq = freq, durationMs = dur, waveType = WaveType.SQUARE, volume = 0.45)
                Thread.sleep((dur + 20).toLong())
            }
        }
        vibrate(120, 255)
    }

    enum class WaveType {
        SQUARE, TRIANGLE, SINE, NOISE
    }
}
