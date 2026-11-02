package com.example.data.analytics

import android.util.Log
import com.example.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.util.UUID

/**
 * First-party product analytics. Stores only module/event/version/session metadata.
 * No message contents, passwords, precise location, health data, or payment credentials.
 */
object UsageAnalyticsRepository {
    private const val TAG = "OmniUsageAnalytics"
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val sessionId: String = UUID.randomUUID().toString()

    fun startSession(uid: String) {
        record(uid, "app", "SESSION_START")
    }

    fun trackScreen(uid: String, screen: String) {
        record(uid, screen, "SCREEN_VIEW")
    }

    fun trackAction(uid: String, action: String, module: String) {
        record(uid, module, action)
    }

    fun trackWalletOperation(uid: String, operation: String) {
        trackAction(uid, operation, "wallet")
    }

    private fun record(requestedUid: String, module: String, eventName: String) {
        // Resolve identity from Firebase Auth as the source of truth; the UI profile
        // can still be initializing when the first screen is composed.
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?.takeIf { it.isNotBlank() }
            ?: requestedUid.takeIf { it.isNotBlank() }
            ?: return

        val event = hashMapOf<String, Any>(
            "uid" to uid,
            "module" to module.take(80),
            "event" to eventName.take(80),
            "sessionId" to sessionId,
            "appVersion" to BuildConfig.VERSION_NAME,
            // Send an explicit Firestore timestamp instead of a server transform,
            // so the security rule can validate the field as a timestamp at create time.
            "createdAt" to Timestamp.now()
        )

        firestore.collection("usageEvents")
            .add(event)
            .addOnSuccessListener { document ->
                Log.d(TAG, "Evento registrado: $eventName / $module / ${document.id}")
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "No se pudo registrar $eventName / $module: ${error.message}", error)
            }
    }
}
