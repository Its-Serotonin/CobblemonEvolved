package com.serotonin.common.client.gui.saveslots.widgets

import com.mojang.blaze3d.systems.RenderSystem
import com.serotonin.common.client.screenshots.extractTimestamp
import com.serotonin.common.saveslots.ClientSaveSlotCache
import com.serotonin.common.saveslots.ClientSaveSlotMetadata
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.lwjgl.opengl.GL11
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SlotIconWidget(
    x: Int,
    y: Int,
    private val slot: Int,
    private var slotMetadata: ClientSaveSlotMetadata?,
    private val onPressAction: () -> Unit,
    private val isClickExcluded: ((Double, Double) -> Boolean)? = null
) : PressableWidget(x, y, 281, 47, Text.empty()) {

    private val texture = Identifier.of("cobblemonevolved", "textures/gui/saveslots/slot_icon.png")
    private val hoverTexture = Identifier.of("cobblemonevolved", "textures/gui/saveslots/slot_icon_hover.png")

    private var screenshotTexture: Identifier? = null

    init {
        loadScreenshotTexture()
    }
    var dynamicTexture: NativeImageBackedTexture? = null

    companion object {
        private val screenshotCache = mutableMapOf<String, Identifier>()
        private val textureObjects = mutableMapOf<String, NativeImageBackedTexture>()

        fun clearScreenshotCache() {
            for ((_, tex) in textureObjects) {
                tex.close()
            }
            screenshotCache.clear()
            textureObjects.clear()
        }
    }


    private fun loadScreenshotTexture() {
        val client = MinecraftClient.getInstance()
        val path = slotMetadata?.screenshotPath

        try {
            val file = if (path != null) {
                File(path)
            } else {
                val slotDir = File(client.runDirectory, "screenshots/saveslots")
                slotDir.listFiles { f -> f.name.startsWith("save_slot_${slot}_") && f.name.endsWith(".png") }
                    ?.maxByOrNull { extractTimestamp(it.name) }
            }

            if (file == null || !file.exists()) {
                screenshotTexture = Identifier.of("cobblemonevolved", "textures/gui/saveslots/default_preview.png")
                return
            }

            val key = file.absolutePath
            screenshotTexture = screenshotCache[key]

            if (screenshotTexture == null) {
                val image = NativeImage.read(file.inputStream())
                val tex = NativeImageBackedTexture(image)
                val id = client.textureManager.registerDynamicTexture("save_slot_preview_$slot", tex)
                screenshotCache[key] = id
                textureObjects[key] = tex
                screenshotTexture = id
            }
        } catch (e: Exception) {
            println("Failed to load screenshot for slot $slot: ${e.message}")
            screenshotTexture = Identifier.of("cobblemonevolved", "textures/gui/saveslots/default_preview.png")
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (isClickExcluded?.invoke(mouseX, mouseY) == true) {
            return false
        }

        if (isMouseOver(mouseX, mouseY)) {
            onPress()
            return true
        }

        return false
    }

    override fun onPress() {
        onPressAction()
    }

    override fun playDownSound(soundManager: net.minecraft.client.sound.SoundManager) {}

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val hovered = isMouseOver(mouseX.toDouble(), mouseY.toDouble())
        val tex = if (hovered) hoverTexture else texture


            //80x40 ; x should = 188
        screenshotTexture?.let {
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.setShaderTexture(0, it)
            context.drawTexture(it, x + 188, y + 4, 0f, 0f, 80, 40, 80, 40)
            RenderSystem.disableBlend()
        }


        RenderSystem.setShaderTexture(0, tex)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
        RenderSystem.disableDepthTest()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        context.drawTexture(tex, x, y, 0f, 0f, width, height, width, height)


        val textRenderer = MinecraftClient.getInstance().textRenderer
        val slotLabel = "Slot $slot"
        val dateAndTime = slotMetadata?.lastSaved?.let {
            val instant = Instant.ofEpochMilli(it)
            val zone = instant.atZone(ZoneId.systemDefault())
            zone.format(DateTimeFormatter.ofPattern("yy/MM/dd hh:mm a"))
        } ?: "--/--/-- 00:00"

        val textX = x + 12
        val timeX = x + 18
        context.drawText(textRenderer, slotLabel, textX, y + 10, 0xFFFFFF, false)
        context.drawText(textRenderer, dateAndTime, timeX, y + 29, 0xFFFFFF, false)
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        builder.put(NarrationPart.TITLE, Text.of("Save Slot $slot"))
    }

    fun refreshScreenshot(newMetadata: ClientSaveSlotMetadata?) {
        if (newMetadata?.screenshotPath != slotMetadata?.screenshotPath) {
            this.slotMetadata = newMetadata
            loadScreenshotTexture()
        } else {

            loadScreenshotTexture()
        }
    }
}