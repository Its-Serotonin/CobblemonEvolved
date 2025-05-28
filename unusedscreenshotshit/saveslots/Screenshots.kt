package com.serotonin.common.saveslots



import com.serotonin.common.client.gui.saveslots.SaveSlotScreen
import com.serotonin.common.networking.PlayerDataSyncNetworkingClient
import com.serotonin.common.networking.SaveSlotRequestPayload
import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.util.ScreenshotRecorder
import net.minecraft.client.util.Window
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.function.Consumer
import javax.imageio.ImageIO
import kotlin.math.abs

object ScreenshotHelper {
    fun captureScreenshot(callback: (ByteArray) -> Unit) {
        val client = MinecraftClient.getInstance()
        val framebuffer = client.framebuffer
        val width = framebuffer.textureWidth
        val height = framebuffer.textureHeight

        val buffer = BufferUtils.createByteBuffer(width * height * 4)
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer)

        val nativeImage = NativeImage(width, height, false)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = (x + y * width) * 4
                val r = buffer.get(i).toInt() and 0xFF
                val g = buffer.get(i + 1).toInt() and 0xFF
                val b = buffer.get(i + 2).toInt() and 0xFF
                val a = buffer.get(i + 3).toInt() and 0xFF
                nativeImage.setColor(x, height - 1 - y, (a shl 24) or (r shl 16) or (g shl 8) or b)
            }
        }

        val image = nativeImage.toBufferedImage().applyRhombusMask() // ← Apply mask here
        nativeImage.close()

        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, "PNG", outputStream)
        callback(outputStream.toByteArray())
    }

    fun captureAndSaveScreenshot(slot: Int, callback: (String) -> Unit) {
        val filename = "save_slot_$slot.png"
        val outputFile = File(MinecraftClient.getInstance().runDirectory, "screenshots/$filename")

        captureScreenshot { bytes ->
            outputFile.parentFile.mkdirs()
            outputFile.writeBytes(bytes)

            ClientSaveSlotCache.updateSlot(
                slot,
                System.currentTimeMillis(),
                "screenshots/$filename"
            )

            val screen = MinecraftClient.getInstance().currentScreen
            if (screen is SaveSlotScreen) {
                screen.refreshSlotButtons()
            }

            callback(outputFile.absolutePath)
        }
    }



    private fun NativeImage.toBufferedImage(): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        for (x in 0 until width) {
            for (y in 0 until height) {
                image.setRGB(x, y, getColor(x, y))
            }
        }
        return image
    }

    private fun BufferedImage.applyRhombusMask(): BufferedImage {
        val w = this.width
        val h = this.height
        val centerX = w / 2
        val centerY = h / 2

        val masked = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val dx = abs(x - centerX)
                val dy = abs(y - centerY)
                val rhombusLimit = (h / 2) * (1 - dx.toDouble() / centerX)
                if (dy <= rhombusLimit) {
                    masked.setRGB(x, y, this.getRGB(x, y))
                } else {
                    masked.setRGB(x, y, 0x00000000)
                }
            }
        }

        return masked
    }
}

fun sendSaveSlotWithScreenshot(slot: Int) {
    ScreenshotHelper.captureAndSaveScreenshot(slot) { screenshotPath ->
        MinecraftClient.getInstance().execute {
            val payload = SaveSlotRequestPayload(
                slot = slot,
                action = SaveSlotRequestPayload.Action.SAVE,
            )
            PlayerDataSyncNetworkingClient.sendSaveSlotRequest(payload)
        }
    }
}

fun cleanupUnusedScreenshots(usedSlots: Set<Int>) {
    val screenshotDir = File(MinecraftClient.getInstance().runDirectory, "screenshots")
    if (!screenshotDir.exists()) return

    screenshotDir.listFiles { file ->
        file.name.startsWith("save_slot_") && file.name.endsWith(".png")
    }?.forEach { file ->
        val slotNumber = file.name.removePrefix("save_slot_").removeSuffix(".png").toIntOrNull()
        if (slotNumber != null && slotNumber !in usedSlots) {
            println("🧹 Deleting unused screenshot: ${file.name}")
            file.delete()
        }
    }
}
