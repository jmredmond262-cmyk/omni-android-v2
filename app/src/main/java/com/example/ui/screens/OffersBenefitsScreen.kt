package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.text.NumberFormat
import java.util.Locale

private val OffersBg = Color(0xFF070A14)
private val OffersCard = Color(0xFF111D32)
private val OffersCyan = Color(0xFF57DDF5)
private val OffersPurple = Color(0xFFB69CFF)
private val OffersMuted = Color(0xFFB8C8D9)

private data class DemoOffer(val id: String, val title: String, val detail: String, val type: String, val samplePrice: Int)
private val demoOffers = listOf(
    DemoOffer("promo-coffee-250", "Promo café clásico", "Producto de catálogo: café molido de 250 g. Promoción ilustrativa, sin descuento comercial real.", "OMNI-DEMO-OFFER", 4200),
    DemoOffer("combo-breakfast-01", "Combo desayuno", "Incluye 1 café molido de 250 g + 1 leche larga vida de 1 litro. Precios calculados desde el catálogo DEMO vigente.", "OMNI-DEMO-COMBO", 0)
)

private fun createOfferQr(payload: String): Bitmap? = runCatching {
    val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 420, 420)
    Bitmap.createBitmap(420, 420, Bitmap.Config.RGB_565).also { bitmap ->
        for (x in 0 until 420) for (y in 0 until 420) bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
    }
}.getOrNull()

@Composable
fun OffersBenefitsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var qrPayload by remember { mutableStateOf<String?>(null) }
    var scanResult by remember { mutableStateOf<String?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(OffersBg),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White) }
                Column(Modifier.weight(1f)) {
                    Text("DESCUBRIMIENTO OMNI", color = OffersPurple, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Ofertas y beneficios", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
                Icon(Icons.Default.LocalOffer, null, tint = OffersCyan, modifier = Modifier.size(25.dp))
            }
        }
        item {
            Surface(color = Color(0xFF201B35), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("ENTORNO DE DEMOSTRACIÓN", color = OffersPurple, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                    Text("Las promociones visibles son ficticias y no pertenecen a comercios reales. No garantizan stock, descuentos ni disponibilidad.", color = Color.White, fontSize = 11.sp)
                }
            }
        }
        item {
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                GmsBarcodeScanning.getClient(context).startScan()
                    .addOnSuccessListener { barcode ->
                        val parts = barcode.rawValue.orEmpty().split("|")
                        scanResult = if (parts.size == 2) {
                            demoOffers.firstOrNull { it.id == parts[1] && it.type == parts[0] }
                                ?.let { "QR reconocido: ${it.title}. ${it.detail} El precio se consulta en el catálogo actual, nunca se toma de un valor fijo dentro del QR." }
                                ?: "El código no corresponde a una promoción o combo disponible en este catálogo DEMO."
                        } else "Formato QR no reconocido."
                    }
                    .addOnFailureListener { scanError = "No se pudo iniciar el lector QR." }
            }) { Icon(Icons.Default.QrCode2, null); Text(" Escanear QR de promoción o combo") }
            scanError?.let { Text(it, color = Color(0xFFFFA5A5), fontSize = 10.sp) }
        }
        items(demoOffers.size) { index ->
            val offer = demoOffers[index]
            Card(colors = CardDefaults.cardColors(containerColor = OffersCard), shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, OffersCyan.copy(alpha = 0.22f))) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (offer.type == "OMNI-DEMO-OFFER") "PROMOCIÓN DEMO" else "COMBO DEMO", color = OffersPurple, fontWeight = FontWeight.ExtraBold, fontSize = 9.sp)
                    Text(offer.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(offer.detail, color = OffersMuted, fontSize = 11.sp)
                    if (offer.samplePrice > 0) Text("Precio de referencia ilustrativo: " + "$ " + NumberFormat.getNumberInstance(Locale("es", "AR")).format(offer.samplePrice), color = OffersCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedButton(onClick = { qrPayload = offer.type + "|" + offer.id }) {
                        Icon(Icons.Default.QrCode2, null); Text(" Ver QR")
                    }
                }
            }
        }
        item {
            Text("Los QR contienen únicamente el tipo y el identificador estable. Para validar pedidos entre dispositivos o canjear beneficios reales hará falta una fuente compartida y autorización específica; esta prueba no modifica Firestore ni sus reglas.", color = OffersMuted, fontSize = 10.sp)
        }
    }
    qrPayload?.let { payload ->
        val bitmap = remember(payload) { createOfferQr(payload) }
        AlertDialog(onDismissRequest = { qrPayload = null }, title = { Text("QR de ${payload.substringBefore("|").removePrefix("OMNI-DEMO-")}") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    bitmap?.let { Image(it.asImageBitmap(), contentDescription = "Código QR DEMO", modifier = Modifier.size(220.dp)) }
                    Text("ID: ${payload.substringAfter("|")}. No contiene un precio fijo ni acredita pagos.", color = OffersMuted, fontSize = 10.sp)
                }
            },
            confirmButton = { TextButton(onClick = { qrPayload = null }) { Text("Cerrar") } })
    }
    scanResult?.let { result ->
        AlertDialog(onDismissRequest = { scanResult = null }, title = { Text("Resultado del escaneo") },
            text = { Text(result, fontSize = 12.sp) }, confirmButton = { TextButton(onClick = { scanResult = null }) { Text("Aceptar") } })
    }
}
