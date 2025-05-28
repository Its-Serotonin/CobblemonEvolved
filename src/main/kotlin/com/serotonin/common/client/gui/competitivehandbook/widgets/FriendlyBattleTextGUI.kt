package com.serotonin.common.client.gui.competitivehandbook.widgets

import com.serotonin.common.registries.FriendlyBattleManager
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class FriendlyBattleStatusTextGUI(private val x: Int, private val y: Int) {

    fun render(context: DrawContext) {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        val isFriendly = FriendlyBattleManager.getCachedSetting(player.uuid)
        val textRenderer = client.textRenderer

        val labelColor = if (isFriendly) Formatting.GREEN else Formatting.RED
        val labelText = Text.literal("Friendly Battles").formatted(labelColor)

        context.drawTextWithShadow(textRenderer, labelText, x, y, 0xFFFFFF)
    }
}