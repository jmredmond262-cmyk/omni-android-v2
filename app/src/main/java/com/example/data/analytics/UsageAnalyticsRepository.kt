package com.example.data.analytics

import com.example.BuildConfig
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

/**
 * Minimal first-party product analytics for OMNI Admin.
 * Stores operational events only; never records message contents, passwords,
 * precise location, health data, or payment credentials.
 */
object UsageAnalyticsRepository {
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val sessionId: String = UUID.randomUUID().toString()

    fun startSession(uid: String) {
        record(uid, "SESSION_START", "Inicio de sesión", emptyMap())
    }

    fun trackScreen(uid: String, screen: String) {
        record(uid, "SCREEN_VIEW", screen, mapOf("screen" to screen))
    }

    fun trackAction(uid: String, action: String, screen: String, details: Map<String, Any> = emptyMap()) {
        record(uid, action, screen, details)
    }

    private fun record(uid: String, eventName: String, screen: String, details: Map<String, Any>) {
        if (uid.isBlank()) return
        val event = hashMapOf<String, Any>(
            "uid" to uid,
            "eventName" to eventName,
            "eventType" to eventName,
            "screen" to screen,
            "sessionId" to sessionId,
            "platform" to "android",
            "appVersion" to BuildConfig.VERSION_NAME,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (details.isNotEmpty()) event["details"] = details
        firestore.collection("usageEvents").add(event)
    }
}
