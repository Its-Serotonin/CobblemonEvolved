package com.serotonin

import com.serotonin.common.client.gui.saveslots.SaveSlotScreen
import com.serotonin.common.client.networking.ClientStatsStorage
import com.serotonin.common.client.screenshots.ScreenshotHelper
import com.serotonin.common.client.screenshots.ScreenshotManager
import com.serotonin.common.networking.PlayerDataSyncNetworkingClient
import com.serotonin.common.registries.ClientEntitiesRenderer
import com.serotonin.common.registries.ShopScreenTracker
import com.serotonin.common.saveslots.ClientSaveSlotCache
import com.serotonin.common.saveslots.SaveSlotKeybinds
import com.serotonin.common.saveslots.SaveSlotScreenOpener
import com.serotonin.common.tourneys.TournamentManagerClient
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.MinecraftClient


@Environment(EnvType.CLIENT)
class CobblemonevolvedClient : ClientModInitializer {
    override fun onInitializeClient() {

        ShopScreenTracker.init()
        PlayerDataSyncNetworkingClient.registerPayloads()
        ClientEntitiesRenderer.register()

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            val player = MinecraftClient.getInstance().player ?: return@register
            val uuid = player.uuid

            println("Client disconnect cleanup for $uuid")
            TournamentManagerClient.clearSignupCache()
            ClientStatsStorage.clearStats(uuid)
        }

        SaveSlotKeybinds.register()

        WorldRenderEvents.END.register { context ->
            if (ScreenshotManager.pendingSlotCountdown > 0) {
                ScreenshotManager.pendingSlotCountdown--
            }

            if (ScreenshotManager.pendingSlotCountdown == 0) {
                ScreenshotManager.pendingSlotCountdown = -1

                val slot = ScreenshotManager.pendingSlotForScreenshot
                ScreenshotManager.pendingSlotForScreenshot = null

                if (slot != null) {
                    ScreenshotHelper.captureAndSaveScreenshot(slot) {

                        MinecraftClient.getInstance().execute {
                            MinecraftClient.getInstance().setScreen(SaveSlotScreen())
                            ScreenshotManager.scheduleGuiRefresh(2)
                        }
                    }
                } else {
                    println("pendingSlotCountdown reached 0, but no slot was set.")
                }
            }
        }


        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (SaveSlotKeybinds.openSaveSlotKey.wasPressed() && client.currentScreen == null) {
                    SaveSlotScreenOpener.markOpened()
                    val activeSlot = ClientSaveSlotCache.getActiveSlot()

                    if (activeSlot != null && ScreenshotManager.pendingSlotForScreenshot == null) {
                        ScreenshotManager.pendingSlotForScreenshot = activeSlot
                        ScreenshotManager.pendingSlotCountdown = 1
                    } else {
                        client.setScreen(SaveSlotScreen())
                    }
                }

            if (ScreenshotManager.guiRefreshCountdown > 0) {
                ScreenshotManager.guiRefreshCountdown--
                if (ScreenshotManager.guiRefreshCountdown == 0) {
                    val screen = client.currentScreen
                    if (screen is SaveSlotScreen) {
                        screen.refreshSlotButtons()
                    }
                }
            }
        }

        println("Client Initialized!")
    }
}








