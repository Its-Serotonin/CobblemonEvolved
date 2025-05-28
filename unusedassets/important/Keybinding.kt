package com.serotonin.common.saveslots

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
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
        return java.lang.System.currentTimeMillis() - lastOpened >= cooldownMillis
    }

    fun markOpened() {
        lastOpened = java.lang.System.currentTimeMillis()
    }
}
