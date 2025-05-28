package com.serotonin.common.networking


object ShopMetadataRegistry {
    val metadataByCategory: MutableMap<String, Map<String, Any>> = mutableMapOf()

    fun getMetadata(categoryName: String): Map<String, Any> {
        return metadataByCategory[categoryName.lowercase()] ?: emptyMap()
    }

    fun getRequiredTierLevel(categoryName: String): Int {
        val meta = getMetadata(categoryName)
        return (meta["requiredTierLevel"] as? Number)?.toInt() ?: 0
    }

    fun putMetadata(categoryName: String, metadata: Map<String, Any>) {
        metadataByCategory[categoryName.lowercase()] = metadata
    }
}