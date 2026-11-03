package com.example.ui.screens

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.style.TextOverflow
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ContactCategory
import com.example.model.NeighborhoodContact
import com.example.ui.MainViewModel
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.CyberDarkSurfaceVariant
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberGlowCyan
import com.example.ui.theme.CyberGlowAmber
import com.example.ui.theme.CyberGlowGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val contacts by viewModel.contacts.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var phoneContacts by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var hasContactsPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    fun loadPhoneContacts() {
        val result = mutableListOf<Pair<String, String>>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC"
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (nameIndex >= 0 && numberIndex >= 0) {
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameIndex).orEmpty().trim()
                        val number = cursor.getString(numberIndex).orEmpty().trim()
                        if (name.isNotBlank() && number.isNotBlank()) result.add(name to number)
                    }
                }
            }
        }
        phoneContacts = result.distinctBy { it.second.filter(Char::isDigit) }.sortedBy { it.first.lowercase() }
    }
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasContactsPermission = granted
        if (granted) loadPhoneContacts()
    }
    LaunchedEffect(hasContactsPermission, selectedTab) {
        if (selectedTab == 0 && hasContactsPermission) loadPhoneContacts()
    }

    var selectedCategory by remember { mutableStateOf<ContactCategory?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf(ContactCategory.REMIS) }
    var newAddress by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }

    val filteredContacts = remember(contacts, selectedCategory, searchQuery) {
        contacts.filter { contact ->
            (selectedCategory == null || contact.category == selectedCategory) &&
                (searchQuery.isBlank() || contact.name.contains(searchQuery, true) || contact.phoneNumber.contains(searchQuery, true))
        }.sortedByDescending { it.isFavorite }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("contacts_screen"),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera Informativa
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Mi Agenda y Directorio OMNI",
                        color = Color(0xFFF4F8FF),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tus contactos personales y los servicios de tu zona, organizados en un solo lugar.",
                        color = Color(0xFFF4F8FF).copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().background(CyberDarkSurface, RoundedCornerShape(14.dp)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Mi agenda", "Directorio OMNI").forEachIndexed { index, label ->
                    Surface(
                        color = if (selectedTab == index) CyberGlowCyan else Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).clickable {
                            selectedTab = index
                            if (index == 0) {
                                if (hasContactsPermission) loadPhoneContacts()
                                else contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        }
                    ) {
                        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(if (index == 0) "👤" else "🏪", fontSize = 19.sp)
                            Text(label, color = if (selectedTab == index) Color(0xFFF4F8FF) else Color(0xFFB8C9DE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(if (index == 0) "Contactos del teléfono" else "Servicios y comercios", color = if (selectedTab == index) Color(0xFFF4F8FF).copy(alpha = 0.85f) else Color(0xFF8FA1B7), fontSize = 9.sp)
                        }
                    }
                }
            }
        }

        if (selectedTab == 1) {
        // Barra de Categorías
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("Todos (${contacts.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberGlowCyan,
                            selectedLabelColor = Color(0xFFF4F8FF)
                        )
                    )
                }
                items(ContactCategory.values()) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberGlowCyan,
                            selectedLabelColor = Color(0xFFF4F8FF)
                        )
                    )
                }
            }
        }

        // Botón Agendar Nuevo Contacto
        item {
            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberDarkSurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = CyberGlowCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Agendar nuevo remis o servicio", color = CyberGlowCyan, fontWeight = FontWeight.Bold)
            }
        }

        // Búsqueda integrada del directorio de servicios
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar servicio, rubro o teléfono") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberGlowCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedLabelColor = CyberGlowCyan,
                    cursorColor = CyberGlowCyan
                )
            )
        }
        // Lista de Contactos
        if (filteredContacts.isEmpty()) item {
            Text("No hay servicios que coincidan con la búsqueda.", color = Color(0xFF9EADC3), modifier = Modifier.padding(12.dp))
        }
        items(filteredContacts, key = { it.id }) { contact ->
            ContactCard(
                contact = contact,
                onToggleFavorite = { viewModel.toggleContactFavorite(contact.id) },
                onCall = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phoneNumber}"))
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Safe fallback
                    }
                }
            )
        }

        } else {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar en mi agenda") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (!hasContactsPermission) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = CyberDarkSurface), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Tus contactos, a tu alcance", color = Color(0xFFF4F8FF), fontWeight = FontWeight.Bold)
                            Text("OMNI necesita permiso para mostrar los nombres y teléfonos guardados en tu celular. La agenda se consulta localmente y no se sube a nuestros servidores.", color = Color(0xFFB8C9DE), fontSize = 12.sp)
                            Button(onClick = { contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) }) { Text("Permitir acceso a contactos") }
                        }
                    }
                }
            } else {
                val visiblePhoneContacts = phoneContacts.filter {
                    it.first.contains(searchQuery, true) || it.second.contains(searchQuery, true)
                }
                if (visiblePhoneContacts.isEmpty()) {
                    item { Text(if (phoneContacts.isEmpty()) "No se encontraron contactos con teléfono." else "No hay coincidencias.", color = Color(0xFFB8C9DE), modifier = Modifier.padding(12.dp)) }
                }
                items(visiblePhoneContacts, key = { it.second }) { entry ->
                    Card(colors = CardDefaults.cardColors(containerColor = CyberDarkSurface), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(entry.first, color = Color(0xFFF4F8FF), fontWeight = FontWeight.Bold)
                                Text(entry.second, color = Color(0xFFB8C9DE), fontSize = 12.sp)
                            }
                            IconButton(onClick = {
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "¡Hola! Te comparto OMNI, una app para descubrir servicios, comercios y oportunidades de tu zona.")
                                }
                                context.startActivity(Intent.createChooser(share, "Compartir con ${entry.first}"))
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Compartir", tint = CyberGlowCyan)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    // Modal para agendar contacto
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Agendar Contacto de Barrio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nombre / Remisería / Negocio") },
                        placeholder = { Text("Ej: Remises La Estación") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("Teléfono o Celular") },
                        placeholder = { Text("Ej: +54 11 4000-0000") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Selector de categoría
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = newCategory.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Rubro") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            ContactCategory.values().forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.label) },
                                    onClick = {
                                        newCategory = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newAddress,
                        onValueChange = { newAddress = it },
                        label = { Text("Dirección o Base (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newPhone.isNotBlank()) {
                            viewModel.addCustomContact(newName, newCategory, newPhone, newAddress)
                            newName = ""
                            newPhone = ""
                            newAddress = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberGlowCyan, contentColor = CyberDarkBg)
                ) {
                    Text("Guardar Contacto")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ContactCard(
    contact: NeighborhoodContact,
    onToggleFavorite: () -> Unit,
    onCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F8FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono según categoría
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        when (contact.category) {
                            ContactCategory.REMIS, ContactCategory.TAXI -> CyberGlowAmber.copy(alpha = 0.14f)
                            ContactCategory.FARMACIA_TURNO -> CyberGlowCyan.copy(alpha = 0.12f)
                            ContactCategory.EMERGENCIA -> CyberGlowAmber.copy(alpha = 0.14f)
                            else -> CyberDarkSurfaceVariant
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (contact.category) {
                        ContactCategory.REMIS, ContactCategory.TAXI -> Icons.Default.LocalTaxi
                        ContactCategory.FARMACIA_TURNO -> Icons.Default.LocalPharmacy
                        ContactCategory.CERRAJERIA -> Icons.Default.Lock
                        ContactCategory.FLETE -> Icons.Default.LocalShipping
                        ContactCategory.EMERGENCIA -> Icons.Default.Emergency
                    },
                    contentDescription = null,
                    tint = when (contact.category) {
                        ContactCategory.REMIS, ContactCategory.TAXI -> CyberGlowAmber
                        ContactCategory.FARMACIA_TURNO -> CyberGlowCyan
                        ContactCategory.EMERGENCIA -> CyberGlowAmber
                        else -> Color(0xFFEAF4FF)
                    },
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFFEAF4FF),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (contact.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorito",
                            tint = if (contact.isFavorite) CyberGlowAmber else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = contact.statusText,
                    fontSize = 11.sp,
                    color = CyberGlowCyan,
                    fontWeight = FontWeight.SemiBold
                )

                if (contact.address != null) {
                    Text(
                        text = contact.address,
                        fontSize = 11.sp,
                        color = Color(0xFF9EADC3)
                    )
                }

                if (contact.averageWaitMinutes != null) {
                    Text(
                        text = "⏱️ Demora aprox: ~${contact.averageWaitMinutes} min",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberGlowAmber
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onCall,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = CyberGlowCyan, contentColor = CyberDarkBg),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Llamar",
                    tint = Color(0xFFF4F8FF),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
