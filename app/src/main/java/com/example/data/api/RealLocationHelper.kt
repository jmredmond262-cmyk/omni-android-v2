package com.example.data.api

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object RealLocationHelper {
    private const val TAG = "RealLocationHelper"

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) {
            Log.d(TAG, "Location permission not granted")
            return null
        }

        return try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            // Reutilizar primero una posición reciente evita esperar un nuevo fix GPS en cada pantalla.
            val cached = suspendCancellableCoroutine<Location?> { continuation ->
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (continuation.isActive) continuation.resume(location)
                    }
                    .addOnFailureListener { exception ->
                        Log.w(TAG, "No se pudo leer lastLocation: ${exception.message}")
                        if (continuation.isActive) continuation.resume(null)
                    }
            }
            val now = System.currentTimeMillis()
            val cachedIsFresh = cached != null &&
                cached.time > 0L && now - cached.time in 0L..180_000L &&
                (cached.accuracy <= 500f || cached.accuracy == 0f)
            if (cachedIsFresh) return cached

            // La ubicación en vivo tiene un límite: la pantalla no queda esperando indefinidamente.
            val fineGranted = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val priority = if (fineGranted) Priority.PRIORITY_HIGH_ACCURACY
                else Priority.PRIORITY_BALANCED_POWER_ACCURACY

            withTimeoutOrNull(10_000L) {
                suspendCancellableCoroutine<Location?> { continuation ->
                    val cancellation = CancellationTokenSource()
                    continuation.invokeOnCancellation { cancellation.cancel() }
                    fusedLocationClient.getCurrentLocation(priority, cancellation.token)
                        .addOnSuccessListener { location ->
                            if (continuation.isActive) continuation.resume(
                                location?.takeIf {
                                    it.time > 0L && System.currentTimeMillis() - it.time <= 600_000L
                                }
                            )
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "Error getting current location: ${exception.message}")
                            if (continuation.isActive) continuation.resume(null)
                        }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception accessing FusedLocation: ${e.message}")
            null
        }
    }
}
