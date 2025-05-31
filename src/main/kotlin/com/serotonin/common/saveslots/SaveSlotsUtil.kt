package com.serotonin.common.saveslots

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.storage.party.PartyPosition
import com.serotonin.common.networking.ActiveSlotUpdatePayload
import dev.emi.trinkets.api.TrinketsApi
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtSizeTracker
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import java.io.ByteArrayInputStream
import java.util.*

/*
fun saveCurrentSlot(
    player: ServerPlayerEntity,
    uuid: UUID,
    slot: Int,
    dao: SaveSlotDAO
): PlayerSaveSlot {
    clearEquippedBackpackAndTrinkets(player)

    val data = PlayerSaveSlot(
        uuid = uuid,
        slot = slot,
        inventoryData = serializeInventory(player),
        pokemonData = serializeParty(player),
        pcData = serializePC(player),
        backpackData = serializeBackpack(player, player.server.registryManager),
        trinketData = serializeTrinkets(player),
        lastSaved = System.currentTimeMillis()
    )

    dao.saveSlot(data)
    SaveSlotBackupManager.backupSlot(data)
    SaveSlotBackupManager.cleanupOldBackups(uuid, slot)

    return data
}*/


fun saveCurrentSlot(player: ServerPlayerEntity, uuid: UUID, slot: Int, dao: SaveSlotDAO) {
    val data = PlayerSaveSlot(
        uuid = uuid,
        slot = slot,
        inventoryData = serializeInventory(player),
        pokemonData = serializeParty(player),
        pcData = serializePC(player),
        backpackData = serializeBackpack(player, player.server.registryManager),
        trinketData = serializeTrinkets(player),
        lastSaved = System.currentTimeMillis()
    )

    dao.saveSlot(data)
    SaveSlotBackupManager.backupSlot(data)
    SaveSlotBackupManager.cleanupOldBackups(uuid, slot)
}

fun loadSaveSlot(
    player: ServerPlayerEntity,
    uuid: UUID,
    slot: Int,
    dao: SaveSlotDAO,
    context: ServerPlayNetworking.Context,
    sendSlotUpdate: Boolean = false
): Boolean {
    val slotData = dao.loadSlot(uuid, slot)
        ?: SaveSlotBackupManager.loadLatestBackup(uuid, slot)?.let {
            println("Loaded slot $slot from backup.")
            PlayerSaveSlot(
                uuid = uuid,
                slot = slot,
                inventoryData = it.inventory,
                pokemonData = it.party,
                pcData = it.pc,
                lastSaved = System.currentTimeMillis(),
                screenshotPath = null,
                backpackData = it.backpack,
                trinketData = it.trinkets
            )
        }

    if (slotData == null) {
        player.sendMessage(Text.literal("§cNo save data or backup found for slot $slot"))
        return false
    }

    println("Loading slot $slot for ${player.name.string}")

    try {

        val inventoryCopy = slotData.inventoryData.copyOf()
        val partyCopy = slotData.pokemonData.copyOf()
        val pcCopy = slotData.pcData.copyOf()
        val backpackCopy = slotData.backpackData.copyOf()
        val trinketCopy = slotData.trinketData.copyOf()


        val inventoryValid = try {
            val invTag = NbtIo.readCompressed(ByteArrayInputStream(inventoryCopy), NbtSizeTracker.ofUnlimitedBytes())
            val inventoryList = invTag.getList("Inventory", NbtElement.COMPOUND_TYPE.toInt())
            (0 until inventoryList.size).all { i ->
                val tag = inventoryList.getCompound(i)
                tag.contains("id")
            }
        } catch (e: Exception) {
            println("Inventory validation failed: ${e.message}")
            false
        }


        val partyValid = try {
            val partyTag = NbtIo.readCompressed(ByteArrayInputStream(partyCopy), NbtSizeTracker.ofUnlimitedBytes())
            val list = partyTag.getList("Party", NbtElement.COMPOUND_TYPE.toInt())
            (0 until list.size).all { list.getCompound(it).contains("Species") }
        } catch (e: Exception) {
            println("Party validation failed: ${e.message}")
            false
        }


        val pcValid = try {
            val pcTag = NbtIo.readCompressed(ByteArrayInputStream(pcCopy), NbtSizeTracker.ofUnlimitedBytes())
            !pcTag.isEmpty
        } catch (e: Exception) {
            println("PC validation failed: ${e.message}")
            false
        }


        val trinketTagValid = try {
            if (trinketCopy.isEmpty()) {
                println("Trinket data is empty, skipping validation.")
                true
            } else {
                NbtIo.readCompressed(ByteArrayInputStream(trinketCopy), NbtSizeTracker.ofUnlimitedBytes())
                    .contains("Trinkets")
            }
        } catch (e: Exception) {
            println("Trinket validation failed: ${e.message}")
            false
        }

        if (!inventoryValid || !partyValid || !pcValid || !trinketTagValid) {
            player.sendMessage(Text.literal("§cSave slot $slot contains invalid or corrupted data."))
            println("Skipping load — failed preliminary validation")
            return false
        }

        player.inventory.clear()
        Cobblemon.storage.getParty(player).apply { repeat(6) { remove(PartyPosition(it)) } }
        Cobblemon.storage.getPC(player).clearAll()
        clearEquippedBackpackAndTrinkets(player)


        deserializeInventory(player, inventoryCopy)
        deserializeParty(player, partyCopy)
        deserializePC(player, pcCopy)
        if (!isMeaningless(backpackCopy)) deserializeBackpack(player, backpackCopy)
        if (!isMeaningless(trinketCopy)) deserializeTrinkets(player, trinketCopy)

    } catch (e: Exception) {
        player.sendMessage(Text.literal("§cError loading slot $slot: ${e.message}"))
        println("Deserialization error while loading slot $slot: ${e.message}")
        e.printStackTrace()
        return false
    }

    ActiveSlotTracker.setSlot(uuid, slot)
    if (sendSlotUpdate) {
        ServerPlayNetworking.send(player, ActiveSlotUpdatePayload(slot))
    }

    println("Successfully loaded save slot $slot for ${player.name.string}")
    return true
}

fun shouldClearBeforeLoad(player: ServerPlayerEntity, currentSlotData: PlayerSaveSlot?): Boolean {
    val hasBackpack = getEquippedBackpack(player) != null
    val hasTrinkets = TrinketsApi.getTrinketComponent(player).map { it ->
        it.inventory.values.flatMap { map -> map.values }
            .any { inv -> (0 until inv.size()).any { !inv.getStack(it).isEmpty } }
    }.orElse(false)

    return currentSlotData != null && !currentSlotData.isMeaningless() && (hasBackpack || hasTrinkets)
}

fun hasResidualEquippedData(player: ServerPlayerEntity): Boolean {
    val party = Cobblemon.storage.getParty(player)
    val pc = Cobblemon.storage.getPC(player)


    val partyHasData = (0..5).any { i ->
        party[PartyPosition(i)] != null
    }

   
    val pcHasData = pc.any { true }

    val inventoryHasData = player.inventory.main.any { !it.isEmpty }
    val backpackEquipped = getEquippedBackpack(player)?.isEmpty == false

    val trinketEquipped = TrinketsApi.getTrinketComponent(player).map { it ->
        it.inventory.values.flatMap { group -> group.values }
            .any { inv -> (0 until inv.size()).any { !inv.getStack(it).isEmpty } }
    }.orElse(false)

    return inventoryHasData || partyHasData || pcHasData || backpackEquipped || trinketEquipped
}