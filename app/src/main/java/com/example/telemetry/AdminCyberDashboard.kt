package com.example.telemetry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

val CyberBlack = Color(0xFF06090F)
val CyberCardBg = Color(0xFF0F1726)
val NeonCyan = Color(0xFF00F0FF)
val NeonGreen = Color(0xFF00FF66)
val NeonMagenta = Color(0xFFFF0055)
val CyberGray = Color(0xFF7A889B)

@Composable
fun AdminCyberDashboardScreen() {
    var events by remember { mutableStateOf<List<TelemetryEvent>>(emptyList()) }
    var selectedEvent by remember { mutableStateOf<TelemetryEvent?>(null) }

    DisposableEffect(Unit) {
        val listener = FirebaseFirestore.getInstance()
            .collection("telemetry_events")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    events = snapshot.toObjects(TelemetryEvent::class.java)
                }
            }
        onDispose { listener.remove() }
    }

    val activeUsersCount = events.map { it.userId }.distinct().size
    val avgBattery = if (events.isNotEmpty()) events.map { it.deviceHealth.batteryLevel }.average().toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CYBER COMMAND // ADMIN",
                    color = NeonCyan,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "TELEMETRÍA & RASTREO TÁCTICO",
                    color = CyberGray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(NeonGreen.copy(alpha = 0.15f))
                    .border(1.dp, NeonGreen, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(text = "EN VIVO", color = NeonGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("USUARIOS", "$activeUsersCount", Modifier.weight(1f), NeonCyan)
            MetricCard("BAT. PROMEDIO", "$avgBattery%", Modifier.weight(1f), if (avgBattery > 20) NeonGreen else NeonMagenta)
            MetricCard("SUCESOS", "${events.size}", Modifier.weight(1f), NeonMagenta)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "REGISTRO DE SUCESOS (FORENSE)",
            color = NeonCyan,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(events) { ev ->
                EventCard(event = ev, onClick = { selectedEvent = ev })
            }
        }
    }

    selectedEvent?.let { ev ->
        AlertDialog(
            onDismissRequest = { selectedEvent = null },
            containerColor = CyberCardBg,
            title = {
                Text(
                    text = "DETALLE: ${ev.eventType}",
                    color = NeonCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DetailRow("ID Evento", ev.eventId.take(12) + "...")
                    DetailRow("Usuario", ev.userId)
                    DetailRow("Modo", ev.trackingMode)
                    DetailRow("Latitud", ev.location.latitude.toString())
                    DetailRow("Longitud", ev.location.longitude.toString())
                    DetailRow("Precisión GPS", "±${ev.location.accuracy}m")
                    DetailRow("Velocidad", "${ev.location.speed} m/s")
                    DetailRow("Batería", "${ev.deviceHealth.batteryLevel}%")
                    DetailRow("Cargando", if (ev.deviceHealth.isCharging) "SÍ" else "NO")
                    DetailRow("Timestamp", ev.timestamp?.toString() ?: "Reciente")
                    if (ev.details.isNotEmpty()) {
                        Text("Metadatos extra:", color = NeonGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        ev.details.forEach { (k, v) -> DetailRow("  • $k", v) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedEvent = null }) {
                    Text("CERRAR", color = NeonMagenta, fontFamily = FontFamily.Monospace)
                }
            }
        )
    }
}

@Composable
fun MetricCard(title: String, value: String, modifier: Modifier, accentColor: Color) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CyberCardBg)
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(text = title, color = CyberGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(text = value, color = accentColor, fontSize = 18.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EventCard(event: TelemetryEvent, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(CyberCardBg)
            .border(1.dp, if (event.trackingMode == "CONTINUO") NeonCyan.copy(alpha = 0.4f) else CyberGray.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = event.eventType,
                        color = if (event.trackingMode == "CONTINUO") NeonGreen else NeonCyan,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "[${event.trackingMode}]",
                        color = CyberGray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Lat: ${event.location.latitude} | Lon: ${event.location.longitude}",
                    color = CyberGray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = "${event.deviceHealth.batteryLevel}% BAT",
                color = if (event.deviceHealth.batteryLevel > 20) NeonCyan else NeonMagenta,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = CyberGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}
