package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.example.data.analytics.UsageAnalyticsRepository
import java.text.SimpleDateFormat
import java.util.Locale

private data class OmniEvent(
    val id: String,
    val ownerUid: String,
    val title: String,
    val description: String,
    val category: String,
    val dateTime: String,
    val location: String,
    val contact: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val prefs = remember { context.getSharedPreferences("omni_event_favorites", 0) }
    var favorites by remember { mutableStateOf(prefs.getStringSet("ids", emptySet()).orEmpty().toSet()) }
    var events by remember { mutableStateOf<List<OmniEvent>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var category by remember { mutableStateOf("Todos") }
    var search by remember { mutableStateOf("") }
    var showPublish by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var eventCategory by remember { mutableStateOf("Comunidad") }
    var dateTime by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    DisposableEffect(uid) {
        var listener: ListenerRegistration? = null
        if (uid.isNotBlank()) {
            listener = db.collection("omniEvents").orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        error = exception.localizedMessage ?: "No se pudieron cargar los eventos."
                    } else {
                        events = snapshot?.documents.orEmpty().mapNotNull { doc ->
                            runCatching {
                                OmniEvent(
                                    id = doc.id, ownerUid = doc.getString("ownerUid").orEmpty(),
                                    title = doc.getString("title").orEmpty(),
                                    description = doc.getString("description").orEmpty(),
                                    category = doc.getString("category").orEmpty(),
                                    dateTime = doc.getString("dateTime").orEmpty(),
                                    location = doc.getString("location").orEmpty(),
                                    contact = doc.getString("contact").orEmpty()
                                )
                            }.getOrNull()
                        }
                        error = null
                    }
                    loading = false
                }
        } else {
            loading = false
            error = "Iniciá sesión para consultar los eventos."
        }
        onDispose { listener?.remove() }
    }

    val visible = events.filter { event ->
        (category == "Todos" || (category == "Favoritos" && favorites.contains(event.id)) || event.category == category) &&
            (search.isBlank() || listOf(event.title, event.description, event.location).any { it.contains(search, true) })
    }

    fun publish() {
        if (title.isBlank() || description.isBlank() || dateTime.isBlank() || location.isBlank()) {
            feedback = "Completá título, descripción, fecha, hora y ubicación."
            return
        }
        val parsed = runCatching { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "AR")).apply { isLenient = false }.parse(dateTime) }.getOrNull()
        if (parsed == null || parsed.time < System.currentTimeMillis() - 60_000) {
            feedback = "La fecha debe tener formato dd/MM/aaaa HH:mm y ser futura."
            return
        }
        busy = true
        db.collection("omniEvents").add(mapOf(
            "ownerUid" to uid, "title" to title.trim(), "description" to description.trim(),
            "category" to eventCategory, "dateTime" to dateTime.trim(),
            "location" to location.trim(), "contact" to contact.trim(),
            "status" to "PUBLISHED", "createdAt" to FieldValue.serverTimestamp()
        )).addOnSuccessListener {
            UsageAnalyticsRepository.trackAction(uid, "EVENT_PUBLISHED", "community")
            busy = false; showPublish = false
            title = ""; description = ""; dateTime = ""; location = ""; contact = ""
            feedback = "Evento publicado para la comunidad OMNI."
        }.addOnFailureListener { e ->
            busy = false; feedback = e.localizedMessage ?: "No se pudo publicar. Revisá las reglas de Firestore."
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF070A14)).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Text("AGENDA OMNI", color = Color(0xFF57DDF5), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                Text("Eventos y actividades", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Default.Event, null, tint = Color(0xFF57DDF5), modifier = Modifier.size(25.dp))
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(search, { search = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Buscar eventos o lugares") })
        Spacer(Modifier.height(7.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            items(listOf("Todos", "Favoritos", "Comunidad", "Cultura", "Deportes", "Comercial")) { value ->
                FilterChip(selected = category == value, onClick = { category = value }, label = { Text(value, fontSize = 9.sp) })
            }
        }
        Button(onClick = { showPublish = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF087EAB))) {
            Text("+ Publicar evento")
        }
        when {
            loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF57DDF5)) }
            error != null -> Text(error.orEmpty(), color = Color(0xFFFFA7A7), fontSize = 12.sp)
            visible.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📅", fontSize = 34.sp)
                    Text("No hay eventos para mostrar", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Probá otra categoría o publicá una actividad.", color = Color(0xFF9AAAC2), fontSize = 11.sp)
                }
            }
            else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 90.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(visible, key = { it.id }) { event ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF101B30)), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(event.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${event.category} · ${event.dateTime}", color = Color(0xFF57DDF5), fontSize = 10.sp)
                                }
                                IconButton(onClick = {
                                    val next = favorites.toMutableSet()
                                    if (!next.add(event.id)) next.remove(event.id)
                                    favorites = next
                                    prefs.edit().putStringSet("ids", next).apply()
                                }) {
                                    Icon(if (favorites.contains(event.id)) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorito", tint = Color(0xFFFF6F91))
                                }
                            }
                            Text(event.description, color = Color(0xFFD2DCEC), fontSize = 11.sp)
                            Text("📍 ${event.location}", color = Color(0xFFB8C8D9), fontSize = 10.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                OutlinedButton(onClick = {
                                    val parsed = runCatching { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "AR")).parse(event.dateTime) }.getOrNull()
                                    if (parsed == null) {
                                        feedback = "No se pudo interpretar la fecha del evento."
                                    } else {
                                        val end = parsed.time + 2 * 60 * 60 * 1000L
                                        val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                                            .putExtra(CalendarContract.Events.TITLE, event.title)
                                            .putExtra(CalendarContract.Events.DESCRIPTION, event.description)
                                            .putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)
                                            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, parsed.time)
                                            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
                                        runCatching { context.startActivity(intent) }.onFailure { feedback = "No se encontró una aplicación de calendario." }
                                    }
                                }, modifier = Modifier.weight(1f)) { Text("Recordarme", fontSize = 10.sp) }
                                Button(onClick = {
                                    val uri = Uri.parse("geo:0,0?q=" + Uri.encode(event.location))
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                        .onFailure { feedback = "No se pudo abrir el mapa." }
                                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF087EAB))) {
                                    Icon(Icons.Default.Map, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(4.dp)); Text("Mapa", fontSize = 10.sp)
                                }
                            }
                            if (event.contact.isNotBlank()) TextButton(onClick = {
                                val value = event.contact.trim()
                                val intent = if (value.contains("@")) Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$value"))
                                    else Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${value.filter { it.isDigit() }}"))
                                runCatching { context.startActivity(intent) }.onFailure { feedback = "No se pudo abrir el medio de contacto." }
                            }) { Text("Consultar / reservar", color = Color(0xFF57DDF5), fontSize = 10.sp) }
                            TextButton(onClick = {
                                val share = "📅 ${event.title}\n${event.dateTime}\n📍 ${event.location}\n\n${event.description}\n\nCompartido desde OMNI."
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, event.title)
                                    putExtra(Intent.EXTRA_TEXT, share)
                                }
                                runCatching { context.startActivity(Intent.createChooser(send, "Compartir evento OMNI")) }
                                    .onFailure { feedback = "No se pudo abrir el menú para compartir." }
                            }) {
                                Icon(Icons.Default.Share, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Compartir", color = Color(0xFF57DDF5), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPublish) {
        AlertDialog(
            onDismissRequest = { if (!busy) showPublish = false },
            containerColor = Color(0xFF101B30),
            title = { Text("Publicar evento", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text("Nombre del evento") }, singleLine = true)
                    OutlinedTextField(description, { description = it }, label = { Text("Descripción") }, minLines = 2)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Comunidad", "Cultura", "Deportes", "Comercial").forEach { value ->
                            FilterChip(selected = eventCategory == value, onClick = { eventCategory = value }, label = { Text(value, fontSize = 9.sp) })
                        }
                    }
                    OutlinedTextField(dateTime, { dateTime = it }, label = { Text("Fecha y hora · dd/MM/aaaa HH:mm") }, singleLine = true)
                    OutlinedTextField(location, { location = it }, label = { Text("Lugar / dirección") }, singleLine = true)
                    OutlinedTextField(contact, { contact = it }, label = { Text("WhatsApp o correo (opcional)") }, singleLine = true)
                }
            },
            confirmButton = { TextButton(onClick = { publish() }, enabled = !busy) { Text(if (busy) "Publicando…" else "Publicar") } },
            dismissButton = { TextButton(onClick = { showPublish = false }, enabled = !busy) { Text("Cancelar") } }
        )
    }
    feedback?.let { message ->
        AlertDialog(onDismissRequest = { feedback = null }, title = { Text("Agenda OMNI") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { feedback = null }) { Text("Aceptar") } })
    }
}
