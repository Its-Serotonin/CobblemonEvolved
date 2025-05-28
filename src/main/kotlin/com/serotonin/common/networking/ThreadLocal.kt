@file:JvmName("CategoryContext")
package com.serotonin.common.networking

object CategoryContext {
    private val current = ThreadLocal<String>()

    @JvmStatic
    fun set(name: String) = current.set(name.lowercase())

    @JvmStatic
    fun get(): String? = current.get()

    @JvmStatic
    fun clear() = current.remove()
}