package com.example.vision

import android.annotation.SuppressLint
import android.graphics.Rect
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ML Kit remains available as a lightweight auxiliary reader, but it is
 * deliberately one-shot. No OCR is executed until requestSingleRead().
 */
class BitScreenAnalyzer(
    private val onNumberDetected: (Double?, String) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
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
                val (detectedNumber, rawText) = extractBitDisplayNumber(
                    visionText,
                    imageProxy.width,
                    imageProxy.height
                )
                onNumberDetected(detectedNumber, rawText)
            }
            .addOnFailureListener { e ->
                Log.e("BitScreenAnalyzer", "OCR failure: ${e.message}")
                onNumberDetected(null, "")
            }
            .addOnCompleteListener {
                isProcessing = false
                imageProxy.close()
            }
    }

    private fun extractBitDisplayNumber(
        visionText: Text,
        imageWidth: Int,
        imageHeight: Int
    ): Pair<Double?, String> {
        val allBlocks = visionText.textBlocks
        if (allBlocks.isEmpty()) {
            return Pair(null, "")
        }

        val candidates = mutableListOf<CandidateNumber>()

        for (block in allBlocks) {
            for (line in block.lines) {
                val rawLine = line.text
                val cleaned = normalizeBitDisplayCharacters(rawLine)
                val parsed = parseCandidateValues(cleaned)

                for (value in parsed) {
                    val box = line.boundingBox ?: Rect(0, 0, imageWidth, imageHeight)
                    val centerX = box.centerX()
                    val centerY = box.centerY()
                    val distFromCenter = Math.hypot(
                        centerX - imageWidth / 2.0,
                        centerY - imageHeight / 2.0
                    )
                    val boxArea = box.width() * box.height()

                    candidates.add(
                        CandidateNumber(
                            value = value,
                            raw = rawLine,
                            normalized = cleaned,
                            centerDistance = distFromCenter,
                            area = boxArea
                        )
                    )
                }
            }
        }

        if (candidates.isEmpty()) {
            val fullCleaned = normalizeBitDisplayCharacters(visionText.text)
            val fallback = parseCandidateValues(fullCleaned).firstOrNull()
            return Pair(fallback, visionText.text.take(120))
        }

        val best = candidates.minByOrNull {
            it.centerDistance / (it.area.coerceAtLeast(1) + 100)
        } ?: candidates.first()

        return Pair(best.value, best.normalized)
    }

    private data class CandidateNumber(
        val value: Double,
        val raw: String,
        val normalized: String,
        val centerDistance: Double,
        val area: Int
    )

    companion object {
        fun normalizeBitDisplayCharacters(input: String): String {
            var s = input.trim()

            s = s.replace(
                Regex("""(?i)\b(net|tare|tara|zero|cero|hold|stable|est|max|min|gross|bruto|pcs)\b"""),
                " "
            )

            s = s.replace(
                Regex("""(?i)\b(kg|kilo|kilos|gr|g|lbs|lb|oz)\b"""),
                " "
            )

            val sb = StringBuilder()

            for (i in s.indices) {
                val c = s[i]
                val prevIsDigit = i > 0 && s[i - 1].isDigit()
                val nextIsDigit = i < s.length - 1 && s[i + 1].isDigit()
                val isNearbyDigit = prevIsDigit || nextIsDigit

                val normalizedChar = when {
                    (c == 'O' || c == 'o' || c == 'D') && isNearbyDigit -> '0'
                    (c == 'I' || c == 'l' || c == '|' || c == '!') && isNearbyDigit -> '1'
                    (c == 'Z' || c == 'z') && isNearbyDigit -> '2'
                    (c == 'S' || c == 's') && isNearbyDigit -> '5'
                    (c == 'B') && isNearbyDigit -> '8'
                    (c == 'q' || c == 'g') && isNearbyDigit -> '9'
                    c == ',' -> '.'
                    c == ':' && isNearbyDigit -> '.'
                    else -> c
                }

                sb.append(normalizedChar)
            }

            return sb.toString()
        }

        fun parseCandidateValues(text: String): List<Double> {
            val list = mutableListOf<Double>()
            val regex = Regex("""(?:\b|(?<=[^\d.]))(\d{1,4}(?:\.\d{1,3})?)(?:\b|(?=[^\d.]))""")

            for (match in regex.findAll(text)) {
                val token = match.groupValues[1]
                val d = token.toDoubleOrNull()

                if (d != null && d >= 0.0 && d < 9999.0) {
                    list.add(d)
                }
            }

            return list
        }
    }
}
