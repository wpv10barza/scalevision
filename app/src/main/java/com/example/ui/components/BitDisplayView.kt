package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun BitDisplayView(
    currentValue: Double?,
    targetValue: Double,
    unit: String,
    isMatch: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isMatch) 1.04f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isMatch) Color(0xFF00E676) else Color(0xFF00E5FF).copy(alpha = 0.5f),
        label = "borderColor"
    )

    val bgGradient = if (isMatch) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF00381C),
                Color(0xFF001F0F)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0D1B2A),
                Color(0xFF050B14)
            )
        )
    }

    Box(
        modifier = modifier
            .testTag("bit_display_view")
            .scale(pulseScale)
            .shadow(
                elevation = if (isMatch) 16.dp else 6.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = if (isMatch) Color(0xFF00E676) else Color(0xFF00E5FF)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(bgGradient)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PANTALLA DE BITS DETECTADA",
                    color = if (isMatch) Color(0xFF00E676) else Color(0xFF90CAF9),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                Text(
                    text = if (isMatch) "● EXACTO" else "EN LECTURA",
                    color = if (isMatch) Color(0xFF00E676) else Color(0xFFFFB300),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Big 7-Segment / LED styled Readout
            Row(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                val displayText = if (currentValue != null) {
                    formatDecimal(currentValue)
                } else {
                    "--.--"
                }

                Text(
                    text = displayText,
                    color = if (isMatch) Color(0xFF00FF88) else Color(0xFF00F0FF),
                    fontSize = 58.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    modifier = Modifier.testTag("current_detected_number")
                )

                Text(
                    text = " $unit",
                    color = if (isMatch) Color(0xFF00FF88).copy(alpha = 0.8f) else Color(0xFF00F0FF).copy(alpha = 0.7f),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Delta difference indicator
            if (currentValue != null) {
                val diff = currentValue - targetValue
                val absDiff = Math.abs(diff)
                val diffText = when {
                    absDiff < 0.005 -> "¡CANTIDAD EXACTA ALCANZADA!"
                    diff < 0 -> "Faltan ${formatDecimal(absDiff)} $unit"
                    else -> "Exceso de +${formatDecimal(absDiff)} $unit"
                }

                Text(
                    text = diffText,
                    color = if (isMatch) Color(0xFF00FF88) else Color(0xFFE2E8F0),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "Apunta la cámara al visor digital de la balanza",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun formatDecimal(value: Double): String {
    return String.format(Locale.US, "%.2f", value)
}
