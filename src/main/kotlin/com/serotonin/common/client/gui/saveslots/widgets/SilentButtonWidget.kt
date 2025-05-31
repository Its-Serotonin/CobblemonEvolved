package com.serotonin.common.client.gui.saveslots.widgets

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.client.sound.SoundManager
import net.minecraft.text.Text

class SilentButtonWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val label: String,
    private val onPressAction: () -> Unit,
    private val isClickExcluded: (mouseX: Double, mouseY: Double) -> Boolean = { _, _ -> false }
) : PressableWidget(x, y, width, height, Text.of(label)) {

    override fun onPress() {
            onPressAction()
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (isClickExcluded(mouseX, mouseY)) {
            println("SilentButton: click excluded at $mouseX, $mouseY")
            return false // Don't consume click; allow underlying widgets to receive it
        }
        println("SilentButton: accepted click at $mouseX, $mouseY")
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun playDownSound(soundManager: SoundManager) {

    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        }


    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        builder.put(NarrationPart.TITLE, Text.of(label))
    }

    companion object {
        fun create(
            x: Int, y: Int, width: Int, height: Int,
            label: Text,
            onPress: () -> Unit
        ): SilentButtonWidget {
            return SilentButtonWidget(x, y, width, height, label.string, onPress)
        }

        fun of(
            x: Int, y: Int, width: Int, height: Int,
            label: Text,
            onPress: () -> Unit,
            excludeTarget: PressableWidget
        ): SilentButtonWidget {
            return SilentButtonWidget(
                x, y, width, height,
                label.string,
                onPress,
                isClickExcluded = { mx, my ->
                    mx >= excludeTarget.x && mx <= excludeTarget.x + excludeTarget.width &&
                            my >= excludeTarget.y && my <= excludeTarget.y + excludeTarget.height
                }
            )
        }
    }
}