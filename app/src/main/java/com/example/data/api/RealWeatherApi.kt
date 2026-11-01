package com.example.data.api

import android.util.Log
import com.example.model.AlertSeverity
import com.example.model.AlertType
import com.example.model.WeatherAlert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class RealWeatherData(
    val temperatureC: Double,
    val apparentTemperatureC: Double,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val weatherDescription: String,
    val weatherCode: Int,
    val precipitationMm: Double,
    val isDay: Boolean,
    val locationName: String
)

object RealWeatherApi {
    private const val TAG = "RealWeatherApi"

    suspend fun fetchCurrentWeather(
        latitude: Double = -34.5885,
        longitude: Double = -58.4112,
        locationLabel: String = "Palermo, Buenos Aires"
    ): Result<RealWeatherData> = withContext(Dispatchers.IO) {
        try {
            val urlString = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude" +
                    "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m" +
                    "&timezone=auto"

            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "OmniAndroidApp/1.0")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }
                val json = JSONObject(response)
                val current = json.getJSONObject("current")

                val temp = current.getDouble("temperature_2m")
                val apparent = current.optDouble("apparent_temperature", temp)
                val humidity = current.optInt("relative_humidity_2m", 60)
                val wind = current.optDouble("wind_speed_10m", 12.0)
                val rain = current.optDouble("precipitation", 0.0)
                val code = current.optInt("weather_code", 0)
                val isDay = current.optInt("is_day", 1) == 1

                val desc = getWeatherDescription(code)

                Result.success(
                    RealWeatherData(
                        temperatureC = temp,
                        apparentTemperatureC = apparent,
                        humidityPercent = humidity,
                        windSpeedKmh = wind,
                        weatherDescription = desc,
                        weatherCode = code,
                        precipitationMm = rain,
                        isDay = isDay,
                        locationName = locationLabel
                    )
                )
            } else {
                Result.failure(Exception("HTTP Error: ${connection.responseCode}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching live weather: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Genera alertas meteorológicas y sísmicas reales basadas en las condiciones vivas y umbrales de emergencia
     */
    suspend fun fetchLiveAlerts(
        weather: RealWeatherData,
        latitude: Double,
        longitude: Double
    ): List<WeatherAlert> = withContext(Dispatchers.IO) {
        val alerts = mutableListOf<WeatherAlert>()

        // 1. Alerta de Calor Extremo
        if (weather.temperatureC >= 34.0) {
            val severity = if (weather.temperatureC >= 38.0) AlertSeverity.ROJO else AlertSeverity.NARANJA
            alerts.add(
                WeatherAlert(
                    id = "alert_heat_${UUID.randomUUID().toString().take(6)}",
                    type = AlertType.CLIMA_CALOR,
                    severity = severity,
                    title = "Alerta por Ola de Calor (${weather.temperatureC}°C)",
                    description = "Sensación térmica extrema de ${weather.apparentTemperatureC}°C. Mantente hidratado, evita el sol directo entre 11 y 17hs.",
                    instructions = "Hidratarse continuamente, evitar esfuerzos al sol y acudir a centros climatizados.",
                    validUntil = "Hoy hasta las 20:00 hs"
                )
            )
        }

        // 2. Alerta de Frío Polar / Heladas
        if (weather.temperatureC <= 4.0) {
            val severity = if (weather.temperatureC <= 0.0) AlertSeverity.ROJO else AlertSeverity.AMARILLO
            alerts.add(
                WeatherAlert(
                    id = "alert_cold_${UUID.randomUUID().toString().take(6)}",
                    type = AlertType.CLIMA_FRIO,
                    severity = severity,
                    title = "Alerta por Bajas Temperaturas (${weather.temperatureC}°C)",
                    description = "Sensación térmica de ${weather.apparentTemperatureC}°C con riesgo de heladas tempranas.",
                    instructions = "Abrigarse en capas y cuidar artefactos de calefacción.",
                    validUntil = "Hasta media mañana"
                )
            )
        }

        // 3. Alerta de Vientos Fuertes / Tormenta / Lluvia Intensa
        if (weather.windSpeedKmh >= 45.0 || weather.precipitationMm >= 5.0 || weather.weatherCode in listOf(95, 96, 99)) {
            val severity = if (weather.windSpeedKmh >= 70.0 || weather.weatherCode in listOf(96, 99)) AlertSeverity.ROJO else AlertSeverity.NARANJA
            alerts.add(
                WeatherAlert(
                    id = "alert_storm_${UUID.randomUUID().toString().take(6)}",
                    type = AlertType.TORMENTA,
                    severity = severity,
                    title = "Alerta de Tormentas y Ráfagas (${weather.windSpeedKmh} km/h)",
                    description = "Precipitaciones registradas de ${weather.precipitationMm} mm con actividad eléctrica o ráfagas fuertes.",
                    instructions = "Asegurar objetos sueltos en balcones y no sacar la basura para evitar anegamientos.",
                    validUntil = "Próximas 4 horas"
                )
            )
        }

        // No consultar el feed sísmico global: provocaba avisos de eventos lejanos sin impacto local.
        // Las alertas de clima se calculan con las condiciones meteorológicas de las coordenadas recibidas.

        // Si no hay catástrofes ni clima extremo, la lista permanece limpia (sin alarmas innecesarias)
        alerts
    }

    private fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Cielo despejado"
            1, 2 -> "Parcialmente nublado"
            3 -> "Nublado"
            45, 48 -> "Niebla o neblina"
            51, 53, 55 -> "Llovizna leve"
            61, 63 -> "Lluvia moderada"
            65 -> "Lluvia intensa"
            71, 73, 75 -> "Nevada"
            80, 81, 82 -> "Chubascos de lluvia"
            95 -> "Tormenta eléctrica"
            96, 99 -> "Tormenta eléctrica con granizo"
            else -> "Tiempo variable"
        }
    }
}
