package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceRecognitionManager(
    private val context: Context,
    private val onQuantityRecognized: (Double) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow("")
    val lastRecognizedText: StateFlow<String> = _lastRecognizedText.asStateFlow()

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w("VoiceRecognizer", "SpeechRecognizer not available on this device")
            return
        }
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    _rmsLevel.value = (rmsdB.coerceIn(0f, 10f) / 10f)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _rmsLevel.value = 0f
                }

                override fun onError(error: Int) {
                    Log.d("VoiceRecognizer", "SpeechRecognizer error: $error")
                    _isListening.value = false
                    _rmsLevel.value = 0f
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    _rmsLevel.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        processTranscript(matches[0])
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    // Low latency partial results parsing
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        _lastRecognizedText.value = text
                        val parsed = extractQuantityFromSpokenText(text)
                        if (parsed != null) {
                            onQuantityRecognized(parsed)
                        }
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    fun startListening() {
        if (speechRecognizer == null) {
            initRecognizer()
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "es-ES")
            // Ultra-low latency partial results
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            Log.e("VoiceRecognizer", "Failed to start listening: ${e.message}")
            _isListening.value = false
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e("VoiceRecognizer", "Failed to stop listening: ${e.message}")
        } finally {
            _isListening.value = false
            _rmsLevel.value = 0f
        }
    }

    private fun processTranscript(text: String) {
        _lastRecognizedText.value = text
        val parsed = extractQuantityFromSpokenText(text)
        if (parsed != null) {
            onQuantityRecognized(parsed)
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("VoiceRecognizer", "Destroy error: ${e.message}")
        }
        speechRecognizer = null
    }

    companion object {
        /**
         * Intelligent extractor for spoken quantities in Spanish or English:
         * Examples handled:
         * "1.5 kg", "1,5", "dos kilos", "uno punto cinco", "tres y medio", "medio kilo",
         * "cinco", "2.0", "fijar en 4 con 2", "cero punto setenta y cinco"
         */
        fun extractQuantityFromSpokenText(input: String): Double? {
            val normalized = input.lowercase(Locale.ROOT).trim()

            // 1. Direct regex match for numbers with decimal point or comma (e.g., 1.5, 1,5, 2.00, 10)
            val directMatch = Regex("""\b(\d+([.,]\d+)?)\b""").find(normalized)
            if (directMatch != null) {
                val numStr = directMatch.groupValues[1].replace(',', '.')
                numStr.toDoubleOrNull()?.let { return it }
            }

            // 2. Common spoken fractions / idioms in Spanish
            if (normalized.contains("medio kilo") || normalized.contains("medio") && !normalized.contains("y medio")) {
                return 0.5
            }

            // Word number dictionary
            val wordMap = mapOf(
                "cero" to 0.0,
                "medio" to 0.5,
                "un" to 1.0,
                "uno" to 1.0,
                "una" to 1.0,
                "dos" to 2.0,
                "tres" to 3.0,
                "cuatro" to 4.0,
                "cinco" to 5.0,
                "seis" to 6.0,
                "siete" to 7.0,
                "ocho" to 8.0,
                "nueve" to 9.0,
                "diez" to 10.0,
                "once" to 11.0,
                "doce" to 12.0,
                "trece" to 13.0,
                "catorce" to 14.0,
                "quince" to 15.0,
                "veinte" to 20.0,
                "veinticinco" to 25.0,
                "treinta" to 30.0,
                "cuarenta" to 40.0,
                "cincuenta" to 50.0
            )

            // Pattern: "<word1> punto <word2>" or "<word1> con <word2>"
            val pointRegex = Regex("""(\w+)\s+(?:punto|coma|con)\s+(\w+)""")
            val pointMatch = pointRegex.find(normalized)
            if (pointMatch != null) {
                val integerPartWord = pointMatch.groupValues[1]
                val decimalPartWord = pointMatch.groupValues[2]

                val intVal = wordMap[integerPartWord] ?: integerPartWord.toDoubleOrNull()
                val decVal = wordMap[decimalPartWord] ?: decimalPartWord.toDoubleOrNull()

                if (intVal != null && decVal != null) {
                    val decStr = decVal.toInt().toString()
                    return "$intVal.$decStr".toDoubleOrNull()
                }
            }

            // Pattern: "<word1> y medio" (e.g. "uno y medio", "dos y medio", "tres y medio")
            val halfRegex = Regex("""(\w+)\s+y\s+medio""")
            val halfMatch = halfRegex.find(normalized)
            if (halfMatch != null) {
                val baseWord = halfMatch.groupValues[1]
                val baseVal = wordMap[baseWord] ?: baseWord.toDoubleOrNull()
                if (baseVal != null) {
                    return baseVal + 0.5
                }
            }

            // Pattern: "<word1> y cuarto" (e.g. "dos y cuarto" -> 2.25)
            val quarterRegex = Regex("""(\w+)\s+y\s+cuarto""")
            val quarterMatch = quarterRegex.find(normalized)
            if (quarterMatch != null) {
                val baseWord = quarterMatch.groupValues[1]
                val baseVal = wordMap[baseWord] ?: baseWord.toDoubleOrNull()
                if (baseVal != null) {
                    return baseVal + 0.25
                }
            }

            // Single word lookup
            for ((word, value) in wordMap) {
                if (Regex("""\b$word\b""").containsMatchIn(normalized)) {
                    return value
                }
            }

            return null
        }
    }
}
