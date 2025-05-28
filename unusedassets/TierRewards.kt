package com.serotonin.common.elosystem

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.api.moves.Move
import com.cobblemon.mod.common.api.moves.MoveTemplate
import com.cobblemon.mod.common.api.moves.Moves
import com.cobblemon.mod.relocations.oracle.truffle.api.instrumentation.Tag
import com.gmail.brendonlf.cobblemon_utility.Item.UtilityItems
import com.serotonin.common.networking.Database
import dragomordor.simpletms.SimpleTMsItems
import dragomordor.simpletms.fabric.SimpleTMs
import dragomordor.simpletms.item.SimpleTMsItem
import dragomordor.simpletms.item.group.SimpleTMsItemGroups
import dragomordor.simpletms.util.MoveAssociations
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.util.UUID

data class TierReward(
    val id: String,
    val displayName: String,
    val iconTexture: Identifier,
    val rewardItems: List<ItemStack>
)



val allTierRewards: Map<String, TierReward> = mapOf(


    "master_ball" to TierReward(
        id = "master_ball",
        displayName = "Master Ball Tier",
        iconTexture = Identifier.of("cobblemonevolved", "textures/gui/competitivehandbook/tier_master_ball.png"),
        rewardItems = listOf(
            ItemStack(CobblemonItems.MASTER_BALL, 1),
            ItemStack(CobblemonItems.ULTRA_BALL, 20),
            ItemStack(Items.EMERALD, 20),
            buildProtectItem(),
            ItemStack(UtilityItems.WOODENCAP, 2),
            ItemStack(CobblemonItems.RED_MINT_SEEDS, 1),
            ItemStack(CobblemonItems.ASSAULT_VEST, 1)
        )
    ),

    "ultra_ball" to TierReward(
        id = "ultra_ball",
        displayName = "Ultra Ball Tier",
        iconTexture = Identifier.of("cobblemonevolved", "textures/gui/competitivehandbook/tier_ultra_ball.png"),
        rewardItems = listOf(
            ItemStack(CobblemonItems.ULTRA_BALL, 15),
            ItemStack(Items.EMERALD, 10),
            ItemStack(CobblemonItems.HYPER_POTION, 3),
            ItemStack(CobblemonItems.SITRUS_BERRY, 1),
            ItemStack(CobblemonItems.FOCUS_SASH, 1),
            ItemStack(CobblemonItems.GREEN_MINT_SEEDS, 1)
        )
    ),

"great_ball" to TierReward(
id = "great_ball",
displayName = "Great Ball Tier",
iconTexture = Identifier.of("cobblemonevolved", "textures/gui/competitivehandbook/tier_great_ball.png"),
rewardItems = listOf(
    ItemStack(CobblemonItems.GREAT_BALL, 15),
    ItemStack(CobblemonItems.SUPER_POTION, 2),
    ItemStack(CobblemonItems.ORAN_BERRY, 5),
    ItemStack(CobblemonItems.REVIVE, 1)
)
),
    "poke_ball" to TierReward(
        id = "poke_ball",
        displayName = "Poké Ball Tier",
        iconTexture = Identifier.of("cobblemonevolved", "textures/gui/competitivehandbook/tier_poke_ball.png"),
        rewardItems = listOf(
            ItemStack(CobblemonItems.POKE_BALL, 10),
            ItemStack(CobblemonItems.POTION, 1)
        )
    ),
)

val claimedTiers: MutableMap<java.util.UUID, MutableSet<String>> = mutableMapOf()






fun loadClaimedTiers(uuid: java.util.UUID): Set<String> {
    try {
        Database.dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT claimed_tiers FROM player_stats WHERE player_id = ?").use { stmt ->
                stmt.setObject(1, uuid)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        val array = rs.getArray("claimed_tiers")?.array as? Array<*>
                        return array?.mapNotNull { it as? String }?.toSet() ?: emptySet()
                    }
                }
            }
        }
    } catch (e: Exception) {
        println("Failed to load claimed tiers for $uuid: ${e.message}")
        e.printStackTrace()
    }
    return emptySet()
}

fun saveClaimedTier(uuid: java.util.UUID, tier: String) {
    val current = claimedTiers.getOrPut(uuid) { loadClaimedTiers(uuid).toMutableSet() }
    if (!current.add(tier)) return

    try {
        Database.dataSource.connection.use { conn ->
            val sqlArray = conn.createArrayOf("text", current.toTypedArray())
            conn.prepareStatement("UPDATE player_stats SET claimed_tiers = ? WHERE player_id = ?").use { stmt ->
                stmt.setArray(1, sqlArray)
                stmt.setObject(2, uuid)
                stmt.executeUpdate()
            }
        }
    } catch (e: Exception) {
        println("Failed to save claimed tier for $uuid: ${e.message}")
        e.printStackTrace()
    }
}

fun hasClaimedTier(uuid: java.util.UUID, tier: String): Boolean {
    return claimedTiers[uuid]?.contains(tier) ?: false
}

fun resetClaimedTiers(uuid: java.util.UUID) {
    claimedTiers.remove(uuid)
    try {
        Database.dataSource.connection.use { conn ->
            conn.prepareStatement("UPDATE player_stats SET claimed_tiers = '[]' WHERE player_id = ?").use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.executeUpdate()
            }
        }
    } catch (e: Exception) {
        println("Failed to reset claimed tiers for $uuid: ${e.message}")
        e.printStackTrace()
    }
}

fun resetAllClaimedTiers() {
    claimedTiers.clear()

    try {
        Database.dataSource.connection.use { conn ->
            conn.prepareStatement(

                "UPDATE player_stats SET claimed_tiers = '{}'::text[]"
            ).use { stmt ->
                val updated = stmt.executeUpdate()
                println("Reset claimed_tiers for $updated players in the database.")
            }
        }
    } catch (e: Exception) {
        println("Failed to reset all claimed tiers: ${e.message}")
        e.printStackTrace()
    }
}

fun buildProtectItem(): ItemStack {
    println("🛠️ buildProtectItem CALLED")

    return try {
        val move = getMoveByName("protect")
        val isTR = SimpleTMsItems.hasItemForMove(move.template, true)
        val isTM = SimpleTMsItems.hasItemForMove(move.template, false)

        when {
            isTR -> {
                println("✅ Using TR for protect")
                ItemStack(SimpleTMsItems.getTMorTRItemFromMove(move, true), 1)
            }
            isTM -> {
                println("✅ Using TM for protect")
                ItemStack(SimpleTMsItems.getTMorTRItemFromMove(move, false), 1)
            }
            else -> {
                println("❌ No TM/TR item found for protect — using emerald")
                ItemStack(Items.EMERALD, 1)
            }
        }
    } catch (e: Exception) {
        println("❌ Exception in buildProtectItem: ${e.message}")
        ItemStack(Items.EMERALD, 1)
    }
}

fun getMoveByName(name: String): Move {
    val template = Moves.getByName(name)
    if (template == null) {
        println("MoveTemplate NOT FOUND for: $name")
        throw IllegalStateException("MoveTemplate not found for: $name")
    }

    println("MoveTemplate FOUND for: $name")
    return Move(template, template.maxPp)
}

fun logAllMoves() {
    println("All available move names:")
    Moves.names().sorted().forEach { println(" - $it") }

}

