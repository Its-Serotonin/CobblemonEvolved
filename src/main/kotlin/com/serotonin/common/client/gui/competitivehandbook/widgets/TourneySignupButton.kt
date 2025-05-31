package com.serotonin.common.client.gui.competitivehandbook.widgets



import com.serotonin.common.registries.SoundRegistry
import net.minecraft.text.Text
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.client.sound.PositionedSoundInstance


class TournamentSignupButton(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val getCurrentState: () -> Boolean,
    private val onToggle: (Boolean) -> Unit,
    private val isTournamentActive: () -> Boolean
) : PressableWidget(x, y, width, height, Text.empty()) {

    override fun onPress() {
        if (!isTournamentActive()) return
        val newState = !getCurrentState()

        val sound = if (newState) {
            SoundRegistry.TOURNAMENT_SIGNUP
        } else {
            SoundRegistry.TOURNAMENT_SIGNOUT
        }
        MinecraftClient.getInstance().soundManager.play(PositionedSoundInstance.master(sound, 1.0f, 1.0f))
        onToggle(newState)
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val active = isTournamentActive()

        val text = when {
            !active -> "§7No Tournament"
            getCurrentState() -> "§aSigned Up"
            else -> "§cClick to Join"
        }

        val renderer = MinecraftClient.getInstance().textRenderer
        val xText = x + (width - renderer.getWidth(text)) / 2
        val yText = y + (height - 8) / 2
        context.drawTextWithShadow(renderer, Text.literal(text), xText, yText, 0xFFFFFF)
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder?) {
        builder?.put(NarrationPart.TITLE, Text.literal("Tournament signup button"))
    }

    override fun playDownSound(soundManager: net.minecraft.client.sound.SoundManager) {

    }
}