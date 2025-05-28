package com.serotonin.common.registries

import net.minecraft.util.Identifier
import java.lang.reflect.Field

@Suppress("UNCHECKED_CAST")
fun registerBeadsOfRuinKeyItem() {
    try {
        val clazz = Class.forName("com.github.d0ctorleon.mythsandlegends.items.Items")

        val itemsField: Field = clazz.getDeclaredField("MYTHS_AND_LEGENDS_ITEMS")
        itemsField.isAccessible = true
        val itemList = itemsField.get(null) as MutableList<Identifier>

        val id = Identifier.of("cobblemonevolved", "beads_of_ruin")
        if (!itemList.contains(id)) {
            itemList.add(id)
            println("Successfully injected beads_of_ruin into MythsAndLegends key items.")
        } else {
            println("beads_of_ruin already present in MYTHS_AND_LEGENDS_ITEMS.")
        }

    } catch (e: Exception) {
        println("Failed to inject beads_of_ruin into MythsAndLegends item registry:")
        e.printStackTrace()
    }
}