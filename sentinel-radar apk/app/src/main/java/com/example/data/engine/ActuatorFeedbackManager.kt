package com.example.data.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

class ActuatorFeedbackManager(private val context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var alarmJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun triggerAlarm(durationMs: Long = 3000) {
        alarmJob?.cancel()
        alarmJob = scope.launch {
            // Trigger haptic vibration pattern
            triggerVibration()

            // Generate synthetic 2.8kHz piezo buzzer sound for duration
            playPiezoTone(durationMs)
        }
    }

    fun stopAlarm() {
        alarmJob?.cancel()
        alarmJob = null
        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
    }

    private fun triggerVibration() {
        try {
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 300, 100, 300, 100, 500)
                    val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                    val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 300, 100, 300, 100, 500), -1)
                }
            }
        } catch (_: Exception) {}
    }

    private fun playPiezoTone(durationMs: Long) {
        try {
            val sampleRate = 44100
            val freqHz = 2800.0 // Classic 2.8 kHz piezo buzzer frequency
            val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val audioData = ShortArray(totalSamples)

            for (i in 0 until totalSamples) {
                val time = i.toDouble() / sampleRate
                // Pulse envelope: 100ms beep on, 50ms off
                val pulseTime = (i % (sampleRate * 0.15)) / sampleRate
                val amp = if (pulseTime < 0.10) 0.75 else 0.0
                val sample = (sin(2.0 * Math.PI * freqHz * time) * amp * Short.MAX_VALUE).toInt()
                audioData[i] = sample.toShort()
            }

            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(audioData.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(audioData, 0, audioData.size)
            audioTrack.play()

            // Wait for duration
            Thread.sleep(durationMs)
            audioTrack.stop()
            audioTrack.release()
        } catch (_: Exception) {}
    }
}
