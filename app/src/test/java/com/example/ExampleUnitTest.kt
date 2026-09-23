package com.example

import com.example.audio.VoiceRecognitionManager
import com.example.vision.BitScreenAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testVoiceQuantityParsing_decimalsAndKeywords() {
        assertEquals(1.5, VoiceRecognitionManager.extractQuantityFromSpokenText("1.5 kg") ?: 0.0, 0.001)
        assertEquals(1.5, VoiceRecognitionManager.extractQuantityFromSpokenText("1,5") ?: 0.0, 0.001)
        assertEquals(2.0, VoiceRecognitionManager.extractQuantityFromSpokenText("dos kilos") ?: 0.0, 0.001)
        assertEquals(3.5, VoiceRecognitionManager.extractQuantityFromSpokenText("tres y medio") ?: 0.0, 0.001)
        assertEquals(0.5, VoiceRecognitionManager.extractQuantityFromSpokenText("medio kilo") ?: 0.0, 0.001)
        assertEquals(5.0, VoiceRecognitionManager.extractQuantityFromSpokenText("cinco") ?: 0.0, 0.001)
        assertEquals(4.0, VoiceRecognitionManager.extractQuantityFromSpokenText("fijar en 4") ?: 0.0, 0.001)
    }

    @Test
    fun testBitScreenAnalyzer_characterNormalization() {
        val normalized = BitScreenAnalyzer.normalizeBitDisplayCharacters("NET 2.00 kg")
        val values = BitScreenAnalyzer.parseCandidateValues(normalized)
        assertNotNull(values)
        assertEquals(1, values.size)
        assertEquals(2.00, values[0], 0.001)
    }

    @Test
    fun testBitScreenAnalyzer_commaDecimalNormalization() {
        val normalized = BitScreenAnalyzer.normalizeBitDisplayCharacters("1,50")
        val values = BitScreenAnalyzer.parseCandidateValues(normalized)
        assertEquals(1.50, values[0], 0.001)
    }
}
