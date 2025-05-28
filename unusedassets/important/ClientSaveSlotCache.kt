package com.serotonin.common.saveslots

data class ClientSaveSlotMetadata(
    val lastSaved: Long,
    val screenshotPath: String? = null
)

object ClientSaveSlotCache {
    private val lastSaved = mutableMapOf<Int, Long>()
    private var activeSlot: Int? = null

    fun updateSlot(slot: Int, timestamp: Long, screenshotPath: String? = null) {
        lastSaved[slot] = timestamp
    }

    fun getSlot(slot: Int): Long? = lastSaved[slot]
    fun isEmpty(slot: Int): Boolean = !lastSaved.containsKey(slot)
    fun clear() {
        lastSaved.clear()
    }


    fun setActiveSlot(slot: Int) {
        activeSlot = slot
    }

    fun getActiveSlot(): Int? = activeSlot

    fun isActive(slot: Int): Boolean = activeSlot == slot

    fun getAllSlots(): Map<Int, Long> = lastSaved.toMap()
}
