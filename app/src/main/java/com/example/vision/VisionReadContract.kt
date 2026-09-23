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

    fun fromRemoteResponse(response: JSONObject): VisionReadResult =
        fromRemoteValues(
            status = response.optString("status"),
            numberText = response.optString("number_text"),
            rawText = response.optString("raw_text"),
            reason = response.optString("reason")
        )

    fun fromRemoteValues(
        status: String,
        numberText: String?,
        rawText: String?,
        reason: String?
    ): VisionReadResult {
        val remoteStatus = status.trim().lowercase()
        val remoteNumber = numberText?.trim().orEmpty()
        val cleanRawText = rawText?.trim()?.ifBlank {
            remoteNumber.ifBlank { null }
        }
        val cleanReason = reason?.trim()?.ifBlank { null }

        return when (remoteStatus) {
            "ok", "read" -> {
                if (!NUMBER_PATTERN.matches(remoteNumber)) {
                    conflict(
                        rawText = cleanRawText,
                        reason = "La API declaró una lectura, pero number_text no contiene un número inequívoco."
                    )
                } else {
                    VisionReadResult(
                        status = Status.READ,
                        number = remoteNumber,
                        rawText = cleanRawText,
                        reason = cleanReason
                    )
                }
            }

            "not_readable", "not_legible" -> VisionReadResult(
                status = Status.NOT_LEGIBLE,
                number = null,
                rawText = cleanRawText,
                reason = cleanReason ?: "La lectura no es legible."
            )

            "conflict" -> conflict(
                rawText = cleanRawText,
                reason = cleanReason ?: "La lectura visual es conflictiva."
            )

            else -> conflict(
                rawText = cleanRawText,
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
