package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import com.example.ui.components.PulsingStatusDot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ProductCategory
import com.example.data.ai.ProductVisionAnalyzer
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.theme.AmberDeal
import com.example.ui.theme.AmberDealContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RosePromo
import com.example.ui.theme.SlateNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantHubScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val store by viewModel.managedStore.collectAsState()
    val newsList by viewModel.storeNews.collectAsState()

    var showAddProductDialog by remember { mutableStateOf(false) }
    var showAddComboDialog by remember { mutableStateOf(false) }
    var showPublishNewsDialog by remember { mutableStateOf(false) }
    var showThresholdDialog by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }

    // Campos formulario horarios
    var editOpenTime by remember { mutableStateOf(store.openTime) }
    var editCloseTime by remember { mutableStateOf(store.closeTime) }
    var editWeekendHours by remember { mutableStateOf(store.weekendHoursText) }
    var editIs24Hours by remember { mutableStateOf(store.is24Hours) }
    var editIsOnDuty by remember { mutableStateOf(store.isOnDuty) }
    var editOpenOnHolidays by remember { mutableStateOf(store.openOnHolidays) }
    var editScheduleNotes by remember { mutableStateOf(store.scheduleNotes) }
    var editIsOpenNow by remember { mutableStateOf(store.isOpen) }

    // Campos formulario producto
    val merchantBenefits by viewModel.merchantBenefits.collectAsState()
    var prodName by remember { mutableStateOf("") }
    var prodDescription by remember { mutableStateOf("") }
    var isAiAnalyzing by remember { mutableStateOf(false) }
    var aiAnalysisMessage by remember { mutableStateOf("") }
    val screenScope = rememberCoroutineScope()
    var prodCategory by remember { mutableStateOf(ProductCategory.ALMACEN) }
    var prodPrice by remember { mutableStateOf("") }
    var prodOriginalPrice by remember { mutableStateOf("") }
    var prodPhotoUri by remember { mutableStateOf<String?>(null) }
    var prodBarcode by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }

    fun copyImageToPrivateStorage(source: Uri): String? = runCatching {
        val photoDir = File(context.filesDir, "product_photos").apply { mkdirs() }
        val photoFile = File(photoDir, "product_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(source).use { input ->
            requireNotNull(input) { "No se pudo leer la imagen seleccionada" }
            FileOutputStream(photoFile).use { output -> input.copyTo(output) }
        }
        Uri.fromFile(photoFile).toString()
    }.getOrNull()

    fun analyzeProductPhoto(uri: String) {
        isAiAnalyzing = true
        aiAnalysisMessage = "Gemini está leyendo la etiqueta y preparando la ficha del producto…"
        screenScope.launch {
            runCatching {
                ProductVisionAnalyzer.analyze(context, Uri.parse(uri))
            }.onSuccess { draft ->
                prodName = draft.name
                prodDescription = draft.description
                ProductCategory.values().firstOrNull { it.name == draft.category }?.let { prodCategory = it }
                if (draft.barcode.isNotBlank()) prodBarcode = draft.barcode
                aiAnalysisMessage = "Ficha sugerida por IA. Revisá los datos antes de publicar."
            }.onFailure { error ->
                aiAnalysisMessage = error.localizedMessage
                    ?: "No se pudo analizar la foto. Podés completar los datos manualmente."
            }
            isAiAnalyzing = false
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            prodPhotoUri = copyImageToPrivateStorage(uri) ?: uri.toString()
            analyzeProductPhoto(prodPhotoUri!!)
        }
    }

    // Captura real desde la cámara del dispositivo y copia la imagen al almacenamiento privado de OMNI.
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            runCatching {
                val photoDir = File(context.filesDir, "product_photos").apply { mkdirs() }
                val photoFile = File(photoDir, "product_${System.currentTimeMillis()}.jpg")
                FileOutputStream(photoFile).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
                }
                prodPhotoUri = Uri.fromFile(photoFile).toString()
                analyzeProductPhoto(prodPhotoUri!!)
            }
        }
    }

    // Campos formulario combo
    var comboTitle by remember { mutableStateOf("") }
    var comboDesc by remember { mutableStateOf("") }
    var comboBadge by remember { mutableStateOf("-20%") }

    // Campos formulario noticia
    var newsTitle by remember { mutableStateOf("") }
    var newsContent by remember { mutableStateOf("") }
    var newsBadge by remember { mutableStateOf("Novedad Flash 🔥") }

    // Campos regla de descuento
    var thresholdAmount by remember { mutableStateOf("${store.minSpendDiscountThreshold.toInt()}") }
    var thresholdPercent by remember { mutableStateOf("${store.minSpendDiscountPercent}") }

    BackHandler {
        viewModel.navigateTo(Screen.UserProfile)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("merchant_hub_screen"),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera del Comercio Administrado
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = SlateNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.navigateTo(Screen.UserProfile) },
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Panel de Comercio",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = store.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Surface(
                            color = if (store.isOpen) EmeraldPrimary else Color.Red,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (store.isOpen) "Abierto" else "Cerrado",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Estado en tiempo real para vecinos:", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            Text(
                                text = if (store.isOpen) "Tu local aparece como ABIERTO" else "Tu local aparece como CERRADO",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Switch(
                            checked = store.isOpen,
                            onCheckedChange = { viewModel.toggleStoreOpenStatus() },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldPrimary)
                        )
                    }
                }
            }
        }

        // Beneficios Comerciales, Puntos & Rendimiento por Ventas
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD600).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(15.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🏆", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = merchantBenefits.merchantLevel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "${merchantBenefits.merchantPoints} Puntos Comerciales acumulados",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFFFFD600)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Text(
                                text = "0% COMISIÓN",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "📦 Ventas Concretadas", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text(text = "${merchantBenefits.completedSalesCount} pedidos", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "⚡ Respuesta a Vecinos", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text(text = merchantBenefits.customerResponseRate, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                            }
                        }
                    }
                }
            }
        }

        // Métricas Rápidas del Día
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "214", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SlateNavy)
                        Text(text = "Vistas en el mapa", fontSize = 10.sp, color = Color.Gray)
                    }
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Icon(imageVector = Icons.Default.NearMe, contentDescription = null, tint = AmberDeal, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "48", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SlateNavy)
                        Text(text = "Rutas guiadas al local", fontSize = 10.sp, color = Color.Gray)
                    }
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Icon(imageVector = Icons.Default.Inventory, contentDescription = null, tint = SlateNavy, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "${store.inventory.size}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SlateNavy)
                        Text(text = "Productos activos", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }

        // Configuración de Horarios de Cierre, Apertura & Guardia de Turno
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("merchant_schedule_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Horarios & Guardia de Turno", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SlateNavy)
                        }

                        Button(
                            onClick = {
                                editOpenTime = store.openTime
                                editCloseTime = store.closeTime
                                editWeekendHours = store.weekendHoursText
                                editIs24Hours = store.is24Hours
                                editIsOnDuty = store.isOnDuty
                                editOpenOnHolidays = store.openOnHolidays
                                editScheduleNotes = store.scheduleNotes
                                editIsOpenNow = store.isOpen
                                showScheduleDialog = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary.copy(alpha = 0.12f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp).testTag("btn_edit_merchant_schedule")
                        ) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Configurar", color = EmeraldDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Estado actual destacado con pelotita verde parpadeando tenue
                    Surface(
                        color = if (store.isEffectivelyOpen) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (store.isEffectivelyOpen) {
                                PulsingStatusDot(color = Color(0xFF10B981), sizeDp = 9.dp)
                            } else {
                                Box(modifier = Modifier.size(9.dp).background(Color(0xFFEF4444), CircleShape))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when {
                                        store.isOnDuty -> "💊 Guardia Activa • Farmacia / Comercio de Turno"
                                        store.is24Hours -> "🌙 Abierto 24 Horas continuas"
                                        store.isOpen -> "🟢 Abierto Ahora (Horario Habitual)"
                                        else -> "🔴 Cerrado en este momento"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (store.isEffectivelyOpen) EmeraldDark else Color(0xFF991B1B)
                                )
                                Text(
                                    text = store.hoursText,
                                    fontSize = 11.sp,
                                    color = SlateNavy.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Detalle de días laborables vs no laborables
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = SlateNavy, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Lunes a Viernes", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateNavy)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${store.openTime} a ${store.closeTime} hs", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = EmeraldDark)
                            }
                        }

                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.EventAvailable, contentDescription = null, tint = SlateNavy, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sábados / Feriados", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateNavy)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (store.openOnHolidays) "Abre feriados: ${store.weekendHoursText}" else store.weekendHoursText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SlateNavy
                                )
                            }
                        }
                    }

                    if (store.scheduleNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ℹ️ ${store.scheduleNotes}",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Muro de Noticias Flash & Novedades para el Barrio
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Campaign, contentDescription = null, tint = RosePromo, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Noticias Flash para Vecinos", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SlateNavy)
                        }

                        Button(
                            onClick = { showPublishNewsDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RosePromo.copy(alpha = 0.12f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("+ Publicar", color = RosePromo, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    newsList.filter { it.storeId == store.id || it.storeName.contains("San Martín") }.forEach { news ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                                .padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = AmberDealContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(text = news.badge, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AmberDeal, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                                Text(text = news.timeAgoText, fontSize = 10.sp, color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = news.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SlateNavy)
                            Text(text = news.content, fontSize = 11.sp, color = Color.DarkGray)
                            TextButton(
                                onClick = {
                                    val message = "${news.badge}\n${news.title}\n${news.content}\n\nComercio: ${store.name}\n${store.address}\nAbrí la tienda en OMNI: omni://tienda/${store.id}"
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, news.title)
                                        putExtra(Intent.EXTRA_TEXT, message)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Compartir desde OMNI"))
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Compartir", fontSize = 11.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        // Regla de Descuento por Monto de Canasta
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.LocalOffer, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Regla de Ahorro Automático", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EmeraldDark)
                        }

                        Button(
                            onClick = { showThresholdDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Editar", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ofreces ${store.minSpendDiscountPercent}% de descuento directo a canastas que superen $${String.format("%,.0f", store.minSpendDiscountThreshold)}.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateNavy
                    )
                }
            }
        }

        // Catálogo de Productos del Local
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Catálogo (${store.inventory.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateNavy
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            showAddProductDialog = true
                            cameraLauncher.launch(null)
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberDeal),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("📷 Foto + IA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showAddProductDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Manual", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        items(store.inventory, key = { it.id }) { prod ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Foto del producto si tiene
                    if (prod.photoUri != null) {
                        AsyncImage(
                            model = prod.photoUri,
                            contentDescription = prod.name,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F5F9)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = prod.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SlateNavy)
                        if (prod.description.isNotBlank()) {
                            Text(text = prod.description, fontSize = 10.sp, color = Color(0xFF64748B), maxLines = 2)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "${prod.category.displayName} • En stock", fontSize = 11.sp, color = Color.Gray)
                            if (prod.barcode != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "BAR: ${prod.barcode}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SlateNavy,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "${String.format("%,.0f", prod.price)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = EmeraldPrimary
                    )
                    IconButton(
                        onClick = {
                            val message = buildString {
                                appendLine("🛍️ ${prod.name}")
                                appendLine("Comercio: ${store.name}")
                                appendLine("Categoría: ${prod.category.displayName}")
                                if (prod.description.isNotBlank()) appendLine(prod.description)
                                appendLine("Precio: ${String.format("%,.0f", prod.price)}")
                                prod.barcode?.let { appendLine("Código: $it") }
                                appendLine("Dirección: ${store.address}")
                                append("Abrí el comercio en OMNI: omni://tienda/${store.id}")
                            }
                            val imageUri = prod.photoUri?.let { raw ->
                                runCatching {
                                    val parsed = Uri.parse(raw)
                                    if (parsed.scheme == "file") {
                                        FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            File(requireNotNull(parsed.path))
                                        )
                                    } else parsed
                                }.getOrNull()
                            }
                            val send = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_SUBJECT, prod.name)
                                putExtra(Intent.EXTRA_TEXT, message)
                                if (imageUri != null) {
                                    type = "image/jpeg"
                                    putExtra(Intent.EXTRA_STREAM, imageUri)
                                    clipData = android.content.ClipData.newUri(context.contentResolver, prod.name, imageUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                } else {
                                    type = "text/plain"
                                }
                            }
                            context.startActivity(Intent.createChooser(send, "Compartir producto"))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir producto", tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Combos y Promociones
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Combos Publicados (${store.comboDeals.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateNavy
                )

                Button(
                    onClick = { showAddComboDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberDeal),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nuevo Combo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(store.comboDeals) { combo ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = combo.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SlateNavy)
                        Text(text = combo.description, fontSize = 11.sp, color = Color.Gray)
                    }
                    Surface(color = AmberDealContainer, shape = RoundedCornerShape(6.dp)) {
                        Text(text = combo.discountBadge, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberDeal, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    IconButton(
                        onClick = {
                            val message = "🔥 ${combo.title}\n${combo.description}\nDescuento: ${combo.discountBadge}\n\nComercio: ${store.name}\n${store.description}\n${store.address}\nAbrí la tienda en OMNI: omni://tienda/${store.id}"
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, combo.title)
                                putExtra(Intent.EXTRA_TEXT, message)
                            }
                            context.startActivity(Intent.createChooser(send, "Compartir promoción"))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir promoción", tint = AmberDeal, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Modal para Agregar Producto con Cámara y Código de Barras
    if (showAddProductDialog) {
        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cargar Producto al Catálogo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Sección de Cámara / Foto
                    if (prodPhotoUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = prodPhotoUri,
                                contentDescription = "Foto del producto",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .clickable { prodPhotoUri = null }
                            ) {
                                Text(
                                    text = "✕ Quitar foto",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { cameraLauncher.launch(null) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Abrir cámara", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Elegir de galería", fontSize = 11.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = prodName,
                        onValueChange = { prodName = it },
                        label = { Text("Nombre del producto") },
                        placeholder = { Text("Ej: Facturas surtidas x docena") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = prodDescription,
                        onValueChange = { prodDescription = it },
                        label = { Text("Descripción profesional") },
                        placeholder = { Text("Características y presentación del producto") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                    if (isAiAnalyzing || aiAnalysisMessage.isNotBlank()) {
                        Text(
                            text = aiAnalysisMessage,
                            fontSize = 11.sp,
                            color = if (isAiAnalyzing) EmeraldDark else SlateNavy
                        )
                    }

                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = prodCategory.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Categoría") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            ProductCategory.values().forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.displayName) },
                                    onClick = {
                                        prodCategory = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = prodPrice,
                            onValueChange = { value -> prodPrice = value.filter(Char::isDigit) },
                            label = { Text("Precio ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = prodOriginalPrice,
                            onValueChange = { value -> prodOriginalPrice = value.filter(Char::isDigit) },
                            label = { Text("Tachado ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = prodBarcode,
                        onValueChange = { prodBarcode = it },
                        label = { Text("Código de barras / SKU (opcional)") },
                        placeholder = { Text("Ej: 7791234567890") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Surface(
                        color = AmberDealContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⭐ +25 Omni Puntos para tu comercio al sumar fotos y datos reales",
                            color = AmberDeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = prodPrice.toDoubleOrNull()
                        if (prodName.isNotBlank() && price != null) {
                            val orig = prodOriginalPrice.toDoubleOrNull()
                            viewModel.addProductToManagedStore(
                                name = prodName,
                                category = prodCategory,
                                price = price,
                                originalPrice = orig,
                                photoUri = prodPhotoUri,
                                barcode = prodBarcode.ifBlank { null },
                                description = prodDescription.trim()
                            )
                            prodName = ""
                            prodDescription = ""
                            aiAnalysisMessage = ""
                            prodPrice = ""
                            prodOriginalPrice = ""
                            prodPhotoUri = null
                            prodBarcode = ""
                            showAddProductDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Guardar Producto")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddProductDialog = false
                    prodPhotoUri = null
                }) { Text("Cancelar") }
            }
        )
    }

    // Modal para Nuevo Combo
    if (showAddComboDialog) {
        AlertDialog(
            onDismissRequest = { showAddComboDialog = false },
            title = { Text("Crear Nuevo Combo de Oferta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = comboTitle,
                        onValueChange = { comboTitle = it },
                        label = { Text("Título del Combo") },
                        placeholder = { Text("Ej: Combo Merienda") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = comboDesc,
                        onValueChange = { comboDesc = it },
                        label = { Text("Descripción de productos incluidos") },
                        placeholder = { Text("Ej: Café molido + Galletitas con 20% OFF") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = comboBadge,
                        onValueChange = { comboBadge = it },
                        label = { Text("Insignia / Etiqueta de descuento") },
                        placeholder = { Text("Ej: -20% o 2x1") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (comboTitle.isNotBlank() && comboDesc.isNotBlank()) {
                            viewModel.addComboToManagedStore(comboTitle, comboDesc, comboBadge)
                            comboTitle = ""
                            comboDesc = ""
                            showAddComboDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberDeal)
                ) {
                    Text("Publicar Combo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddComboDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Modal para Publicar Novedad Flash
    if (showPublishNewsDialog) {
        AlertDialog(
            onDismissRequest = { showPublishNewsDialog = false },
            title = { Text("Publicar Novedad Flash para Vecinos") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newsTitle,
                        onValueChange = { newsTitle = it },
                        label = { Text("Título impactante") },
                        placeholder = { Text("Ej: ¡Salieron medialunas calientes!") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newsContent,
                        onValueChange = { newsContent = it },
                        label = { Text("Detalle de la novedad u oferta") },
                        placeholder = { Text("Aparecerá en el inicio de los vecinos de la zona.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newsBadge,
                        onValueChange = { newsBadge = it },
                        label = { Text("Etiqueta") },
                        placeholder = { Text("Ej: Recién Horneado 🔥") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newsTitle.isNotBlank() && newsContent.isNotBlank()) {
                            viewModel.publishStoreNews(newsTitle, newsContent, newsBadge)
                            newsTitle = ""
                            newsContent = ""
                            showPublishNewsDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RosePromo)
                ) {
                    Text("Lanzar Noticia")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPublishNewsDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Modal para Editar Regla de Descuento por Monto
    if (showThresholdDialog) {
        AlertDialog(
            onDismissRequest = { showThresholdDialog = false },
            title = { Text("Configurar Descuento por Canasta Total") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Los vecinos verán este beneficio en el comparador de precios y aumentará el ticket promedio de compra:", fontSize = 12.sp)
                    OutlinedTextField(
                        value = thresholdAmount,
                        onValueChange = { thresholdAmount = it },
                        label = { Text("Monto mínimo de compra ($)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = thresholdPercent,
                        onValueChange = { thresholdPercent = it },
                        label = { Text("Porcentaje de descuento (%)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val thresh = thresholdAmount.toDoubleOrNull()
                        val perc = thresholdPercent.toIntOrNull()
                        if (thresh != null && perc != null) {
                            viewModel.updateDiscountThreshold(thresh, perc)
                            showThresholdDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Guardar Regla")
                }
            },
            dismissButton = {
                TextButton(onClick = { showThresholdDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showScheduleDialog) {
        AlertDialog(
            onDismissRequest = { showScheduleDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Configurar Horarios & Turno", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Configura tus horarios para que los clientes te encuentren cuando buscan comercios abiertos ahora o de turno en feriados y horarios nocturnos.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editOpenTime,
                            onValueChange = { editOpenTime = it },
                            label = { Text("Abre (Lun-Vie)") },
                            placeholder = { Text("08:30") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editCloseTime,
                            onValueChange = { editCloseTime = it },
                            label = { Text("Cierra (Lun-Vie)") },
                            placeholder = { Text("21:00") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = editWeekendHours,
                        onValueChange = { editWeekendHours = it },
                        label = { Text("Fines de semana y no laborables") },
                        placeholder = { Text("Sáb: 09:00 - 14:00 • Dom: Cerrado") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editScheduleNotes,
                        onValueChange = { editScheduleNotes = it },
                        label = { Text("Avisos (Ventanilla, timbre, etc.)") },
                        placeholder = { Text("Atención nocturna por ventanilla") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Switch 24hs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Atención 24 Horas", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Abierto día y noche sin interrupción", fontSize = 10.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = editIs24Hours,
                            onCheckedChange = { editIs24Hours = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldPrimary)
                        )
                    }

                    // Switch De Turno (Guardia)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("De Turno / Guardia Activa 💊", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Aparecer prioritario en farmacias y guardias barriales", fontSize = 10.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = editIsOnDuty,
                            onCheckedChange = { editIsOnDuty = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldPrimary)
                        )
                    }

                    // Switch Abre Feriados
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Abre en Feriados 📅", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Atención en días no laborables y festivos", fontSize = 10.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = editOpenOnHolidays,
                            onCheckedChange = { editOpenOnHolidays = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldPrimary)
                        )
                    }

                    // Switch Abierto ahora mismo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Abierto ahora mismo", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Mostrar en verde como abierto a los vecinos", fontSize = 10.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = editIsOpenNow,
                            onCheckedChange = { editIsOpenNow = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldPrimary)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateManagedStoreSchedule(
                            openTime = editOpenTime.ifBlank { "08:30" },
                            closeTime = editCloseTime.ifBlank { "21:00" },
                            weekendHoursText = editWeekendHours.ifBlank { "Sáb: 09:00 - 14:00" },
                            is24Hours = editIs24Hours,
                            isOnDuty = editIsOnDuty,
                            openOnHolidays = editOpenOnHolidays,
                            scheduleNotes = editScheduleNotes,
                            isOpenNow = editIsOpenNow
                        )
                        showScheduleDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Guardar Horarios")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScheduleDialog = false }) { Text("Cancelar") }
            }
        )
    }
}
