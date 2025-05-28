package com.serotonin.common.api.events

import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor
import com.serotonin.common.api.events.EloManager.playerElos
import com.serotonin.common.api.events.EloManager.sendEloChangeMessage
import com.serotonin.common.client.gui.competitivehandbook.CustomBookScreen
import com.serotonin.common.elosystem.cachedElo
import com.serotonin.common.elosystem.claimedTiers
import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import kotlin.math.pow
import java.util.UUID
//import com.serotonin.common.api.events.EloManager.sendEloChangeMessage
import com.serotonin.common.elosystem.getTierName
import com.serotonin.common.elosystem.mapOfRankedPlayerNameTags
import com.serotonin.common.elosystem.pendingNameTagUpdates
import com.serotonin.common.elosystem.playerRankTags
import com.serotonin.common.elosystem.resetAllClaimedTiers
import com.serotonin.common.elosystem.resetClaimedTiers
import com.serotonin.common.elosystem.updatePlayerNametag
import com.serotonin.common.networking.ClientEloStorage
import com.serotonin.common.networking.Database
import com.serotonin.common.networking.LeaderboardData
import com.serotonin.common.networking.PlayerStatsPayload
import com.serotonin.common.networking.RankResponsePayload
import com.serotonin.common.networking.RawJsonPayload
import com.serotonin.common.networking.ServerContext.server
import com.serotonin.common.networking.getPlayerStats
import com.serotonin.common.networking.requestPlayerElo
import com.serotonin.common.networking.saveCobbledollarsToDatabase
import com.serotonin.common.networking.sendEloUpdateToClient
import net.minecraft.client.MinecraftClient
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import java.time.Duration
import java.time.Instant
import kotlin.collections.filterValues
import kotlin.concurrent.thread
import fr.harmex.cobbledollars.common.utils.extensions.addCobbleDollars
import fr.harmex.cobbledollars.common.utils.extensions.getCobbleDollars
import java.math.BigInteger


//private val silentPlayers = Collections.synchronizedSet(mutableSetOf<UUID>())

object EloManager {

    private val pendingEloCallbacks = mutableMapOf<UUID, (Int) -> Unit>()
    val playerElos = mutableMapOf<UUID, Int>()
    val recentMatchStats = mutableMapOf<UUID, String>()
    val lastStatsAccess = mutableMapOf<UUID, Instant>()
    val battleStartTimes = mutableMapOf<String, Instant>()


    fun handleEloResponseServer(uuid: UUID, elo: Int) {
        println("handleEloResponseServer called with uuid=$uuid, elo=$elo")

        try {

            val callback = pendingEloCallbacks.remove(uuid)
            if (callback != null) {
                println("handleEloResponse: running callback for $uuid")
                callback(elo)
                //return //temp disable i guess
            }

            playerElos[uuid] = elo
            println("handleEloResponse: no callback for $uuid — showing message")

            val player = pendingNameTagUpdates.remove(uuid)
            if (player != null) {
                try {
                    val tier = getTierName(elo)
                    println("Updating nametag for ${player.name.string} with Elo $elo")
                    // val rankText = Text.literal(tier)
                    //    .append(Text.literal(": "))
                    //    .append(Text.literal("§c§l$elo"))


                    updatePlayerNametag(player, elo, getTierName(elo))


                    val nameTag = mapOfRankedPlayerNameTags[uuid]
                    val rankTag = playerRankTags[uuid]

                    if (nameTag == null || rankTag == null) {
                        println("Missing tags after updatePlayerNametag for $uuid")
                    } else if (!nameTag.hasVehicle() || !rankTag.hasVehicle()) {
                        println("Entity attachments failed: nameTag.hasVehicle=${nameTag.hasVehicle()}, rankTag.hasVehicle=${rankTag.hasVehicle()}")
                    }

                } catch (e: Exception) {
                    println("Error creating rank entities for player ${player.name.string}: ${e.message}")
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            println("Error in handleEloResponse for UUID $uuid: ${e.message}")
            e.printStackTrace()
        }
    }


    fun handleEloResponseClient(uuid: UUID, elo: Int) {
        try {
            val callback = pendingEloCallbacks.remove(uuid)
            if (callback != null) {
                println("handleEloResponseClient: running callback for $uuid")
                callback(elo)
                return
            }

            playerElos[uuid] = elo
            println("handleEloResponseClient: stored Elo $elo for $uuid")

        } catch (e: Exception) {
            println("Error in handleEloResponseClient for UUID $uuid: ${e.message}")
            e.printStackTrace()
        }
    }


    fun handleClientRankDisplay(payload: RankResponsePayload) {
        try {
            val uuid = payload.uuid
            val elo = payload.elo
            val name = payload.name
            val tier = getTierName(elo)
            val isSelf = MinecraftClient.getInstance().player?.uuidAsString == uuid

            if (payload.reset) {
                println("Payload marked reset — clearing client cache")
                ClientEloStorage.clear()

                requestUpdatedClaimedTiers()


                val screen = MinecraftClient.getInstance().currentScreen
                if (screen is CustomBookScreen) {
                    screen.forceEloRefresh(elo)
                }

                MinecraftClient.getInstance().player?.sendMessage(
                    Text.literal("§7[Info] Rank data has been reset.")
                )
            }

            ClientEloStorage.setElo(uuid, elo)

            if (payload.silent) {
                println("handleClientRankDisplay: payload marked silent, skipping")
                return
            }


            val client = MinecraftClient.getInstance()
            val player = client.player ?: return

            if (isSelf) {
                (client.currentScreen as? com.serotonin.common.client.gui.competitivehandbook.CustomBookScreen)?.apply {
                    currentElo = elo
                    println("Updated currentElo in GUI to $elo")
                }
            }

            val msg = if (isSelf) {
                "Your rank is $tier: §c§l$elo"
            } else {
                "$name's rank is $tier: §c§l$elo"
            }

            player.sendMessage(Text.literal(msg))
            println("Displayed rank: $msg")

        } catch (e: Exception) {
            println("Error displaying rank from payload: ${e.message}")
            e.printStackTrace()
        }
    }

    fun requestElo(
        uuid: UUID,
        player: ServerPlayerEntity,
        silent: Boolean = false,
        callback: ((Int) -> Unit)? = null
    ) {
        if (callback != null) {
            pendingEloCallbacks[uuid] = callback
        }

        val isSelfRequest = player.uuid == uuid
        requestPlayerElo(player, uuid, isSelfRequest, silent)
        println("requestElo: Requested Elo for $uuid (player ${player.name.string}), silent=$silent")
    }

    fun requestUpdatedClaimedTiers() {
        val json = buildJsonObject {
            put("type", "get_claimed_tiers")
        }
        ClientPlayNetworking.send(RawJsonPayload(json.toString()))
    }


    fun preloadElo(
        players: List<UUID>,
        playerLookup: (UUID) -> ServerPlayerEntity?,
        onComplete: (Map<UUID, Int>) -> Unit
    ) {
        val preloaded = mutableMapOf<UUID, Int>()
        val remaining = players.toMutableSet()

        players.forEach { uuid ->
            val player = playerLookup(uuid) ?: return@forEach
            requestElo(uuid, player, silent = true) { elo ->
                println("Preloaded Elo: $uuid = $elo")
                preloaded[uuid] = elo
                remaining.remove(uuid)

                if (remaining.isEmpty()) {
                    println("All Elo preloaded, firing event")
                    onComplete(preloaded)
                }
            }
        }
    }

    fun handleBattleVictoryEvent(event: BattleVictoryEvent, server: MinecraftServer) {
        val winners = event.winners.filterIsInstance<PlayerBattleActor>()
        val losers = event.losers.filterIsInstance<PlayerBattleActor>()
        val uuids = (winners + losers).map { it.uuid }.distinct()

        preloadElo(uuids, { server.playerManager.getPlayer(it) }) { preloadedElo ->
            val rankedEvent = RankedMatchVictoryEvent(event, winners, losers, false, preloadedElo)
            RankedMatchVictoryEvent.RMVictoryEvent.fire(rankedEvent)
        }
    }

    fun sendEloDataToDatabase(player: ServerPlayerEntity, uuid: UUID, newElo: Int) {
        try {

            Database.dataSource.connection.use { conn ->
                val stmt = conn.prepareStatement(
                    "UPDATE player_stats SET elo = ?, tier = ?, last_updated = CURRENT_TIMESTAMP WHERE player_id = ?"
                )
                stmt.setInt(1, newElo)
                stmt.setString(2, getTierName(newElo))
                stmt.setObject(3, uuid)
                stmt.executeUpdate()
                stmt.close()
            }

            playerElos[uuid] = newElo
            cachedElo[uuid] = newElo

            val rankName = getTierName(newElo)
            val tag = playerRankTags[uuid]
            if (tag != null && tag.isAlive) {

                val newText = Text.literal(rankName)
                    .append(Text.literal(": §c§l$newElo"))
                tag.customName = newText
            } else {

                updatePlayerNametag(player, newElo, rankName)
            }




            sendEloUpdateToClient(player, uuid, newElo, silent = true)
            println("Saved updated Elo $newElo for ${player.name.string} to database.")

        } catch (e: Exception) {
            println("Failed to save Elo to database for ${player.name.string}: ${e.message}")
            e.printStackTrace()
        }
    }

    //old match stats message
    /*fun sendEloChangeMessage(player: ServerPlayerEntity, oldElo: Int, newElo: Int) {
            val change = newElo - oldElo
            val changeText = if (change >= 0) "§aYou gained §l$change§r§a points!" else "§cYou lost §l${-change}§r§c points!"
            val rankName = getTierName(newElo)

            val message = "$changeText §rYou are now rank §e$rankName§r with §c§l$newElo§r points."

            player.sendMessage(Text.literal(message))
        }*/


    fun incrementBattleStats(uuid: UUID, won: Boolean) {
        try {
            Database.dataSource.connection.use { conn ->
                val stmt = conn.prepareStatement(
                    """
                UPDATE player_stats
                SET battles_total = battles_total + 1,
                    battles_won = battles_won + ?
                WHERE player_id = ?
            """.trimIndent()
                )
                stmt.setInt(1, if (won) 1 else 0)
                stmt.setObject(2, uuid)
                stmt.executeUpdate()
                stmt.close()

                println("Updated battle stats for $uuid: won=$won")
            }
        } catch (e: Exception) {
            println("Failed to update battle stats for $uuid: ${e.message}")
            e.printStackTrace()
        }
    }

    fun updateWinStreak(uuid: UUID, won: Boolean): Int {
        var newStreak = 0
        try {
            Database.dataSource.connection.use { conn ->
                val stmt = conn.prepareStatement(
                    """
    UPDATE player_stats
    SET 
        win_streak = CASE WHEN ? THEN win_streak + 1 ELSE 0 END,
        longest_win_streak = GREATEST(longest_win_streak, 
            CASE WHEN ? THEN win_streak + 1 ELSE 0 END)
    WHERE player_id = ?
    RETURNING win_streak
""".trimIndent()
                )
                stmt.setBoolean(1, won)
                stmt.setBoolean(2, won)
                stmt.setObject(3, uuid)
                val rs = stmt.executeQuery()
                if (rs.next()) newStreak = rs.getInt("win_streak")
                stmt.close()
            }
        } catch (e: Exception) {
            println("Failed to update win streak for $uuid: ${e.message}")
        }
        return newStreak
    }


    fun sendEloChangeMessage(
        player: ServerPlayerEntity,
        oldElo: Int,
        newElo: Int,
        winners: List<String>,
        losers: List<String>,
        duration: String = "00:00",
        turns: Int = 0,
        bonusPoints: Int = 0
    ) {
        val uuid = player.uuid
        val change = newElo - oldElo - bonusPoints
        val rankName = getTierName(newElo)

        val coloredWinners = winners.joinToString(" §7vs§r ") { "§6$it" }
        val coloredLosers = losers.joinToString(" §7vs§r ") { "§f$it" }
        val vsLine = if (winners.isNotEmpty() && losers.isNotEmpty()) {
            "$coloredWinners §7vs§r $coloredLosers"
        } else (winners + losers).joinToString(" §7vs§r ") { "§f$it" }

        val outcomeLine = if (change >= 0)
            "§aYou gained §l$change§r§a points! §fyour new rank is §r$rankName: §c§l$newElo§r"
        else
            "§cYou lost §l${-change}§r§c points. §fyour new rank is §r$rankName: §c§l$newElo§r"

        val stats = getPlayerStats(uuid)
        val streak = stats?.winStreak ?: 0
        val longest = stats?.longestWinStreak ?: 0
        val streakLine = if (change >= 0) {
            if (streak <= 1) "§fCurrent Streak: §70 (Longest: §7$longest)"
            else "§bCurrent Streak: §b$streak §7(Longest: $longest)"
        } else {
            "§cStreak lost! §fCurrent Streak: 0 §7(Longest: $longest)"
        }

        val bonusLine = if (change >= 0 && bonusPoints > 0) {
            "§a+$bonusPoints bonus points"
        } else ""


        val statsDetail = listOf(
            "§e§l--- Recent Match Statistics ---",
            vsLine,
            outcomeLine,
            "§fBattle Duration: §7$duration",
            "§fTurns Taken: §7$turns",
            streakLine,
            bonusLine
        ).filter { it.isNotBlank() }
            .joinToString("\n")

        EloManager.recentMatchStats[uuid] = statsDetail

        val statsComponent = Text.literal("§a[Match Stats]")
            .styled {
                it.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/matchstats"))
                    .withHoverEvent(
                        HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Text.literal("§7Click to view match stats")
                        )
                    )
            }

        val message = Text.literal("§bBattle ended. Click here to view ").append(statsComponent)
        player.sendMessage(message)
    }

    fun cleanupOldMatchStats() {
        val cutoff = Instant.now().minus(Duration.ofMinutes(5))
        val toRemove = lastStatsAccess.filterValues { it.isBefore(cutoff) }.keys

        for (uuid in toRemove) {
            recentMatchStats.remove(uuid)
            lastStatsAccess.remove(uuid)
        }

        if (toRemove.isNotEmpty()) {
            println("Cleaned up ${toRemove.size} expired match stats.")
        }
    }


    fun backupEloData() {
        val timestamp =
            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"))
        val backupFile = java.io.File("elo_backup_$timestamp.json")

        try {
            Database.dataSource.connection.use { conn ->
                conn.prepareStatement("SELECT player_id, player_name, elo, tier FROM player_stats").use { stmt ->
                    val rs = stmt.executeQuery()
                    val data = mutableMapOf<String, Any>()

                    while (rs.next()) {
                        data[rs.getString("player_id")] = mapOf(
                            "name" to rs.getString("player_name"),
                            "elo" to rs.getInt("elo"),
                            "tier" to rs.getString("tier")
                        )
                    }

                    backupFile.writeText(Json.encodeToString(data))
                    println("Elo data backup created: ${backupFile.name}")
                }
            }
        } catch (e: Exception) {
            println("Failed to create Elo backup: ${e.message}")
        }
    }
}


data class RankedMatchVictoryEvent(
    val battle: BattleVictoryEvent,
    val winners: List<BattleActor>,
    val losers: List<BattleActor>,
    val wasWildCapture: Boolean,
    val preloadedElo: Map<UUID, Int> = emptyMap()
) {
    fun updateEloForAll(server: MinecraftServer?) {
        println("Updating Elo for all participants")


        val playerWinners: List<PlayerBattleActor> = winners.filterIsInstance<PlayerBattleActor>()
        val playerLosers: List<PlayerBattleActor> = losers.filterIsInstance<PlayerBattleActor>()

        val winnerNames = playerWinners.mapNotNull { server?.playerManager?.getPlayer(it.uuid)?.name?.string }
        val loserNames = playerLosers.mapNotNull { server?.playerManager?.getPlayer(it.uuid)?.name?.string }

        val pokemonBattle = battle.battle
        val startInstant = EloManager.battleStartTimes.remove(pokemonBattle.battleId.toString())

        val durationStr = if (startInstant != null) {
            val duration = Duration.between(startInstant, Instant.now())
            String.format("%02d:%02d", duration.toMinutesPart(), duration.toSecondsPart())
        } else {
            "??:??"
        }
        val turnCount = pokemonBattle.turn


        playerWinners.forEach { winner ->
            playerLosers.forEach { loser ->


                var eloWinner = preloadedElo[winner.uuid] //?: 1000
                var eloLoser = preloadedElo[loser.uuid] //?: 1000




                if (eloWinner == null || eloLoser == null) {
                    println("Missing Elo data: ${if (eloWinner == null) "Winner ${winner.uuid}" else ""} ${if (eloLoser == null) "Loser ${loser.uuid}" else ""}")


                    if (eloWinner == null) {
                        println("WARNING: Missing winner Elo data for ${winner.uuid}, attempting recovery")


                        val winnerStats = getPlayerStats(winner.uuid)
                        if (winnerStats != null) {
                            eloWinner = winnerStats.elo
                            println("Recovery successful: Found stats in database for ${winner.uuid}: $eloWinner")
                        } else {

                            val cachedElo = playerElos[winner.uuid]
                            if (cachedElo != null) {
                                eloWinner = cachedElo
                                println("Recovery successful: Found in local cache for ${winner.uuid}: $eloWinner")
                            } else {
                                eloWinner = 1000
                                println("CRITICAL: No data found for ${winner.uuid}, using default 1000. Player may lose rating!")

                                logMissingEloIssue(winner.uuid, "winner")
                            }
                        }
                    }

                    if (eloLoser == null) {
                        println("WARNING: Missing loser Elo data for ${loser.uuid}, attempting recovery")


                        val loserStats = getPlayerStats(loser.uuid)
                        if (loserStats != null) {
                            eloLoser = loserStats.elo
                            println("Recovery successful: Found stats in database for ${loser.uuid}: $eloLoser")
                        } else {

                            val cachedElo = playerElos[loser.uuid]
                            if (cachedElo != null) {
                                eloLoser = cachedElo
                                println("Recovery successful: Found in local cache for ${loser.uuid}: $eloLoser")
                            } else {

                                eloLoser = 1000
                                println("CRITICAL: No data found for ${loser.uuid}, using default 1000. Player may lose rating!")

                                logMissingEloIssue(loser.uuid, "loser")
                            }
                        }
                    }
                }

                val expectedWinner = 1.0 / (1.0 + 10.0.pow((eloLoser - eloWinner) / 400.0))
                val expectedLoser = 1.0 / (1.0 + 10.0.pow((eloWinner - eloLoser) / 400.0))
                val k = 32

                val winnerChange = max(1.0, k * (1 - expectedWinner))
                val loserChange = max(1.0, k * (0 - expectedLoser).absoluteValue)

                val streak = EloManager.updateWinStreak(winner.uuid, true)
                val bonus = minOf(50, streak)  // Cap at +50
                val newEloWinner = (eloWinner + winnerChange + bonus).toInt()
                EloManager.updateWinStreak(loser.uuid, false)
                val newEloLoser = (eloLoser - loserChange).toInt()

                val winnerPlayer = server?.playerManager?.getPlayer(winner.uuid)
                val loserPlayer = server?.playerManager?.getPlayer(loser.uuid)

                if (winnerPlayer != null) {
                    EloManager.sendEloDataToDatabase(winnerPlayer, winner.uuid, newEloWinner)
                    EloManager.incrementBattleStats(winner.uuid, won = true)
                    sendPlayerStatsPayload(winnerPlayer)

                    val cobbleDollarReward = newEloWinner - eloWinner
                    if (cobbleDollarReward > 0) {
                        try {
                            winnerPlayer.addCobbleDollars(BigInteger.valueOf(cobbleDollarReward.toLong()))
                            println("Gave $cobbleDollarReward CobbleDollars to ${winnerPlayer.name.string}")
                            winnerPlayer.sendMessage(Text.literal("§fYou earned §a$$cobbleDollarReward for winning!"))
                            saveCobbledollarsToDatabase(winnerPlayer.uuid, winnerPlayer.getCobbleDollars())
                        } catch (e: Exception) {
                            println("Failed to give CobbleDollars to ${winnerPlayer.name.string}: ${e.message}")
                            e.printStackTrace()
                        }
                    }

                    sendEloChangeMessage(
                        winnerPlayer,
                        eloWinner,
                        newEloWinner,
                        winnerNames,
                        loserNames,
                        durationStr,
                        turnCount,
                        bonus
                    )
                } else {
                    println("Winner player ${winner.uuid} is offline, cannot send Elo")

                    updateOfflinePlayerElo(winner.uuid, newEloWinner)


                }
                if (loserPlayer != null) {
                    EloManager.sendEloDataToDatabase(loserPlayer, loser.uuid, newEloLoser)
                    EloManager.incrementBattleStats(loser.uuid, won = false)
                    sendPlayerStatsPayload(loserPlayer)
                    sendEloChangeMessage(
                        loserPlayer,
                        eloLoser,
                        newEloLoser,
                        winnerNames,
                        loserNames,
                        durationStr,
                        turnCount,
                        0
                    )
                } else {
                    println("Loser player ${loser.uuid} is offline, cannot send Elo")
                    // try to update from db if offine - hopefully prevents people from just DCing if theyre gonna lose the battle
                    updateOfflinePlayerElo(loser.uuid, newEloLoser)

                }

                println("Elo Updated: ${winner.uuid} = $newEloWinner, ${loser.uuid} = $newEloLoser")
            }
        }
    }


    private fun updateOfflinePlayerElo(uuid: UUID, newElo: Int) {
        try {
            Database.dataSource.connection.use { conn ->
                val stmt = conn.prepareStatement(
                    "UPDATE player_stats SET elo = ?, tier = ?, last_updated = CURRENT_TIMESTAMP WHERE player_id = ?"
                )
                stmt.setInt(1, newElo)
                stmt.setString(2, getTierName(newElo))
                stmt.setObject(3, uuid)
                val updated = stmt.executeUpdate()
                stmt.close()

                if (updated > 0) {
                    println("Updated Elo for offline player $uuid to $newElo")
                } else {
                    println("No rows updated for player $uuid")
                }
            }
        } catch (e: Exception) {
            println("Error updating offline player Elo: ${e.message}")
            e.printStackTrace()
        }
    }


    fun interface RMVictoryEvent {
        fun onVictory(event: RankedMatchVictoryEvent)

        companion object {
            val EVENT: Event<RMVictoryEvent> = EventFactory.createArrayBacked(
                RMVictoryEvent::class.java
            ) { listeners ->
                RMVictoryEvent { event ->
                    println("RMVictoryEvent listener triggered")
                    listeners.forEach { it.onVictory(event) }
                }
            }

            fun fire(event: RankedMatchVictoryEvent) {
                println("Firing RMVictoryEvent")
                EVENT.invoker().onVictory(event)
            }
        }
    }
}


private fun logMissingEloIssue(uuid: UUID, role: String) {
    try {
        val timestamp = java.time.LocalDateTime.now()
        val logFile = java.io.File("elo_critical_issues.log")
        logFile.appendText("[$timestamp] CRITICAL: Missing Elo data for $role $uuid\n")
    } catch (e: Exception) {
        println("Failed to log missing Elo issue: ${e.message}")
    }
}


fun resetPlayerRankStats(uuid: UUID?) {
    thread(name = if (uuid != null) "reset-rank-$uuid" else "reset-all-ranks") {
        try {
            val defaultElo = 1000
            val defaultTier = getTierName(defaultElo)
            val emptyClaims = "[]"

            Database.dataSource.connection.use { conn ->
                val stmt = if (uuid != null) {
                    conn.prepareStatement(
                        "UPDATE player_stats SET elo = ?, tier = ?, claimed_tiers = '{}'::text[], last_updated = CURRENT_TIMESTAMP WHERE player_id = ?"
                    ).apply {
                        setInt(1, defaultElo)
                        setString(2, defaultTier)
                        setObject(3, uuid)
                    }
                } else {
                    conn.prepareStatement(
                        "UPDATE player_stats SET elo = ?, tier = ?, claimed_tiers = '{}'::text[], last_updated = CURRENT_TIMESTAMP"
                    ).apply {
                        setInt(1, defaultElo)
                        setString(2, defaultTier)
                    }
                }

                val count = stmt.executeUpdate()
                println("Updated $count rows for player $uuid")
                stmt.close()

                if (count == 0) {
                    println("No rows updated — UUID may not match DB. Skipping sync.")
                    return@thread
                }


                if (uuid != null) {

                    EloManager.playerElos[uuid] = defaultElo
                    claimedTiers.remove(uuid)
                    EloManager.recentMatchStats.remove(uuid)
                    EloManager.lastStatsAccess.remove(uuid)
                    resetClaimedTiers(uuid)


                    val player = server?.playerManager?.getPlayer(uuid)
                    if (player != null) {

                        val stats = getPlayerStats(uuid)
                        println("Post-reset stats: $stats")
                        if (stats != null) {
                            sendEloUpdateToClient(player, uuid, defaultElo, silent = false, reset = true)
                        }
                        val claimedRequestJson = buildJsonObject {
                            put("type", "get_claimed_tiers")
                        }.toString()
                        ServerPlayNetworking.send(player, RawJsonPayload(claimedRequestJson))
                    }

                    println("Reset rank stats for $uuid")
                } else {
                    EloManager.playerElos.clear()
                    claimedTiers.clear()
                    EloManager.recentMatchStats.clear()
                    EloManager.lastStatsAccess.clear()
                    LeaderboardData.latestLeaderboard.clear()
                    resetAllClaimedTiers()

                    val players = server?.playerManager?.playerList ?: emptyList()
                    for (player in players) {
                        sendEloUpdateToClient(player, player.uuid, defaultElo, silent = false, reset = true)

                        val claimedRequestJson = buildJsonObject {
                            put("type", "get_claimed_tiers")
                        }.toString()
                        ServerPlayNetworking.send(player, RawJsonPayload(claimedRequestJson))
                    }

                    println("Reset rank stats for ALL players ($count entries)")
                }
            }
        } catch (e: Exception) {
            println("Failed to reset rank stats: ${e.message}")
            e.printStackTrace()
        }
    }
}

fun getMinimumEloForTier(tierId: String): Int = when (tierId) {
    "poke_ball" -> 0
    "great_ball" -> 1500
    "ultra_ball" -> 2000
    "master_ball" -> 2500
    "pseudo_legendary" -> 3000
    "legendary" -> 3500
    "mythical" -> 4000
    "ultra_beast" -> 4500
    else -> Int.MAX_VALUE
}

fun sendPlayerStatsPayload(player: ServerPlayerEntity) {
    try {
        val uuid = player.uuid
        val stats = getPlayerStats(uuid) ?: return
        val payload = PlayerStatsPayload(
            uuid = uuid.toString(),
            name = stats.name,
            elo = stats.elo,
            tier = stats.tier,
            battlesWon = stats.battlesWon,
            battlesTotal = stats.battlesTotal,
            winStreak = stats.winStreak,
            longestWinStreak = stats.longestWinStreak
        )
        ServerPlayNetworking.send(player, payload)
        println("Sent PlayerStatsPayload to ${player.name.string}: $payload")
    } catch (e: Exception) {
        println("Failed to send PlayerStatsPayload: ${e.message}")
        e.printStackTrace()
    }
}
