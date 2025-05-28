package com.serotonin.common.elosystem

import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos

fun spawnFloatingText(
    world: ServerWorld,
    pos: BlockPos,
    lines: List<String>,
    tag: String = "RANKLEADERBOARD"
): List<ArmorStandEntity> {
    val entities = mutableListOf<ArmorStandEntity>()
    val baseX = pos.x.toDouble()
    val baseY = pos.y.toDouble()
    val baseZ = pos.z.toDouble()

    lines.forEachIndexed { index, line ->
        val yOffset = baseY - index * 0.3
        val armorStand = ArmorStandEntity(world, baseX, yOffset, baseZ).apply {
            customName = Text.of(line)
            isCustomNameVisible = true
            isInvisible = true
            isInvulnerable = true
            setNoGravity(true)
            addCommandTag(tag)

        }
        world.spawnEntity(armorStand)
        entities.add(armorStand)
    }

    return entities
}
