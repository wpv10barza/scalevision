package com.example.vision

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BitmapCaptureHelper {

    fun bitmapToJpegBytes(bitmap: Bitmap, quality: Int = 85): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    /**
     * Generates a realistic digital screen capture of the bit display
     * when camera bitmap is not directly available or in simulator mode.
     */
    fun createSyntheticBitDisplayBitmap(
        targetValue: Double,
        detectedValue: Double,
        unit: String
    ): ByteArray {
        val width = 800
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Dark industrial background
        val bgPaint = Paint().apply { color = Color.parseColor("#0A0E17") }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Glowing display frame
        val frameRect = RectF(50f, 60f, (width - 50).toFloat(), (height - 60).toFloat())
        val framePaint = Paint().apply {
            color = Color.parseColor("#0F2027")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(frameRect, 24f, 24f, framePaint)

        val borderPaint = Paint().apply {
            color = Color.parseColor("#00FF88")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawRoundRect(frameRect, 24f, 24f, borderPaint)

        // Header Title
        val titlePaint = Paint().apply {
            color = Color.parseColor("#80D8FF")
            textSize = 26f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("SCALEVISION • PANTALLA DIGITAL DE BITS", width / 2f, 130f, titlePaint)

        // Big Digital 7-Segment Value
        val valStr = String.format(Locale.US, "%.2f", detectedValue)
        val digitPaint = Paint().apply {
            color = Color.parseColor("#00FF66")
            textSize = 120f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(16f, 0f, 0f, Color.parseColor("#00FF88"))
        }
        canvas.drawText("$valStr $unit", width / 2f, 320f, digitPaint)

        // Target badge
        val targetPaint = Paint().apply {
            color = Color.parseColor("#F1F5F9")
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("META PEDIDA POR VOZ: $targetValue $unit", width / 2f, 420f, targetPaint)

        val statusPaint = Paint().apply {
            color = Color.parseColor("#00FF88")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ESTADO: ¡CANTIDAD EXACTA ALCANZADA!", width / 2f, 470f, statusPaint)

        // Timestamp
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val timePaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Registro: $dateStr", width / 2f, 515f, timePaint)

        return bitmapToJpegBytes(bitmap)
    }
}
