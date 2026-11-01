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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    val activeDeals = briefing.activeDealsCount

    val greeting = briefing.greeting.replace("Santiago", "¡Hola").let {
        if (it.startsWith("¡Hola")) it else it
    }
    val weatherLine = weather?.let {
        "${it.temperatureC.toInt()}°C · ${it.weatherDescription}"
    } ?: briefing.weatherSummary
    val temperatureLine = weather?.let {
        "Sensación ${it.apparentTemperatureC.toInt()}°C · Humedad ${it.humidityPercent}%"
    } ?: briefing.temperatureRange

    val shareMessage = buildString {
        appendLine("🌅 OMNI BUENOS DÍAS")
        appendLine(briefing.dateText)
        appendLine("📍 ${city.ifBlank { "Tu ciudad" }}")
        appendLine("🌤️ $weatherLine")
        appendLine("💬 ${briefing.motivationalQuote}")
        appendLine()
        append("Descubrí tu día con OMNI.")
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF07111F)).padding(18.dp),
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
                    briefing.greeting,
                    color = Color.White,
                    fontSize = 25.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(briefing.dateText, color = Color(0xFFB9D4EB), fontSize = 12.sp)
            }
        }

        Card(
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
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10243A)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("UNA IDEA PARA HOY", color = Color(0xFFFFC857), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp)
                Text(
                    "“${briefing.motivationalQuote}”",
                    color = Color.White,
                    fontSize = 18.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.weight(1f))
        OutlinedButton(
            onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "OMNI Buenos Días")
                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                }
                context.startActivity(Intent.createChooser(intent, "Compartir OMNI Buenos Días"))
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
