package com.serotonin.common.item

import com.mojang.serialization.MapCodec
import com.serotonin.Cobblemonevolved.MOD_ID
import net.minecraft.loot.condition.LootCondition
import net.minecraft.loot.condition.LootConditionType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

object ModLootConditions {
    val HAS_NO_HANDBOOK: LootConditionType = LootConditionType(HasNoHandbookLootCondition.CODEC)

    fun register() {
        Registry.register(
            Registries.LOOT_CONDITION_TYPE,
            Identifier.of(MOD_ID, "has_no_handbook"),
            HAS_NO_HANDBOOK
        )
    }
}