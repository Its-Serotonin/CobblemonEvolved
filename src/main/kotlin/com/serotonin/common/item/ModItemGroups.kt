package com.serotonin.common.item

import com.serotonin.Cobblemonevolved.MOD_ID
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.text.Text
import net.minecraft.util.Identifier

object ModItemGroups {
    val COBBLEMON_EVOLVED_ITEMS_GROUP: ItemGroup? = Registry.register(
        Registries.ITEM_GROUP,
        Identifier.of(MOD_ID, "cobblemon_evolved_items"),
        FabricItemGroup.builder().icon { ItemStack(ModItems.COMPETITIVE_HANDBOOK) }
            .displayName(Text.translatable("itemgroup.cobblemonevolved.cobblemon_evolved_items"))
            .entries { displayContext: ItemGroup.DisplayContext?, entries: ItemGroup.Entries? ->
                entries?.add(ModItems.COMPETITIVE_HANDBOOK)
                entries?.add(ModItems.BEADS_OF_RUIN)
                entries?.add(ModItems.METAL_ALLOY)
            }.build()
    )

    fun registerCEItemGroups() {
        println("registering shit")
    }
}