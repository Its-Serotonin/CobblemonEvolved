package com.serotonin.common.client.gui.effects

import net.minecraft.client.gui.DrawContext
import net.minecraft.util.math.MathHelper
import kotlin.random.Random

class GuiParticle(
    var x: Float,
    var y: Float,
    val vx: Float = Random.nextFloat() * 1f - 0.5f,
    val vy: Float = -Random.nextFloat() * 0.5f - 0.5f,
    val color: Int = 0xFFFFFF,
    val maxAge: Int = 70
) {
    private var age: Int = 0

    fun update() {
        x += vx
        y += vy
        age++
    }

    fun render(context: DrawContext) {
        val alpha = 1f - (age.toFloat() / maxAge)
        val fadedColor = (color and 0x00FFFFFF) or (MathHelper.clamp((alpha * 255).toInt(), 0, 255) shl 24)
        context.fill(x.toInt(), y.toInt(), x.toInt() + 2, y.toInt() + 2, fadedColor)
    }

    fun isAlive(): Boolean = age < maxAge
}

object GuiParticleManager {
    private val particles = mutableListOf<GuiParticle>()

    fun spawnConfetti(x: Int, y: Int, count: Int = 10) {
        repeat(count) {
            particles.add(
                GuiParticle(
                    x = x.toFloat(),
                    y = y.toFloat(),
                    color = listOf(0xFFFF55, 0x55FF55, 0x5555FF, 0xFF55FF, 0x55FFFF).random()
                )
            )
        }
    }

    fun tick() {
        particles.removeIf { !it.isAlive() }
        particles.forEach { it.update() }
    }

    fun render(context: DrawContext) {
        particles.forEach { it.render(context) }
    }
}
