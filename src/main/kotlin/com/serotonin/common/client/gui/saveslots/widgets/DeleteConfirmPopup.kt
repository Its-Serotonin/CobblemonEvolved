package com.serotonin.common.client.gui.saveslots.widgets


import com.serotonin.common.client.gui.competitivehandbook.CompetitiveHandbookGUIConstants.BASE_HEIGHT
import com.serotonin.common.client.gui.competitivehandbook.CompetitiveHandbookGUIConstants.BASE_WIDTH
import com.serotonin.common.registries.SoundRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.text.Text

class DeleteConfirmPopup(
    private val slot: Int,
    private val textRenderer: TextRenderer,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val add: (PressableWidget) -> Unit,
    private val addDrawable: (Drawable) -> Unit,
    private val onConfirm: () -> Unit,
    private val onCancel: () -> Unit,
    private val remove: (Element) -> Unit
) : Popup {

    private lateinit var confirmButton: PressableWidget
    private lateinit var cancelButton: PressableWidget
    private lateinit var background: PopupBackgroundWidget

    override fun init() {

        val x = (screenWidth - BASE_WIDTH) / 2
        val y = (screenHeight - BASE_HEIGHT) / 2
        background = PopupBackgroundWidget(x, y)


        val popupX = (screenWidth - BASE_WIDTH) / 2
        val popupY = (screenHeight - BASE_HEIGHT) / 2
        val centerX = popupX + BASE_WIDTH / 2
        val centerY = popupY + BASE_HEIGHT / 2

        val buttonY = centerY + 40
        val spacing = 12

        confirmButton = PopupButtonWidget(
            centerX - 79 - spacing / 2, buttonY, "Yes"
        ) {
            MinecraftClient.getInstance().soundManager.play(
                PositionedSoundInstance.master(SoundRegistry.CONFIRM_DELETE, 1.0f, 1.0f)
            )
            onConfirm()
        }

        cancelButton = PopupButtonWidget(
            centerX + spacing / 2, buttonY, "Cancel"
        ) {
            MinecraftClient.getInstance().soundManager.play(
                PositionedSoundInstance.master(SoundRegistry.CANCEL, 1.0f, 1.0f)
            )
            onCancel()
        }

        confirmButton.visible = true
        cancelButton.visible = true


        add(confirmButton)
        add(cancelButton)

    }

    override fun dispose() {
        remove(background)
        remove(confirmButton)
        remove(cancelButton)

    }

    override fun render(context: DrawContext) {
        context.fill(0, 0, screenWidth, screenHeight, 0x88000000.toInt())
        background.render(context, 0, 0, 0f)
        val text = Text.of("Delete save slot $slot?")
        val x = screenWidth / 2 - textRenderer.getWidth(text) / 2
        context.drawText(textRenderer, text, x, screenHeight / 2 - 10, 0xFFFFFF, false)
    }
    fun getBackground(): PopupBackgroundWidget = background
}