package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.example.data.analytics.UsageAnalyticsRepository

private data class OmniJobPost(
    val id: String,
    val ownerUid: String,
    val ownerName: String,
    val title: String,
    val description: String,
    val category: String,
    val location: String,
    val workType: String,
    val experience: String,
    val contactPhone: String,
    val contactEmail: String,
    val createdAt: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobBoardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid.orEmpty()
    var posts by remember { mutableStateOf<List<OmniJobPost>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Todos") }
    var showPublish by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var postCategory by remember { mutableStateOf("Oficios y servicios") }
    var location by remember { mutableStateOf("") }
    var workType by remember { mutableStateOf("Presencial") }
    var experience by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf(user?.phoneNumber.orEmpty()) }
    var email by remember { mutableStateOf(user?.email.orEmpty()) }
    var cvLinks by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    DisposableEffect(uid) {
        var registration: ListenerRegistration? = null
        if (uid.isNotBlank()) {
            registration = db.collection("jobPosts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        error = exception.localizedMessage ?: "No se pudieron cargar las publicaciones."
                        loading = false
                    } else {
                        posts = snapshot?.documents.orEmpty().mapNotNull { doc ->
                            runCatching {
                                OmniJobPost(
                                    id = doc.id,
                                    ownerUid = doc.getString("ownerUid").orEmpty(),
                                    ownerName = doc.getString("ownerName").orEmpty(),
                                    title = doc.getString("title").orEmpty(),
                                    description = doc.getString("description").orEmpty(),
                                    category = doc.getString("category").orEmpty(),
                                    location = doc.getString("location").orEmpty(),
                                    workType = doc.getString("workType").orEmpty(),
                                    experience = doc.getString("experience").orEmpty(),
                                    contactPhone = doc.getString("contactPhone").orEmpty(),
                                    contactEmail = doc.getString("contactEmail").orEmpty(),
                                    createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                                )
                            }.getOrNull()
                        }
                        loading = false
                        error = null
                    }
                }
        } else {
            loading = false
            error = "Iniciá sesión para consultar la Bolsa de Trabajo."
        }
        onDispose { registration?.remove() }
    }

    val visiblePosts = posts.filter { post ->
        (category == "Todos" || post.category == category) &&
            listOf(post.title, post.description, post.location, post.category)
                .any { it.contains(search, ignoreCase = true) }
    }

    fun publishPost() {
        if (uid.isBlank() || title.isBlank() || description.isBlank() || location.isBlank() || (phone.isBlank() && email.isBlank())) {
            feedback = "Completá título, descripción, localidad y al menos un medio de contacto."
            return
        }
        busy = true
        val ref = db.collection("jobPosts").document()
        ref.set(mapOf(
            "ownerUid" to uid,
            "ownerName" to (user?.displayName?.takeIf { it.isNotBlank() } ?: user?.email?.substringBefore("@") ?: "Usuario OMNI"),
            "title" to title.trim(),
            "description" to description.trim(),
            "category" to postCategory,
            "location" to location.trim(),
            "workType" to workType,
            "experience" to experience.trim(),
            "contactPhone" to phone.trim(),
            "contactEmail" to email.trim(),
            "status" to "ACTIVE",
            "createdAt" to FieldValue.serverTimestamp()
        )).addOnSuccessListener {
            UsageAnalyticsRepository.trackEvent(uid, "jobs", "JOB_POST_PUBLISHED", mapOf("category" to postCategory, "success" to true))
            busy = false
            showPublish = false
            selectedTab = 0
            title = ""; description = ""; experience = ""
            feedback = "Publicación creada y visible para la comunidad OMNI."
        }.addOnFailureListener { exception ->
            busy = false
            feedback = exception.localizedMessage ?: "No se pudo publicar. Revisá las reglas de Firestore."
        }
    }

    fun applyTo(post: OmniJobPost) {
        if (uid.isBlank()) return
        busy = true
        val applicationId = "${uid}_${post.id}"
        db.collection("jobApplications").document(applicationId).get()
            .addOnSuccessListener { existing ->
                if (existing.exists()) {
                    busy = false
                    feedback = "Ya enviaste una postulación a esta publicación."
                } else {
                    db.collection("jobApplications").document(applicationId).set(mapOf(
                        "postId" to post.id,
                        "postOwnerUid" to post.ownerUid,
                        "applicantUid" to uid,
                        "applicantName" to (user?.displayName ?: user?.email?.substringBefore("@") ?: "Usuario OMNI"),
                        "applicantEmail" to user?.email.orEmpty(),
                        "applicantPhone" to user?.phoneNumber.orEmpty(),
                        "message" to "Me interesa esta oportunidad. Podemos conversar.",
                        "cvAndCertificatesLinks" to cvLinks.trim().take(1500),
                        "status" to "RECIBIDA",
                        "createdAt" to FieldValue.serverTimestamp()
                    )).addOnSuccessListener {
                        UsageAnalyticsRepository.trackEvent(uid, "jobs", "JOB_APPLICATION_SENT", mapOf("success" to true))
                        busy = false
                        feedback = "Postulación enviada correctamente."
                    }.addOnFailureListener { e ->
                        busy = false
                        feedback = e.localizedMessage ?: "No se pudo enviar la postulación."
                    }
                }
            }.addOnFailureListener { e ->
                busy = false
                feedback = e.localizedMessage ?: "No se pudo verificar la postulación."
            }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF070A14)).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Text("OMNI OPORTUNIDADES", color = Color(0xFF57DDF5), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                Text("Bolsa de Trabajo y Servicios", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
            Surface(color = Color(0xFF163B36), shape = RoundedCornerShape(8.dp)) {
                Text("COMUNIDAD", color = Color(0xFF65E3B0), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(7.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(selected = selectedTab == 0, onClick = { selectedTab = 0 }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("Buscar") }
            SegmentedButton(selected = selectedTab == 1, onClick = { selectedTab = 1 }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("Publicar") }
        }
        if (selectedTab == 0) {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(search, { search = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Buscar empleo, oficio o servicio") }, leadingIcon = { Icon(Icons.Default.Search, null) })
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Todos", "Empleo", "Oficios y servicios").forEach { value ->
                    FilterChip(selected = category == value, onClick = { category = value }, label = { Text(value, fontSize = 10.sp) })
                }
            }
            OutlinedTextField(cvLinks, { cvLinks = it }, modifier = Modifier.fillMaxWidth(), label = { Text("CV / títulos / certificados (enlace opcional)") }, singleLine = true, supportingText = { Text("Pegá un enlace compartido de Drive u otro servicio. OMNI no aloja documentos en esta etapa gratuita.") })
            when {
                loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF57DDF5)) }
                error != null -> Text(error.orEmpty(), color = Color(0xFFFFA7A7), fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                visiblePosts.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Work, null, tint = Color(0xFF57DDF5), modifier = Modifier.size(36.dp))
                        Text("Todavía no hay publicaciones", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Cuando alguien publique una oportunidad aparecerá acá.", color = Color(0xFF9AAAC2), fontSize = 11.sp)
                    }
                }
                else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 90.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(visiblePosts, key = { it.id }) { post ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF101B30)), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(post.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${post.category} · ${post.location}", color = Color(0xFF57DDF5), fontSize = 10.sp)
                                    }
                                    Surface(color = Color(0xFF1B2B45), shape = RoundedCornerShape(7.dp)) { Text(post.workType, color = Color(0xFFB8C8D9), fontSize = 9.sp, modifier = Modifier.padding(6.dp)) }
                                }
                                Text(post.description, color = Color(0xFFD2DCEC), fontSize = 11.sp)
                                if (post.experience.isNotBlank()) Text("Experiencia: ${post.experience}", color = Color(0xFF9AAAC2), fontSize = 10.sp)
                                Text("Publicado por ${post.ownerName}", color = Color(0xFF9AAAC2), fontSize = 9.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (post.ownerUid != uid) {
                                        OutlinedButton(onClick = { applyTo(post) }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Postularme", fontSize = 10.sp) }
                                    } else {
                                        Text("Tu publicación", color = Color(0xFF65E3B0), fontSize = 10.sp, modifier = Modifier.weight(1f).align(Alignment.CenterVertically))
                                    }
                                    Button(onClick = {
                                        val phoneNumber = post.contactPhone.filter { it.isDigit() }
                                        try {
                                            if (phoneNumber.isNotBlank()) {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$phoneNumber")))
                                            } else if (post.contactEmail.isNotBlank()) {
                                                UsageAnalyticsRepository.trackCurrentAction("jobs", "JOB_CONTACT_OPENED", mapOf("method" to "email"))
                                                context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${post.contactEmail}")))
                                            }
                                        } catch (_: Exception) { feedback = "No se encontró una aplicación para contactar." }
                                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF087EAB))) { Text("Contactar", fontSize = 10.sp) }
                                    IconButton(onClick = {
                                        val share = "🧰 ${post.title}\n${post.category} · ${post.location}\n\n${post.description}\n\nContacto: ${post.contactPhone.ifBlank { post.contactEmail }}\n\nCompartido desde OMNI."
                                        UsageAnalyticsRepository.trackCurrentAction("jobs", "JOB_POST_SHARED", mapOf("method" to "share_sheet"))
                                        val send = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, post.title)
                                            putExtra(Intent.EXTRA_TEXT, share)
                                        }
                                        runCatching { context.startActivity(Intent.createChooser(send, "Compartir oportunidad OMNI")) }
                                            .onFailure { feedback = "No se pudo abrir el menú para compartir." }
                                    }) { Icon(Icons.Default.Share, "Compartir publicación", tint = Color(0xFF57DDF5)) }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Spacer(Modifier.height(10.dp))
            Text("Compartí una oportunidad con personas de tu zona.", color = Color(0xFF9AAAC2), fontSize = 11.sp)
            OutlinedTextField(title, { title = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Título de la publicación") }, singleLine = true)
            OutlinedTextField(description, { description = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Descripción y requisitos") }, minLines = 3)
            Text("Tipo de publicación", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FilterChip(selected = postCategory == "Empleo", onClick = { postCategory = "Empleo" }, label = { Text("Empleo") })
                FilterChip(selected = postCategory == "Oficios y servicios", onClick = { postCategory = "Oficios y servicios" }, label = { Text("Oficio / Servicio") })
            }
            OutlinedTextField(location, { location = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Localidad o zona") }, singleLine = true)
            OutlinedTextField(experience, { experience = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Experiencia o requisitos (opcional)") })
            Text("Modalidad", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("Presencial", "Remoto", "Híbrido").forEach { value ->
                    FilterChip(selected = workType == value, onClick = { workType = value }, label = { Text(value, fontSize = 10.sp) })
                }
            }
            OutlinedTextField(phone, { phone = it }, modifier = Modifier.fillMaxWidth(), label = { Text("WhatsApp de contacto") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
            OutlinedTextField(email, { email = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Correo de contacto") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { publishPost() }, enabled = !busy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF087EAB))) {
                if (busy) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp) else Text("Publicar oportunidad")
            }
            Text("No publiques datos sensibles. El contacto que ingreses será visible para usuarios autenticados de OMNI.", color = Color(0xFFFFC66D), fontSize = 10.sp)
        }
    }
    feedback?.let { message ->
        AlertDialog(onDismissRequest = { feedback = null }, title = { Text("OMNI Oportunidades") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { feedback = null }) { Text("Aceptar") } })
    }
}
