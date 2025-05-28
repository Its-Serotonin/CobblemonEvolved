package com.serotonin.common.saveslots




import java.util.*

interface SaveSlotDAO {
    fun saveSlot(data: PlayerSaveSlot)
    fun loadSlot(uuid: UUID, slot: Int): PlayerSaveSlot?
    fun deleteSlot(uuid: UUID, slot: Int)
    fun getAllSlots(uuid: UUID): List<PlayerSaveSlot>
}
