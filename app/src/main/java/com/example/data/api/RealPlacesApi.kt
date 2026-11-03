package com.example.data.api

import android.content.Context
import android.util.Log
import com.example.data.SampleData
import com.example.model.ComboDeal
import com.example.model.ProductCategory
import com.example.model.Store
import com.example.model.StoreProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class RealLocationResult(
    val name: String,
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val type: String
)

object RealPlacesApi {
    private const val TAG = "RealPlacesApi"

    /**
     * Busca cualquier lugar real en el mundo usando OpenStreetMap Nominatim
     */
    suspend fun searchRealLocation(query: String): List<RealLocationResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val urlString = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=6&addressdetails=1"

            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "OmniAndroidApp/1.0 (santiago.rossi@email.com)")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }
                val jsonArray = JSONArray(response)
                val results = mutableListOf<RealLocationResult>()

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val name = item.optString("name").ifBlank {
                        item.optString("display_name").split(",").firstOrNull() ?: query
                    }
                    val displayName = item.optString("display_name")
                    val lat = item.getDouble("lat")
                    val lon = item.getDouble("lon")
                    val cat = item.optString("category", "place")
                    val type = item.optString("type", "location")

                    results.add(
                        RealLocationResult(
                            name = name,
                            displayName = displayName,
                            latitude = lat,
                            longitude = lon,
                            category = cat,
                            type = type
                        )
                    )
                }
                results
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in searchRealLocation: ${e.message}")
            emptyList()
        }
    }

    data class LocationDetails(
        val city: String,
        val neighborhood: String,
        val fullAddress: String
    )

    val knownCities = mapOf(
        "romang" to Pair(-29.5052, -59.7562),
        "reconquista" to Pair(-29.1444, -59.6457),
        "san javier" to Pair(-30.5833, -59.9333),
        "malabrigo" to Pair(-29.3500, -59.8167),
        "avellaneda" to Pair(-29.1172, -59.6583),
        "alejandra" to Pair(-30.0167, -59.8000),
        "vera" to Pair(-29.4594, -60.2144),
        "santa fe" to Pair(-31.6333, -60.7000),
        "santa fe capital" to Pair(-31.6333, -60.7000),
        "rosario" to Pair(-32.9468, -60.6393),
        "buenos aires" to Pair(-34.6037, -58.3816),
        "caba" to Pair(-34.6037, -58.3816),
        "capital federal" to Pair(-34.6037, -58.3816),
        "cordoba" to Pair(-31.4201, -64.1888),
        "córdoba" to Pair(-31.4201, -64.1888),
        "mendoza" to Pair(-32.8895, -68.8458),
        "mar del plata" to Pair(-38.0055, -57.5562),
        "la plata" to Pair(-34.9214, -57.9545),
        "salta" to Pair(-24.7821, -65.4232),
        "san juan" to Pair(-31.5375, -68.5364),
        "tucuman" to Pair(-26.8083, -65.2176),
        "tucumán" to Pair(-26.8083, -65.2176),
        "bariloche" to Pair(-41.1335, -71.3103),
        "neuquen" to Pair(-38.9516, -68.0591),
        "neuquén" to Pair(-38.9516, -68.0591),
        "bahia blanca" to Pair(-38.7196, -62.2724),
        "posadas" to Pair(-27.3671, -55.8961),
        "corrientes" to Pair(-27.4692, -58.8306),
        "parana" to Pair(-31.7320, -60.5290),
        "paran\u00e1" to Pair(-31.7320, -60.5290),
        "resistencia" to Pair(-27.4514, -58.9866),
        "jujuy" to Pair(-24.1858, -65.2995),
        "carlos paz" to Pair(-31.4241, -64.4978),
        "villa maria" to Pair(-32.4075, -63.2402),
        "rio cuarto" to Pair(-33.1307, -64.3499),
        "san luis" to Pair(-33.2950, -66.3356),
        "obelisco" to Pair(-34.6037, -58.3816),
        "palermo" to Pair(-34.5885, -58.4112),
        "retiro" to Pair(-34.5922, -58.3753)
    )

    fun resolveKnownCoordinates(query: String): Pair<Double, Double>? {
        val qClean = query.lowercase().trim()
        knownCities.forEach { (key, coords) ->
            if (qClean.contains(key)) return coords
        }
        return null
    }

    suspend fun resolveLocationCoordinates(query: String): Pair<Double, Double>? {
        if (query.isBlank()) return null
        val search = searchRealLocation(query)
        if (search.isNotEmpty()) return Pair(search.first().latitude, search.first().longitude)
        return resolveKnownCoordinates(query)
    }

    /**
     * Obtiene la ciudad, barrio y dirección legible a partir de coordenadas GPS
     */
    suspend fun reverseGeocodeDetails(latitude: Double, longitude: Double): LocationDetails = withContext(Dispatchers.IO) {
        try {
            val urlString = "https://nominatim.openstreetmap.org/reverse?lat=$latitude&lon=$longitude&format=json&addressdetails=1"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "OmniAndroidApp/1.0 (santiago.rossi@email.com)")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val json = JSONObject(reader.use { it.readText() })
                val displayName = json.optString("display_name")
                val address = json.optJSONObject("address")
                if (address != null) {
                    val road = address.optString("road", "")
                    val houseNumber = address.optString("house_number", "")
                    val suburb = address.optString("suburb", address.optString("neighbourhood", ""))
                    val town = address.optString("town", "")
                    val village = address.optString("village", "")
                    val municipality = address.optString("municipality", "")
                    val city = address.optString("city", "")
                    val hamlet = address.optString("hamlet", "")
                    val county = address.optString("county", "")
                    val state = address.optString("state", "")

                    // Priorizar pueblo/municipio sobre el nombre de la provincia (state)
                    val rawDetected = when {
                        town.isNotBlank() -> town
                        village.isNotBlank() -> village
                        municipality.isNotBlank() -> municipality
                        city.isNotBlank() -> city
                        hamlet.isNotBlank() -> hamlet
                        county.isNotBlank() && !county.contains("departamento", ignoreCase = true) -> county
                        else -> state.ifBlank { "Ubicación cercana" }
                    }

                    val finalCity = rawDetected
                    val fullAddr = if (road.isNotBlank()) {
                        listOf("$road $houseNumber".trim(), suburb, finalCity).filter { it.isNotBlank() }.joinToString(", ")
                    } else {
                        displayName.split(",").take(3).joinToString(",").ifBlank { finalCity }
                    }

                    return@withContext LocationDetails(
                        city = finalCity,
                        neighborhood = if (suburb.isNotBlank()) suburb else finalCity,
                        fullAddress = fullAddr
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in reverseGeocodeDetails: ${e.message}")
        }
        LocationDetails("Ubicación desconocida", "", "Ubicación no identificada")
    }

    suspend fun reverseGeocode(latitude: Double, longitude: Double): String = withContext(Dispatchers.IO) {
        reverseGeocodeDetails(latitude, longitude).fullAddress
    }

    /**
     * Busca comercios y locales reales cercanos a las coordenadas del usuario.
     * MOTOR PRIMARIO: Google Places API (New) con clave de demostración / producción.
     * MOTOR DE RESPALDO: OpenStreetMap (Overpass) conservado como contingencia.
     */
    suspend fun fetchNearbyRealStores(
        userLat: Double,
        userLon: Double,
        radiusMeters: Int = 3000,
        localityName: String = "Romang",
        context: Context? = null
    ): List<Store> = withContext(Dispatchers.IO) {
        val cleanLocality = localityName.split(",").firstOrNull()?.trim()?.ifBlank { "Romang" } ?: "Romang"

        // 1. MOTOR PRIMARIO: Google Places API (New)
        if (context != null) {
            val googleStores = GooglePlacesApi.searchNearbyGooglePlaces(context, userLat, userLon, radiusMeters, cleanLocality)
            if (googleStores.isNotEmpty()) {
                // Devolver estrictamente los comercios reales encontrados por Google Maps en la ubicación y radio activo
                return@withContext googleStores.sortedBy { it.distanceMeters }
            }
        }

        // 2. Base DEMO local: solo se permite en Romang y nunca se renombra
        // para simular comercios de otra localidad. Las distancias del catálogo
        // también deben respetar el radio solicitado.
        val isRomangArea = cleanLocality.contains("romang", ignoreCase = true) ||
            calculateDistanceMeters(userLat, userLon, -29.5052, -59.7562) <= 30000.0
        val safeRadius = radiusMeters.coerceIn(300, 15000)
        val baseStores = if (isRomangArea) {
            SampleData.sampleStores.filter { it.distanceMeters in 1..safeRadius }
        } else {
            emptyList()
        }

        // 3. Respaldo OSM: solo lugares encontrados dentro del radio real.
        val osmBackup = fetchOsmStoresBackup(userLat, userLon, safeRadius, cleanLocality)
        (baseStores + osmBackup)
            .filter { it.distanceMeters in 1..safeRadius }
            .distinctBy { it.id.lowercase() }
            .sortedBy { it.distanceMeters }
    }

    /** Motor de respaldo sobre OpenStreetMap / Overpass (anulado como primario pero conservado) */
    suspend fun fetchOsmStoresBackup(
        userLat: Double,
        userLon: Double,
        radiusMeters: Int = 3000,
        localityName: String = "Romang"
    ): List<Store> = withContext(Dispatchers.IO) {
        val detectedStores = mutableListOf<Store>()
        val safeRadius = radiusMeters.coerceIn(300, 15000)
        val cleanLocality = localityName.split(",").firstOrNull()?.trim()?.ifBlank { "Romang" } ?: "Romang"
        val query = "[out:json][timeout:15];(node[\"shop\"](around:$safeRadius,$userLat,$userLon);way[\"shop\"](around:$safeRadius,$userLat,$userLon);relation[\"shop\"](around:$safeRadius,$userLat,$userLon);node[\"amenity\"~\"pharmacy|marketplace|bank|fuel|restaurant|cafe|fast_food\"](around:$safeRadius,$userLat,$userLon);way[\"amenity\"~\"pharmacy|marketplace|bank|fuel|restaurant|cafe|fast_food\"](around:$safeRadius,$userLat,$userLon););out center tags;"

        val endpoints = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
            "https://overpass.private.coffee/api/interpreter"
        )
        for (endpoint in endpoints) {
            try {
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 6000
                    readTimeout = 14000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    setRequestProperty("User-Agent", "OMNI-Android/1.0 (Nearby backup directory)")
                }
                conn.outputStream.use { it.write("data=".toByteArray() + URLEncoder.encode(query, "UTF-8").toByteArray()) }
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                    val elements = json.optJSONArray("elements") ?: JSONArray()
                    for (i in 0 until elements.length()) {
                        val elem = elements.getJSONObject(i)
                        val tags = elem.optJSONObject("tags") ?: continue
                        val name = tags.optString("name").ifBlank { tags.optString("name:es") }.ifBlank { tags.optString("brand") }.ifBlank { tags.optString("operator") }.trim()
                        if (name.isBlank()) continue
                        val center = elem.optJSONObject("center")
                        val lat = center?.optDouble("lat") ?: elem.optDouble("lat", Double.NaN)
                        val lon = center?.optDouble("lon") ?: elem.optDouble("lon", Double.NaN)
                        if (!lat.isFinite() || !lon.isFinite()) continue
                        val distance = calculateDistanceMeters(userLat, userLon, lat, lon).toInt()
                        if (distance > safeRadius) continue

                        val shop = tags.optString("shop")
                        val amenity = tags.optString("amenity")
                        val type = when {
                            amenity == "pharmacy" || shop == "chemist" -> com.example.model.StoreType.FARMACIA
                            shop == "bakery" -> com.example.model.StoreType.PANADERIA
                            shop == "kiosk" -> com.example.model.StoreType.KIOSCO
                            shop == "supermarket" || shop == "convenience" || shop == "grocery" || amenity == "marketplace" -> com.example.model.StoreType.SUPERMERCADO
                            shop == "greengrocer" || shop == "health_food" -> com.example.model.StoreType.TIENDA_NATURAL
                            else -> com.example.model.StoreType.ALMACEN
                        }
                        val street = tags.optString("addr:street")
                        val number = tags.optString("addr:housenumber")
                        val address = when {
                            street.isNotBlank() && number.isNotBlank() -> "$street $number, $cleanLocality"
                            street.isNotBlank() -> "$street, $cleanLocality"
                            tags.optString("addr:place").isNotBlank() -> "${tags.optString("addr:place")}, $cleanLocality"
                            else -> "Dirección no informada · $cleanLocality"
                        }
                        val hours = tags.optString("opening_hours").ifBlank { "Horario no informado" }
                        val isTwentyFour = hours.contains("24/7", ignoreCase = true)
                        val phone = tags.optString("phone").ifBlank { tags.optString("contact:phone") }
                        val description = buildString {
                            append("Local registrado. ")
                            if (phone.isNotBlank()) append("Teléfono: $phone. ")
                            append("Horarios y servicios informados por el comercio.")
                        }
                        val isPharm = type == com.example.model.StoreType.FARMACIA
                        val storeSeed = kotlin.math.abs(name.hashCode())
                        val hoursDisplay = if (isTwentyFour) "DE TURNO 24HS • Guardia Activa" else if (hours.isNotBlank() && hours != "Horario no informado") hours else "Abierto de 08:30 a 20:30 hs"
                        detectedStores.add(
                            Store(
                                id = "backup_${elem.optString("type")}_${elem.optLong("id")}",
                                name = name,
                                type = type,
                                rating = 4.5f + ((storeSeed % 5) * 0.1f),
                                reviewsCount = 25 + (storeSeed % 140),
                                distanceMeters = distance.coerceAtLeast(40),
                                address = address,
                                isOpen = true,
                                hoursText = hoursDisplay,
                                accentColorHex = if (isPharm) 0xFF0D9488 else 0xFF059669,
                                photosCount = 4 + (storeSeed % 6),
                                hasVideoTour = true,
                                videoTourDuration = "0:30",
                                description = description,
                                specialOfferText = "10% reintegro abonando con OmniPay QR",
                                minSpendDiscountThreshold = 5000.0,
                                minSpendDiscountPercent = 10,
                                comboDeals = listOf(ComboDeal("Ahorro Barrial", "Descuento en compras", "-10%")),
                                inventory = SampleData.generateInventoryForStore(type, storeSeed),
                                mapOffsetX = 0.5f,
                                mapOffsetY = 0.5f,
                                openTime = if (isTwentyFour) "00:00" else "08:30",
                                closeTime = if (isTwentyFour) "23:59" else "20:30",
                                weekendHoursText = "Atención regular",
                                is24Hours = isTwentyFour,
                                isOnDuty = isPharm,
                                openOnHolidays = isTwentyFour,
                                scheduleNotes = "Fuente: Directorio regional · $cleanLocality"
                            )
                        )
                    }
                    break
                }
            } catch (e: Exception) {
                Log.w(TAG, "OSM backup endpoint falló: $endpoint · ${e.message}")
            }
        }
        detectedStores
    }

    data class DrivingRouteResult(val distanceKm: Double, val durationMinutes: Int)

    /** Distancia y duración de conducción calculadas sobre la red vial de OpenStreetMap (OSRM). */
    suspend fun fetchDrivingRoute(
        originLat: Double,
        originLon: Double,
        destinationLat: Double,
        destinationLon: Double
    ): DrivingRouteResult? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://router.project-osrm.org/route/v1/driving/$originLon,$originLat;$destinationLon,$destinationLat?overview=false")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 12000
                setRequestProperty("User-Agent", "OMNI-Android/1.0")
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val route = json.optJSONArray("routes")?.optJSONObject(0) ?: return@withContext null
            val distanceMeters = route.optDouble("distance", 0.0)
            val durationSeconds = route.optDouble("duration", 0.0)
            if (distanceMeters <= 0.0 || durationSeconds <= 0.0) null
            else DrivingRouteResult(distanceMeters / 1000.0, (durationSeconds / 60.0).toInt().coerceAtLeast(1))
        } catch (e: Exception) {
            Log.e(TAG, "Error calculando ruta vial: ${e.message}")
            null
        }
    }

    /**
     * Calcula la distancia en metros entre dos puntos satelitales usando fórmula de Haversine
     */
    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Radio de la Tierra en metros
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
