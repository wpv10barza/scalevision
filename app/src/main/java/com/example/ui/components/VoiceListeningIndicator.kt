package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VoiceListeningIndicator(
    isListening: Boolean,
    rmsLevel: Float,
    lastSpokenText: String,
    targetQuantity: Double,
    unit: String,
    onToggleListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micPulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xE6101827))
            .border(1.dp, if (isListening) Color(0xFFFF5252) else Color(0x3338BDF8), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated Mic Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (isListening) Color(0x33FF5252) else Color(0x220284C7))
                    .clickable { onToggleListening() }
                    .testTag("voice_mic_button")
            ) {
                if (isListening) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .border(2.dp, Color(0xFFFF5252), CircleShape)
                    )
                }

                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.Mic,
                    contentDescription = if (isListening) "Detener micrófono" else "Iniciar comando de voz",
                    tint = if (isListening) Color(0xFFFF5252) else Color(0xFF38BDF8),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text & Status
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isListening) "ESCUCHANDO COMANDO..." else "COMANDO DE VOZ (BAJA LATENCIA)",
                        color = if (isListening) Color(0xFFFF5252) else Color(0xFF38BDF8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // Audio level waveform bars
                    if (isListening) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(18.dp)
                        ) {
                            val barCount = 5
                            for (i in 0 until barCount) {
                                val dynamicHeight = (8 + (rmsLevel * (16 + i * 4)) % 16).dp
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(dynamicHeight)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFFFF5252))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (isListening) {
                    Text(
                        text = if (lastSpokenText.isNotBlank()) "\"$lastSpokenText\"" else "Di una cantidad (ej: \"1.5\", \"2.0 kg\", \"tres y medio\")...",
                        color = if (lastSpokenText.isNotBlank()) Color(0xFFF1F5F9) else Color(0xFFCBD5E1),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Toca para dictar cantidad deseada",
                        color = Color(0xFFCBD5E1),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
