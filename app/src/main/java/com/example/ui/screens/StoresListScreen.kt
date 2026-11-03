package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Phone
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Store
import com.example.model.isOpenAtCurrentTime
import com.example.model.StoreType
import com.example.ui.MainViewModel
import com.example.ui.screens.OmniDirectoryPanel
import com.example.ui.StoreScheduleFilter
import com.example.ui.components.DistancePill
import com.example.ui.components.OpenStatusBadge
import com.example.ui.components.PulsingStatusDot
import com.example.ui.components.RatingBadge
import com.example.ui.components.StoreTypeChip
import com.example.ui.components.VideoTourTag
import com.example.ui.components.getStoreGradient
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RosePromo
import com.example.ui.theme.SlateNavy
import com.example.ui.util.GoogleMapsHelper


@Composable
fun StoresListScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val stores by viewModel.stores.collectAsState()
    val scheduleFilter by viewModel.scheduleFilter.collectAsState()
    val isLoadingGps by viewModel.isLoadingLiveLocation.collectAsState()
    val searchRadius by viewModel.searchRadiusMeters.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val currentCity by viewModel.currentCity.collectAsState()
    var clockMinute by remember { mutableStateOf(System.currentTimeMillis() / 60000L) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000L)
            clockMinute = System.currentTimeMillis() / 60000L
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<StoreType?>(null) }

    val filteredAndRankedStores = remember(stores, searchQuery, selectedType, scheduleFilter, currentCity, clockMinute) {
        stores.filter { store ->
            val localityToken = currentCity.substringBefore(",").trim()
            val matchesLocality = localityToken.isNotBlank() && store.address.contains(localityToken, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                store.name.contains(searchQuery, ignoreCase = true) ||
                store.address.contains(searchQuery, ignoreCase = true) ||
                store.description.contains(searchQuery, ignoreCase = true)

            val matchesType = selectedType == null || store.type == selectedType

            val matchesSchedule = when (scheduleFilter) {
                StoreScheduleFilter.TODOS -> true
                StoreScheduleFilter.ABIERTOS_AHORA -> store.isOpenAtCurrentTime()
                StoreScheduleFilter.FARMACIAS_TURNO -> store.isOnDuty
                StoreScheduleFilter.TURNO_24HS -> store.is24Hours || store.isOnDuty
                StoreScheduleFilter.FERIADOS -> store.openOnHolidays || store.is24Hours || store.isOnDuty
            }

            matchesLocality && matchesSearch && matchesType && matchesSchedule
        }.sortedWith(
            // Priorizar los que están abiertos o de turno en ese horario sobre los cerrados
            compareByDescending<Store> {
                when {
                    it.isOnDuty -> 4
                    it.is24Hours -> 3
                    it.isOpenAtCurrentTime() -> 2
                    else -> 0
                }
            }.thenBy { it.distanceMeters }
        )
    }

    var showRadarControls by remember { mutableStateOf(false) }
    var showOsmAdvancedPanel by remember { mutableStateOf(false) }
    var showDemoKeyDialog by remember { mutableStateOf(false) }
    var inputDemoKey by remember { mutableStateOf(viewModel.getGoogleApiKey(context)) }

    if (showDemoKeyDialog) {
        AlertDialog(
            onDismissRequest = { showDemoKeyDialog = false },
            title = {
                Text("🔑 Clave Google Places API (New)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Ingresa tu clave de demostración para consultar en vivo los comercios reales de Google Maps.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = inputDemoKey,
                        onValueChange = { inputDemoKey = it },
                        label = { Text("API Key de Google") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text(
                        "Al guardar, el radar escaneará inmediatamente con la API oficial de Google Maps.",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setDemoGoogleApiKey(context, inputDemoKey)
                        showDemoKeyDialog = false
                    }
                ) {
                    Text("Guardar y Escanear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDemoKeyDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("stores_list_screen"),
        contentPadding = PaddingValues(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Buscador principal compacto y visible
        item {
            Surface(modifier = Modifier.fillMaxWidth().height(46.dp), color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(19.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (searchQuery.isBlank()) Text("Buscar comercios o servicios...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        BasicTextField(value = searchQuery, onValueChange = { searchQuery = it }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp), modifier = Modifier.fillMaxWidth().testTag("input_search_stores"))
                    }
                    if (searchQuery.isNotBlank()) IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Clear, contentDescription = "Borrar búsqueda", modifier = Modifier.size(17.dp)) }
                }
            }
        }

        // Banner de Detección GPS en Vivo con Radio Configurable y Botón Buscar Nuevamente
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().clickable { showRadarControls = !showRadarControls }.padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(9.dp).size(18.dp))
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Radar cercano", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("$currentCity · ${if (searchRadius >= 1000) "${searchRadius / 1000} km" else "$searchRadius m"}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp)) {
                            Text("${filteredAndRankedStores.size}", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                        }
                        Icon(Icons.Default.Map, contentDescription = "Configurar radar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                        Text(if (showRadarControls) "▲" else "▼", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    if (showRadarControls) {
                        val activeKey = viewModel.getGoogleApiKey(context)
                        val isGoogleActive = activeKey.isNotBlank()
    
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isGoogleActive) "🟢 Google Places API (New) Activa" else "🗺️ Google Places: Clave de demostración",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark
                            )
                            TextButton(
                                onClick = {
                                    inputDemoKey = viewModel.getGoogleApiKey(context)
                                    showDemoKeyDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = if (isGoogleActive) "Editar clave" else "🔑 Cargar clave",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
    
                        Spacer(modifier = Modifier.height(10.dp))
    
                        // Selector de Radio de Búsqueda Configurable
                        Text(
                            text = "Configurar Radio de Cobertura:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark
                        )
    
                        Spacer(modifier = Modifier.height(6.dp))
    
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(500 to "500 m", 1000 to "1 km", 2000 to "2 km", 5000 to "5 km", 10000 to "10 km").forEach { (meters, label) ->
                                val isSelected = searchRadius == meters
                                Surface(
                                    color = if (isSelected) EmeraldPrimary else Color.White,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setSearchRadius(meters, context) }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else SlateNavy
                                        )
                                    }
                                }
                            }
                        }
    
                        Spacer(modifier = Modifier.height(10.dp))
    
                        // BOTÓN PRINCIPAL: Buscar Nuevamente en esta área
                        Button(
                            onClick = { viewModel.rescanAreaStores(context) },
                            enabled = !isLoadingGps,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isLoadingGps) "Escaneando comercios en vivo..." else "🔄 Buscar Nuevamente en Esta Área",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
    
                        Spacer(modifier = Modifier.height(8.dp))
    
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✅ ${filteredAndRankedStores.size} locales escaneados y listados directamente en tu app",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SlateNavy
                                )
                            }
                        }
    
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    TextButton(
                        onClick = { showOsmAdvancedPanel = !showOsmAdvancedPanel },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = if (showOsmAdvancedPanel) "▲ Ocultar Búsqueda Extendida OSM" else "▼ Búsqueda Extendida: Salud, Escuelas, Organismos (OSM)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark
                        )
                    }
                }
            }
        }

        if (showOsmAdvancedPanel) {
            item {
                OmniDirectoryPanel(
                    viewModel = viewModel,
                    latUsuario = userProfile.currentLatitude,
                    lonUsuario = userProfile.currentLongitude
                )
            }
        }

        // Filtro por Horarios & Guardia (Requerimiento Clave)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filtro por Horario & Guardia:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateNavy
                    )
                    Text(
                        text = "${filteredAndRankedStores.size} encontrados",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(StoreScheduleFilter.values()) { filter ->
                        FilterChip(
                            selected = scheduleFilter == filter,
                            onClick = { viewModel.setScheduleFilter(filter) },
                            label = { Text(filter.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("filter_schedule_${filter.name}")
                        )
                    }
                }
            }
        }

        // Filtro por tipos de comercio
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedType == null,
                        onClick = { selectedType = null },
                        label = { Text("Rubro: Todos", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                items(StoreType.values()) { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = if (selectedType == type) null else type },
                        label = { Text(type.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Resultados con ranking de comercios abiertos arriba
        if (filteredAndRankedStores.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.Bedtime, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = if (stores.isEmpty()) "No encontramos comercios registrados en esta zona" else "No hay comercios con este filtro o búsqueda", fontWeight = FontWeight.Bold, color = SlateNavy, fontSize = 14.sp)
                        Text(text = if (stores.isEmpty()) "Pulsa para cargar los comercios verificados de Romang." else "Prueba seleccionando 'Todos' o borrando el texto de búsqueda.", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                searchQuery = ""
                                selectedType = null
                                viewModel.setScheduleFilter(StoreScheduleFilter.TODOS)
                                viewModel.rescanAreaStores(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("Ver Todos los Locales de Romang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(filteredAndRankedStores, key = { it.id }) { store ->
                StoreDirectoryCard(
                    store = store,
                    onOpenDetail = { viewModel.selectStoreAndOpenDetail(store) },
                    onOpenRoute = { viewModel.selectStoreAndOpenRoute(store) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun StoreDirectoryCard(
    store: Store,
    onOpenDetail: () -> Unit,
    onOpenRoute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outline.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth().clickable { onOpenDetail() }
            .testTag("directory_store_${store.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(store.name, color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, modifier = Modifier.weight(1f))
                    Surface(color = colors.surfaceVariant, shape = RoundedCornerShape(7.dp)) {
                        Text(store.distanceText, color = colors.onSurfaceVariant, fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(color = colors.primary.copy(alpha = 0.13f), shape = RoundedCornerShape(6.dp)) {
                        Text(store.type.name.replace("_", " "), color = colors.primary, fontSize = 9.sp,
                            fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                    }
                    val scheduleKnown = store.hoursText.isNotBlank() && !store.hoursText.equals("Horario no informado", ignoreCase = true)
                    val openNow = store.isOpenAtCurrentTime()
                    val openLabel = when {
                        store.isOnDuty -> "DE TURNO"
                        store.is24Hours -> "24 HORAS"
                        !scheduleKnown -> "HORARIO S/D"
                        openNow -> "ABIERTO"
                        else -> "CERRADO"
                    }
                    Text(openLabel,
                        color = if (scheduleKnown && (openNow || store.isOnDuty || store.is24Hours)) colors.primary else colors.onSurfaceVariant,
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    if (store.rating > 0f && store.reviewsCount > 0) {
                        Text("★ ${"%.1f".format(store.rating)}", color = colors.tertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(store.address, color = colors.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
                Text("Ver perfil y toda la información  ›", color = colors.primary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = onOpenRoute, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.Directions, contentDescription = "Trazar ruta a ${store.name}",
                    tint = colors.primary, modifier = Modifier.size(21.dp))
            }
        }
    }
}
