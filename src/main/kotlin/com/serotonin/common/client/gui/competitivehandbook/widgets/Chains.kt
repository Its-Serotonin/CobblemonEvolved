package com.serotonin.common.client.gui.competitivehandbook.widgets
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.util.Identifier
import org.lwjgl.opengl.GL11


class Chains(
    private val x: Int,
    private val y: Int,
    private val width: Int,
    private val height: Int,
) : Drawable {

    private val texture = Identifier.of("cobblemonevolved", "textures/gui/competitivehandbook/chains.png")

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        RenderSystem.setShaderTexture(0, texture)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)

        context.drawTexture(
            texture,
            x, y,
            0f, 0f,
            width, height,
            width, height
        )
    }
}