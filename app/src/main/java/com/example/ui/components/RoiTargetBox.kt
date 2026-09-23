package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RoiTargetBox(
    isMatch: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanProgress"
    )

    val reticleColor = if (isMatch) Color(0xFF00FF88) else Color(0xFF00E5FF)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Reticle corners & Laser line
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cornerLen = 32.dp.toPx()
            val strokeW = 4.dp.toPx()
            val w = size.width
            val h = size.height
            val pad = 12.dp.toPx()

            // Top-Left
            drawLine(reticleColor, Offset(pad, pad), Offset(pad + cornerLen, pad), strokeW)
            drawLine(reticleColor, Offset(pad, pad), Offset(pad, pad + cornerLen), strokeW)

            // Top-Right
            drawLine(reticleColor, Offset(w - pad, pad), Offset(w - pad - cornerLen, pad), strokeW)
            drawLine(reticleColor, Offset(w - pad, pad), Offset(w - pad, pad + cornerLen), strokeW)

            // Bottom-Left
            drawLine(reticleColor, Offset(pad, h - pad), Offset(pad + cornerLen, h - pad), strokeW)
            drawLine(reticleColor, Offset(pad, h - pad), Offset(pad, h - pad - cornerLen), strokeW)

            // Bottom-Right
            drawLine(reticleColor, Offset(w - pad, h - pad), Offset(w - pad - cornerLen, h - pad), strokeW)
            drawLine(reticleColor, Offset(w - pad, h - pad), Offset(w - pad, h - pad - cornerLen), strokeW)

            // Animated horizontal scan laser line
            if (!isMatch) {
                val lineY = pad + (h - 2 * pad) * scanProgress
                drawLine(
                    color = reticleColor.copy(alpha = 0.65f),
                    start = Offset(pad + 8.dp.toPx(), lineY),
                    end = Offset(w - pad - 8.dp.toPx(), lineY),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // Center guidance badge
        if (isMatch) {
            Box(
                modifier = Modifier
                    .background(Color(0xE6003B1E), RoundedCornerShape(24.dp))
                    .border(2.dp, Color(0xFF00FF88), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completado",
                        tint = Color(0xFF00FF88),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "¡CANTIDAD EXACTA!",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .background(Color(0x990A1128), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "ÁREA DE LECTURA DE BITS",
                    color = Color(0xFF80D8FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
