package com.serotonin.common.saveslots

import com.serotonin.common.client.gui.saveslots.SaveSlotScreen
import com.serotonin.common.client.screenshots.ScreenshotHelper
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW

object SaveSlotKeybinds {
    lateinit var openSaveSlotKey: KeyBinding

    fun register() {
        openSaveSlotKey = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.cobblemonevolved.open_save_slots",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F7,
                "category.cobblemonevolved"
            )
        )
    }
}

object SaveSlotScreenOpener {
    private var lastOpened: Long = 0
    private const val cooldownMillis = 500L

    fun canOpen(): Boolean {
        return System.currentTimeMillis() - lastOpened >= cooldownMillis
    }

    fun markOpened() {
        lastOpened = System.currentTimeMillis()
    }

    fun tryOpen() {
        if (!canOpen()) return
        markOpened()

        val mc = MinecraftClient.getInstance()
        val activeSlot = ClientSaveSlotCache.getActiveSlot()

        if (activeSlot != null) {
            mc.execute {
                ScreenshotHelper.captureAndSaveScreenshot(activeSlot) {
                    mc.execute {
                        mc.setScreen(SaveSlotScreen())
                    }
                }
            }
        } else {
            mc.setScreen(SaveSlotScreen())
        }
    }
}