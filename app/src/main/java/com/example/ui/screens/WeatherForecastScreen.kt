package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WeatherForecastScreen(viewModel: MainViewModel) {
    val weather by viewModel.liveWeatherData.collectAsState()
    val city by viewModel.currentCity.collectAsState()
    val background = Color(0xFF07111F)
    val panel = Color(0xFF101F33)
    val cyan = Color(0xFF25D9FF)

    Column(
        modifier = Modifier.fillMaxSize().background(background).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("CLIMA EN VIVO", color = cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text(city.ifBlank { weather?.locationName ?: "Tu ubicación" }, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(onClick = { viewModel.refreshWeatherNow() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF12334A))) {
                Text("Actualizar", color = cyan, fontSize = 11.sp)
            }
        }

        if (weather == null) {
            Card(colors = CardDefaults.cardColors(containerColor = panel), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = cyan, modifier = Modifier.width(22.dp).height(22.dp), strokeWidth = 2.dp)
                    Text("Consultando datos meteorológicos…", color = Color.White, modifier = Modifier.padding(start = 12.dp), fontSize = 13.sp)
                }
            }
        } else {
            val w = weather!!
            Card(
                colors = CardDefaults.cardColors(containerColor = panel),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(weatherEmoji(w.weatherCode, w.precipitationMm), fontSize = 42.sp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("${w.temperatureC.toInt()}°", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Light)
                            Text(w.weatherDescription, color = cyan, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text("Sensación térmica ${w.apparentTemperatureC.toInt()}°  ·  Humedad ${w.humidityPercent}%", color = Color(0xFFB8C8D9), fontSize = 12.sp)
                    Text("Viento ${w.windSpeedKmh.toInt()} km/h  ·  Precipitación ${"%.1f".format(Locale.US, w.precipitationMm)} mm", color = Color(0xFFB8C8D9), fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
                    Text("Actualizado: ${w.observedAt.replace("T", " ")}", color = Color(0xFF8196AC), fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }

            Text("PRÓXIMAS HORAS", color = cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                w.hourly.filter { it.time.length >= 16 && it.time >= w.observedAt.take(13) }.take(24).forEach { hour ->
                    Card(colors = CardDefaults.cardColors(containerColor = panel), shape = RoundedCornerShape(13.dp)) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(hour.time.substring(11,16), color = Color(0xFFB8C8D9), fontSize = 11.sp)
                            Text(weatherEmoji(hour.weatherCode, 0.0), fontSize = 22.sp, modifier = Modifier.padding(vertical = 6.dp))
                            Text("${hour.temperatureC.toInt()}°", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${hour.precipitationProbability}% lluvia", color = cyan, fontSize = 9.sp)
                        }
                    }
                }
            }

            Text("PRONÓSTICO · 7 DÍAS", color = cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                w.daily.forEachIndexed { index, day ->
                    Card(colors = CardDefaults.cardColors(containerColor = panel), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (index == 0) "Hoy" else day.date.takeLast(5), color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text(weatherEmoji(day.weatherCode, 0.0), fontSize = 19.sp)
                            Text("${day.precipitationProbability}% ", color = cyan, fontSize = 11.sp, modifier = Modifier.padding(start = 10.dp))
                            Text("${day.minC.toInt()}° / ${day.maxC.toInt()}°", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Text("Pronóstico basado en modelos meteorológicos de Open-Meteo. Puede diferir de una estación de observación local.", color = Color(0xFF8196AC), fontSize = 10.sp)
        }
    }
}

private fun weatherEmoji(code: Int, precipitation: Double): String {
    if (precipitation >= 0.1) return "🌧️"
    return when (code) {
        0 -> "☀️"
        1, 2 -> "🌤️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> "🌧️"
        71, 73, 75, 77 -> "❄️"
        95, 96, 99 -> "⛈️"
        else -> "🌡️"
    }
}
