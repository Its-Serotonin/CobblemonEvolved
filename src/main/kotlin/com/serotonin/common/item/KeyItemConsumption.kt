package com.serotonin.common.item

import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier

fun consumeKeyItem(player: ServerPlayerEntity, itemId: Identifier): Boolean {
    val inventory = player.inventory
    for (i in 0 until inventory.size()) {
        val stack = inventory.getStack(i)
        if (!stack.isEmpty && Registries.ITEM.getId(stack.item) == itemId) {
            stack.decrement(1)
            return true
        }
    }
    return false
}
