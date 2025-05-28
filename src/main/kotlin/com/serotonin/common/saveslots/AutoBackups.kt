package com.serotonin.common.saveslots

import com.serotonin.common.networking.Database
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.server.MinecraftServer
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object SaveSlotAutoBackup {
    private const val AUTO_BACKUP_INTERVAL_TICKS = 20 * 60 * 60 // 1 hour
    private const val AUTO_SAVE_INTERVAL_TICKS = 20 * 60 * 5    // 5 minutes

    private var tickCounter = 0
    private val lastAutoSaveTimestamps = ConcurrentHashMap<UUID, Long>()

    fun register() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            tickCounter++
            if (tickCounter >= AUTO_BACKUP_INTERVAL_TICKS) {
                tickCounter = 0
                runAutoBackup(server)
            }
        }

        ServerLifecycleEvents.SERVER_STOPPING.register { server ->
            println("Server shutting down: backing up and saving all active save slots...")
            runAutoBackup(server)
            runShutdownSave(server)
        }
    }

    private fun runAutoBackup(server: MinecraftServer) {
        server.playerManager.playerList.forEach { player ->
            val uuid = player.uuid
            val slot = ActiveSlotTracker.getSlot(uuid) ?: return@forEach

            try {
                val slotData = PlayerSaveSlot(
                    uuid = uuid,
                    slot = slot,
                    inventoryData = serializeInventory(player),
                    pokemonData = serializeParty(player),
                    pcData = serializePC(player),
                    lastSaved = System.currentTimeMillis()
                )

                val now = System.currentTimeMillis()
                val lastSaved = lastAutoSaveTimestamps[uuid] ?: 0L

                if (now - lastSaved >= AUTO_SAVE_INTERVAL_TICKS * 50L) {
                    SaveSlotBackupManager.backupSlot(slotData)
                    SaveSlotBackupManager.cleanupOldBackups(uuid, slot)
                    lastAutoSaveTimestamps[uuid] = now
                    println("Auto-backed up slot $slot for ${player.name.string}")
                }
            } catch (e: Exception) {
                println("Failed to auto-backup slot for ${player.name.string}: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun runShutdownSave(server: MinecraftServer) {
        val dao = SaveSlotDAOImpl(Database.dataSource)
        server.playerManager.playerList.forEach { player ->
            val uuid = player.uuid
            val slot = ActiveSlotTracker.getSlot(uuid) ?: return@forEach

            try {
                val slotData = PlayerSaveSlot(
                    uuid = uuid,
                    slot = slot,
                    inventoryData = serializeInventory(player),
                    pokemonData = serializeParty(player),
                    pcData = serializePC(player),
                    lastSaved = System.currentTimeMillis()
                )
                dao.saveSlot(slotData)
                println("Saved slot $slot to database for ${player.name.string}")
            } catch (e: Exception) {
                println("Failed to save slot $slot for ${player.name.string} on shutdown: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}