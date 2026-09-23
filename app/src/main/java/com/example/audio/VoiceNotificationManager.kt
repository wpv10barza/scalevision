package com.example.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class VoiceNotificationManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            textToSpeech = TextToSpeech(context.applicationContext, this)
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        } catch (e: Exception) {
            Log.e("VoiceNotifManager", "Init error: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale("es", "ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to default locale
                textToSpeech?.language = Locale.getDefault()
            }
            textToSpeech?.setSpeechRate(1.1f) // Slightly faster for low-latency response
            textToSpeech?.setPitch(1.0f)
            isTtsReady = true
        } else {
            Log.e("VoiceNotifManager", "TTS initialization failed status=$status")
        }
    }

    fun playClickSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
        } catch (e: Exception) {
            Log.e("VoiceNotifManager", "Tone error: ${e.message}")
        }
    }

    fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val effect = VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 160), intArrayOf(0, 255, 0, 255), -1)
                vibratorManager?.vibrate(CombinedVibration.createParallel(effect))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 160), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(250)
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceNotifManager", "Vibrate error: ${e.message}")
        }
    }

    fun notifyTargetReached(
        phraseMode: String,
        targetValue: Double,
        unit: String,
        enableSound: Boolean,
        enableTts: Boolean,
        enableHaptics: Boolean
    ) {
        if (enableSound) {
            playClickSound()
        }
        if (enableHaptics) {
            triggerVibration()
        }

        if (!enableTts) return

        val textToSpeak = when (phraseMode.lowercase()) {
            "clic" -> "Clic. Peso alcanzado."
            "listo" -> "¡Listo! ${formatNumber(targetValue)} $unit"
            "peso_completo", "peso completo" -> "¡Peso completo! ${formatNumber(targetValue)} $unit"
            else -> "¡Listo! $phraseMode"
        }

        if (isTtsReady) {
            textToSpeech?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "notif_${System.currentTimeMillis()}")
        }
    }

    fun speakPrompt(text: String) {
        if (isTtsReady) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_ADD, null, "prompt_${System.currentTimeMillis()}")
        }
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", value).replace('.', ',')
        }
    }

    fun shutdown() {
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            toneGenerator?.release()
        } catch (e: Exception) {
            Log.e("VoiceNotifManager", "Shutdown error: ${e.message}")
        }
    }
}
