package com.serotonin.common.client.gui.competitivehandbook.widgets

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import net.minecraft.util.Identifier
import org.joml.Quaternionf
import org.lwjgl.opengl.GL11

class TournamentInfoAnimationWidget(
    private val x: Int,
    private val y: Int,
    private val frameWidth: Int = 89,
    private val frameHeight: Int = 31,
    private val totalFrames: Int = 211,
    private val fps: Int = 15
) : Drawable {

    private val texture = Identifier.of("cobblemonevolved", "textures/gui/competitivehandbook/animated/tournamentinfoanim.png")
    private val textureWidth = 507
    private val textureHeight = 1157

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val ticks = MinecraftClient.getInstance().world?.time?.toInt() ?: 0
        val frameIndex = ((ticks / 20.0) * fps).toInt() % totalFrames

        val (u, v, rotated) = when {
            frameIndex < 185 -> {
                val column = frameIndex / 37
                val row = frameIndex % 37
                Triple(column * frameWidth, row * frameHeight, false)
            }

            frameIndex < 198 -> {
                val row = frameIndex - 185
                val u = row * frameHeight
                val v = 5 * frameWidth
                Triple(u, v, true)
            }

            else -> {
                val row = frameIndex - 198
                val u = row * frameHeight
                val v = 6 * frameWidth
                Triple(u, v, true)
            }
        }


        RenderSystem.setShaderTexture(0, texture)
        RenderSystem.enableBlend()
        RenderSystem.disableDepthTest()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)

        if (rotated) {
            val matrices = context.matrices
            matrices.push()
            matrices.translate(x + frameHeight.toDouble(), y.toDouble(), 0.0)
            matrices.multiply(Quaternionf().rotateZ(java.lang.Math.toRadians(90.0).toFloat()))

            context.drawTexture(
                texture,
                0, 0,
                u.toFloat(), v.toFloat(),
                frameHeight, frameWidth,
                textureWidth, textureHeight
            )
            matrices.pop()
        } else {
            context.drawTexture(
                texture,
                x, y,
                u.toFloat(), v.toFloat(),
                frameWidth, frameHeight,
                textureWidth, textureHeight
            )
        }
    }
}