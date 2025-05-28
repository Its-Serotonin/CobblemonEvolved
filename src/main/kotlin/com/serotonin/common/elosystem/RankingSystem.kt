package com.serotonin.common.elosystem

import com.serotonin.common.networking.OpenCompetitiveHandbookPayload
import com.cobblemon.mod.common.api.text.text
import kotlinx.serialization.json.Json
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.CommandManager.literal
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.serotonin.common.api.events.EloManager
import com.serotonin.common.api.events.EloManager.playerElos
import com.serotonin.common.api.events.EloManager.requestElo
import com.serotonin.common.api.events.resetPlayerRankStats
import com.serotonin.common.chat.withCooldownCheck
import com.serotonin.common.entities.LeaderboardArmorStandEntity
import com.serotonin.common.networking.Database
import com.serotonin.common.networking.LeaderboardEntry
import com.serotonin.common.networking.PlayerStats
import com.serotonin.common.networking.deduplicateAllPlayers
import com.serotonin.common.networking.getFriendlyBattle
import com.serotonin.common.networking.getPlayerStats
import com.serotonin.common.networking.givePlaytestRewardIfEligible
import com.serotonin.common.networking.sendEloUpdateToClient
import com.serotonin.common.networking.setFriendlyBattle
import com.serotonin.common.networking.syncAllOnlinePlayers
import com.serotonin.common.networking.syncFriendlyBattleToClient
import com.serotonin.common.networking.triggerLeaderboardDisplayOnServer
import com.serotonin.common.networking.updatePlayerStats
import com.serotonin.common.registries.EntityRegister
import com.serotonin.common.registries.FriendlyBattleManager
import fr.harmex.cobbledollars.common.world.entity.CobbleMerchant
import net.minecraft.server.command.CommandManager.argument
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.entity.Entity
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.registry.Registries
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.world.World
import java.time.Duration
import java.time.Instant
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.concurrent.thread



var elo = 0

val eloMap: MutableMap<UUID, Pair<String, Int>> = mutableMapOf()

private lateinit var server: MinecraftServer
private val json = Json { prettyPrint = true }
private val filePath: Path get() = server.runDirectory.resolve("player_rank_data.json") as Path
private var lastResetAllCommandTime: Long = 0

object PairSerializer : KSerializer<Pair<String, Int>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Pair") {
        element("first", String.serializer().descriptor)
        element("second", Int.serializer().descriptor )
    }

    override fun serialize(encoder: Encoder, value: Pair<String, Int>) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeStringElement(descriptor, 0, value.first)
        composite.encodeIntElement(descriptor, 1, value.second)
        composite.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): Pair<String, Int> {
        val dec = decoder.beginStructure(descriptor)
        var first = ""
        var second = 0
        loop@ while (true) {
            when (val index = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                0 -> first = dec.decodeStringElement(descriptor, index)
                1 -> second = dec.decodeIntElement(descriptor, index)
                else -> throw SerializationException("Unknown index $index")
            }
        }
        dec.endStructure(descriptor)
        return Pair(first, second)
    }
}

val data = eloMap.mapKeys { it.key.toString() }.mapValues {
    Pair(it.value.first, it.value.second)
}
val jsonString = json.encodeToString(MapSerializer(String.serializer(), PairSerializer), data)


object CommandRegister {

    fun rankCmds() {

        ServerLifecycleEvents.SERVER_STARTED.register {
            server = it

        }

        ServerLifecycleEvents.SERVER_STOPPING.register {

        }

        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            registerCompetitiveHandbookCommand(dispatcher)
            registerClaimPlaytestRewardCommand(dispatcher)
            registerSpawnLobbyVendorCommand(dispatcher)
            registerRemoveLobbyVendorsCommand(dispatcher)
            registerSyncPlayersCommand(dispatcher)
            registerDeduplicatePlayersCommand(dispatcher)
            registerRankCommand(dispatcher)
            registerSetRankCommand(dispatcher)
            registerLeaderboardCommands(dispatcher)
            registerTierCommand(dispatcher)
            registerFriendlyBattleCommand(dispatcher)
            registerMatchStatsCommand(dispatcher)
            registerRankStatsCommand(dispatcher)
            registerResetRankCommands(dispatcher)
        }
    }


    private fun registerCompetitiveHandbookCommand(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            withCooldownCheck("competitivehandbook") {
                requires { it.hasPermissionLevel(0) }
                    .executes { context ->
                        val player = context.source.playerOrThrow

                        ServerPlayNetworking.send(player, OpenCompetitiveHandbookPayload)

                        1
                    }
            }
        )
    }


    private fun registerClaimPlaytestRewardCommand(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            withCooldownCheck("claimplaytestreward") {
                requires { it.hasPermissionLevel(0) }
                .executes { ctx ->
                    val player = ctx.source.playerOrThrow
                    givePlaytestRewardIfEligible(player)
                    1
                }
            }
        )
    }

    private fun registerSpawnLobbyVendorCommand(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            literal("spawnlobbyvendor")
                .requires { it.hasPermissionLevel(4) }
                .executes { context ->
                    val player = context.source.playerOrThrow
                    val world = player.world
                    val pos = player.blockPos

                    val entityType = Registries.ENTITY_TYPE.get(Identifier.of("cobbledollars:cobble_merchant"))

                    val entity = entityType.create(world) as? CobbleMerchant ?: return@executes -1

                    val facingYaw = (player.yaw + 180) % 360

                    entity.refreshPositionAndAngles(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5, facingYaw, 0f)
                    entity.headYaw = facingYaw
                    entity.bodyYaw = facingYaw
                    entity.yaw = facingYaw
                   // entity.initialize(world as ServerWorldAccess?, world.getLocalDifficulty(pos), SpawnReason.COMMAND, null)

                    println("Spawned entity class: ${entity.javaClass.name}")


                    entity.addCommandTag("lobby_vendor")
                    entity.isAiDisabled = true
                    entity.isInvulnerable = true
                    entity.customName = Text.literal("Lobby Vendor")
                    entity.isCustomNameVisible = true
                    entity.isSilent = true

                    //for if i want to give it an actual player model
                    /*
                    val stream = MinecraftClient.getInstance().resourceManager
                        .getResource(Identifier.of("cobblemonevolved", "lobby_vendor/lobby_vendor"))
                        .get().inputStream

                    val bytes = stream.readBytes()

                    NPCPlayerTexture(bytes, NPCPlayerModelType.DEFAULT)



                    (entity as? ICobbleMerchantTexture)?.`cobblemonEvolvedModV2_1_21_1$setCustomTexture`(texture)
                    (entity as? ICobbleMerchantTexture)?.`cobblemonEvolvedModV2_1_21_1$setRawTexture`(ByteArray(0))
                    */


                    println("Final entity class: ${entity::class.qualifiedName}")
                    world.spawnEntity(entity)

                    context.source.sendFeedback(
                        { Text.literal("§aSpawned lobby vendor at your location.") },
                        false
                    )

                    return@executes 1
                }
        )
    }

    fun registerRemoveLobbyVendorsCommand(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            literal("removelobbyvendors")
                .requires { it.hasPermissionLevel(4) }
                .executes { context ->
                    val world = context.source.world

                    val removed = world.iterateEntities()
                        .filter { it is CobbleMerchant && it.commandTags.contains("lobby_vendor") }
                        .onEach {
                            it.commandTags.remove("lobby_vendor")
                            it.remove(Entity.RemovalReason.DISCARDED)
                        }
                        .count()
                    context.source.sendFeedback(
                        { Text.literal("§aDeleted $removed lobby vendors.") },
                        false
                    )

                    removed
                }
        )
    }

    private fun registerSyncPlayersCommand(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            withCooldownCheck("syncplayers") {
                requires { it.hasPermissionLevel(4) }
                .executes { context ->
                    val player = context.source.playerOrThrow
                    if (!player.hasPermissionLevel(4)) {
                        context.source.sendFeedback(
                            { text("§cYou don't have permission to execute that command.") },
                            false
                        )
                        return@executes 1
                    }

                    val server = context.source.server
                    syncAllOnlinePlayers(server)

                    context.source.sendFeedback(
                        { text("§aSynced all online players to the database.") },
                        false
                    )
                    1

                }
                }
        )
    }

    private fun registerDeduplicatePlayersCommand(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            withCooldownCheck("deduplicateplayers") {
                requires { it.hasPermissionLevel(4) }
                    .executes { context ->
                        val player = context.source.playerOrThrow
                        if (!player.hasPermissionLevel(4)) {
                            context.source.sendFeedback(
                                { text("§cYou don't have permission to execute that command.") },
                                false
                            )
                            return@executes 1
                        }

                        val server = context.source.server
                        deduplicateAllPlayers()

                        context.source.sendFeedback(
                            { text("Running deduplication...") },
                            false
                        )
                        1
                    }
            }
        )
    }

    private fun registerRankCommand(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            withCooldownCheck("rank") {
                executes { context ->
                    val player = context.source.player ?: return@executes 1
                    requestElo(player.uuid, player)
                    1
                }
                .then(
                    argument("target", StringArgumentType.word())
                        .executes { context ->
                            val targetName = StringArgumentType.getString(context, "target")
                            val server = context.source.server
                            val sourcePlayer = context.source.playerOrThrow

                            val targetPlayer = server.playerManager.getPlayer(targetName)

                            if (targetPlayer != null) {
                                requestElo(targetPlayer.uuid, sourcePlayer)
                                return@executes 1
                            }

                            val offlineUuid = lookupUuidOffline(targetName)
                            if (offlineUuid != null) {
                                requestElo(offlineUuid, sourcePlayer)
                            } else {
                                context.source.sendFeedback({ text("§cPlayer '$targetName' was not found.") }, false)
                            }

                            1
                        }
                )
            }
        )

    }

    private fun lookupUuidOffline(name: String): UUID? {
        Database.dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT player_id FROM player_stats WHERE player_name = ?").use { stmt ->
                stmt.setString(1, name)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    return UUID.fromString(rs.getString("player_id"))
                }
            }
        }
        return null
    }


    private fun registerSetRankCommand(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            withCooldownCheck("setrank") {
                requires { it.hasPermissionLevel(4) }
                .then(
                    argument("elo", IntegerArgumentType.integer(0))
                        .executes { context ->
                            val player = context.source.playerOrThrow
                            val eloValue = IntegerArgumentType.getInteger(context, "elo")

                            if (!player.hasPermissionLevel(4)) {
                                context.source.sendFeedback(
                                    { text("§cYou don't have permission to set your rank.") },
                                    false
                                )
                                return@executes 1
                            }

                            val currentStats = getPlayerStats(player.uuid)
                            val updatedStats = PlayerStats(
                                player.uuid,
                                player.name.string,
                                eloValue,
                                getTierName(eloValue),
                                battlesWon = currentStats?.battlesWon ?: 0,
                                battlesTotal = currentStats?.battlesTotal ?: 0,
                                winStreak = currentStats?.winStreak ?: 0,
                                longestWinStreak = currentStats?.longestWinStreak ?: 0
                            )
                            updatePlayerStats(updatedStats)

                            playerElos[player.uuid] = eloValue
                            cachedElo[player.uuid] = eloValue

                            val tier = getTierName(eloValue)
                            val tag = playerRankTags[player.uuid]
                            if (tag != null && tag.isAlive) {

                                val newText = Text.literal(tier)
                                    .append(Text.literal(": §c§l$eloValue"))
                                tag.customName = newText
                            } else {

                                updatePlayerNametag(player, eloValue, tier)
                            }

                            sendEloUpdateToClient(player, player.uuid, eloValue, silent = true)


                            val styledMessage = Text.literal("Your rank has been set to ")
                                .append(Text.literal(tier))
                                .append(Text.literal(": "))
                                .append(Text.literal("§c§l$eloValue"))

                            context.source.sendFeedback({ styledMessage }, false)
                            1
                        }
                )

                .then(
                    argument("target", StringArgumentType.word())
                        .then(
                            argument("elo", IntegerArgumentType.integer(0))
                                .executes { context ->
                                    val source = context.source
                                    val sourcePlayer = source.playerOrThrow
                                    val targetName = StringArgumentType.getString(context, "target")
                                    val eloValue = IntegerArgumentType.getInteger(context, "elo")

                                    if (!sourcePlayer.hasPermissionLevel(4)) {
                                        source.sendFeedback(
                                            { text("§cYou don't have permission to set ranks.") },
                                            false
                                        )
                                        return@executes 1
                                    }

                                    val targetPlayer = source.server.playerManager.getPlayer(targetName)

                                    if (targetPlayer != null) {
                                        val isSelf = targetPlayer.uuid == sourcePlayer.uuid

                                        val currentStats = getPlayerStats(targetPlayer.uuid)
                                        val updatedStats = PlayerStats(
                                            targetPlayer.uuid,
                                            targetPlayer.name.string,
                                            eloValue,
                                            getTierName(eloValue),
                                            battlesWon = currentStats?.battlesWon ?: 0,
                                            battlesTotal = currentStats?.battlesTotal ?: 0,
                                            winStreak = currentStats?.winStreak ?: 0,
                                            longestWinStreak = currentStats?.longestWinStreak ?: 0
                                        )
                                        updatePlayerStats(updatedStats)

                                        playerElos[targetPlayer.uuid] = eloValue
                                        cachedElo[targetPlayer.uuid] = eloValue

                                        val tier = getTierName(eloValue)
                                        val tag = playerRankTags[targetPlayer.uuid]
                                        if (tag != null && tag.isAlive) {

                                            val newText = Text.literal(tier)
                                                .append(Text.literal(": §c§l$eloValue"))
                                            tag.customName = newText
                                        } else {

                                            updatePlayerNametag(targetPlayer, eloValue, tier)
                                        }

                                        sendEloUpdateToClient(targetPlayer, targetPlayer.uuid, eloValue, silent = true)

                                         if (isSelf) {
                                            val tier = getTierName(eloValue)
                                            val styledMessage = Text.literal("Your rank has been set to ")
                                                .append(Text.literal(tier))
                                                .append(Text.literal(": "))
                                                .append(Text.literal("§c§l$eloValue"))

                                            context.source.sendFeedback({ styledMessage }, false)
                                        } else {
                                            val tier = getTierName(eloValue)
                                            val styledMessage =
                                                Text.literal("${targetPlayer.name.string}'s rank has been set to ")
                                                    .append(Text.literal(tier))
                                                    .append(Text.literal(": "))
                                                    .append(Text.literal("§c§l$eloValue"))


                                            source.sendFeedback({ styledMessage }, false)
                                        }
                                    } else {
                                        source.sendFeedback({ text("§cPlayer '$targetName' not found.") }, false)
                                    }

                                    1
                                }
                        )
                )
            }
        )
    }

    private fun registerMatchStatsCommand(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            withCooldownCheck("matchstats") {
                executes { context ->
                    val player = context.source.player ?: return@executes run {
                        context.source.sendError(Text.literal("§cOnly players can use this command."))
                        0
                    }

                    val uuid = player.uuid
                    val now = Instant.now()
                    val lastAccess = EloManager.lastStatsAccess[uuid]

                    val stats = EloManager.recentMatchStats[player.uuid]

                    if (stats == null) {
                        player.sendMessage(Text.literal("§7No recent match stats found."))
                        return@executes 1
                    }

                    if (lastAccess != null && Duration.between(lastAccess, now).seconds < 30) {
                        val remaining = 30 - Duration.between(lastAccess, now).seconds
                        player.sendMessage(Text.literal("§cYou must wait $remaining seconds before viewing match stats again."))
                        return@executes 1
                    }

                    EloManager.lastStatsAccess[uuid] = now
                    player.sendMessage(Text.literal(stats))
                    1
                }
            }
        )
    }

    fun registerRankStatsCommand(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            withCooldownCheck("rankstats") {
                executes { ctx ->
                    val player = ctx.source.player ?: return@executes run {
                        ctx.source.sendError(Text.literal("§cOnly players can use this command."))
                        0
                    }

                    val uuid = player.uuid
                    val stats = getPlayerStats(uuid)

                    if (stats != null) {
                        val winRate = if (stats.battlesTotal > 0) {
                            (stats.battlesWon * 100.0 / stats.battlesTotal).let { "%.1f".format(it) }
                        } else "0.0"

                        val msg = """
                    §6§lRanked Stats for ${stats.name}§r
                    §eRank: §c${stats.elo}
                    §eTier: §f${stats.tier}
                    §eTotal Battles: §f${stats.battlesTotal}
                    §eWins: §a${stats.battlesWon}
                    §eWin Rate: §b$winRate%
                    §eLongest Win Streak: §d${stats.longestWinStreak}
                """.trimIndent()

                        player.sendMessage(Text.literal(msg))
                    } else {
                        player.sendMessage(Text.literal("§cNo ranked stats found for you."))
                    }

                    1
                }
            }
        )
    }

    private fun registerResetRankCommands(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            withCooldownCheck("resetrank") {
                requires { it.hasPermissionLevel(4) }
                    .then(argument("player", EntityArgumentType.player()).executes { ctx ->
                        val target = EntityArgumentType.getPlayer(ctx, "player")


                        if (target == null) {
                            ctx.source.sendError(Text.literal("§cPlayer '$target' not found."))
                            return@executes 1
                        }

                        resetPlayerRankStats(target.uuid)

                        ctx.source.sendFeedback(
                            { Text.literal("§eReset rank data for ${target.name.string}.") },
                            false
                        )
                        1
                    }
                    )
            }
        )

        dispatcher.register(
            withCooldownCheck("resetallranks") {
                requires { it.hasPermissionLevel(4) }
                    .executes { ctx ->
                        val now = System.currentTimeMillis()
                        val remaining = 15000 - (now - lastResetAllCommandTime)

                        if (remaining > 0) {
                            resetPlayerRankStats(null)
                            lastResetAllCommandTime = 0

                            val server = ctx.source.server
                            val message = Text.literal("§4Reset all rank and reward data.")
                            server.playerManager.playerList.forEach {
                                it.sendMessage(message)
                            }

                            ctx.source.sendFeedback(
                                { Text.literal("§eReset rank data for all players in the database.") },
                                false
                            )
                        } else {
                            lastResetAllCommandTime = now
                            ctx.source.sendFeedback(
                                { Text.literal("§6 Run /resetallranks again within 15 seconds to confirm full reset.") },
                                false
                            )
                        }
                        1
                    }
            }
        )
    }


    private fun registerLeaderboardCommands(dispatcher: CommandDispatcher<ServerCommandSource>) {


        dispatcher.register(
            withCooldownCheck("leaderboard") {
                requires { it.hasPermissionLevel(0) }
                    .executes { context ->
                        val player = context.source.playerOrThrow


                        val leaderboard = mutableListOf<LeaderboardEntry>()

                        Database.dataSource.connection.use { conn ->
                            conn.prepareStatement("SELECT player_name, elo, tier, longest_win_streak FROM player_stats ORDER BY elo DESC LIMIT 10")
                                .use { stmt ->
                                    stmt.executeQuery().use { rs ->
                                        var index = 0
                                        while (rs.next()) {
                                            val prefix = when (index) {
                                                0 -> "§6§l" // Gold
                                                1 -> "§7§l" // Silver
                                                2 -> "§3§l" // Bronze
                                                else -> "§f"
                                            }
                                            leaderboard.add(
                                                LeaderboardEntry(
                                                    name = rs.getString("player_name"),
                                                    elo = rs.getInt("elo"),
                                                    tier = rs.getString("tier"),
                                                    longestWinStreak = rs.getInt("longest_win_streak"),
                                                    prefix = prefix
                                                )
                                            )
                                            index++
                                        }
                                    }
                                }
                        }

                        val leaderboardText = leaderboard.withIndex().joinToString("\n") { (index, it) ->
                            //with longest streak: "${it.prefix}#${index + 1}. ${it.name}§r ${it.tier}: §c§l${it.elo}§r §d§oLongest streak: §r§b§l[§r§f§b${it.longestWinStreak}§b§l]§r"
                            "${it.prefix}#${index + 1}. ${it.name}§r ${it.tier}: §c§l${it.elo}§r"
                        }

                        player.sendMessage(Text.literal("§e§l--- Leaderboard ---§r\n$leaderboardText"))

                        1
                    }
            }
        )

        dispatcher.register(
            withCooldownCheck("setleaderboard") {
                requires { it.hasPermissionLevel(4) }
                    .then(
                        argument("x", IntegerArgumentType.integer())
                            .then(
                                argument("y", IntegerArgumentType.integer())
                                    .then(
                                        argument("z", IntegerArgumentType.integer())
                                            .executes { context ->
                                                // Check if the player is an operator
                                                val player = context.source.player
                                                if (player?.hasPermissionLevel(4) == true) {  // Level 4 is for server operators
                                                    val x = IntegerArgumentType.getInteger(context, "x")
                                                    val y = IntegerArgumentType.getInteger(context, "y")
                                                    val z = IntegerArgumentType.getInteger(context, "z")

                                                    triggerLeaderboardDisplayOnServer(BlockPos(x, y, z))



                                                    context.source.sendFeedback(
                                                        { text("Leaderboard set at position: $x, $y, $z") },
                                                        false
                                                    )
                                                    return@executes 1
                                                } else {
                                                    context.source.sendFeedback(
                                                        { text("You do not have permission to execute this command.") },
                                                        false
                                                    )
                                                    return@executes 0
                                                }
                                            }
                                    )
                            )
                    )
                    .then(
                        literal("here")
                            .executes { context ->
                                val player = context.source.player
                                if (player != null && player.hasPermissionLevel(4)) {
                                    val pos = player.blockPos
                                    triggerLeaderboardDisplayOnServer(pos)


                                    context.source.sendFeedback(
                                        { text("Leaderboard set at your current position: ${pos.x}, ${pos.y}, ${pos.z}") },
                                        false
                                    )
                                    return@executes 1
                                } else {
                                    context.source.sendFeedback(
                                        { text("You do not have permission to execute this command.") },
                                        false
                                    )
                                    return@executes 0
                                }
                            }

                    )
            }
        )


        dispatcher.register(
            withCooldownCheck("removeleaderboard") {
                requires { it.hasPermissionLevel(4) }
                    .executes { context ->
                        val player = context.source.player
                        if (player != null && player.hasPermissionLevel(4)) {

                            val world = context.source.world
                            val entitiesToRemove = world.iterateEntities()
                                .filterIsInstance<ArmorStandEntity>()
                                .filter { it.commandTags.contains("RANKLEADERBOARD") }

                            entitiesToRemove.forEach { it.remove(Entity.RemovalReason.DISCARDED) }
                            leaderboardEntities.forEach { it.remove(Entity.RemovalReason.DISCARDED) }
                            leaderboardEntities.clear()

                            LeaderboardManager.removeLeaderboard(leaderboardPosition)
                            leaderboardPosition = null

                            context.source.sendFeedback(
                                { text("Leaderboard removed.") },
                                false
                            )
                            return@executes 1
                        } else {
                            context.source.sendFeedback(
                                { text("You do not have permission to execute this command.") },
                                false
                            )
                            return@executes 0
                        }
                    }
            }
        )

    }


    private fun registerTierCommand(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            withCooldownCheck("tiers") {
                executes { context ->
                val player = context.source.player
                val elo = eloMap.getOrDefault(player!!.uuid, 1000)
                context.source.sendFeedback({ text("§lCurrent Ladder Tiers: ${getTierList()}") }, false)
                1
            }
            }
        )
    }

    fun registerFriendlyBattleCommand(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            withCooldownCheck("friendlybattle") {
                requires { it.entity is ServerPlayerEntity }
                    .executes { context ->
                        val player = context.source.player!!
                        val uuid = player.uuid

                        thread(name = "toggle-friendly-$uuid") {
                            try {
                                val current = getFriendlyBattle(uuid)
                                val newSetting = !current
                                setFriendlyBattle(uuid, newSetting)
                                FriendlyBattleManager.cacheSetting(uuid, newSetting)

                                syncFriendlyBattleToClient(player, newSetting)

                                player.sendMessage(
                                    Text.literal(
                                        if (newSetting)
                                            "§fFriendly Battle mode is now §a§l§oON§r"
                                        else
                                            "§fFriendly Battle mode is now §c§l§oOFF§r"
                                    ),
                                    false
                                )
                                println("Toggled friendly battle for $uuid: $newSetting")
                            } catch (e: Exception) {
                                player.sendMessage(Text.literal("§cError toggling Friendly Battle mode."), false)
                                e.printStackTrace()
                            }
                        }

                        return@executes 1
                    }
            }
        )
    }
}

    private val ladderTiers = listOf<String>(
        "\n", "§r§f1000-1499 §cPoke §fBall", "\n",
        "§r§f1500-1999 §9Great §fBall", "\n",
        "§r§f2000-2499 §eUltra §8Ball", "\n",
        "§r§f2500-2999 §5§lMaster §f§lBall", "\n",
        "§r§f3000-3499 §6§lPsuedo §a§lLegendary", "\n",
        "§r§f3500-3999 §b§lLegendary", "\n",
        "§r§f4000-4499 §2§kA§d§l§oMythical§2§kA", "\n",
        "§r§f4500+ §e§o§kAA§e§l§oULTRA§b§l§oBEAST§kAA§r"
    )


     fun getTierName(elo: Int): String {
        return when (elo) {
            in 0..1499 -> "§cPoke §fBall"
            in 1500..1999 -> "§9Great §fBall"
            in 2000..2499 -> "§eUltra §8Ball"
            in 2500..2999 -> "§5§lMaster §f§lBall"
            in 3000..3499 -> "§6§lPsuedo §a§lLegendary"
            in 3500..3999 -> "§b§lLegendary"
            in 4000..4499 -> "§2§kA§d§l§oMythical§2§kA"
            else -> "§e§o§kAA§e§l§oULTRA §b§l§oBEAST§kAA"
        }
    }

    private fun getTierList(): List<String> {
        return (ladderTiers)
    }


    object IntAsStringSerializer : KSerializer<Int> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IntAsString", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Int) {
            encoder.encodeString(value.toString())
        }

        override fun deserialize(decoder: Decoder): Int {
            return decoder.decodeString().toInt()
        }
    }


@Serializable data class PlayerRankData(
    val uuid: String,
    //@Serializable(with = IntAsStringSerializer::class)
    val name: String,
    val elo: Int
)

fun initializeEloFile() {
    if (!Files.exists(filePath)) {
        Files.createFile(filePath)
        Files.writeString(filePath, "{}")
    }
}



fun saveEloData() {

    val data = eloMap.mapKeys { it.key.toString() }
        .mapValues { entry ->
            PlayerRankData(
                uuid = entry.key.toString(),
                name = entry.value.first,
                elo = entry.value.second
            )
        }
    val jsonString = json.encodeToString(MapSerializer(String.serializer(), PlayerRankData.serializer()), data)
    Files.write(filePath, jsonString.toByteArray())
    println("Saving rank data")
}


fun loadEloData() {
    if (!Files.exists(filePath)) return

    val jsonString = Files.readString(filePath)
    if (jsonString.isBlank()) return

    val data: Map<String, PlayerRankData> = json.decodeFromString(jsonString)

    eloMap.clear()

    data.forEach { (uuidStr, playerData) ->
        val uuid = UUID.fromString(uuidStr)
        eloMap[uuid] = playerData.name to playerData.elo
    }


            //not sure ab this yet
            ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
                val player = handler.player
                val uuid = player.uuid
                val name = player.name.string

                val existing = eloMap[uuid]
                if (existing == null) {
                    eloMap[uuid] = name to 1000
                } else if (existing.first != name) {
                    eloMap[uuid] = name to existing.second
                }
            }
        }

val leaderboardEntities = mutableListOf<ArmorStandEntity>()

var leaderboardPosition: BlockPos? = null

fun displayLeaderboardAsFloatingText(x: Int, y: Int, z: Int) {
    val baseY = y.toDouble()
    val world = server.getWorld(World.OVERWORLD) ?: return

    val area = Box(
        x.toDouble() - 1.0, y.toDouble() - 5.0, z.toDouble() - 1.0,
        x.toDouble() + 1.0, y.toDouble() + 1.0, z.toDouble() + 1.0
    )

    val oldEntities = world.getEntitiesByClass(LeaderboardArmorStandEntity::class.java, area) { true }

    oldEntities.forEach { it.remove(Entity.RemovalReason.DISCARDED) }
    println("Removed ${oldEntities.size} old leaderboard entities.")

    leaderboardPosition = BlockPos(x, y, z)

    val leaderboard = mutableListOf<LeaderboardEntry>()

    Database.dataSource.connection.use { conn ->
        conn.prepareStatement("SELECT player_name, elo, tier, longest_win_streak FROM player_stats ORDER BY elo DESC LIMIT 10")
            .use { stmt ->
                stmt.executeQuery().use { rs ->
                    var index = 0
                    while (rs.next()) {
                        val colorPrefix = when (index) {
                            0 -> "§6§l"  // Gold
                            1 -> "§7§l"  // Silver
                            2 -> "§3§l"  // Bronze
                            else -> "§f"
                        }

                        leaderboard.add(
                            LeaderboardEntry(
                                name = rs.getString("player_name"),
                                elo = rs.getInt("elo"),
                                tier = rs.getString("tier"),
                                prefix = colorPrefix
                            )
                        )
                        index++
                    }
                }
            }
    }

    leaderboardEntities.clear()


    val titleStand = LeaderboardArmorStandEntity(EntityRegister.LEADERBOARD_ARMOR_STAND, world).apply {
        refreshPositionAndAngles(x.toDouble(), baseY + 0.3, z.toDouble(), 0f, 0f)
        customName = Text.of("§e§l--- Leaderboard ---§r")
        isCustomNameVisible = true
        isInvisible = true
        isInvulnerable = true
        setNoGravity(true)
        addCommandTag("RANKLEADERBOARD")
        setMarker(true) // optional

    }

    world.spawnEntity(titleStand)
    leaderboardEntities.add(titleStand)


    leaderboard.withIndex().forEach { (index, entry) ->
        val colorPrefix = when (index) {
            0 -> "§6§l"  // Gold
            1 -> "§7§l"  // Silver
            2 -> "§3§l"  // Bronze
            else -> "§f"
        }

        //text line with streaks: val textLine = "$colorPrefix#${index + 1}. ${entry.name}§r ${entry.tier}: §c§l${entry.elo}§r §d§oLongest streak: §r§b§l[§r§f§b${entry.longestWinStreak}§b§l]§r"
        val textLine = "$colorPrefix#${index + 1}. ${entry.name}§r ${entry.tier}: §c§l${entry.elo}§r"


        val armourStand = LeaderboardArmorStandEntity(EntityRegister.LEADERBOARD_ARMOR_STAND, world).apply {
            refreshPositionAndAngles(x.toDouble(), baseY - (index + 1) * 0.3, z.toDouble(), 0f, 0f)
            customName = Text.of(textLine)
            isCustomNameVisible = true
            isInvisible = true
            isInvulnerable = true
            setNoGravity(true)
            addCommandTag("RANKLEADERBOARD")
            setMarker(true)
        }

        world.spawnEntity(armourStand)
        leaderboardEntities.add(armourStand)
        println("Spawning leaderboard line: $textLine")
    }
}


    private fun ArmorStandEntity.setMarker(marker: Boolean) {}


var tickCounter = 0
fun startPeriodicLeaderboardUpdate(server: MinecraftServer) {
    ServerTickEvents.START_SERVER_TICK.register { _ ->
        tickCounter++
        if (tickCounter >= 500) {
            if (leaderboardPosition != null) {
                updateLeaderboard()
                println("Debug: Leaderboard updating")
            } else {
                println("Skipping leaderboard update — no leaderboard placed")
            }
            tickCounter = 0
        }
    }
}

fun updateLeaderboard() {

    if (leaderboardPosition != null) {

        leaderboardEntities.forEach { it.remove(Entity.RemovalReason.DISCARDED) }
        leaderboardEntities.clear()

        leaderboardPosition?.let { pos ->
            displayLeaderboardAsFloatingText(pos.x, pos.y, pos.z)
        }
    }
}

