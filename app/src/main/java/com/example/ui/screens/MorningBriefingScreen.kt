package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MorningBriefingScreen(
    viewModel: MainViewModel,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val briefing by viewModel.morningBriefing.collectAsState()
    val city by viewModel.currentCity.collectAsState()
    val weather by viewModel.liveWeatherData.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val previousSummary = remember {
        val summaryPrefs = context.getSharedPreferences("omni_daily_summary", android.content.Context.MODE_PRIVATE)
        val yesterdayCalendar = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
        val yesterday = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(yesterdayCalendar.time)
        if (summaryPrefs.getString("summary_date", "") == yesterday) {
            summaryPrefs.getInt("summary_completed", 0) to summaryPrefs.getInt("summary_total", 0)
        } else null
    }
    val profile by viewModel.userProfile.collectAsState()
    val activeDeals = briefing.activeDealsCount
    val preferences = remember { context.getSharedPreferences("omni_morning_briefing", android.content.Context.MODE_PRIVATE) }
    var showWeather by remember { mutableStateOf(preferences.getBoolean("show_weather", true)) }
    var showMetrics by remember { mutableStateOf(preferences.getBoolean("show_metrics", true)) }
    var showQuote by remember { mutableStateOf(preferences.getBoolean("show_quote", true)) }
    var showYesterday by remember { mutableStateOf(preferences.getBoolean("show_yesterday", true)) }
    val todayKey = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
    val dailyQuotes = listOf(
        "Lo importante no es correr, sino avanzar con dirección.",
        "Cada día trae una oportunidad para empezar mejor.",
        "Las pequeñas decisiones construyen grandes cambios.",
        "No hace falta tener todo resuelto para dar el próximo paso.",
        "La constancia convierte las ideas en resultados.",
        "Hacé espacio para lo que realmente importa."
    )
    val dailyQuote = dailyQuotes[Math.floorMod(todayKey.hashCode(), dailyQuotes.size)]
    val rawTodayDate = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "AR")).format(Date())
    val todayDateText = rawTodayDate.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale("es", "AR")) else it.toString()
    } + " • Comienzo de día"
    val displayName = profile.fullName.trim()
        .takeUnless { it.isBlank() || it.equals("Usuario OMNI", ignoreCase = true) }
    val greeting = displayName?.let { "¡Buen día, $it! Esperamos que tengas una excelente jornada." }
        ?: "¡Buen día! Esperamos que tengas una excelente jornada."
    val weatherLine = weather?.let {
        "${it.temperatureC.toInt()}°C · ${it.weatherDescription}"
    } ?: briefing.weatherSummary
    val temperatureLine = weather?.let {
        "Sensación ${it.apparentTemperatureC.toInt()}°C · Humedad ${it.humidityPercent}%"
    } ?: briefing.temperatureRange

    val shareMessage = buildString {
        appendLine("🌅 OMNI BUENOS DÍAS")
        appendLine(todayDateText)
        appendLine("📍 ${city.ifBlank { "Tu ciudad" }}")
        if (showWeather) appendLine("🌤️ $weatherLine")
        if (showMetrics) appendLine("📌 ${tasks.count { !it.isCompleted }} tareas pendientes · ${activeDeals} ofertas activas")
        if (showQuote) appendLine("💬 $dailyQuote")
        if (showYesterday) previousSummary?.let { appendLine("📋 Ayer: " + it.first + " de " + it.second + " tareas completadas") }
        appendLine()
        append("Descubrí tu día con OMNI. Muy pronto vas a poder descargar la app.")
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF07111F)).verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF123B63), Color(0xFF111D3B), Color(0xFF241D4B))))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        Text("🌅", fontSize = 24.sp)
                    }
                    Spacer(Modifier.size(10.dp))
                    Column {
                        Text("OMNI", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                        Text("BUENOS DÍAS", color = Color(0xFF66E4FF), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    }
                }
                Text(
                    greeting,
                    color = Color.White,
                    fontSize = 25.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(todayDateText, color = Color(0xFFB9D4EB), fontSize = 12.sp)
                Text("Compartido desde tu perfil personal de OMNI", color = Color(0xFFB9D4EB), fontSize = 11.sp)
            }
        }

        if (showWeather) Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10243A)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFFFFC857))
                    Text("  EL TIEMPO EN ${city.ifBlank { "TU ZONA" }.uppercase()}", color = Color(0xFF72E4FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text(weatherLine, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(temperatureLine, color = Color(0xFFB5C7D9), fontSize = 12.sp)
                weather?.daily?.firstOrNull()?.let { day ->
                    Text("Hoy: ${day.minC.toInt()}° / ${day.maxC.toInt()}° · ${day.precipitationProbability}% prob. de lluvia", color = Color(0xFFB5C7D9), fontSize = 11.sp)
                    Text("🌅 ${day.sunrise.takeLast(5)}  ·  🌇 ${day.sunset.takeLast(5)}", color = Color(0xFFB5C7D9), fontSize = 11.sp)
                }
                val nowMillis = System.currentTimeMillis()
                val hourParser = remember { SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()) }
                val nextHours = weather?.hourly.orEmpty().mapNotNull { hour ->
                    hourParser.parse(hour.time)?.let { parsed -> parsed.time to hour }
                }.filter { it.first >= nowMillis - 30 * 60 * 1000L }.sortedBy { it.first }.take(6).map { it.second }
                val nearHours = nextHours.ifEmpty { weather?.hourly.orEmpty().take(6) }
                val nextTemps = nearHours.take(3).map { it.apparentTemperatureC }
                val minNearTemp = nextTemps.minOrNull() ?: weather?.apparentTemperatureC ?: 20.0
                val maxNearTemp = nextTemps.maxOrNull() ?: weather?.apparentTemperatureC ?: 20.0
                val rainChance = nearHours.maxOfOrNull { it.precipitationProbability } ?: 0
                val maxWind = nearHours.maxOfOrNull { it.windSpeedKmh } ?: weather?.windSpeedKmh ?: 0.0
                val severeCode = nearHours.any { it.weatherCode in listOf(95, 96, 99) }
                val hailExpected = nearHours.any { it.weatherCode in listOf(96, 99) }
                val clothing = when {
                    minNearTemp <= 10 -> "Abrigo importante y varias capas: el aire se mantendrá frío en las próximas horas."
                    minNearTemp <= 15 -> "Llevá campera o buzo; la sensación térmica puede bajar durante las próximas horas."
                    minNearTemp <= 19 -> "Ropa de media estación y una capa liviana para la noche."
                    maxNearTemp >= 29 -> "Ropa liviana y fresca; buscá sombra si seguís al aire libre."
                    else -> "Ropa cómoda acorde a la temperatura prevista para las próximas horas."
                }
                Text("👕 Próximas horas: $clothing", color = Color(0xFF72E4FF), fontSize = 11.sp)
                Text("Pronóstico utilizado: ${nearHours.size} horas desde ahora.", color = Color(0xFF8FA9C1), fontSize = 10.sp)
                if (rainChance >= 35) Text("☔ Llevá paraguas o impermeable: probabilidad de lluvia de hasta $rainChance% en las próximas horas.", color = Color(0xFFFFC978), fontSize = 11.sp)
                if (severeCode) Text("⛈️ Posible tormenta${if (hailExpected) " o granizo" else ""}: evitá exponerte y buscá un lugar cerrado y seguro si se aproxima.", color = Color(0xFFFF9B8F), fontSize = 11.sp)
                if (maxWind >= 45.0) Text("💨 Atención al viento: se esperan velocidades de hasta ${maxWind.toInt()} km/h. Asegurá objetos sueltos y evitá zonas arboladas.", color = Color(0xFFFFC978), fontSize = 11.sp)
            }
        }

        if (showMetrics) Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            BriefingMetric(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Default.LocalOffer, contentDescription = null, tint = Color(0xFFFFC857)) },
                value = activeDeals.toString(),
                label = "Ofertas activas"
            )
            BriefingMetric(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Default.TaskAlt, contentDescription = null, tint = Color(0xFF66E4FF)) },
                value = (tasks.count { !it.isCompleted }).takeIf { it > 0 }?.toString() ?: briefing.pendingTasksCount.toString(),
                label = "Tareas pendientes"
            )
        }

        if (showQuote) Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10243A)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("UNA IDEA PARA HOY", color = Color(0xFFFFC857), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp)
                Text(
                    "“$dailyQuote”",
                    color = Color.White,
                    fontSize = 18.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (showYesterday) Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10243A)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("AYER EN OMNI", color = Color(0xFF72E4FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                if (previousSummary != null) {
                    Text(previousSummary.first.toString() + " de " + previousSummary.second + " tareas completadas", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Resumen local de tareas del día anterior. Solo se guardan cantidades, no los títulos de tus actividades.", color = Color(0xFFB5C7D9), fontSize = 10.sp)
                } else {
                    Text("Todavía no hay un resumen guardado de ayer. OMNI comenzará a registrar el progreso diario de tus tareas en este dispositivo.", color = Color(0xFFB5C7D9), fontSize = 11.sp)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF10243A)), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("PERSONALIZAR OMNI BUENOS DÍAS", color = Color(0xFF72E4FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                BriefingSettingRow("Mostrar clima", showWeather) { showWeather = it; preferences.edit().putBoolean("show_weather", it).apply() }
                BriefingSettingRow("Mostrar indicadores", showMetrics) { showMetrics = it; preferences.edit().putBoolean("show_metrics", it).apply() }
                BriefingSettingRow("Mostrar frase diaria", showQuote) { showQuote = it; preferences.edit().putBoolean("show_quote", it).apply() }
                BriefingSettingRow("Mostrar resumen de ayer", showYesterday) { showYesterday = it; preferences.edit().putBoolean("show_yesterday", it).apply() }
            }
        }
        Spacer(Modifier.height(4.dp))
        OutlinedButton(
            onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "OMNI Buenos Días")
                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                }
                runCatching { context.startActivity(Intent.createChooser(intent, "Compartir OMNI Buenos Días")) }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF66E4FF))
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(17.dp))
            Text("  Compartir resumen")
        }
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF087BFF)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Text("  Entrar a OMNI", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Text(
            "Podés volver a esta presentación desde el menú de opciones.",
            color = Color(0xFF8196AC),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


@Composable
private fun BriefingSettingRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 12.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun BriefingMetric(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10243A)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            icon()
            Text(value, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color(0xFFB5C7D9), fontSize = 10.sp)
        }
    }
}
