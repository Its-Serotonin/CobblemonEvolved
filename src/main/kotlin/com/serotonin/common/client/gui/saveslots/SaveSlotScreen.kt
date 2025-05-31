package com.serotonin.common.client.gui.saveslots

import com.serotonin.common.client.gui.competitivehandbook.CompetitiveHandbookGUIConstants
import com.serotonin.common.client.gui.saveslots.widgets.SaveSlotBackgroundWidget
import com.serotonin.common.client.gui.saveslots.widgets.SaveSlotButtonSet
import com.serotonin.common.client.screenshots.deleteScreenshotsForSlot
import com.serotonin.common.networking.PlayerDataSyncNetworkingClient
import com.serotonin.common.networking.SaveSlotRequestPayload
import com.serotonin.common.registries.SoundRegistry
import com.serotonin.common.saveslots.ClientSaveSlotCache
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier

class SaveSlotScreen : Screen(Text.of("Save Slots")) {
    private var lastActionTime = 0L
    private val debounceMillis = 1000
    private val slotButtons = mutableListOf<SaveSlotButtonSet>()
    var suppressOpenSound = false
    private val backgroundTexture = Identifier.of("cobblemonevolved", "textures/gui/saveslots/saveslot_base.png")

    override fun init() {
        slotButtons.clear()

        if (!suppressOpenSound) {
            MinecraftClient.getInstance().soundManager.play(
                PositionedSoundInstance.master(SoundRegistry.SAVE_SLOT_OPEN, 1.0f, 1.3f)
            )
        }
        suppressOpenSound = false

        val bgX = (width - CompetitiveHandbookGUIConstants.BASE_WIDTH) / 2
        val bgY = (height - CompetitiveHandbookGUIConstants.BASE_HEIGHT) / 2

        val backgroundWidget = SaveSlotBackgroundWidget(
            texture = backgroundTexture,
            x = bgX,
            y = bgY,
            width = CompetitiveHandbookGUIConstants.BASE_WIDTH,
            height = CompetitiveHandbookGUIConstants.BASE_HEIGHT
        )

        this.addDrawable(backgroundWidget)

        val slotOffsets = listOf(
            Pair(17, 32),   // Slot 1
            Pair(37, 84),   // Slot 2
            Pair(57, 136)   // Slot 3
        )

        for (i in 1..3) {
            val lastSaved = ClientSaveSlotCache.getSlot(i)
            val (offsetX, offsetY) = slotOffsets[i - 1]

            val buttonSet = SaveSlotButtonSet(
                slot = i,
                x = bgX + offsetX,
                y = bgY + offsetY,
                slotMetadata = lastSaved,
                isCurrentSlot = ClientSaveSlotCache.isActive(i),
                isPopupActive = { false },
                onActivate = onActivate@{
                    if (ClientSaveSlotCache.isActive(i)) {
                        val now = System.currentTimeMillis()
                        if (now - lastActionTime < debounceMillis) return@onActivate
                        lastActionTime = now

                        ClientSaveSlotCache.updateSlot(i, System.currentTimeMillis())

                        MinecraftClient.getInstance().soundManager.play(
                            PositionedSoundInstance.master(SoundRegistry.SAVE_LOAD_SLOT, 1.0f, 1.0f)
                        )

                        refreshSlotButtons()
                    } else {
                        MinecraftClient.getInstance().soundManager.play(
                            PositionedSoundInstance.master(SoundRegistry.SWITCH_SLOTS_POPUP, 1.0f, 1.0f)
                        )

                        MinecraftClient.getInstance().setScreen(
                            ConfirmationPopupScreen(
                                message = Text.literal("Switch to save slot $i?").formatted(Formatting.WHITE),
                                onConfirm = {
                                    PlayerDataSyncNetworkingClient.sendSaveSlotRequest(
                                        i,
                                        SaveSlotRequestPayload.Action.SWITCH
                                    )
                                    ClientSaveSlotCache.updateSlot(i, System.currentTimeMillis())

                                    val newMetadata = ClientSaveSlotCache.getSlot(i)
                                    slotButtons.find { it.slot == i }?.refreshScreenshot(newMetadata)

                                    MinecraftClient.getInstance().soundManager.play(
                                        PositionedSoundInstance.master(SoundRegistry.CONFIRM, 1.0f, 1.0f)
                                    )
                                    refreshSlotButtons()
                                },
                                onCancel = {
                                    MinecraftClient.getInstance().soundManager.play(
                                        PositionedSoundInstance.master(SoundRegistry.CANCEL, 1.0f, 1.0f)
                                    )
                                },
                                parent = this,
                            )
                        )
                    }
                },
                onDeleteRequest = {
                    MinecraftClient.getInstance().soundManager.play(
                        PositionedSoundInstance.master(SoundRegistry.DELETE_SLOT_POPUP, 1.0f, 1.2f)
                    )

                    MinecraftClient.getInstance().setScreen(
                        ConfirmationPopupScreen(
                            message = Text.literal("Are you sure you want to delete this save?").formatted(Formatting.RED),

                                    onConfirm = {
                                PlayerDataSyncNetworkingClient.sendSaveSlotRequest(
                                    i,
                                    SaveSlotRequestPayload.Action.DELETE
                                )
                                MinecraftClient.getInstance().soundManager.play(
                                    PositionedSoundInstance.master(SoundRegistry.CONFIRM_DELETE, 1.0f, 1.0f)
                                )


                                        deleteScreenshotsForSlot(i)

                                refreshSlotButtons()
                            },
                            onCancel = {
                                MinecraftClient.getInstance().soundManager.play(
                                    PositionedSoundInstance.master(SoundRegistry.CANCEL, 1.0f, 1.0f)
                                )
                            },
                            parent = this,
                        )
                    )
                },
                add = { widget ->
                    when (widget) {
                        is PressableWidget -> {
                            addDrawableChild(widget)
                        }
                        is Drawable -> {
                            addDrawable(widget)
                        }
                        else -> {
                            println("Unknown widget type: ${widget::class.simpleName}")
                        }
                    }
                }
            )

            buttonSet.init()
            slotButtons.add(buttonSet)
            addDrawable(buttonSet)
        }
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val client = MinecraftClient.getInstance()
        if (client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            client.soundManager.play(
                PositionedSoundInstance.master(SoundRegistry.SAVE_SLOT_CLOSE, 1.0f, 1.3f)
            )
            client.setScreen(null)
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun close() {
        MinecraftClient.getInstance().soundManager.play(
            PositionedSoundInstance.master(SoundRegistry.SAVE_SLOT_CLOSE, 1.0f, 1.3f)
        )
        super.close()
    }

    fun refreshSlotButtons() {
        this.clearChildren()
        this.slotButtons.clear()
        suppressOpenSound = true
        this.init()
        slotButtons.forEach { button ->
            val metadata = ClientSaveSlotCache.getSlot(button.slot)
            button.refreshScreenshot(metadata)
        }
        /*
        cleanupUnusedScreenshots(
            usedSlots = setOf(1, 2, 3).filter { ClientSaveSlotCache.getSlot(it)?.screenshotPath != null }.toSet()
        )*/
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        renderPlayerName(context)
    }

    override fun shouldPause(): Boolean {
        return false
    }

    private fun renderPlayerName(context: DrawContext) {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        val username = player.gameProfile.name

        val nameText = "$username§r"

        val scale = 1.00f
        val renderer = client.textRenderer

        val centerX = (width - CompetitiveHandbookGUIConstants.BASE_WIDTH) / 2
        val centerY = (height - CompetitiveHandbookGUIConstants.BASE_HEIGHT) / 2
        val textX = centerX + CompetitiveHandbookGUIConstants.BASE_WIDTH - 279
        val textY = centerY + 14

        val matrices = context.matrices
        matrices.push()

        matrices.translate(textX.toDouble(), textY.toDouble(), 0.0)
        matrices.scale(scale, scale, 1.0f)

        context.drawTextWithShadow(
            renderer,
            Text.of(nameText),
            -renderer.getWidth(nameText),
            0,
            0xFFFFFF
        )
        matrices.pop()
    }
}