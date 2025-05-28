package com.serotonin.common.registries

import net.minecraft.client.sound.Sound
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier

object SoundRegistry {

    /* Competitive Handbook */
    lateinit var COMPETITIVE_GUI_OPEN: SoundEvent
    lateinit var COMPETITIVE_GUI_CLOSE: SoundEvent
    lateinit var FRIENDLY_BUTTON_ON: SoundEvent
    lateinit var FRIENDLY_BUTTON_OFF: SoundEvent
    lateinit var REWARD_CLAIMED: SoundEvent
    lateinit var REWARD_ALREADY_CLAIMED: SoundEvent
    lateinit var TOURNAMENT_SIGNUP: SoundEvent
    lateinit var TOURNAMENT_SIGNOUT: SoundEvent

    /* Save Slots */
    lateinit var SAVE_SLOT_OPEN: SoundEvent
    lateinit var SAVE_SLOT_CLOSE: SoundEvent
    lateinit var SAVE_LOAD_SLOT: SoundEvent
    lateinit var CONFIRM_DELETE: SoundEvent
    lateinit var DELETE_SLOT_POPUP: SoundEvent
    lateinit var SWITCH_SLOTS_POPUP: SoundEvent
    lateinit var CONFIRM: SoundEvent
    lateinit var CANCEL: SoundEvent

    fun initSounds() {

        /* Competitive Handbook */
        COMPETITIVE_GUI_OPEN = registerSound("gui.competitive.open")
        COMPETITIVE_GUI_CLOSE = registerSound("gui.competitive.close")
        FRIENDLY_BUTTON_ON = registerSound("gui.competitive.friendly_on")
        FRIENDLY_BUTTON_OFF = registerSound("gui.competitive.friendly_off")
        REWARD_CLAIMED = registerSound("gui.competitive.reward_claimed")
        REWARD_ALREADY_CLAIMED = registerSound("gui.competitive.reward_already_claimed")
        TOURNAMENT_SIGNUP = registerSound("gui.competitive.tournament_signup")
        TOURNAMENT_SIGNOUT = registerSound("gui.competitive.tournament_signout")

        /* Save Slots */
        SAVE_SLOT_OPEN = registerSound("gui.saveslots.save_slot_open")
        SAVE_SLOT_CLOSE = registerSound("gui.saveslots.save_slot_close")
        SAVE_LOAD_SLOT = registerSound("gui.saveslots.save_load_slot")
        CONFIRM_DELETE = registerSound("gui.saveslots.confirm_delete")
        DELETE_SLOT_POPUP = registerSound("gui.saveslots.delete_slot_popup")
        SWITCH_SLOTS_POPUP = registerSound("gui.saveslots.switch_slots_popup")
        CONFIRM = registerSound("gui.saveslots.confirm")
        CANCEL = registerSound("gui.saveslots.cancel")
    }

    private fun registerSound(id: String): SoundEvent {
        val identifier = Identifier.of("cobblemonevolved", id)
        val event = SoundEvent.of(identifier)
        Registry.register(Registries.SOUND_EVENT, identifier, event)
        return event
    }
}