package com.example.vision

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Optional one-shot auxiliary OCR.
 *
 * The enhanced OmniBot vision path does not depend on this analyzer and does
 * not use its output as the authority for a new reading.
 */
class BitScreenAnalyzer(
    private val onNumberDetected: (Double?, String) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val requestPending = AtomicBoolean(false)
    @Volatile
    private var isProcessing = false

    fun requestSingleRead() {
        requestPending.set(true)
    }

    @OptIn(ExperimentalGetImage::class)
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        if (!requestPending.compareAndSet(true, false) || isProcessing) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            onNumberDetected(null, "")
            return
        }

        isProcessing = true

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                onNumberDetected(
                    parseStrictNumber(visionText.text),
                    visionText.text.take(120)
                )
            }
            .addOnFailureListener { e ->
                Log.e("BitScreenAnalyzer", "Auxiliary OCR failure: ${e.message}")
                onNumberDetected(null, "")
            }
            .addOnCompleteListener {
                isProcessing = false
                imageProxy.close()
            }
    }

    companion object {
        private val NUMBER_PATTERN =
            Regex("""(?<![\d.])[-+]?\d+(?:\.\d+)?(?![\d.])""")

        /**
         * Only accept one explicit numeric token.
         * No character substitution, seven-segment heuristics, rounding,
         * fallback selection, or contextual inference is permitted here.
         */
        fun parseStrictNumber(text: String): Double? {
            val matches = NUMBER_PATTERN
                .findAll(text)
                .map { it.value }
                .toList()
                .distinct()

            if (matches.size != 1) return null
            return matches.single().toDoubleOrNull()
        }
    }
}
