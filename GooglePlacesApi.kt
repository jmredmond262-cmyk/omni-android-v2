package com.example.data.api

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.SampleData
import com.example.model.ComboDeal
import com.example.model.Store
import com.example.model.StoreType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Cliente oficial para Google Places API (New)
 * Endpoint: https://places.googleapis.com/v1/places:searchNearby
 * Proporciona comercios, datos reales, fotos, calificaciones y horarios de Google Maps.
 */
object GooglePlacesApi {
    private const val TAG = "GooglePlacesApi"
    private const val SEARCH_NEARBY_URL = "https://places.googleapis.com/v1/places:searchNearby"
    private const val SEARCH_TEXT_URL = "https://places.googleapis.com/v1/places:searchText"

    private const val FIELD_MASK = "places.id,places.displayName,places.formattedAddress,places.location,places.rating,places.userRatingCount,places.currentOpeningHours,places.primaryType,places.types,places.nationalPhoneNumber,places.googleMapsUri,places.photos"

    /**
     * Obtiene la clave de Google Maps configurada en BuildConfig (vía .env / Secrets panel)
     * o guardada en SharedPreferences de demostración.
     */
    fun getApiKey(context: Context? = null): String {
        // 1. Clave guardada en SharedPreferences de la app
        if (context != null) {
            val prefsKey = context.getSharedPreferences("omni_api_keys", Context.MODE_PRIVATE)
                .getString("google_places_demo_key", "") ?: ""
            if (prefsKey.isNotBlank()) return prefsKey.trim()
        }

        // 2. Clave opcional de BuildConfig (si estuviera inyectada en tiempo de compilación)
        try {
            val field = BuildConfig::class.java.getField("GOOGLE_MAPS_API_KEY")
            val key = field.get(null) as? String
            if (!key.isNullOrBlank() && !key.startsWith("YOUR_") && !key.startsWith("MY_")) {
                return key.trim()
            }
        } catch (_: Throwable) {
            // No inyectada vía Secrets plugin
        }

        // 3. Clave de demostración activa provista por el usuario
        return "AIzaSyApzwMwuoqteI9cqJLJFZDJtM7jjgoPORA"
    }

    /**
     * Guarda una clave de prueba de demostración provista por el usuario
     */
    fun saveDemoApiKey(context: Context, key: String) {
        context.getSharedPreferences("omni_api_keys", Context.MODE_PRIVATE)
            .edit()
            .putString("google_places_demo_key", key.trim())
            .apply()
    }

    /**
     * Busca comercios reales en Google Maps mediante Google Places API (New).
     */
    suspend fun searchNearbyGooglePlaces(
        context: Context,
        userLat: Double,
        userLon: Double,
        radiusMeters: Int = 3000,
        localityName: String = "Romang"
    ): List<Store> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context)
        if (apiKey.isBlank()) {
            Log.d(TAG, "No se encontró GOOGLE_MAPS_API_KEY activa. Usando comercios base locales de $localityName.")
            return@withContext emptyList()
        }

        val safeRadius = radiusMeters.coerceIn(300, 40000).toDouble()
        val stores = mutableListOf<Store>()

        try {
            val url = URL(SEARCH_NEARBY_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 12000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("X-Goog-Api-Key", apiKey)
                setRequestProperty("X-Goog-FieldMask", FIELD_MASK)
            }

            val requestBody = JSONObject().apply {
                put("maxResultCount", 20)
                put("includedTypes", JSONArray(listOf(
                    "store", "supermarket", "grocery_store", "convenience_store",
                    "bakery", "pharmacy", "restaurant", "cafe", "meal_takeaway",
                    "bank", "gas_station", "hardware_store", "electronics_store",
                    "clothing_store", "car_repair"
                )))
                val center = JSONObject().apply {
                    put("latitude", userLat)
                    put("longitude", userLon)
                }
                val circle = JSONObject().apply {
                    put("center", center)
                    put("radius", safeRadius)
                }
                put("locationRestriction", JSONObject().apply {
                    put("circle", circle)
                })
            }

            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(responseText)
                val placesArray = root.optJSONArray("places") ?: JSONArray()

                for (i in 0 until placesArray.length()) {
                    val placeObj = placesArray.getJSONObject(i)
                    val store = parseGooglePlace(placeObj, userLat, userLon, localityName, apiKey)
                    if (store != null) {
                        stores.add(store)
                    }
                }
                Log.d(TAG, "Google Places API (New) retornó ${stores.size} comercios reales en $localityName")
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.w(TAG, "Google Places API (New) error HTTP $responseCode: $errorStream")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción consultando Google Places API (New): ${e.message}", e)
        }

        stores.filter { it.distanceMeters <= (safeRadius * 1.25).toInt() }.sortedBy { it.distanceMeters }
    }

    /**
     * Parsea un objeto Place devuelto por Google Places API (New) a nuestro modelo Store.
     */
    private fun parseGooglePlace(
        place: JSONObject,
        userLat: Double,
        userLon: Double,
        localityName: String,
        apiKey: String
    ): Store? {
        val placeId = place.optString("id")
        if (placeId.isBlank()) return null

        val displayNameObj = place.optJSONObject("displayName")
        val name = displayNameObj?.optString("text")?.ifBlank { "Comercio" } ?: "Comercio"

        val locationObj = place.optJSONObject("location")
        val lat = locationObj?.optDouble("latitude") ?: Double.NaN
        val lon = locationObj?.optDouble("longitude") ?: Double.NaN
        if (!lat.isFinite() || !lon.isFinite()) return null

        val distance = calculateDistanceMeters(userLat, userLon, lat, lon).toInt().coerceAtLeast(30)
        val formattedAddress = place.optString("formattedAddress").ifBlank { "Dirección en $localityName" }

        val primaryType = place.optString("primaryType")
        val typesArray = place.optJSONArray("types") ?: JSONArray()
        val typesList = mutableListOf<String>()
        for (j in 0 until typesArray.length()) {
            typesList.add(typesArray.getString(j))
        }

        val mappedType = mapGoogleTypeToStoreType(name, primaryType, typesList)
        val rating = place.optDouble("rating", 4.7).toFloat()
        val userRatingCount = place.optInt("userRatingCount", 28)

        val openingHoursObj = place.optJSONObject("currentOpeningHours")
        val openNow = openingHoursObj?.optBoolean("openNow", true) ?: true
        val weekdayDescArray = openingHoursObj?.optJSONArray("weekdayDescriptions")
        val hoursText = if (weekdayDescArray != null && weekdayDescArray.length() > 0) {
            weekdayDescArray.getString(0)
        } else if (openNow) {
            "Abierto en horario habitual"
        } else {
            "Cerrado en este momento"
        }

        val isPharm = mappedType == StoreType.FARMACIA
        val is24Hours = hoursText.contains("24 horas", true) || hoursText.contains("24/7", true) || isPharm
        val phone = place.optString("nationalPhoneNumber")
        val googleMapsUri = place.optString("googleMapsUri")
        val photosArray = place.optJSONArray("photos")
        val photosCount = (photosArray?.length() ?: 0).coerceAtLeast(4)
        val firstPhotoObj = photosArray?.optJSONObject(0)
        val photoResourceName = firstPhotoObj?.optString("name")
        val realPhotoUrl = if (!photoResourceName.isNullOrBlank()) {
            "https://places.googleapis.com/v1/$photoResourceName/media?key=$apiKey&maxHeightPx=400&maxWidthPx=600"
        } else null

        val seed = abs(placeId.hashCode())

        return Store(
            id = "gplace_$placeId",
            name = name,
            type = mappedType,
            rating = rating,
            reviewsCount = userRatingCount,
            distanceMeters = distance,
            address = formattedAddress,
            isOpen = openNow,
            hoursText = if (is24Hours) "DE TURNO 24HS • Guardia Activa" else hoursText,
            accentColorHex = if (isPharm) 0xFF0D9488 else if (mappedType == StoreType.GASTRONOMIA) 0xFF00E676 else 0xFF059669,
            photosCount = photosCount,
            hasVideoTour = true,
            videoTourDuration = "0:30",
            description = buildString {
                append("Comercio verificado en Google Maps. ")
                if (phone.isNotBlank()) append("Tel: $phone. ")
                append("Dirección: $formattedAddress.")
            },
            specialOfferText = "10% reintegro abonando con OmniPay QR",
            minSpendDiscountThreshold = 5000.0,
            minSpendDiscountPercent = 10,
            comboDeals = listOf(ComboDeal("Ahorro Barrial", "Descuento en compras con QR", "-10%")),
            inventory = SampleData.generateInventoryForStore(mappedType, seed),
            mapOffsetX = 0.5f,
            mapOffsetY = 0.5f,
            openTime = if (is24Hours) "00:00" else "08:30",
            closeTime = if (is24Hours) "23:59" else "20:30",
            weekendHoursText = "Atención según Google Maps",
            is24Hours = is24Hours,
            isOnDuty = isPharm,
            openOnHolidays = is24Hours,
            scheduleNotes = "Fuente oficial: Google Places API (New) • Google Maps",
            phone = phone,
            photoUrl = realPhotoUrl,
            googleMapsUri = googleMapsUri
        )
    }

    private fun mapGoogleTypeToStoreType(name: String, primaryType: String, types: List<String>): StoreType {
        val n = name.lowercase()
        if (n.contains("redcom") || n.contains("computacion") || n.contains("informatica")) return StoreType.ALMACEN
        if (n.contains("farmacia")) return StoreType.FARMACIA
        if (n.contains("panaderia") || n.contains("panadería")) return StoreType.PANADERIA
        if (n.contains("supermercado") || n.contains("coop")) return StoreType.SUPERMERCADO
        if (n.contains("kiosco") || n.contains("drugstore")) return StoreType.KIOSCO
        if (n.contains("rotiser") || n.contains("pizza") || n.contains("parrilla") || n.contains("restauran") || n.contains("comida") || n.contains("bar") || n.contains("heladeria")) return StoreType.GASTRONOMIA
        if (n.contains("taller") || n.contains("mecanic") || n.contains("plomer") || n.contains("gas") || n.contains("electric")) return StoreType.SERVICIOS

        val all = (listOf(primaryType) + types).map { it.lowercase() }
        return when {
            all.any { it.contains("pharmacy") || it.contains("drugstore") } -> StoreType.FARMACIA
            all.any { it.contains("bakery") } -> StoreType.PANADERIA
            all.any { it.contains("restaurant") || it.contains("food") || it.contains("meal") || it.contains("bar") || it.contains("cafe") } -> StoreType.GASTRONOMIA
            all.any { it.contains("service") || it.contains("repair") || it.contains("hair") || it.contains("beauty") } -> StoreType.SERVICIOS
            all.any { it.contains("kiosk") || it.contains("convenience") || it.contains("newsagent") } -> StoreType.KIOSCO
            all.any { it.contains("supermarket") || it.contains("grocery") } -> StoreType.SUPERMERCADO
            all.any { it.contains("health") || it.contains("organic") || it.contains("natural") } -> StoreType.TIENDA_NATURAL
            else -> StoreType.ALMACEN
        }
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // metros
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
