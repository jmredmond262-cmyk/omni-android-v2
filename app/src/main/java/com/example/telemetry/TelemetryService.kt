package com.example.telemetry

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class TelemetryService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        const val CHANNEL_ID = "telemetry_channel"
        const val NOTIFICATION_ID = 9991
        const val ACTION_START = "START_TRACKING"
        const val ACTION_STOP = "STOP_TRACKING"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    sendLocationUpdate(location, "RASTREO_EN_VIVO")
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startContinuousTracking()
            ACTION_STOP -> stopContinuousTracking()
        }
        return START_STICKY
    }

    private fun startContinuousTracking() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(3000L)
            .setMinUpdateDistanceMeters(5f)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (_: SecurityException) {}
    }

    private fun stopContinuousTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun sendLocationUpdate(location: Location, eventType: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "USUARIO_PRUEBA"
        val batteryStatus = getBatteryHealth()

        val event = TelemetryEvent(
            eventId = UUID.randomUUID().toString(),
            userId = userId,
            eventType = eventType,
            trackingMode = "CONTINUO",
            location = GeoPointData(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                speed = location.speed,
                bearing = location.bearing,
                altitude = location.altitude
            ),
            deviceHealth = batteryStatus,
            details = mapOf("provider" to (location.provider ?: "gps"))
        )

        firestore.collection("telemetry_events").add(event)
    }

    private fun getBatteryHealth(): DeviceHealth {
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val batteryPct = if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else 100

        return DeviceHealth(
            batteryLevel = batteryPct,
            isCharging = isCharging,
            networkType = "ACTIVA"
        )
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Cyber Telemetry Activo")
            .setContentText("Transmitiendo ubicación y sucesos en tiempo real")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Canal de Telemetría",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
