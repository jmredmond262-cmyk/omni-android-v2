package com.example.telemetry

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class GeoPointData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val speed: Float = 0f,
    val bearing: Float = 0f,
    val altitude: Double = 0.0
)

data class DeviceHealth(
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false,
    val networkType: String = "DESCONOCIDO"
)

data class TelemetryEvent(
    val eventId: String = "",
    val userId: String = "",
    val eventType: String = "",
    val trackingMode: String = "PUNTUAL",
    val location: GeoPointData = GeoPointData(),
    val deviceHealth: DeviceHealth = DeviceHealth(),
    val details: Map<String, String> = emptyMap(),
    @ServerTimestamp val timestamp: Date? = null
)
