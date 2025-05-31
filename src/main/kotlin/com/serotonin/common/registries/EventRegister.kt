package com.serotonin.common.registries
//import com.serotonin.common.api.events.RankedMatchVictoryListener
import com.cobblemon.mod.common.api.events.CobblemonEvents.BATTLE_STARTED_POST
import com.cobblemon.mod.common.api.events.CobblemonEvents.BATTLE_STARTED_PRE
import com.cobblemon.mod.common.api.events.CobblemonEvents.BATTLE_VICTORY
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor
import com.serotonin.common.api.events.EloManager
import com.serotonin.common.api.events.RankedMatchVictoryEvent
import com.serotonin.common.networking.ServerContext.server
import com.serotonin.common.networking.getFriendlyBattle
import com.serotonin.common.networking.getPlayerStats
import com.serotonin.common.networking.setFriendlyBattle
import com.serotonin.common.networking.syncFriendlyBattleToClient
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

object FriendlyBattleManager {
    private val playerSettingCache = ConcurrentHashMap<UUID, Boolean>()


    fun cacheSetting(uuid: UUID, setting: Boolean) {
        playerSettingCache[uuid] = setting
    }

    fun getCachedSetting(uuid: UUID): Boolean {
        return playerSettingCache[uuid] ?: false
    }

    fun clearCache(uuid: UUID) {
        playerSettingCache.remove(uuid)
    }



    fun preloadFromDatabase(uuid: UUID, name: String, player: ServerPlayerEntity? = null) {
        thread(name = "preload-friendly-$uuid") {
            try {
                val setting = getFriendlyBattle(uuid)
                cacheSetting(uuid, setting)
                println("Cached friendly battle for $name ($uuid): $setting")

                player?.let {
                    syncFriendlyBattleToClient(it, setting)
                }

            } catch (e: Exception) {
                println("Failed to load friendly battle for $name ($uuid): ${e.message}")
                cacheSetting(uuid, false)
            }
        }
    }
}

fun toggleFriendly(uuid: UUID, newValue: Boolean) {
    thread(name = "toggle-friendly-ui-$uuid") {
        try {

            setFriendlyBattle(uuid, newValue)

            FriendlyBattleManager.cacheSetting(uuid, newValue)


            val player = server?.playerManager?.getPlayer(uuid)
            if (player != null) {
                syncFriendlyBattleToClient(player, newValue)
            }
        } catch (e: Exception) {
            println("Error toggling friendly battle from UI for $uuid: ${e.message}")
            e.printStackTrace()
        }
    }
}



object RankedBattleEvents {

    val battleEloCache = ConcurrentHashMap<String, Map<UUID, Int>>()
    val friendlyBattleCache = ConcurrentHashMap<String, Boolean>()
    val activePlayerBattles = ConcurrentHashMap<UUID, String>()

    fun registerBattleEvents() {
        println("registerBattleEvents() called")


        BATTLE_STARTED_PRE.subscribe { startEvent ->
            println("BATTLE_STARTED_PRE: Checking friendly battle settings...")

            val playerActors = startEvent.battle.actors.filterIsInstance<PlayerBattleActor>()
            val playerUuids = playerActors.map { it.uuid }

            val friendlyStatuses = playerUuids.associateWith { uuid ->
                FriendlyBattleManager.getCachedSetting(uuid)
            }

            val allFriendly = friendlyStatuses.values.all { it }
            val noneFriendly = friendlyStatuses.values.all { !it }

            if (!allFriendly && !noneFriendly) {
                playerActors.forEach {
                    val player = it.entity
                    player?.sendMessage(Text.literal("§cBattle cancelled: all participants must have Friendly Battle either ON or OFF."))
                }
                println("Battle cancelled due to mixed friendly battle settings: $friendlyStatuses")
                startEvent.cancel()
            } else {
                val battleId = startEvent.battle.battleId.toString()
                friendlyBattleCache[battleId] = allFriendly
                println("Battle approved. Friendly battle: $allFriendly")
            }
        }


        BATTLE_STARTED_POST.subscribe { startEvent ->
            println("Battle started: ${startEvent.battle.battleId}")

            val battleId = startEvent.battle.battleId.toString()

            startEvent.battle.actors
                .filterIsInstance<PlayerBattleActor>()
                .forEach { actor ->
                    activePlayerBattles[actor.uuid] = battleId
                    println("Marked ${actor.uuid} as in battle $battleId")
                }




            val players = startEvent.battle.actors
                .filterIsInstance<PlayerBattleActor>()
                .map { it.uuid }

            if (players.isNotEmpty()) {

                preloadBattleEloData(startEvent.battle.battleId.toString(), players)
                EloManager.battleStartTimes[startEvent.battle.battleId.toString()] = Instant.now()

            }
            ActionResult.PASS
        }



        BATTLE_VICTORY.subscribe { victoryEvent ->

            println("Battle: ${victoryEvent.battle}")
            println("Winners: ${victoryEvent.winners}")
            println("Losers: ${victoryEvent.losers}")
            println("Was wild capture: ${victoryEvent.wasWildCapture}")


            println("BATTLE_VICTORY event triggered")
            val winners = victoryEvent.winners.filterIsInstance<PlayerBattleActor>()
            val losers = victoryEvent.losers.filterIsInstance<PlayerBattleActor>()

            if (winners.isNotEmpty() && losers.isNotEmpty()) {
                val battleId = victoryEvent.battle.battleId.toString()
                val preloadedElo = battleEloCache.remove(battleId) ?: emptyMap()


                val isFriendly = friendlyBattleCache.remove(victoryEvent.battle.battleId.toString()) ?: false

                val event = RankedMatchVictoryEvent(
                    battle = victoryEvent,
                    winners = winners,
                    losers = losers,
                    wasWildCapture = false,
                    preloadedElo = preloadedElo
                )

                RankedMatchVictoryEvent.RMVictoryEvent.fire(event)
                if (!isFriendly) {
                    event.updateEloForAll(server)
                } else {
                    println("Friendly battle detected: skipping Elo updates.")
                }

            }

            victoryEvent.battle.actors
                .filterIsInstance<PlayerBattleActor>()
                .forEach { actor ->
                    activePlayerBattles.remove(actor.uuid)
                    println("Removed ${actor.uuid} from active battles (battle ended)")
                }


            ActionResult.PASS
        }
    }


    private fun preloadBattleEloData(battleId: String, playerUuids: List<UUID>) {
        println("Preloading Elo data for battle: $battleId with players: $playerUuids")

        val eloData = mutableMapOf<UUID, Int>()

        playerUuids.forEach { uuid ->
            try {
                val stats = getPlayerStats(uuid)
                if (stats != null) {
                    eloData[uuid] = stats.elo
                    println("Preloaded Elo for $uuid: ${stats.elo}")
                } else {
                    eloData[uuid] = 1000
                    println("No stats found for $uuid, using default 1000")
                }
            } catch (e: Exception) {
                println("Error loading Elo data for $uuid: ${e.message}")
                eloData[uuid] = 1000
            }
        }


        battleEloCache[battleId] = eloData
    }


    fun initializeCleanupTask(server: MinecraftServer) {
        var tickCounter = 0
        ServerTickEvents.START_SERVER_TICK.register { _ ->
            tickCounter++
            if (tickCounter >= 6000) {
                cleanupStaleData()
                tickCounter = 0
            }
        }
    }

    private fun cleanupStaleData() {
        if (battleEloCache.isNotEmpty()) {
            println("Cleaning up stale battle Elo data. Before: ${battleEloCache.size} entries")
            println("After cleanup: ${battleEloCache.size} entries")
        }
    }
}
