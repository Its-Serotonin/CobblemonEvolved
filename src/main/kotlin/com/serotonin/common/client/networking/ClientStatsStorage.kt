package com.serotonin.common.client.networking

import com.serotonin.common.networking.PlayerStatsPayload
import java.util.UUID

object ClientStatsStorage {
    private val statsMap: MutableMap<UUID, PlayerStatsPayload> = mutableMapOf()

    fun setStats(uuid: UUID, payload: PlayerStatsPayload) {
        statsMap[uuid] = payload
    }

    fun getStats(uuid: UUID): PlayerStatsPayload? {
        return statsMap[uuid]
    }

    fun clear() {
        statsMap.clear()
    }
    fun clearStats(uuid: UUID) {
        statsMap.remove(uuid)
    }
}