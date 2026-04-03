package com.serotonin.common.registries

import com.google.common.collect.ImmutableSet
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import net.minecraft.village.VillagerProfession

object MerchantProfessions {
    lateinit var LOBBY_VENDOR: VillagerProfession
        private set

    fun register() {
        LOBBY_VENDOR = Registry.register(
            Registries.VILLAGER_PROFESSION,
            Identifier.of("cobblemonevolved", "lobby_vendor"),
            VillagerProfession("lobby_vendor", { false }, { false }, ImmutableSet.of(), ImmutableSet.of(), null)
        )
    }
}