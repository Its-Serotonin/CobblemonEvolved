package com.serotonin.common.entities


import net.minecraft.entity.EntityType
import net.minecraft.entity.attribute.DefaultAttributeContainer
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.world.World

class LeaderboardArmorStandEntity(type: EntityType<out ArmorStandEntity>, world: World) : ArmorStandEntity(type, world) {
    var isLeaderboard = true


    init {
        setNoGravity(true)
    }


    override fun writeCustomDataToNbt(nbt: NbtCompound) {
        super.writeCustomDataToNbt(nbt)
        nbt.putBoolean("RANKLEADERBOARD", isLeaderboard)
    }

    override fun readCustomDataFromNbt(nbt: NbtCompound) {
        super.readCustomDataFromNbt(nbt)
        isLeaderboard = nbt.getBoolean("RANKLEADERBOARD")
    }

    override fun shouldSave(): Boolean {
        return false
    }


    companion object {
        fun createAttributes(): DefaultAttributeContainer.Builder {
            return createLivingAttributes()
        }
    }
}
