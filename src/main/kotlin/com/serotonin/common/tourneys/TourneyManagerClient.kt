package com.serotonin.common.tourneys


import net.minecraft.client.MinecraftClient
import java.util.*

data class CachedTournament(val ruleset: String, val startTime: String, val status: String)
//for expanding to tournament uuid store
//data class CachedTournament(val id: UUID, val ruleset: String, val startTime: String, val status: String)


object TournamentManagerClient {
    private val signupCache = mutableMapOf<UUID, Boolean>()
    private var cachedTournament: CachedTournament? = null

    fun cacheSignupStatus(uuid: UUID, status: Boolean) {
        signupCache[uuid] = status
    }

    fun isSignedUp(uuid: UUID): Boolean {
        return signupCache[uuid] == true
    }

    fun isSignedUpCached(): Boolean {
        val uuid = MinecraftClient.getInstance().player?.uuid ?: return false
        return isSignedUp(uuid)
    }

    fun clearSignupCache() {
        val uuid = MinecraftClient.getInstance().player?.uuid ?: return
        signupCache.remove(uuid)
    }

    fun cacheTournamentInfo(ruleset: String, startTime: String, status: String) {
        cachedTournament = CachedTournament(ruleset, startTime, status)
    }

    fun getCachedTournament(): CachedTournament? = cachedTournament

    fun clearTournamentCache() {
        cachedTournament = null
    }

    fun hasCachedTournament(): Boolean = cachedTournament != null

}
