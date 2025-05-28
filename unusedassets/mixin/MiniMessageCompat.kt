package com.serotonin.common.chat

import com.google.gson.JsonParser
import com.mojang.serialization.Dynamic
import com.mojang.serialization.JsonOps
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.text.Text
import net.minecraft.text.TextCodecs
/*
object MiniMessageCompat {
    fun tryDeserializeMiniMessage(raw: String): Text? {
        return try {
            val mini = MiniMessage.miniMessage()
            val component = mini.deserialize(raw)
            val json = GsonComponentSerializer.gson().serialize(component)
            val jsonElement = JsonParser.parseString(json)
            val result = TextCodecs.CODEC.decode(Dynamic(JsonOps.INSTANCE, jsonElement))
            result.result().map { it.first }.orElse(null)
        } catch (e: Exception) {
            println("[MiniMessageCompat] Failed to parse message: $raw")
            null
        }
    }
}*/

object MiniMessageCompat {
    fun tryDeserializeMiniMessage(raw: String): Text? {
        return try {
            val mini = MiniMessage.miniMessage()
            val component = mini.deserialize(raw)
            val json = GsonComponentSerializer.gson().serialize(component)
            val result = TextCodecs.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
            result.result().orElse(null)
        } catch (e: Exception) {
            println("[MiniMessageCompat] Failed to parse message: $raw")
            null
        }
    }
}