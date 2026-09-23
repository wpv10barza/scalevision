package com.example.data

import android.util.Log
import com.example.model.ScaleConfig
import com.example.model.WeightHistoryLog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirebaseConfigRepository {

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private val configDocRef by lazy {
        firestore.collection("scale_settings").document("current_config")
    }

    private val historyCollectionRef by lazy {
        firestore.collection("weight_history_logs")
    }

    /**
     * Real-time listener for scale configuration stored in Firebase Firestore.
     */
    fun observeConfig(): Flow<ScaleConfig> = callbackFlow {
        var registration: ListenerRegistration? = null
        try {
            registration = configDocRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirebaseConfigRepo", "Listen error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists() && snapshot.data != null) {
                    val config = ScaleConfig.fromMap(snapshot.data!!)
                    trySend(config)
                } else {
                    // Initialize default in Firestore if empty
                    val defaultConfig = ScaleConfig()
                    configDocRef.set(defaultConfig.toMap(), SetOptions.merge())
                    trySend(defaultConfig)
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseConfigRepo", "Failed to register snapshot listener: ${e.message}")
            trySend(ScaleConfig())
        }

        awaitClose {
            registration?.remove()
        }
    }

    /**
     * Updates configuration in Firebase Firestore in real-time.
     */
    fun updateConfig(config: ScaleConfig, onComplete: (Boolean) -> Unit = {}) {
        try {
            configDocRef.set(config.toMap(), SetOptions.merge())
                .addOnSuccessListener {
                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseConfigRepo", "Error updating config: ${e.message}")
                    onComplete(false)
                }
        } catch (e: Exception) {
            Log.e("FirebaseConfigRepo", "Exception updating config: ${e.message}")
            onComplete(false)
        }
    }

    /**
     * Logs successful weight match event to Firestore history collection.
     */
    fun logCompletedWeight(target: Double, reached: Double, unit: String) {
        try {
            val entry = hashMapOf(
                "targetQuantity" to target,
                "reachedQuantity" to reached,
                "unit" to unit,
                "timestamp" to System.currentTimeMillis()
            )
            historyCollectionRef.add(entry)
                .addOnSuccessListener { doc ->
                    Log.d("FirebaseConfigRepo", "Logged weight match: ${doc.id}")
                }
                .addOnFailureListener { e ->
                    Log.w("FirebaseConfigRepo", "Failed to log weight match: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e("FirebaseConfigRepo", "Log error: ${e.message}")
        }
    }
}
