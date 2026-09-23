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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ScaleConfig
import java.util.Locale

@Composable
fun SettingsDialog(
    currentConfig: ScaleConfig,
    onDismiss: () -> Unit,
    onSave: (ScaleConfig) -> Unit,
    onTestVoicePhrase: (String) -> Unit
) {
    var selectedPhrase by remember { mutableStateOf(currentConfig.voiceAlertPhrase) }
    var soundEnabled by remember { mutableStateOf(currentConfig.soundBeepEnabled) }
    var ttsEnabled by remember { mutableStateOf(currentConfig.ttsEnabled) }
    var hapticsEnabled by remember { mutableStateOf(currentConfig.hapticsEnabled) }
    var holdMode by remember { mutableStateOf(currentConfig.holdMode) }
    var selectedTolerance by remember { mutableDoubleStateOf(currentConfig.tolerance) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Ajustes de Visión & Voz",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                // Firebase badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x2210B981))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Firebase sync",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Firebase Activo",
                        color = Color(0xFF10B981),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Section 1: Voice Alert Phrase
                Text(
                    text = "Notificación de voz al llegar al número exacto:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                val phrases = listOf(
                    "listo" to "¡Listo!",
                    "clic" to "Clic (Sonido/Aviso)",
                    "peso_completo" to "¡Peso completo!"
                )

                phrases.forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedPhrase == key) Color(0x330284C7) else Color(0x11334155))
                            .clickable {
                                selectedPhrase = key
                                onTestVoicePhrase(key)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = if (selectedPhrase == key) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedPhrase == key) Color(0xFF38BDF8) else Color.White
                        )
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Probar sonido",
                            tint = if (selectedPhrase == key) Color(0xFF38BDF8) else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Tolerance
                Text(
                    text = "Tolerancia de lectura del indicador:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        0.0 to "Exacto (0.00)",
                        0.02 to "±0.02",
                        0.05 to "±0.05"
                    ).forEach { (tol, label) ->
                        FilterChip(
                            selected = selectedTolerance == tol,
                            onClick = { selectedTolerance = tol },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: Switches
                Text(
                    text = "Retroalimentación y Comportamiento:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Mantener número (Hold Mode)", fontSize = 13.sp)
                    Switch(checked = holdMode, onCheckedChange = { holdMode = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Sonido \"Clic\" instantáneo", fontSize = 13.sp)
                    Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Síntesis de voz (TTS)", fontSize = 13.sp)
                    Switch(checked = ttsEnabled, onCheckedChange = { ttsEnabled = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Vibración háptica", fontSize = 13.sp)
                    Switch(checked = hapticsEnabled, onCheckedChange = { hapticsEnabled = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = currentConfig.copy(
                        voiceAlertPhrase = selectedPhrase,
                        tolerance = selectedTolerance,
                        holdMode = holdMode,
                        soundBeepEnabled = soundEnabled,
                        ttsEnabled = ttsEnabled,
                        hapticsEnabled = hapticsEnabled,
                        lastUpdated = System.currentTimeMillis()
                    )
                    onSave(updated)
                },
                modifier = Modifier.testTag("save_settings_button")
            ) {
                Text("Guardar Cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
