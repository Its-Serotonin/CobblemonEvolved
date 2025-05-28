package com.serotonin.common.elosystem

import com.serotonin.common.networking.ServerContext
import com.serotonin.common.networking.triggerLeaderboardDisplayOnServer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.BlockPos
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption


object LeaderboardManager {
    @Serializable
    data class LeaderboardData(
        val leaderboardPositions: List<LeaderboardPosition> = emptyList()
    )

    @Serializable
    data class LeaderboardPosition(
        val x: Int,
        val y: Int,
        val z: Int,
        val dimensionId: String = "minecraft:overworld"
    )

    private var leaderboardData = LeaderboardData()
    private val json = Json { prettyPrint = true }

    private fun getConfigPath(server: MinecraftServer): Path {
        val configDir = server.runDirectory.resolve("config")
        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir)
        }
        return configDir.resolve("leaderboards.json")
    }

    fun initialize() {
        ServerLifecycleEvents.SERVER_STARTING.register { server ->
            loadLeaderboardData(server)
        }

        ServerLifecycleEvents.SERVER_STARTED.register { server ->

            restoreLeaderboards(server)
        }

        ServerLifecycleEvents.SERVER_STOPPING.register { server ->
            saveLeaderboardData(server)
        }
    }

    private fun loadLeaderboardData(server: MinecraftServer) {
        val path = getConfigPath(server)
        if (Files.exists(path)) {
            try {
                val content = Files.readString(path)
                leaderboardData = json.decodeFromString(content)
                println("Loaded leaderboard data: ${leaderboardData.leaderboardPositions.size} leaderboards")
            } catch (e: Exception) {
                println("Error loading leaderboard data: ${e.message}")
                e.printStackTrace()
                leaderboardData = LeaderboardData()
            }
        } else {
            println("No leaderboard data file found, using defaults")
            leaderboardData = LeaderboardData()
        }
    }

    private fun saveLeaderboardData(server: MinecraftServer) {
        val path = getConfigPath(server)
        try {
            val content = json.encodeToString(leaderboardData)
            Files.writeString(
                path,
                content,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            )
            println("Saved leaderboard data: ${leaderboardData.leaderboardPositions.size} leaderboards")
        } catch (e: Exception) {
            println("Error saving leaderboard data: ${e.message}")
            e.printStackTrace()
        }
    }

    fun addLeaderboard(pos: BlockPos, dimensionId: String = "minecraft:overworld") {
        val newPosition = LeaderboardPosition(pos.x, pos.y, pos.z, dimensionId)


        val exists = leaderboardData.leaderboardPositions.any {
            it.x == pos.x && it.y == pos.y && it.z == pos.z && it.dimensionId == dimensionId
        }

        if (!exists) {
            leaderboardData = leaderboardData.copy(
                leaderboardPositions = leaderboardData.leaderboardPositions + newPosition
            )
            println("Added leaderboard at $pos")

            ServerContext.server?.let { saveLeaderboardData(it) }
        }
    }

    fun removeLeaderboard(pos: BlockPos?, dimensionId: String = "minecraft:overworld") {
        if (pos == null) {

            leaderboardData = leaderboardData.copy(leaderboardPositions = emptyList())
            println("Removed all leaderboards")
        } else {

            leaderboardData = leaderboardData.copy(
                leaderboardPositions = leaderboardData.leaderboardPositions.filterNot {
                    it.x == pos.x && it.y == pos.y && it.z == pos.z && it.dimensionId == dimensionId
                }
            )
            println("Removed leaderboard at $pos")
        }

        ServerContext.server?.let { saveLeaderboardData(it) }
    }

    private fun restoreLeaderboards(server: MinecraftServer) {
        println("Restoring ${leaderboardData.leaderboardPositions.size} leaderboards...")

        val world = server.getWorld(net.minecraft.world.World.OVERWORLD) ?: return

        leaderboardData.leaderboardPositions.forEach { pos ->
            val blockPos = BlockPos(pos.x, pos.y, pos.z)
            val renderPos = blockPos.up(2)

            if (pos.dimensionId == "minecraft:overworld") {
                try {
                    val aabb = net.minecraft.util.math.Box(
                        renderPos.x - 1.0, renderPos.y - 5.0, renderPos.z - 1.0,
                        renderPos.x + 1.0, renderPos.y + 1.0, renderPos.z + 1.0
                    )

                    val oldStands = world.getEntitiesByClass(
                        com.serotonin.common.entities.LeaderboardArmorStandEntity::class.java,
                        aabb
                    ) { true }
                    oldStands.forEach {
                         it.remove(net.minecraft.entity.Entity.RemovalReason.DISCARDED)
                        println("Removed old leaderboard ArmorStand at ${it.blockPos}")
                    }


                    triggerLeaderboardDisplayOnServer(blockPos)
                    println("Restored leaderboard at $blockPos")
                } catch (e: Exception) {
                    println("Failed to restore leaderboard at $blockPos: ${e.message}")
                    removeLeaderboard(blockPos)
                    println("Auto-removed invalid leaderboard entry at $blockPos")
                    e.printStackTrace()
                }
            }
        }
    }
}
