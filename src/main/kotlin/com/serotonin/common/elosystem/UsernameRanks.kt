package com.serotonin.common.elosystem

import com.serotonin.common.api.events.EloManager.playerElos
import com.serotonin.common.api.events.EloManager.requestElo
import com.serotonin.common.entities.CustomNameTagRankEntity
import com.serotonin.common.entities.CustomRankedPlayerNameEntity
import com.serotonin.common.networking.RankResponsePayload
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.entity.Entity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

val playerRankTags = mutableMapOf<UUID, CustomNameTagRankEntity>()

val mapOfRankedPlayerNameTags = mutableMapOf<UUID, CustomRankedPlayerNameEntity>()

val pendingNameTagUpdates: MutableMap<UUID, ServerPlayerEntity> = ConcurrentHashMap()

val lastTagHidingTime = mutableMapOf<UUID, Int>()

val cachedElo = mutableMapOf<UUID, Int>()


fun updatePlayerNametag(player: ServerPlayerEntity, elo: Int, rankName: String) {
    println("Starting updatePlayerNametag for ${player.name.string} with elo=$elo, rank=$rankName")


    player.server.execute {


        val rankText = Text.literal(rankName).append(Text.literal(": §c§l$elo"))
        val playerNameTag = player.name


        val tag = tagPlayersRanks(player, rankText)
        playerRankTags[player.uuid] = tag
        println("Created rank tag for ${player.name.string}: ID=${tag.id}, hasVehicle=${tag.hasVehicle()}")


        val nameTag = tagPlayersName(player, playerNameTag)
        mapOfRankedPlayerNameTags[player.uuid] = nameTag
        println("Created bame tag for ${player.name.string}: ID=${nameTag.id}, hasVehicle=${tag.hasVehicle()}")

        syncTagAcrossServers(player, elo, rankName)
    }
}



fun syncTagAcrossServers(player: ServerPlayerEntity, elo: Int, rankName: String) {

    val response = RankResponsePayload(
        player.uuid.toString(),
        player.name.string,
        elo,
        silent = true
    )
    ServerPlayNetworking.send(player, response)

    println("Sent visible rank update to ${player.name.string}: $rankName ($elo)")
}



fun tagPlayersName(player: ServerPlayerEntity, playerNameTag: Text): CustomRankedPlayerNameEntity {
    val world = player.world as? ServerWorld
        ?: return CustomRankedPlayerNameEntity(player.world)

    println("CREATING NAME TAG for ${player.name.string}")


    mapOfRankedPlayerNameTags[player.uuid]?.remove(Entity.RemovalReason.DISCARDED)

    val namePlateTag = CustomRankedPlayerNameEntity(world).apply {

        customName = playerNameTag
        isCustomNameVisible = true
        isInvisible = true
        isInvulnerable = true
        // isMarker
        //  hasNoGravity()
        setNoGravity(true)
        addCommandTag("RANKEDPLAYERNAMETAG")
        addCommandTag("HIDEFROMPLAYER:${player.uuid}")
        //setPosition(pos.x, pos.y, pos.z)
        //setMarker(true)
    }

    world.spawnEntity(namePlateTag)
    println("debug: spawned player names")

    if (!namePlateTag.hasVehicle()) {
        namePlateTag.startRiding(player, true)
    }


    println("debug: spawned player names")

   // val serverPlayer = player
  //  serverPlayer.networkHandler.sendPacket(EntitiesDestroyS2CPacket(*intArrayOf(namePlateTag.id)))


    return namePlateTag
}


 fun tagPlayersRanks(player: ServerPlayerEntity, rankTag: Text): CustomNameTagRankEntity {
    val world = player.world
    val pos = player.pos.add(0.0, -0.4, 0.0)
     println("CREATING RANK TAG for ${player.name.string}")

     playerRankTags[player.uuid]?.remove(Entity.RemovalReason.DISCARDED)
        val armourStandTag = CustomNameTagRankEntity(world).apply {

            customName = rankTag
            isCustomNameVisible = true
            isInvisible = true
            isInvulnerable = true
           // isMarker
          //  hasNoGravity()
            setNoGravity(true)
            addCommandTag("RANKTAG")
            addCommandTag("HIDEFROMPLAYER:${player.uuid}")
            // setPosition(pos.x, pos.y, pos.z)
             //setMarker(true)

        }

     world.spawnEntity(armourStandTag)
     println("debug: spawned the tag")



     if (!armourStandTag.hasVehicle()) {
         armourStandTag.startRiding(player, true)
     }

    // val serverPlayer = player
    // serverPlayer.networkHandler.sendPacket(EntitiesDestroyS2CPacket(*intArrayOf(armourStandTag.id)))

              return armourStandTag
        }


val playersToHideTagsFrom = mutableSetOf<UUID>()


fun cleanupPlayerEntities(uuid: UUID) {
    val rankTag = playerRankTags.remove(uuid)
    if (rankTag != null && rankTag.isAlive) {
        rankTag.remove(Entity.RemovalReason.DISCARDED)
    }

    val nameTag = mapOfRankedPlayerNameTags.remove(uuid)
    if (nameTag != null && nameTag.isAlive) {
        nameTag.remove(Entity.RemovalReason.DISCARDED)
    }
}


fun hidePlayerOwnTags(player: ServerPlayerEntity) {
   /* val tagsToHide = mutableListOf<Int>()
    val uuid = player.uuid

    // Get the rank tag
    playerRankTags[uuid]?.let { tagsToHide.add(it.id) }

    // Get the name tag
    mapOfRankedPlayerNameTags[uuid]?.let { tagsToHide.add(it.id) }

    if (tagsToHide.isNotEmpty()) {
        player.networkHandler.sendPacket(
            EntitiesDestroyS2CPacket(*tagsToHide.toIntArray())
        )
    }*/
    lastTagHidingTime[player.uuid] = player.server.ticks
}




object PlayerRankTagsRegister {
    fun registerRankTags() {

        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            val player = handler.player
            val uuid = player.uuid




            player.server.execute {

                Thread.sleep(500) // hopefully stop "unable to connect to world" message


                if (!playerRankTags.containsKey(uuid)) {
                    requestElo(uuid, player, silent = true) { /* no op */ }
                    pendingNameTagUpdates[uuid] = player
                    //playersToHideTagsFrom += uuid
                }

                player.server.execute {

                    Thread.sleep(500)

                    hidePlayerOwnTags(player)
                    lastTagHidingTime[uuid] = player.server.ticks
                }
            }

        }

        ServerTickEvents.START_SERVER_TICK.register { server ->
            if (server.ticks % 5 == 0) {
                for (player in server.playerManager.playerList) {
                    val uuid = player.uuid
                    if (!player.isAlive) continue


                    val currentElo = playerElos[uuid] ?: 1000
                    val cachedPlayerElo = cachedElo[uuid]


                    if (cachedPlayerElo == null || cachedPlayerElo != currentElo) {

                        cachedElo[uuid] = currentElo
                        val rankName = getTierName(currentElo)


                        val tag = playerRankTags[uuid]
                        if (tag != null && tag.isAlive) {

                            val newText = Text.literal(rankName)
                                .append(Text.literal(": §c§l$currentElo"))
                            tag.customName = newText
                            println("Updated rank tag for ${player.name.string} to $rankName ($currentElo)")
                        } else {

                            updatePlayerNametag(player, currentElo, rankName)
                        }


                        hidePlayerOwnTags(player)
                        lastTagHidingTime[uuid] = server.ticks
                        continue
                    }



                    val tag = playerRankTags[uuid] ?: continue
                    val playerNameTag = mapOfRankedPlayerNameTags[uuid] ?: continue


                    val tagsDetached = !tag.hasVehicle() || !playerNameTag.hasVehicle()

                    if (tagsDetached) {

                        val elo = playerElos[uuid] ?: 1000
                        val rankName = getTierName(elo)


                        cleanupPlayerEntities(uuid)


                        updatePlayerNametag(player, elo, rankName)



                        lastTagHidingTime[uuid] = server.ticks


                        continue
                    }


                    val newText = Text.literal(getTierName(currentElo))
                        .append(Text.literal(": §c§l$currentElo"))


                    val updatedRankTag = tag.customName != newText
                    if (updatedRankTag) {
                        tag.customName = newText
                    }



                    val updatedNameTag = playerNameTag.customName != player.name
                    if (updatedNameTag) {
                        playerNameTag.customName = player.name
                    }



                    val currentTime = server.ticks
                    val lastHideTime = lastTagHidingTime[uuid] ?: 0


                    if (updatedRankTag || updatedNameTag || currentTime - lastHideTime > 40) {
                        hidePlayerOwnTags(player)
                        lastTagHidingTime[uuid] = currentTime
                    }
                }
            }
        }

        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            val uuid = handler.player.uuid
            cleanupPlayerEntities(uuid)
            lastTagHidingTime.remove(uuid)
            // playerRankTags.remove(uuid)?.kill()
           // mapOfRankedPlayerNameTags.remove(uuid)?.kill()
        }
            }
        }
