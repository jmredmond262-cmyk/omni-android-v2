package com.example.model

enum class StoreType(val label: String) {
    SUPERMERCADO("Supermercado"),
    ALMACEN("Almacén de Barrio"),
    KIOSCO("Kiosco 24hs"),
    PANADERIA("Panadería & Café"),
    TIENDA_NATURAL("Dietética & Natural"),
    FARMACIA("Farmacia de Turno & Salud"),
    GASTRONOMIA("Gastronomía & Rotisería"),
    SERVICIOS("Servicios & Reparaciones")
}

data class ComboDeal(
    val title: String,
    val description: String,
    val discountBadge: String,
    val audience: String = "TODOS"
)

data class StoreProduct(
    val id: String,
    val name: String,
    val category: ProductCategory,
    val price: Double,
    val originalPrice: Double? = null,
    val inStock: Boolean = true,
    val unitLabel: String = "unid.",
    val photoUri: String? = null,
    val barcode: String? = null,
    val description: String = ""
)

data class Store(
    val id: String,
    val name: String,
    val type: StoreType,
    val rating: Float,
    val reviewsCount: Int,
    val distanceMeters: Int,
    val address: String,
    val isOpen: Boolean,
    val hoursText: String,
    val accentColorHex: Long,
    val photosCount: Int,
    val hasVideoTour: Boolean,
    val videoTourDuration: String,
    val description: String,
    val specialOfferText: String,
    val minSpendDiscountThreshold: Double,
    val minSpendDiscountPercent: Int,
    val comboDeals: List<ComboDeal>,
    val inventory: List<StoreProduct>,
    val mapOffsetX: Float,
    val mapOffsetY: Float,
    // Horarios de apertura y cierre configurables
    val openTime: String = "08:30",
    val closeTime: String = "21:00",
    val weekendHoursText: String = "Sáb: 09:00 - 14:00 • Dom: Cerrado",
    val is24Hours: Boolean = false,
    val isOnDuty: Boolean = false, // Farmacia de turno o guardia nocturna/urgencia
    val openOnHolidays: Boolean = false,
    val scheduleNotes: String = "",
    val phone: String = "",
    val photoUrl: String? = null,
    val googleMapsUri: String = ""
) {
    val distanceText: String
        get() = if (distanceMeters < 1000) "${distanceMeters} m" else String.format("%.1f km", distanceMeters / 1000.0)

    val walkingTimeMin: Int
        get() = maxOf(1, (distanceMeters / 75.0).toInt())

    val isEffectivelyOpen: Boolean
        get() = isOpen || is24Hours || isOnDuty
}



/** True only when the current local time falls inside hours explicitly published by the store. */
fun Store.isOpenAtCurrentTime(now: java.util.Calendar = java.util.Calendar.getInstance()): Boolean {
    // Estar marcada como farmacia de turno no significa estar abierta las 24 horas.
    // Solo un horario explícito o una declaración 24 h permite afirmar que está abierta.
    if (is24Hours) return true
    val day = now.get(java.util.Calendar.DAY_OF_WEEK)
    val minuteNow = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)

    fun parseMinute(value: String): Int? {
        val parts = value.trim().split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }

    fun containsCurrentTime(text: String): Boolean {
        val ranges = Regex("(\\d{1,2}:\\d{2})\\s*(?:-|–|a)\\s*(\\d{1,2}:\\d{2})").findAll(text)
        for (range in ranges) {
            val start = parseMinute(range.groupValues[1]) ?: continue
            val end = parseMinute(range.groupValues[2]) ?: continue
            if (if (end >= start) minuteNow in start until end else minuteNow >= start || minuteNow < end) return true
        }
        return false
    }

    if (day == java.util.Calendar.SATURDAY || day == java.util.Calendar.SUNDAY) {
        val pattern = if (day == java.util.Calendar.SATURDAY) "(?i)s[aá]b(?:ado)?s?\\s*:" else "(?i)dom(?:ingo)?s?\\s*:"
        val dayText = Regex(pattern + "([^•]+)").find(weekendHoursText)?.groupValues?.getOrNull(1).orEmpty()
        if (dayText.isNotBlank()) {
            if (dayText.contains("cerrado", ignoreCase = true)) return false
            if (containsCurrentTime(dayText)) return true
            if (Regex("\\d{1,2}:\\d{2}").containsMatchIn(dayText)) return false
        }
    }

    if (containsCurrentTime(hoursText)) return true
    if (Regex("\\d{1,2}:\\d{2}").containsMatchIn(hoursText)) return false
    val start = parseMinute(openTime)
    val end = parseMinute(closeTime)
    if (start != null && end != null) {
        return if (end >= start) minuteNow in start until end else minuteNow >= start || minuteNow < end
    }
    return isOpen
}
