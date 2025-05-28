package com.serotonin.common.client.gui.competitivehandbook.widgets

import com.mojang.blaze3d.systems.RenderSystem
import com.serotonin.common.registries.SoundRegistry.FRIENDLY_BUTTON_OFF
import com.serotonin.common.registries.SoundRegistry.FRIENDLY_BUTTON_ON
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.lwjgl.opengl.GL11


class FriendlyBattleButtons(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val isGreen: Boolean,
    private val getCurrentState: () -> Boolean,
    private val onToggle: (Boolean) -> Unit
) : PressableWidget(x, y, width, height, Text.empty()) {

    private val greenOn = Identifier.of("cobblemonevolved", "textures/gui/competitivehandbook/green_button_on.png")
    private val greenOff = Identifier.of("cobblemonevolved", "textures/gui/competitivehandbook/green_button_off.png")
    private val redOn = Identifier.of("cobblemonevolved", "textures/gui/competitivehandbook/red_button_on.png")
    private val redOff = Identifier.of("cobblemonevolved", "textures/gui/competitivehandbook/red_button_off.png")

    private var lastClickTime = 0L
    private val CLICK_COOLDOWN_MS = 500



    override fun onClick(mouseX: Double, mouseY: Double) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < CLICK_COOLDOWN_MS) return
        lastClickTime = currentTime

        val newState = !getCurrentState()
        onToggle(newState)


        val sound = if (newState) FRIENDLY_BUTTON_ON else FRIENDLY_BUTTON_OFF
        MinecraftClient.getInstance().player?.playSound(sound, 0.5f, 1.0f)


    }

    override fun onPress() {}

    override fun playDownSound(soundManager: net.minecraft.client.sound.SoundManager) {

    }

    protected override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val isFriendly = getCurrentState()
        val texture = when {
            isGreen && isFriendly -> greenOff // friendly is ON, green is pressed
            isGreen && !isFriendly -> greenOn // friendly is OFF, green is up
            !isGreen && isFriendly -> redOn  // friendly is ON, red is up
            else -> redOff                   // friendly is OFF, red is pressed
        }

        val textureX = x + (width - 8) / 2
        val textureY = y + (height - 8) / 2


        RenderSystem.setShaderTexture(0, texture)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)

        context.drawTexture(texture, x, y, 0f, 0f, width, height, width, height)
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        builder.put(
            net.minecraft.client.gui.screen.narration.NarrationPart.TITLE,
            Text.of("Toggle Friendly Battle Mode")
        )
    }
}