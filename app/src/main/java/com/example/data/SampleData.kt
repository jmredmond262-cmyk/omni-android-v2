package com.example.data

import com.example.model.ComboDeal
import com.example.model.DailyTask
import com.example.model.MatchedItem
import com.example.model.NeedItem
import com.example.model.NeighborhoodContact
import com.example.model.ProductCategory
import com.example.model.SmartSuggestion
import com.example.model.Store
import com.example.model.StoreComparisonResult
import com.example.model.StoreProduct
import com.example.model.StoreType
import com.example.model.TaskCategory

object SampleData {

    val initialTasks = listOf(
        DailyTask(
            id = "t1",
            title = "Planificar compras semanales y canasta familiar",
            category = TaskCategory.COMPRAS,
            isCompleted = false,
            timeTag = "08:30"
        ),
        DailyTask(
            id = "t2",
            title = "Chequear promociones y combos activos en el barrio",
            category = TaskCategory.COMPRAS,
            isCompleted = true,
            timeTag = "09:00"
        ),
        DailyTask(
            id = "t3",
            title = "Caminata activa y retiro de productos en camino",
            category = TaskCategory.RUTINA,
            isCompleted = false,
            timeTag = "11:15"
        ),
        DailyTask(
            id = "t4",
            title = "Revisar despensa y reposición de frescos",
            category = TaskCategory.HOGAR,
            isCompleted = false,
            timeTag = "18:00"
        )
    )

    val predefinedCatalog = listOf(
        com.example.model.CatalogProduct("cat_1", "Leche entera larga vida 1L", ProductCategory.LACTEOS, 1250.0, "litro", "🥛"),
        com.example.model.CatalogProduct("cat_2", "Café tostado molido 250g", ProductCategory.ALMACEN, 3850.0, "paq.", "☕"),
        com.example.model.CatalogProduct("cat_3", "Pan de campo / masa madre", ProductCategory.ALMACEN, 1950.0, "kg", "🥖"),
        com.example.model.CatalogProduct("cat_4", "Aceite de oliva extra 500ml", ProductCategory.ALMACEN, 5400.0, "bot.", "🫒"),
        com.example.model.CatalogProduct("cat_5", "Detergente concentrado 750ml", ProductCategory.LIMPIEZA, 2200.0, "bot.", "🧴"),
        com.example.model.CatalogProduct("cat_6", "Arroz largo fino 1kg", ProductCategory.ALMACEN, 1600.0, "kg", "🍚"),
        com.example.model.CatalogProduct("cat_7", "Fideos secos spaghetti 500g", ProductCategory.ALMACEN, 1150.0, "paq.", "🍝"),
        com.example.model.CatalogProduct("cat_8", "Yerba mate suave con palo 1kg", ProductCategory.ALMACEN, 4200.0, "paq.", "🧉"),
        com.example.model.CatalogProduct("cat_9", "Azúcar blanca refinada 1kg", ProductCategory.ALMACEN, 980.0, "kg", "🍬"),
        com.example.model.CatalogProduct("cat_10", "Huevos de granja (Docena)", ProductCategory.FRESCOS, 3200.0, "doc.", "🥚"),
        com.example.model.CatalogProduct("cat_11", "Queso cremoso 500g", ProductCategory.LACTEOS, 4100.0, "trozo", "🧀"),
        com.example.model.CatalogProduct("cat_12", "Jamón cocido natural 200g", ProductCategory.ALMACEN, 2900.0, "paq.", "🥓"),
        com.example.model.CatalogProduct("cat_13", "Tomates perita seleccionados 1kg", ProductCategory.FRESCOS, 1850.0, "kg", "🍅"),
        com.example.model.CatalogProduct("cat_14", "Manzanas rojas deliciosas 1kg", ProductCategory.FRESCOS, 2200.0, "kg", "🍎"),
        com.example.model.CatalogProduct("cat_15", "Gaseosa cola 1.5L", ProductCategory.BEBIDAS, 2100.0, "bot.", "🥤"),
        com.example.model.CatalogProduct("cat_16", "Agua mineral sin gas 2L", ProductCategory.BEBIDAS, 950.0, "bot.", "💧"),
        com.example.model.CatalogProduct("cat_17", "Cerveza rubia clásica 1L", ProductCategory.BEBIDAS, 2300.0, "bot.", "🍺"),
        com.example.model.CatalogProduct("cat_18", "Papel higiénico premium x4", ProductCategory.LIMPIEZA, 2400.0, "pack", "🧻"),
        com.example.model.CatalogProduct("cat_19", "Lavandina concentrada 1L", ProductCategory.LIMPIEZA, 1100.0, "bot.", "🫧"),
        com.example.model.CatalogProduct("cat_20", "Jabón de tocador cremoso x3", ProductCategory.LIMPIEZA, 1850.0, "pack", "🧼"),
        com.example.model.CatalogProduct("cat_21", "Shampoo nutritivo 400ml", ProductCategory.LIMPIEZA, 3400.0, "bot.", "💆"),
        com.example.model.CatalogProduct("cat_22", "Dentífrico protección total 90g", ProductCategory.FARMACIA, 1950.0, "tubo", "🪥"),
        com.example.model.CatalogProduct("cat_23", "Ibuprofeno 400mg x 10 cápsulas", ProductCategory.FARMACIA, 2100.0, "caja", "💊"),
        com.example.model.CatalogProduct("cat_24", "Alcohol en gel sanitizante 250ml", ProductCategory.FARMACIA, 1500.0, "bot.", "🩹")
    )

    fun generateInventoryForStore(type: StoreType, storeSeed: Int): List<StoreProduct> {
        val multiplier = when (type) {
            StoreType.SUPERMERCADO -> 0.96
            StoreType.ALMACEN -> 1.05
            StoreType.KIOSCO -> 1.20
            StoreType.TIENDA_NATURAL -> 1.02
            StoreType.PANADERIA -> 1.04
            StoreType.FARMACIA -> 1.08
            StoreType.GASTRONOMIA -> 1.05
            StoreType.SERVICIOS -> 1.10
        }
        return predefinedCatalog.mapIndexed { idx, cat ->
            var itemMult = multiplier
            // Categorías con descuentos o ventajas específicas
            if (type == StoreType.PANADERIA && cat.category == ProductCategory.ALMACEN && cat.name.contains("Pan")) itemMult = 0.85
            if (type == StoreType.ALMACEN && (cat.category == ProductCategory.LACTEOS || cat.name.contains("Pan"))) itemMult = 0.92
            if (type == StoreType.FARMACIA && cat.category == ProductCategory.FARMACIA) itemMult = 0.88
            if (type == StoreType.TIENDA_NATURAL && (cat.name.contains("Aceite") || cat.category == ProductCategory.FRESCOS)) itemMult = 0.90
            if (type == StoreType.KIOSCO && cat.category == ProductCategory.BEBIDAS) itemMult = 1.15

            val jitter = 1.0 + (((idx + storeSeed) % 7 - 3) * 0.02)
            val price = Math.round(cat.basePrice * itemMult * jitter / 10.0) * 10.0
            val hasDiscount = (idx + storeSeed) % 4 == 0
            val originalPrice = if (hasDiscount) Math.round(price * 1.15 / 10.0) * 10.0 else null

            StoreProduct(
                id = "prod_${type.name}_$idx",
                name = cat.name,
                category = cat.category,
                price = price,
                originalPrice = originalPrice,
                inStock = true
            )
        }
    }

    val initialNeeds = listOf(
        NeedItem("n1", "Leche entera larga vida 1L", ProductCategory.LACTEOS, 2, "litro", false),
        NeedItem("n2", "Café tostado molido 250g", ProductCategory.ALMACEN, 1, "paq.", false),
        NeedItem("n3", "Pan de campo / masa madre", ProductCategory.ALMACEN, 1, "kg", false),
        NeedItem("n4", "Aceite de oliva extra 500ml", ProductCategory.ALMACEN, 1, "bot.", false),
        NeedItem("n5", "Detergente concentrado 750ml", ProductCategory.LIMPIEZA, 1, "bot.", false),
        NeedItem("n6", "Arroz largo fino 1kg", ProductCategory.ALMACEN, 1, "kg", false)
    )

    val sampleStores = listOf(
        Store(
            id = "store_redcom_romang",
            name = "Redcom - Tecnología, Informática & Conectividad",
            type = StoreType.ALMACEN,
            rating = 4.95f,
            reviewsCount = 184,
            distanceMeters = 40,
            address = "Simón de Iriondo y San Martín, Romang (Tu Local)",
            isOpen = true,
            hoursText = "Abierto de 08:00 a 12:30 y 16:30 a 20:30 hs",
            accentColorHex = 0xFF059669,
            photosCount = 12,
            hasVideoTour = true,
            videoTourDuration = "0:45",
            description = "Tu tienda de confianza en Romang: insumos de computación, accesorios para celulares, servicio técnico integral, periféricos gaming, cargadores rápidos y soporte de conectividad.",
            specialOfferText = "15% de reintegro en accesorios informáticos con OmniPay QR",
            minSpendDiscountThreshold = 5000.0,
            minSpendDiscountPercent = 15,
            comboDeals = listOf(
                ComboDeal("Combo Oficina / Estudio", "Cable USB-C reforzado + Cargador rápido 25W", "-20%"),
                ComboDeal("Kit Gaming Romang", "Mouse gamer RGB + Pad mouse XL", "Precio Barrial")
            ),
            inventory = listOf(
                StoreProduct("redcom_1", "Cable USB-C carga rápida 1m", ProductCategory.ALMACEN, 3200.0, 3900.0, true),
                StoreProduct("redcom_2", "Cargador pared 25W Fast Charge", ProductCategory.ALMACEN, 6800.0, 7900.0, true),
                StoreProduct("redcom_3", "Pendrive USB 3.0 Kingston 64GB", ProductCategory.ALMACEN, 5400.0, 6200.0, true),
                StoreProduct("redcom_4", "Mouse inalámbrico óptico USB", ProductCategory.ALMACEN, 4900.0, null, true),
                StoreProduct("redcom_5", "Auriculares In-Ear con micrófono", ProductCategory.ALMACEN, 3800.0, 4500.0, true),
                StoreProduct("redcom_6", "Resma papel A4 Boreal 75g (500 hojas)", ProductCategory.ALMACEN, 5800.0, null, true)
            ),
            mapOffsetX = 0.50f,
            mapOffsetY = 0.50f,
            openTime = "08:00",
            closeTime = "20:30",
            weekendHoursText = "Sábados de 08:30 a 13:00 hs",
            is24Hours = false,
            isOnDuty = false,
            openOnHolidays = false,
            scheduleNotes = "Atención comercial Redcom • Tu local de tecnología en Romang"
        ),
        Store(
            id = "coop_agricola_romang",
            name = "Supermercado Coop. Agrícola Ganadera de Romang Ltda.",
            type = StoreType.SUPERMERCADO,
            rating = 4.8f,
            reviewsCount = 320,
            distanceMeters = 130,
            address = "Calle San Martín y Brigadier López, Romang",
            isOpen = true,
            hoursText = "Abierto de 07:30 a 12:30 y 16:30 a 20:30 hs",
            accentColorHex = 0xFF0D9488,
            photosCount = 10,
            hasVideoTour = true,
            videoTourDuration = "0:40",
            description = "El supermercado tradicional de Romang: carnicería de pastura de la región, quesos de la cuenca santafesina, panadería, lácteos y gran variedad de productos de almacén al mejor precio cooperativo.",
            specialOfferText = "Billetera Santa Fe 30% reintegro los lunes, martes y miércoles",
            minSpendDiscountThreshold = 7000.0,
            minSpendDiscountPercent = 15,
            comboDeals = listOf(
                ComboDeal("Canasta Láctea Cuenca", "2 Leches 1L + Queso cremoso 500g", "-18%"),
                ComboDeal("Combo Almacén", "Aceite 1.5L + Arroz 1kg + Fideos 500g", "-15%")
            ),
            inventory = listOf(
                StoreProduct("p1_1", "Leche entera larga vida 1L", ProductCategory.LACTEOS, 1250.0, 1400.0, true),
                StoreProduct("p1_2", "Café tostado molido 250g", ProductCategory.ALMACEN, 3850.0, 4200.0, true),
                StoreProduct("p1_3", "Pan de campo / masa madre", ProductCategory.ALMACEN, 2100.0, null, true),
                StoreProduct("p1_4", "Aceite de oliva virgen extra 500ml", ProductCategory.ALMACEN, 5400.0, 5900.0, true),
                StoreProduct("p1_5", "Detergente concentrado vajilla 750ml", ProductCategory.LIMPIEZA, 2200.0, 2600.0, true),
                StoreProduct("p1_6", "Galletitas dulces surtidas 300g", ProductCategory.KIOSCO, 1150.0, null, true),
                StoreProduct("p1_7", "Yogur bebible frutilla 1L", ProductCategory.LACTEOS, 1800.0, 2000.0, true)
            ),
            mapOffsetX = 0.60f,
            mapOffsetY = 0.40f,
            openTime = "07:30",
            closeTime = "20:30",
            weekendHoursText = "Sábados de 08:00 a 13:00 y 17:00 a 20:30",
            is24Hours = false,
            isOnDuty = false,
            openOnHolidays = false,
            scheduleNotes = "Acepta Billetera Santa Fe, débito, crédito y OmniPay"
        ),
        Store(
            id = "farmacia_romang_turno",
            name = "Farmacia Romang (De Turno 24hs)",
            type = StoreType.FARMACIA,
            rating = 4.9f,
            reviewsCount = 215,
            distanceMeters = 180,
            address = "Calle San Martín 840, Romang",
            isOpen = true,
            hoursText = "DE TURNO 24HS • Guardia Activa en Romang",
            accentColorHex = 0xFF0D9488,
            photosCount = 8,
            hasVideoTour = true,
            videoTourDuration = "0:25",
            description = "Farmacia oficial de turno en Romang. Medicamentos con y sin receta, botiquín médico, insumos pediátricos, perfumería y ventanilla de guardia nocturna activa.",
            specialOfferText = "15% en analgésicos, repelentes y primeros auxilios",
            minSpendDiscountThreshold = 4500.0,
            minSpendDiscountPercent = 10,
            comboDeals = listOf(
                ComboDeal("Botiquín Verano", "Repelente spray + Alcohol en gel + Curitas", "-20%")
            ),
            inventory = listOf(
                StoreProduct("p6_1", "Ibuprofeno 400mg x 10 comp.", ProductCategory.FARMACIA, 1950.0, 2400.0, true),
                StoreProduct("p6_2", "Alcohol en gel sanitizante 250ml", ProductCategory.FARMACIA, 1400.0, null, true),
                StoreProduct("p6_3", "Pack apósitos protectores curitas x 20", ProductCategory.FARMACIA, 980.0, null, true),
                StoreProduct("p6_4", "Repelente de mosquitos spray 165ml", ProductCategory.FARMACIA, 3900.0, 4500.0, true),
                StoreProduct("p6_5", "Pañales descartables talle M x 36", ProductCategory.FARMACIA, 8600.0, 9500.0, true)
            ),
            mapOffsetX = 0.45f,
            mapOffsetY = 0.35f,
            openTime = "00:00",
            closeTime = "23:59",
            weekendHoursText = "Guardia 24hs continua sábados, domingos y feriados",
            is24Hours = true,
            isOnDuty = true,
            openOnHolidays = true,
            scheduleNotes = "De turno continuo hoy en Romang. Timbre de noche activo."
        ),
        Store(
            id = "panaderia_el_sol_romang",
            name = "Panadería & Confitería Artesanal El Sol",
            type = StoreType.PANADERIA,
            rating = 4.88f,
            reviewsCount = 190,
            distanceMeters = 95,
            address = "Simón de Iriondo 450, Romang",
            isOpen = true,
            hoursText = "Abierto de 06:45 a 12:30 y 16:30 a 20:30 hs",
            accentColorHex = 0xFFD97706,
            photosCount = 7,
            hasVideoTour = true,
            videoTourDuration = "0:30",
            description = "Pan fresco caliente salido del horno a la mañana y la tarde, facturas de manteca, criollitos santafesinos, chipá casero y masas finas en el corazón de Romang.",
            specialOfferText = "Docena de medialunas con 20% OFF después de las 18:00 hs",
            minSpendDiscountThreshold = 3500.0,
            minSpendDiscountPercent = 10,
            comboDeals = listOf(
                ComboDeal("Desayuno Romang", "Pan de campo recién horneado + Facturas", "$2.400")
            ),
            inventory = listOf(
                StoreProduct("p5_1", "Pan de campo / masa madre", ProductCategory.ALMACEN, 1750.0, 1950.0, true),
                StoreProduct("p5_2", "Café tostado molido 250g", ProductCategory.ALMACEN, 4300.0, null, true),
                StoreProduct("p5_3", "Leche entera larga vida 1L", ProductCategory.LACTEOS, 1450.0, null, true)
            ),
            mapOffsetX = 0.52f,
            mapOffsetY = 0.60f,
            openTime = "06:45",
            closeTime = "20:30",
            weekendHoursText = "Domingos de 07:30 a 13:00 hs",
            is24Hours = false,
            isOnDuty = false,
            openOnHolidays = true,
            scheduleNotes = "Horneadas calientes a las 07:00 hs y 17:00 hs"
        ),
        Store(
            id = "kiosco_centro_24hs_romang",
            name = "Kiosco & Minimarket Centro 24hs",
            type = StoreType.KIOSCO,
            rating = 4.7f,
            reviewsCount = 145,
            distanceMeters = 60,
            address = "San Martín y Ramírez, Romang",
            isOpen = true,
            hoursText = "Abierto 24 Horas continuas • 365 días",
            accentColorHex = 0xFFEF4444,
            photosCount = 6,
            hasVideoTour = false,
            videoTourDuration = "",
            description = "Bebidas bien frías, hielo en rolito, golosinas, artículos de almacén de urgencia, cigarrillos y recargas electrónicas en pleno centro de Romang.",
            specialOfferText = "2x1 en gaseosas y snacks nocturnos",
            minSpendDiscountThreshold = 3000.0,
            minSpendDiscountPercent = 8,
            comboDeals = listOf(
                ComboDeal("Combo Noche", "Gaseosa 1.5L + Snack salado", "-15%")
            ),
            inventory = listOf(
                StoreProduct("p3_1", "Leche entera larga vida 1L", ProductCategory.LACTEOS, 1500.0, null, true),
                StoreProduct("p3_2", "Café tostado molido 250g", ProductCategory.ALMACEN, 4200.0, 4600.0, true),
                StoreProduct("p3_3", "Galletitas dulces surtidas 300g", ProductCategory.KIOSCO, 1200.0, null, true),
                StoreProduct("p3_5", "Detergente concentrado vajilla 750ml", ProductCategory.LIMPIEZA, 2700.0, null, true)
            ),
            mapOffsetX = 0.48f,
            mapOffsetY = 0.52f,
            openTime = "00:00",
            closeTime = "23:59",
            weekendHoursText = "24 Horas continuas",
            is24Hours = true,
            isOnDuty = false,
            openOnHolidays = true,
            scheduleNotes = "Abierto toda la noche"
        ),
        Store(
            id = "carniceria_don_juan_romang",
            name = "Carnicería & Fiambrería Don Juan",
            type = StoreType.ALMACEN,
            rating = 4.92f,
            reviewsCount = 160,
            distanceMeters = 220,
            address = "Moreno y Belgrano, Romang",
            isOpen = true,
            hoursText = "Abierto de 08:00 a 12:30 y 17:00 a 20:30 hs",
            accentColorHex = 0xFFF59E0B,
            photosCount = 9,
            hasVideoTour = true,
            videoTourDuration = "0:30",
            description = "Cortes vacunos seleccionados de pastura, embutidos y chacinados artesanales propios de la costa santafesina, fiambres de primera y quesos.",
            specialOfferText = "10% de descuento en cortes para asado y picadas",
            minSpendDiscountThreshold = 8000.0,
            minSpendDiscountPercent = 10,
            comboDeals = listOf(
                ComboDeal("Combo Asado Costero", "Costilla + Vacío + Chorizos caseros", "Precio Amigo")
            ),
            inventory = listOf(
                StoreProduct("p2_1", "Leche entera larga vida 1L", ProductCategory.LACTEOS, 1380.0, null, true),
                StoreProduct("p2_2", "Café tostado molido 250g", ProductCategory.ALMACEN, 3950.0, null, true),
                StoreProduct("p2_3", "Pan de campo / masa madre", ProductCategory.ALMACEN, 1850.0, 2100.0, true),
                StoreProduct("p2_4", "Aceite de oliva virgen extra 500ml", ProductCategory.ALMACEN, 5800.0, null, true),
                StoreProduct("p2_5", "Detergente concentrado vajilla 750ml", ProductCategory.LIMPIEZA, 2450.0, null, true)
            ),
            mapOffsetX = 0.38f,
            mapOffsetY = 0.58f,
            openTime = "08:00",
            closeTime = "20:30",
            weekendHoursText = "Sábados y domingos medio día",
            is24Hours = false,
            isOnDuty = false,
            openOnHolidays = true,
            scheduleNotes = "Carne fresca todos los días"
        ),
        Store(
            id = "ypf_romang_full",
            name = "Estación de Servicio YPF Romang (Tienda Full 24hs)",
            type = StoreType.SUPERMERCADO,
            rating = 4.8f,
            reviewsCount = 280,
            distanceMeters = 480,
            address = "Ruta Provincial 1 Km 270, Romang",
            isOpen = true,
            hoursText = "Abierto 24 Horas • Tienda Full & Combustibles",
            accentColorHex = 0xFF1E40AF,
            photosCount = 11,
            hasVideoTour = true,
            videoTourDuration = "0:35",
            description = "Parada obligada sobre Ruta 1: Tienda Full con café express, sándwiches tostados calientes, bebidas, cajero automático y servicios al viajero las 24 horas.",
            specialOfferText = "10% reintegro en café y almuerzos abonando con App YPF / OmniPay",
            minSpendDiscountThreshold = 4000.0,
            minSpendDiscountPercent = 10,
            comboDeals = listOf(
                ComboDeal("Promo Full", "Café latte + Tostado de jamón y queso", "$3.500")
            ),
            inventory = listOf(
                StoreProduct("p4_1", "Aceite de oliva virgen extra 500ml", ProductCategory.ALMACEN, 5100.0, 5700.0, true),
                StoreProduct("p4_2", "Café tostado molido 250g", ProductCategory.ALMACEN, 4100.0, null, true),
                StoreProduct("p4_3", "Pan de campo / masa madre", ProductCategory.ALMACEN, 2200.0, 2400.0, true),
                StoreProduct("p4_4", "Leche vegetal almendras 1L", ProductCategory.LACTEOS, 2100.0, null, true)
            ),
            mapOffsetX = 0.70f,
            mapOffsetY = 0.30f,
            openTime = "00:00",
            closeTime = "23:59",
            weekendHoursText = "24 Horas continuas siempre",
            is24Hours = true,
            isOnDuty = false,
            openOnHolidays = true,
            scheduleNotes = "Ubicado sobre Ruta Provincial 1 acceso Romang"
        )
    )

    fun calculateComparison(stores: List<Store>, needs: List<NeedItem>, variationSeed: Int = 0): List<StoreComparisonResult> {
        return stores.mapIndexed { storeIdx, store ->
            val storeRandFactor = 1.0 + (((storeIdx * 7 + variationSeed * 13) % 15 - 7) / 100.0) // ±7% fluctuación de mercado
            val matchedItems = needs.map { need ->
                val requestedTerms = need.name.lowercase()
                    .split(Regex("[^\\p{L}\\p{N}]+"))
                    .filter { it.length >= 3 }
                val match = store.inventory.firstOrNull { product ->
                    if (!product.inStock || product.category != need.category) {
                        false
                    } else {
                        val productName = product.name.lowercase()
                        requestedTerms.isNotEmpty() &&
                            requestedTerms.any { term -> productName.contains(term) }
                    }
                }
                com.example.model.MatchedItem(
                    need = need,
                    matchedProduct = match,
                    isAvailable = match != null && match.inStock
                )
            }

            val availableCount = matchedItems.count { it.isAvailable }
            val rawSubtotal = matchedItems.filter { it.isAvailable }.sumOf { item ->
                (item.matchedProduct?.price ?: 0.0) * item.need.quantity
            }

            var discount = 0.0
            val effectiveThreshold = store.minSpendDiscountThreshold
            val effectivePercent = if (variationSeed > 0) ((store.minSpendDiscountPercent + (storeIdx + variationSeed) % 8)) else store.minSpendDiscountPercent
            if (rawSubtotal >= effectiveThreshold) {
                discount = rawSubtotal * (effectivePercent / 100.0)
            }

            val finalTotal = maxOf(0.0, rawSubtotal - discount)
            val regularTotalIfNoPromo = matchedItems.filter { it.isAvailable }.sumOf { item ->
                ((item.matchedProduct?.originalPrice ?: item.matchedProduct?.price) ?: 0.0) * item.need.quantity
            }
            val estimatedSavings = maxOf(0.0, (regularTotalIfNoPromo - rawSubtotal) + discount)

            var badge: String? = null
            when {
                store.distanceMeters <= 150 -> badge = "⚡ Más Cercano (${store.walkingTimeMin} min a pie)"
                store.rating >= 4.9f -> badge = "⭐ Local Destacado de la Comunidad"
                discount > 0 -> badge = "🏆 Ahorro Extra ($effectivePercent% OFF aplicado)"
                variationSeed > 0 && storeIdx % 2 == 0 -> badge = "🔥 Oferta Relámpago del Día"
            }

            StoreComparisonResult(
                store = store,
                matchedItems = matchedItems,
                availableCount = availableCount,
                totalRequestedCount = needs.size,
                rawSubtotal = rawSubtotal,
                thresholdDiscountApplied = discount,
                finalTotal = finalTotal,
                estimatedSavings = estimatedSavings,
                highlightBadge = badge
            )
        }.sortedWith(
            compareByDescending<StoreComparisonResult> { it.availableCount }
                .thenBy { it.finalTotal }
        )
    }

    val sampleContacts = listOf(
        NeighborhoodContact(
            id = "c1",
            name = "Remisería Belgrano Centro",
            category = com.example.model.ContactCategory.REMIS,
            phoneNumber = "+54 11 4782-9900",
            statusText = "Disponible ahora • 4 móviles en zona",
            rating = 4.8f,
            address = "Av. Cabildo 2240",
            isFavorite = true,
            averageWaitMinutes = 5
        ),
        NeighborhoodContact(
            id = "c2",
            name = "Radio Taxi Express Barrial",
            category = com.example.model.ContactCategory.TAXI,
            phoneNumber = "+54 11 4555-1234",
            statusText = "Móvil a 3 cuadras listo",
            rating = 4.7f,
            address = "Base Belgrano Norte",
            isFavorite = false,
            averageWaitMinutes = 3
        ),
        NeighborhoodContact(
            id = "c3",
            name = "Farmacia San Juan (De Turno 24hs)",
            category = com.example.model.ContactCategory.FARMACIA_TURNO,
            phoneNumber = "+54 11 4701-8844",
            statusText = "De Turno 24hs hoy • Guardias activas",
            rating = 4.9f,
            address = "Calle Juramento 1840",
            isFavorite = true
        ),
        NeighborhoodContact(
            id = "c4",
            name = "Cerrajería Urgencias 24hs Don Mario",
            category = com.example.model.ContactCategory.CERRAJERIA,
            phoneNumber = "+54 11 3820-1122",
            statusText = "Guardia activa • Llega en 15 minutos",
            rating = 4.95f,
            address = "Móvil a domicilio",
            isFavorite = false,
            averageWaitMinutes = 15
        ),
        NeighborhoodContact(
            id = "c5",
            name = "Fletes & Repartos San Martín",
            category = com.example.model.ContactCategory.FLETE,
            phoneNumber = "+54 11 6940-5566",
            statusText = "Camioneta disponible para compras grandes",
            rating = 4.75f,
            address = "Av. San Martín 1500",
            isFavorite = false
        ),
        NeighborhoodContact(
            id = "c6",
            name = "SAME / Emergencias Médicas 107",
            category = com.example.model.ContactCategory.EMERGENCIA,
            phoneNumber = "107",
            statusText = "Servicio público gratuito de emergencias",
            rating = 5.0f,
            isFavorite = true
        )
    )

    val sampleSuggestions = listOf(
        SmartSuggestion(
            id = "sug1",
            productName = "Manteca artesanal primera marca 200g",
            category = ProductCategory.LACTEOS,
            reasonText = "Combina con el pan y café de tu lista",
            discountBadge = "-20% con Combo Desayuno",
            storeName = "Almacén Don Pedro",
            price = 1450.0,
            originalPrice = 1800.0,
            isOnRoute = false
        ),
        SmartSuggestion(
            id = "sug2",
            productName = "Mermelada orgánica frutos del bosque",
            category = ProductCategory.ALMACEN,
            reasonText = "Ideal para acompañar tu pan de masa madre",
            discountBadge = "-15% especial",
            storeName = "Dietética Vida Verde",
            price = 2100.0,
            originalPrice = 2480.0,
            isOnRoute = false
        ),
        SmartSuggestion(
            id = "sug3",
            productName = "Esponjas antibacteriales (Pack x3)",
            category = ProductCategory.LIMPIEZA,
            reasonText = "Complemento para tu detergente concentrado",
            discountBadge = "2x1",
            storeName = "Supermercado San Martín Express",
            price = 950.0,
            originalPrice = 1350.0,
            isOnRoute = false
        ),
        SmartSuggestion(
            id = "sug4",
            productName = "Medialunas de manteca calientes (Docena)",
            category = ProductCategory.ALMACEN,
            reasonText = "A solo 25m de tu camino habitual",
            discountBadge = "Promo Mañana",
            storeName = "Panadería La Espiga de Oro",
            price = 3200.0,
            originalPrice = 4000.0,
            isOnRoute = true,
            detourMeters = 25
        ),
        SmartSuggestion(
            id = "sug5",
            productName = "Agua mineral con gas 2L",
            category = ProductCategory.BEBIDAS,
            reasonText = "Al paso sin desviarte de tu recorrido",
            discountBadge = "-25% Flash",
            storeName = "Kiosco La Esquina 24hs",
            price = 850.0,
            originalPrice = 1150.0,
            isOnRoute = true,
            detourMeters = 10
        )
    )

    val sampleNews = listOf(
        com.example.model.StoreNews(
            id = "news_1",
            storeId = "s5",
            storeName = "Panadería La Espiga de Oro",
            title = "¡Acaban de salir medialunas calientes de manteca!",
            content = "Horneada fresca recién salida del horno. 20% de descuento llevando docena en la próxima hora.",
            timeAgoText = "Hace 15 min",
            badge = "Recién Horneado 🔥"
        ),
        com.example.model.StoreNews(
            id = "news_2",
            storeId = "s1",
            storeName = "Supermercado San Martín",
            title = "Llegó Palta Hass premium a precio de cajón",
            content = "Stock limitado de palta directa del norte. $1.990 el kilo por compras superiores a $5.000.",
            timeAgoText = "Hace 1 hora",
            badge = "Oferta Flash 🥑"
        ),
        com.example.model.StoreNews(
            id = "news_3",
            storeId = "s2",
            storeName = "Almacén Don Pedro",
            title = "Quesos de campo y salamines tandileros ingresados hoy",
            content = "Cortamos fiambrería a la vista como siempre. Pide tu degustación gratuita al pasar.",
            timeAgoText = "Hace 3 horas",
            badge = "Novedad 🧀"
        )
    )

    fun calculateTrip(origin: String, destination: String): com.example.model.TripRoute {
        val destLower = destination.lowercase()
        val origLower = origin.lowercase()

        val origCoord = com.example.data.api.RealPlacesApi.resolveKnownCoordinates(origin)
        val destCoord = com.example.data.api.RealPlacesApi.resolveKnownCoordinates(destination)

        var isIntercity = false
        val distance: Double = if (origCoord != null && destCoord != null) {
            val distM = com.example.data.api.RealPlacesApi.calculateDistanceMeters(
                origCoord.first, origCoord.second,
                destCoord.first, destCoord.second
            )
            val km = distM / 1000.0
            isIntercity = km > 25.0
            Math.round(km * 10.0) / 10.0
        } else if (destCoord != null && (origLower.contains("mi ubicaci") || origLower.contains("actual") || origLower.contains("gps") || origLower.isBlank() || origLower.contains("romang"))) {
            // Asume punto de partida Romang base (-29.5052, -59.7562) hacia destino
            val distM = com.example.data.api.RealPlacesApi.calculateDistanceMeters(
                -29.5052, -59.7562,
                destCoord.first, destCoord.second
            )
            val km = distM / 1000.0
            isIntercity = km > 25.0
            Math.round(km * 10.0) / 10.0
        } else {
            when {
                destLower.contains("obelisco") || destLower.contains("centro") -> 6.4
                destLower.contains("palermo") || destLower.contains("soho") -> 2.8
                destLower.contains("retiro") || destLower.contains("estacion") -> 4.9
                destLower.contains("aeroparque") -> 7.8
                destLower.contains("plaza de mayo") -> 6.9
                destLower.contains("san martín") || origLower.contains("san martín") -> 3.2
                else -> {
                    val hashVal = kotlin.math.abs((origin + destination).hashCode())
                    val dynamicDist = 1.4 + ((hashVal % 75) / 10.0)
                    Math.round(dynamicDist * 10.0) / 10.0
                }
            }
        }

        val walking: Int
        val driving: Int
        val transit: Int
        val remisCost: Double

        val steps: List<com.example.model.TripStep>
        val landmarks: List<String>

        if (isIntercity || distance > 25.0) {
            driving = maxOf(15, ((distance / 88.0) * 60).toInt())
            transit = maxOf(25, ((distance / 72.0) * 60).toInt())
            walking = (distance * 12).toInt()
            remisCost = Math.round(2500.0 + (distance * 280.0)).toDouble()

            steps = listOf(
                com.example.model.TripStep("Salida desde $origin", "0 km", "Partida hacia ruta / autopista principal"),
                com.example.model.TripStep("Ingreso a Autovía / Ruta Nacional", "${String.format("%.1f", distance * 0.15)} km", "Tránsito de ruta a velocidad constante (~90 km/h)"),
                com.example.model.TripStep("Tramo Interurbano y Peajes", "${String.format("%.1f", distance * 0.70)} km", "Seguimiento satelital y estaciones de servicio"),
                com.example.model.TripStep("Llegada a $destination", "${String.format("%.1f", distance * 0.15)} km", "Acceso urbano final y arribo a destino")
            )
            landmarks = listOf(
                "Estación YPF Full / Shell Select en ruta",
                "Área de Parador y Descanso",
                "Puesto de Peaje y Control Vial",
                "Acceso Autopista"
            )
        } else {
            walking = maxOf(10, (distance * 13).toInt())
            driving = maxOf(3, (distance * 2.8).toInt())
            transit = maxOf(6, (distance * 4.2).toInt())
            remisCost = Math.round(850.0 + (distance * 460.0)).toDouble()

            steps = listOf(
                com.example.model.TripStep("Salida desde $origin", "0 m", "Comienza el recorrido en dirección a tu destino"),
                com.example.model.TripStep("Incorporación a avenida principal", "${String.format("%.1f", distance * 0.4)} km", "Sigue la señalización hacia $destination"),
                com.example.model.TripStep("Avance por tramo intermedio", "${String.format("%.1f", distance * 0.4)} km", "Tránsito verificado con Google Maps"),
                com.example.model.TripStep("Llegada a $destination", "${String.format("%.1f", distance * 0.2)} km", "Destino alcanzado • Parada habilitada")
            )
            landmarks = listOf(
                "Farmacia y Comercios de cercanía",
                "Parada de transporte público",
                "Kiosco y Punto de Recarga",
                "Estación de Servicio"
            )
        }

        return com.example.model.TripRoute(
            originName = origin.ifBlank { "Mi Ubicación Actual" },
            destinationName = destination.ifBlank { "Destino Seleccionado" },
            distanceKm = distance,
            walkingMinutes = walking,
            drivingMinutes = driving,
            transitMinutes = transit,
            estimatedRemisCost = remisCost,
            steps = steps,
            landmarksOnRoute = landmarks
        )
    }

    val sampleWeatherAlerts = listOf(
        com.example.model.WeatherAlert(
            id = "alert_1",
            type = com.example.model.AlertType.TORMENTA,
            severity = com.example.model.AlertSeverity.AMARILLO,
            title = "Alerta Amarilla por Tormentas Fuertes",
            description = "Se prevén tormentas aisladas con ocasional caída de granizo y ráfagas intensas en tu zona geográfica.",
            instructions = "Evita dejar objetos sueltos en balcones y planifica compras techadas.",
            validUntil = "Hoy hasta 19:00 hs"
        ),
        com.example.model.WeatherAlert(
            id = "alert_2",
            type = com.example.model.AlertType.CLIMA_CALOR,
            severity = com.example.model.AlertSeverity.INFO,
            title = "Aviso Preventivo de Radiación UV & Calor",
            description = "Índice UV alto entre las 11:30 y 16:30 hs. Temperatura máxima estimada de 30°C.",
            instructions = "Lleva agua mineral contigo al salir y prefiere recorridos sombreados.",
            validUntil = "Hoy hasta 17:00 hs"
        )
    )

    val sampleMorningBriefing = com.example.model.MorningBriefing(
        greeting = "¡Buen día! Esperamos que tengas una excelente jornada.",
        dateText = "Jueves, 29 de Octubre • Comienzo de día",
        weatherSummary = "Cielo despejado a algo nublado con brisa agradable",
        temperatureRange = "Min 17°C • Máx 28°C",
        motivationalQuote = "El orden en tu mañana define la paz de tu día.",
        activeDealsCount = 5,
        pendingTasksCount = 3
    )

    val sampleReceipts = listOf(
        com.example.model.DigitalReceipt(
            id = "rec_1",
            receiptNumber = "T-0048-0001928",
            storeName = "Supermercado San Martín Express",
            dateText = "28 Oct 2026, 18:42 hs",
            items = listOf(
                com.example.model.ReceiptItem("Leche entera larga vida 1L", 2, 1250.0, 2500.0),
                com.example.model.ReceiptItem("Café tostado molido 250g", 1, 3850.0, 3850.0),
                com.example.model.ReceiptItem("Detergente concentrado 750ml", 1, 2200.0, 2200.0)
            ),
            subtotal = 8550.0,
            discountApplied = 1282.5,
            total = 7267.5,
            paymentMethod = "OmniPay QR (5% Cashback Acreditado)",
            qrVerificationCode = "OMNI-VALID-98214"
        ),
        com.example.model.DigitalReceipt(
            id = "rec_2",
            receiptNumber = "T-0012-0000412",
            storeName = "Panadería La Espiga de Oro",
            dateText = "27 Oct 2026, 09:15 hs",
            items = listOf(
                com.example.model.ReceiptItem("Pan de masa madre 1kg", 1, 1750.0, 1750.0),
                com.example.model.ReceiptItem("Medialunas de manteca (Docena)", 1, 3200.0, 3200.0)
            ),
            subtotal = 4950.0,
            discountApplied = 500.0,
            total = 4450.0,
            paymentMethod = "Tarjeta Visa Débito Galicia",
            qrVerificationCode = "OMNI-VALID-41209"
        )
    )

    val sampleTouristAttractions = ArgentinaAttractions.nearby(-29.5052, -59.9990)
}
