package com.example.model

data class ScaleConfig(
    val targetQuantity: Double = 2.0,
    val unit: String = "kg",
    val voiceAlertPhrase: String = "listo", // "listo", "clic", "peso_completo"
    val tolerance: Double = 0.0, // 0.0 means exact bit match
    val holdMode: Boolean = true,
    val soundBeepEnabled: Boolean = true,
    val ttsEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val minStabilityFrames: Int = 2,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "targetQuantity" to targetQuantity,
            "unit" to unit,
            "voiceAlertPhrase" to voiceAlertPhrase,
            "tolerance" to tolerance,
            "holdMode" to holdMode,
            "soundBeepEnabled" to soundBeepEnabled,
            "ttsEnabled" to ttsEnabled,
            "hapticsEnabled" to hapticsEnabled,
            "minStabilityFrames" to minStabilityFrames,
            "lastUpdated" to lastUpdated
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): ScaleConfig {
            return ScaleConfig(
                targetQuantity = (map["targetQuantity"] as? Number)?.toDouble() ?: 2.0,
                unit = map["unit"] as? String ?: "kg",
                voiceAlertPhrase = map["voiceAlertPhrase"] as? String ?: "listo",
                tolerance = (map["tolerance"] as? Number)?.toDouble() ?: 0.0,
                holdMode = map["holdMode"] as? Boolean ?: true,
                soundBeepEnabled = map["soundBeepEnabled"] as? Boolean ?: true,
                ttsEnabled = map["ttsEnabled"] as? Boolean ?: true,
                hapticsEnabled = map["hapticsEnabled"] as? Boolean ?: true,
                minStabilityFrames = (map["minStabilityFrames"] as? Number)?.toInt() ?: 2,
                lastUpdated = (map["lastUpdated"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

data class DetectionState(
    val detectedValue: Double? = null,
    val rawText: String = "",
    val isExactMatch: Boolean = false,
    val isToleranceMatch: Boolean = false,
    val diff: Double? = null,
    val consecutiveCount: Int = 0,
    val lastDetectionTimestamp: Long = 0L
)

data class WeightHistoryLog(
    val id: String = "",
    val targetQuantity: Double = 0.0,
    val reachedQuantity: Double = 0.0,
    val unit: String = "kg",
    val timestamp: Long = System.currentTimeMillis()
)
