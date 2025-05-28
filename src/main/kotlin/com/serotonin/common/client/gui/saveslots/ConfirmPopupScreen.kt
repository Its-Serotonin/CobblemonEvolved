package com.serotonin.common.client.gui.saveslots

import com.serotonin.common.client.gui.competitivehandbook.CompetitiveHandbookGUIConstants.BASE_HEIGHT
import com.serotonin.common.client.gui.competitivehandbook.CompetitiveHandbookGUIConstants.BASE_WIDTH
import com.serotonin.common.client.gui.saveslots.widgets.PopupBackgroundWidget
import com.serotonin.common.client.gui.saveslots.widgets.PopupButtonWidget
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.text.MutableText
import net.minecraft.text.Text

class ConfirmationPopupScreen(
    private val message: MutableText,
    private val onConfirm: () -> Unit,
    private val onCancel: () -> Unit,
    private val parent: Screen
) : Screen(Text.of("Confirmation")) {

    private lateinit var confirmButton: PressableWidget
    private lateinit var cancelButton: PressableWidget
    private lateinit var background: PopupBackgroundWidget

    override fun init() {
        val x = (width - BASE_WIDTH) / 2
        val y = (height - BASE_HEIGHT) / 2
        background = PopupBackgroundWidget(x, y)

        val centerX = x + BASE_WIDTH / 2
        val centerY = y + BASE_HEIGHT / 2
        val buttonY = centerY + 10
        val spacing = 24

        confirmButton = PopupButtonWidget(centerX - 79 - spacing / 2, buttonY, "Yes") {
            if (parent is SaveSlotScreen) {
                parent.suppressOpenSound = true
            }
            MinecraftClient.getInstance().setScreen(parent)
            onConfirm()
        }

        cancelButton = PopupButtonWidget(centerX + spacing / 2, buttonY, "No") {
            if (parent is SaveSlotScreen) {
                parent.suppressOpenSound = true
            }
            MinecraftClient.getInstance().setScreen(parent)
            onCancel()
        }

        addDrawable(background)
        addDrawableChild(confirmButton)
        addDrawableChild(cancelButton)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        parent.render(context, mouseX, mouseY, delta)
        context.fill(0, 0, width, height, 0x88000000.toInt())
        background.render(context, mouseX, mouseY, delta)

        super.render(context, mouseX, mouseY, delta)

        val textX = width / 2 - textRenderer.getWidth(message) / 2
        val textY = (height - BASE_HEIGHT) / 2 + 70
        context.drawTextWithShadow(textRenderer, message, textX, textY, 0xFFFFFF)


    }
}