package com.serotonin.common.networking


import kotlinx.serialization.json.*

object JsonMetadataBuilder {
    @JvmStatic
    fun buildShopMetadata(metadata: Map<String, Map<String, Any>>): JsonObject {
        return buildJsonObject {
            put("type", "shop_metadata_sync")
            put("metadata", buildJsonObject {
                for ((category, inner) in metadata) {
                    put(category.lowercase(), buildJsonObject {
                        for ((key, value) in inner) {
                            when (value) {
                                is Number -> put(key, JsonPrimitive(value))
                                is String -> put(key, JsonPrimitive(value))
                            }
                        }
                    })
                }
            })
        }
    }
}
