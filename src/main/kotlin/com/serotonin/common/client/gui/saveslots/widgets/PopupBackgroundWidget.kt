package com.serotonin.common.client.gui.saveslots.widgets

import com.mojang.blaze3d.systems.RenderSystem
import com.serotonin.common.client.gui.competitivehandbook.CompetitiveHandbookGUIConstants.BASE_WIDTH
import com.serotonin.common.client.gui.competitivehandbook.CompetitiveHandbookGUIConstants.BASE_HEIGHT
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import net.minecraft.util.Identifier
import org.lwjgl.opengl.GL11

class PopupBackgroundWidget(
    private val x: Int,
    private val y: Int
) : Drawable, Element {
    private var focused: Boolean = false

    private val texture = Identifier.of("cobblemonevolved", "textures/gui/saveslots/popup_background.png")

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        RenderSystem.setShaderTexture(0, texture)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
        RenderSystem.disableDepthTest()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

        context.drawTexture(texture, x, y, 0f, 0f, BASE_WIDTH, BASE_HEIGHT, BASE_WIDTH, BASE_HEIGHT)
    }

    override fun setFocused(focused: Boolean) {
        this.focused = focused
    }

    override fun isFocused(): Boolean {
        return focused
    }


    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean = false
    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean = false
    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean = false
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = false
    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = false
    override fun charTyped(chr: Char, modifiers: Int): Boolean = false
}