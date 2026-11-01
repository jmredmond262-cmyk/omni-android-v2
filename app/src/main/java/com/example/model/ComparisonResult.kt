package com.example.model

data class MatchedItem(
    val need: NeedItem,
    val matchedProduct: StoreProduct?,
    val isAvailable: Boolean
)

data class StoreComparisonResult(
    val store: Store,
    val matchedItems: List<MatchedItem>,
    val availableCount: Int,
    val totalRequestedCount: Int,
    val rawSubtotal: Double,
    val thresholdDiscountApplied: Double,
    val finalTotal: Double,
    val estimatedSavings: Double,
    val highlightBadge: String? = null
) {
    val coveragePercent: Int
        get() = if (totalRequestedCount > 0) (availableCount * 100) / totalRequestedCount else 100
}
