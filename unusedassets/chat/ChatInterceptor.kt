package com.serotonin.common.chat

import com.serotonin.mixin.ChatHudAccessor
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents

object ChatInterceptor {
    private val seenMessages = mutableSetOf<String>()

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val hud = client.inGameHud.chatHud
            val accessor = hud as ChatHudAccessor
            val messages = accessor.messages

            // Use a copy to avoid ConcurrentModificationException
            val toRemove = mutableListOf<String>()

            for (line in messages.toList()) {
                val string = line.content.string
                if (!string.contains("<") || seenMessages.contains(string)) continue

                seenMessages += string
                toRemove += string

                val parsed = parseMiniMessageLite(string)
                client.execute {
                    hud.addMessage(parsed)
                }
            }

            // Remove originals
            client.execute {
                accessor.messages.removeIf { it.content.string in toRemove }
            }
        }
    }
}