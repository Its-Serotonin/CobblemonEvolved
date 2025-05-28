package com.serotonin.common.client.gui.saveslots.widgets

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.lwjgl.opengl.GL11


class PopupButtonWidget(
    x: Int,
    y: Int,
    private val label: String,
    private val onPressAction: () -> Unit
) : PressableWidget(x, y, 79, 30, Text.of(label)) {

    private val texture = Identifier.of("cobblemonevolved", "textures/gui/saveslots/popup_buttons.png")

    override fun onPress() {
        onPressAction()
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(x, y, x + width, y + height, 0x8800FF00.toInt()) // semi-transparent green box

        RenderSystem.setShaderTexture(0, texture)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
        RenderSystem.disableDepthTest()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)


        context.drawTexture(texture, x, y, 0f, 0f, width, height, width, height)

        if (isMouseOver(mouseX.toDouble(), mouseY.toDouble())) {
            context.fill(x, y, x + width, y + height, 0x22FFFFFF) // transparent white overlay
        }

        val textRenderer = MinecraftClient.getInstance().textRenderer
        val textX = x + (width - textRenderer.getWidth(label)) / 2
        val textY = y + (height - 8) / 2
        context.drawTextWithShadow(textRenderer, label, textX, textY, 0xFFFFFF)
    }

    override fun playDownSound(soundManager: net.minecraft.client.sound.SoundManager) {
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        builder.put(NarrationPart.TITLE, Text.of(label))
    }
}