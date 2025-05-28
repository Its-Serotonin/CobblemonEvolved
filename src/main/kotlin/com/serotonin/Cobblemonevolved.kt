package com.serotonin


//import com.serotonin.common.elosystem.removeLeaderboardCommand
//import com.serotonin.common.elosystem.setLeaderboardCommand
//import com.serotonin.common.elosystem.ShowRankOnUsername //old scoreboard method
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.storage.party.PartyPosition
import com.serotonin.common.api.events.EloManager
import com.serotonin.common.api.events.EloManager.playerElos
import com.serotonin.common.api.events.EloManager.requestElo
import com.serotonin.common.chat.ChatModRegister
import com.serotonin.common.chat.ChatRateLimiter
import com.serotonin.common.elosystem.*
import com.serotonin.common.entities.LeaderboardArmorStandEntity
import com.serotonin.common.item.ModItemGroups
import com.serotonin.common.item.ModItems
import com.serotonin.common.item.ModLootConditions
import com.serotonin.common.networking.*
import com.serotonin.common.networking.ShopGatekeeper.lastRequestTimes
import com.serotonin.common.registries.*
import com.serotonin.common.saveslots.*
import com.serotonin.common.tourneys.CachedFormatSuggestions
import com.serotonin.common.tourneys.TournamentManager
import dev.emi.trinkets.api.TrinketsApi
import fr.harmex.cobbledollars.common.utils.extensions.getCobbleDollars
import fr.harmex.cobbledollars.common.utils.extensions.setCobbleDollars
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.world.World
import org.slf4j.LoggerFactory
import java.util.*

object Cobblemonevolved : ModInitializer {
    const val MOD_ID: String = "cobblemonevolved"
    private val logger = LoggerFactory.getLogger(MOD_ID)
    private val pendingRevalidations = mutableMapOf<UUID, Int>()
    private val lastRevalidated = mutableMapOf<UUID, Long>()
    private const val REVALIDATION_COOLDOWN_MS = 5000L

    override fun onInitialize() {


        PlayerDataSyncNetworking.registerPayloads()
        println("networking goin up!")


        SoundRegistry.initSounds()
        ModItems.registerCEModItems()
        ModItemGroups.registerCEItemGroups()
        CommandRegister.rankCmds()
        RankedBattleEvents.registerBattleEvents()
        ServerContext.registerServerLifecycleHooks()
        LeaderboardManager.initialize()
        EntityRegister.registerEntities()
        ChatModRegister.registerChatModeration()
        SaveSlotAutoBackup.register()
        SaveSlotAutoSaver.register()
        ModLootConditions.register()

        RegisterSerializer.registerFixedBackpackDyeSerializer()

        CustomVendorRegistry.register()
        registerBeadsOfRuinKeyItem()


        /*
                Registry.register(
                    Registries.RECIPE_SERIALIZER,
                    Identifier.of("cobblemonevolved", "fixed_backpack_dye"),
                    FIXED_BACKPACK_DYE_SERIALIZER
                )

                if (FabricLoader.getInstance().isModLoaded("sophisticatedbackpacks")) {
                    val dyeRecipeId = Identifier.of("sophisticatedbackpacks", "backpack_dye")
                    Registry.register(
                        Registries.RECIPE_SERIALIZER,
                        dyeRecipeId,
                        FIXED_BACKPACK_DYE_SERIALIZER
                    )
                    println("Overrode backpack dye serializer")
                } else {
                    println("Skipped backpack dye override — mod not loaded")
                }

         */




        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            TournamentManager.registerCommands(dispatcher)
        }

        //CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
        //	setLeaderboardCommand(dispatcher)  // Register the command
        //}

        /*
        BATTLE_VICTORY.subscribe(Priority.NORMAL) { event ->
            val server = ServerContext.server
            if (server != null) {
                EloManager.handleBattleVictoryEvent(event, server)
            } else {
                println("Cannot handle battle victory: server is null")
            }
        }*/
        /*
        RankedMatchVictoryEvent.RMVictoryEvent.EVENT.register(RankedMatchVictoryEvent.RMVictoryEvent { event ->
            println("BATTLE_VICTORY event triggered")
            val server = ServerContext.server
            //val uuids = (event.winners + event.losers).map { it.uuid }.distinct()
            if (server != null) {

                    event.updateEloForAll(server)

            } else {
                println("Cannot update Elo: server instance not available")
            }
        })
*/

        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            ServerContext.server = server

            ModConfig.load(server)


            if (ModConfig.jdbcUrl.isBlank() || ModConfig.username.isBlank() || ModConfig.password.isBlank()) {
                println("Config not filled in. Please check database.properties.")
                return@register
            }

            val world = server.getWorld(World.OVERWORLD) ?: return@register

            val existing = world.iterateEntities()
                .filterIsInstance<LeaderboardArmorStandEntity>()
                .filter { it.commandTags.contains("RANKLEADERBOARD") }

            existing.forEach { it.remove(Entity.RemovalReason.DISCARDED) }
            println("Cleared ${existing.size} lingering leaderboard entities on startup")

            startPeriodicLeaderboardUpdate(server)
            println("Debug: leaderboard updating!")

            PlayerRankTagsRegister.registerRankTags()
            println("debug: made player tags")

            RankedBattleEvents.initializeCleanupTask(server)
            println("debug: made battle cleanup task")

            CachedFormatSuggestions.refresh()
            SaveSlotBackupManager.cleanupInactivePlayers()

            LobbyVendorDamagePrevention.preventDamage()
            initializeTierRewards()
            println("Server started — backing up active save slots")
            val dao = SaveSlotDAOImpl(Database.dataSource)
            server.playerManager.playerList.forEach { player ->
                val uuid = player.uuid
                ActiveSlotTracker.getSlot(uuid)?.let { slot ->
                    dao.loadSlot(uuid, slot)?.let { SaveSlotBackupManager.backupSlot(it) }
                }
            }
        }
        //dont need this during development, probably will turn it on for the final build
        //syncAllOnlinePlayers(server)


        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            try {
                val player = handler.player
                val uuid = player.uuid
                val name = player.name.string
                val dao = SaveSlotDAOImpl(Database.dataSource)

                syncOrInsertPlayer(uuid, name)
                ChatRateLimiter.markJoin(player.uuid)

                pendingNameTagUpdates[uuid] = player
                println("Added $name to pendingNameTagUpdates")

                requestElo(uuid, player, silent = true) { /* no op */ }

                FriendlyBattleManager.preloadFromDatabase(uuid, name)
                claimedTiers[uuid] = loadClaimedTiers(uuid).toMutableSet()

                givePlaytestRewardIfEligible(player)

                val cobbleDollars = loadCobbledollarsFromDatabase(uuid)
                player.setCobbleDollars(cobbleDollars)
                println("Loaded $$cobbleDollars for $name")


                val claimed = claimedTiers[uuid]?.toList() ?: emptyList()
                ServerPlayNetworking.send(player, ClaimedTiersPayload(uuid.toString(), claimed))

                for (slot in 1..3) {
                    val cleaned = SaveSlotBackupManager.cleanupOldBackups(uuid, slot)
                    if (cleaned > 0) {
                        println("Cleaned $cleaned old backups for ${player.name.string} slot $slot")
                    }
                }


                ServerPlayNetworking.send(player, RawJsonPayload(buildJsonObject {
                    put("type", "get_tournament_signup_status")
                }.toString()))

                TournamentManager.getActiveTournament()?.let { tournament ->
                    ServerPlayNetworking.send(player, RawJsonPayload(buildJsonObject {
                        put("type", "tournament_info")
                        put("ruleset", tournament.ruleset)
                        put("startTime", TournamentManager.formatDate())
                        put("status", TournamentManager.timeUntil())
                    }.toString()))
                }



                val slots = dao.getAllSlots(uuid)
                val activeSlot = dao.loadActiveSlot(uuid)

                val dbActiveSlot = dao.loadActiveSlot(uuid)
                val memorySlot = ActiveSlotTracker.getSlot(uuid)
                ActiveSlotTracker.clear(uuid)
                val slotToLoad = dbActiveSlot ?: slots.maxByOrNull { it.lastSaved }?.slot

                if (dbActiveSlot == null && slots.all { it.isMeaningless() }) {
                    println("Skipping slot load for $name: no active or meaningful slots")


                    val slotJson = buildJsonObject {
                        put("type", "save_slots_response")
                        put("slots", Json.encodeToJsonElement(slots.map {
                            buildJsonObject {
                                put("slot", it.slot)
                                put("lastSaved", it.lastSaved)
                            }
                        }))
                    }.toString()
                    ServerPlayNetworking.send(player, RawJsonPayload(slotJson))

                    return@register
                }



                if (dbActiveSlot != memorySlot) {
                    println("Active slot mismatch on join for $name: memory=$memorySlot, db=$dbActiveSlot")
                    ActiveSlotTracker.setSlot(uuid, dbActiveSlot ?: -1)
                }

                if (slotToLoad != null) {
                    val data = dao.loadSlot(uuid, slotToLoad)

                    if (data != null) {
                        ActiveSlotTracker.setSlot(uuid, slotToLoad)

                        player.inventory.clear()
                        Cobblemon.storage.getParty(player).apply { repeat(6) { remove(PartyPosition(it)) } }
                        Cobblemon.storage.getPC(player).clearAll()

                        try {
                            deserializeInventory(player, data.inventoryData)
                            deserializeParty(player, data.pokemonData)
                            deserializePC(player, data.pcData)
                            deserializeBackpack(player, data.backpackData)
                            deserializeTrinkets(player, data.trinketData)
                            println("Loaded slot $slotToLoad for $name (from ${if (activeSlot != null) "active" else "fallback"})")
                        } catch (e: Exception) {
                            println("Failed to load save slot $slotToLoad for $name: ${e.message}")
                            e.printStackTrace()
                        }

                        ServerPlayNetworking.send(player, ActiveSlotUpdatePayload(slotToLoad))
                        val slots = dao.getAllSlots(uuid)
                        val json = buildJsonObject {
                            put("type", "save_slots_response")
                            put("slots", Json.encodeToJsonElement(slots.map {
                                buildJsonObject {
                                    put("slot", it.slot)
                                    put("lastSaved", it.lastSaved)
                                }
                            }))
                        }.toString()
                        ServerPlayNetworking.send(player, RawJsonPayload(json))

                    } else {
                        val backup = SaveSlotBackupManager.loadLatestBackup(uuid, slotToLoad)
                        if (backup != null) {
                            println("Loaded backup for $name slot $slotToLoad")

                            ActiveSlotTracker.setSlot(uuid, slotToLoad)
                            dao.setActiveSlot(uuid, slotToLoad)

                            player.inventory.clear()
                            Cobblemon.storage.getParty(player).apply { repeat(6) { remove(PartyPosition(it)) } }
                            Cobblemon.storage.getPC(player).clearAll()

                            deserializeInventory(player, backup.inventory)
                            deserializeParty(player, backup.party)
                            deserializePC(player, backup.pc)
                            deserializeBackpack(player, backup.backpack)
                            deserializeTrinkets(player, backup.trinkets)

                            ServerPlayNetworking.send(player, ActiveSlotUpdatePayload(slotToLoad))
                        } else {
                            println("No save or backup found for $name slot $slotToLoad")
                        }
                    }


                    val slotJson = buildJsonObject {
                        put("type", "save_slots_response")
                        put("slots", Json.encodeToJsonElement(slots.map {
                            buildJsonObject {
                                put("slot", it.slot)
                                put("lastSaved", it.lastSaved)
                            }
                        }))
                    }.toString()
                    ServerPlayNetworking.send(player, RawJsonPayload(slotJson))

                }
            } catch (e: Exception) {
                println("failed JOIN handler: ${e.message}")
                e.printStackTrace()
            }
        }

        var statsTickCounter = 0
        var cleanupTickCounter = 0
        ServerTickEvents.START_SERVER_TICK.register { server ->
            statsTickCounter++
            cleanupTickCounter++

            if (statsTickCounter >= 6000) {
                EloManager.cleanupOldMatchStats()
                statsTickCounter = 0
            }

            if (cleanupTickCounter >= 20 * 60 * 10) {
                cleanupTickCounter = 0
                val online = server.playerManager.playerList.map { it.uuid }.toSet()
                ActiveSlotTracker.cleanupOfflinePlayers(online)
                SaveSlotCooldowns.cleanupOfflinePlayers(online)
                SaveSlotAutoSaver.cleanupOfflinePlayers(online)
                println("Cleaned up inactive player data")
            }
        }


        ServerLivingEntityEvents.AFTER_DEATH.register { entity, _ ->
            if (entity is ServerPlayerEntity) {
                entity.server.execute {
                    entity.server.execute {
                        try {

                            for (i in 0 until entity.inventory.size()) {
                                entity.inventory.setStack(i, ItemStack.EMPTY)
                            }


                            TrinketsApi.getTrinketComponent(entity).ifPresent { component ->
                                for ((_, slotMap) in component.inventory) {
                                    for ((_, inventory) in slotMap) {
                                        for (i in 0 until inventory.size()) {
                                            inventory.setStack(i, ItemStack.EMPTY)
                                        }
                                    }
                                }
                            }


                            val uuid = entity.uuid
                            val slot = ActiveSlotTracker.getSlot(uuid) ?: return@execute
                            val dao = SaveSlotDAOImpl(Database.dataSource)

                            val saveData = PlayerSaveSlot(
                                uuid = uuid,
                                slot = slot,
                                inventoryData = serializeInventory(entity),
                                pokemonData = serializeParty(entity),
                                pcData = serializePC(entity),
                                backpackData = serializeBackpack(entity, entity.server.registryManager),
                                trinketData = serializeTrinkets(entity),
                                lastSaved = System.currentTimeMillis()
                            )

                            dao.saveSlot(saveData)
                            println("Auto-saved slot $slot for ${entity.name.string} on death")
                            ChatRateLimiter.handleChatOrCommand(uuid, systemMessage = true)
                        } catch (e: Exception) {
                            println("Failed to auto-save on death: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
            }
        }



        ServerEntityEvents.ENTITY_LOAD.register { entity, _ ->
            if (entity is ServerPlayerEntity) {
                val player = entity
                val uuid = player.uuid


                player.passengerList.forEach { it.discard() }
                player.removeAllPassengers()

                player.serverWorld.iterateEntities()
                    .filter {
                        (it.commandTags.contains("RANKEDPLAYERNAMETAG") || it.commandTags.contains("RANKTAG")) &&
                                it.vehicle?.uuid == player.uuid
                    }
                    .forEach { it.discard() }

                if (player.serverWorld.registryKey == World.OVERWORLD) {
                    val cached = pendingNameTagUpdates[uuid]
                    if (cached != null) {
                        println("Re-spawning nametags for ${player.name.string} in Overworld")
                        val elo = playerElos[uuid] ?: 1000
                        val rankName = getTierName(elo)
                        updatePlayerNametag(player, elo, rankName)
                    }
                }


                val dao = SaveSlotDAOImpl(Database.dataSource)
                val activeSlot = ActiveSlotTracker.getSlot(uuid) ?: return@register


                val now = System.currentTimeMillis()
                val last = lastRevalidated[uuid] ?: 0L

                if (now - last < REVALIDATION_COOLDOWN_MS) {
                    println("Skipping revalidation for ${player.name.string}: cooldown active")
                    return@register
                }

                lastRevalidated[uuid] = now
                pendingRevalidations[uuid] = 1


                val slotData = dao.loadSlot(uuid, activeSlot)
                if (slotData == null || slotData.isMeaningless()) {
                    println("No valid save data found to revalidate slot $activeSlot for ${player.name.string}")
                } else {
                    println("Queued revalidation of slot $activeSlot for ${player.name.string} after dimension switch")
                }
            }
        }


        ServerTickEvents.END_SERVER_TICK.register { server ->
            val iterator = pendingRevalidations.iterator()

            while (iterator.hasNext()) {
                val (uuid, ticksLeft) = iterator.next()

                if (ticksLeft > 1) {
                    pendingRevalidations[uuid] = ticksLeft - 1
                } else {
                    val player = server.playerManager.getPlayer(uuid)
                    if (player == null) {
                        println("Skipped revalidation for $uuid: player not found")
                        iterator.remove()
                        continue
                    }

                    val dao = SaveSlotDAOImpl(Database.dataSource)
                    val activeSlot = ActiveSlotTracker.getSlot(uuid)
                    val slotData = dao.loadSlot(uuid, activeSlot ?: -1)

                    if (slotData != null && !slotData.isMeaningless()) {
                        println("Revalidating save slot $activeSlot for ${player.name.string}")

                        player.server.execute {
                            try {
                                player.inventory.clear()
                                Cobblemon.storage.getParty(player).apply { repeat(6) { remove(PartyPosition(it)) } }
                                Cobblemon.storage.getPC(player).clearAll()
                                clearEquippedBackpackAndTrinkets(player)

                                deserializeInventory(player, slotData.inventoryData)
                                deserializeParty(player, slotData.pokemonData)
                                deserializePC(player, slotData.pcData)
                                deserializeBackpack(player, slotData.backpackData)
                                deserializeTrinkets(player, slotData.trinketData)

                                println("Reloaded save slot $activeSlot for ${player.name.string}")
                            } catch (e: Exception) {
                                println("Failed to reload save slot for ${player.name.string}: ${e.message}")
                                e.printStackTrace()
                            }
                        }
                    }

                    iterator.remove()
                }
            }
        }


        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            val player = handler.player
            val uuid = player.uuid
            val name = player.name.string
            val dao = SaveSlotDAOImpl(Database.dataSource)

            player.passengerList.forEach { it.discard() }
            player.removeAllPassengers()
            lastRequestTimes.remove(player.uuid)

            AsyncExecutor.disconnectExecutor.submit {
                try {
                    val currentSlot = ActiveSlotTracker.getSlot(uuid)
                    if (currentSlot != null) {
                        val data = PlayerSaveSlot(
                            uuid = uuid,
                            slot = currentSlot,
                            inventoryData = serializeInventory(player),
                            pokemonData = serializeParty(player),
                            pcData = serializePC(player),
                            lastSaved = System.currentTimeMillis(),
                            backpackData = serializeBackpack(player, player.server.registryManager),
                            trinketData = serializeTrinkets(player)

                        )
                        dao.saveSlot(data)
                        dao.setActiveSlot(uuid, currentSlot)
                        SaveSlotAutoSaver.lastSaved[uuid] = System.currentTimeMillis()
                        println("Auto-saved slot $currentSlot for $name")

                        saveCobbledollarsToDatabase(uuid, player.getCobbleDollars())
                        println("Saved $${player.getCobbleDollars()} for $name on disconnect")


                        val slots = dao.getAllSlots(uuid)
                        val slotJson = buildJsonObject {
                            put("type", "save_slots_response")
                            put("slots", Json.encodeToJsonElement(slots.map {
                                buildJsonObject {
                                    put("slot", it.slot)
                                    put("lastSaved", it.lastSaved)
                                }
                            }))
                        }.toString()
                    }

                    FriendlyBattleManager.clearCache(uuid)
                    pendingNameTagUpdates.remove(uuid)
                    claimedTiers.remove(uuid)
                    ChatRateLimiter.cleanup(uuid)

                } catch (e: Exception) {
                    println("failed DC handler: ${e.message}")
                    e.printStackTrace()
                } finally {
                    ActiveSlotTracker.clear(uuid)
                    SaveSlotCooldowns.clear(uuid)
                }
            }
        }

//i got rid of this but its supposed to be if you dc during a battle idk if its needed ill do it later
        /*ServerPlayConnectionEvents.DISCONNECT.register { handler, server ->
            val player = handler.player
            val uuid = player.uuid

            // Check if player was in a battle
            if (BattleEndHandler.isInBattle(uuid)) {
                val battle = CobblemonBattleManager.getBattle(uuid)

                // Log or handle incomplete battle
                println("Player $uuid disconnected during battle")

                // Optionally notify the other player(s)
                battle?.participants
                    ?.filterIsInstance<PlayerBattleActor>()
                    ?.filter { it.uuid != uuid }
                    ?.forEach { opponent ->
                        server.playerManager.getPlayer(opponent.uuid)?.sendMessage(
                            Text.literal("§cYour opponent disconnected. The battle has been cancelled.")
                        )
                    }

                // Optionally: Mark the battle as incomplete or for review
                // Or: End the battle early and auto-award the win (risky)
            }
        }*/


        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        logger.info("Cobblemon Evolved Mod Initialized.")

    }
}
