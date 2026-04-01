package com.serotonin.common.client.gui

import io.wispforest.accessories.client.gui.ButtonEvents
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.text.Text

abstract class AccessoriesWidget(x: Int, y: Int, width: Int, height: Int, message: Text)
    : PressableWidget(x, y, width, height, message) {

    private val renderingEvent = EventFactory.createArrayBacked(ButtonEvents.AdjustRendering::class.java) { cbs ->
        ButtonEvents.AdjustRendering { btn, ctx, tex, x, y, w, h -> cbs.all { it.render(btn, ctx, tex, x, y, w, h) } }
    }
    override fun getRenderingEvent() = renderingEvent
}