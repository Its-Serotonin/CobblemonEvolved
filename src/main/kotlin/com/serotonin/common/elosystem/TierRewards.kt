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
import net.gensir.cobgyms.CobGyms
import net.gensir.cobgyms.fabric.CobGymsFabric
import net.gensir.cobgyms.item.custom.GymCacheItem
import net.gensir.cobgyms.item.custom.GymKeyItem
import net.gensir.cobgyms.util.GymUtils
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.util.UUID
import net.gensir.cobgyms.registry.ModItemRegistry

data class TierReward(
    val id: String,
    val displayName: String,
    val iconTexture: Identifier,
    val rewardItemsSupplier: () -> List<ItemStack>
) {
    fun getRewardItems(): List<ItemStack> = rewardItemsSupplier()
}


var allTierRewards: Map<String, TierReward> = emptyMap()
    private set

fun initializeTierRewards() {
    if (allTierRewards.isNotEmpty()) return

    allTierRewards = mapOf(



        "ultra_beast" to TierReward(
            id = "ultra_beast",
            displayName = "Ultra Beast Tier",
            iconTexture = Identifier.of("cobblemonevolved", "textures/gui/competitivehandbook/tier_ultra_beast.png"),
            rewardItemsSupplier = {
                listOf(
                    ItemStack(CobblemonItems.BEAST_BALL, 10),
                    ItemStack(Items.NETHERITE_BLOCK, 1),
                    ItemStack(CobblemonItems.QUICK_BALL, 25),
                    ItemStack(CobblemonItems.MAX_REVIVE, 10),
                    ItemStack(CobblemonItems.RARE_CANDY, 10),
                    ItemStack(UtilityItems.COBBLEMAX, 1),
                    ItemStack(CobblemonItems.HELIX_FOSSIL, 1),
                    buildMoveItemSafe("overheat", preferTR = true),
                    buildMoveItemSafe("moonblast", preferTR = true),
                    buildMoveItemSafe("dracometeor", preferTR = true),
                    ItemStack(ModItemRegistry.LEGENDARY_SHARD.get(), 1),
                    ItemStack(CobblemonItems.ABILITY_PATCH, 2),

                    ItemStack(UtilityItems.HPSILVERCAP, 5),
                    ItemStack(UtilityItems.SPEEDSILVERCAP, 4),
                    ItemStack(UtilityItems.SPATKSILVERCAP, 5),
                    ItemStack(UtilityItems.SPDEFSILVERCAP, 5),
                    ItemStack(UtilityItems.ATKSILVERCAP, 4),
                    ItemStack(UtilityItems.DEFSILVERCAP, 5),

                    ItemStack(UtilityItems.GOLDENCAP, 3),

                    ItemStack(CobblemonItems.HP_UP, 10),
                    ItemStack(CobblemonItems.PROTEIN, 10),
                    ItemStack(CobblemonItems.IRON, 10),
                    ItemStack(CobblemonItems.CALCIUM, 10),
                    ItemStack(CobblemonItems.ZINC, 10),
                    ItemStack(CobblemonItems.CARBOS, 10),
                    ItemStack(CobblemonItems.WHITE_MINT_SEEDS, 1)
                )
            }
        ),

        "mythical" to TierReward(
            id = "mythical",
            displayName = "Mythical Tier",
            iconTexture = Identifier.of("cobblemonevolved", "textures/gui/competitivehandbook/tier_mythical.png"),
            rewardItemsSupplier = {
                listOf(
                    ItemStack(CobblemonItems.ULTRA_BALL, 35),
                    ItemStack(CobblemonItems.QUICK_BALL, 25),
                    ItemStack(Items.EMERALD, 35),
                    buildMoveItemSafe("suckerpunch", preferTR = false),
                    buildMoveItemSafe("helpinghand", preferTR = false),
                    buildMoveItemSafe("overheat", preferTR = true),
                    buildMoveItemSafe("moonblast", preferTR = true),
                    buildMoveItemSafe("dracometeor", preferTR = true),

                    ItemStack(UtilityItems.SPDEFSILVERCAP, 3),
                    ItemStack(UtilityItems.SPEEDSILVERCAP, 3),
                    ItemStack(UtilityItems.ATKSILVERCAP, 3),
                    ItemStack(UtilityItems.DEFSILVERCAP, 2),
                    ItemStack(UtilityItems.HPSILVERCAP, 2),
                    ItemStack(UtilityItems.SPATKSILVERCAP, 2),

                    ItemStack(UtilityItems.GOLDENCAP, 2),
                    ItemStack(ModItemRegistry.LEGENDARY_SHARD.get(), 2),
                    ItemStack(CobblemonItems.PINK_MINT_SEEDS, 1),
                    ItemStack(CobblemonItems.CHOICE_SCARF, 1)
                )
            }
        ),


        "legendary" to TierReward(
            id = "legendary",
            displayName = "Legendary Tier",
            iconTexture = Identifier.of("cobblemonevolved", "textures/gui/competitivehandbook/tier_legendary.png"),
            rewardItemsSupplier = {
                listOf(
                    ItemStack(CobblemonItems.ULTRA_BALL, 30),
                    ItemStack(CobblemonItems.QUICK_BALL, 15),
                    ItemStack(Items.EMERALD, 30),
                    buildMoveItemSafe("protect", preferTR = false),
                    buildMoveItemSafe("fakeout", preferTR = false),

                    ItemStack(UtilityItems.DEFSILVERCAP, 2),
                    ItemStack(UtilityItems.SPEEDSILVERCAP, 2),
                    ItemStack(UtilityItems.HPSILVERCAP, 2),
                    ItemStack(UtilityItems.SPATKSILVERCAP, 2),
                    ItemStack(UtilityItems.SPDEFSILVERCAP, 1),
                    ItemStack(UtilityItems.ATKSILVERCAP, 1),

                    ItemStack(UtilityItems.GOLDENCAP, 1),
                    ItemStack(ModItemRegistry.LEGENDARY_SHARD.get(), 2),
                    ItemStack(CobblemonItems.CYAN_MINT_SEEDS, 1),
                    ItemStack(CobblemonItems.CHOICE_SCARF, 1),
                    ItemStack(CobblemonItems.CALCIUM, 5),
                    ItemStack(CobblemonItems.ZINC, 5)
                )
            }
        ),


        "pseudo_legendary" to TierReward(
            id = "pseudo_legendary",
            displayName = "Pseudo Legendary Tier",
            iconTexture = Identifier.of("cobblemonevolved", "textures/gui/competitivehandbook/tier_pseudo_legendary.png"),
            rewardItemsSupplier = {
                listOf(
                    ItemStack(CobblemonItems.ULTRA_BALL, 25),
                    ItemStack(CobblemonItems.QUICK_BALL, 10),
                    ItemStack(Items.EMERALD, 25),
                    buildMoveItemSafe("fakeout", preferTR = true),
                    ItemStack(UtilityItems.HPSILVERCAP, 1),
                    ItemStack(UtilityItems.SPEEDSILVERCAP, 1),
                    ItemStack(UtilityItems.SPATKSILVERCAP, 1),
                    ItemStack(UtilityItems.SPDEFSILVERCAP, 1),
                    ItemStack(UtilityItems.ATKSILVERCAP, 1),
                    ItemStack(ModItemRegistry.LEGENDARY_SHARD.get(), 1),
                    ItemStack(CobblemonItems.BLUE_MINT_SEEDS, 1),
                    ItemStack(CobblemonItems.LEFTOVERS, 1),
                    ItemStack(CobblemonItems.PROTEIN, 5),
                    ItemStack(CobblemonItems.IRON, 5)
                )
            }
        ),

        "master_ball" to TierReward(
            id = "master_ball",
            displayName = "Master Ball Tier",
            iconTexture = Identifier.of("cobblemonevolved", "textures/gui/competitivehandbook/tier_master_ball.png"),
            rewardItemsSupplier = {
                listOf(
                    ItemStack(CobblemonItems.MASTER_BALL, 1),
                    ItemStack(CobblemonItems.ULTRA_BALL, 20),
                    ItemStack(Items.EMERALD, 20),
                    buildProtectItemSafe(),
                    ItemStack(UtilityItems.ATKSILVERCAP, 1),
                    ItemStack(UtilityItems.DEFSILVERCAP, 1),
                    ItemStack(CobblemonItems.RED_MINT_SEEDS, 1),
                    ItemStack(CobblemonItems.ASSAULT_VEST, 1)
                )
            }
        ),

        "ultra_ball" to TierReward(
            id = "ultra_ball",
            displayName = "Ultra Ball Tier",
            iconTexture = Identifier.of("cobblemonevolved", "textures/gui/competitivehandbook/tier_ultra_ball.png"),
            rewardItemsSupplier = {
                listOf(
                    ItemStack(CobblemonItems.ULTRA_BALL, 15),
                    ItemStack(Items.EMERALD, 10),
                    ItemStack(CobblemonItems.HYPER_POTION, 3),
                    ItemStack(CobblemonItems.SITRUS_BERRY, 1),
                    ItemStack(CobblemonItems.FOCUS_SASH, 1),
                    ItemStack(CobblemonItems.GREEN_MINT_SEEDS, 1)
                )
            }
        ),

        "great_ball" to TierReward(
            id = "great_ball",
            displayName = "Great Ball Tier",
            iconTexture = Identifier.of("cobblemonevolved", "textures/gui/competitivehandbook/tier_great_ball.png"),
            rewardItemsSupplier = {
                listOf(
                    ItemStack(CobblemonItems.GREAT_BALL, 15),
                    ItemStack(CobblemonItems.SUPER_POTION, 2),
                    ItemStack(CobblemonItems.ORAN_BERRY, 5),
                    ItemStack(CobblemonItems.REVIVE, 1)
                )
            }
        ),

        "poke_ball" to TierReward(
            id = "poke_ball",
            displayName = "Poké Ball Tier",
            iconTexture = Identifier.of("cobblemonevolved", "textures/gui/competitivehandbook/tier_poke_ball.png"),
            rewardItemsSupplier = {
                listOf(
                    ItemStack(CobblemonItems.POKE_BALL, 10),
                    ItemStack(CobblemonItems.POTION, 1)
                )
            }
        ),
    )
}
val claimedTiers: MutableMap<UUID, MutableSet<String>> = mutableMapOf()






fun loadClaimedTiers(uuid: UUID): Set<String> {
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

fun saveClaimedTier(uuid: UUID, tier: String) {
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

fun hasClaimedTier(uuid: UUID, tier: String): Boolean {
    return claimedTiers[uuid]?.contains(tier) ?: false
}

fun resetClaimedTiers(uuid: UUID) {
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

fun buildMoveItemSafe(moveName: String, preferTR: Boolean = true): ItemStack {
    return try {
        val move = getMoveByName(moveName)
        if (!SimpleTMsItems.hasItemForMove(move.template, preferTR)) {
            println("⚠️ No ${if (preferTR) "TR" else "TM"} found for $moveName")
            ItemStack(Items.BARRIER)
        } else {
            ItemStack(SimpleTMsItems.getTMorTRItemFromMove(move, preferTR), 1)
        }
    } catch (e: Exception) {
        println("⚠️ buildMoveItemSafe($moveName) failed: ${e.message}")
        ItemStack(Items.BARRIER)
    }
}


fun buildProtectItemSafe(): ItemStack {
    return try {
        buildProtectItem() // the real one, only safe on server
    } catch (e: Exception) {
        println("⚠️ buildProtectItemSafe failed: ${e.message}")
        ItemStack(Items.BARRIER) // some placeholder or nothing
    }
}
fun buildProtectItem(): ItemStack {

    val move = getMoveByName("protect")
    val isTR = SimpleTMsItems.hasItemForMove(move.template, true)
    val isTM = SimpleTMsItems.hasItemForMove(move.template, false)

    return when {
        isTR -> {
            println("Using TR for protect")
            ItemStack(SimpleTMsItems.getTMorTRItemFromMove(move, true), 1)
        }
        isTM -> {
            println("Using TM for protect")
            ItemStack(SimpleTMsItems.getTMorTRItemFromMove(move, false), 1)
        }
        else -> error("No TM or TR found for protect — configuration or registration issue")
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

