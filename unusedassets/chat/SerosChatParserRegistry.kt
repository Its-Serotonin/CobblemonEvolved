package com.serotonin.common.registries

import com.serotonin.common.chat.parseMiniMessageLite
import net.minecraft.client.MinecraftClient
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents

object RegisterChatParser{

    fun register() {


        fun handleIncomingChat(message: String) {
            val parsed = parseMiniMessageLite(message)
            MinecraftClient.getInstance().inGameHud.chatHud.addMessage(parsed)
        }

        fun showChatMessage(raw: String) {
            val parsed = parseMiniMessageLite(raw)
            MinecraftClient.getInstance().inGameHud.chatHud.addMessage(parsed)
        }
    }
}