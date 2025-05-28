package com.serotonin.common.client.gui.saveslots

import com.serotonin.common.client.gui.competitivehandbook.BackgroundTexture
import com.serotonin.common.client.gui.competitivehandbook.CompetitiveHandbookGUIConstants.BASE_HEIGHT
import com.serotonin.common.client.gui.competitivehandbook.CompetitiveHandbookGUIConstants.BASE_WIDTH
import com.serotonin.common.client.gui.competitivehandbook.widgets.CompetitiveHandbookTextGUI
import com.serotonin.common.client.gui.competitivehandbook.widgets.TournamentInfoTextGUI
import com.serotonin.common.client.gui.saveslots.widgets.DeleteConfirmPopup
import com.serotonin.common.client.gui.saveslots.widgets.Popup
import com.serotonin.common.client.gui.saveslots.widgets.SaveSlotBackgroundWidget
import com.serotonin.common.client.gui.saveslots.widgets.SaveSlotButtonSet
import com.serotonin.common.client.gui.saveslots.widgets.SwitchSlotConfirmPopup
import com.serotonin.common.networking.Database
import com.serotonin.common.networking.PlayerDataSyncNetworkingClient
import com.serotonin.common.networking.SaveSlotRequestPayload
import com.serotonin.common.registries.SoundRegistry
import com.serotonin.common.saveslots.ClientSaveSlotCache
import com.serotonin.common.saveslots.PlayerSaveSlot
import com.serotonin.common.saveslots.SaveSlotDAO
import com.serotonin.common.saveslots.SaveSlotDAOImpl
import com.serotonin.common.saveslots.clearNearbyDroppedItems
import com.serotonin.common.saveslots.deserializeInventory
import com.serotonin.common.saveslots.deserializePC
import com.serotonin.common.saveslots.deserializeParty
import com.serotonin.common.saveslots.serializeInventory
import com.serotonin.common.saveslots.serializePC
import com.serotonin.common.saveslots.serializeParty
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class SaveSlotScreen : Screen(Text.of("Save Slots")) {
    private var confirmPopup: Popup? = null
    private var lastActionTime = 0L
    private val debounceMillis = 1000
    private val slotButtons = mutableListOf<SaveSlotButtonSet>()
    private var suppressOpenSound = false
    private val backgroundTexture = Identifier.of("cobblemonevolved", "textures/gui/saveslots/saveslot_base.png")



    override fun init() {
        slotButtons.clear()

        if (!suppressOpenSound) {
            MinecraftClient.getInstance().soundManager.play(
                PositionedSoundInstance.master(SoundRegistry.SAVE_SLOT_OPEN, 1.0f, 1.3f)
            )
        }
        suppressOpenSound = false


        val bgX = (width - BASE_WIDTH) / 2
        val bgY = (height - BASE_HEIGHT) / 2

        val backgroundWidget = SaveSlotBackgroundWidget(
            texture = backgroundTexture,
            x = bgX,
            y = bgY,
            width = BASE_WIDTH,
            height = BASE_HEIGHT
        )

        this.addDrawable(backgroundWidget)


        val slotBaseX = (width - BASE_WIDTH) / 2
        val slotBaseY = (height - BASE_HEIGHT) / 2

        val slotOffsets = listOf(
            Pair(22, 20),   // Slot 1: x +22, y +20
            Pair(37, 80),   // Slot 2: x +37, y +80
            Pair(52, 140)   // Slot 3: x +52, y +140
        )

        for (i in 1..3) {
            val lastSaved = ClientSaveSlotCache.getSlot(i)
            val (offsetX, offsetY) = slotOffsets[i - 1]


            val buttonSet = SaveSlotButtonSet(
                slot = i,
                x = slotBaseX + offsetX,
                y = slotBaseY + offsetY,
                lastSaved = lastSaved,
                isCurrentSlot = ClientSaveSlotCache.isActive(i),
                isPopupActive = { confirmPopup != null },
                onActivate = onActivate@{
                    if (ClientSaveSlotCache.isActive(i)) {
                        val now = System.currentTimeMillis()
                        if (now - lastActionTime < debounceMillis) return@onActivate
                        lastActionTime = now
                       // SaveSlotSender.sendSaveSlotWithScreenshot(i)
                       // PlayerDataSyncNetworkingClient.sendSaveSlotRequest(i, SaveSlotRequestPayload.Action.SAVE)
                        ClientSaveSlotCache.updateSlot(i, System.currentTimeMillis())

                        MinecraftClient.getInstance().soundManager.play(
                            PositionedSoundInstance.master(SoundRegistry.SAVE_LOAD_SLOT, 1.0f, 1.0f)
                        )

                        refreshSlotButtons()
                    } else {

                        MinecraftClient.getInstance().soundManager.play(
                            PositionedSoundInstance.master(SoundRegistry.SWITCH_SLOTS_POPUP, 1.0f, 1.0f)
                        )


                        confirmPopup = SwitchSlotConfirmPopup(
                            targetSlot = i,
                            textRenderer = this.textRenderer,
                            screenWidth = this.width,
                            screenHeight = this.height,
                            add = { it: PressableWidget -> this.addDrawableChild(it) },
                            addDrawable = { this.addDrawable(it) },
                            remove = { this.remove(it as Element?) },
                            onConfirm = {
                                PlayerDataSyncNetworkingClient.sendSaveSlotRequest(i, SaveSlotRequestPayload.Action.SWITCH)
                                ClientSaveSlotCache.updateSlot(i, System.currentTimeMillis())

                                MinecraftClient.getInstance().soundManager.play(
                                    PositionedSoundInstance.master(SoundRegistry.SAVE_LOAD_SLOT, 1.0f, 1.0f)
                                )

                                confirmPopup?.dispose()
                                confirmPopup = null
                                refreshSlotButtons()
                            },
                            onCancel = {

                                MinecraftClient.getInstance().soundManager.play(
                                    PositionedSoundInstance.master(SoundRegistry.SAVE_LOAD_SLOT, 1.0f, 1.0f)
                                )

                                confirmPopup?.dispose()
                                confirmPopup = null
                            }
                        ).also { it.init() }
                    }
                },
                onDeleteRequest = {

                    MinecraftClient.getInstance().soundManager.play(
                        PositionedSoundInstance.master(SoundRegistry.DELETE_SLOT_POPUP, 1.0f, 1.2f)
                    )

                    confirmPopup = DeleteConfirmPopup(
                        slot = i,
                        textRenderer = this.textRenderer,
                        screenWidth = this.width,
                        screenHeight = this.height,
                        add = { it: PressableWidget -> this.addDrawableChild(it) },
                        addDrawable = { this.addDrawable(it) },
                        remove = { this.remove(it as Element?) },
                        onConfirm = {
                            PlayerDataSyncNetworkingClient.sendSaveSlotRequest(i, SaveSlotRequestPayload.Action.DELETE)

                            MinecraftClient.getInstance().soundManager.play(
                                PositionedSoundInstance.master(SoundRegistry.CONFIRM_DELETE, 1.0f, 1.0f)
                            )

                            confirmPopup?.dispose()
                            confirmPopup = null
                        },
                        onCancel = {

                            confirmPopup?.dispose()
                            confirmPopup = null
                        }
                    ).also { it.init() }
                },
                add = { this.addDrawableChild(it) }
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
            client.execute { client.setScreen(null) }
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {

        confirmPopup?.let {
            // Darken the screen
            context.fill(0, 0, width, height, 0xAA000000.toInt())

            // Render the background manually here (drawn first)
            if (it is DeleteConfirmPopup) {
                it.getBackground().render(context, mouseX, mouseY, delta)
            }
            if (it is SwitchSlotConfirmPopup) {
                it.getBackground().render(context, mouseX, mouseY, delta)
            }
        }

        super.render(context, mouseX, mouseY, delta)
        renderPlayerName(context)
        confirmPopup?.render(context)
    }

    private fun renderPlayerName(context: DrawContext) {


        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        val username = player.gameProfile.name

        val nameText = "$username§r"

        val scale = 1.00f
        val renderer = client.textRenderer

        val centerX = (width - BASE_WIDTH) / 2
        val centerY = (height - BASE_HEIGHT) / 2
        val textX = centerX + BASE_WIDTH - 50
        val textY = centerY + 9

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




    override fun close() {
        MinecraftClient.getInstance().soundManager.play(
            PositionedSoundInstance.master(SoundRegistry.SAVE_SLOT_CLOSE, 1.0f, 1.3f)
        )
        super.close()
    }


    fun refreshSlotButtons() {

        if (confirmPopup != null) return

        this.clearChildren()
        suppressOpenSound = true
        this.init()
    }
}

