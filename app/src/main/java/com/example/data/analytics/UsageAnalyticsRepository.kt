package com.example.data.analytics

import android.util.Log
import com.example.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.util.UUID

/**
 * First-party product analytics.
 * Stores event metadata only: no message contents, passwords, precise location,
 * health data, payment credentials, or search text.
 *
 * Events are written to the existing usageEvents collection and use only fields
 * already allowed by firestore.rules.
 */
object UsageAnalyticsRepository {
    private const val TAG = "OmniUsageAnalytics"
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    @Volatile private var sessionId: String = ""
    @Volatile private var activeUid: String = ""
    @Volatile private var sessionStartedAt: Long = 0L
    @Volatile private var lastScreen: String = ""
    @Volatile private var screenStartedAt: Long = 0L
    @Volatile private var sessionActive: Boolean = false

    @Synchronized
    fun startSession(uid: String) {
        if (uid.isBlank()) return
        if (sessionActive && activeUid == uid) return
        beginSession(uid)
    }

    @Synchronized
    fun resumeSession(uid: String) {
        if (uid.isBlank()) return
        if (!sessionActive || activeUid != uid) {
            beginSession(uid)
        } else {
            record(uid, "app", "APP_FOREGROUND")
            if (lastScreen.isNotBlank()) trackScreen(uid, lastScreen)
        }
    }

    @Synchronized
    private fun beginSession(uid: String) {
        activeUid = uid
        sessionId = UUID.randomUUID().toString()
        sessionStartedAt = System.currentTimeMillis()
        sessionActive = true
        screenStartedAt = 0L
        record(uid, "app", "SESSION_START")
        // Sentinel kept for the existing end-to-end connection diagnostic.
        record(uid, "diagnostics", "DIAGNOSTIC_TEST")
        if (lastScreen.isNotBlank()) trackScreen(uid, lastScreen)
    }

    @Synchronized
    fun endSession() {
        if (!sessionActive || activeUid.isBlank()) return
        val uid = activeUid
        closeCurrentScreen(uid)
        record(uid, "app", "APP_BACKGROUND")
        record(uid, "app", "SESSION_END")
        sessionActive = false
        sessionStartedAt = 0L
        screenStartedAt = 0L
    }

    @Synchronized
    fun trackScreen(uid: String, screen: String) {
        if (uid.isBlank() || screen.isBlank()) return
        if (!sessionActive || activeUid != uid) beginSession(uid)
        val cleanScreen = screen.trim().take(80)
        if (lastScreen == cleanScreen && screenStartedAt > 0L) return
        closeCurrentScreen(uid)
        lastScreen = cleanScreen
        screenStartedAt = System.currentTimeMillis()
        record(uid, cleanScreen, "SCREEN_VIEW")
    }

    @Synchronized
    private fun closeCurrentScreen(uid: String) {
        if (lastScreen.isNotBlank() && screenStartedAt > 0L) {
            record(uid, lastScreen, "SCREEN_EXIT")
        }
        screenStartedAt = 0L
    }

    fun trackAction(uid: String, action: String, module: String) {
        if (uid.isBlank() || action.isBlank() || module.isBlank()) return
        record(uid, module, action.trim().uppercase().take(80))
    }

    fun trackWalletOperation(uid: String, operation: String) {
        trackAction(uid, operation, "wallet")
    }

    private fun record(requestedUid: String, module: String, eventName: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?.takeIf { it.isNotBlank() }
            ?: requestedUid.takeIf { it.isNotBlank() }
            ?: return
        val currentSession = sessionId.ifBlank { UUID.randomUUID().toString() }
        val event = hashMapOf<String, Any>(
            "uid" to uid,
            "module" to module.take(80),
            "event" to eventName.take(80),
            "sessionId" to currentSession,
            "appVersion" to BuildConfig.VERSION_NAME,
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
