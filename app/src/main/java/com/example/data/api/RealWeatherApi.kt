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

data class WeatherHour(
    val time: String,
    val temperatureC: Double,
    val precipitationProbability: Int,
    val weatherCode: Int,
    val windSpeedKmh: Double,
    val apparentTemperatureC: Double = temperatureC,
    val uvIndex: Double = 0.0
)

data class WeatherDay(
    val date: String,
    val minC: Double,
    val maxC: Double,
    val precipitationProbability: Int,
    val weatherCode: Int,
    val sunrise: String,
    val sunset: String
)

data class RealWeatherData(
    val temperatureC: Double,
    val apparentTemperatureC: Double,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val weatherDescription: String,
    val weatherCode: Int,
    val precipitationMm: Double,
    val isDay: Boolean,
    val locationName: String,
    val observedAt: String,
    val hourly: List<WeatherHour>,
    val daily: List<WeatherDay>
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
                     "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,rain,showers,weather_code,wind_speed_10m" +
                    "&hourly=temperature_2m,apparent_temperature,precipitation_probability,weather_code,wind_speed_10m,uv_index" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset" +
                    "&forecast_days=7&timezone=auto"

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

                val rainNow = current.optDouble("rain", 0.0) + current.optDouble("showers", 0.0)
                val desc = if (rainNow >= 0.1 || rain >= 0.1) "Lloviendo" else getWeatherDescription(code)
                val hourlyJson = json.optJSONObject("hourly")
                val dailyJson = json.optJSONObject("daily")
                val hourlyTimes = hourlyJson?.optJSONArray("time")
                val hourlyTemps = hourlyJson?.optJSONArray("temperature_2m")
                val hourlyRain = hourlyJson?.optJSONArray("precipitation_probability")
                val hourlyCodes = hourlyJson?.optJSONArray("weather_code")
                val hourlyWind = hourlyJson?.optJSONArray("wind_speed_10m")
                val hourlyApparent = hourlyJson?.optJSONArray("apparent_temperature")
                val hourlyUv = hourlyJson?.optJSONArray("uv_index")
                val hourly = mutableListOf<WeatherHour>()
                val hourCount = minOf(hourlyTimes?.length() ?: 0, hourlyTemps?.length() ?: 0)
                for (i in 0 until hourCount) {
                    hourly.add(WeatherHour(
                        time = hourlyTimes?.optString(i).orEmpty(),
                        temperatureC = hourlyTemps?.optDouble(i) ?: temp,
                        precipitationProbability = hourlyRain?.optInt(i) ?: 0,
                        weatherCode = hourlyCodes?.optInt(i) ?: code,
                        windSpeedKmh = hourlyWind?.optDouble(i) ?: wind,
                        apparentTemperatureC = hourlyApparent?.optDouble(i) ?: (hourlyTemps?.optDouble(i) ?: temp),
                        uvIndex = hourlyUv?.optDouble(i) ?: 0.0
                    ))
                }
                val dailyTimes = dailyJson?.optJSONArray("time")
                val dailyMin = dailyJson?.optJSONArray("temperature_2m_min")
                val dailyMax = dailyJson?.optJSONArray("temperature_2m_max")
                val dailyRain = dailyJson?.optJSONArray("precipitation_probability_max")
                val dailyCodes = dailyJson?.optJSONArray("weather_code")
                val sunrise = dailyJson?.optJSONArray("sunrise")
                val sunset = dailyJson?.optJSONArray("sunset")
                val days = mutableListOf<WeatherDay>()
                val dayCount = dailyTimes?.length() ?: 0
                for (i in 0 until dayCount) {
                    days.add(WeatherDay(
                        date = dailyTimes?.optString(i).orEmpty(),
                        minC = dailyMin?.optDouble(i) ?: temp,
                        maxC = dailyMax?.optDouble(i) ?: temp,
                        precipitationProbability = dailyRain?.optInt(i) ?: 0,
                        weatherCode = dailyCodes?.optInt(i) ?: code,
                        sunrise = sunrise?.optString(i).orEmpty(),
                        sunset = sunset?.optString(i).orEmpty()
                    ))
                }

                Result.success(
                    RealWeatherData(
                        temperatureC = temp,
                        apparentTemperatureC = apparent,
                        humidityPercent = humidity,
                        windSpeedKmh = wind,
                        weatherDescription = desc,
                        weatherCode = code,
                        precipitationMm = maxOf(rain, rainNow),
                        isDay = isDay,
                        locationName = locationLabel,
                        observedAt = current.optString("time", ""),
                        hourly = hourly,
                        daily = days
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
            0 -> "Despejado"
            1 -> "Mayormente despejado"
            2 -> "Parcialmente nublado"
            3 -> "Nublado"
            45, 48 -> "Niebla o neblina"
            51, 53, 55 -> "Llovizna leve"
            56, 57 -> "Llovizna engelante"
            61, 63 -> "Lluvia moderada"
            65 -> "Lluvia intensa"
            66, 67 -> "Lluvia engelante"
            71, 73, 75, 77, 85, 86 -> "Nevada"
            80, 81, 82 -> "Chubascos de lluvia"
            95 -> "Tormenta eléctrica"
            96, 99 -> "Tormenta eléctrica con granizo"
            else -> "Tiempo variable"
        }
    }
}
