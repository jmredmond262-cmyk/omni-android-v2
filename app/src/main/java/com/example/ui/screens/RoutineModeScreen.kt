package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

private data class OmniRoutine(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val hour: Int,
    val minute: Int,
    val days: Set<Int>,
    val mode: String = "SOUND",
    val enabled: Boolean = true
)

private object RoutineStore {
    private const val PREFS = "omni_routines"
    private const val KEY = "items"

    fun load(context: Context): List<OmniRoutine> = runCatching {
        val array = JSONArray(context.getSharedPreferences(PREFS, 0).getString(KEY, "[]"))
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            OmniRoutine(
                id = item.optString("id"),
                title = item.optString("title"),
                message = item.optString("message"),
                hour = item.optInt("hour"),
                minute = item.optInt("minute"),
                days = item.optString("days").split(",").mapNotNull { it.toIntOrNull() }.toSet(),
                mode = item.optString("mode", "SOUND"),
                enabled = item.optBoolean("enabled", true)
            )
        }
    }.getOrDefault(emptyList())

    fun save(context: Context, items: List<OmniRoutine>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id); put("title", item.title); put("message", item.message)
                put("hour", item.hour); put("minute", item.minute)
                put("days", item.days.sorted().joinToString(","))
                put("mode", item.mode); put("enabled", item.enabled)
            })
        }
        context.getSharedPreferences(PREFS, 0).edit().putString(KEY, array.toString()).apply()
        items.forEach { RoutineAlarmScheduler.schedule(context, it) }
    }
}

private object RoutineAlarmScheduler {
    fun schedule(context: Context, routine: OmniRoutine) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, RoutineReminderReceiver::class.java).putExtra("routine_id", routine.id)
        val pending = PendingIntent.getBroadcast(
            context, routine.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
        if (!routine.enabled || routine.days.isEmpty()) return
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, routine.hour); set(Calendar.MINUTE, routine.minute)
            var attempts = 0
            while ((get(Calendar.DAY_OF_WEEK) !in routine.days || timeInMillis <= now.timeInMillis) && attempts < 8) {
                add(Calendar.DAY_OF_YEAR, 1); attempts++
            }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
    }
}

class RoutineReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("routine_id") ?: return
        val routine = RoutineStore.load(context).firstOrNull { it.id == id && it.enabled } ?: return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = if (routine.mode == "SILENT") "omni_routines_silent" else "omni_routines_sound"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = if (routine.mode == "SILENT") NotificationManager.IMPORTANCE_LOW else NotificationManager.IMPORTANCE_HIGH
            if (manager.getNotificationChannel(channelId) == null) {
                manager.createNotificationChannel(NotificationChannel(channelId, "Recordatorios OMNI", importance))
            }
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(routine.title)
            .setContentText(routine.message.ifBlank { "Es momento de completar tu rutina." })
            .setStyle(NotificationCompat.BigTextStyle().bigText(routine.message.ifBlank { "Es momento de completar tu rutina." }))
            .setAutoCancel(true)
            .setPriority(if (routine.mode == "SILENT") NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)
            .build()
        manager.notify(id.hashCode(), notification)
        RoutineAlarmScheduler.schedule(context, routine)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineModeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    var routines by remember { mutableStateOf(RoutineStore.load(context)) }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<OmniRoutine?>(null) }
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf("08") }
    var minute by remember { mutableStateOf("00") }
    var days by remember { mutableStateOf(setOf(2,3,4,5,6)) }
    var mode by remember { mutableStateOf("SOUND") }

    fun persist(next: List<OmniRoutine>) {
        routines = next
        RoutineStore.save(context, next)
    }
    fun openEditor(item: OmniRoutine? = null) {
        editing = item
        title = item?.title.orEmpty()
        message = item?.message.orEmpty()
        hour = (item?.hour ?: 8).toString().padStart(2, '0')
        minute = (item?.minute ?: 0).toString().padStart(2, '0')
        days = item?.days ?: setOf(2,3,4,5,6)
        mode = item?.mode ?: "SOUND"
        showEditor = true
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF070A14)).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("MODO RUTINA", color = Color(0xFF57DDF5), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text("Tus hábitos, a tu ritmo", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("Recordatorios que siguen funcionando en segundo plano", color = Color(0xFF9AAAC2), fontSize = 11.sp)
            }
            TextButton(onClick = onBack) { Text("Volver") }
        }
        Spacer(Modifier.height(14.dp))
        Button(onClick = { openEditor() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF087EAB))) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Crear rutina")
        }
        Spacer(Modifier.height(12.dp))
        if (routines.isEmpty()) {
            Surface(color = Color(0xFF101B30), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Alarm, null, tint = Color(0xFF57DDF5), modifier = Modifier.size(34.dp))
                    Text("Todavía no tenés rutinas", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Creá un recordatorio para organizar tu día.", color = Color(0xFF9AAAC2), fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
                items(routines, key = { it.id }) { item ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF101B30)), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(String.format("%02d:%02d · %s", item.hour, item.minute, item.days.sorted().joinToString(" ") { day -> listOf("", "D", "L", "M", "X", "J", "V", "S")[day] }), color = Color(0xFF57DDF5), fontSize = 11.sp)
                                }
                                IconButton(onClick = { persist(routines.map { if (it.id == item.id) it.copy(enabled = !it.enabled) else it }) }) {
                                    Icon(if (item.enabled) Icons.Default.Pause else Icons.Default.PlayArrow, if (item.enabled) "Pausar" else "Activar", tint = Color(0xFF57DDF5))
                                }
                                IconButton(onClick = { openEditor(item) }) { Icon(Icons.Default.Edit, "Editar", tint = Color.LightGray) }
                                IconButton(onClick = { persist(routines.filterNot { it.id == item.id }) }) { Icon(Icons.Default.Delete, "Eliminar", tint = Color(0xFFFF6B79)) }
                            }
                            Text(item.message.ifBlank { "Recordatorio sin mensaje adicional" }, color = Color(0xFFB8C8D9), fontSize = 11.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (item.enabled) "ACTIVA" else "PAUSADA", color = if (item.enabled) Color(0xFF5DE0A0) else Color(0xFFFFC66D), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp)); Text(if (item.mode == "SILENT") "Silenciosa" else if (item.mode == "VOICE") "Voz (lectura al abrir)" else "Con sonido", color = Color(0xFF9AAAC2), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            containerColor = Color(0xFF101B30),
            title = { Text(if (editing == null) "Nueva rutina" else "Editar rutina", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text("Actividad") }, singleLine = true)
                    OutlinedTextField(message, { message = it }, label = { Text("Mensaje o detalle") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(hour, { hour = it.filter(Char::isDigit).take(2) }, label = { Text("Hora") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(minute, { minute = it.filter(Char::isDigit).take(2) }, label = { Text("Minutos") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Text("Repetir", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        listOf("D","L","M","X","J","V","S").forEachIndexed { index, label ->
                            val day = index + 1
                            FilterChip(selected = days.contains(day), onClick = { days = if (days.contains(day)) days - day else days + day }, label = { Text(label) })
                        }
                    }
                    Text("Aviso", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    SingleChoiceSegmentedButtonRow {
                        listOf("SILENT" to "Silencio", "SOUND" to "Sonido", "VOICE" to "Voz").forEachIndexed { index, option ->
                            SegmentedButton(selected = mode == option.first, onClick = { mode = option.first }, shape = SegmentedButtonDefaults.itemShape(index, 3)) { Text(option.second, fontSize = 10.sp) }
                        }
                    }
                    if (mode == "VOICE") Text("La opción voz muestra el mensaje para lectura al abrir el aviso; no inicia audio inesperado en segundo plano.", color = Color(0xFFFFC66D), fontSize = 10.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val h = hour.toIntOrNull(); val m = minute.toIntOrNull()
                    if (title.isNotBlank() && h != null && h in 0..23 && m != null && m in 0..59 && days.isNotEmpty()) {
                        val item = OmniRoutine(id = editing?.id ?: UUID.randomUUID().toString(), title = title.trim(), message = message.trim(), hour = h, minute = m, days = days, mode = mode, enabled = editing?.enabled ?: true)
                        persist(if (editing == null) routines + item else routines.map { if (it.id == item.id) item else it })
                        showEditor = false
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("Cancelar") } }
        )
    }
}
