package com.serotonin.common.client.gui.competitivehandbook.widgets

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.util.Identifier
import org.lwjgl.opengl.GL11

class TournamentInfoAnimationWidget(
    private val x: Int,
    private val y: Int,
    private val frameWidth: Int = 89,
    private val frameHeight: Int = 31,
    private val totalFrames: Int = 185,
    private val fps: Int = 15
) : Drawable {

    private val texture = Identifier.of("cobblemonevolved", "textures/gui/competitivehandbook/animated/tournamentinfoanim.png")
    private val textureWidth = 507
    private val textureHeight = 1157
    private val startTime = System.currentTimeMillis()
    private val speedMultiplier = 0.25

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val elapsedTime = ((System.currentTimeMillis() - startTime) * speedMultiplier).toLong()
        val frameDurationMs = 1000 / fps
        val frameIndex = ((elapsedTime / frameDurationMs) % totalFrames).toInt()
        val column = frameIndex / 37
        val row = frameIndex % 37
        val u = column * frameWidth
        val v = row * frameHeight


        RenderSystem.setShaderTexture(0, texture)
        RenderSystem.enableBlend()
        RenderSystem.disableDepthTest()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)

        context.drawTexture(
            texture,
            x, y,
            u.toFloat(), v.toFloat(),
            frameWidth, frameHeight,
            textureWidth, textureHeight
        )
    }
}