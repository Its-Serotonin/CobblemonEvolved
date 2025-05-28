package com.serotonin.common.saveslots

object SaveSlotScreenshotCache {
    private val slotScreenshots = mutableMapOf<Int, ByteArray>()

    fun store(slot: Int, data: ByteArray) {
        slotScreenshots[slot] = data
    }

    fun get(slot: Int): ByteArray? = slotScreenshots[slot]
}