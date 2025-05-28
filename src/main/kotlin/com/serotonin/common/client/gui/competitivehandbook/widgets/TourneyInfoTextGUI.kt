package com.serotonin.common.client.gui.competitivehandbook.widgets


import com.serotonin.common.client.gui.competitivehandbook.CompetitiveHandbookGUIConstants.BASE_WIDTH
import com.serotonin.common.tourneys.TournamentManager
import com.serotonin.common.tourneys.TournamentManagerClient
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class TournamentInfoTextGUI(private val x: Int, private val y: Int) {

    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a z").withZone(zoneId)

    fun render(context: DrawContext) {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        val textRenderer = client.textRenderer
        val uuid = player.uuid

        val isSignedUp = TournamentManagerClient.isSignedUp(uuid)
        val tournament = TournamentManagerClient.getCachedTournament()

        val lines = mutableListOf<String>()

        if (tournament == null || tournament.ruleset == "None" || tournament.startTime == "Not set") {
            lines.add("§7No active tournament.")

        } else {
            try {
                val startTime = Instant.parse(tournament.startTime)
                val now = Instant.now()
                val duration = Duration.between(now, startTime)
                val timeSinceStart = Duration.between(startTime, now)
                val hours = duration.toHours()
                val minutes = duration.toMinutesPart()
                val timeZone = java.time.ZoneId.systemDefault().id

                // Dynamically determine status
                val statusText = when {
                    now.isBefore(startTime) && duration.toMinutes() <= 1 -> "§a§lStarts now!"
                    now.isBefore(startTime) -> "§aUpcoming"
                    timeSinceStart.toHours() >= 2 -> "§cFinished"
                    else -> "§aOngoing"
                }

                lines.add("§fRuleset: §b${tournament.ruleset}")
                val formattedTime = timeFormatter.format(startTime)
                lines.add("§fStart: §a$formattedTime")
                lines.add("§fStatus: $statusText")

                if (now.isBefore(startTime)) {
                    lines.add("§fStarts in: §d${hours}h ${minutes}m")
                }
            } catch (e: Exception) {
                lines.add("§fStart: §7N/A")
                lines.add("§fStatus: §7N/A")
            }
        }

        lines.add(if (isSignedUp) "§a✔ You are signed up!" else "§c✖ You are not signed up.")

        var yOffset = 0


        for (line in lines) {
            val lineWidth = textRenderer.getWidth(line)
            val panelCenterX = x + (BASE_WIDTH / 2)

            context.drawTextWithShadow(
                textRenderer,
                Text.literal(line),
                panelCenterX,
                y + yOffset,
                0xFFFFFF
            )
            yOffset += 10
        }
    }
}