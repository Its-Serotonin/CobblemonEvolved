package com.serotonin.common.client.gui

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.Identifier
import org.lwjgl.opengl.GL11

fun drawCrispTexture(
    context: DrawContext,
    texture: Identifier,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    scale: Float = 1.0f
) {
    val matrices = context.matrices
    matrices.push()
    matrices.scale(scale, scale, 1f)

    RenderSystem.setShaderTexture(0, texture)

    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)

    RenderSystem.enableBlend()
    RenderSystem.defaultBlendFunc()
    RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

    context.drawTexture(texture, (x / scale).toInt(), (y / scale).toInt(), 0f, 0f, width, height, width, height)
    matrices.pop()
}
