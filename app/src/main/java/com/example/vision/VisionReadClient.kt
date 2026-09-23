package com.example.vision

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class VisionReadResult(
    val status: String,
    val number: Double?,
    val rawText: String,
    val reason: String
)

class VisionReadClient(
    private val backendUrl: String = BuildConfig.VISION_BACKEND_URL,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    suspend fun readNumber(
        bitmap: Bitmap,
        localOcrText: String,
        prompt: String = "Lee el valor numérico principal visible en la pantalla o indicador."
    ): VisionReadResult = withContext(Dispatchers.IO) {
        val imageBytes = bitmapToVisionJpeg(bitmap)
        val base = backendUrl.trimEnd('/')

        val capturePayload = JSONObject()
            .put(
                "image_base64",
                Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            )
            .put("mime_type", "image/jpeg")
            .put("local_ocr_text", localOcrText.take(500))

        val captureResponse = postJson(
            "${base}/api/vision/captures",
            capturePayload
        )

        val captureId = captureResponse.getString("capture_id")

        val agentPayload = JSONObject()
            .put("capture_id", captureId)
            .put("prompt", prompt)

        val result = postJson("${base}/api/vision/agent", agentPayload)
        val tool = result.optJSONObject("tool_result")
            ?: throw IllegalStateException("El agente no devolvió tool_result.")

        VisionReadResult(
            status = tool.optString("status", "NOT_LEGIBLE"),
            number = if (tool.isNull("number")) {
                null
            } else {
                tool.optString("number").toDoubleOrNull()
            },
            rawText = tool.optString("raw_text", ""),
            reason = tool.optString("reason", "")
        )
    }

    private fun postJson(url: String, payload: JSONObject): JSONObject {
        val body = payload.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        httpClient.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val message = runCatching {
                    JSONObject(text).optString("error").ifBlank { text }
                }.getOrDefault(text)

                throw IllegalStateException(
                    "Vision backend ${response.code}: $message"
                )
            }

            return JSONObject(text)
        }
    }

    private fun bitmapToVisionJpeg(bitmap: Bitmap): ByteArray {
        val maxDimension = 1280
        val scale = minOf(
            1f,
            maxDimension.toFloat() / bitmap.width.toFloat(),
            maxDimension.toFloat() / bitmap.height.toFloat()
        )

        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }

        return try {
            BitmapCaptureHelper.bitmapToJpegBytes(scaled, quality = 82)
        } finally {
            if (scaled !== bitmap) scaled.recycle()
        }
    }
}
