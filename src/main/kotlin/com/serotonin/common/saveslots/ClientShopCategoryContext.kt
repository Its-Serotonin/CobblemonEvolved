package com.serotonin.common.saveslots

// Kotlin object (or Java class with static field)

object ClientShopCategoryContext {
    private var currentCategory: String? = null

    fun set(category: String?) {
        currentCategory = category?.lowercase()
    }

    fun get(): String? = currentCategory

    fun clear() {
        currentCategory = null
    }
}