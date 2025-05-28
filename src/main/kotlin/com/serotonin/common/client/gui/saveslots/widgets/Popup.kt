package com.serotonin.common.client.gui.saveslots.widgets

import net.minecraft.client.gui.DrawContext

interface Popup {
    fun init()
    fun render(context: DrawContext)
    fun dispose()
}