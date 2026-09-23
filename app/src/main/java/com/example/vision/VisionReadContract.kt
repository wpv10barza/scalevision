package com.example.vision

import org.json.JSONObject

object VisionReadContract {

    enum class Status {
        READ,
        NOT_LEGIBLE,
        CONFLICT
    }

    private val NUMBER_PATTERN =
        Regex("""^[+-]?\d+(?:\.\d+)?$""")

    fun fromRemoteResponse(response: JSONObject): VisionReadResult {
        val remoteStatus = response.optString("status").trim().lowercase()
        val remoteNumber = response.optString("number_text").trim()
        val rawText = response.optString("raw_text")
            .ifBlank { remoteNumber.ifBlank { null } }
        val reason = response.optString("reason").ifBlank { null }

        return when (remoteStatus) {
            "ok", "read" -> {
                if (!NUMBER_PATTERN.matches(remoteNumber)) {
                    conflict(
                        rawText = rawText,
                        reason = "La API declaró una lectura, pero number_text no contiene un número inequívoco."
                    )
                } else {
                    VisionReadResult(
                        status = Status.READ,
                        number = remoteNumber,
                        rawText = rawText,
                        reason = reason
                    )
                }
            }

            "not_readable", "not_legible" -> VisionReadResult(
                status = Status.NOT_LEGIBLE,
                number = null,
                rawText = rawText,
                reason = reason ?: "La lectura no es legible."
            )

            "conflict" -> conflict(
                rawText = rawText,
                reason = reason ?: "La lectura visual es conflictiva."
            )

            else -> conflict(
                rawText = rawText,
                reason = "Respuesta de visión desconocida."
            )
        }
    }

    private fun conflict(rawText: String?, reason: String): VisionReadResult =
        VisionReadResult(
            status = Status.CONFLICT,
            number = null,
            rawText = rawText,
            reason = reason
        )
}

data class VisionReadResult(
    val status: VisionReadContract.Status,
    val number: String?,
    val rawText: String?,
    val reason: String?
)
