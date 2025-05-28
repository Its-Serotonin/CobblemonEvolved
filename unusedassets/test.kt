package com.serotonin.common.elosystem

import com.cobblemon.mod.common.api.events.battles.BattleEndEvent
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.CommandManager
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.command.argument.IntegerArgumentType
import org.lwjgl.glfw.GLFW
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlinx.serialization.json.Json

object Cobblemonevolved : ModInitializer {
    private val eloMap: MutableMap<UUID, Int> = mutableMapOf()
    private lateinit var server: MinecraftServer
    private val json = Json { prettyPrint = true }
    private val filePath: Path get() = server.runDirectory.toPath().resolve("elo_data.json") as Path

    val LEADERBOARD_PACKET_ID = Identifier("cobblemonelo", "leaderboard")

    override fun onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register { minecraftServer ->
            server = minecraftServer
            loadEloData()
        }

        ServerLifecycleEvents.SERVER_STOPPING.register {
            saveEloData()
        }

        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(CommandManager.literal("rank")
                .executes { context ->
                    val player = context.source.player
                    val elo = eloMap.getOrDefault(player!!.uuid, 1000)
                    val tier = getTierName(elo)
                    player.sendMessage(Text.literal("Your current Elo: $elo ($tier tier)"), false)
                    1
                })

            dispatcher.register(CommandManager.literal("leaderboard")
                .executes { context ->
                    val topPlayers = eloMap.entries.sortedByDescending { it.value }.take(10)
                    val buf = PacketByteBufs.create()
                    buf.writeInt(topPlayers.size)
                    topPlayers.forEach { (uuid, elo) ->
                        val name = server.playerManager.getPlayer(uuid)?.name?.string ?: uuid.toString()
                        buf.writeUuid(uuid)
                        buf.writeString(name)
                        buf.writeInt(elo)
                    }
                    if (context.source.entity is ServerPlayerEntity) {
                        ServerPlayNetworking.send(context.source.entity as ServerPlayerEntity, LEADERBOARD_PACKET_ID, buf)
                    }
                    context.source.sendMessage(Text.literal("Leaderboard sent to client GUI."))
                    1
                })

            dispatcher.register(CommandManager.literal("setelo")
                .requires { it.hasPermissionLevel(4) }
                .then(CommandManager.argument("player", net.minecraft.command.argument.EntityArgumentType.player())
                    .then(CommandManager.argument("elo", IntegerArgumentType.integer(0))
                        .executes { context ->
                            val target = net.minecraft.command.argument.EntityArgumentType.getPlayer(context, "player")
                            val newElo = IntegerArgumentType.getInteger(context, "elo")
                            eloMap[target.uuid] = newElo
                            context.source.sendMessage(Text.literal("Set Elo for ${target.name.string} to $newElo"))
                            1
                        })))
        }

        BattleEndEvent.SUBSCRIBE.register { event ->
            val player1 = event.participantOneMinecraftPlayer ?: return@register
            val player2 = event.participantTwoMinecraftPlayer ?: return@register
            val winner = event.winnerMinecraftPlayer ?: return@register

            updateElo(player1, player2, winner)
        }
    }

    private fun updateElo(p1: ServerPlayerEntity, p2: ServerPlayerEntity, winner: ServerPlayerEntity) {
        val p1Elo = eloMap.getOrDefault(p1.uuid, 1000)
        val p2Elo = eloMap.getOrDefault(p2.uuid, 1000)

        val p1Wins = winner.uuid == p1.uuid
        val (newElo1, newElo2) = calculateElo(p1Elo, p2Elo, p1Wins)

        eloMap[p1.uuid] = newElo1
        eloMap[p2.uuid] = newElo2
    }

    private fun calculateElo(elo1: Int, elo2: Int, elo1Wins: Boolean): Pair<Int, Int> {
        val expected1 = 1.0 / (1.0 + Math.pow(10.0, ((elo2 - elo1) / 400.0)))
        val expected2 = 1.0 / (1.0 + Math.pow(10.0, ((elo1 - elo2) / 400.0)))
        val k = 32

        val newElo1 = (elo1 + k * ((if (elo1Wins) 1 else 0) - expected1)).toInt()
        val newElo2 = (elo2 + k * ((if (elo1Wins) 0 else 1) - expected2)).toInt()

        return newElo1 to newElo2
    }

    private fun getTierName(elo: Int): String {
        return when (elo) {
            in 0..1499 -> "Poke Ball"
            in 1500..1999 -> "Great Ball"
            in 2000..2499 -> "Ultra Ball"
            else -> "Master Ball"
        }
    }

    fun getTierIcon(elo: Int): Identifier {
        val tier = getTierName(elo).lowercase().replace(" ", "_")
        return Identifier("cobblemonelo", "textures/gui/tier_$tier.png")
    }

    private fun saveEloData() {
        val data = eloMap.mapKeys { it.key.toString() }
        val jsonString = json.encodeToString(data)
        Files.write(filePath, jsonString.toByteArray())
    }

    private fun loadEloData() {
        if (!Files.exists(filePath)) return
        val jsonString = Files.readString(filePath)
        val data = json.decodeFromString<Map<String, Int>>(jsonString)
        eloMap.clear()
        data.forEach { (uuid, elo) -> eloMap[UUID.fromString(uuid)] = elo }
    }

    fun getTopPlayers(limit: Int): List<Pair<UUID, Int>> {
        return eloMap.entries.sortedByDescending { it.value }.take(limit).map { it.toPair() }
    }
}

object CobblemonEloClient : ClientModInitializer {
    private lateinit var leaderboardKey: KeyBinding
    private val leaderboardEntries: MutableList<Triple<UUID, String, Int>> = mutableListOf()

    override fun onInitializeClient() {
        leaderboardKey = KeyBindingHelper.registerKeyBinding(KeyBinding(
            "key.cobblemonelo.leaderboard",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_L,
            "category.cobblemonelo"
        ))

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (leaderboardKey.wasPressed()) {
                client.setScreen(LeaderboardScreen(leaderboardEntries))
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(Cobblemonevolved.LEADERBOARD_PACKET_ID) { client, _, buf, _ ->
            val count = buf.readInt()
            val entries = mutableListOf<Triple<UUID, String, Int>>()
            repeat(count) {
                val uuid = buf.readUuid()
                val name = buf.readString()
                val elo = buf.readInt()
                entries.add(Triple(uuid, name, elo))
            }
            client.execute {
                leaderboardEntries.clear()
                leaderboardEntries.addAll(entries)
            }
        }
    }
}

class LeaderboardScreen(private val entries: List<Triple<UUID, String, Int>>) : Screen(Text.literal("Leaderboard")) {
    private val backgroundTexture = Identifier("cobblemonelo", "textures/gui/leaderboard_bg.png")

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        context.drawTexture(backgroundTexture, width / 2 - 110, height / 4 - 10, 0f, 0f, 220, 160, 220, 160)
    }

    override fun init() {
        entries.forEachIndexed { index, (uuid, name, elo) ->
            val tierIcon = Cobblemonevolved.getTierIcon(elo)
            val y = 40 + index * 22
            addDrawableChild(ButtonWidget.Builder(Text.literal("#${index + 1}: $name - $elo")) {}.dimensions(width / 2 - 70, y, 170, 20).build())
            this.addDrawable { context.drawTexture(tierIcon, width / 2 - 100, y, 0f, 0f, 16, 16, 16, 16) }
        }
    }

    override fun shouldCloseOnEsc(): Boolean = true
}
