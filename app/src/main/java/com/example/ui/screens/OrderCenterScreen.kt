package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.content.ClipData
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
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
import com.example.model.Store
import com.example.model.StoreProduct
import com.example.ui.MainViewModel
import com.example.data.analytics.UsageAnalyticsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.util.UUID

private data class OrderLine(val productId: String, val name: String, val price: Double, val quantity: Int)
private data class OmniStoreOrder(
    val id: String,
    val storeId: String,
    val storeName: String,
    val merchantUid: String,
    val customerUid: String,
    val customerName: String,
    val customerPhone: String,
    val items: List<OrderLine>,
    val total: Double,
    val status: String,
    val paymentStatus: String,
    val fulfillmentType: String,
    val deliveryAddress: String,
    val qrNonce: String
)

@Composable
fun OrderCenterScreen(viewModel: MainViewModel, merchantMode: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val profile by viewModel.userProfile.collectAsState()
    val selectedStore by viewModel.selectedStore.collectAsState()
    val managedStore by viewModel.managedStore.collectAsState()
    val store = if (merchantMode) managedStore else (selectedStore ?: managedStore)
    val db = remember { FirebaseFirestore.getInstance() }

    var merchantReady by remember { mutableStateOf(false) }
    var merchantError by remember { mutableStateOf<String?>(null) }
    var orders by remember { mutableStateOf<List<OmniStoreOrder>>(emptyList()) }
    var cart by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var fulfillmentType by remember { mutableStateOf("RETIRO") }
    var deliveryAddress by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var createdOrder by remember { mutableStateOf<OmniStoreOrder?>(null) }
    var showOrderReview by remember { mutableStateOf(false) }
    var scannedOrderId by remember { mutableStateOf<String?>(null) }

    fun decodeOrder(doc: com.google.firebase.firestore.DocumentSnapshot): OmniStoreOrder? = runCatching {
        val lines = (doc.get("items") as? List<*>)?.mapNotNull { raw ->
            val item = raw as? Map<*, *> ?: return@mapNotNull null
            OrderLine(
                productId = item["productId"] as? String ?: "",
                name = item["name"] as? String ?: "",
                price = (item["price"] as? Number)?.toDouble() ?: 0.0,
                quantity = (item["quantity"] as? Number)?.toInt() ?: 0
            )
        }.orEmpty()
        OmniStoreOrder(
            id = doc.id,
            storeId = doc.getString("storeId").orEmpty(),
            storeName = doc.getString("storeName").orEmpty(),
            merchantUid = doc.getString("merchantUid").orEmpty(),
            customerUid = doc.getString("customerUid").orEmpty(),
            customerName = doc.getString("customerName").orEmpty(),
            customerPhone = doc.getString("customerPhone").orEmpty(),
            items = lines,
            total = (doc.get("total") as? Number)?.toDouble() ?: 0.0,
            status = doc.getString("status").orEmpty(),
            paymentStatus = doc.getString("paymentStatus").orEmpty(),
            fulfillmentType = doc.getString("fulfillmentType").orEmpty(),
            deliveryAddress = doc.getString("deliveryAddress").orEmpty(),
            qrNonce = doc.getString("qrNonce").orEmpty()
        )
    }.getOrNull()

    LaunchedEffect(merchantMode, store.id, uid) {
        if (merchantMode && uid.isNotBlank()) {
            merchantReady = false
            merchantError = null
            val profileRef = db.collection("merchantProfiles").document(store.id)
            profileRef.get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    if (doc.getString("ownerUid") == uid) {
                        merchantReady = true
                    } else {
                        merchantError = "Este comercio ya está asociado a otra cuenta. No podés administrar sus pedidos."
                    }
                } else {
                    profileRef.set(mapOf(
                        "storeId" to store.id,
                        "storeName" to store.name,
                        "address" to store.address,
                        "contactPhone" to store.phone,
                        "ownerUid" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )).addOnSuccessListener { merchantReady = true }
                        .addOnFailureListener { merchantError = it.localizedMessage ?: "No se pudo habilitar el panel de pedidos." }
                }
            }.addOnFailureListener { merchantError = it.localizedMessage ?: "No se pudo verificar el comercio." }
        } else if (!merchantMode) {
            merchantReady = true
        }
    }

    DisposableEffect(merchantMode, merchantReady, store.id, uid) {
        var listener: ListenerRegistration? = null
        if (uid.isNotBlank() && (!merchantMode || merchantReady)) {
            val query = if (merchantMode) {
                db.collection("storeOrders").whereEqualTo("merchantUid", uid)
            } else {
                db.collection("storeOrders").whereEqualTo("customerUid", uid)
            }
            listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    feedback = error.localizedMessage ?: "No se pudieron sincronizar los pedidos."
                } else {
                    orders = snapshot?.documents.orEmpty().mapNotNull(::decodeOrder)
                        .filter { !merchantMode || it.storeId == store.id }
                        .sortedByDescending { it.id }
                }
            }
        }
        onDispose { listener?.remove() }
    }

    val cartLines = store.inventory.filter { (cart[it.id] ?: 0) > 0 && it.inStock }.map {
        OrderLine(it.id, it.name, it.price, cart[it.id] ?: 0)
    }
    val total = cartLines.sumOf { it.price * it.quantity }

    fun submitOrder() {
        if (uid.isBlank() || cartLines.isEmpty() || cartLines.size > 30 || total <= 0.0 || total > 500000.0 || (fulfillmentType == "DELIVERY" && deliveryAddress.isBlank())) {
            feedback = "Revisá el pedido: máximo 30 productos distintos, total hasta OM$ 500.000 y dirección obligatoria para delivery."
            return
        }
        sending = true
        db.collection("merchantProfiles").document(store.id).get()
            .addOnSuccessListener { merchant ->
                val merchantUid = merchant.getString("ownerUid").orEmpty()
                if (!merchant.exists() || merchantUid.isBlank() || merchantUid == uid) {
                    sending = false
                    feedback = "Este comercio todavía no habilitó pedidos QR para clientes."
                } else {
                    val ref = db.collection("storeOrders").document()
                    val nonce = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().take(8)
                    val payloadItems = cartLines.map { mapOf(
                        "productId" to it.productId, "name" to it.name,
                        "price" to it.price, "quantity" to it.quantity
                    ) }
                    ref.set(mapOf(
                        "storeId" to store.id, "storeName" to store.name,
                        "merchantUid" to merchantUid, "customerUid" to uid,
                        "customerName" to profile.fullName, "customerPhone" to profile.phone,
                        "items" to payloadItems, "total" to total,
                        "status" to "RECIBIDO", "paymentStatus" to "PENDIENTE",
                        "paymentMethod" to "PAGO_AL_RETIRAR",
                        "fulfillmentType" to fulfillmentType, "deliveryAddress" to deliveryAddress.trim(),
                        "qrNonce" to nonce,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )).addOnSuccessListener {
                        UsageAnalyticsRepository.trackAction(uid, "STORE_ORDER_CREATED", "orders")
                        sending = false
                        createdOrder = OmniStoreOrder(
                            ref.id, store.id, store.name, merchantUid, uid,
                            profile.fullName, profile.phone, cartLines, total,
                            "RECIBIDO", "PENDIENTE", fulfillmentType, deliveryAddress.trim(), nonce
                        )
                        cart = emptyMap()
                    }.addOnFailureListener { e ->
                        sending = false
                        feedback = e.localizedMessage ?: "No se pudo registrar el pedido. Revisá las reglas de Firestore."
                    }
                }
            }.addOnFailureListener { e ->
                sending = false
                feedback = e.localizedMessage ?: "No se pudo verificar el comercio."
            }
    }

    fun scanOrder() {
        UsageAnalyticsRepository.trackCurrentAction("orders", "ORDER_QR_SCAN_STARTED")
        GmsBarcodeScanning.getClient(context).startScan()
            .addOnSuccessListener { barcode ->
                val parts = barcode.rawValue.orEmpty().split("|")
                if (parts.size != 3 || parts[0] != "OMNI-ORDER") {
                    feedback = "El código no corresponde a un pedido OMNI."
                } else {
                    db.collection("storeOrders").document(parts[1]).get()
                        .addOnSuccessListener { doc ->
                            val order = decodeOrder(doc)
                            val createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                            val expired = createdAt == 0L || System.currentTimeMillis() - createdAt > 24L * 60L * 60L * 1000L
                            if (order == null || order.storeId != store.id || order.merchantUid != uid || order.qrNonce != parts[2] || expired || order.status in listOf("ENTREGADO", "RECHAZADO", "CANCELADO")) {
                                feedback = "QR inválido, vencido, ya cerrado o no corresponde a este comercio."
                            } else {
                                scannedOrderId = order.id
                                UsageAnalyticsRepository.trackCurrentAction("orders", "ORDER_QR_VERIFIED", mapOf("success" to true))
                                feedback = "Pedido verificado: ${order.id.take(8)} · ${order.status}"
                            }
                        }.addOnFailureListener { feedback = "No se pudo verificar el pedido en Firestore." }
                }
            }.addOnFailureListener { feedback = "No se pudo abrir el lector QR." }
    }

    fun advanceOrder(order: OmniStoreOrder, next: String) {
        db.collection("storeOrders").document(order.id).update(
            mapOf("status" to next, "updatedAt" to FieldValue.serverTimestamp())
        ).addOnSuccessListener {
            UsageAnalyticsRepository.trackAction(uid, "ORDER_STATUS_$next", "orders")
        }.addOnFailureListener { feedback = it.localizedMessage ?: "No se pudo actualizar el estado." }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF070A14)).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Text(if (merchantMode) "PANEL DE PEDIDOS" else "PEDIDOS OMNI", color = Color(0xFF57DDF5), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (merchantMode) store.name else "Pedido QR · ${store.name}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            if (merchantMode) {
                IconButton(onClick = { scanOrder() }) { Icon(Icons.Default.QrCodeScanner, "Verificar QR de pedido", tint = Color(0xFF57DDF5)) }
            }
        }

        if (merchantMode) {
            Text("Pedidos recibidos y seguimiento de preparación / entrega.", color = Color(0xFF9AAAC2), fontSize = 11.sp)
            if (merchantError != null) Text(merchantError.orEmpty(), color = Color(0xFFFF9B8F), fontSize = 12.sp, modifier = Modifier.padding(8.dp))
            else if (!merchantReady) Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF57DDF5)) }
        } else {
            Text("Elegí productos y enviá tu pedido al comercio. El QR identifica el pedido; no acredita un pago.", color = Color(0xFF9AAAC2), fontSize = 11.sp)
            if (store.inventory.isEmpty()) Text("Este comercio todavía no publicó productos para pedidos.", color = Color(0xFFFFC978), fontSize = 12.sp)
            else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(top = 10.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(store.inventory.filter { it.inStock }, key = { it.id }) { product ->
                    OrderProductRow(product, cart[product.id] ?: 0,
                        onAdd = {
                            if (!cart.containsKey(product.id) && cart.size >= 30) {
                                feedback = "El pedido admite hasta 30 productos distintos."
                            } else {
                                cart = cart + (product.id to ((cart[product.id] ?: 0) + 1))
                            }
                        },
                        onRemove = {
                            val next = (cart[product.id] ?: 0) - 1
                            cart = if (next <= 0) cart - product.id else cart + (product.id to next)
                        }
                    )
                }
            }
            if (cartLines.isNotEmpty()) {
                Surface(color = Color(0xFF101B30), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("${cartLines.sumOf { it.quantity }} productos · Total estimado OM$ ${String.format("%,.2f", total)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = fulfillmentType == "RETIRO", onClick = { fulfillmentType = "RETIRO" }, label = { Text("Retirar") })
                            FilterChip(selected = fulfillmentType == "DELIVERY", onClick = { fulfillmentType = "DELIVERY" }, label = { Text("Delivery") })
                        }
                        if (fulfillmentType == "DELIVERY") {
                            OutlinedTextField(deliveryAddress, { deliveryAddress = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Dirección de entrega") }, singleLine = true)
                        }
                        Button(onClick = { showOrderReview = true }, enabled = !sending, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF087EAB))) {
                            Text("Revisar y confirmar pedido")
                        }
                    }
                }
            }
        }

        if (merchantMode && merchantReady && merchantError == null) {
            if (orders.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Text("Todavía no recibiste pedidos.", color = Color(0xFF9AAAC2), fontSize = 12.sp) }
            } else {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    items(orders, key = { it.id }) { order ->
                        OrderStatusCard(order, merchantMode = true, onAdvance = { next -> advanceOrder(order, next) })
                    }
                }
            }
        } else if (!merchantMode) {
            val ownOrders = orders.filter { it.customerUid == uid }
            if (ownOrders.isNotEmpty()) {
                Text("MIS PEDIDOS", color = Color(0xFF57DDF5), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(top = 6.dp, bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ownOrders, key = { it.id }) { order ->
                        OrderStatusCard(order, merchantMode = false, onAdvance = {})
                    }
                }
            }
        }
    }

    if (showOrderReview) {
        AlertDialog(
            onDismissRequest = { if (!sending) showOrderReview = false },
            containerColor = Color(0xFF101B30),
            title = { Text("Confirmar pedido", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(store.name, color = Color(0xFF57DDF5), fontWeight = FontWeight.Bold)
                    cartLines.forEach { line ->
                        Text("${line.quantity} × ${line.name} · OM$ ${String.format("%,.2f", line.price * line.quantity)}", color = Color.White, fontSize = 11.sp)
                    }
                    Text("Total: OM$ ${String.format("%,.2f", total)}", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(if (fulfillmentType == "DELIVERY") "Entrega: ${deliveryAddress.trim()}" else "Modalidad: Retiro en el comercio", color = Color(0xFFB8C8D9), fontSize = 11.sp)
                    Text("Pago pendiente. Se abona al retirar o recibir; el QR solo identifica el pedido.", color = Color(0xFFFFC978), fontSize = 10.sp)
                }
            },
            confirmButton = { Button(onClick = { showOrderReview = false; submitOrder() }, enabled = !sending) { Text("Confirmar y generar QR") } },
            dismissButton = { TextButton(onClick = { showOrderReview = false }, enabled = !sending) { Text("Volver al carrito") } }
        )
    }
    createdOrder?.let { order ->
        val payload = "OMNI-ORDER|${order.id}|${order.qrNonce}"
        val bitmap = remember(payload) {
            runCatching {
                val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 480, 480)
                Bitmap.createBitmap(480, 480, Bitmap.Config.RGB_565).also { image ->
                    for (x in 0 until 480) for (y in 0 until 480) {
                        image.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                    }
                }
            }.getOrNull()
        }
        AlertDialog(
            onDismissRequest = { createdOrder = null },
            containerColor = Color(0xFF101B30),
            title = { Text("Pedido enviado", color = Color.White) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Mostrá este QR en el comercio para identificar tu pedido.", color = Color(0xFFB8C8D9), fontSize = 11.sp)
                    bitmap?.let { qrBitmap ->
                        Image(qrBitmap.asImageBitmap(), "QR del pedido", modifier = Modifier.size(190.dp))
                        OutlinedButton(onClick = {
                            runCatching {
                                val folder = File(context.cacheDir, "shared_qr").apply { mkdirs() }
                                val qrFile = File(folder, "omni-pedido-${order.id.take(10)}.png")
                                FileOutputStream(qrFile).use { output -> check(qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) }
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", qrFile)
                                UsageAnalyticsRepository.trackCurrentAction("orders", "ORDER_SHARED", mapOf("method" to "share_sheet"))
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_TEXT, "Pedido OMNI ${order.id}\nComercio: ${order.storeName}\nTotal: OM$ ${String.format("%,.2f", order.total)}\nPago pendiente al retirar o recibir.")
                                    clipData = ClipData.newUri(context.contentResolver, "QR Pedido OMNI", uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(share, "Compartir QR del pedido"))
                            }.onFailure { feedback = it.localizedMessage ?: "No se pudo compartir el QR." }
                        }) { Text("Compartir QR") }
                    } ?: Text("No se pudo generar la imagen QR.", color = Color(0xFFFF9B8F), fontSize = 10.sp)
                    Text("Pedido ${order.id.take(10)} · ${order.status}", color = Color(0xFF57DDF5), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("Pago pendiente · se abona al retirar o recibir. Este QR no es un comprobante de pago.", color = Color(0xFFFFC978), fontSize = 10.sp)
                }
            },
            confirmButton = { TextButton(onClick = { createdOrder = null }) { Text("Cerrar") } }
        )
    }

    feedback?.let { message ->
        AlertDialog(onDismissRequest = { feedback = null }, title = { Text("Pedidos OMNI") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { feedback = null }) { Text("Aceptar") } })
    }
}

@Composable
private fun OrderProductRow(product: StoreProduct, quantity: Int, onAdd: () -> Unit, onRemove: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF101B30)), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(product.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("OM$ ${String.format("%,.2f", product.price)} · ${product.unitLabel}", color = Color(0xFF57DDF5), fontSize = 10.sp)
            }
            IconButton(onClick = onRemove, enabled = quantity > 0) { Icon(Icons.Default.Remove, "Quitar", tint = Color(0xFFB8C8D9)) }
            Text(quantity.toString(), color = Color.White, fontWeight = FontWeight.Bold)
            IconButton(onClick = onAdd) { Icon(Icons.Default.Add, "Agregar", tint = Color(0xFF57DDF5)) }
        }
    }
}

@Composable
private fun OrderStatusCard(order: OmniStoreOrder, merchantMode: Boolean, onAdvance: (String) -> Unit) {
    val context = LocalContext.current
    val next = when (order.status) {
        "RECIBIDO" -> "ACEPTADO"
        "ACEPTADO" -> "EN_PREPARACION"
        "EN_PREPARACION" -> "LISTO"
        "LISTO" -> "EN_CAMINO"
        "EN_CAMINO" -> "ENTREGADO"
        else -> ""
    }
    val label = when (order.status) {
        "RECIBIDO" -> "Recibido"
        "ACEPTADO" -> "Aceptado"
        "EN_PREPARACION" -> "En preparación"
        "LISTO" -> "Listo para retirar"
        "EN_CAMINO" -> "En camino"
        "ENTREGADO" -> "Entregado"
        "RECHAZADO" -> "Rechazado"
        "CANCELADO" -> "Cancelado"
        else -> order.status
    }
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF101B30)), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (merchantMode) order.customerName else order.storeName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Pedido ${order.id.take(9)} · ${label}", color = Color(0xFF57DDF5), fontSize = 10.sp)
                }
                Text("OM$ ${String.format("%,.2f", order.total)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            order.items.forEach { Text("${it.quantity} × ${it.name}", color = Color(0xFFB8C8D9), fontSize = 10.sp) }
            Text(if (order.fulfillmentType == "DELIVERY") "Delivery · ${order.deliveryAddress}" else "Retiro en el comercio", color = Color(0xFFB8C8D9), fontSize = 9.sp)
            Text("Pago: pendiente · efectivo al retirar/recibir", color = Color(0xFFFFC978), fontSize = 9.sp)
            if (merchantMode && order.customerPhone.isNotBlank()) {
                TextButton(onClick = {
                    val phoneNumber = order.customerPhone.filter { it.isDigit() }
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$phoneNumber"))) }
                }) { Text("Contactar cliente por WhatsApp", color = Color(0xFF57DDF5), fontSize = 10.sp) }
            }
            if (merchantMode && next.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Button(onClick = { onAdvance(next) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF087EAB))) {
                        Text(when (next) {
                            "ACEPTADO" -> "Aceptar pedido"
                            "EN_PREPARACION" -> "Comenzar preparación"
                            "LISTO" -> "Marcar listo"
                            "EN_CAMINO" -> "Enviar delivery"
                            else -> "Marcar entregado"
                        }, fontSize = 10.sp)
                    }
                    if (order.status == "RECIBIDO") {
                        OutlinedButton(onClick = { onAdvance("RECHAZADO") }) {
                            Text("Rechazar", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
