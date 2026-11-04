package com.example.data.analytics

import android.util.Log
import com.example.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

/**
 * Universal first-party analytics. Firestore's Android SDK persists pending writes
 * locally and retries them when connectivity returns. Stable document IDs prevent
 * duplicate records; no shared secret or second analytics server is embedded in the APK.
 *
 * Only allowlisted, non-sensitive metadata is recorded. Never pass message/search text,
 * names, phone numbers, precise locations, photo URIs, credentials, health details,
 * or exact payment amounts.
 */
object UsageAnalyticsRepository {
    private const val TAG = "OmniUsageAnalytics"
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val allowedMetadataKeys = setOf(
        "category", "source", "resultCount", "durationMs", "success",
        "itemType", "method", "flow", "surface", "destinationType",
        "screenName", "radiusBucket", "status", "operationId", "correlationId", "stage"
    )

    @Volatile private var sessionId: String = ""
    @Volatile private var activeUid: String = ""
    @Volatile private var lastScreen: String = ""
    @Volatile private var screenStartedAt: Long = 0L
    @Volatile private var sessionActive: Boolean = false

    @Synchronized fun startSession(uid: String) {
        if (uid.isBlank() || (sessionActive && activeUid == uid)) return
        beginSession(uid)
    }

    @Synchronized fun resumeSession(uid: String) {
        if (uid.isBlank()) return
        if (!sessionActive || activeUid != uid) beginSession(uid)
        else {
            record(uid, "app", "APP_FOREGROUND")
            if (lastScreen.isNotBlank()) trackScreen(uid, lastScreen)
        }
    }

    @Synchronized private fun beginSession(uid: String) {
        activeUid = uid
        sessionId = UUID.randomUUID().toString()
        sessionActive = true
        screenStartedAt = 0L
        record(uid, "app", "SESSION_START")
        record(uid, "diagnostics", "DIAGNOSTIC_TEST")
        if (lastScreen.isNotBlank()) trackScreen(uid, lastScreen)
    }

    @Synchronized fun endSession() {
        if (!sessionActive || activeUid.isBlank()) return
        val uid = activeUid
        closeCurrentScreen(uid)
        record(uid, "app", "APP_BACKGROUND")
        record(uid, "app", "SESSION_END")
        sessionActive = false
        screenStartedAt = 0L
    }

    @Synchronized fun trackScreen(uid: String, screen: String) {
        if (uid.isBlank() || screen.isBlank()) return
        if (!sessionActive || activeUid != uid) beginSession(uid)
        val clean = screen.trim().take(80)
        if (lastScreen == clean && screenStartedAt > 0L) return
        closeCurrentScreen(uid)
        lastScreen = clean
        screenStartedAt = System.currentTimeMillis()
        record(uid, clean, "SCREEN_VIEW", mapOf("screenName" to clean))
    }

    @Synchronized private fun closeCurrentScreen(uid: String) {
        if (lastScreen.isNotBlank() && screenStartedAt > 0L) {
            record(uid, lastScreen, "SCREEN_EXIT", mapOf("screenName" to lastScreen))
        }
        screenStartedAt = 0L
    }

    fun trackAction(uid: String, action: String, module: String) = trackEvent(uid, module, action)

    fun trackEvent(uid: String, module: String, action: String, metadata: Map<String, Any> = emptyMap()) {
        if (uid.isBlank() || action.isBlank() || module.isBlank()) return
        record(uid, module, action, metadata)
    }

    fun trackCurrentAction(module: String, action: String, metadata: Map<String, Any> = emptyMap()) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (uid.isNotBlank()) trackEvent(uid, module, action, metadata)
    }

    fun trackWalletOperation(uid: String, operation: String) {
        trackEvent(uid, "wallet", operation, mapOf("flow" to "demo_wallet"))
    }

    private fun safeMetadata(metadata: Map<String, Any>): Map<String, Any> =
        metadata.entries.asSequence().filter { it.key in allowedMetadataKeys }.take(10)
            .mapNotNull { (key, value) ->
                when (value) {
                    is String -> key to value.trim().take(80)
                    is Boolean -> key to value
                    is Int -> key to value
                    is Long -> if (value >= 0L) key to value else null
                    is Double -> if (value.isFinite() && value >= 0.0) key to value else null
                    is Float -> if (value.isFinite() && value >= 0f) key to value else null
                    else -> null
                }
            }.toMap()

    private fun record(requestedUid: String, module: String, eventName: String, metadata: Map<String, Any> = emptyMap()) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: requestedUid.takeIf { it.isNotBlank() } ?: return
        val eventId = UUID.randomUUID().toString()
        val cleanModule = module.trim().take(80)
        val cleanEvent = eventName.trim().uppercase().take(80)
        val event = hashMapOf<String, Any>(
            "eventId" to eventId, "uid" to uid, "module" to cleanModule,
            "category" to analyticsCategory(cleanModule, cleanEvent, metadata),
            "event" to cleanEvent,
            "sessionId" to sessionId.ifBlank { UUID.randomUUID().toString() },
            "appVersion" to BuildConfig.VERSION_NAME, "platform" to "android",
            "schemaVersion" to 3, "createdAt" to FieldValue.serverTimestamp()
        )
        val safe = safeMetadata(metadata)
        if (safe.isNotEmpty()) event["data"] = safe
        firestore.collection("usageEvents").document(eventId).set(event)
            .addOnSuccessListener { Log.d(TAG, "Evento registrado: $eventName / $module / $eventId") }
            .addOnFailureListener { error ->
                Log.e(TAG, "No se pudo registrar $eventName / $module: ${error.message}", error)
            }
    }
}


/** Produces a readable category for every new usage event. */
private fun analyticsCategory(module: String, event: String, metadata: Map<String, Any>): String {
    val data = metadata.entries.associate { it.key.lowercase() to it.value.toString().lowercase() }
    val source = listOf(module, data["category"].orEmpty(), data["screenname"].orEmpty(),
        data["source"].orEmpty(), data["surface"].orEmpty(), data["flow"].orEmpty())
        .joinToString(" ").lowercase()
    return when {
        listOf("wallet", "billetera", "saldo", "qr_pay", "qr_payment", "transfer", "deposit").any(source::contains) ||
            event.contains("QR_PAYMENT") || event.contains("TRANSFER") || event.contains("DEPOSIT") -> "Billetera"
        listOf("order", "pedido", "market", "carrito", "checkout").any(source::contains) -> "Pedidos y Mercado"
        listOf("event", "evento", "agenda_event").any(source::contains) -> "Eventos"
        listOf("job", "trabajo", "empleo", "cv").any(source::contains) -> "Bolsa de Trabajo"
        listOf("merchant", "comercio", "store", "negocio").any(source::contains) -> "Comercios"
        listOf("user", "usuario", "profile", "perfil", "contact").any(source::contains) -> "Usuarios"
        listOf("navigation", "map", "mapa", "route", "ruta", "location", "ubicacion").any(source::contains) -> "Navegación"
        listOf("tourism", "turismo", "experience", "experiencia").any(source::contains) -> "Turismo y experiencias"
        listOf("routine", "rutina", "weather", "clima", "reminder", "recordatorio").any(source::contains) -> "Rutinas y clima"
        listOf("camera", "camara", "ai", "inteligencia").any(source::contains) -> "Cámara e IA"
        module.equals("app", true) || module.equals("diagnostics", true) ||
            event.startsWith("SESSION_") || event.startsWith("APP_") || event.startsWith("SCREEN_") -> "Sistema"
        else -> "Otros"
    }
}
