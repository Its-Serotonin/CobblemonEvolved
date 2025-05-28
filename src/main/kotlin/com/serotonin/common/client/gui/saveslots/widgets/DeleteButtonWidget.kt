package com.serotonin.common.client.gui.saveslots.widgets

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.lwjgl.opengl.GL11

class DeleteButtonWidget(
    x: Int,
    y: Int,
    width: Int = 10,
    height: Int = 10,
    private val onPressAction: () -> Unit
) : PressableWidget(x, y, width, height, Text.of("")) {

    private val normalTexture = Identifier.of("cobblemonevolved", "textures/gui/saveslots/delete_button.png")
    private val hoverTexture = Identifier.of("cobblemonevolved", "textures/gui/saveslots/delete_confirm_button.png")

    override fun onPress() {
        onPressAction()
    }


    override fun playDownSound(soundManager: net.minecraft.client.sound.SoundManager) {

    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val hovered = isMouseOver(mouseX.toDouble(), mouseY.toDouble())
        val texture = if (hovered) hoverTexture else normalTexture


        context.fill(x, y, x + width, y + height, 0x80FF0000.toInt())
        RenderSystem.setShaderTexture(0, texture)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
        RenderSystem.disableDepthTest()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

        context.drawTexture(texture, x, y, 0f, 0f, width, height, width, height)


    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        builder.put(NarrationPart.TITLE, Text.of("Delete save slot"))
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + height

}