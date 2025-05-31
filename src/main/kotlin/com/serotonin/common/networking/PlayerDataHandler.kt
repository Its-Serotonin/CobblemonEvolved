package com.serotonin.common.networking

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.storage.party.PartyPosition
import com.serotonin.common.api.events.EloManager
import com.serotonin.common.client.gui.competitivehandbook.CustomBookScreen
import com.serotonin.common.client.gui.saveslots.SaveSlotScreen
import com.serotonin.common.client.networking.ClientStatsStorage
import com.serotonin.common.elosystem.*
import com.serotonin.common.networking.LeaderboardData.pendingLeaderboardCallbacks
import com.serotonin.common.registries.FriendlyBattleManager
import com.serotonin.common.registries.RankedBattleEvents
import com.serotonin.common.saveslots.*
import com.serotonin.common.tourneys.TournamentManager
import com.serotonin.common.tourneys.TournamentManagerClient
import kotlinx.serialization.json.*
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.client.MinecraftClient
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import java.util.*
import kotlin.concurrent.thread


data class RawJsonPayload(val json: String) : CustomPayload {
    companion object {
        val ID = CustomPayload.id<RawJsonPayload>("cobblemonevolved_raw_json")
        val CODEC = PacketCodec.of<PacketByteBuf, RawJsonPayload>(
            { value, buf -> buf.writeString(value.json) },
            { buf -> RawJsonPayload(buf.readString()) }
        )
    }

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID
}


data class RankResponsePayload(
    val uuid: String,
    val name: String,
    val elo: Int,
    val silent: Boolean = false,
    val reset: Boolean = false
) : CustomPayload {

    companion object {

        val ID = CustomPayload.id<RankResponsePayload>("cobblemonevolved_rank_response")
        val CODEC = PacketCodec.of<PacketByteBuf, RankResponsePayload>(
            { value, buf ->
                buf.writeString(value.uuid)
                buf.writeString(value.name)
                buf.writeInt(value.elo)
                buf.writeBoolean(value.silent)
                buf.writeBoolean(value.reset)
            },
            { buf ->
                val uuid = buf.readString()
                val name = buf.readString()
                val elo = buf.readInt()
                val silent = if (buf.isReadable) buf.readBoolean() else false
                val reset = if (buf.isReadable) buf.readBoolean() else false
                RankResponsePayload(uuid, name, elo, silent, reset)
            }
        )
    }


    override fun getId(): CustomPayload.Id<out CustomPayload> = ID

}

data class ClaimedTiersPayload(
    val uuid: String,
    val tiers: List<String>
) : CustomPayload {
    companion object {
        val ID = CustomPayload.id<ClaimedTiersPayload>("cobblemonevolved_claimed_tiers")
        val CODEC = PacketCodec.of<PacketByteBuf, ClaimedTiersPayload>(
            { value, buf ->
                buf.writeString(value.uuid)
                buf.writeVarInt(value.tiers.size)
                value.tiers.forEach { buf.writeString(it) }
            },
            { buf ->
                val uuid = buf.readString()
                val size = buf.readVarInt()
                val tiers = List(size) { buf.readString() }
                ClaimedTiersPayload(uuid, tiers)
            }
        )
    }

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID
}


data class TournamentSignupPayload(val toggle: Boolean) : CustomPayload {
    companion object {
        val ID = CustomPayload.Id<TournamentSignupPayload>(Identifier.of("cobblemonevolved", "tournament_signup"))
        val CODEC: PacketCodec<PacketByteBuf, TournamentSignupPayload> = PacketCodec.of(
            { payload, buf -> buf.writeBoolean(payload.toggle) },
            { buf -> TournamentSignupPayload(buf.readBoolean()) }
        )
    }

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID
}


data class PlayerStatsPayload(
    val uuid: String,
    val name: String,
    val elo: Int,
    val tier: String,
    val battlesWon: Int,
    val battlesTotal: Int,
    val winStreak: Int,
    val longestWinStreak: Int
) : CustomPayload {

    companion object {
        val ID = CustomPayload.id<PlayerStatsPayload>("cobblemonevolved_player_stats")
        val CODEC: PacketCodec<PacketByteBuf, PlayerStatsPayload> = PacketCodec.of(
            { value, buf ->
                buf.writeString(value.uuid)
                buf.writeString(value.name)
                buf.writeInt(value.elo)
                buf.writeString(value.tier)
                buf.writeInt(value.battlesWon)
                buf.writeInt(value.battlesTotal)
                buf.writeInt(value.winStreak)
                buf.writeInt(value.longestWinStreak)
            },
            { buf ->
                PlayerStatsPayload(
                    uuid = buf.readString(),
                    name = buf.readString(),
                    elo = buf.readInt(),
                    tier = buf.readString(),
                    battlesWon = buf.readInt(),
                    battlesTotal = buf.readInt(),
                    winStreak = buf.readInt(),
                    longestWinStreak = buf.readInt()
                )
            }
        )
    }

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID
}

data class SaveSlotRequestPayload(
    val slot: Int,
    val action: Action,
    val isSwitching: Boolean = false,
    val backpackData: ByteArray = ByteArray(0),
    val trinketData: ByteArray = ByteArray(0)
) : CustomPayload {

    enum class Action {
        SAVE, LOAD, DELETE, SWITCH
    }

    companion object {
        val ID = CustomPayload.id<SaveSlotRequestPayload>("cobblemonevolved_save_slot_request")
        val CODEC: PacketCodec<PacketByteBuf, SaveSlotRequestPayload> = PacketCodec.of(
            { payload, buf ->
                buf.writeInt(payload.slot)
                buf.writeEnumConstant(payload.action)
                buf.writeBoolean(payload.isSwitching)
                buf.writeByteArray(payload.backpackData)
                buf.writeByteArray(payload.trinketData)
            },
            { buf ->
                val slot = buf.readInt()
                val action = buf.readEnumConstant(Action::class.java)
                val isSwitching = if (buf.isReadable) buf.readBoolean() else false
                val backpackData = if (buf.isReadable) buf.readByteArray() else ByteArray(0)
                val trinketData = if (buf.isReadable) buf.readByteArray() else ByteArray(0)
                SaveSlotRequestPayload(slot, action, isSwitching, backpackData, trinketData)
            }
        )
    }

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SaveSlotRequestPayload

        if (slot != other.slot) return false
        if (isSwitching != other.isSwitching) return false
        if (action != other.action) return false
        if (!backpackData.contentEquals(other.backpackData)) return false
        if (!trinketData.contentEquals(other.trinketData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = slot
        result = 31 * result + isSwitching.hashCode()
        result = 31 * result + action.hashCode()
        result = 31 * result + backpackData.contentHashCode()
        result = 31 * result + trinketData.contentHashCode()
        return result
    }
}


data class SaveSlotResponsePayload(
    val slot: Int,
    val action: String,
    val success: Boolean,
    val backpackData: ByteArray = ByteArray(0),
    val trinketData: ByteArray = ByteArray(0),
) : CustomPayload {
    companion object {
        val ID = CustomPayload.id<SaveSlotResponsePayload>("cobblemonevolved_save_slot_response")
        val CODEC = PacketCodec.of<PacketByteBuf, SaveSlotResponsePayload>(
            { value, buf ->
                buf.writeInt(value.slot)
                buf.writeString(value.action)
                buf.writeBoolean(value.success)
                buf.writeByteArray(value.backpackData)
                buf.writeByteArray(value.trinketData)
            },
            { buf ->
                SaveSlotResponsePayload(
                    slot = buf.readInt(),
                    action = buf.readString(),
                    success = buf.readBoolean(),
                    backpackData = if (buf.isReadable) buf.readByteArray() else ByteArray(0),
                    trinketData = if (buf.isReadable) buf.readByteArray() else ByteArray(0)
                )
            }
        )
    }

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SaveSlotResponsePayload

        if (slot != other.slot) return false
        if (success != other.success) return false
        if (action != other.action) return false
        if (!backpackData.contentEquals(other.backpackData)) return false
        if (!trinketData.contentEquals(other.trinketData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = slot
        result = 31 * result + success.hashCode()
        result = 31 * result + action.hashCode()
        result = 31 * result + backpackData.contentHashCode()
        result = 31 * result + trinketData.contentHashCode()
        return result
    }
}


data class ActiveSlotUpdatePayload(val slot: Int) : CustomPayload {
    companion object {
        val ID = CustomPayload.id<ActiveSlotUpdatePayload>("cobblemonevolved_active_slot")
        val CODEC = PacketCodec.of<PacketByteBuf, ActiveSlotUpdatePayload>(
            { value, buf -> buf.writeInt(value.slot) },
            { buf -> ActiveSlotUpdatePayload(buf.readInt()) }
        )
    }

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID
}


object PlayerDataSyncNetworking {

    fun registerPayloads() {

        playC2S().register<RawJsonPayload>(
            RawJsonPayload.ID,
            RawJsonPayload.CODEC
        )
        playC2S().register(
            RankResponsePayload.ID,
            RankResponsePayload.CODEC
        )

        playS2C().register(
            RankResponsePayload.ID,
            RankResponsePayload.CODEC
        )
        playS2C().register(
            RawJsonPayload.ID,
            RawJsonPayload.CODEC
        )

        playS2C().register(
            ClaimedTiersPayload.ID,
            ClaimedTiersPayload.CODEC
        )

        playC2S().register(
            TournamentSignupPayload.ID,
            TournamentSignupPayload.CODEC
        )

        playS2C().register(
            PlayerStatsPayload.ID,
            PlayerStatsPayload.CODEC
        )

        playS2C().register(
            ActiveSlotUpdatePayload.ID,
            ActiveSlotUpdatePayload.CODEC
        )

        playC2S().register(
            SaveSlotRequestPayload.ID,
            SaveSlotRequestPayload.CODEC
        )

        playS2C().register(
            SaveSlotResponsePayload.ID,
            SaveSlotResponsePayload.CODEC
        )

        playS2C().register(
            OpenCompetitiveHandbookPayload.ID,
            OpenCompetitiveHandbookPayload.CODEC
        )


        ServerPlayNetworking.registerGlobalReceiver<RawJsonPayload>(
            RawJsonPayload.ID,
        )
        { payload, context ->
            context.server().execute {
                handleRawJsonPayloadServerSide(payload, context.player())
            }
        }

        println("Registered RawJsonPayload C2S")



        ServerPlayNetworking.registerGlobalReceiver<RankResponsePayload>(
            RankResponsePayload.ID
        ) { payload, context ->
            context.server().execute {
                handleRankResponseServer(context.player(), payload)
            }
        }


        ServerPlayNetworking.registerGlobalReceiver<TournamentSignupPayload>(
            TournamentSignupPayload.ID,
        ) { payload, context ->
            context.server().execute {
                val player = context.player()
                val newState = payload.toggle
                val actual = TournamentManager.toggleSignup(player)

                if (actual == newState) {
                    val message = if (actual) {
                        "§aYou are now signed up for the tournament."
                    } else {
                        "§eYou have been removed from the tournament."
                    }
                    player.sendMessage(Text.literal(message), false)

                } else {
                    player.sendMessage(Text.literal("§cSignup toggle mismatch. Try again."), false)
                }
            }
        }

        ServerPlayNetworking.registerGlobalReceiver<SaveSlotRequestPayload>(
            SaveSlotRequestPayload.ID
        ) { payload, context ->
            val player = context.player()
            val uuid = player.uuid
            val dao = SaveSlotDAOImpl(Database.dataSource)

            context.server().execute {
                when (payload.action) {

                    SaveSlotRequestPayload.Action.SAVE -> {
                        if (payload.slot !in 1..3) {
                            player.sendMessage(Text.literal("§cInvalid slot number: ${payload.slot}"),false)
                            return@execute
                        }

                        if (RankedBattleEvents.activePlayerBattles.containsKey(player.uuid)) {
                            player.sendMessage(Text.literal("§cYou can't use save slots during a battle!"), false)
                            return@execute
                        }

                        if (player.hasVehicle()) {
                            player.sendMessage(Text.literal("§cCannot save while riding an entity!"),false)
                            return@execute
                        }

                        val allSlots = dao.getAllSlots(uuid)
                        val targetSlot = allSlots.find { it.slot == payload.slot }
                        val allSlotsMeaningless = allSlots.all { it.isMeaningless() }


                        val inventory = serializeInventory(player)
                        val party = serializeParty(player)
                        val pc = serializePC(player)
                        val backpack = serializeBackpack(player, player.server.registryManager)
                        val trinkets = serializeTrinkets(player)

                        val data = PlayerSaveSlot(
                            uuid = uuid,
                            slot = payload.slot,
                            inventoryData = inventory,
                            pokemonData = party,
                            pcData = pc,
                            backpackData = backpack,
                            trinketData = trinkets,
                            lastSaved = System.currentTimeMillis()
                        )

                        val isDuplicate =
                            !data.isMeaningless() &&
                                    !allSlotsMeaningless &&
                                    allSlots.any { other ->
                                        other.slot != data.slot &&
                                                !other.isMeaningless() &&
                                                other.inventoryData.contentEquals(data.inventoryData) &&
                                                other.pokemonData.contentEquals(data.pokemonData) &&
                                                other.pcData.contentEquals(data.pcData) &&
                                                other.backpackData.contentEquals(data.backpackData) &&
                                                other.trinketData.contentEquals(data.trinketData)
                                    }

                        if (isDuplicate) {

                            println("Suspicious duplicate save for slot ${payload.slot} by ${player.name.string}")

                        }

                        dao.saveSlot(data)
                        SaveSlotBackupManager.backupSlot(data)
                        SaveSlotBackupManager.cleanupOldBackups(uuid, payload.slot)
                        ActiveSlotTracker.setSlot(uuid, payload.slot)
                        dao.setActiveSlot(uuid, payload.slot)
                        ServerPlayNetworking.send(player, ActiveSlotUpdatePayload(payload.slot))
                        ServerPlayNetworking.send(player, SaveSlotResponsePayload(payload.slot, "SAVE", true, data.backpackData, data.trinketData))
                        player.sendMessage(Text.literal("§aSaved slot ${payload.slot}"),false)

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
                    }


                    SaveSlotRequestPayload.Action.LOAD -> {
                        if (payload.slot !in 1..3) {
                            player.sendMessage(Text.literal("§cInvalid slot number: ${payload.slot}"),false)
                            return@execute
                        }


                        if (RankedBattleEvents.activePlayerBattles.containsKey(player.uuid)) {
                            player.sendMessage(Text.literal("§cYou can't use save slots during a battle!"), false)
                            return@execute
                        }


                        if (player.hasVehicle()) {
                            player.sendMessage(Text.literal("§cCannot load while riding an entity!"),false)
                            return@execute
                        }

                        val currentSlot = ActiveSlotTracker.getSlot(uuid)



                        val targetSlot = dao.loadSlot(uuid, payload.slot)
                        if (targetSlot == null) {
                            player.sendMessage(Text.literal("§cThat save slot doesn't exist."),false)
                            return@execute
                        }


                        if (hasResidualEquippedData(player)) {
                            println("Clearing before loading slot ${payload.slot}")
                            player.inventory.clear()
                            Cobblemon.storage.getParty(player).apply { repeat(6) { remove(PartyPosition(it)) } }
                            Cobblemon.storage.getPC(player).clearAll()
                            clearEquippedBackpackAndTrinkets(player)
                        }



                        val success = loadSaveSlot(
                            player, uuid, payload.slot, dao, context,
                            sendSlotUpdate = payload.isSwitching
                        )

                        if (success) {
                            ActiveSlotTracker.setSlot(uuid, payload.slot)
                            dao.setActiveSlot(uuid, payload.slot)
                            player.sendMessage(Text.literal("§bLoaded save slot ${payload.slot}"),false)
                            ServerPlayNetworking.send(player, ActiveSlotUpdatePayload(payload.slot))
                        } else {
                            player.sendMessage(Text.literal("§cFailed to load slot ${payload.slot}."),false)
                        }
                    }

                    SaveSlotRequestPayload.Action.SWITCH -> {
                        if (SaveSlotCooldowns.isOnCooldown(uuid)) {
                            player.sendMessage(Text.literal("§cPlease wait before switching slots again."), false)
                            return@execute
                        }
                        SaveSlotCooldowns.updateTimestamp(uuid)

                        if (isPlayerMounted(player)) {
                            player.sendMessage(Text.literal("§cCannot switch save slots while mounted!"), false)
                            return@execute
                        }

                        if (RankedBattleEvents.activePlayerBattles.containsKey(player.uuid)) {
                            player.sendMessage(Text.literal("§cYou can't use save slots during a battle!"), false)
                            return@execute
                        }

                        val targetSlot = payload.slot
                        if (targetSlot !in 1..3) {
                            player.sendMessage(Text.literal("§cInvalid slot number: $targetSlot"), false)
                            return@execute
                        }

                        val currentSlot = ActiveSlotTracker.getSlot(uuid)
                        if (currentSlot == targetSlot) {
                            player.sendMessage(Text.literal("§eYou're already using save slot $targetSlot."), false)
                            return@execute
                        }

                        val allSlots = dao.getAllSlots(uuid)
                        val hasOtherSlots = allSlots.any { it.slot != targetSlot && !it.isMeaningless() }
                        val isFreshStart = !hasOtherSlots


                        if (currentSlot != null) {
                            val serializedData = PlayerSaveSlot(
                                uuid = uuid,
                                slot = currentSlot,
                                inventoryData = serializeInventory(player),
                                pokemonData = serializeParty(player),
                                pcData = serializePC(player),
                                backpackData = serializeBackpack(player, player.server.registryManager),
                                trinketData = serializeTrinkets(player),
                                lastSaved = System.currentTimeMillis()
                            )

                            val currentSlotData = dao.loadSlot(uuid, currentSlot)
                            val shouldClear = shouldClearBeforeLoad(player, currentSlotData)

                            if (shouldClear) {
                                player.inventory.clear()
                                Cobblemon.storage.getParty(player).apply { repeat(6) { remove(PartyPosition(it)) } }
                                Cobblemon.storage.getPC(player).clearAll()
                                clearEquippedBackpackAndTrinkets(player)
                                println("Cleared inventory, party, PC, trinkets, and backpack before switching from slot $currentSlot")
                            } else {
                                println("Skipped clearing — current slot is empty or has no equipped data")
                            }

                            dao.saveSlot(serializedData)
                            SaveSlotBackupManager.backupSlot(serializedData)
                            SaveSlotBackupManager.cleanupOldBackups(uuid, currentSlot)
                            println("Auto-saved slot $currentSlot before switching.")
                        }

                        val loadedSlot = dao.loadSlot(uuid, targetSlot)
                        if (loadedSlot != null) {
                            println("Loading save slot $targetSlot for ${player.name.string}")
                            clearNearbyDroppedItems(context.server()!!, player)
                            //risky but hopefully prevents duping
                            //clearEquippedBackpackAndTrinkets(player)

                            //even riskier but lets try it
                            if (hasResidualEquippedData(player)) {
                                println("Clearing in-memory data before loading slot $targetSlot")
                                player.inventory.clear()
                                Cobblemon.storage.getParty(player).apply { repeat(6) { remove(PartyPosition(it)) } }
                                Cobblemon.storage.getPC(player).clearAll()
                                clearEquippedBackpackAndTrinkets(player)
                            }

                            val success = loadSaveSlot(player, uuid, targetSlot, dao, context, sendSlotUpdate = true)
                            if (success) {
                                ActiveSlotTracker.setSlot(uuid, targetSlot)
                                dao.setActiveSlot(uuid, targetSlot)
                                ServerPlayNetworking.send(player, ActiveSlotUpdatePayload(targetSlot))
                                player.sendMessage(Text.literal("§bSwitched to save slot $targetSlot"), false)
                            } else {
                                player.sendMessage(Text.literal("§cFailed to load save slot $targetSlot"), false)
                            }
                        } else {

                            if (!isFreshStart && hasResidualEquippedData(player)) {
                                println("Clearing in-memory data before initializing blank slot $targetSlot")
                                player.inventory.clear()
                                Cobblemon.storage.getParty(player).apply { repeat(6) { remove(PartyPosition(it)) } }
                                Cobblemon.storage.getPC(player).clearAll()
                                clearEquippedBackpackAndTrinkets(player)


                            //old but commenting out in case i need to bring it back
                            /*
                            val hasEquippedData = getEquippedBackpack(player) != null ||
                                    TrinketsApi.getTrinketComponent(player).map {
                                        it.inventory.values.flatMap { group -> group.values }
                                            .any { inv -> (0 until inv.size()).any { !inv.getStack(it).isEmpty } }
                                    }.orElse(false)

                            if (!isFreshStart && hasEquippedData) {
                                println("Wiping equipped state before initializing new slot $targetSlot")
                                player.inventory.clear()
                                Cobblemon.storage.getParty(player).apply { repeat(6) { remove(PartyPosition(it)) } }
                                Cobblemon.storage.getPC(player).clearAll()
                                clearEquippedBackpackAndTrinkets(player)

                             */
                            } else {
                                println("ℹ️ No equipped data found — skipping clear for new slot $targetSlot")
                            }

                            clearNearbyDroppedItems(context.server()!!, player)



                            val serializedNew = PlayerSaveSlot(
                                uuid = uuid,
                                slot = targetSlot,
                                inventoryData = serializeInventory(player),
                                pokemonData = serializeParty(player),
                                pcData = serializePC(player),
                                backpackData = serializeBackpack(player, player.server.registryManager),
                                trinketData = serializeTrinkets(player),
                                lastSaved = System.currentTimeMillis()
                            )

                            val isDuplicate = hasOtherSlots &&
                                    !serializedNew.isMeaningless() &&
                                    allSlots.any { other ->
                                        other.slot != targetSlot && !other.isMeaningless() &&
                                                other.inventoryData.contentEquals(serializedNew.inventoryData) &&
                                                other.pokemonData.contentEquals(serializedNew.pokemonData) &&
                                                other.pcData.contentEquals(serializedNew.pcData) &&
                                                other.backpackData.contentEquals(serializedNew.backpackData) &&
                                                other.trinketData.contentEquals(serializedNew.trinketData)
                                    }

                            if (serializedNew.isMeaningless() && allSlots.all { it.isMeaningless() }) {
                                println("Skipping duplicate check: all slots meaningless")
                            } else if (isDuplicate) {
                                println("Suspected duplicate save on SWITCH: slot $targetSlot matches existing data.")
                            }

                            dao.saveSlot(serializedNew)
                            SaveSlotBackupManager.backupSlot(serializedNew)
                            SaveSlotBackupManager.cleanupOldBackups(uuid, targetSlot)

                            val success = loadSaveSlot(player, uuid, targetSlot, dao, context, sendSlotUpdate = true)
                            if (success) {
                                ActiveSlotTracker.setSlot(uuid, targetSlot)
                                dao.setActiveSlot(uuid, targetSlot)
                                ServerPlayNetworking.send(player, ActiveSlotUpdatePayload(targetSlot))
                                player.sendMessage(Text.literal("§aInitialized and switched to new save slot $targetSlot"), false)
                            } else {
                                player.sendMessage(Text.literal("§cFailed to load save slot $targetSlot"), false)
                            }
                        }
                    }

                SaveSlotRequestPayload.Action.DELETE -> {
                        dao.deleteSlot(uuid, payload.slot)

                        if (ActiveSlotTracker.getSlot(uuid) == payload.slot) {
                            ActiveSlotTracker.clear(uuid)
                            ServerPlayNetworking.send(player, ActiveSlotUpdatePayload(-1))
                        }


                        SaveSlotBackupManager.deleteAllBackups(uuid, payload.slot)

                    val screenshotDir = player.server.runDirectory.resolve("screenshots/saveslots").toFile()
                    if (screenshotDir.exists()) {
                        val deletedFiles = screenshotDir.listFiles { file ->
                            file.name.startsWith("save_slot_${payload.slot}")
                        }?.onEach {
                            println("Deleting screenshot: ${it.name}")
                            it.delete()
                        } ?: emptyArray()

                        if (deletedFiles.isEmpty()) {
                            println("No screenshots found for slot ${payload.slot}")
                        }
                    }



                        player.sendMessage(Text.literal("§cDeleted slot ${payload.slot}"),false)

                        val slots = dao.getAllSlots(uuid)
                        val json = buildJsonObject {
                            put("type", "save_slots_response")
                            put(
                                "slots", kotlinx.serialization.json.Json.encodeToJsonElement(
                                slots.map {
                                    buildJsonObject {
                                        put("slot", it.slot)
                                        put("lastSaved", it.lastSaved)
                                    }
                                }
                            ))
                        }.toString()

                        ServerPlayNetworking.send(player, RawJsonPayload(json))


                    }
                }

            }
        }
    }

}

private fun handleRawJsonPayloadServerSide(payload: RawJsonPayload, player: ServerPlayerEntity) {

    try {
        val json = Json.parseToJsonElement(payload.json).jsonObject
        val type = RawPayloadType.from(json["type"]?.jsonPrimitive?.content ?: return)
            ?: throw IllegalArgumentException("Missing 'type' field")


        when (type) {

            RawPayloadType.GetElo -> {
                val uuid = UUID.fromString(json["uuid"]?.jsonPrimitive?.content ?: return)
                val stats = getPlayerStats(uuid)

                println("handleRawJsonPayloadServerSide: stats for $uuid = $stats")
                if (stats != null) {
                    val silent = json["silent"]?.jsonPrimitive?.booleanOrNull ?: false

                    val response = RankResponsePayload(
                        uuid.toString(),
                        stats.name,
                        stats.elo,
                        silent = silent
                    )
                    println("Sending RankResponsePayload: $response")

                    ServerPlayNetworking.send(player, response)
                } else {
                    println("No stats found for $uuid, skipping response")
                }
            }

            RawPayloadType.UpdateElo -> {
                val uuid = UUID.fromString(json["uuid"]?.jsonPrimitive?.content ?: return)
                val newElo = json["elo"]?.jsonPrimitive?.int ?: return
                val stats = getPlayerStats(uuid)
                if (stats != null) {
                    updatePlayerStats(stats.copy(elo = newElo))
                    val response = RankResponsePayload(
                        uuid.toString(),
                        stats.name,
                        newElo,
                        silent = true
                    )
                    ServerPlayNetworking.send(player, response)
                }
            }

            RawPayloadType.EloResponse -> {
                val uuid = UUID.fromString(json["uuid"]?.jsonPrimitive?.content ?: return)
                val name = json["name"]?.jsonPrimitive?.content ?: return

                println("Server received elo_response with name info: UUID=$uuid, Name=$name")

                val stats = getPlayerStats(uuid)
                if (stats != null) {
                    val response = RankResponsePayload(uuid.toString(), stats.name, stats.elo)
                    ServerPlayNetworking.send(player, response)
                } else {
                    println("No stats found for $uuid on elo_response")
                }
            }


            RawPayloadType.GetLeaderboard -> {
                val leaderboard = mutableListOf<LeaderboardEntry>()

                Database.dataSource.connection.use { conn ->
                    conn.prepareStatement("SELECT player_name, elo, tier FROM player_stats ORDER BY elo DESC LIMIT 10")
                        .use { stmt ->
                            stmt.executeQuery().use { rs ->
                                var index = 0
                                while (rs.next()) {

                                    val colorPrefix = when (index) {
                                        0 -> "§6§l"  // Gold
                                        1 -> "§7§l"  // Silver
                                        2 -> "§3§l"  // Bronze
                                        else -> "§f"
                                    }
                                    leaderboard.add(
                                        LeaderboardEntry(
                                            name = rs.getString("player_name"),
                                            elo = rs.getInt("elo"),
                                            tier = rs.getString("tier"),
                                            prefix = colorPrefix
                                        )
                                    )
                                    index++
                                }
                            }
                        }
                }

                LeaderboardServerCache.latestLeaderboard.clear()
                LeaderboardServerCache.latestLeaderboard.addAll(leaderboard)


                val leaderboardJson = buildJsonObject {
                    put("type", "leaderboard_response")
                    put("leaderboard", Json.encodeToJsonElement(leaderboard))
                }.toString()
                ServerPlayNetworking.send(player, RawJsonPayload(leaderboardJson))
            }

            RawPayloadType.UpdateName -> {
                val uuid = UUID.fromString(json["uuid"]?.jsonPrimitive?.content ?: return)
                val newName = json["name"]?.jsonPrimitive?.content ?: return
                val stats = getPlayerStats(uuid)
                if (stats != null) {
                    updatePlayerStats(stats.copy(name = newName))
                }
            }

            RawPayloadType.TriggerLeaderboardDisplay -> {
                val x = json["x"]?.jsonPrimitive?.int ?: return
                val y = json["y"]?.jsonPrimitive?.int ?: return
                val z = json["z"]?.jsonPrimitive?.int ?: return
                leaderboardPosition = BlockPos(x, y, z)
                displayLeaderboardAsFloatingText(x, y, z)
            }

            RawPayloadType.GetFriendlyBattle -> {
                val uuid = player.uuid
                val setting = getFriendlyBattle(uuid)
                FriendlyBattleManager.cacheSetting(uuid, setting)
                syncFriendlyBattleToClient(player, setting)
            }


            RawPayloadType.ToggleFriendlyBattle -> {
                val json = Json.decodeFromString<JsonObject>(payload.json)

                val newState = json["value"]?.jsonPrimitive?.booleanOrNull ?: run {
                    println("Server error: Missing or invalid 'value' in toggle_friendly_battle payload")
                    return
                }

                val silent = json["silent"]?.jsonPrimitive?.booleanOrNull ?: false

                val uuid = player.uuid

                thread(name = "toggle-friendly-$uuid") {
                    try {

                        setFriendlyBattle(uuid, newState)

                        FriendlyBattleManager.cacheSetting(uuid, newState)

                        syncFriendlyBattleToClient(player, newState)

                        if (!silent) {
                            player.sendMessage(
                                Text.literal(
                                    if (newState)
                                        "§fFriendly Battle mode is now §a§l§oON§r"
                                    else
                                        "§fFriendly Battle mode is now §c§l§oOFF§r"
                                ),
                                false
                            )
                        }

                        println("Player ${player.name.string} toggled friendly battle to: $newState")
                    } catch (e: Exception) {
                        player.sendMessage(Text.literal("§cError toggling Friendly Battle mode."), false)
                        e.printStackTrace()
                    }
                }
            }

            RawPayloadType.GetClaimedTiers -> {
                val uuid = player.uuid
                val claimed = claimedTiers[uuid]?.toList() ?: loadClaimedTiers(uuid).toList()

                val payload = ClaimedTiersPayload(uuid.toString(), claimed)
                println("Sending ClaimedTiersPayload: $payload")

                ServerPlayNetworking.send(player, payload)
            }

            RawPayloadType.GetTournamentSignupStatus -> {
                val tournament = TournamentManager.getActiveTournament()
                val uuid = player.uuid

                val isSignedUp = tournament?.let {
                    TournamentManager.isPlayerSignedUp(uuid, it.id)
                } ?: false

                val response = buildJsonObject {
                    put("type", "tournament_info")
                    put("signedUp", isSignedUp)

                    tournament?.let {
                        put("ruleset", it.ruleset)
                        put("startTime", it.startTime.toString())
                        put("status", "Active")
                    } ?: run {
                        put("status", "NoTournament")
                    }
                }.toString()

                ServerPlayNetworking.send(player, RawJsonPayload(response))
            }


            RawPayloadType.GetPlayerStats -> {
                val stats = getPlayerStats(player.uuid)
                if (stats != null) {
                    val payload = PlayerStatsPayload(
                        uuid = player.uuid.toString(),
                        name = stats.name,
                        elo = stats.elo,
                        tier = stats.tier,
                        battlesWon = stats.battlesWon,
                        battlesTotal = stats.battlesTotal,
                        winStreak = stats.winStreak,
                        longestWinStreak = stats.longestWinStreak
                    )
                    ServerPlayNetworking.send(player, payload)
                    println("Sent PlayerStatsPayload to ${player.name.string}")
                }
            }

            RawPayloadType.GetAllSaveSlots -> {
                val uuid = player.uuid
                val dao = SaveSlotDAOImpl(Database.dataSource)
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
            }

            RawPayloadType.ClaimTierReward -> {
                val tier = json["tier"]?.jsonPrimitive?.content ?: return
                val uuid = player.uuid

                if (hasClaimedTier(uuid, tier)) {
                    val errorJson = buildJsonObject {
                        put("type", "claim_tier_result")
                        put("success", false)
                        put("reason", "already_claimed")
                        put("tier", tier)
                        put(
                            "message",
                            "You already claimed rewards for ${
                                tier.replace('_', ' ').replaceFirstChar { it.uppercaseChar() }
                            } Tier."
                        )
                    }.toString()
                    ServerPlayNetworking.send(player, RawJsonPayload(errorJson))
                    return
                }


                saveClaimedTier(uuid, tier)

                val successJson = buildJsonObject {
                    put("type", "claim_tier_result")
                    put("success", true)
                    put("tier", tier)
                    put(
                        "message",
                        "§aRewards claimed for ${tier.replace('_', ' ').replaceFirstChar { it.uppercaseChar() }} Tier!"
                    )
                }.toString()
                ServerPlayNetworking.send(player, RawJsonPayload(successJson))

                val claimed = claimedTiers[uuid]?.toList() ?: loadClaimedTiers(uuid).toList()
                ServerPlayNetworking.send(player, ClaimedTiersPayload(uuid.toString(), claimed))
                println("Claimed and synced tier $tier for $uuid")
            }


            else -> {
                println("Unknown or unhandled RawPayloadType: $type")
            }
        }
    } catch (e: Exception) {
        println("Error handling RawJsonPayload: ${e.message}")
    }
}


object PlayerDataSyncNetworkingClient {

    fun sendSaveSlotRequest(slot: Int, action: SaveSlotRequestPayload.Action, isSwitching: Boolean = false) {
        val payload = SaveSlotRequestPayload(slot, action, isSwitching)
        ClientPlayNetworking.send(payload)
    }

    fun sendSaveSlotRequest(payload: SaveSlotRequestPayload) {
        ClientPlayNetworking.send(payload)
    }

    fun registerPayloads() {

        ClientPlayNetworking.registerGlobalReceiver(
            RankResponsePayload.ID
        ) { payload: RankResponsePayload, context ->
            context.client().execute {
                handleRankResponseClient(payload)
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(
            RawJsonPayload.ID
        ) { payload: RawJsonPayload, context ->
            context.client().execute {
                handleRawJsonClientSide(payload)
            }
        }


        ClientPlayNetworking.registerGlobalReceiver(
            ClaimedTiersPayload.ID
        ) { payload, _ ->
            val uuid = UUID.fromString(payload.uuid)
            claimedTiers[uuid] = payload.tiers.toMutableSet()
            println("Refreshed claimed tiers from server: ${payload.tiers}")
        }


        ClientPlayNetworking.registerGlobalReceiver(PlayerStatsPayload.ID) { payload, _ ->
            val uuid = UUID.fromString(payload.uuid)
            ClientStatsStorage.setStats(uuid, payload)
            println("Stored PlayerStatsPayload: $payload")
        }



        ClientPlayNetworking.registerGlobalReceiver<SaveSlotResponsePayload>(
            SaveSlotResponsePayload.ID
        ) { payload, context ->
            context.client().execute {

                println("SaveSlotResponsePayload received: $payload")

            }
        }


        ClientPlayNetworking.registerGlobalReceiver(
            ActiveSlotUpdatePayload.ID
        ) { payload, context ->
            context.client().execute {
                ClientSaveSlotCache.setActiveSlot(payload.slot)
                println("Active slot updated client-side to ${payload.slot}")

                val currentScreen = MinecraftClient.getInstance().currentScreen
                if (currentScreen is SaveSlotScreen) {
                    currentScreen.refreshSlotButtons()
                }
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(OpenCompetitiveHandbookPayload.ID) { _, context ->
            context.client().execute {
                MinecraftClient.getInstance().setScreen(CustomBookScreen())
            }
        }

    }

}


private fun handleRankResponseClient(
    payload: RankResponsePayload
) {
    try {
        println("Client received RankResponsePayload: $payload")
        val uuid = UUID.fromString(payload.uuid)

        ClientEloStorage.updateElo(payload.uuid, payload.elo)
        EloManager.handleEloResponseClient(uuid, payload.elo)

        if (!payload.silent) {
            EloManager.handleClientRankDisplay(payload)
        }

    } catch (e: Exception) {
        println("Error handling RankResponsePayload: ${e.message}")
        e.printStackTrace()
    }
}


fun handleRankResponse(payload: RankResponsePayload) {
    handleRankResponseClient(payload)
}

fun handleRankResponseServer(player: ServerPlayerEntity, payload: RankResponsePayload) {
    try {
        println("Server handling RankResponsePayload: $payload")
        val uuid = UUID.fromString(payload.uuid)

        if (uuid == player.uuid) {
            pendingNameTagUpdates[uuid] = player
            println("Added ${player.name.string} to pendingNameTagUpdates")
        }

        EloManager.handleEloResponseServer(uuid, payload.elo)


    } catch (e: Exception) {
        println("Error handling RankResponsePayload on server: ${e.message}")
        e.printStackTrace()
    }
}


fun handleSilentRankUpdate(payload: RankResponsePayload) {
    handleRankResponseClient(payload)
}


fun syncFriendlyBattleToClient(player: ServerPlayerEntity, isFriendly: Boolean) {
    val jsonData = buildJsonObject {
        put("type", "friendly_battle_sync")
        put("value", isFriendly)
    }.toString()

    val payload = RawJsonPayload(jsonData)
    ServerPlayNetworking.send(player, payload)
}


private fun handleRawJsonClientSide(payload: RawJsonPayload) {
    try {
        // val json = Json.parseToJsonElement(payload.json).jsonObject
        val jsonObject = Json.decodeFromString<JsonObject>(payload.json)
        val type = jsonObject["type"]?.jsonPrimitive?.content ?: run {
            println("Client error: Missing 'type' in payload: ${payload.json}")
            return
        }


        when (type) {
            "leaderboard_response" -> {
                val leaderboard =
                    jsonObject["leaderboard"]?.jsonArray
                        ?: throw IllegalArgumentException("Missing 'leaderboard' field")


                LeaderboardData.latestLeaderboard.clear()
                leaderboard.withIndex().forEach { (index, jsonElement) ->
                    val colorPrefix = when (index) {
                        0 -> "§6§l"  // Gold
                        1 -> "§7§l"  // Silver
                        2 -> "§3§l"  // Bronze
                        else -> "§f"
                    }


                    val obj = jsonElement.jsonObject
                    val name = obj["name"]?.jsonPrimitive?.content ?: "Unknown"
                    val elo = obj["elo"]?.jsonPrimitive?.int ?: 0
                    val tier = obj["tier"]?.jsonPrimitive?.content ?: "Unknown"


                    LeaderboardData.latestLeaderboard.add(
                        LeaderboardEntry(name, elo, tier, colorPrefix)
                    )
                }


                val client = MinecraftClient.getInstance()
                val uuid = client.player?.uuid

                if (uuid != null) {
                    pendingLeaderboardCallbacks.remove(uuid)?.invoke()
                }


                val leaderboardText = LeaderboardData.latestLeaderboard.withIndex().joinToString("\n") { (index, it) ->
                    val rankColor = when (index) {
                        0 -> "§6§l"  // Gold
                        1 -> "§7§l"  // Silver
                        2 -> "§3§l"  // Bronze
                        else -> "§f"
                    }
                    "${rankColor}#${index + 1}. ${it.name}§r ${it.tier}: §c§l${it.elo}§r"
                }
                MinecraftClient.getInstance().player?.sendMessage(
                    Text.literal("§e§l--- Leaderboard ---§r\n$leaderboardText")
                )
                println("Displayed leaderboard with ${leaderboard.size} entries")
            }

            /*"trigger_leaderboard_display" -> {
                val x = jsonObject["x"]?.jsonPrimitive?.int ?: return
                val y = jsonObject["y"]?.jsonPrimitive?.int ?: return
                val z = jsonObject["z"]?.jsonPrimitive?.int ?: return

                val pos = BlockPos(x, y, z)
                requestAndDisplayLeaderboardAtClient(pos)
            }*/



            "get_elo" -> {
                println("Client received unexpected get_elo — ignoring.")
                /* val uuid = jsonObject["uuid"]?.jsonPrimitive?.content
                println("Client processing GetElo request for UUID: $uuid")


               if (uuid != null) {
                   val playerName = getPlayerNameFromUuid(uuid) ?: return

                   val responseJson = buildJsonObject {
                       put("type", "elo_response")
                       put("uuid", uuid)
                       put("name", playerName)
                   }.toString()

                   println("Sending RawJsonPayload response to server: $responseJson")

                   ClientPlayNetworking.send(RawJsonPayload(responseJson))

                   println("Client sent elo_response: UUID=$uuid, Name=$playerName")
               }*/
            }

            // ... other handlers ...


            "update_elo" -> {
                val uuid = jsonObject["uuid"]?.jsonPrimitive?.content
                val elo = jsonObject["elo"]?.jsonPrimitive?.int
                if (uuid != null && elo != null) {
                    println("Received updated ELO for $uuid: $elo")



                    val clientPlayer = MinecraftClient.getInstance().player
                    if (clientPlayer != null && clientPlayer.uuidAsString == uuid) {
                        val tierName = getTierName(elo)
                        clientPlayer.sendMessage(
                            Text.literal("Your rank has been updated to $tierName: §c§l$elo")
                        )
                    }
                }
            }

            "friendly_battle_sync" -> {

                val isFriendly = jsonObject["value"]?.jsonPrimitive?.booleanOrNull ?: run {
                    println("Client error: Invalid or missing 'value' in friendly_battle_sync payload")
                    return
                }


                val client = MinecraftClient.getInstance()
                val player = client.player ?: return


                FriendlyBattleManager.cacheSetting(player.uuid, isFriendly)

                println("Updated friendly battle setting to: $isFriendly")
            }


            "tournament_signup_status" -> {
                val isSignedUp = jsonObject["value"]?.jsonPrimitive?.booleanOrNull ?: false
                val uuid = MinecraftClient.getInstance().player?.uuid ?: return
                TournamentManagerClient.cacheSignupStatus(uuid, isSignedUp)
            }


            "tournament_info" -> {
                val isSignedUp = jsonObject["signedUp"]?.jsonPrimitive?.booleanOrNull ?: false
                val uuid = MinecraftClient.getInstance().player?.uuid ?: return
                TournamentManagerClient.cacheSignupStatus(uuid, isSignedUp)

                val ruleset = jsonObject["ruleset"]?.jsonPrimitive?.content ?: "Unknown"
                val startTime = jsonObject["startTime"]?.jsonPrimitive?.content ?: "TBD"

                if (ruleset == "None" || startTime == "Not set") {
                    TournamentManagerClient.clearTournamentCache()
                    println("Tournament cleared client-side")
                } else {
                    TournamentManagerClient.cacheTournamentInfo(
                        ruleset = ruleset,
                        startTime = startTime,
                        status = jsonObject["status"]?.jsonPrimitive?.content ?: "Unknown"
                    )
                    println("Cached full tournament info: ruleset=$ruleset, startTime=$startTime, signedUp=$isSignedUp")
                }
            }

            "claim_tier_result" -> {
                val tier = jsonObject["tier"]?.jsonPrimitive?.content ?: return
                val success = jsonObject["success"]?.jsonPrimitive?.booleanOrNull ?: false
                val message = jsonObject["message"]?.jsonPrimitive?.content ?: "No message"

                val clientPlayer = MinecraftClient.getInstance().player ?: return
                clientPlayer.sendMessage(Text.literal(message))

                if (success) {
                    val uuid = clientPlayer.uuid
                    claimedTiers.getOrPut(uuid) { mutableSetOf() }.add(tier)
                }
            }


            "save_slots_response" -> {
                val slotArray = jsonObject["slots"]?.jsonArray ?: return
                val uuid = MinecraftClient.getInstance().player?.uuid ?: return


                ClientSaveSlotCache.clear()
                val usedSlots = mutableSetOf<Int>()


                slotArray.forEach { element ->
                    val obj = element.jsonObject
                    val slot = obj["slot"]?.jsonPrimitive?.int ?: return@forEach
                    val lastSaved = obj["lastSaved"]?.jsonPrimitive?.long ?: return@forEach



                    usedSlots.add(slot)


                    ClientSaveSlotCache.updateSlot(slot, lastSaved)
                }



                val currentScreen = MinecraftClient.getInstance().currentScreen
                if (currentScreen is SaveSlotScreen) {
                    currentScreen.refreshSlotButtons()
                }

                println("Refreshed client slot cache from server.")
            }

            "shop_metadata_sync" -> {
                val metadataJson = jsonObject["metadata"]?.jsonObject ?: return
                ShopMetadataRegistry.metadataByCategory.clear()

                for ((categoryName, metaElement) in metadataJson) {
                    val meta = metaElement.jsonObject
                    val requiredTier = meta["requiredTierLevel"]?.jsonPrimitive?.intOrNull ?: 0
                    ShopMetadataRegistry.metadataByCategory[categoryName.lowercase()] = mapOf(
                        "requiredTierLevel" to requiredTier
                    )
                }

                println("Synced ShopMetadataRegistry with ${ShopMetadataRegistry.metadataByCategory.size} entries.")
            }


            else -> {
                println("Unhandled JSON message type: $type")
            }
        }
    } catch (e: Exception) {
        println("Error handling RawJsonPayload on client: ${e.message}")
        e.printStackTrace()
    }
}

private fun getStoredClientElo(uuid: String): Int? {

    return ClientEloStorage.getElo(uuid)
}


private fun getOtherPlayerCachedElo(uuid: String): Int? {

    return ClientEloStorage.getElo(uuid)
}


private fun getPlayerNameFromUuid(uuidStr: String): String? {
    return MinecraftClient.getInstance().networkHandler?.playerList
        ?.find { it.profile.id.toString() == uuidStr }
        ?.profile?.name
}


fun requestPlayerElo(
    player: ServerPlayerEntity,
    uuid: UUID,
    isSelfRequest: Boolean = true,
    silent: Boolean = false
) {
    println("requestPlayerElo: Looking up stats for $uuid directly on server")

    val stats = getPlayerStats(uuid)
    if (stats != null) {
        val response = RankResponsePayload(uuid.toString(), stats.name, stats.elo, silent)

        EloManager.handleEloResponseServer(uuid, stats.elo)
        println("called elomanager.handleEloResponseServer")

        ServerPlayNetworking.send(player, response)
        println("Sent RankResponsePayload: $response")
    } else {
        println("No stats found for $uuid — cannot send RankResponsePayload")
    }
}

/*fun requestLeaderboard(player: ServerPlayerEntity?) {
    val json = buildJsonObject {
        put("type", "get_leaderboard")
    }.toString()


    if (player != null) {
        ServerPlayNetworking.send(player, RawJsonPayload(json))
    }
}*/ //might be crashing shit

/*
fun requestLeaderboardFromClient() {
    val json = buildJsonObject {
        put("type", "get_leaderboard")
    }.toString()

    ClientPlayNetworking.send(RawJsonPayload(json))
}

*/

fun requestAndDisplayLeaderboard(pos: BlockPos, player: ServerPlayerEntity) {
    pendingLeaderboardCallbacks[player.uuid] = {
        displayLeaderboardAsFloatingText(pos.x, pos.y, pos.z)
    }

    // requestLeaderboard(player)
}


fun sendEloUpdateToClient(
    player: ServerPlayerEntity,
    uuid: UUID, newElo: Int,
    silent: Boolean = false,
    reset: Boolean = false
) {
    val response = RankResponsePayload(
        uuid.toString(),
        player.name.string,
        newElo,
        silent,
        reset = reset
    )
    println("Sending RankResponsePayload to ${player.name.string}, silent=$silent, reset=$reset")
    ServerPlayNetworking.send(player, response)

}

fun triggerLeaderboardDisplayOnServer(pos: BlockPos) {
    val offsetPos = pos.up(2)
    leaderboardPosition = offsetPos
    displayLeaderboardAsFloatingText(offsetPos.x, offsetPos.y, offsetPos.z)

    LeaderboardManager.addLeaderboard(pos)

}







