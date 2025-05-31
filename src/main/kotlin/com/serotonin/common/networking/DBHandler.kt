package com.serotonin.common.networking

import com.cobblemon.mod.common.CobblemonItems
import com.gmail.brendonlf.cobblemon_utility.Item.UtilityItems
import com.serotonin.common.elosystem.getTierName
import net.minecraft.item.ItemStack
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import java.math.BigInteger
import java.util.*
import kotlin.concurrent.thread

data class PlayerStats(
    val uuid: UUID,
    val name: String,
    val elo: Int,
    val tier: String,
    val battlesWon: Int,
    val battlesTotal: Int,
    val winStreak: Int,
    val longestWinStreak: Int
)

fun getPlayerStats(uuid: UUID): PlayerStats? {
    try {

        Database.dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT player_name, elo, tier, battles_won, battles_total, win_streak, longest_win_streak FROM player_stats WHERE player_id = ?")
                .use { stmt ->
                    println("getPlayerStats: Executing DB query for uuid $uuid")
                    stmt.setObject(1, uuid, java.sql.Types.OTHER)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            val stats = PlayerStats(
                                uuid,
                                rs.getString("player_name"),
                                rs.getInt("elo"),
                                rs.getString("tier"),
                                rs.getInt("battles_won"),
                                rs.getInt("battles_total"),
                                rs.getInt("win_streak"),
                                rs.getInt("longest_win_streak")
                            )
                            println("DB Result: Found stats for $uuid: ${stats.elo}")
                            return stats
                        } else {
                            println("DB Result: No stats found for $uuid")
                            return null
                        }
                    }
                }
        }
    } catch (e: Exception) {
        println("DB Error: ${e.message}")
        e.printStackTrace()
        return null
    }
}


fun updatePlayerStats(stats: PlayerStats) {
    try {
        Database.dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
            INSERT INTO player_stats (player_id, player_name, elo, tier)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (player_id) DO UPDATE
            SET player_name = EXCLUDED.player_name,
                elo = EXCLUDED.elo,
                tier = EXCLUDED.tier,
                last_updated = CURRENT_TIMESTAMP
            """.trimIndent()
            ).use { stmt ->
                stmt.setObject(1, stats.uuid)
                stmt.setString(2, stats.name)
                stmt.setInt(3, stats.elo)
                stmt.setString(4, stats.tier)

                val rowsAffected = stmt.executeUpdate()
                println("Updated player stats for ${stats.name} (${stats.uuid}), rows affected: $rowsAffected")
            }
        }
    } catch (e: Exception) {
        println("Error updating player stats: ${e.message}")
        e.printStackTrace()
    }
}

fun findDuplicateUUIDsByName(name: String): List<UUID> {
    val duplicates = mutableListOf<UUID>()
    try {
        Database.dataSource.connection.use { conn ->
            conn.prepareStatement(
                "SELECT player_id FROM player_stats WHERE player_name = ?"
            ).use { stmt ->
                stmt.setString(1, name)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        duplicates.add(UUID.fromString(rs.getString("player_id")))
                    }
                }
            }
        }
    } catch (e: Exception) {
        println("Error finding duplicates for name '$name': ${e.message}")
        e.printStackTrace()
    }
    return duplicates
}

fun deduplicateByName(name: String) {
    try {
        Database.dataSource.connection.use { conn ->
            val keepUuid: UUID? = conn.prepareStatement(
                """
                SELECT player_id FROM player_stats
                WHERE player_name = ?
                ORDER BY last_updated DESC
                LIMIT 1
            """.trimIndent()
            ).use { stmt ->
                stmt.setString(1, name)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) UUID.fromString(rs.getString("player_id")) else null
                }
            }

            if (keepUuid != null) {
                conn.prepareStatement(
                    """
                    DELETE FROM player_stats
                    WHERE player_name = ?
                    AND player_id != ?
                """.trimIndent()
                ).use { stmt ->
                    stmt.setString(1, name)
                    stmt.setObject(2, keepUuid)
                    val deleted = stmt.executeUpdate()
                    println("Deduplicated '$name': $deleted old record(s) removed")
                }
            } else {
                println("No records found to deduplicate for '$name'")
            }
        }
    } catch (e: Exception) {
        println("Error deduplicating name '$name': ${e.message}")
        e.printStackTrace()
    }
}


fun deduplicateAllPlayers(serverName: String = "Server") {
    thread(name = "deduplicate-thread") {
        println("[$serverName] 🔍 Starting async deduplication...")

        try {
            Database.dataSource.connection.use { conn ->
                val duplicateNames = mutableListOf<String>()
                conn.prepareStatement(
                    """
                    SELECT player_name
                    FROM player_stats
                    GROUP BY player_name
                    HAVING COUNT(*) > 1
                """.trimIndent()
                ).use { stmt ->
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            duplicateNames.add(rs.getString("player_name"))
                        }
                    }
                }

                println("[$serverName] Found ${duplicateNames.size} duplicated name(s)")

                for (name in duplicateNames) {
                    deduplicateByName(name)
                }

                println("[$serverName] Deduplication complete")
            }
        } catch (e: Exception) {
            println("[$serverName] Error during async deduplication: ${e.message}")
            e.printStackTrace()
        }
    }
}


fun syncOrInsertPlayer(uuid: UUID, name: String) {
    thread(name = "sync-player-$uuid") {
        val defaultElo = 1000
        val defaultTier = getTierName(defaultElo)

        val stats = getPlayerStats(uuid)
        if (stats == null) {
            println("Sync insert: inserting new player $name ($uuid)")
            updatePlayerStats(PlayerStats(uuid, name, defaultElo, defaultTier, battlesWon = 0, battlesTotal = 0, winStreak = 0, longestWinStreak = 0))
        } else if (stats.name != name) {
            println("Sync insert: updating name for $uuid to $name")
            updatePlayerStats(stats.copy(name = name))
        }
    }
}


fun syncAllOnlinePlayers(server: MinecraftServer) {
    thread(name = "sync-players-thread") {
        println("Starting batch player sync...")

        val defaultElo = 1000

        server.playerManager.playerList.forEach { player ->
            val uuid = player.uuid
            val name = player.name.string
            val existing = getPlayerStats(uuid)

            if (existing == null) {
                println("Batch sync: inserting new player $name ($uuid)")
                updatePlayerStats(
                    PlayerStats(
                        uuid,
                        name,
                        defaultElo,
                        getTierName(defaultElo),
                        battlesWon = 0,
                        battlesTotal = 0,
                        winStreak = 0,
                        longestWinStreak = 0
                    )
                )
            } else if (existing.name != name) {
                println("Batch sync: updating name for $uuid to $name")
                updatePlayerStats(existing.copy(name = name))
            }
        }

        println("Batch player sync complete.")
    }
}

fun getFriendlyBattle(uuid: UUID): Boolean {
    val query = "SELECT friendly_battle FROM player_stats WHERE player_id = ?"
    try {
        Database.dataSource.connection.use { conn ->
            conn.prepareStatement(query).use { stmt ->
                stmt.setObject(1, uuid)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.getBoolean("friendly_battle") else false
                }
            }
        }
    } catch (e: Exception) {
        println("Error retrieving friendly battle setting for $uuid: ${e.message}")
        e.printStackTrace()
        return false
    }
}

fun setFriendlyBattle(uuid: UUID, value: Boolean) {
    val update = "UPDATE player_stats SET friendly_battle = ? WHERE player_id = ?"
    try {
        Database.dataSource.connection.use { conn ->
            conn.prepareStatement(update).use { stmt ->
                stmt.setBoolean(1, value)
                stmt.setObject(2, uuid)
                stmt.executeUpdate()
            }
        }
    } catch (e: Exception) {
        println("Error setting friendly battle setting for $uuid to $value: ${e.message}")
        e.printStackTrace()
    }
}

fun givePlaytestRewardIfEligible(player: ServerPlayerEntity) {
    val uuid = player.uuid

    try {
        Database.dataSource.connection.use { conn ->
            val stmt = conn.prepareStatement("SELECT reward_granted FROM playtest_rewards WHERE player_id = ?")
            stmt.setObject(1, uuid)
            val rs = stmt.executeQuery()

            if (rs.next()) {
                val granted = rs.getBoolean("reward_granted")
                if (!granted) {

                    val updateStmt = conn.prepareStatement("UPDATE playtest_rewards SET reward_granted = TRUE WHERE player_id = ?")
                    updateStmt.setObject(1, uuid)
                    updateStmt.executeUpdate()
                    updateStmt.close()

                    player.inventory.insertStack(ItemStack(CobblemonItems.RARE_CANDY, 25))
                    player.inventory.insertStack(ItemStack(UtilityItems.COBBLEMAX, 1))
                    player.sendMessage(Text.literal("§aYou received your playtester reward!"))
                } else {

                }
            } else {

                val insertStmt = conn.prepareStatement(
                    "INSERT INTO playtest_rewards (player_id, reward_granted) VALUES (?, TRUE)"
                )
                insertStmt.setObject(1, uuid)
                insertStmt.executeUpdate()
                insertStmt.close()

                player.inventory.insertStack(ItemStack(CobblemonItems.RARE_CANDY, 25))
                player.inventory.insertStack(ItemStack(UtilityItems.COBBLEMAX, 1))
                player.sendMessage(Text.literal("§aYou received your playtester reward!"))
            }

            rs.close()
            stmt.close()
        }
    } catch (e: Exception) {
        println("Failed to check or grant playtest reward to ${player.name.string}: ${e.message}")
        e.printStackTrace()
    }
}

fun saveCobbledollarsToDatabase(uuid: UUID, amount: BigInteger) {
    Database.dataSource.connection.use { conn ->
        conn.prepareStatement("UPDATE player_stats SET cobbledollars_balance = ? WHERE player_id = ?").use { stmt ->
            val safeAmount = if (amount.bitLength() < 63) {
                amount.toLong()
            } else {
                println("CobbleDollars overflow! Truncating $amount to max Long.")
                Long.MAX_VALUE
            }

            stmt.setLong(1, safeAmount)
            stmt.setObject(2, uuid)
            stmt.executeUpdate()
        }
    }
}

fun loadCobbledollarsFromDatabase(uuid: UUID): BigInteger {
    Database.dataSource.connection.use { conn ->
        conn.prepareStatement("SELECT cobbledollars_balance FROM player_stats WHERE player_id = ?").use { stmt ->
            stmt.setObject(1, uuid)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                return try {
                    BigInteger.valueOf(rs.getLong("cobbledollars_balance"))
                } catch (e: Exception) {
                    println("Failed to load CobbleDollars for $uuid, defaulting to 0")
                    BigInteger.ZERO
                }
            }
        }
    }
    return BigInteger.ZERO
}
