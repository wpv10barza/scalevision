package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BitDisplayView
import com.example.ui.components.BitScreenSimulator
import com.example.ui.components.DriveSyncDialog
import com.example.ui.components.ManualInputDialog
import com.example.ui.components.RoiTargetBox
import com.example.ui.components.SettingsDialog
import com.example.ui.components.TargetQuickSelector
import com.example.ui.components.VoiceListeningIndicator
import com.example.vision.CameraPreviewView
import androidx.compose.material.icons.filled.CloudUpload

@Composable
fun ScaleVisionScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: false
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: false
    }

    // Google Drive OAuth sign-in launcher
    val driveAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleDriveSignInResult(result.data)
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasAudioPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    val config by viewModel.config.collectAsStateWithLifecycle()
    val currentNumber by viewModel.currentDetectedNumber.collectAsStateWithLifecycle()
    val isMatch by viewModel.isMatch.collectAsStateWithLifecycle()
    val isReading by viewModel.isReading.collectAsStateWithLifecycle()
    val torchEnabled by viewModel.torchEnabled.collectAsStateWithLifecycle()
    val zoomRatio by viewModel.zoomRatio.collectAsStateWithLifecycle()
    val isSimulatorVisible by viewModel.isSimulatorVisible.collectAsStateWithLifecycle()
    val simulatedValue by viewModel.simulatedValue.collectAsStateWithLifecycle()
    val isListening by viewModel.isVoiceListening.collectAsStateWithLifecycle()
    val rmsLevel by viewModel.voiceRmsLevel.collectAsStateWithLifecycle()
    val lastVoiceTranscript by viewModel.lastVoiceTranscript.collectAsStateWithLifecycle()
    val statusMsg by viewModel.statusMessage.collectAsStateWithLifecycle()

    val driveUser by viewModel.driveUser.collectAsStateWithLifecycle()
    val driveUploadStatus by viewModel.driveUploadStatus.collectAsStateWithLifecycle()
    val isSavingToDrive by viewModel.isSavingToDrive.collectAsStateWithLifecycle()
    val lastDriveResult by viewModel.lastDriveResult.collectAsStateWithLifecycle()

    var showManualInputDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showZoomSlider by remember { mutableStateOf(false) }
    var showDriveDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF070B14))
        ) {
            // Background: Camera Viewfinder or Simulator Fallback
            if (hasCameraPermission && !isSimulatorVisible) {
                CameraPreviewView(
                    modifier = Modifier.fillMaxSize(),
                    torchEnabled = torchEnabled,
                    zoomRatio = zoomRatio,
                    onBitmapProviderReady = { provider ->
                        viewModel.bitmapProvider = provider
                    }
                )
            } else {
                // Digital dark pattern background when simulator is active or camera denied
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF0D1B2A), Color(0xFF040812))
                            )
                        )
                )
            }

            // Foreground Overlays & Controls
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Top Header Row with 20px Drive button
                TopHeaderBar(
                    isTorchOn = torchEnabled,
                    isSimulatorActive = isSimulatorVisible,
                    isDriveConnected = driveUser != null,
                    onToggleTorch = { viewModel.toggleTorch() },
                    onToggleZoom = { showZoomSlider = !showZoomSlider },
                    onToggleSimulator = { viewModel.toggleSimulator() },
                    onOpenDrive = { showDriveDialog = true },
                    onOpenSettings = { showSettingsDialog = true }
                )

                // Optional Zoom Slider Bar
                AnimatedVisibility(visible = showZoomSlider) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xCC000000))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Zoom",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = zoomRatio,
                            onValueChange = { viewModel.setZoomRatio(it) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00E5FF),
                                activeTrackColor = Color(0xFF00E5FF)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(1f + zoomRatio * 4f).toInt()}x",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable container for HUD elements to support various screen sizes
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Vision ROI Box (Targeting Reticle)
                    if (!isSimulatorVisible) {
                        RoiTargetBox(
                            isMatch = isMatch,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = { viewModel.readNumberNow() },
                            enabled = !isReading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("read_number_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0284C7)
                            )
                        ) {
                            Text(
                                text = if (isReading) "LEYENDO…" else "LEER NÚMERO",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Main 7-Segment / Bit Display Readout
                    BitDisplayView(
                        currentValue = currentNumber,
                        targetValue = config.targetQuantity,
                        unit = config.unit,
                        isMatch = isMatch,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Reset action button if target reached and held
                    if (isMatch) {
                        Button(
                            onClick = { viewModel.resetMatch() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("reset_match_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reiniciar lectura",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PESO LISTO • REINICIAR PARA SIGUIENTE",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Interactive Simulator Panel (if enabled)
                    if (isSimulatorVisible) {
                        BitScreenSimulator(
                            simulatedValue = simulatedValue,
                            targetValue = config.targetQuantity,
                            unit = config.unit,
                            onValueChange = { viewModel.updateSimulatedValue(it) },
                            onReachTarget = { viewModel.simulateReachTarget() },
                            onClose = { viewModel.toggleSimulator() }
                        )
                    }

                    // Voice Listening Indicator & Low Latency Speech Control
                    VoiceListeningIndicator(
                        isListening = isListening,
                        rmsLevel = rmsLevel,
                        lastSpokenText = lastVoiceTranscript,
                        targetQuantity = config.targetQuantity,
                        unit = config.unit,
                        onToggleListening = {
                            if (!hasAudioPermission) {
                                permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                            } else {
                                viewModel.toggleVoiceListening()
                            }
                        }
                    )

                    // Target Quick Selector & Presets
                    TargetQuickSelector(
                        targetQuantity = config.targetQuantity,
                        unit = config.unit,
                        onSelectTarget = { viewModel.setTargetQuantity(it, source = "Selector") },
                        onManualEditRequest = { showManualInputDialog = true }
                    )

                    // Minimal 20px Drive Sync bar (preserves space for camera & large numbers)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xCC111827))
                            .border(1.dp, Color(0x2238BDF8), RoundedCornerShape(12.dp))
                            .clickable { showDriveDialog = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Drive 20px",
                                tint = if (driveUser != null) Color(0xFF10B981) else Color(0xFF38BDF8),
                                modifier = Modifier
                                    .size(20.dp)
                                    .testTag("drive_icon_20px")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (driveUser != null) "Drive sync: ${driveUser?.email ?: "Activo"}" else "Google Drive (.md + foto)",
                                color = if (driveUser != null) Color(0xFF10B981) else Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = if (isSavingToDrive) "Guardando..." else if (driveUser != null) "Auto .md" else "Conectar",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Permission Request Prompt (if camera missing)
            if (!hasCameraPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xE6070B14))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF1E293B))
                            .padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Cámara",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Acceso a Cámara y Micrófono",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ScaleVision necesita la cámara para leer pantallas de bits y el micrófono para recibir comandos de voz.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            modifier = Modifier.testTag("request_permission_button")
                        ) {
                            Text("Conceder Permisos")
                        }
                    }
                }
            }

            // Dialogs
            if (showManualInputDialog) {
                ManualInputDialog(
                    currentTarget = config.targetQuantity,
                    currentUnit = config.unit,
                    onDismiss = { showManualInputDialog = false },
                    onConfirm = { qty, unit ->
                        viewModel.setTargetQuantity(qty, source = "Teclado")
                        if (unit != config.unit) {
                            viewModel.updateConfig(config.copy(unit = unit))
                        }
                        showManualInputDialog = false
                    }
                )
            }

            if (showSettingsDialog) {
                SettingsDialog(
                    currentConfig = config,
                    onDismiss = { showSettingsDialog = false },
                    onSave = { updated ->
                        viewModel.updateConfig(updated)
                        showSettingsDialog = false
                    },
                    onTestVoicePhrase = { phrase ->
                        viewModel.notificationManager.notifyTargetReached(
                            phraseMode = phrase,
                            targetValue = config.targetQuantity,
                            unit = config.unit,
                            enableSound = true,
                            enableTts = true,
                            enableHaptics = true
                        )
                    }
                )
            }

            if (showDriveDialog) {
                DriveSyncDialog(
                    userAccount = driveUser,
                    statusText = driveUploadStatus,
                    isSaving = isSavingToDrive,
                    lastResult = lastDriveResult,
                    onConnectDrive = {
                        driveAuthLauncher.launch(viewModel.getDriveSignInIntent())
                    },
                    onDisconnectDrive = {
                        viewModel.disconnectDrive()
                    },
                    onManualSave = {
                        val target = config.targetQuantity
                        val reached = currentNumber ?: target
                        viewModel.triggerSaveToGoogleDrive(target, reached)
                    },
                    onDismiss = { showDriveDialog = false }
                )
            }
        }
    }
}

@Composable
private fun TopHeaderBar(
    isTorchOn: Boolean,
    isSimulatorActive: Boolean,
    isDriveConnected: Boolean,
    onToggleTorch: () -> Unit,
    onToggleZoom: () -> Unit,
    onToggleSimulator: () -> Unit,
    onOpenDrive: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xCC0B132B))
            .border(1.dp, Color(0x2238BDF8), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // App Title & Status
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00FF88))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "ScaleVision",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isDriveConnected) "Drive & Markdown Activo" else "Visión de Pantalla de Bits",
                    color = if (isDriveConnected) Color(0xFF10B981) else Color(0xFF00E5FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Action icons: Drive (20px), Simulator, Torch, Zoom, Settings
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Google Drive Compact 20px Button
            IconButton(
                onClick = onOpenDrive,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isDriveConnected) Color(0x3310B981) else Color(0x220284C7))
                    .testTag("drive_compact_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = "Google Drive",
                    tint = if (isDriveConnected) Color(0xFF10B981) else Color(0xFF38BDF8),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Simulator Mode Toggle
            IconButton(
                onClick = onToggleSimulator,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isSimulatorActive) Color(0x3310B981) else Color(0x11FFFFFF))
                    .testTag("toggle_simulator_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SmartDisplay,
                    contentDescription = "Simulador digital",
                    tint = if (isSimulatorActive) Color(0xFF10B981) else Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Torch Toggle
            IconButton(
                onClick = onToggleTorch,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isTorchOn) Color(0x33FBBF24) else Color(0x11FFFFFF))
                    .testTag("toggle_torch_button")
            ) {
                Icon(
                    imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Flash linterna",
                    tint = if (isTorchOn) Color(0xFFFBBF24) else Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Zoom Toggle
            IconButton(
                onClick = onToggleZoom,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0x11FFFFFF))
                    .testTag("toggle_zoom_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = "Zoom digital",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Settings
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0x11FFFFFF))
                    .testTag("open_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Ajustes de visión",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
