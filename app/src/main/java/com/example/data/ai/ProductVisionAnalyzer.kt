package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeBackend
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ProductVisionDraft(
    val name: String,
    val category: String,
    val description: String,
    val barcode: String
)

object ProductVisionAnalyzer {
    suspend fun analyze(context: Context, imageUri: Uri): ProductVisionDraft {
        val bitmap = withContext(Dispatchers.IO) {
            val decoded = context.contentResolver.openInputStream(imageUri).use { stream ->
                requireNotNull(stream) { "No se pudo abrir la imagen del producto." }
                BitmapFactory.decodeStream(stream)
            } ?: throw IllegalArgumentException("La imagen no tiene un formato compatible.")
            val maxDimension = maxOf(decoded.width, decoded.height)
            if (maxDimension > 1280) {
                val scale = 1280f / maxDimension
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).toInt().coerceAtLeast(1),
                    (decoded.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else decoded
        }

        val model = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-3.8-flash")
        val prompt = content {
            image(bitmap)
            text(
                """
                Actuá como especialista en catalogación de productos para un comercio argentino.
                Analizá exclusivamente lo que se pueda leer o reconocer en la imagen.
                Devolvé SOLO un objeto JSON válido, sin Markdown, con estas claves:
                "name": nombre comercial claro, incluyendo marca, variedad y presentación cuando sean visibles;
                "category": una de estas categorías exactas: ALMACEN, LACTEOS, FRESCOS, BEBIDAS, LIMPIEZA, KIOSCO, FARMACIA;
                "description": descripción profesional, concreta y atractiva para una tienda online, sin inventar ingredientes, origen, certificaciones, beneficios médicos ni atributos que no se vean;
                "barcode": código EAN/UPC únicamente si es completamente legible, de lo contrario cadena vacía.
                No propongas ni inventes precios. Si no podés identificar con seguridad el producto, indicá "Producto sin identificar" y explicá brevemente qué dato falta en la descripción.
                Escribí todo el contenido de salida en español rioplatense neutro.
                """.trimIndent()
            )
        }
        val response = model.generateContent(prompt)
        val raw = response.text?.trim().orEmpty()
        if (raw.isBlank()) throw IllegalStateException("La IA no devolvió una descripción. Probá con una foto más nítida.")
        val jsonStart = raw.indexOf('{')
        val jsonEnd = raw.lastIndexOf('}')
        if (jsonStart < 0 || jsonEnd <= jsonStart) {
            throw IllegalStateException("La IA no pudo estructurar los datos del producto. Intentá otra foto.")
        }
        val json = JSONObject(raw.substring(jsonStart, jsonEnd + 1))
        ProductVisionDraft(
            name = json.optString("name").trim(),
            category = json.optString("category", "ALMACEN").trim().uppercase(),
            description = json.optString("description").trim(),
            barcode = json.optString("barcode").trim()
        ).also {
            if (it.name.isBlank()) throw IllegalStateException("No se pudo identificar el nombre del producto.")
        }
    }
}
