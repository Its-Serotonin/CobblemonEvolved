package com.serotonin.common.item

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import com.mojang.serialization.MapCodec
import com.mojang.serialization.MapLike
import net.minecraft.loot.condition.LootCondition
import net.minecraft.loot.condition.LootConditionType
import net.minecraft.loot.context.LootContext
import net.minecraft.loot.context.LootContextParameters
import net.minecraft.server.network.ServerPlayerEntity
import java.util.stream.Stream

class HasNoHandbookLootCondition : LootCondition {
    override fun test(context: LootContext): Boolean {
        val player = context.get(LootContextParameters.THIS_ENTITY) as? ServerPlayerEntity ?: run {
            println("has_no_handbook: THIS_ENTITY is null or not a player")
            return true
        }
        return !hasHandbookAnywhere(player)
    }

    override fun getType(): LootConditionType {
        return ModLootConditions.HAS_NO_HANDBOOK
    }

    companion object {
        val CODEC: MapCodec<HasNoHandbookLootCondition> = MapCodec.unit(HasNoHandbookLootCondition())

    }
}
