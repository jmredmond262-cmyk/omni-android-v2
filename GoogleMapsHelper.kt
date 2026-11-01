package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

object GoogleMapsHelper {
    /**
     * Abre coordenadas específicas directamente en Google Maps con etiqueta
     */
    fun openLocationInGoogleMaps(context: Context, latitude: Double, longitude: Double, label: String) {
        val encodedLabel = URLEncoder.encode(label, "UTF-8")
        val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedLabel)")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            context.startActivity(mapIntent)
        } catch (_: Exception) {
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"))
            context.startActivity(fallback)
        }
    }

    /**
     * Abre una búsqueda por nombre o calle en Google Maps
     */
    fun openSearchInGoogleMaps(context: Context, query: String) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedQuery")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            context.startActivity(mapIntent)
        } catch (_: Exception) {
            val fallback = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(fallback)
        }
    }

    /**
     * Inicia navegación paso a paso hacia un destino en Google Maps
     */
    fun openNavigationInGoogleMaps(context: Context, destination: String) {
        val encoded = URLEncoder.encode(destination, "UTF-8")
        val uri = Uri.parse("google.navigation:q=$encoded")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            context.startActivity(mapIntent)
        } catch (_: Exception) {
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$encoded"))
            context.startActivity(fallback)
        }
    }

    /**
     * Abre ruta completa con origen y destino en Google Maps
     */
    fun openRouteInGoogleMaps(context: Context, origin: String, destination: String, mode: String = "driving") {
        val encOrigin = URLEncoder.encode(origin, "UTF-8")
        val encDest = URLEncoder.encode(destination, "UTF-8")
        val mapUrl = "https://www.google.com/maps/dir/?api=1&origin=$encOrigin&destination=$encDest&travelmode=$mode"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl)).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl))
            context.startActivity(fallback)
        }
    }

    /**
     * Busca comercios cercanos a unas coordenadas GPS en Google Maps
     */
    fun searchNearbyInGoogleMaps(context: Context, query: String, latitude: Double, longitude: Double) {
        val encQuery = URLEncoder.encode(query, "UTF-8")
        val mapUrl = "https://www.google.com/maps/search/$encQuery/@$latitude,$longitude,15z"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl)).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl))
            context.startActivity(fallback)
        }
    }

    /**
     * Llama o envía mensaje a la remisería con el recorrido
     */
    fun callOrMessageRemis(context: Context, phoneNumber: String, origin: String, destination: String) {
        val cleanPhone = phoneNumber.replace("[^0-9+]".toRegex(), "")
        val msg = "Hola! Necesito un remis desde: $origin hasta: $destination. ¿Tienen móvil disponible?"
        val encMsg = URLEncoder.encode(msg, "UTF-8")
        val whatsappIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleanPhone?text=$encMsg"))
        try {
            context.startActivity(whatsappIntent)
        } catch (_: Exception) {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
            try {
                context.startActivity(dialIntent)
            } catch (_: Exception) {}
        }
    }
}
