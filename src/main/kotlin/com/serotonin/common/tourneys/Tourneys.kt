package com.serotonin.common.tourneys

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import com.mojang.brigadier.arguments.StringArgumentType.word
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.serotonin.common.chat.withCooldownCheck
import com.serotonin.common.networking.Database
import com.serotonin.common.networking.RawJsonPayload
import com.serotonin.common.networking.ServerContext.server
import com.serotonin.common.tourneys.TeamValidator.getPlayerTeamData
import com.serotonin.common.tourneys.TeamValidator.validateTeamWithAPI
import com.serotonin.common.tourneys.TournamentManager.getActiveTournament
import com.serotonin.common.tourneys.TournamentManager.isPlayerSignedUp
import com.serotonin.common.tourneys.TournamentManager.isTournamentActive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.command.CommandManager.*
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.concurrent.thread


object TournamentManager {
    data class Tournament(val id: UUID, val ruleset: String, val startTime: Instant)

    /*val FORMAT_SUGGESTIONS =
        SuggestionProvider<ServerCommandSource> { _: CommandContext<ServerCommandSource>, builder: SuggestionsBuilder ->
            val filters = listOf("vgc", "gen9", "gen8", "ubers", "ou", "doubles")
            filters.forEach { builder.suggest(it) }
            builder.buildFuture()
        }*/

    val DYNAMIC_FORMAT_SUGGESTIONS = SuggestionProvider<ServerCommandSource> { context, builder ->
        val input = builder.remainingLowerCase
        val suggestions = fuzzyMatch(input, CachedFormatSuggestions.suggestions)
        suggestions.forEach { builder.suggest(it) }
        builder.buildFuture()
    }


    fun getActiveTournament(): Tournament? {
        Database.dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                SELECT id, ruleset_name, start_time FROM tournaments
                WHERE is_active = TRUE
                ORDER BY start_time DESC
                LIMIT 1
            """
            ).use { stmt ->
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) {
                        Tournament(
                            UUID.fromString(rs.getString("id")),
                            rs.getString("ruleset_name"),
                            rs.getTimestamp("start_time").toInstant()
                        )
                    } else null
                }
            }
        }
    }

    fun createTournament(ruleset: String, startTime: Instant, createdBy: UUID): UUID {
        val id = UUID.randomUUID()
        Database.dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                INSERT INTO tournaments (id, start_time, ruleset_name, created_by)
                VALUES (?, ?, ?, ?)
            """
            ).use { stmt ->
                stmt.setObject(1, id)
                stmt.setTimestamp(2, java.sql.Timestamp.from(startTime))
                stmt.setString(3, ruleset)
                stmt.setObject(4, createdBy)
                stmt.executeUpdate()
            }
        }
        return id
    }

    fun isTournamentActive(): Boolean = getActiveTournament()?.startTime?.isAfter(Instant.now()) == true

    fun formatDate(): String {
        val tournament = getActiveTournament() ?: return "Not set"
        val timezone = getTournamentTimezone(tournament.id) ?: "UTC"
        val zoned = ZonedDateTime.ofInstant(tournament.startTime, ZoneId.of(timezone))
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z")
        return formatter.format(zoned)
    }

    fun getTournamentTimezone(id: UUID): String? {
        Database.dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT timezone FROM tournaments WHERE id = ?").use { stmt ->
                stmt.setObject(1, id)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.getString("timezone") else null
                }
            }
        }
    }


    fun timeUntil(): String {
        val now = Instant.now()
        val target = getActiveTournament()?.startTime ?: return "No active tournament."
        if (target.isBefore(now)) return "No active tournament."

        val duration = Duration.between(now, target)
        val days = duration.toDays()
        val hours = duration.toHoursPart()
        val minutes = duration.toMinutesPart()

        return "$days days, $hours hours, $minutes minutes remaining."
    }

    fun toggleSignup(player: ServerPlayerEntity): Boolean {
        val playerId = player.uuid
        val tournament = getActiveTournament() ?: return false
        val isSignedUp = isPlayerSignedUp(playerId, tournament.id)

        Database.dataSource.connection.use { conn ->
            if (isSignedUp) {
                conn.prepareStatement("DELETE FROM tournament_signups WHERE tournament_id = ? AND player_id = ?")
                    .use { stmt ->
                        stmt.setObject(1, tournament.id)
                        stmt.setObject(2, playerId)
                        stmt.executeUpdate()
                    }
            } else {
                conn.prepareStatement(
                    """
                    INSERT INTO tournament_signups (tournament_id, player_id)
                    VALUES (?, ?) ON CONFLICT DO NOTHING
                """
                ).use { stmt ->
                    stmt.setObject(1, tournament.id)
                    stmt.setObject(2, playerId)
                    stmt.executeUpdate()
                }
            }
        }
        return !isSignedUp
    }

    fun isPlayerSignedUp(playerId: UUID, tournamentId: UUID): Boolean {
        Database.dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                SELECT 1 FROM tournament_signups WHERE tournament_id = ? AND player_id = ?
            """
            ).use { stmt ->
                stmt.setObject(1, tournamentId)
                stmt.setObject(2, playerId)
                stmt.executeQuery().use { rs ->
                    return rs.next()
                }
            }
        }
    }

    fun getSignedUpPlayerNames(): List<String> {
        val tournament = getActiveTournament() ?: return emptyList()
        val names = mutableListOf<String>()
        Database.dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
            SELECT ps.player_name
            FROM tournament_signups ts
            JOIN player_stats ps ON ts.player_id = ps.player_id
            WHERE ts.tournament_id = ?
        """
            ).use { stmt ->
                stmt.setObject(1, tournament.id)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        names.add(rs.getString("player_name"))
                    }
                }
            }
        }
        return names
    }


    fun registerCommands(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            withCooldownCheck("tournament") {
                then(
                    literal("setruleset")
                        .then(
                            argument("name", word())
                                .requires { it.hasPermissionLevel(4) }
                                .executes {
                                    val name = StringArgumentType.getString(it, "name").lowercase(Locale.ROOT)
                                    val player = it.source.playerOrThrow

                                    val existingTournament = getActiveTournament()
                                    if (existingTournament != null) {
                                        Database.dataSource.connection.use { conn ->
                                            conn.prepareStatement("UPDATE tournaments SET ruleset_name = ? WHERE id = ?")
                                                .use { stmt ->
                                                    stmt.setString(1, name)
                                                    stmt.setObject(2, existingTournament.id)
                                                    stmt.executeUpdate()
                                                }
                                        }

                                        sendTournamentInfo(player, name, formatDate())

                                        it.source.sendFeedback(
                                            { Text.literal("Updated ruleset to '$name' for the current tournament.") },
                                            false
                                        )
                                    } else {
                                        val now = Instant.now().plusSeconds(3600)
                                        createTournament(name, now, player.uuid)
                                        it.source.sendFeedback(
                                            { Text.literal("Created new tournament with ruleset '$name' starting in 1 hour.") },
                                            false
                                        )
                                    }
                                    1
                                })
                )

                    .then(
                        literal("settime")
                            .then(
                                argument("datetime", greedyString())
                                    .requires { it.hasPermissionLevel(4) }
                                    .executes {
                                        val input = StringArgumentType.getString(it, "datetime")
                                        val player = it.source.playerOrThrow

                                        try {
                                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z")
                                            val zonedTime = ZonedDateTime.parse(input, formatter)
                                            val newTime = zonedTime.toInstant()
                                            val timezone = zonedTime.zone.id

                                            val tournament = getActiveTournament()
                                            if (tournament == null) {
                                                it.source.sendError(Text.literal("No active tournament to update time."))
                                            } else {
                                                Database.dataSource.connection.use { conn ->
                                                    conn.prepareStatement("UPDATE tournaments SET start_time = ?, timezone = ? WHERE id = ?")
                                                        .use { stmt ->
                                                            stmt.setTimestamp(1, java.sql.Timestamp.from(newTime))
                                                            stmt.setString(2, timezone)
                                                            stmt.setObject(3, tournament.id)
                                                            stmt.executeUpdate()
                                                        }
                                                }
                                                sendTournamentInfo(player, tournament.ruleset, formatDate())
                                                it.source.sendFeedback(
                                                    { Text.literal("Tournament time updated to $input (${zonedTime.zone.id}).") },
                                                    false
                                                )
                                            }
                                        } catch (e: Exception) {
                                            it.source.sendError(
                                                Text.literal("Invalid format. Use 'yyyy-MM-dd HH:mm z' (e.g. '2025-05-10 18:30 CST').")
                                            )
                                        }
                                        1
                                    }
                            )
                    )
            }
                .then(
                    literal("signup")
                        .executes {
                            val player = it.source.playerOrThrow
                            val toggled = toggleSignup(player)
                            player.sendMessage(
                                Text.literal(
                                    if (toggled) "§aYou have signed up for the tournament!" else "§eYou have been removed from the tournament."
                                )
                            )
                            1
                        }
                )
                .then(
                    literal("status")
                        .executes {
                            val tournament = getActiveTournament()
                            if (tournament == null) {
                                it.source.sendFeedback({ Text.literal("§7No active tournament.") }, false)
                            } else {
                                it.source.sendFeedback({
                                    Text.literal("§aTournament Info:\n§fRuleset: ${tournament.ruleset}\n§fTime: ${formatDate()}\n§f${timeUntil()}")
                                }, false)
                            }
                            1
                        }
                )

                .then(
                    literal("list")
                        .requires { it.hasPermissionLevel(4) }
                        .executes {
                            val names = getSignedUpPlayerNames()
                            if (names.isEmpty()) {
                                it.source.sendFeedback(
                                    { Text.literal("§7No players are signed up for the current tournament.") },
                                    false
                                )
                            } else {
                                val list = names.joinToString(", ")
                                it.source.sendFeedback({ Text.literal("§aSigned-up players: §f$list") }, false)
                            }
                            1
                        }
                )

                .then(
                    literal("formats")
                        .executes { ctx ->
                            fetchAndSendFormats(ctx.source.playerOrThrow, null)
                            1
                        }
                        .then(
                            argument("filter", word())
                                .suggests(DYNAMIC_FORMAT_SUGGESTIONS)
                                .executes { ctx ->
                                    val filter = StringArgumentType.getString(ctx, "filter")
                                    fetchAndSendFormats(ctx.source.playerOrThrow, filter)
                                    1
                                }
                        )
                )


                .then(
                    literal("clear")
                        .requires { it.hasPermissionLevel(4) }
                        .executes {
                            val tournament = getActiveTournament()
                            if (tournament == null) {
                                it.source.sendError(Text.literal("§7No active tournament to clear."))
                                return@executes 1
                            }

                            Database.dataSource.connection.use { conn ->
                                conn.prepareStatement("UPDATE tournaments SET is_active = FALSE WHERE id = ?")
                                    .use { stmt ->
                                        stmt.setObject(1, tournament.id)
                                        stmt.executeUpdate()
                                    }
                            }

                            server?.playerManager?.playerList?.forEach { player ->
                                sendTournamentInfo(player, ruleset = "None", startTime = "Not set")
                            }


                            it.source.sendFeedback(
                                { Text.literal("§aCleared active tournament '${tournament.ruleset}'.") },
                                false
                            )
                            1
                        }
                )
        )

        dispatcher.register(withCooldownCheck("verifyteam") {
            executes {
                val player = it.source.playerOrThrow
                val ruleset = getActiveTournament()?.ruleset
                    ?: run {
                        it.source.sendError(Text.literal("\u00a7cNo active tournament and no ruleset specified."))
                        return@executes 1
                    }

                thread(start = true) {
                    val team = getPlayerTeamData(player)
                    val result = validateTeamWithAPI(team, ruleset)

                    if (result.isValid) {
                        player.sendMessage(Text.literal("\u00a7aYour team is valid for '$ruleset'."))
                    } else {
                        val errorMessage = result.errors.joinToString("\n") { "• $it" }
                        player.sendMessage(Text.literal("\u00a7cInvalid team for '$ruleset':\n\u00a77$errorMessage"))
                    }
                }
                1
            }

                .then(
                    argument("ruleset", word())
                        .executes {
                            val player = it.source.playerOrThrow
                            val ruleset = StringArgumentType.getString(it, "ruleset")

                            val team = getPlayerTeamData(player)
                            val result = validateTeamWithAPI(team, ruleset)

                            if (result.isValid) {
                                player.sendMessage(Text.literal("\u00a7aYour team is valid for '$ruleset'."))
                            } else {
                                val errorMessage = result.errors.joinToString("\n") { "• $it" }
                                player.sendMessage(Text.literal("\u00a7cInvalid team for '$ruleset':\n\u00a77$errorMessage"))
                            }
                            1
                        })

                .then(
                    argument("target", word())
                        .requires { it.hasPermissionLevel(4) }
                        .executes {
                            val source = it.source
                            val targetName = StringArgumentType.getString(it, "target")
                            val target = server?.playerManager?.getPlayer(targetName)
                                ?: run {
                                    source.sendError(Text.literal("\u00a7cPlayer not found."))
                                    return@executes 1
                                }
                            val ruleset = getActiveTournament()?.ruleset
                                ?: run {
                                    source.sendError(Text.literal("\u00a7cNo active tournament and no ruleset specified."))
                                    return@executes 1
                                }

                            thread(start = true) {
                                val team = getPlayerTeamData(target)
                                val result = validateTeamWithAPI(team, ruleset)

                                source.sendFeedback({
                                    if (result.isValid)
                                        Text.literal("\u00a7a${targetName}'s team is valid for '$ruleset'.")
                                    else {
                                        val errorMessage = result.errors.joinToString("\n") { "• $it" }
                                        Text.literal("\u00a7c${targetName}'s team is invalid for '$ruleset':\n\u00a77$errorMessage")
                                    }
                                }, false)
                            }
                            1
                        }

                        .then(
                            argument("ruleset", word())
                                .executes {
                                    val source = it.source
                                    val targetName = StringArgumentType.getString(it, "target")
                                    val ruleset = StringArgumentType.getString(it, "ruleset")
                                    val target = server?.playerManager?.getPlayer(targetName)
                                        ?: run {
                                            source.sendError(Text.literal("\u00a7cPlayer not found."))
                                            return@executes 1
                                        }
                                    val team = getPlayerTeamData(target)
                                    val result = validateTeamWithAPI(team, ruleset)

                                    source.sendFeedback({
                                        if (result.isValid)
                                            Text.literal("\u00a7a${targetName}'s team is valid for '$ruleset'.")
                                        else {
                                            val errorMessage = result.errors.joinToString("\n") { "• $it" }
                                            Text.literal("\u00a7c${targetName}'s team is invalid for '$ruleset':\n\u00a77$errorMessage")
                                        }
                                    }, false)
                                    1
                                })
                )
        })

    }

}


fun sendTournamentInfo(player: ServerPlayerEntity, ruleset: String, startTime: String) {
    val now = Instant.now()
    val tournament = getActiveTournament()

    val responseJson = if (tournament == null || ruleset == "None" || startTime == "Not set") {
        buildJsonObject {
            put("type", "tournament_info")
            put("ruleset", "None")
            put("startTime", "Not set")
            put("status", "NoTournament")
            put("signedUp", false)
        }.toString()
    } else {
        val status = when {
            tournament.startTime.isAfter(now) -> {
                val minutesUntil = Duration.between(now, tournament.startTime).toMinutes()
                if (minutesUntil <= 5) "Starts Now" else "Active"
            }

            tournament.startTime.plus(Duration.ofHours(2)).isBefore(now) -> "Finished"
            else -> "Ongoing"
        }

        buildJsonObject {
            put("type", "tournament_info")
            put("ruleset", ruleset)
            put("startTime", startTime)
            put("status", status)
            put("signedUp", isPlayerSignedUp(player.uuid, tournament.id))
        }.toString()
    }

    ServerPlayNetworking.send(player, RawJsonPayload(responseJson))
}

fun fetchAndSendFormats(player: ServerPlayerEntity, filter: String?) {
    thread(start = true) {
        try {
            val client = OkHttpClient()
            val actualFilter = filter ?: "vgc"
            val url = "http://localhost:3000/formats?type=${actualFilter.lowercase()}"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) {
                    player.sendMessage(Text.literal("§cFailed to fetch formats."))
                    return@use
                }

                val jsonArray = JSONArray(body)
                CachedFormatSuggestions.suggestions = (0 until jsonArray.length()).mapNotNull { i ->
                    jsonArray.getJSONObject(i).optString("id")
                }.distinct().sorted()

                val lines = (0 until jsonArray.length()).map { i ->
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.getString("id")
                    val name = obj.getString("name")
                    "§e$id §8– §f$name"
                }

                if (lines.isEmpty()) {
                    player.sendMessage(Text.literal("§7No formats found for filter '${actualFilter}'."))
                } else {
                    player.sendMessage(Text.literal("§aAvailable Showdown Rulesets §7(filtered by '${actualFilter}')§a:"))
                    lines.take(20).forEach { player.sendMessage(Text.literal(it)) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            player.sendMessage(Text.literal("§cError fetching formats: ${e.message}"))
        }
    }
}

fun fuzzyMatch(input: String, options: List<String>): List<String> {
    val lowerInput = input.lowercase()
    return options
        .filter { it.contains(lowerInput) || lowerInput.contains(it) }
        .sortedBy { it.indexOf(lowerInput) }
}