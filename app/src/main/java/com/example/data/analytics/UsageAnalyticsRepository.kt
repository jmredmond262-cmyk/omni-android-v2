package com.example.data.analytics

import com.example.BuildConfig
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

/**
 * First-party product analytics compatible with the existing Firestore rules.
 * Records only module/event/version/session metadata; never message contents,
 * passwords, precise location, health data, or payment credentials.
 */
object UsageAnalyticsRepository {
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

    private fun record(uid: String, module: String, eventName: String) {
        if (uid.isBlank()) return
        val event = hashMapOf<String, Any>(
            "uid" to uid,
            "module" to module,
            "event" to eventName,
            "sessionId" to sessionId,
            "appVersion" to BuildConfig.VERSION_NAME,
            "createdAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("usageEvents").add(event)
    }
}
