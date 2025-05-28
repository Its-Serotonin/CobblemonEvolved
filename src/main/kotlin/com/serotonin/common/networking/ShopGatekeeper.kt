package com.serotonin.common.networking

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.MinecraftClient
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import java.util.UUID

object ShopGatekeeper {

    val lastRequestTimes = mutableMapOf<UUID, Long>()
    private const val COOLDOWN_MS = 2000

    fun canPurchaseFromCategory(
        player: ServerPlayerEntity,
        categoryRequiredTier: Int,
        showMessage: Boolean = true
    ): Boolean {
        val playerTier = getTierLevelFromDatabase(player)
        val allowed = playerTier >= categoryRequiredTier

        if (!allowed && showMessage) {
            player.sendMessage(Text.literal("§cYou must be ${getTierNameFromLevel(categoryRequiredTier)} or higher to buy from this category."), false)
        }

        return allowed
    }

    private fun getTierLevelFromDatabase(player: ServerPlayerEntity): Int {
        val uuid = player.uuid
        Database.dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT tier FROM player_stats WHERE player_id = ?").use { stmt ->
                stmt.setObject(1, uuid)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        return tierNameToLevel(rs.getString("tier"))
                    }
                }
            }
        }
        return 0
    }

    private fun tierNameToLevel(tierName: String): Int {
        return when (tierName.lowercase()) {
            "poke ball", "§cpoke §fball" -> 0
            "great ball", "§9great §fball" -> 1
            "ultra ball", "§eultra §8ball" -> 2
            "master ball", "§5§lmaster §f§lball" -> 3
            "psuedo legendary", "§6§lpsuedo §a§llegendary" -> 4
            "legendary", "§b§llegendary" -> 5
            "mythical", "§2§kA§d§l§omythical§2§kA" -> 6
            "ultra beast", "§e§o§kaa§e§l§oultra §b§l§obeast§kaa" -> 7
            else -> 0
        }
    }

    private fun getTierNameFromLevel(level: Int): String {
        return when (level) {
            0 -> "Poké Ball"
            1 -> "Great Ball"
            2 -> "Ultra Ball"
            3 -> "Master Ball"
            4 -> "Psuedo Legendary"
            5 -> "Legendary"
            6 -> "Mythical"
            7 -> "Ultra Beast"
            else -> "Unknown"
        }
    }
    @JvmStatic
    fun getTierLevelFromElo(elo: Int): Int {
        return when {
            elo >= 4500 -> 7 // Ultra Beast
            elo >= 4000 -> 6 // Mythical
            elo >= 3500 -> 5 // Legendary
            elo >= 3000 -> 4 // Psuedo Legendary
            elo >= 2500 -> 3 // Master Ball
            elo >= 2000 -> 2 // Ultra Ball
            elo >= 1500 -> 1 // Great Ball
            else -> 0         // Poké Ball
        }
    }


    @JvmStatic
    fun requestEloUpdate(silent: Boolean = false) {
        val player = MinecraftClient.getInstance().player ?: return
        val uuid = player.uuid

        val now = System.currentTimeMillis()
        val lastTime = lastRequestTimes[uuid] ?: 0

        if (now - lastTime < COOLDOWN_MS) {
            println("[$uuid] Skipping Elo update")
            return
        }

        lastRequestTimes[uuid] = now

        val json = buildJsonObject {
            put("type", "get_elo")
            put("uuid", uuid.toString())
            put("silent", silent)
        }.toString()

        ClientPlayNetworking.send(RawJsonPayload(json))
    }

    @JvmStatic
    fun requestEloUpdateSilent() {
        requestEloUpdate(true)
    }
}
