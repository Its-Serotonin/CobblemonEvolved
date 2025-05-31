package com.serotonin.common.saveslots

import com.serotonin.common.networking.Database
import com.serotonin.common.networking.saveCobbledollarsToDatabase
import fr.harmex.cobbledollars.common.utils.extensions.getCobbleDollars
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.network.ServerPlayerEntity
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object SaveSlotAutoSaver {
    private const val AUTO_SAVE_INTERVAL_TICKS = 20 * 60 * 5

    private var tickCounter = 0
    val lastSaved = ConcurrentHashMap<UUID, Long>()

    fun register() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            tickCounter++
            if (tickCounter >= AUTO_SAVE_INTERVAL_TICKS) {
                tickCounter = 0

                server.playerManager.playerList.forEach { player ->
                    val uuid = player.uuid
                    val activeSlot = ActiveSlotTracker.getSlot(uuid) ?: return@forEach

                    if (!player.networkHandler.isConnectionOpen || !player.isAlive || player.serverWorld == null) {
                        println("Skipping autosave for ${player.name.string}: connection not fully open")
                        return@forEach
                    }

                    if (player.hasVehicle()) {
                        println("Skipping save for ${player.name.string}: player is riding an entity")
                        return@forEach
                    }

                    val slotData = PlayerSaveSlot(
                        uuid = uuid,
                        slot = activeSlot,
                        inventoryData = serializeInventory(player),
                        pokemonData = serializeParty(player),
                        pcData = serializePC(player),
                        lastSaved = System.currentTimeMillis(),
                        backpackData = serializeBackpack(player, player.server.registryManager),
                        trinketData = serializeTrinkets(player)

                    )
                    if (slotData.isMeaningless()) {
                        println("Skipping autosave for ${player.name.string}: slot is empty or meaningless")
                        return@forEach
                    }

                    try {
                        val now = System.currentTimeMillis()
                        val last = lastSaved[uuid] ?: 0L
                        if (now - last < AUTO_SAVE_INTERVAL_TICKS * 50L) return@forEach

                        val dao = SaveSlotDAOImpl(Database.dataSource)
                        val data = PlayerSaveSlot(
                            uuid = uuid,
                            slot = activeSlot,
                            inventoryData = serializeInventory(player),
                            pokemonData = serializeParty(player),
                            pcData = serializePC(player),
                            lastSaved = now,
                            backpackData = serializeBackpack(player, player.server.registryManager),
                            trinketData = serializeTrinkets(player)
                        )

                        dao.saveSlot(data)
                        dao.setActiveSlot(uuid, activeSlot)
                        lastSaved[uuid] = now
                        println("Auto-saved slot $activeSlot for ${player.name.string}")
                    } catch (e: Exception) {
                        println("Failed to auto-save for ${player.name.string}: ${e.message}")
                        e.printStackTrace()
                    }
                }

                val onlineUUIDs = server.playerManager.playerList.map { it.uuid }.toSet()
                lastSaved.keys.removeIf { it !in onlineUUIDs }
            }
        }


        ServerEntityEvents.ENTITY_UNLOAD.register { entity, world ->
            if (entity is ServerPlayerEntity) {
                val player = entity
                val uuid = player.uuid
                val slot = ActiveSlotTracker.getSlot(uuid) ?: return@register
                val dao = SaveSlotDAOImpl(Database.dataSource)
                val balance = entity.getCobbleDollars()



                player.server.execute {
                    try {

                        saveCobbledollarsToDatabase(uuid, balance)
                        println("Saved CobbleDollars for ${player.name.string} on dimension change")

                        val saveData = PlayerSaveSlot(
                            uuid = uuid,
                            slot = slot,
                            inventoryData = serializeInventory(player),
                            pokemonData = serializeParty(player),
                            pcData = serializePC(player),
                            backpackData = serializeBackpack(player, player.server.registryManager),
                            trinketData = serializeTrinkets(player),
                            lastSaved = System.currentTimeMillis()
                        )
                        dao.saveSlot(saveData)
                        dao.setActiveSlot(uuid, slot)
                        lastSaved[uuid] = System.currentTimeMillis()
                        println("Auto-saved slot $slot for ${player.name.string} on world unload (${world.registryKey.value.path})")
                    } catch (e: Exception) {
                        println("Failed to save on world unload: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun cleanupOfflinePlayers(online: Set<UUID>) {
        lastSaved.keys.removeIf { it !in online }
    }
}