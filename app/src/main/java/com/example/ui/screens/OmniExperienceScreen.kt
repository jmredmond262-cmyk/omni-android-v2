package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Store
import com.example.ui.MainViewModel
import com.example.ui.Screen

private val ExperienceBackground = Color(0xFF070A14)
private val ExperienceCard = Color(0xFF101B30)
private val ExperienceCyan = Color(0xFF57DDF5)
private val ExperiencePurple = Color(0xFFB879FF)

@Composable
fun OmniExperienceScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val stores by viewModel.stores.collectAsState()
    val attractions by viewModel.touristAttractions.collectAsState()
    val rewards by viewModel.userRewards.collectAsState()
    val city by viewModel.currentCity.collectAsState()
    var section by remember { mutableStateOf("Todos") }
    val sections = listOf("Todos", "Jóvenes", "Turistas", "Usuarios", "Flash", "Puntos")
    val discountStores = stores.filter { it.minSpendDiscountPercent > 0 && it.minSpendDiscountThreshold > 0.0 }
    val flashStores = stores.filter { it.comboDeals.isNotEmpty() }
    val studentDeals = stores.flatMap { store -> store.comboDeals.filter { it.audience == "ESTUDIANTES" }.map { store to it } }
    val touristDeals = stores.flatMap { store -> store.comboDeals.filter { it.audience == "TURISTAS" }.map { store to it } }
    val userDeals = stores.flatMap { store -> store.comboDeals.filter { it.audience == "TODOS" || it.audience == "USUARIOS" }.map { store to it } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(ExperienceBackground),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text("OFERTAS Y BENEFICIOS", color = ExperiencePurple, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                    Text("Promociones, combos y beneficios de tu zona", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ExperiencePurple)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = ExperienceCard), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Tu experiencia OMNI en " + city.ifBlank { "tu zona" }, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Descubrí propuestas locales, oportunidades y beneficios disponibles. La disponibilidad depende de los comercios y publicaciones activas.", color = Color(0xFFB8C8D9), fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                sections.forEach { value ->
                    FilterChip(
                        selected = section == value,
                        onClick = { section = value },
                        label = { Text(value, fontSize = 9.sp) }
                    )
                }
            }
        }
        if (section == "Todos" || section == "Jóvenes") {
            item { ExperienceSectionHeader("🎓", "OMNI JOVEN", "Descuentos locales para consultar") }
            if (discountStores.isEmpty()) {
                item { ExperienceEmptyState("Todavía no hay descuentos por compra mínima publicados en los comercios disponibles.") }
            } else {
                items(discountStores, key = { "young-" + it.id }) { store ->
                    DiscountOfferCard(store = store, onOpen = { viewModel.selectStoreAndOpenDetail(store) })
                }
            }
            item {
                Text("Los descuentos mostrados no son exclusivos para estudiantes salvo que el comercio lo indique. Consultá condiciones y acreditación antes de comprar.", color = Color(0xFF8FA2BA), fontSize = 9.sp, lineHeight = 13.sp)
            }
            item { ExperienceSectionHeader("🎓", "PROMOCIONES PARA ESTUDIANTES", "Beneficios publicados específicamente para OMNI Joven") }
            if (studentDeals.isEmpty()) {
                item { ExperienceEmptyState("Todavía no hay combos exclusivos para estudiantes publicados por comercios.") }
            } else {
                items(studentDeals, key = { "student-" + it.first.id + "-" + it.second.title }) { offer ->
                    AudienceDealCard(store = offer.first, deal = offer.second)
                }
            }
        }
        if (section == "Todos" || section == "Turistas") {
            item { ExperienceSectionHeader("🧳", "OMNI TURISTA", "Atractivos cercanos a tu ubicación") }
            if (attractions.isEmpty()) {
                item { ExperienceEmptyState("No hay atractivos cargados para esta ubicación. Actualizá tu GPS o seleccioná una localidad.") }
            } else {
                items(attractions.take(12), key = { "tour-" + it.name }) { attraction ->
                    Card(colors = CardDefaults.cardColors(containerColor = ExperienceCard), shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(attraction.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(attraction.category + " · " + String.format("%.1f", attraction.distanceKm) + " km · " + attraction.estimatedVisitTime, color = ExperienceCyan, fontSize = 10.sp)
                            Text(attraction.description, color = Color(0xFFB8C8D9), fontSize = 11.sp)
                            Button(
                                onClick = {
                                    viewModel.searchTrip(city.ifBlank { "Mi ubicación" }, attraction.name)
                                    viewModel.navigateTo(Screen.TripPlanner)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF164B63)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                            ) { Text("Planificar visita", fontSize = 10.sp) }
                        }
                    }
                }
            }
            item { ExperienceSectionHeader("🧳", "BENEFICIOS PARA TURISTAS", "Promociones identificadas para visitantes") }
            if (touristDeals.isEmpty()) {
                item { ExperienceEmptyState("No hay promociones para turistas publicadas en esta zona por ahora.") }
            } else {
                items(touristDeals, key = { "tour-deal-" + it.first.id + "-" + it.second.title }) { offer ->
                    AudienceDealCard(store = offer.first, deal = offer.second)
                }
            }
        }
        if (section == "Todos" || section == "Usuarios") {
            item { ExperienceSectionHeader("⭐", "BENEFICIOS PARA USUARIOS", "Promociones generales de comercios adheridos") }
            if (userDeals.isEmpty()) {
                item { ExperienceEmptyState("Todavía no hay promociones generales publicadas.") }
            } else {
                items(userDeals, key = { "user-deal-" + it.first.id + "-" + it.second.title }) { offer ->
                    AudienceDealCard(store = offer.first, deal = offer.second)
                }
            }
            if (discountStores.isNotEmpty()) {
                items(discountStores, key = { "user-discount-" + it.id }) { store ->
                    DiscountOfferCard(store = store, onOpen = { viewModel.selectStoreAndOpenDetail(store) })
                }
            }
        }
        if (section == "Todos" || section == "Flash") {
            item { ExperienceSectionHeader("⚡", "OMNI FLASH", "Combos y promociones de comercios") }
            if (flashStores.isEmpty()) {
                item { ExperienceEmptyState("Todavía no hay combos publicados en los comercios disponibles.") }
            } else {
                items(flashStores, key = { "flash-" + it.id }) { store ->
                    store.comboDeals.forEach { deal ->
                        Card(colors = CardDefaults.cardColors(containerColor = ExperienceCard), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(deal.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Surface(color = Color(0xFF302044), shape = RoundedCornerShape(7.dp)) {
                                        Text(deal.discountBadge, color = ExperiencePurple, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                                    }
                                }
                                Text(store.name, color = ExperienceCyan, fontSize = 10.sp)
                                Text(deal.description, color = Color(0xFFB8C8D9), fontSize = 11.sp)
                                Text("Consultá vigencia y condiciones con el comercio.", color = Color(0xFF8FA2BA), fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
        if (section == "Todos" || section == "Puntos") {
            item { ExperienceSectionHeader("⭐", "OMNI PUNTOS", "Tu progreso y recompensas") }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = ExperienceCard), shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(rewards.points.toString() + " puntos", color = ExperienceCyan, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Text(rewards.levelName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Los puntos y recompensas visibles en esta etapa son de demostración; no equivalen a dinero ni modifican el saldo de OMNI Pay.", color = Color(0xFFB8C8D9), fontSize = 10.sp, lineHeight = 15.sp)
                        Button(onClick = { viewModel.navigateTo(Screen.Wallet) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF164B63))) {
                            Text("Abrir OMNI Pay", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
        item {
            ExperienceSectionHeader("🚀", "MÁS DE OMNI", "Accesos rápidos")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.navigateTo(Screen.Events) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF17314A))) {
                    Icon(Icons.Default.Event, contentDescription = null)
                    Text("Eventos", fontSize = 10.sp)
                }
                Button(onClick = { viewModel.navigateTo(Screen.JobBoard) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF17314A))) {
                    Icon(Icons.Default.Work, contentDescription = null)
                    Text("Oportunidades", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun ExperienceSectionHeader(emoji: String, title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(emoji + "  " + title, color = ExperiencePurple, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)
        Text(subtitle, color = Color(0xFF9AAAC2), fontSize = 10.sp)
    }
}

@Composable
private fun ExperienceEmptyState(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = ExperienceCard), shape = RoundedCornerShape(12.dp)) {
        Text(message, color = Color(0xFFB8C8D9), fontSize = 11.sp, modifier = Modifier.padding(13.dp))
    }
}

@Composable
private fun AudienceDealCard(store: Store, deal: com.example.model.ComboDeal) {
    Card(colors = CardDefaults.cardColors(containerColor = ExperienceCard), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(deal.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(color = Color(0xFF302044), shape = RoundedCornerShape(7.dp)) {
                    Text(deal.discountBadge, color = ExperiencePurple, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                }
            }
            Text(store.name, color = ExperienceCyan, fontSize = 10.sp)
            Text(deal.description, color = Color(0xFFB8C8D9), fontSize = 11.sp)
            Text("Consultá vigencia y condiciones con el comercio.", color = Color(0xFF8FA2BA), fontSize = 9.sp)
        }
    }
}

@Composable
private fun DiscountOfferCard(store: Store, onOpen: () -> Unit) {
    Card(onClick = onOpen, colors = CardDefaults.cardColors(containerColor = ExperienceCard), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(store.minSpendDiscountPercent.toString() + "% de descuento", color = ExperienceCyan, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text(store.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("En compras desde " + String.format("$%,.0f", store.minSpendDiscountThreshold), color = Color(0xFFB8C8D9), fontSize = 10.sp)
            Text("Consultá condiciones y vigencia con el comercio. Tocá para ver el local.", color = Color(0xFF8FA2BA), fontSize = 9.sp)
        }
    }
}
