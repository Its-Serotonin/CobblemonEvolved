package com.serotonin.common.saveslots

data class ClientSaveSlotMetadata(
    val lastSaved: Long,
    val screenshotPath: String? = null
)

object ClientSaveSlotCache {
    private val slotData = mutableMapOf<Int, ClientSaveSlotMetadata>()
    private var activeSlot: Int? = null

    fun updateSlot(slot: Int, timestamp: Long, screenshotPath: String? = null) {
        slotData[slot] = ClientSaveSlotMetadata(timestamp, screenshotPath)
    }

    fun getSlot(slot: Int): ClientSaveSlotMetadata? = slotData[slot]

    fun isEmpty(slot: Int): Boolean = !slotData.containsKey(slot)

    fun clear() {
        slotData.clear()
    }

    fun setActiveSlot(slot: Int) {
        activeSlot = slot
    }

    fun getActiveSlot(): Int? = activeSlot

    fun isActive(slot: Int): Boolean = activeSlot == slot

    fun getAllSlots(): Map<Int, ClientSaveSlotMetadata> = slotData.toMap()
}