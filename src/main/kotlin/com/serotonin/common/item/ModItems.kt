package com.serotonin.common.item

import com.github.d0ctorleon.mythsandlegends.cobblemon.spawning.condition.keyitem.KeyItemCondition
import com.github.d0ctorleon.mythsandlegends.cobblemon.spawning.condition.keyitem.KeyItemConditions
import com.github.d0ctorleon.mythsandlegends.items.KeyItem
import com.github.d0ctorleon.mythsandlegends.utils.ForceSpawningUtils
import com.serotonin.Cobblemonevolved.MOD_ID
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.world.World
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import com.serotonin.common.client.gui.competitivehandbook.CustomBookScreen
import com.serotonin.common.saveslots.getEquippedBackpack
import dev.emi.trinkets.api.TrinketsApi
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.ContainerComponent
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.server.network.ServerPlayerEntity

object ModItems {

        val COMPETITIVE_HANDBOOK: Item? = registerItem("competitive_handbook",
            CompetitiveHandbookItem(Item.Settings().maxCount(1)))

        val BEADS_OF_RUIN: Item? = registerItem("beads_of_ruin",
        BeadsOfRuinItem(Item.Settings().maxCount(64)))

        val METAL_ALLOY: Item? = registerItem("metal_alloy",
            MetalAlloyItem(Item.Settings().maxCount(64)))


        private fun registerItem(name: String?, item: Item?): Item? {
            return Registry.register(Registries.ITEM, Identifier.of(MOD_ID, name), item)
        }


fun registerCEModItems() {
            println("Registering Mod Items for $MOD_ID")
        }
    }


class CompetitiveHandbookItem(settings: Item.Settings) : Item(settings) {
    override fun appendTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltip: MutableList<Text>,
        type: TooltipType
    ) {
        tooltip.add(Text.translatable("tooltip.cobblemonevolved.competitive_handbook").formatted(Formatting.GRAY))
    }

    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {

        if (!world.isClient) {
            return TypedActionResult.pass(user.getStackInHand(hand))
        }

        MinecraftClient.getInstance().execute {
            MinecraftClient.getInstance().setScreen(CustomBookScreen())
        }
        return TypedActionResult.success(user.getStackInHand(hand))
    }
}

class BeadsOfRuinItem(settings: Settings) : KeyItem(settings, "beads_of_ruin") {
    override fun appendTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltip: MutableList<Text>,
        type: TooltipType
    ) {
        tooltip.add(Text.translatable("tooltip.cobblemonevolved.beads_of_ruin").formatted(Formatting.WHITE))
    }

    override fun getName(stack: ItemStack): Text {
        return Text.translatable(this.translationKey).formatted(Formatting.LIGHT_PURPLE)
    }

    /*
    override fun use(world: World, player: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val stack = player.getStackInHand(hand)

        if (!world.isClient && player is ServerPlayerEntity) {

            ForceSpawningUtils.forceSpawnv1(world, player, hand, "beads_of_ruin")
        }

        return TypedActionResult.success(stack, world.isClient)
    }
*/
}

class MetalAlloyItem(settings: Item.Settings) : Item(settings) {
    override fun appendTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltip: MutableList<Text>,
        type: TooltipType
    ) {
        tooltip.add(Text.translatable("tooltip.cobblemonevolved.metal_alloy").formatted(Formatting.WHITE))
    }
}


fun hasHandbookAnywhere(player: ServerPlayerEntity): Boolean {
    val handbook = ModItems.COMPETITIVE_HANDBOOK ?: run {
        println("Handbook item not registered")
        return false
    }
    val handbookId = Registries.ITEM.getId(handbook).toString()

    val inventoryCheck = player.inventory.main.any { it.item == handbook }
    val trinketCheck = TrinketsApi.getTrinketComponent(player).map {
        it.inventory.values.flatMap { group -> group.values }
            .any { inv -> (0 until inv.size()).any { slot -> inv.getStack(slot).item == handbook } }
    }.orElse(false)

    val backpacks = player.inventory.main.filter { stack ->
        val id = Registries.ITEM.getId(stack.item)
        id.namespace == "sophisticatedbackpacks" && id.path.endsWith("_backpack")
    } + getEquippedBackpack(player)?.takeIf { !it.isEmpty }

    val backpackContainsBook = backpacks.any { stack ->
        val container = try {
            stack?.get(DataComponentTypes.CONTAINER)
        } catch (e: Exception) {
            println("Failed to read backpack container: ${e.message}")
            null
        } ?: return@any false

        container.iterateNonEmpty().any {
            Registries.ITEM.getId(it.item).toString() == handbookId
        }
    }

    println("hasHandbookAnywhere → Inventory=$inventoryCheck, Trinkets=$trinketCheck, Backpack=$backpackContainsBook")

    return inventoryCheck || trinketCheck || backpackContainsBook
}