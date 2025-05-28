package com.serotonin.common.saveslots

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore
import com.cobblemon.mod.common.api.storage.party.PartyPosition
import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.item.ItemStack
import net.minecraft.nbt.*
import net.minecraft.server.network.ServerPlayerEntity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import net.minecraft.registry.RegistryWrapper
import net.minecraft.registry.DynamicRegistryManager
import net.minecraft.registry.RegistryKeys
import net.minecraft.nbt.NbtCompound
import com.cobblemon.mod.common.api.storage.pc.PCStore
import com.cobblemon.mod.common.api.storage.pc.PCBox
import com.cobblemon.mod.common.api.storage.pc.PCPosition
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.ItemEntity
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.util.math.Box
import java.util.*
import dev.emi.trinkets.api.TrinketsApi
import dev.emi.trinkets.api.SlotReference
import net.minecraft.registry.Registries


data class PlayerSaveSlot(
    val uuid: UUID,
    val slot: Int,
    val inventoryData: ByteArray,
    val pokemonData: ByteArray,
    val pcData: ByteArray,
    val lastSaved: Long,
    val screenshotPath: String? = null,
    val backpackData: ByteArray = ByteArray(0),
    val trinketData: ByteArray = ByteArray(0)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlayerSaveSlot) return false

        return uuid == other.uuid &&
                slot == other.slot &&
                inventoryData.contentEquals(other.inventoryData) &&
                pokemonData.contentEquals(other.pokemonData) &&
                pcData.contentEquals(other.pcData) &&
                backpackData.contentEquals(other.backpackData) &&
                trinketData.contentEquals(other.trinketData) &&
                lastSaved == other.lastSaved &&
                screenshotPath == other.screenshotPath
    }

    override fun hashCode(): Int {
        var result = uuid.hashCode()
        result = 31 * result + slot
        result = 31 * result + inventoryData.contentHashCode()
        result = 31 * result + pokemonData.contentHashCode()
        result = 31 * result + pcData.contentHashCode()
        result = 31 * result + backpackData.contentHashCode()
        result = 31 * result + trinketData.contentHashCode()
        result = 31 * result + lastSaved.hashCode()
        result = 31 * result + (screenshotPath?.hashCode() ?: 0)
        return result
    }
}


// === INVENTORY ===

fun serializeInventory(player: ServerPlayerEntity): ByteArray {
    val inventoryList = NbtList()

    for (i in 0 until player.inventory.size()) {
        val stack = player.inventory.getStack(i)
        if (!stack.isEmpty) {
            val tag = stack.writeNbtCompat(player.server.registryManager)
            tag.putByte("Slot", i.toByte())
            inventoryList.add(tag)
        }
    }

    val root = NbtCompound()
    root.put("Inventory", inventoryList)

    val output = ByteArrayOutputStream()
    NbtIo.writeCompressed(root, output)
    return output.toByteArray()
}

fun deserializeInventory(player: ServerPlayerEntity, data: ByteArray) {
    try {
        println("Deserializing inventory for ${player.name.string}, ${data.size} bytes")

        val registry = player.server.registryManager
        val root = NbtIo.readCompressed(ByteArrayInputStream(data), NbtSizeTracker.ofUnlimitedBytes())
        val inventoryList = root.getList("Inventory", NbtElement.COMPOUND_TYPE.toInt())

        player.inventory.clear()

        for (i in 0 until inventoryList.size) {
            val tag = inventoryList.getCompound(i)
            val slot = tag.getByte("Slot").toInt() and 255


            if (!tag.contains("id")) {
                println("Skipping item in slot $slot — missing 'id'")
                continue
            }

            val optional = ItemStack.fromNbt(registry, tag)

            if (optional.isPresent) {
                println("    Slot $slot: ${optional.get().name.string}")
                player.inventory.setStack(slot, optional.get())
            } else {
                println("    Failed to load item in slot $slot (NBT malformed?)")
            }
        }

        player.inventory.markDirty()
    } catch (e: Exception) {
        println("Error deserializing inventory: ${e.message}")
        e.printStackTrace()
    }
}



// === PARTY ===

fun serializeParty(player: ServerPlayerEntity): ByteArray {
    val registryAccess = player.server.registryManager
    val partyStore = Cobblemon.storage.getParty(player)
    val pokemonList = NbtList()

    for (position in 0 until 6) {
        val partySlot = PartyPosition(position)
        val pokemon = partyStore[partySlot]
        if (pokemon != null) {
            try {
                val tag = pokemon.saveToNBT(registryAccess)
                tag.putInt("Slot", position)
                pokemonList.add(tag)
            } catch (e: Exception) {
                println("Failed to serialize Pokémon in party slot $position: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    val root = NbtCompound()
    root.put("Party", pokemonList)

    return try {
        val output = ByteArrayOutputStream()
        NbtIo.writeCompressed(root, output)
        output.toByteArray()
    } catch (e: Exception) {
        println("Error compressing party data: ${e.message}")
        e.printStackTrace()
        ByteArray(0)
    }
}

fun deserializeParty(player: ServerPlayerEntity, data: ByteArray) {
    try {
        println("Deserializing party for ${player.name.string}, ${data.size} bytes")

        val registryAccess = player.server.registryManager
        val root = NbtIo.readCompressed(ByteArrayInputStream(data), NbtSizeTracker.ofUnlimitedBytes())
        val pokemonList = root.getList("Party", NbtElement.COMPOUND_TYPE.toInt())

        val partyStore = Cobblemon.storage.getParty(player)
        for (i in 0 until 6) {
            partyStore.remove(PartyPosition(i))
        }

        for (i in 0 until pokemonList.size) {
            val tag = pokemonList.getCompound(i)
            val position = tag.getInt("Slot")
            val pokemon = Pokemon.loadFromNBT(registryAccess, tag)

            println("    Slot $position: ${pokemon.getDisplayName().string}")
            partyStore[PartyPosition(position)] = pokemon
        }
    } catch (e: Exception) {
        println("Error deserializing party: ${e.message}")
        e.printStackTrace()
    }
}

fun serializePC(player: ServerPlayerEntity): ByteArray {
    val registryAccess = player.server.registryManager
    val pcStore = Cobblemon.storage.getPC(player)

    return try {
        val tag = NbtCompound()
        pcStore.saveToNBT(tag, registryAccess)

        val output = ByteArrayOutputStream()
        NbtIo.writeCompressed(tag, output)
        output.toByteArray()
    } catch (e: Exception) {
        println("Error serializing PC store: ${e.message}")
        e.printStackTrace()
        ByteArray(0)
    }
}

fun deserializePC(player: ServerPlayerEntity, data: ByteArray) {
    if (data.isEmpty()) {
        println("PC data is empty for ${player.name.string} — skipping.")
        return
    }

    try {
        println("Deserializing PC for ${player.name.string}, ${data.size} bytes")

        val registryAccess = player.server.registryManager
        val tag = NbtIo.readCompressed(ByteArrayInputStream(data), NbtSizeTracker.ofUnlimitedBytes())

        val pcStore = Cobblemon.storage.getPC(player)
        pcStore.loadFromNBT(tag, registryAccess)

        println("    PC data loaded successfully")
    } catch (e: Exception) {
        println("Error deserializing PC: ${e.message}")
        e.printStackTrace()
    }
}

fun ItemStack.writeNbtCompat(registry: DynamicRegistryManager): NbtCompound {
    return this.encode(registry) as NbtCompound
}
/*
fun PCStore.clearAll() {
    for ((boxIndex, box) in boxes.withIndex()) {
        for ((slotIndex, pokemon) in box.withIndex()) {
            this.remove(pokemon)
        }
    }
}*/

fun PCStore.clearAll() {
    boxes.flatten().forEach { this.remove(it) }
}

fun getEquippedBackpack(player: ServerPlayerEntity): ItemStack? {
    val armorStack = player.inventory.getArmorStack(EquipmentSlot.CHEST.entitySlotId)
    if (!armorStack.isEmpty) {
        val id = Registries.ITEM.getId(armorStack.item)
        if (id.namespace == "sophisticatedbackpacks" && id.path.endsWith("_backpack")) {
            return armorStack
        }
    }

    var found: ItemStack? = null
    TrinketsApi.getTrinketComponent(player).ifPresent { component ->
        loop@ for ((_, slotMap) in component.inventory) {
            for ((_, inventory) in slotMap) {
                for (i in 0 until inventory.size()) {
                    val stack = inventory.getStack(i)
                    if (!stack.isEmpty) {
                        val id = Registries.ITEM.getId(stack.item)
                        if (id.namespace == "sophisticatedbackpacks" && id.path.endsWith("_backpack")) {
                            found = stack
                            break@loop
                        }
                    }
                }
            }
        }
    }

    return found.also {
        if (it == null) {
            println("No equipped backpack found in armor or trinket slots for ${player.name.string}")
        }
    }
}

fun serializeBackpack(player: ServerPlayerEntity, registry: RegistryWrapper.WrapperLookup): ByteArray {
    val armorStack = player.inventory.getArmorStack(EquipmentSlot.CHEST.entitySlotId)
    val trinketStack: ItemStack? = run {
        var found: ItemStack? = null
        TrinketsApi.getTrinketComponent(player).ifPresent { component ->
            outer@ for ((_, slotMap) in component.inventory) {
                for ((_, inventory) in slotMap) {
                    for (i in 0 until inventory.size()) {
                        val stack = inventory.getStack(i)
                        val id = Registries.ITEM.getId(stack.item)
                        if (id.namespace == "sophisticatedbackpacks" && id.path.endsWith("_backpack")) {
                            found = stack
                            break@outer
                        }
                    }
                }
            }
        }
        found
    }

    val (selectedStack, location) = when {
        armorStack.isEmpty && trinketStack == null -> return ByteArray(0)
        !armorStack.isEmpty && trinketStack == null -> armorStack to "armor"
        armorStack.isEmpty && trinketStack != null -> trinketStack to "trinket"
        else -> {
            val armorNbtSize = armorStack.writeNbtCompat(player.server.registryManager).toString().length
            val trinketNbtSize = trinketStack!!.writeNbtCompat(player.server.registryManager).toString().length
            if (armorNbtSize >= trinketNbtSize) armorStack to "armor" else trinketStack to "trinket"
        }
    }

    val nbtCompound = selectedStack.encode(registry) as? NbtCompound ?: return ByteArray(0)
    nbtCompound.putString("BackpackSlotLocation", location)

    val output = ByteArrayOutputStream()
    NbtIo.writeCompressed(nbtCompound, output)
    val byteArray = output.toByteArray()

    if (byteArray.size > 2048) {
        println("Serialized backpack for ${player.name.string} is ${byteArray.size} bytes — unusually large.")
    }

    return byteArray
}

fun deserializeBackpack(player: ServerPlayerEntity, data: ByteArray) {
    if (data.isEmpty()) return

    try {
        val registry = player.server.registryManager
        val tag = NbtIo.readCompressed(ByteArrayInputStream(data), NbtSizeTracker.ofUnlimitedBytes())


        if (!tag.contains("id")) {
            println("Skipping backpack load — missing 'id' tag")
            return
        }

        val optionalStack = ItemStack.fromNbt(registry, tag)
        if (!optionalStack.isPresent) {
            println("Failed to load backpack: optional stack was empty")
            return
        }

        val stack = optionalStack.get()
        val slotTag = tag.getString("BackpackSlotLocation")
        var placed = false


        player.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY)
        TrinketsApi.getTrinketComponent(player).ifPresent { component ->
            for ((_, slotMap) in component.inventory) {
                for ((_, inventory) in slotMap) {
                    for (i in 0 until inventory.size()) {
                        val s = inventory.getStack(i)
                        val id = Registries.ITEM.getId(s.item)
                        if (!s.isEmpty && id.namespace == "sophisticatedbackpacks" && id.path.endsWith("_backpack")) {
                            inventory.setStack(i, ItemStack.EMPTY)
                        }
                    }
                }
            }
        }


        when (slotTag) {
            "armor" -> {
                player.equipStack(EquipmentSlot.CHEST, stack)
                placed = true
                println("Placed backpack in armor slot")
            }
            "trinket" -> {
                TrinketsApi.getTrinketComponent(player).ifPresent { component ->
                    val inventory = component.inventory["chest"]?.get("back")
                    if (inventory != null && inventory.size() > 0 && inventory.getStack(0).isEmpty) {
                        inventory.setStack(0, stack)
                        placed = true
                        println("Placed backpack in trinket slot chest/back")
                    }
                }
            }
        }


        if (!placed && slotTag == "trinket") {
            TrinketsApi.getTrinketComponent(player).ifPresent { component ->
                outer@ for ((_, slotMap) in component.inventory) {
                    for ((_, inventory) in slotMap) {
                        for (i in 0 until inventory.size()) {
                            if (inventory.getStack(i).isEmpty) {
                                inventory.setStack(i, stack)
                                placed = true
                                println("Placed backpack in fallback trinket slot")
                                break@outer
                            }
                        }
                    }
                }
            }
        }


        if (!placed) {
            println("Couldn't place backpack — dropping at feet")
            clearNearbyUnplacedBackpacks(player)
            val world = player.world
            val dropPos = player.eyePos.add(player.rotationVector.multiply(0.25))
            val itemEntity = ItemEntity(world, dropPos.x, dropPos.y, dropPos.z, stack)
            itemEntity.customName = Text.of("Unplaced Backpack")
            itemEntity.isCustomNameVisible = false
            itemEntity.addCommandTag("UNPLACED_BACKPACK")
            world.spawnEntity(itemEntity)
        }
    } catch (e: Exception) {
        println("Failed to deserialize backpack: ${e.message}")
        e.printStackTrace()
    }
}

fun serializeTrinkets(player: ServerPlayerEntity): ByteArray {
    val root = NbtCompound()
    val registry = player.server.registryManager
    var result: ByteArray = ByteArray(0)

    var foundAny = false

    TrinketsApi.getTrinketComponent(player).ifPresent { component ->
        val slotsNbt = NbtCompound()
        val inventoryMap = component.inventory

        for ((group, slotMap) in inventoryMap) {
            for ((slotName, inventory) in slotMap) {
                val slotNbt = NbtList()
                for (i in 0 until inventory.size()) {
                    val stack = inventory.getStack(i)
                    if (stack.isEmpty) continue

                    val tag = NbtCompound()
                    tag.putInt("Index", i)
                    tag.put("Item", stack.encode(registry))
                    slotNbt.add(tag)
                }
                if (slotNbt.isNotEmpty()) {
                    slotsNbt.put("$group/$slotName", slotNbt)
                    foundAny = true
                }
            }
        }

        if (foundAny) {
            root.put("Trinkets", slotsNbt)
            val output = ByteArrayOutputStream()
            NbtIo.writeCompressed(root, output)
            result = output.toByteArray()

            val playerName = player.name.string
            println("Saved trinket data for $playerName: ${result.size} bytes")
            if (result.size > 2048) {
                println("Serialized trinkets for $playerName are ${result.size} bytes — may be excessive.")
            }
        }
    }

    if (!foundAny) {
        println("No trinkets to serialize — returning empty array.")
    }

    return result
}

fun deserializeTrinkets(player: ServerPlayerEntity, data: ByteArray) {
    if (data.isEmpty()) {
        println("Trinket data is empty for ${player.name.string} — skipping.")
        return
    }

    try {
        val registry = player.server.registryManager
        val root = NbtIo.readCompressed(ByteArrayInputStream(data), NbtSizeTracker.ofUnlimitedBytes())
        val slotsNbt = root.getCompound("Trinkets")

        TrinketsApi.getTrinketComponent(player).ifPresent { component ->

            for ((_, slotMap) in component.inventory) {
                for ((_, inventory) in slotMap) {
                    for (i in 0 until inventory.size()) {
                        inventory.setStack(i, ItemStack.EMPTY)
                    }
                }
            }

            for (groupSlot in slotsNbt.keys) {
                val listTag = slotsNbt.get(groupSlot) as? NbtList ?: continue

                val parts = groupSlot.split("/")
                if (parts.size != 2) continue
                val (group, slot) = parts

                val inventory = component.inventory[group]?.get(slot)
                if (inventory == null) {
                    println("Missing trinket slot: $group/$slot — skipping.")
                    continue
                }

                for (entry in listTag) {
                    val entryTag = entry as? NbtCompound ?: continue
                    val itemTag = entryTag.getCompound("Item")

                    if (!itemTag.contains("id")) {
                        println("Skipping trinket — missing 'id' tag")
                        continue
                    }

                    val index = entryTag.getInt("Index")
                    val optional = ItemStack.fromNbt(registry, itemTag)
                    if (!optional.isPresent || index !in 0 until inventory.size()) continue

                    val stack = optional.get()
                    val current = inventory.getStack(index)

                    println("    Trying to insert trinket: $group/$slot index $index (${stack.name.string})")

                    if (current.isEmpty) {
                        inventory.setStack(index, stack)
                        println("Inserted trinket into $group/$slot at index $index")
                    } else {
                        println("Skipped trinket $group/$slot at index $index — already occupied")
                    }
                }
            }
        }
    } catch (e: Exception) {
        println("Failed to deserialize trinkets: ${e.message}")
        e.printStackTrace()
    }
}

fun clearNearbyDroppedItems(server: MinecraftServer, player: ServerPlayerEntity) {
    val radius = 100.0
    val center = player.pos

    var totalCleared = 0

    for (world in server.worlds) {
        val box = Box(
            center.x - radius, center.y - radius, center.z - radius,
            center.x + radius, center.y + radius, center.z + radius
        )
        val items = world.getEntitiesByClass(ItemEntity::class.java, box) { true }
        items.forEach { it.remove(Entity.RemovalReason.DISCARDED) }
        totalCleared += items.size
    }

    println("Cleared $totalCleared dropped items across all dimensions for ${player.name.string}")
}


fun isPlayerMounted(player: ServerPlayerEntity): Boolean {
    return player.hasVehicle() || player.passengerList.any {
        !it.commandTags.contains("RANKEDPLAYERNAMETAG") && !it.commandTags.contains("RANKTAG")
    }
}

fun isSlotEmpty(slot: PlayerSaveSlot?): Boolean {
    return slot?.isMeaningless() ?: true
}

fun isMeaningless(data: ByteArray): Boolean {
    if (data.isEmpty()) return true
    return try {
        val tag = NbtIo.readCompressed(ByteArrayInputStream(data), NbtSizeTracker.ofUnlimitedBytes())
        when {
            tag.isEmpty -> true
            tag.contains("Trinkets") && tag.getCompound("Trinkets").isEmpty -> true
            tag.contains("Inventory") && tag.getList("Inventory", NbtElement.COMPOUND_TYPE.toInt()).isEmpty() -> true
            tag.contains("Party") && tag.getList("Party", NbtElement.COMPOUND_TYPE.toInt()).isEmpty() -> true
            else -> false
        }
    } catch (e: Exception) {
        true
    }
}

fun PlayerSaveSlot.isMeaningless(): Boolean {
    return isMeaningless(inventoryData) &&
            isMeaningless(pokemonData) &&
            isMeaningless(pcData) &&
            isMeaningless(backpackData) &&
            isMeaningless(trinketData)
}

fun clearNearbyUnplacedBackpacks(player: ServerPlayerEntity, radius: Double = 4.0) {
    val world = player.world
    val box = Box(
        player.x - radius, player.y - radius, player.z - radius,
        player.x + radius, player.y + radius, player.z + radius
    )

    val toRemove = world.getEntitiesByClass(ItemEntity::class.java, box) {
        it.commandTags.contains("UNPLACED_BACKPACK")
    }

    toRemove.forEach { it.remove(Entity.RemovalReason.DISCARDED) }

    if (toRemove.isNotEmpty()) {
        println("Removed ${toRemove.size} unplaced backpacks near ${player.name.string}")
    }
}

fun clearEquippedBackpackAndTrinkets(player: ServerPlayerEntity) {
    if (getEquippedBackpack(player) != null) {
        player.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY)
    }

    TrinketsApi.getTrinketComponent(player).ifPresent { component ->
        val hasTrinkets = component.inventory.values
            .flatMap { it.values }
            .any { inv -> (0 until inv.size()).any { !inv.getStack(it).isEmpty } }

        if (hasTrinkets) {
            for ((_, slotMap) in component.inventory) {
                for ((_, inventory) in slotMap) {
                    for (i in 0 until inventory.size()) {
                        inventory.setStack(i, ItemStack.EMPTY)
                    }
                }
            }
        }
    }
}