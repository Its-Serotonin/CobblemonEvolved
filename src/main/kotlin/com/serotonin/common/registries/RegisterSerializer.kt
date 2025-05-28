package com.serotonin.common.registries

import com.serotonin.common.saveslots.FIXED_BACKPACK_DYE_SERIALIZER
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

object RegisterSerializer {

    val BACKPACK_DYE_FIX_ID: Identifier? = Identifier.of("cobblemonevolved", "backpack_dye_fixed")
    
    fun registerFixedBackpackDyeSerializer() {
        Registry.register(Registries.RECIPE_SERIALIZER, BACKPACK_DYE_FIX_ID, FIXED_BACKPACK_DYE_SERIALIZER)
    }
}