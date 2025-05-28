package com.serotonin.common.saveslots

import java.util.UUID

fun PlayerSaveSlot.isDuplicateOf(other: PlayerSaveSlot): Boolean {
    return inventoryData.contentEquals(other.inventoryData) &&
            pokemonData.contentEquals(other.pokemonData) &&
            pcData.contentEquals(other.pcData)
}

fun hasDuplicateSlot(playerUUID: UUID, newSlotData: PlayerSaveSlot, dao: SaveSlotDAO): Boolean {
    return dao.getAllSlots(playerUUID).any { existing ->
        existing.slot != newSlotData.slot && newSlotData.isDuplicateOf(existing)
    }
}
