package com.serotonin.common.registries


import com.serotonin.common.saveslots.ClientShopCategoryContext
import fr.harmex.cobbledollars.common.client.gui.screen.ShopScreen
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen

object ShopScreenTracker {

    private var lastScreen: Screen? = null

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { client: MinecraftClient ->
            val current = client.currentScreen
            if (lastScreen is ShopScreen && current != lastScreen) {
                ClientShopCategoryContext.set(null)
            }
            lastScreen = current
        }
    }
}