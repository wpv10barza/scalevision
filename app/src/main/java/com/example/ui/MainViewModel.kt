package com.example.ui

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.VoiceNotificationManager
import com.example.audio.VoiceRecognitionManager
import com.example.data.DriveUploadResult
import com.example.data.FirebaseConfigRepository
import com.example.data.GoogleDriveManager
import com.example.model.ScaleConfig
import com.example.vision.BitmapCaptureHelper
import com.example.vision.BitScreenAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseRepo = FirebaseConfigRepository()
    val notificationManager = VoiceNotificationManager(application)
    val driveManager = GoogleDriveManager(application)

    private val _config = MutableStateFlow(ScaleConfig())
    val config: StateFlow<ScaleConfig> = _config.asStateFlow()

    private val _currentDetectedNumber = MutableStateFlow<Double?>(null)
    val currentDetectedNumber: StateFlow<Double?> = _currentDetectedNumber.asStateFlow()

    private val _rawOcrText = MutableStateFlow("")
    val rawOcrText: StateFlow<String> = _rawOcrText.asStateFlow()

    private val _isMatch = MutableStateFlow(false)
    val isMatch: StateFlow<Boolean> = _isMatch.asStateFlow()

    private val _torchEnabled = MutableStateFlow(false)
    val torchEnabled: StateFlow<Boolean> = _torchEnabled.asStateFlow()

    private val _zoomRatio = MutableStateFlow(0f)
    val zoomRatio: StateFlow<Float> = _zoomRatio.asStateFlow()

    private val _isSimulatorVisible = MutableStateFlow(false)
    val isSimulatorVisible: StateFlow<Boolean> = _isSimulatorVisible.asStateFlow()

    private val _simulatedValue = MutableStateFlow(0.0)
    val simulatedValue: StateFlow<Double> = _simulatedValue.asStateFlow()

    private val _statusMessage = MutableStateFlow("Listo para escanear indicador de bits")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _lastDriveResult = MutableStateFlow<DriveUploadResult?>(null)
    val lastDriveResult: StateFlow<DriveUploadResult?> = _lastDriveResult.asStateFlow()

    private val _isSavingToDrive = MutableStateFlow(false)
    val isSavingToDrive: StateFlow<Boolean> = _isSavingToDrive.asStateFlow()

    // Camera Frame Bitmap Provider
    var bitmapProvider: (() -> Bitmap?)? = null

    // Consecutive frame counter for stability
    private var lastStableValue: Double? = null
    private var consecutiveStableCount: Int = 0
    private var hasAnnouncedCurrentMatch: Boolean = false

    // Voice recognition
    val voiceRecognitionManager = VoiceRecognitionManager(application) { spokenQuantity ->
        setTargetQuantity(spokenQuantity, source = "Voz")
    }

    val isVoiceListening = voiceRecognitionManager.isListening
    val voiceRmsLevel = voiceRecognitionManager.rmsLevel
    val lastVoiceTranscript = voiceRecognitionManager.lastRecognizedText

    // Google Drive state
    val driveUser = driveManager.currentUser
    val driveUploadStatus = driveManager.uploadStatus

    // Vision Analyzer
    val bitScreenAnalyzer = BitScreenAnalyzer { detectedVal, raw ->
        if (!_isSimulatorVisible.value) {
            processDetectedNumber(detectedVal, raw)
        }
    }

    init {
        // Observe real-time config from Firebase Firestore
        viewModelScope.launch {
            firebaseRepo.observeConfig().collect { remoteConfig ->
                _config.value = remoteConfig
                // Reset match when remote target changes
                if (Math.abs(remoteConfig.targetQuantity - (_currentDetectedNumber.value ?: -1.0)) > remoteConfig.tolerance) {
                    _isMatch.value = false
                    hasAnnouncedCurrentMatch = false
                }
            }
        }
    }

    fun processDetectedNumber(detectedVal: Double?, raw: String) {
        _rawOcrText.value = raw
        if (detectedVal == null) {
            _currentDetectedNumber.value = null
            if (!_config.value.holdMode) {
                _isMatch.value = false
            }
            return
        }

        // Stability check: rounded to 2 decimal places
        val rounded = Math.round(detectedVal * 100.0) / 100.0
        if (lastStableValue != null && Math.abs(lastStableValue!! - rounded) < 0.009) {
            consecutiveStableCount++
        } else {
            lastStableValue = rounded
            consecutiveStableCount = 1
        }

        _currentDetectedNumber.value = rounded

        val target = _config.value.targetQuantity
        val tol = _config.value.tolerance
        val isTargetReached = Math.abs(rounded - target) <= tol

        if (isTargetReached) {
            if (!_isMatch.value || !hasAnnouncedCurrentMatch) {
                _isMatch.value = true
                hasAnnouncedCurrentMatch = true
                _statusMessage.value = "¡Cantidad exacta alcanzada: $rounded ${_config.value.unit}!"

                // Trigger low-latency voice alert / click
                notificationManager.notifyTargetReached(
                    phraseMode = _config.value.voiceAlertPhrase,
                    targetValue = rounded,
                    unit = _config.value.unit,
                    enableSound = _config.value.soundBeepEnabled,
                    enableTts = _config.value.ttsEnabled,
                    enableHaptics = _config.value.hapticsEnabled
                )

                // Sync log to Firebase Firestore
                firebaseRepo.logCompletedWeight(target, rounded, _config.value.unit)

                // Save to Google Drive & Markdown with captured image
                triggerSaveToGoogleDrive(target, rounded)
            }
        } else {
            if (!_config.value.holdMode) {
                _isMatch.value = false
                hasAnnouncedCurrentMatch = false
            }
        }
    }

    fun triggerSaveToGoogleDrive(target: Double, reached: Double) {
        viewModelScope.launch {
            _isSavingToDrive.value = true
            try {
                // Get image bytes
                val imageBytes = withContext(Dispatchers.Default) {
                    val frameBitmap = if (!_isSimulatorVisible.value) bitmapProvider?.invoke() else null
                    if (frameBitmap != null) {
                        BitmapCaptureHelper.bitmapToJpegBytes(frameBitmap)
                    } else {
                        BitmapCaptureHelper.createSyntheticBitDisplayBitmap(target, reached, _config.value.unit)
                    }
                }

                val result = driveManager.saveRecord(
                    targetQuantity = target,
                    detectedQuantity = reached,
                    unit = _config.value.unit,
                    voicePhrase = _config.value.voiceAlertPhrase,
                    imageBytes = imageBytes
                )
                _lastDriveResult.value = result
                if (result.success) {
                    _statusMessage.value = "¡Guardado en Google Drive y Markdown!"
                } else if (result.localMarkdownPath != null) {
                    _statusMessage.value = "Guardado localmente en Markdown (sin conexión a Drive)"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSavingToDrive.value = false
            }
        }
    }

    fun setTargetQuantity(quantity: Double, source: String = "Manual") {
        val safeQuantity = Math.round(quantity * 100.0) / 100.0
        val updated = _config.value.copy(
            targetQuantity = safeQuantity,
            lastUpdated = System.currentTimeMillis()
        )
        _config.value = updated
        _isMatch.value = false
        hasAnnouncedCurrentMatch = false
        _statusMessage.value = "Meta fijada: $safeQuantity ${updated.unit} ($source)"

        // Play short sound & verbal feedback for voice recognition confirmation
        if (source == "Voz") {
            notificationManager.playClickSound()
            notificationManager.speakPrompt("Meta $safeQuantity ${updated.unit}")
        }

        // Sync to Firebase in real-time
        firebaseRepo.updateConfig(updated)
    }

    fun updateConfig(newConfig: ScaleConfig) {
        _config.value = newConfig
        _isMatch.value = false
        hasAnnouncedCurrentMatch = false
        firebaseRepo.updateConfig(newConfig)
    }

    fun toggleVoiceListening() {
        if (isVoiceListening.value) {
            voiceRecognitionManager.stopListening()
        } else {
            voiceRecognitionManager.startListening()
        }
    }

    fun toggleTorch() {
        _torchEnabled.value = !_torchEnabled.value
    }

    fun setZoomRatio(ratio: Float) {
        _zoomRatio.value = ratio.coerceIn(0f, 1f)
    }

    fun toggleSimulator() {
        _isSimulatorVisible.value = !_isSimulatorVisible.value
        if (_isSimulatorVisible.value) {
            _simulatedValue.value = 0.0
            processDetectedNumber(0.0, "SIM: 0.00")
        }
    }

    fun updateSimulatedValue(value: Double) {
        val safeVal = Math.round(value * 100.0) / 100.0
        _simulatedValue.value = safeVal
        processDetectedNumber(safeVal, "SIM: $safeVal")
    }

    fun simulateReachTarget() {
        val target = _config.value.targetQuantity
        updateSimulatedValue(target)
    }

    fun resetMatch() {
        _isMatch.value = false
        hasAnnouncedCurrentMatch = false
        _statusMessage.value = "Listo para nueva lectura"
    }

    // Google Drive Auth handlers
    fun getDriveSignInIntent(): Intent = driveManager.getSignInIntent()

    fun handleDriveSignInResult(data: Intent?) {
        driveManager.handleSignInResult(data)
    }

    fun disconnectDrive() {
        driveManager.signOut()
    }

    override fun onCleared() {
        super.onCleared()
        voiceRecognitionManager.destroy()
        notificationManager.shutdown()
    }
}
