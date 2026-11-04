package com.example.ui.screens

import android.content.Context
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
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ShoppingCart
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
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val DemoBg = Color(0xFF070A14)
private val DemoCard = Color(0xFF111D32)
private val DemoCyan = Color(0xFF57DDF5)
private val DemoViolet = Color(0xFFB69CFF)
private val DemoMuted = Color(0xFFB8C8D9)

private data class DemoProduct(
    val id: String, val name: String, val category: String, val unit: String,
    val firstPrice: Int, val secondPrice: Int, val emoji: String, val offer: String = ""
)
private data class DemoOrder(val id: String, val total: Int, val delivery: String, val date: String)

private val demoCatalog = listOf(
    DemoProduct("demo-milk-1l", "Leche entera larga vida", "Lácteos", "1 litro", 1450, 1390, "🥛"),
    DemoProduct("demo-coffee-250", "Café molido clásico", "Almacén", "250 g", 4200, 4490, "☕", "PROMO DEMO"),
    DemoProduct("demo-rice-1k", "Arroz largo fino", "Almacén", "1 kg", 1750, 1690, "🍚"),
    DemoProduct("demo-oil-15", "Aceite de girasol", "Almacén", "1,5 litros", 2850, 2790, "🫒"),
    DemoProduct("demo-eggs-12", "Huevos de granja", "Frescos", "12 unidades", 3600, 3490, "🥚"),
    DemoProduct("demo-cheese-500", "Queso cremoso", "Lácteos", "500 g", 4650, 4790, "🧀"),
    DemoProduct("demo-water-2l", "Agua mineral", "Bebidas", "2 litros", 1100, 990, "💧"),
    DemoProduct("demo-detergent", "Detergente concentrado", "Limpieza", "750 ml", 2450, 2390, "🧴")
)

private fun demoPrice(value: Int): String =
    "$ " + NumberFormat.getNumberInstance(Locale("es", "AR")).format(value)

private fun demoQr(payload: String): Bitmap? = runCatching {
    val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 420, 420)
    Bitmap.createBitmap(420, 420, Bitmap.Config.RGB_565).also { bitmap ->
        for (x in 0 until 420) for (y in 0 until 420) {
            bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
}.getOrNull()

private fun readDemoOrders(context: Context): List<DemoOrder> = runCatching {
    val raw = context.getSharedPreferences("omni_demo_market", Context.MODE_PRIVATE).getString("orders", "[]") ?: "[]"
    val array = JSONArray(raw)
    (0 until array.length()).mapNotNull { index ->
        val item = array.optJSONObject(index) ?: return@mapNotNull null
        DemoOrder(item.optString("id"), item.optInt("total"), item.optString("delivery"), item.optString("date"))
    }
}.getOrDefault(emptyList())

private fun writeDemoOrders(context: Context, orders: List<DemoOrder>) {
    val array = JSONArray()
    orders.forEach { order ->
        array.put(JSONObject().put("id", order.id).put("total", order.total)
            .put("delivery", order.delivery).put("date", order.date))
    }
    context.getSharedPreferences("omni_demo_market", Context.MODE_PRIVATE)
        .edit().putString("orders", array.toString()).apply()
}

private fun readDemoCart(context: Context): Map<String, Int> = runCatching {
    val raw = context.getSharedPreferences("omni_demo_market", Context.MODE_PRIVATE)
        .getString("cart", "{}") ?: "{}"
    val json = JSONObject(raw)
    json.keys().asSequence().mapNotNull { key ->
        val quantity = json.optInt(key, 0)
        if (quantity > 0) key to quantity else null
    }.toMap()
}.getOrDefault(emptyMap())

private fun writeDemoCart(context: Context, cart: Map<String, Int>) {
    val json = JSONObject()
    cart.filterValues { it > 0 }.forEach { (id, quantity) -> json.put(id, quantity) }
    context.getSharedPreferences("omni_demo_market", Context.MODE_PRIVATE)
        .edit().putString("cart", json.toString()).apply()
}

@Composable
fun DemoMarketplaceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val cart = remember { mutableStateMapOf<String, Int>().apply { putAll(readDemoCart(context)) } }
    val orders = remember { mutableStateListOf<DemoOrder>().apply { addAll(readDemoOrders(context)) } }
    LaunchedEffect(cart.toMap()) { writeDemoCart(context, cart.toMap()) }
    var store by remember { mutableIntStateOf(0) }
    var checkout by remember { mutableStateOf(false) }
    var fulfillment by remember { mutableStateOf("RETIRO") }
    var address by remember { mutableStateOf("") }
    var qrPayload by remember { mutableStateOf<String?>(null) }
    var qrTitle by remember { mutableStateOf("Código QR DEMO") }
    var scanResult by remember { mutableStateOf<String?>(null) }
    var scannedProductId by remember { mutableStateOf<String?>(null) }
    val total = demoCatalog.sumOf { product ->
        (if (store == 0) product.firstPrice else product.secondPrice) * (cart[product.id] ?: 0)
    }
    val count = cart.values.sum()
    val storeName = if (store == 0) "Mercado OMNI Demo" else "Tienda Nube Demo"

    fun scanDemoQr() {
        GmsBarcodeScanning.getClient(context).startScan()
            .addOnSuccessListener { barcode ->
                val parts = barcode.rawValue.orEmpty().split("|")
                scannedProductId = null
                scanResult = when {
                    parts.size == 2 && parts[0] == "OMNI-DEMO-PRODUCT" -> {
                        val product = demoCatalog.firstOrNull { it.id == parts[1] }
                        if (product == null) "Producto no encontrado en el catálogo DEMO actual."
                        else {
                            scannedProductId = product.id
                            val currentPrice = if (store == 0) product.firstPrice else product.secondPrice
                            "Producto reconocido: ${product.name} · ${product.category} · ${product.unit}. Precio vigente en ${storeName}: ${demoPrice(currentPrice)}. El importe se consulta del catálogo seleccionado."
                        }
                    }
                    parts.size == 2 && parts[0] == "OMNI-DEMO-ORDER" -> {
                        val order = orders.firstOrNull { it.id == parts[1] }
                        if (order == null) "El pedido no existe en este dispositivo. Los pedidos DEMO se guardan localmente y no se sincronizan entre dispositivos."
                        else "Pedido DEMO verificado: ${order.id.take(8).uppercase()} · ${demoPrice(order.total)} · ${order.date} · ${order.delivery}. Es una referencia local, no acredita pagos."
                    }
                    else -> "QR no reconocido. Este lector admite productos y pedidos del catálogo DEMO."
                }
            }
            .addOnFailureListener { scanResult = "No se pudo completar el escaneo QR. Intentá nuevamente." }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DemoBg),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White) }
                Column(Modifier.weight(1f)) {
                    Text("EXPERIENCIA DE PRUEBA", color = DemoViolet, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Mercado OMNI DEMO", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
                Icon(Icons.Default.ShoppingCart, null, tint = DemoCyan, modifier = Modifier.size(25.dp))
            }
        }
        item {
            Surface(color = Color(0xFF201B35), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("ENTORNO FICTICIO · SIN PAGOS REALES", color = DemoViolet, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                    Text("Tiendas, productos y precios ilustrativos. No representan ofertas ni stock de comercios reales.", color = Color.White, fontSize = 11.sp)
                }
            }
        }
        item {
            Text("Elegí una tienda", color = Color.White, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = store == 0, onClick = { store = 0 }, label = { Text("Mercado OMNI") })
                FilterChip(selected = store == 1, onClick = { store = 1 }, label = { Text("Tienda Nube") })
            }
            Text("Precios de demostración en pesos argentinos · " + storeName, color = DemoMuted, fontSize = 10.sp)
        }
        item {
            OutlinedButton(onClick = { scanDemoQr() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.QrCode2, null, modifier = Modifier.size(17.dp))
                Text(" Escanear QR de catálogo DEMO")
            }
            Text("Este lector es solo para el catálogo local de muestra. Para pedidos entre dos teléfonos usá Pedidos OMNI y el botón «Escanear pedido QR» del comercio.", color = DemoMuted, fontSize = 10.sp)
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Catálogo de prueba", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text(demoCatalog.size.toString() + " productos", color = DemoCyan, fontSize = 10.sp)
            }
        }
        items(demoCatalog, key = { it.id }) { product ->
            val price = if (store == 0) product.firstPrice else product.secondPrice
            val comparePrice = if (store == 0) product.secondPrice else product.firstPrice
            Card(colors = CardDefaults.cardColors(containerColor = DemoCard), shape = RoundedCornerShape(15.dp),
                border = BorderStroke(1.dp, DemoCyan.copy(alpha = 0.18f))) {
                Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(product.emoji, fontSize = 25.sp)
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(product.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(product.category + " · " + product.unit, color = DemoMuted, fontSize = 10.sp)
                        }
                        if (product.offer.isNotBlank()) Text(product.offer, color = DemoViolet, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(demoPrice(price), color = DemoCyan, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                            Text("Otra tienda: " + demoPrice(comparePrice), color = DemoMuted, fontSize = 9.sp)
                        }
                        OutlinedButton(onClick = {
                            qrTitle = "QR de " + product.name
                            qrPayload = "OMNI-DEMO-PRODUCT|" + product.id
                        }) {
                            Icon(Icons.Default.QrCode2, null, modifier = Modifier.size(16.dp))
                            Text(" QR", fontSize = 10.sp)
                        }
                        Spacer(Modifier.width(5.dp))
                        Button(onClick = { cart[product.id] = (cart[product.id] ?: 0) + 1 },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF087D98))) {
                            Text("Agregar", fontSize = 10.sp)
                        }
                    }
                    Text("El QR identifica el producto; el precio se consulta en el catálogo vigente, no queda fijado en el código.", color = DemoMuted, fontSize = 9.sp)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = DemoCard), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingCart, null, tint = DemoCyan)
                        Text("Tu carrito", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp).weight(1f))
                        Text(count.toString() + " unidades", color = DemoCyan, fontSize = 10.sp)
                    }
                    if (count == 0) Text("Todavía no agregaste productos.", color = DemoMuted, fontSize = 11.sp)
                    demoCatalog.filter { (cart[it.id] ?: 0) > 0 }.forEach { product ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(product.name, color = Color.White, fontSize = 11.sp)
                                Text(demoPrice((if (store == 0) product.firstPrice else product.secondPrice) * (cart[product.id] ?: 0)), color = DemoMuted, fontSize = 10.sp)
                            }
                            OutlinedButton(onClick = {
                                val qty = cart[product.id] ?: 0
                                if (qty <= 1) cart.remove(product.id) else cart[product.id] = qty - 1
                            }, contentPadding = PaddingValues(horizontal = 9.dp, vertical = 1.dp)) { Text("−") }
                            Text((cart[product.id] ?: 0).toString(), color = Color.White, modifier = Modifier.padding(horizontal = 8.dp))
                            OutlinedButton(onClick = { cart[product.id] = (cart[product.id] ?: 0) + 1 },
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 1.dp)) { Text("+") }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Total estimado", color = DemoMuted, modifier = Modifier.weight(1f), fontSize = 11.sp)
                        Text(demoPrice(total), color = DemoCyan, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                    }
                    Button(onClick = { checkout = true }, enabled = count > 0, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF087D98))) {
                        Text("Continuar con compra DEMO")
                    }
                    Text("El carrito se conserva localmente en este dispositivo. El checkout no debita OMNI Pay ni mueve dinero.", color = DemoMuted, fontSize = 9.sp)
                }
            }
        }
        item { Text("Pedidos DEMO guardados en este dispositivo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        if (orders.isEmpty()) item { Text("Todavía no hay pedidos DEMO.", color = DemoMuted, fontSize = 11.sp) }
        items(orders.asReversed(), key = { it.id }) { order ->
            Card(colors = CardDefaults.cardColors(containerColor = DemoCard), shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Pedido " + order.id.take(8).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(order.date + " · " + order.delivery, color = DemoMuted, fontSize = 9.sp)
                        Text(demoPrice(order.total), color = DemoCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    OutlinedButton(onClick = { qrTitle = "QR del pedido DEMO"; qrPayload = "OMNI-DEMO-ORDER|" + order.id }) {
                        Icon(Icons.Default.QrCode2, null)
                        Text(" Ver QR", fontSize = 9.sp)
                    }
                }
            }
        }
    }

    if (checkout) {
        AlertDialog(
            onDismissRequest = { checkout = false },
            title = { Text("Confirmar compra DEMO") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Tienda: " + storeName + "\nTotal ilustrativo: " + demoPrice(total), fontSize = 12.sp)
                    Text("¿Cómo querés recibirlo?", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = fulfillment == "RETIRO", onClick = { fulfillment = "RETIRO" })
                        Text("Retiro en tienda", fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = fulfillment == "DELIVERY", onClick = { fulfillment = "DELIVERY" })
                        Text("Envío a domicilio", fontSize = 11.sp)
                    }
                    if (fulfillment == "DELIVERY") {
                        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Dirección de entrega") }, singleLine = true)
                    }
                    Text("No se cobra dinero. El pedido y el QR quedan guardados localmente.", color = DemoMuted, fontSize = 10.sp)
                }
            },
            confirmButton = {
                TextButton(enabled = fulfillment == "RETIRO" || address.isNotBlank(), onClick = {
                    val id = UUID.randomUUID().toString()
                    val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "AR")).format(Date())
                    val delivery = if (fulfillment == "RETIRO") "RETIRO" else "DELIVERY · " + address.trim()
                    val order = DemoOrder(id, total, delivery, date)
                    orders.add(order)
                    writeDemoOrders(context, orders.toList())
                    cart.clear()
                    checkout = false
                    qrTitle = "QR del pedido DEMO"
                    qrPayload = "OMNI-DEMO-ORDER|" + id
                }) { Text("Generar pedido y QR") }
            },
            dismissButton = { TextButton(onClick = { checkout = false }) { Text("Cancelar") } }
        )
    }

    scanResult?.let { result ->
        AlertDialog(
            onDismissRequest = { scanResult = null; scannedProductId = null },
            title = { Text("Resultado del QR DEMO") },
            text = { Text(result, fontSize = 12.sp) },
            confirmButton = {
                Row {
                    scannedProductId?.let { productId ->
                        TextButton(onClick = {
                            cart[productId] = (cart[productId] ?: 0) + 1
                            scanResult = null
                            scannedProductId = null
                        }) { Text("Agregar al carrito") }
                    }
                    TextButton(onClick = { scanResult = null; scannedProductId = null }) { Text("Cerrar") }
                }
            }
        )
    }

    qrPayload?.let { payload ->
        val bitmap = remember(payload) { demoQr(payload) }
        AlertDialog(
            onDismissRequest = { qrPayload = null },
            title = { Text(qrTitle) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    bitmap?.let { Image(it.asImageBitmap(), contentDescription = qrTitle, modifier = Modifier.size(220.dp)) }
                    Text("Código identificador DEMO. No acredita pagos ni genera pedidos reales.", color = DemoMuted, fontSize = 10.sp)
                    Text("Referencia: " + payload.substringAfter("|"), color = DemoMuted, fontSize = 9.sp)
                }
            },
            confirmButton = { TextButton(onClick = { qrPayload = null }) { Text("Cerrar") } }
        )
    }
}
