package com.serotonin.common.saveslots


import net.minecraft.server.MinecraftServer
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object ActiveSlotTracker {
    private val activeSlots: MutableMap<UUID, Int> = ConcurrentHashMap()

    fun setSlot(uuid: UUID, slot: Int) {
        activeSlots[uuid] = slot
    }

    fun getSlot(uuid: UUID): Int? = activeSlots[uuid]

    fun clear(uuid: UUID) {
        activeSlots.remove(uuid)
    }

    fun cleanupOfflinePlayers(online: Set<UUID>) {
        activeSlots.keys.removeIf { it !in online }
    }
}


object SaveSlotCooldowns {
    private const val COOLDOWN_MS = 3000L
    private val lastSwitchTimestamps = ConcurrentHashMap<UUID, Long>()

    fun isOnCooldown(uuid: UUID): Boolean {
        val now = System.currentTimeMillis()
        val last = lastSwitchTimestamps[uuid] ?: return false
        return now - last < COOLDOWN_MS
    }

    fun updateTimestamp(uuid: UUID) {
        lastSwitchTimestamps[uuid] = System.currentTimeMillis()
    }

    fun clear(uuid: UUID) {
        lastSwitchTimestamps.remove(uuid)
    }

    fun cleanupOfflinePlayers(online: Set<UUID>) {
        lastSwitchTimestamps.keys.removeIf { it !in online }
    }
}