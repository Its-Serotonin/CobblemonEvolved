package com.serotonin.common.chat


import com.google.gson.Gson
import com.google.gson.JsonObject
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.serotonin.common.chat.ProfanityLogger.cleanIfOld
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import java.io.File
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.UUID
import net.minecraft.text.MutableText


object ChatRateLimiter {
    private val messageTimestamps = mutableMapOf<UUID, ArrayDeque<Long>>()
    private val cooldowns = mutableMapOf<UUID, Long>()
    private const val MESSAGE_LIMIT = 5
    private const val TIME_WINDOW_MS = 3000L
    private const val COOLDOWN_MS = 5000L
    private val joinTimestamps = mutableMapOf<UUID, Long>()
    private const val JOIN_GRACE_PERIOD_MS = 5000L


    fun markJoin(uuid: UUID) {
        joinTimestamps[uuid] = System.currentTimeMillis()
    }

    fun handleChatOrCommand(uuid: UUID, systemMessage: Boolean = false): String? {
        if (systemMessage) return null
        val now = System.currentTimeMillis()


        joinTimestamps[uuid]?.let { joinTime ->
            if (now - joinTime <= JOIN_GRACE_PERIOD_MS) {
                return null
            }
        }

        cooldowns[uuid]?.let { cooldownEnd ->
            if (now < cooldownEnd) {
                val remaining = ((cooldownEnd - now) / 1000.0).coerceAtLeast(0.0)
                return "§cYou must wait %.1f more seconds before sending messages!".format(remaining)
            }
        }

        val timestamps = messageTimestamps.getOrPut(uuid) { ArrayDeque() }
        timestamps.addLast(now)

        while (timestamps.isNotEmpty() && now - timestamps.first() > TIME_WINDOW_MS) {
            timestamps.removeFirst()
        }

        if (timestamps.size > MESSAGE_LIMIT) {
            cooldowns[uuid] = now + COOLDOWN_MS
            return "§cYou are sending messages too quickly! Cooldown: ${COOLDOWN_MS / 1000} seconds."
        }

        return null
    }

    fun cleanup(uuid: UUID) {
        messageTimestamps.remove(uuid)
        cooldowns.remove(uuid)
        joinTimestamps.remove(uuid)
    }
}

object ProfanityFilter {
    private val configFile = File("config/cobblemonevolved/chatblacklist.json")
    val bannedWords = loadOrCreateBannedWords()

    private fun loadOrCreateBannedWords(): Set<String> {
        if (!configFile.exists()) {
            try {
                configFile.parentFile.mkdirs()
                configFile.writeText(
                    """
                    {
                      "bannedWords": [
                        "badword",
                        "swear"
                      ]
                    }
                    """.trimIndent()
                )
                println("[ProfanityFilter] Created default chat blacklist config.")
            } catch (e: IOException) {
                println("[ProfanityFilter] Failed to create blacklist config: ${e.message}")
                return emptySet()
            }
        }

        return try {
            val json = Gson().fromJson(configFile.readText(), JsonObject::class.java)
            json.getAsJsonArray("bannedWords").mapNotNull { it.asString }.toSet()
        } catch (e: Exception) {
            println("[ProfanityFilter] Failed to load blacklist: ${e.message}")
            emptySet()
        }
    }

    private val bannedPatterns = listOf(
        Regex("n[\\W_]*[i1!]+[\\W_]*g+[\\W_]*[g3]+[\\W_]*[e3]+[\\W_]*r", RegexOption.IGNORE_CASE),
        Regex("f[\\W_]*[a1!]+[\\W_]*g+[\\W_]*[g3]+[\\W_]*[o3]+[\\W_]*t", RegexOption.IGNORE_CASE),
    )


    fun containsProfanity(message: String): Boolean {
        val cleaned = message.lowercase().replace(Regex("[^a-z]"), "")
        return bannedWords.any { cleaned.contains(it) } ||
                bannedPatterns.any { it.containsMatchIn(message) }
    }
}


object ProfanityLogger {
    private val logFile = File("logs/profanity-violations.log")
    private val timestampFile = File("logs/profanity-clean-timestamp.txt")


    fun log(playerName: String, message: String) {
        try {
            logFile.appendText("[${Instant.now()}] $playerName: $message\n")
        } catch (e: IOException) {
            println("§c[ProfanityLogger] Failed to write log: ${e.message}")
        }
    }

    fun cleanIfOld() {
        val now = Instant.now()
        val last = if (timestampFile.exists())
            Instant.parse(timestampFile.readText())
        else Instant.EPOCH

        if (Duration.between(last, now).toDays() >= 7) {
            logFile.writeText("")
            timestampFile.writeText(now.toString())
            println("[ProfanityLogger] Cleaned old violation log.")
        }
    }
}


object ChatModRegister {

    fun registerChatModeration() {
        cleanIfOld()


        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register { message, player, _ ->
            val text = message.content.string
            println("chat message from ${player.name.string}: $text")

            if (isSystemMessage(text)) return@register true

            if (ProfanityFilter.containsProfanity(text)) {
                ProfanityLogger.log(player.name.string, text)
                player.sendMessage(Text.literal("§cPlease refrain from using profanity in chat."))
                return@register false
            }

            val result = ChatRateLimiter.handleChatOrCommand(player.uuid)
            if (result != null) {
                player.sendMessage(Text.literal(result))
                return@register false
            }
            true
        }

        /*
        ServerMessageEvents.ALLOW_COMMAND_MESSAGE.register { command, source, _ ->
            val player = source.entity as? ServerPlayerEntity ?: return@register true

            val result = ChatRateLimiter.handleChatOrCommand(player.uuid)
            if (result != null) {
                player.sendMessage(Text.literal(result), false)
                return@register false
            }
            true
        }*/
    }
}
val commandWhitelist = listOf("lp", "luckperms", "permissions", "pl")


fun shouldBypassRateLimit(command: String): Boolean {
    val cmd = command.trim().lowercase()
    return commandWhitelist.any { cmd.startsWith(it) }
}



fun withCooldownCheck(
    name: String,
    build: LiteralArgumentBuilder<ServerCommandSource>.() -> Unit
): LiteralArgumentBuilder<ServerCommandSource> {
    return LiteralArgumentBuilder.literal<ServerCommandSource>(name)
        .requires { source ->
            val player = source.entity as? ServerPlayerEntity ?: return@requires true
            

            if (!source.isExecutedByPlayer) return@requires true

            val result = ChatRateLimiter.handleChatOrCommand(player.uuid)
            if (result != null) {
                player.sendMessage(Text.literal(result))
                return@requires false
            }
            true
        }
        .apply(build)
}

fun isSystemMessage(text: String): Boolean {
    return text.startsWith("CESM") || text.startsWith("[System]") || "debug" in text.lowercase()
}