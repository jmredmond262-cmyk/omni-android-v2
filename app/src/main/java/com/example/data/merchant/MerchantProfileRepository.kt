package com.example.data.merchant

import com.example.model.ComboDeal
import com.example.model.ProductCategory
import com.example.model.Store
import com.example.model.StoreNews
import com.example.model.StoreProduct
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Persists the merchant-owned part of a Store in the existing merchantProfiles document.
 * Product photo URIs are device-local until OMNI has an approved free image-hosting strategy.
 */
object MerchantProfileRepository {
    private val db = FirebaseFirestore.getInstance()

    fun loadOrClaim(store: Store, uid: String, callback: (Result<Pair<Store, List<StoreNews>>>) -> Unit) {
        val ref = db.collection("merchantProfiles").document(store.id)
        ref.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val ownerUid = snapshot.getString("ownerUid").orEmpty()
                    if (ownerUid != uid) {
                        callback(Result.failure(IllegalStateException("Este comercio ya está vinculado a otra cuenta OMNI.")))
                    } else {
                        callback(Result.success(decode(snapshot, store)))
                    }
                } else {
                    ref.set(mapOf(
                        "storeId" to store.id,
                        "ownerUid" to uid,
                        "storeName" to store.name,
                        "createdAt" to FieldValue.serverTimestamp()
                    )).addOnSuccessListener {
                        callback(Result.success(store to emptyList()))
                    }.addOnFailureListener { callback(Result.failure(it)) }
                }
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun save(store: Store, news: List<StoreNews>, uid: String, callback: (Result<Unit>) -> Unit) {
        val data = mapOf(
            "storeId" to store.id,
            "ownerUid" to uid,
            "storeName" to store.name,
            "address" to store.address,
            "phone" to store.phone,
            "description" to store.description,
            "isOpen" to store.isOpen,
            "hoursText" to store.hoursText,
            "openTime" to store.openTime,
            "closeTime" to store.closeTime,
            "weekendHoursText" to store.weekendHoursText,
            "is24Hours" to store.is24Hours,
            "isOnDuty" to store.isOnDuty,
            "openOnHolidays" to store.openOnHolidays,
            "scheduleNotes" to store.scheduleNotes,
            "minSpendDiscountThreshold" to store.minSpendDiscountThreshold,
            "minSpendDiscountPercent" to store.minSpendDiscountPercent,
            "specialOfferText" to store.specialOfferText,
            "comboDeals" to store.comboDeals.map { deal ->
                mapOf(
                    "title" to deal.title,
                    "description" to deal.description,
                    "discountBadge" to deal.discountBadge,
                    "audience" to deal.audience
                )
            },
            "inventory" to store.inventory.map { product ->
                mapOf(
                    "id" to product.id,
                    "name" to product.name,
                    "category" to product.category.name,
                    "price" to product.price,
                    "originalPrice" to product.originalPrice,
                    "inStock" to product.inStock,
                    "unitLabel" to product.unitLabel,
                    "photoUri" to product.photoUri,
                    "barcode" to product.barcode,
                    "description" to product.description
                )
            },
            "news" to news.map { item ->
                mapOf(
                    "id" to item.id,
                    "storeId" to item.storeId,
                    "storeName" to item.storeName,
                    "title" to item.title,
                    "content" to item.content,
                    "timeAgoText" to item.timeAgoText,
                    "badge" to item.badge,
                    "isHot" to item.isHot
                )
            },
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("merchantProfiles").document(store.id)
            .set(data, SetOptions.merge())
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }


    /**
     * Enriches real directory results with the merchant's latest public catalog.
     * It never claims a profile and falls back to the directory data if Firestore is unavailable.
     */
    fun loadPublicCatalogs(stores: List<Store>, callback: (List<Store>) -> Unit) {
        if (stores.isEmpty()) {
            callback(emptyList())
            return
        }
        db.collection("merchantProfiles").get()
            .addOnSuccessListener { snapshot ->
                val profiles = snapshot.documents
                val enriched = stores.map { base ->
                    val profile = profiles.firstOrNull { it.id == base.id }
                        ?: profiles.firstOrNull { document ->
                            val nameMatches = document.getString("storeName").orEmpty()
                                .equals(base.name, ignoreCase = true)
                            val profileAddress = document.getString("address").orEmpty()
                            val addressMatches = profileAddress.isNotBlank() &&
                                (base.address.contains(profileAddress, ignoreCase = true) ||
                                    profileAddress.contains(base.address, ignoreCase = true))
                            nameMatches && addressMatches
                        }
                    if (profile != null) decode(profile, base).first else base
                }
                callback(enriched)
            }
            .addOnFailureListener { callback(stores) }
    }

    private fun decode(snapshot: DocumentSnapshot, base: Store): Pair<Store, List<StoreNews>> {
        val combos = (snapshot.get("comboDeals") as? List<*>)?.mapNotNull { raw ->
            val map = raw as? Map<*, *> ?: return@mapNotNull null
            ComboDeal(
                title = map["title"] as? String ?: return@mapNotNull null,
                description = map["description"] as? String ?: "",
                discountBadge = map["discountBadge"] as? String ?: "",
                audience = map["audience"] as? String ?: "TODOS"
            )
        } ?: base.comboDeals

        val products = (snapshot.get("inventory") as? List<*>)?.mapNotNull { raw ->
            val map = raw as? Map<*, *> ?: return@mapNotNull null
            val categoryName = map["category"] as? String ?: ProductCategory.ALMACEN.name
            val category = runCatching { ProductCategory.valueOf(categoryName) }.getOrDefault(ProductCategory.ALMACEN)
            StoreProduct(
                id = map["id"] as? String ?: return@mapNotNull null,
                name = map["name"] as? String ?: return@mapNotNull null,
                category = category,
                price = (map["price"] as? Number)?.toDouble() ?: 0.0,
                originalPrice = (map["originalPrice"] as? Number)?.toDouble(),
                inStock = map["inStock"] as? Boolean ?: true,
                unitLabel = map["unitLabel"] as? String ?: "unid.",
                photoUri = map["photoUri"] as? String,
                barcode = map["barcode"] as? String,
                description = map["description"] as? String ?: ""
            )
        }.orEmpty().ifEmpty { base.inventory }

        val updated = base.copy(
            address = snapshot.getString("address") ?: base.address,
            phone = snapshot.getString("phone") ?: base.phone,
            description = snapshot.getString("description") ?: base.description,
            isOpen = snapshot.getBoolean("isOpen") ?: base.isOpen,
            hoursText = snapshot.getString("hoursText") ?: base.hoursText,
            openTime = snapshot.getString("openTime") ?: base.openTime,
            closeTime = snapshot.getString("closeTime") ?: base.closeTime,
            weekendHoursText = snapshot.getString("weekendHoursText") ?: base.weekendHoursText,
            is24Hours = snapshot.getBoolean("is24Hours") ?: base.is24Hours,
            isOnDuty = snapshot.getBoolean("isOnDuty") ?: base.isOnDuty,
            openOnHolidays = snapshot.getBoolean("openOnHolidays") ?: base.openOnHolidays,
            scheduleNotes = snapshot.getString("scheduleNotes") ?: base.scheduleNotes,
            minSpendDiscountThreshold = snapshot.getDouble("minSpendDiscountThreshold") ?: base.minSpendDiscountThreshold,
            minSpendDiscountPercent = (snapshot.getLong("minSpendDiscountPercent") ?: base.minSpendDiscountPercent.toLong()).toInt(),
            specialOfferText = snapshot.getString("specialOfferText") ?: base.specialOfferText,
            comboDeals = combos,
            inventory = products
        )

        val news = (snapshot.get("news") as? List<*>)?.mapNotNull { raw ->
            val map = raw as? Map<*, *> ?: return@mapNotNull null
            StoreNews(
                id = map["id"] as? String ?: return@mapNotNull null,
                storeId = map["storeId"] as? String ?: base.id,
                storeName = map["storeName"] as? String ?: base.name,
                title = map["title"] as? String ?: return@mapNotNull null,
                content = map["content"] as? String ?: "",
                timeAgoText = map["timeAgoText"] as? String ?: "Publicado",
                badge = map["badge"] as? String ?: "Novedad",
                isHot = map["isHot"] as? Boolean ?: true
            )
        }.orEmpty()

        return updated to news
    }
}
