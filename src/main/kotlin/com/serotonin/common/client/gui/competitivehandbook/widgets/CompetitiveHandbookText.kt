package com.serotonin.common.client.gui.competitivehandbook.widgets


import com.serotonin.common.client.networking.ClientStatsStorage
import com.serotonin.common.elosystem.getTierName
import com.serotonin.common.networking.PlayerStatsPayload
import com.serotonin.common.networking.getPlayerStats
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.util.Formatting


class CompetitiveHandbookTextGUI(private val x: Int, private val y: Int) {

    fun render(context: DrawContext) {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        val uuid = player.uuid
        val textRenderer = client.textRenderer

        val stats = ClientStatsStorage.getStats(uuid) ?: PlayerStatsPayload(
            uuid = uuid.toString(),
            name = player.name.string,
            elo = 1000,
            tier = getTierName(1000),
            battlesWon = 0,
            battlesTotal = 0,
            winStreak = 0,
            longestWinStreak = 0
        )

        val wins = stats.battlesWon
        val total = stats.battlesTotal
        val winRate = if (total > 0) (wins * 100.0 / total).let { "%.1f".format(it) } else "0.0"

        val winText = Text.literal("Total Wins: ").formatted(Formatting.GREEN)
            .append(Text.literal("$wins").formatted(Formatting.WHITE))
        val matchText = Text.literal("Total Matches: ").formatted(Formatting.GOLD)
            .append(Text.literal("$total").formatted(Formatting.WHITE))
        val percentText = Text.literal("Win %: ").formatted(Formatting.AQUA)
            .append(Text.literal(winRate).formatted(Formatting.WHITE))
        val streakText = Text.literal("Current Streak: ").formatted(Formatting.DARK_AQUA)
            .append(Text.literal("${stats.winStreak}").formatted(Formatting.WHITE))

        val longestText = Text.literal("Longest Streak: ").formatted(Formatting.LIGHT_PURPLE)
            .append(Text.literal("${stats.longestWinStreak}").formatted(Formatting.WHITE))

        context.drawTextWithShadow(textRenderer, winText, x, y, 0xFFFFFF)
        context.drawTextWithShadow(textRenderer, matchText, x, y + 10, 0xFFFFFF)
        context.drawTextWithShadow(textRenderer, percentText, x, y + 20, 0xFFFFFF)
        context.drawTextWithShadow(textRenderer, streakText, x, y + 30, 0xFFFFFF)
        context.drawTextWithShadow(textRenderer, longestText, x, y + 40, 0xFFFFFF)

    }
}