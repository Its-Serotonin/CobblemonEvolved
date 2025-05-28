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
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket
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

// Remove and delete all existing rank tags
    /*fun clearAllRankTags() {
    // Iterate over all player UUIDs in the playerRankTags map
    for ((uuid, tag) in playerRankTags) {
        // Make sure the tag exists and is still in the world
        if (tag.world != null) {
            tag.kill() // Removes the entity from the world and kills it
        }
    }

    // Clear the map after deleting the tags
    playerRankTags.clear()
}*/


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
    /*
    player.world.players.forEach { otherPlayer ->
        if (otherPlayer is ServerPlayerEntity && otherPlayer.uuid != player.uuid) {
            // Force entity data sync to this player
            val packet = playerRankTags[player.uuid]?.createSpawnPacket()
            if (packet != null) {
                otherPlayer.networkHandler.sendPacket(packet)
            }

            val namePacket = mapOfRankedPlayerNameTags[player.uuid]?.createSpawnPacket()
            if (namePacket != null) {
                otherPlayer.networkHandler.sendPacket(namePacket)
            }
        }
    }*/
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





//private fun CustomNameTagRankEntity.setMarker(marker: Boolean) {}
//private fun SlimeEntity.setMarker(marker: Boolean) {}


                /*fun stopShowingShit(player: ServerPlayerEntity, playerNameTag: Text, rankTag: Text ) {
                        val serverPlayer = player
                        val world = player.world

                    val namePlateTag = CustomRankedPlayerNameEntity(world).apply {
                        customName = playerNameTag
                        isCustomNameVisible = true
                        isInvisible = true
                        isInvulnerable = true
                        setNoGravity(true)
                        addCommandTag("RANKEDPLAYERNAMETAG")
                    }
                    val armourStandTag = CustomNameTagRankEntity(world).apply {
                        customName = rankTag
                        isCustomNameVisible = true
                        isInvisible = true
                        isInvulnerable = true
                        setNoGravity(true)
                        addCommandTag("RANKTAG")
                    }
                        serverPlayer.networkHandler.sendPacket(EntitiesDestroyS2CPacket(*intArrayOf(armourStandTag.id)))
                        serverPlayer.networkHandler.sendPacket(EntitiesDestroyS2CPacket(*intArrayOf(namePlateTag.id)))
                    }*/






               // iterator.remove() // Done with this player

                //player.networkHandler.sendPacket(
                  //  EntitiesDestroyS2CPacket(*intArrayOf(tagAllPlayers.id, tag.id)))

                //  if (!tag.hasVehicle()) {

                //   tag.startRiding(player, true)
//}

                // val newX = player.x
                // val newY = player.y - 0.4
                // val newZ = player.z

                // tag.teleport(newX, newY, newZ, false)


                /*val isFirstPerson = player.pitch in -90.0..-55.0
                tag.isCustomNameVisible = !isFirstPerson
                playersNamee.isCustomNameVisible = !isFirstPerson*/


               // if (player != null && player.world != null) {
                //    val newPos = player.pos.add(0.0, -0.4, 0.0)
                   // if (tag.squaredDistanceTo(playerPos.x, playerPos.y, playerPos.z) > 0.01) {
                   //     tag.setPosition(newPos.x, newPos.y, newPos.z)
                  //      tag.yaw = player.yaw
                  //      tag.bodyYaw = player.yaw
                  //      tag.headYaw = player.yaw
                 //   } else {
                 //       println("Player or player world is null!")

                 //   }


                    //  if (tag.vehicle == player) {
                    // val pos = player.pos
                    //tag.teleport(
                    //   player.world as ServerWorld,
                    //   newX,
                    //  newY,
                    //   newZ,
                    //  mutableSetOf(PositionFlag.X, PositionFlag.Y, PositionFlag.Z),
                    //  player.yaw.toFloat(),
                    //   0f
                    // )
                    //  val newPos = playerPos.add(0.0, -0.4, 0.0)
                    // tag.setPosition(newPos.x, newPos.y, newPos.z)
               // }
           // }
     //   }







/*private fun setNameTagVisibility(player: ServerPlayerEntity, tag: ArmorStandEntity, visible: Boolean) {
    val packet = ClientboundSetEntityCustomTagPacket(tag.id, mutableListOf("RANKTAG"))
    if (visible) {
        // Send packet to make the tag visible to the player
        player.networkHandler.sendPacket(packet)
    } else {
        // Send packet to make the tag invisible to the player
        player.networkHandler.sendPacket(packet)
    }
}*/




   /* var tagTickCounter = 0
    fun rankTagUpdater() {
        ServerTickEvents.START_SERVER_TICK.register { _ ->
            tagTickCounter++
            // server.scheduler.scheduleRepeat({
            if (tagTickCounter >= 500) {
                updateRankTags()
                println("Debug: rank tags updating")
                tagTickCounter = 0
            }
        }
    }
*/

  //  private fun ArmorStandEntity.remove() {


  // var playerCompetitiveRankTagsPosition: BlockPos? = null
/*
    fun updateRankTags() {


        playerRankTags.forEach { it.remove(Entity.RemovalReason.DISCARDED) }
        playerRankTags.clear()

        playerRankTags.forEach { player ->
            // nothing here yet

    }
}
private fun Map.Entry<UUID, ArmorStandEntity>.remove(discarded: Entity.RemovalReason) {}
}
*/

//private val showRankOnUsername.numberformat: NumberFormat? = NumberFormatType<StyledNumberFormat>

//private val fancy = StyledNumberFormat.TYPE
/* //START OF SCOREBOARD
val numberFormat: NumberFormat = StyledNumberFormat(
    Style.EMPTY.withColor(Formatting.RED).withBold(true)
)
object ShowRankOnUsername{
    //private val scoreboardDisplayName = ScoreboardDisplaySlot.BELOW_NAME
    val userRank = Scoreboard()
    fun makeUserScoreboard(player: ServerPlayerEntity, elo: Int){
        val objective = userRank.addObjective(
            "userRank",
            ScoreboardCriterion.DUMMY,
            text("${getTierName(elo)}: §c§l$elo"),
            ScoreboardCriterion.RenderType.INTEGER,
            true,
            numberFormat
            //fancy as NumberFormat?,
        )
        userRank.setObjectiveSlot(ScoreboardDisplaySlot.BELOW_NAME, objective)

        val score = userRank.getOrCreateScore(player, objective)
        score.score = elo
        val scoreboard = player.server.scoreboard
       // scoreboard = userRank
    } //END OF SCOREBOARD SO FAR */

   /* private fun scoreboardDisplayName(text: MutableText): Text {
        ScoreboardDisplaySlot.BELOW_NAME
        return scoreboardDisplayName(text)
    }*/

    //could make fun thats just "getplayertier" and "getplayerrank" then put it into the shit

  /*  ScoreboardObjective(
    userRank,
    "UserRank",
    ScoreboardCriterion.DUMMY,
    scoreboardDisplayName(text("Your current rank is ${getTierName(elo)}: §c§l$elo")),
    ScoreboardCriterion.RenderType.INTEGER,
    true,
    fancy as NumberFormat?
    )*/




    //text("Your current rank is ${getTierName(elo)}: §c§l$elo")


//   val player = context.source.player
// val entry = eloMap[player!!.uuid]
//  if (entry != null) {
//    val (name, elo) = entry


//val player = context.source.player
//val elo = eloMap.getOrDefault(player!!.uuid, 1000)
//context.source.sendFeedback({ text("§lCurrent Ladder Tiers: ${getTierList()}") }, false)