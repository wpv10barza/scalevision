package com.example.vision

import android.graphics.Bitmap
import android.net.Uri
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
        unit: String
    ): VisionReadResult = withContext(Dispatchers.IO) {
        val base = validateBackendUrl(backendUrl).trimEnd('/')
        val imageBytes = bitmapToVisionJpeg(bitmap)

        val payload = JSONObject()
            .put(
                "image_base64",
                Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            )
            .put("mime_type", "image/jpeg")
            .put("unit", unit)

        val response = postJson(
            "$base/api/vision/read-number",
            payload
        )

        VisionReadContract.fromRemoteResponse(response)
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
                throw IllegalStateException(
                    "Vision API HTTP ${response.code}: " +
                        runCatching {
                            val json = JSONObject(text)
                            json.optString("reason")
                                .ifBlank { json.optString("error") }
                                .ifBlank { text }
                        }.getOrDefault(text)
                )
            }

            if (text.isBlank()) {
                throw IllegalStateException("La API de visión devolvió una respuesta vacía.")
            }

            return JSONObject(text)
        }
    }

    private fun bitmapToVisionJpeg(bitmap: Bitmap): ByteArray {
        val maxDimension = 1536
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
            BitmapCaptureHelper.bitmapToJpegBytes(scaled, quality = 90)
        } finally {
            if (scaled !== bitmap) {
                scaled.recycle()
            }
        }
    }

    private fun validateBackendUrl(rawUrl: String): String {
        val normalized = rawUrl.trim()

        if (normalized.isBlank()) {
            throw IllegalStateException(
                "VISION_BACKEND_URL no está configurada."
            )
        }

        val scheme = Uri.parse(normalized).scheme?.lowercase()

        if (!BuildConfig.DEBUG && scheme != "https") {
            throw IllegalStateException(
                "En producción la API de visión debe utilizar HTTPS."
            )
        }

        if (scheme != "http" && scheme != "https") {
            throw IllegalStateException(
                "VISION_BACKEND_URL debe usar http:// o https://."
            )
        }

        return normalized
    }
}
