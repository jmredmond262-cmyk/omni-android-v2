package com.example.model

enum class ProductCategory(val displayName: String, val iconName: String) {
    ALMACEN("Almacén", "Store"),
    LACTEOS("Lácteos", "LocalDrink"),
    FRESCOS("Frescos", "Eco"),
    BEBIDAS("Bebidas", "LocalBar"),
    LIMPIEZA("Limpieza", "CleaningServices"),
    KIOSCO("Snacks y Kiosco", "Fastfood"),
    FARMACIA("Farmacia & Salud", "MedicalServices")
}

data class NeedItem(
    val id: String,
    val name: String,
    val category: ProductCategory,
    val quantity: Int = 1,
    val unit: String = "unid.",
    val isChecked: Boolean = false
)

data class CatalogProduct(
    val id: String,
    val name: String,
    val category: ProductCategory,
    val basePrice: Double,
    val unit: String = "unid.",
    val iconEmoji: String = "🛒"
)
