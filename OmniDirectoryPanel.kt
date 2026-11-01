package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.FiltrosLugaresService
import com.example.data.api.Lugar
import com.example.ui.MainViewModel

private val directoryCategories = listOf(
    "all" to "Todo", "education" to "Educación", "health" to "Salud",
    "commerce" to "Comercios", "services" to "Servicios / mutuales",
    "government" to "Organismos", "food" to "Gastronomía",
    "transport" to "Transporte", "tourism" to "Turismo",
    "finance" to "Finanzas", "auto" to "Automotores"
)
private val directoryRadii = listOf(1000, 3000, 5000, 10000, 25000, 50000, 100000)

@Composable
fun OmniDirectoryPanel(
    viewModel: MainViewModel,
    latUsuario: Double,
    lonUsuario: Double
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val results by viewModel.omniDirectoryResults.collectAsState()
    val loading by viewModel.isSearchingOmniDirectory.collectAsState()
    val message by viewModel.omniDirectoryMessage.collectAsState()
    val city by viewModel.currentCity.collectAsState()
    val savedRadius by viewModel.searchRadiusMeters.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    var category by remember { mutableStateOf("all") }
    var radius by remember(savedRadius) { mutableStateOf((savedRadius / 1000f).coerceIn(1f, 20f)) }
    var soloAbiertos by remember { mutableStateOf(false) }

    val lugares = remember(results) {
        results.map { place ->
            Lugar(
                id = place.id,
                nombre = place.name,
                rubro = place.category,
                // Solo se considera abierto cuando OSM informa explícitamente atención 24/7.
                // Otros horarios requieren interpretar el calendario completo antes de afirmar estado.
                estaAbierto = if (
                    place.openingHours.contains("24/7", true) ||
                    place.openingHours.contains("24 horas", true)
                ) true else null,
                latitud = place.latitude,
                longitud = place.longitude,
                horario = place.openingHours.ifBlank { "Horario no informado" }
            )
        }
    }

    val resultadosFiltrados = remember(
        lugares, latUsuario, lonUsuario, radius, soloAbiertos
    ) {
        FiltrosLugaresService.filtrarYOrdenar(
            todosLosLugares = lugares,
            latUsuario = latUsuario,
            lonUsuario = lonUsuario,
            rubroFiltro = null,
            soloAbiertos = soloAbiertos,
            radioMaximoKm = radius
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Buscador integral OMNI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 7.dp)
                )
            }
            Text(
                "Lugares cercanos a $city",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(9.dp))
            Text("Categorías", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(directoryCategories) { option ->
                    FilterChip(
                        selected = category == option.first,
                        onClick = { category = option.first },
                        label = { Text(option.second, fontSize = 11.sp) }
                    )
                }
            }
            Spacer(Modifier.height(7.dp))
            Text("Radio: ${radius.toInt()} km", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Slider(
                value = radius,
                onValueChange = { radius = it },
                valueRange = 1f..20f,
                steps = 18
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = soloAbiertos, onCheckedChange = { soloAbiertos = it })
                Text("Mostrar solo abiertos verificables", fontSize = 12.sp)
            }
            Text(
                "El estado de apertura solo se confirma cuando la fuente informa atención 24/7. Los horarios comunes pueden estar incompletos.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(9.dp))
            Button(
                onClick = {
                    val radiusMeters = (radius * 1000).toInt().coerceIn(1000, 20000)
                    viewModel.searchOmniDirectory(context, category, radiusMeters)
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                androidx.compose.material3.Icon(Icons.Default.Search, contentDescription = null)
                Text(if (loading) "Buscando lugares..." else "Buscar en esta zona")
            }
            if (message.isNotBlank()) {
                Text(message, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
            if (results.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                ) {
                    Text(
                        "${resultadosFiltrados.size} de ${results.size} lugares · ordenados por cercanía",
                        modifier = Modifier.padding(10.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                resultadosFiltrados.forEach { item ->
                    val place = results.firstOrNull { it.id == item.lugar.id } ?: return@forEach
                    val distanceText = if (item.distanciaMetros < 1000f) {
                        "${item.distanciaMetros.toInt()} m"
                    } else {
                        String.format(java.util.Locale.getDefault(), "%.1f km", item.distanciaMetros / 1000f)
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(11.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(11.dp)) {
                            Text(place.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "${place.category} · ${place.typeLabel} · $distanceText",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(place.address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (place.phone.isNotBlank()) Text("Tel. ${place.phone}", fontSize = 11.sp)
                            Text(
                                if (place.openingHours.isNotBlank()) "Horario informado: ${place.openingHours}" else "Horario no informado",
                                fontSize = 11.sp
                            )
                            Text(
                                when (item.lugar.estaAbierto) {
                                    true -> "Abierto 24/7 (según OpenStreetMap)"
                                    false -> "Cerrado"
                                    null -> "Estado de apertura no verificado"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Ver ficha y ubicación en OpenStreetMap",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 5.dp).clickable { uriHandler.openUri(place.osmUrl) }
                            )
                        }
                    }
                }
                if (resultadosFiltrados.isEmpty()) {
                    Text(
                        if (soloAbiertos) "No hay lugares con apertura 24/7 verificada en estos resultados."
                        else "No hay lugares dentro del radio seleccionado.",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }
    }
}
