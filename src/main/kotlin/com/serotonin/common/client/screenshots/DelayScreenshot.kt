package com.serotonin.common.client.screenshots

import com.serotonin.common.client.gui.saveslots.SaveSlotScreen
import com.serotonin.common.saveslots.ClientSaveSlotCache
import net.minecraft.client.MinecraftClient

object ScreenshotDelayedCapture {
    private var ticksRemaining = -1

    fun scheduleCapture() {
        ticksRemaining = 2
    }

    fun tick() {
        if (ticksRemaining > 0) {
            ticksRemaining--
        } else if (ticksRemaining == 0) {
            ticksRemaining = -1
            val activeSlot = ClientSaveSlotCache.getActiveSlot()
            if (activeSlot != null) {
                ScreenshotHelper.captureAndSaveScreenshot(activeSlot) {
                    MinecraftClient.getInstance().setScreen(SaveSlotScreen())
                }
            } else {
                MinecraftClient.getInstance().setScreen(SaveSlotScreen())
            }
        }
    }
}
