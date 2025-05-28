package com.serotonin.common.entities

import com.serotonin.common.registries.EntityRegister
import net.minecraft.command.argument.EntityArgumentType.entity
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.EntityPose
import net.minecraft.entity.attribute.DefaultAttributeContainer
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.data.DataTracker
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import net.minecraft.util.math.Box
import net.minecraft.world.World

class CustomNameTagRankEntity(
   // entityType: EntityType<out CustomNameTagRankEntity>,
    world: World

) :  Entity(EntityRegister.CUSTOM_NAME_TAG_RANK_ENTITY, world) {
    //ArmorStandEntity(EntityType.ARMOR_STAND, world) {

    override fun initDataTracker(builder: DataTracker.Builder?) {

    }

    override fun readCustomDataFromNbt(nbt: NbtCompound?) {

    }

    override fun writeCustomDataToNbt(nbt: NbtCompound?) {

    }
    init {
        val simpleTag = Text.literal("Test Rank")
        customName = simpleTag
        isCustomNameVisible = true
        isInvisible = true
        isInvulnerable = true
       // hasNoGravity()
       // setNoGravity(true)
        boundingBox = Box(0.1, 0.1, 0.1, 0.1, 0.1, 0.1) // Optional, makes it fully non-interactable
        addCommandTag("RANKTAG")
        addCommandTag("#waila:blacklist")
        //health = 20.0f

    }





         companion object {

            // private val CUSTOM_NAME: TrackedData<Text?> =
             //    DataTracker.registerData(CustomNameTagRankEntity::class.java, TrackedDataHandlerRegistry.TEXT_COMPONENT)

            // private val CUSTOM_NAME_VISIBLE: TrackedData<Boolean> =
            //     DataTracker.registerData(CustomNameTagRankEntity::class.java, TrackedDataHandlerRegistry.BOOLEAN)

            // private val INVISIBLE: TrackedData<Boolean> =
            //     DataTracker.registerData(CustomNameTagRankEntity::class.java, TrackedDataHandlerRegistry.BOOLEAN)

        fun createCustomAttributes(): DefaultAttributeContainer.Builder {
            return DefaultAttributeContainer.builder()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0) // Set max health
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0) // Immobile (optional)
                .add(EntityAttributes.GENERIC_SCALE, 1.0)
        }
    }

   /* override fun tick() {
        super.tick()
        if (this.world == null) {
            println("World is null in CustomNameTagRankEntity tick!")
            return
        }
        // Optional: Update position if needed
    }*/
    override fun getDimensions(pose: EntityPose): EntityDimensions {
        return EntityDimensions.fixed(0.1f, 0.1f) // Tiny size
    }

    override fun shouldSave(): Boolean {
        return true
    }


    fun renderNameTag(text: Text): Boolean {
        if (this.isCustomNameVisible) {
            this.customName = text
            return true
        }
        return false
    }
   // override fun calculateDimensions() {
    //    super.calculateDimensions()
  //  }
}






class CustomRankedPlayerNameEntity(
    world: World
) :   Entity(EntityRegister.CUSTOM_RANKED_PLAYER_NAME_TAG_ENTITY, world) {
// ArmorStandEntity(EntityType.ARMOR_STAND, world) {

    override fun initDataTracker(builder: DataTracker.Builder?) {
        //super.initDataTracker(builder)
    }

    override fun readCustomDataFromNbt(nbt: NbtCompound?) {

    }

    override fun writeCustomDataToNbt(nbt: NbtCompound?) {

    }
    init {
        val simpleTag = Text.literal("Test Name")
        customName = simpleTag
        isCustomNameVisible = true
        isInvisible = true
        isInvulnerable = true
        // hasNoGravity()
        // setNoGravity(true)
        boundingBox = Box(0.1, 0.1, 0.1, 0.1, 0.1, 0.1) // Optional, makes it fully non-interactable
        addCommandTag("RANKEDPLAYERNAMETAG")
        addCommandTag("#waila:blacklist")
        //health = 20.0f
    }
    companion object {
        fun createCustomAttributes(): DefaultAttributeContainer.Builder {
            return DefaultAttributeContainer.builder()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0) // Set max health
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0) // Immobile (optional)
                .add(EntityAttributes.GENERIC_SCALE, 1.0)
        }
    }

    /* override fun tick() {
         super.tick()
         if (this.world == null) {
             println("World is null in CustomNameTagRankEntity tick!")
             return
         }
         // Optional: Update position if needed
     }*/
    override fun getDimensions(pose: EntityPose): EntityDimensions {
        return EntityDimensions.fixed(0.1f, 0.1f) // Tiny size
    }

    override fun shouldSave(): Boolean {
        return true
    }

    fun renderRankedPlayerName(text: Text): Boolean {
        if (this.isCustomNameVisible) {
            this.customName = text
            return true
        }
        return false
    }
    // override fun calculateDimensions() {
    //    super.calculateDimensions()
    //  }
}
