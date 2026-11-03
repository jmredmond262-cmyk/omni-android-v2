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

