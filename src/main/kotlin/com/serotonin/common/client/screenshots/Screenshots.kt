package com.serotonin.common.client.screenshots



import com.serotonin.common.client.gui.saveslots.SaveSlotScreen
import com.serotonin.common.client.screenshots.ScreenshotHelper.captureAndSaveScreenshot
import com.serotonin.common.saveslots.ClientSaveSlotCache
import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.NativeImage
import net.minecraft.util.Identifier
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

object ScreenshotHelper {

    private var isCapturing = false

    //this shit will crash if the mask isnt there
    val maskImage: BufferedImage by lazy {
        val client = MinecraftClient.getInstance()
        val stream = client.resourceManager
            .getResource(Identifier.of("cobblemonevolved", "textures/gui/saveslots/slot_icon_mask.png"))
            .get().inputStream
        ImageIO.read(stream)
    }


    fun captureScreenshot(slot: Int, timestamp: Long, callback: (ByteArray) -> Unit) {
        val client = MinecraftClient.getInstance()
        val framebuffer = client.framebuffer
        framebuffer.beginRead()
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

        val image = nativeImage.toBufferedImage()
            .resize(80, 40)
            .applyRhombusMask(maskImage.getSubimage(0, 0, 80, 40))
        nativeImage.close()

        val outputStream = ByteArrayOutputStream()
        try {
            ImageIO.write(image, "PNG", outputStream)
            callback(outputStream.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
            println("Failed to encode screenshot: ${e.message}")
        }
    }



    fun captureAndSaveScreenshot(slot: Int, callback: (String) -> Unit) {
        if (isCapturing) {
            println("Screenshot already in progress, skipping.")
            return
        }
        isCapturing = true

        val timestamp = System.currentTimeMillis()
        val filename = "save_slot_${slot}_$timestamp.png"
        val outputDir = File(MinecraftClient.getInstance().runDirectory, "screenshots/saveslots")

        outputDir.mkdirs()
        val outputFile = File(outputDir, filename)

        captureScreenshot(slot, timestamp) { bytes ->
            try {
                outputFile.writeBytes(bytes)
            } catch (e: Exception) {
                e.printStackTrace()
                println("Failed to save screenshot: ${e.message}")

                isCapturing = false
                ClientSaveSlotCache.updateSlot(slot, System.currentTimeMillis(), null)

                val screen = MinecraftClient.getInstance().currentScreen
                if (screen is SaveSlotScreen) {
                    screen.refreshSlotButtons()
                }

                return@captureScreenshot
            }

            ClientSaveSlotCache.updateSlot(
                slot,
                System.currentTimeMillis(),
                "screenshots/saveslots/$filename"
            )

            val screen = MinecraftClient.getInstance().currentScreen
            if (screen is SaveSlotScreen) {
                screen.refreshSlotButtons()
            }

            callback(outputFile.absolutePath)
            isCapturing = false
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

    private fun BufferedImage.resize(width: Int, height: Int): BufferedImage {
        val resized = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = resized.createGraphics()
        g.drawImage(this, 0, 0, width, height, null)
        g.dispose()
        return resized
    }

    fun BufferedImage.applyRhombusMask(mask: BufferedImage): BufferedImage {
        val result = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val maskAlpha = (mask.getRGB(x, y) shr 24) and 0xFF
                val color = this.getRGB(x, y)
                val newColor = (maskAlpha shl 24) or (color and 0x00FFFFFF)
                result.setRGB(x, y, newColor)
            }
        }

        return result
    }
}

fun captureBeforeOpeningSaveSlotScreen(slot: Int, openGui: () -> Unit) {
    captureAndSaveScreenshot(slot) {
        MinecraftClient.getInstance().execute {
            openGui()
        }
    }
}

fun cleanupTempScreenshots() {
    val dir = File(MinecraftClient.getInstance().runDirectory, "screenshots/saveslots")
    dir.listFiles { it.name.startsWith("temp_") && it.name.endsWith(".png") }?.forEach { it.delete() }
}

fun cleanupUnusedScreenshots(usedPaths: Set<String>) {
    val screenshotDir = File(MinecraftClient.getInstance().runDirectory, "screenshots/saveslots")
    if (!screenshotDir.exists()) return

    val usedCanonicalPaths = usedPaths.mapNotNull { path ->
        try {
            File(MinecraftClient.getInstance().runDirectory, path).canonicalPath
        } catch (e: Exception) {
            null
        }
    }.toSet()

    screenshotDir.listFiles { it.name.endsWith(".png") }?.forEach { file ->
        try {
            if (file.canonicalPath !in usedCanonicalPaths) {
                println("Deleting unused screenshot: ${file.name}")
                file.delete()
            }
        } catch (e: Exception) {
            println("Failed to process screenshot file: ${file.name}")
        }
    }
}

fun extractTimestamp(fileName: String): Long {
    val name = fileName.removePrefix("save_slot_").removeSuffix(".png")
    val underscoreIndex = name.indexOf('_')
    return if (underscoreIndex != -1) {
        name.substring(underscoreIndex + 1).toLongOrNull() ?: 0L
    } else 0L
}

/*

fun cleanupUnusedScreenshots(usedSlots: Set<Int>) {
    val screenshotDir = File(MinecraftClient.getInstance().runDirectory, "screenshots/saveslots")
    if (!screenshotDir.exists()) return

    val filesBySlot = mutableMapOf<Int, MutableList<File>>()


    screenshotDir.listFiles { file ->
        file.name.startsWith("save_slot_") && file.name.endsWith(".png")
    }?.forEach { file ->
        val nameWithoutPrefix = file.name.removePrefix("save_slot_").removeSuffix(".png")
        val parts = nameWithoutPrefix.split("_")
        val slot = parts.firstOrNull()?.toIntOrNull()

        if (slot != null) {
            filesBySlot.computeIfAbsent(slot) { mutableListOf() }.add(file)
        }
    }

    for ((slot, files) in filesBySlot) {
        if (slot in usedSlots) {
            val sorted = files.sortedByDescending { extractTimestamp(it.name) }
            sorted.drop(1).forEach { it.delete() }
        } else {
            val mostRecent = files.maxByOrNull { extractTimestamp(it.name) }
            files.filter { it != mostRecent }.forEach { it.delete() }
        }
    }
}
 */