package com.example.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import org.json.JSONObject

data class OrderQRItem(
    val productId: String,
    val name: String,
    val price: Double,
    val quantity: Int
)

data class OrderQRPayload(
    val orderId: String,
    val merchantName: String,
    val totalAmount: Double,
    val items: List<OrderQRItem>,
    val qrNonce: String
)

object OrderQRCodec {
    fun encode(
        orderId: String,
        merchantName: String,
        totalAmount: Double,
        items: List<OrderQRItem>,
        qrNonce: String
    ): String {
        val jsonItems = org.json.JSONArray()
        items.forEach { item ->
            jsonItems.put(JSONObject()
                .put("productId", item.productId)
                .put("name", item.name)
                .put("price", item.price)
                .put("quantity", item.quantity))
        }
        return JSONObject()
            .put("orderId", orderId)
            .put("merchantName", merchantName)
            .put("totalAmount", totalAmount)
            .put("items", jsonItems)
            .put("qrNonce", qrNonce)
            .toString()
    }

    fun decode(raw: String): OrderQRPayload? = runCatching {
        val json = JSONObject(raw)
        val orderId = json.optString("orderId").trim()
        val merchantName = json.optString("merchantName").trim()
        val totalAmount = json.optDouble("totalAmount", Double.NaN)
        val nonce = json.optString("qrNonce").trim()
        val jsonItems = json.optJSONArray("items") ?: throw IllegalArgumentException("Faltan los artículos del pedido.")
        val items = (0 until jsonItems.length()).map { index ->
            val item = jsonItems.getJSONObject(index)
            OrderQRItem(
                productId = item.optString("productId"),
                name = item.optString("name"),
                price = item.optDouble("price", Double.NaN),
                quantity = item.optInt("quantity", 0)
            )
        }
        require(orderId.isNotBlank() && merchantName.isNotBlank() && nonce.isNotBlank())
        require(totalAmount.isFinite() && totalAmount > 0.0)
        require(items.isNotEmpty() && items.all { it.name.isNotBlank() && it.price.isFinite() && it.price >= 0.0 && it.quantity > 0 })
        OrderQRPayload(orderId, merchantName, totalAmount, items, nonce)
    }.getOrNull()
}

@Composable
fun OrderQRScanner(
    onOrderScanned: (OrderQRPayload) -> Unit,
    onLegacyWalletQr: (String) -> Unit = {},
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    buttonLabel: String = "Escanear QR de pedido",
    buttonColor: Color = Color(0xFF087EAB)
) {
    val context = LocalContext.current
    Button(
        onClick = {
            GmsBarcodeScanning.getClient(context).startScan()
                .addOnSuccessListener { barcode ->
                    val raw = barcode.rawValue.orEmpty().trim()
                    val payload = OrderQRCodec.decode(raw)
                    when {
                        payload != null -> onOrderScanned(payload)
                        raw.startsWith("OMNI-DEMO|") -> onLegacyWalletQr(raw)
                        else -> onError("El QR no contiene un pedido OMNI válido.")
                    }
                }
                .addOnFailureListener { error ->
                    onError(error.localizedMessage ?: "No se pudo abrir el lector QR.")
                }
        },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
    ) {
        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(buttonLabel)
    }
}
