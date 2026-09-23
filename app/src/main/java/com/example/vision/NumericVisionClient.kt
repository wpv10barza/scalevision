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
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class VisionReadNumberResult(
    val status: String,
    val numberText: String,
    val value: Double?,
    val reason: String,
    val visionSessionId: String?
)

class NumericVisionClient(
    private val baseUrl: String = BuildConfig.VISION_API_BASE_URL
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun readNumber(
        bitmap: Bitmap,
        unit: String
    ): VisionReadNumberResult = withContext(Dispatchers.IO) {
        val imageBase64 = bitmapToBase64(bitmap)

        val payload = JSONObject()
            .put("image_base64", imageBase64)
            .put("mime_type", "image/jpeg")
            .put("unit", unit)
            .toString()

        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/api/vision/read-number")
            .post(
                payload.toRequestBody(
                    "application/json; charset=utf-8".toMediaType()
                )
            )
            .build()

        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                throw IllegalStateException(
                    "Vision backend HTTP ${response.code}: $body"
                )
            }

            val json = JSONObject(body)

            VisionReadNumberResult(
                status = json.optString("status", "not_readable"),
                numberText = json.optString("number_text", ""),
                value = if (json.isNull("value")) {
                    null
                } else {
                    json.optDouble("value").takeIf { !it.isNaN() }
                },
                reason = json.optString("reason", ""),
                visionSessionId = json.optString("vision_session_id", "").ifBlank { null }
            )
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val buffer = ByteArrayOutputStream()
        val scaled = if (bitmap.width > 1600) {
            val ratio = 1600f / bitmap.width.toFloat()
            Bitmap.createScaledBitmap(
                bitmap,
                1600,
                (bitmap.height * ratio).toInt(),
                true
            )
        } else {
            bitmap
        }

        try {
            scaled.compress(Bitmap.CompressFormat.JPEG, 88, buffer)
            return Base64.encodeToString(buffer.toByteArray(), Base64.NO_WRAP)
        } finally {
            if (scaled !== bitmap) scaled.recycle()
        }
    }
}
