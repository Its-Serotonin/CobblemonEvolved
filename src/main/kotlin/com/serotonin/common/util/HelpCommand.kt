package com.serotonin.common.util

import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.server.command.ServerCommandSource
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.text.ClickEvent
import net.minecraft.util.Formatting


object HelpCommand{
    val pageSuggestionProvider =

        SuggestionProvider { _: CommandContext<ServerCommandSource>, builder: SuggestionsBuilder ->
            listOf("1", "2", "3", "4", "5").forEach { builder.suggest(it) }
            builder.buildFuture()
        }

    fun registerCommand() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                literal<ServerCommandSource>("ce")
                    .then(
                        literal<ServerCommandSource>("help")
                            .then(
                                argument<ServerCommandSource, Int>("page", integer(1,5))
                                    .suggests(pageSuggestionProvider)
                                    .executes { context ->
                                        val page = context.getArgument("page", Int::class.java)
                                        val player = context.source.player
                                        sendHelpPage(player, page)
                                        1
                                    }
                            )
                            .executes { context ->
                                val player = context.source.player
                                sendHelpPage(player, 1)
                                1
                            }
                    )
            )
        }
    }
    fun sendHelpPage(player: ServerPlayerEntity?, page: Int) {
        val lines = when (page) {
            1 -> listOf(
                "Welcome to the help page for Cobblemon Evolved! This is where you will find very useful information about the mod and mod pack.",
                "How to get started:",
                "-The Competitive handbook is the main item centralized around the new competitive system that CE introduces. You get one of these as soon as you join the game, and can be crafted with a Poké Ball and Book in any order. You can also use the command /competitive handbook in case you lose, or don’t want to use the item.",
                "-The next major thing is the Saveslots system; this design sort of mimics the one in Pokémon, so you can have three separate slots that you can carry between worlds!",
                "-The Saveslots system can be accessed by default using F7, but can be changed in your key binding settings to whatever you’d like.",
                "CE Help page (1/5) Type /ce help 2 for more!"
            )
            2 -> listOf(
                "Welcome to CE Help 2!",
                "Structures:",
                "-So far 1 custom structure has been introduced: the Chi-Yu Shrine! This is a very rare structure that only spawns one ~every 10,000 blocks. To complement this structure the key item Beads of Ruin has been added, spawning rarely as loot in various structures, (mineshafts, ancient cities, trial chambers, temples, or more commonly in end cities) and can be used to summon the legendary Pokémon Chi-Yu at their shrine!",
                "-More custom Pokémon have been added, but they do not have special spawning conditions.",
                "-As well as structures I have also integrated with CobbleDollars to add in some custom Lobby Vendors. These can be found in the Lobby and have custom trades depending on which tier you are.",
                "CE Help page (2/5) Type /ce help 3 for more!"
            )
            3 -> listOf(
                "Welcome to CE Help 3!",
                "Useful commands:",
                "-You can use /server <world> to switch between worlds, useful for going back and fourth between servers. You can also use /server Lobby to travel back to the main lobby server.",
                "-This modpack includes the Cobblemon Challenges mod. This means you can use /challenge to have a different way to battle players.",
                "-/gts can be used to open the Global Trading System! Good for trading with far away players",
                "-Many competitive commands have been added with the CE Mod, such as: /rank to view your rank, /leaderboard to see the top ranked players, /friendlybattle to toggle friendly battle status (can also be done inside the Competitive Handbook), and tournament based commands, such as: /tournament status to check the status of any ongoing tournaments, and /verifyteam <ruleset> to connect with my team validator and see if your team is valid for a certain ruleset!",
                "CE Help page (3/5) Type /ce help 4 for more!"
            )
            4 -> listOf(
                "Welcome to CE Help 4!",
                "More about the ranking system:",
                "-Most of the ranking system is ladder based, meaning that you will be assigned various tiers as you progress up the ranks, corresponding to your elo (or rank), which is represented as a number starting at a default of 1000.",
                "-Earning higher tiers can yield special rewards that can be claimed in the Competitive Handbook, as well as unlocking new shops for the Lobby Vendor, and just looking cool. If you don’t want your rank to be affected during a battle, you can enable friendly battle mode.",
                "-You can also earn streaks the more battles that you win consecutively, and the higher streak you earn will attribute more points on victory, for a max of +50 points per win",
                "-Another important note as of right now: if you try to claim tier rewards without having enough inventory space, you will lose whichever rewards you don’t have enough space for. So make sure you have enough space for your rewards.",
                "CE Help page (4/5) Type /ce help 5 for more!"
            )
            5 -> listOf(
                "Welcome to CE Help 5!",
                "Community/Support:",
                "-If you have any questions or need support, you can always contact me or join my Discord server. I will do my best to assist whenever I can.",
                "-If you would like to donate or support me and my work, consider joining my Patreon or donating on Modrinth",
                "Thank you for playing! CE Help page (5/5)"
            )
            else -> {
                player?.sendMessage(Text.literal("§cUnknown help page. Use /ce help 1-5."))
                return
            }
        }

        val message = Text.literal("")
        lines.forEachIndexed { i, line ->
            val color = if (i % 2 == 0) Formatting.YELLOW else Formatting.WHITE
            message.append(Text.literal(line).formatted(color)).append(Text.literal("\n"))
        }

        val discord = Text.literal("-Discord: ").append(
            Text.literal("https://discord.gg/f6K9nVUTh6").styled {
                it.withColor(Formatting.AQUA)
                    .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.gg/f6K9nVUTh6"))
                    .withUnderline(true)
            }
        )

        val patreon = Text.literal("-Patreon: ").append(
            Text.literal("https://patreon.com/CobblemonEvolved").styled {
                it.withColor(Formatting.AQUA)
                    .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.patreon.com/yourname"))
                    .withUnderline(true)
            }
        )

        val modrinth = Text.literal("-Modrinth: ").append(
            Text.literal("https://modrinth.com/project/cobblemon-evolved-sero").styled {
                it.withColor(Formatting.AQUA)
                    .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, "https://modrinth.com/user/yourmod"))
                    .withUnderline(true)
            }
        )

        val curseforge = Text.literal("-CurseForge: ").append(
            Text.literal("https://www.curseforge.com/minecraft/mc-mods/cobblemon-evolved").styled {
                it.withColor(Formatting.AQUA)
                    .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.curseforge.com/minecraft/mc-mods/yourmod"))
                    .withUnderline(true)
            }
        )


        message.append(Text.literal("\n"))


        if (page == 5) {
            message.append(discord).append(Text.literal("\n"))
            message.append(patreon).append(Text.literal("\n"))
            message.append(modrinth).append(Text.literal("\n"))
            message.append(curseforge).append(Text.literal("\n"))
        }


        if (page > 1) {
            val prevButton = Text.literal("[Previous]")
                .styled {
                    it.withColor(Formatting.GRAY)
                        .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ce help ${page - 1}"))
                        .withUnderline(true)
                }
            message.append(prevButton)
        }


        if (page > 1 && page < 5) {
            message.append(Text.literal(" "))
        }


        if (page < 5) {
            val nextButton = Text.literal("[Next]")
                .styled {
                    it.withColor(Formatting.GRAY)
                        .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ce help ${page + 1}"))
                        .withUnderline(true)
                }
            message.append(nextButton)
            message.append(Text.literal("\n"))
        }

        player?.sendMessage(message)
    }
}