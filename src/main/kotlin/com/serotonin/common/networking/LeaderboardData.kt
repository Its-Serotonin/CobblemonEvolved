package com.serotonin.common.networking

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class LeaderboardEntry(
    val name: String,
    val elo: Int,
    val tier: String,
    val prefix: String,
    val longestWinStreak: Int = 0
)

object LeaderboardData {
    val latestLeaderboard = mutableListOf<LeaderboardEntry>()
    val pendingLeaderboardCallbacks: MutableMap<UUID, () -> Unit> = mutableMapOf()
}

object LeaderboardServerCache {
    val latestLeaderboard = mutableListOf<LeaderboardEntry>()
}
