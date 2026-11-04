package com.example.data.analytics

/**
 * Single entry point for module activity sent to Omni Admin.
 *
 * Native Android does not expose fetch(keepalive). Firestore's Android SDK keeps
 * pending writes in its persistent local queue and retries them after connectivity
 * returns; Admin reads the same usageEvents collection.
 */
object OmniTracker {
    fun track(module: String, action: String, data: Map<String, Any> = emptyMap()) {
        UsageAnalyticsRepository.trackCurrentAction(module, action, data)
    }
}
