package com.serotonin.common.client.gui.saveslots.widgets

import com.serotonin.common.saveslots.ClientSaveSlotMetadata
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import net.minecraft.text.Text
import java.time.Instant
import java.time.ZoneId

class SaveSlotButtonSet(
    val slot: Int,
    private val x: Int,
    private val y: Int,
    private var slotMetadata: ClientSaveSlotMetadata?,
    private val isCurrentSlot: Boolean,
    private val onActivate: () -> Unit,
    private val onDeleteRequest: () -> Unit,
    private val add: (Element) -> Unit,
    private val isPopupActive: () -> Boolean
) : Drawable {

    private lateinit var slotIcon: SlotIconWidget

    private val baseX = x
    private val baseY = y

    fun init() {
        val lastSavedText = slotMetadata?.lastSaved?.let {
            val date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime()
            "${date.hour.toString().padStart(2, '0')}:${date.minute.toString().padStart(2, '0')}"
        } ?: "Unused"

        val label = if (isCurrentSlot) "✔ Slot $slot ($lastSavedText)" else "Slot $slot ($lastSavedText)"

        val activateBtn = SilentButtonWidget.create(
            x, y, 130, 20, Text.of(label)
        ) {
            if (!isPopupActive()) {
                onActivate()
            }
        }



        val deleteX = baseX + 184 //253 originally
        val deleteY = baseY + 33 // 7 original

        val deleteButton = DeleteButtonWidget(deleteX, deleteY, 10, 10) {
            if (!isPopupActive()) {
                onDeleteRequest()
            }
        }
        deleteButton.active = true
        deleteButton.visible = true
        deleteButton.setPosition(deleteX, deleteY)

        slotIcon = SlotIconWidget(x, y, slot, slotMetadata, {
            if (!isPopupActive()) onActivate()
        }, isClickExcluded = { mx, my ->
            mx >= deleteButton.x && mx < deleteButton.x + deleteButton.width &&
                    my >= deleteButton.y && my < deleteButton.y + deleteButton.height
        })


        val silentButton = SilentButtonWidget(
            x, y, 281, 47,
            label = "",
            onPressAction = { if (!isPopupActive()) onActivate() },
            isClickExcluded = { mx, my ->

                mx >= deleteButton.x.toDouble() && mx < deleteButton.x + deleteButton.width &&
                        my >= deleteButton.y.toDouble() && my < deleteButton.y + deleteButton.height
            }
        )
        add(slotIcon)
        add(deleteButton)
        //add(silentButton)

    }


    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
      //  if (::slotIcon.isInitialized) {
       //     slotIcon.render(context, mouseX, mouseY, delta)
       // }
    }
    fun refreshScreenshot(newMetadata: ClientSaveSlotMetadata?) {
        this.slotMetadata = newMetadata
        slotIcon.refreshScreenshot(newMetadata)
    }
}