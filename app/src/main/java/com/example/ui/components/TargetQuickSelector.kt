package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun TargetQuickSelector(
    targetQuantity: Double,
    unit: String,
    onSelectTarget: (Double) -> Unit,
    onManualEditRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = listOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xE6111827))
            .border(1.dp, Color(0x2238BDF8), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        // Target Header with Stepper
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CANTIDAD META (INPUT)",
                    color = Color(0xFF38BDF8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${formatDecimal(targetQuantity)} $unit",
                    color = Color(0xFFF8FAFC),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("target_quantity_display")
                )
            }

            // Stepper controls: -0.1, +0.1, Manual Edit
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Minus
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x33334155))
                        .clickable {
                            val newTarget = (targetQuantity - 0.1).coerceAtLeast(0.1)
                            onSelectTarget(Math.round(newTarget * 100.0) / 100.0)
                        }
                        .testTag("target_minus_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Disminuir meta 0.1",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Plus
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x33334155))
                        .clickable {
                            val newTarget = (targetQuantity + 0.1).coerceAtMost(999.0)
                            onSelectTarget(Math.round(newTarget * 100.0) / 100.0)
                        }
                        .testTag("target_plus_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Aumentar meta 0.1",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Edit Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x330284C7))
                        .clickable { onManualEditRequest() }
                        .testTag("target_edit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar meta manualmente",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Preset Chips (horizontal scroll)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                val isSelected = Math.abs(preset - targetQuantity) < 0.001
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B)
                        )
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF38BDF8) else Color(0x33475569),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectTarget(preset) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("preset_chip_${preset}")
                ) {
                    Text(
                        text = "${formatDecimal(preset)} $unit",
                        color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun formatDecimal(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}
