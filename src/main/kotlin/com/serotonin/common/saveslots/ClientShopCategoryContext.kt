package com.serotonin.common.saveslots


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