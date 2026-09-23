package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun BitScreenSimulator(
    simulatedValue: Double,
    targetValue: Double,
    unit: String,
    onValueChange: (Double) -> Unit,
    onReachTarget: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xF0030712))
            .border(2.dp, Color(0xFF10B981), RoundedCornerShape(20.dp))
            .padding(16.dp)
            .testTag("bit_screen_simulator")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SIMULADOR DE PANTALLA DIGITAL (7-SEG)",
                color = Color(0xFF10B981),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x33EF4444))
                    .clickable { onClose() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Ocultar",
                    color = Color(0xFFF87171),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Virtual 7-Segment Screen Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
                    )
                )
                .border(1.dp, Color(0xFF00FF88), RoundedCornerShape(12.dp))
                .padding(vertical = 12.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "BALANZA DIGITAL • INDICADOR DE BITS",
                    color = Color(0xFF80D8FF),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = String.format(Locale.US, "%.2f", simulatedValue),
                        color = Color(0xFF00FF66),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 3.sp
                    )

                    Text(
                        text = " $unit",
                        color = Color(0xFF00FF66).copy(alpha = 0.8f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Simulator controls: Quick Steppers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = {
                    val next = (simulatedValue - 0.1).coerceAtLeast(0.0)
                    onValueChange(Math.round(next * 100.0) / 100.0)
                },
                modifier = Modifier.weight(1f).testTag("sim_minus_100g")
            ) {
                Text("-0.1")
            }

            FilledTonalButton(
                onClick = {
                    val next = (simulatedValue + 0.1).coerceAtMost(999.0)
                    onValueChange(Math.round(next * 100.0) / 100.0)
                },
                modifier = Modifier.weight(1f).testTag("sim_plus_100g")
            ) {
                Text("+0.1")
            }

            FilledTonalButton(
                onClick = {
                    val next = (simulatedValue + 0.5).coerceAtMost(999.0)
                    onValueChange(Math.round(next * 100.0) / 100.0)
                },
                modifier = Modifier.weight(1f).testTag("sim_plus_500g")
            ) {
                Text("+0.5")
            }

            Button(
                onClick = onReachTarget,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                modifier = Modifier.weight(1.4f).testTag("sim_reach_target")
            ) {
                Text("Meta Exacta", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedButton(
            onClick = { onValueChange(0.0) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset a 0.00 $unit (Tara)")
        }
    }
}
