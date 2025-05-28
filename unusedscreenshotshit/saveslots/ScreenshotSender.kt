package com.serotonin.common.saveslots

import com.serotonin.common.networking.PlayerDataSyncNetworkingClient
import com.serotonin.common.networking.SaveSlotRequestPayload
import com.serotonin.common.saveslots.ScreenshotHelper
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen

object SaveSlotSender {
    fun sendSaveSlotWithScreenshot(slot: Int) {
        ScreenshotHelper.captureAndSaveScreenshot(slot) { screenshotPath ->
            MinecraftClient.getInstance().execute {
                val payload = SaveSlotRequestPayload(
                    slot = slot,
                    action = SaveSlotRequestPayload.Action.SAVE,
                )
                PlayerDataSyncNetworkingClient.sendSaveSlotRequest(payload)
            }
        }
    }
}