package com.serotonin.common.client.gui.saveslots.widgets

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.io.File

class ScreenshotPreviewWidget(
    private val x: Int,
    private val y: Int,
    private val width: Int = 60,
    private val height: Int = 40,
    private val path: String?
) : Drawable {

    private var textureId: Identifier? = null

    init {
        if (path != null) {
            val file = File(path)
            if (file.exists()) {
                try {
                    val native = NativeImage.read(file.inputStream())
                    val id = MinecraftClient.getInstance().textureManager.registerDynamicTexture(
                        "save_slot_preview_${file.nameWithoutExtension}",
                        NativeImageBackedTexture(native)
                    )
                    textureId = id
                } catch (e: Exception) {
                    println("Failed to load screenshot texture: ${e.message}")
                }
            }
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        textureId?.let {
            RenderSystem.setShaderTexture(0, it)
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            context.drawTexture(it, x, y, 0f, 0f, width, height, width, height)
        } ?: run {

            context.drawText(
                MinecraftClient.getInstance().textRenderer,
                Text.literal("No Preview"),
                x + 5, y + 5,
                0xAAAAAA,
                false
            )
        }
    }
}
