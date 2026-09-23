package com.example.data

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

data class DriveUploadResult(
    val success: Boolean,
    val imageFileId: String? = null,
    val markdownFileId: String? = null,
    val localMarkdownPath: String? = null,
    val localImagePath: String? = null,
    val errorMessage: String? = null
)

class GoogleDriveManager(private val context: Context) {

    private val driveScope = Scope("https://www.googleapis.com/auth/drive.file")
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(driveScope)
        .build()

    private val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    private val _currentUser = MutableStateFlow<GoogleSignInAccount?>(
        GoogleSignIn.getLastSignedInAccount(context)
    )
    val currentUser: StateFlow<GoogleSignInAccount?> = _currentUser.asStateFlow()

    private val _uploadStatus = MutableStateFlow<String?>("Listo para sincronizar con Drive")
    val uploadStatus: StateFlow<String?> = _uploadStatus.asStateFlow()

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    fun handleSignInResult(data: Intent?): Boolean {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            _currentUser.value = account
            _uploadStatus.value = "Conectado a Google Drive: ${account.email}"
            true
        } catch (e: Exception) {
            Log.e("GoogleDriveManager", "Sign-in error: ${e.message}")
            _uploadStatus.value = "Error al conectar Google Drive: ${e.localizedMessage}"
            false
        }
    }

    fun signOut(onComplete: () -> Unit = {}) {
        googleSignInClient.signOut().addOnCompleteListener {
            _currentUser.value = null
            _uploadStatus.value = "Desconectado de Google Drive"
            onComplete()
        }
    }

    fun isConnected(): Boolean {
        val account = _currentUser.value ?: GoogleSignIn.getLastSignedInAccount(context)
        return account != null && GoogleSignIn.hasPermissions(account, driveScope)
    }

    /**
     * Obtains OAuth Bearer token for Google Drive API.
     */
    private suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        val account = _currentUser.value ?: GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext null
        try {
            val tokenScope = "oauth2:https://www.googleapis.com/auth/drive.file"
            GoogleAuthUtil.getToken(context, account.account ?: return@withContext null, tokenScope)
        } catch (e: Exception) {
            Log.e("GoogleDriveManager", "Error getting OAuth token: ${e.message}")
            null
        }
    }

    /**
     * Uploads the captured image and the Markdown report containing:
     * - Número pedido por comando de voz (Target)
     * - Número detectado en pantalla de bits
     * - Imagen capturada
     */
    suspend fun saveRecord(
        targetQuantity: Double,
        detectedQuantity: Double,
        unit: String,
        voicePhrase: String,
        imageBytes: ByteArray
    ): DriveUploadResult = withContext(Dispatchers.IO) {
        val timeFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val prettyDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timestampStr = timeFormat.format(Date())
        val displayDate = prettyDateFormat.format(Date())
        val recordId = "REC-" + UUID.randomUUID().toString().take(8).uppercase()

        val baseFileName = "registro_pesaje_${timestampStr}_${targetQuantity}${unit}"
        val imageFileName = "captura_${timestampStr}_${targetQuantity}${unit}.jpg"
        val mdFileName = "$baseFileName.md"

        // 1. Save locally first (backup & offline cache)
        val exportDir = File(context.filesDir, "drive_exports").apply { mkdirs() }
        val localImageFile = File(exportDir, imageFileName)
        val localMdFile = File(exportDir, mdFileName)

        try {
            FileOutputStream(localImageFile).use { it.write(imageBytes) }
        } catch (e: Exception) {
            Log.w("GoogleDriveManager", "Could not save local image: ${e.message}")
        }

        val token = getAccessToken()
        var imageDriveId: String? = null
        var mdDriveId: String? = null

        if (token != null) {
            _uploadStatus.value = "Subiendo imagen a Google Drive..."
            // 2. Upload Image to Google Drive
            imageDriveId = uploadMultipartFile(
                token = token,
                fileName = imageFileName,
                mimeType = "image/jpeg",
                fileData = imageBytes
            )
        }

        // 3. Construct Markdown Content
        val imageLinkMarkdown = if (imageDriveId != null) {
            "[Ver Imagen en Google Drive (ID: $imageDriveId)](https://drive.google.com/file/d/$imageDriveId/view)"
        } else {
            "*(Guardada localmente en dispositivo: `$imageFileName`)*"
        }

        val markdownContent = buildString {
            appendLine("# Registro de Pesaje - ScaleVision")
            appendLine()
            appendLine("> **Sistema de Visión de Pantallas de Bits con Control por Voz**")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## Información de la Operación")
            appendLine()
            appendLine("| Parámetro | Detalle |")
            appendLine("| :--- | :--- |")
            appendLine("| **Número pedido por comando de voz** | **$targetQuantity $unit** |")
            appendLine("| **Número detectado en pantalla de bits** | **$detectedQuantity $unit** |")
            appendLine("| **Estado de coincidencia** | **EXACTO (Meta Cumplida)** |")
            appendLine("| **Aviso de voz emitido** | *\"$voicePhrase\"* |")
            appendLine("| **Fecha y Hora de Lectura** | $displayDate |")
            appendLine("| **ID de Registro** | `$recordId` |")
            appendLine("| **Sincronización** | Firebase Firestore + Google Drive |")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## Captura de Pantalla de Bits")
            appendLine()
            appendLine("Captura visual del indicador digital / pantalla de 7 segmentos de la báscula al confirmarse la cantidad exacta:")
            appendLine()
            appendLine("- **Archivo:** `$imageFileName`")
            appendLine("- **Enlace de Acceso:** $imageLinkMarkdown")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("*Generado automáticamente por ScaleVision para Android.*")
        }

        // Save local Markdown file
        try {
            FileOutputStream(localMdFile).use { it.write(markdownContent.toByteArray(Charsets.UTF_8)) }
        } catch (e: Exception) {
            Log.w("GoogleDriveManager", "Could not save local md: ${e.message}")
        }

        if (token != null) {
            _uploadStatus.value = "Subiendo informe Markdown a Google Drive..."
            // 4. Upload Markdown File to Google Drive
            mdDriveId = uploadMultipartFile(
                token = token,
                fileName = mdFileName,
                mimeType = "text/markdown",
                fileData = markdownContent.toByteArray(Charsets.UTF_8)
            )

            _uploadStatus.value = if (mdDriveId != null) {
                "¡Guardado con éxito en Google Drive! ($mdFileName)"
            } else {
                "Guardado localmente. Error en subida a Google Drive"
            }
        } else {
            _uploadStatus.value = "Guardado localmente en Markdown. Conecta tu cuenta para sincronizar con Drive."
        }

        DriveUploadResult(
            success = token != null && mdDriveId != null,
            imageFileId = imageDriveId,
            markdownFileId = mdDriveId,
            localMarkdownPath = localMdFile.absolutePath,
            localImagePath = localImageFile.absolutePath,
            errorMessage = if (token == null) "Sin cuenta de Google conectada" else null
        )
    }

    /**
     * Uploads file to Google Drive v3 via multipart upload.
     */
    private fun uploadMultipartFile(
        token: String,
        fileName: String,
        mimeType: String,
        fileData: ByteArray
    ): String? {
        val url = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"

        val metadataJson = JSONObject().apply {
            put("name", fileName)
            put("mimeType", mimeType)
            put("description", "Registro automático generado por ScaleVision")
        }.toString()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addPart(
                metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType())
            )
            .addPart(
                fileData.toRequestBody(mimeType.toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .post(requestBody)
            .build()

        return try {
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                json.optString("id", null)
            } else {
                Log.e("GoogleDriveManager", "Upload failed: ${response.code} - $responseBody")
                null
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveManager", "Upload exception: ${e.message}")
            null
        }
    }
}
