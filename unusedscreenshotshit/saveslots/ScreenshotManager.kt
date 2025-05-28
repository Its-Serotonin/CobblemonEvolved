package com.serotonin.common.saveslots

import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.NativeImage
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import java.io.File

object ScreenshotManager {
    private var pendingCapture: Boolean = false

    fun capturePendingForSlotScreen() {
        pendingCapture = true
    }

    fun tick(client: MinecraftClient) {
        if (pendingCapture) {
            pendingCapture = false
            captureScreenshot(client)
        }
    }

    private fun captureScreenshot(client: MinecraftClient) {
        val framebuffer = client.framebuffer
        val width = framebuffer.textureWidth
        val height = framebuffer.textureHeight

        // Allocate buffer
        val buffer = BufferUtils.createByteBuffer(width * height * 4) // RGBA = 4 bytes

        // Read pixels directly from the bound framebuffer
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer)

        val image = NativeImage(width, height, false)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = (x + y * width) * 4
                val r = buffer.get(i).toInt() and 0xFF
                val g = buffer.get(i + 1).toInt() and 0xFF
                val b = buffer.get(i + 2).toInt() and 0xFF
                val a = buffer.get(i + 3).toInt() and 0xFF

                image.setColor(x, height - 1 - y, (a shl 24) or (r shl 16) or (g shl 8) or b)
            }
        }

        val file = File("screenshots/save_slot_preview.png")
        image.writeTo(file)

        image.close()
    }
}