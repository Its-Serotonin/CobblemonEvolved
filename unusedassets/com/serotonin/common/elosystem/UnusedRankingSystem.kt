

/* val leaderboard = topPlayers.joinToString("\n") { (uuid, data) ->
                           "${data.first}: §c§l${data.second}§r"  // Display name and Elo rating in red
                       }*/

/*fun displayLeaderboardAsFloatingText(x: Int, y: Int, z: Int) {
    // Sort players by Elo and get the top 10
    val topPlayers = eloMap.entries
        .sortedByDescending { it.value.second }
        .take(10)

    // Format the leaderboard
    val leaderboard = topPlayers.withIndex().joinToString("\n") { (index, entry) ->
        val (uuid, data) = entry

        val TopPlacements = when (index){
            0 -> "§6§l"
            1 -> "§7§l"
            2 -> "§6§l"
            else -> "§f"
        }


        "$TopPlacements#${index +1}. ${data.first}: §c§l${data.second}§r"
    }

    // Choose the position for the floating text
    val position = BlockPos(x, y, z)  // Position from the command input

    // Get the world (assuming you want it in the Overworld)
    val world: ServerWorld? = server.getWorld(World.OVERWORLD)  // Replace with your desired world if needed

    if (world != null) {
        val armourStand = ArmorStandEntity(world, position.x.toDouble(), position.y.toDouble(), position.z.toDouble())
        armourStand.customName = Text.of(leaderboard)
        armourStand.isCustomNameVisible = true
        armourStand.isInvisible = true
        armourStand.isInvulnerable = true
        armourStand.addCommandTag("RANKLEADERBOARD")


        armourStand.setMarker(true)
        // Add the Armor Stand to the world
        world.spawnEntity(armourStand)

        // Track the armor stand for removal

        leaderboardEntities.add(armourStand)
    }
}*/






/*  CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
      dispatcher.register(
          literal("rank")
              .executes { context ->
                  val player = context.source.player
                  val entry = eloMap[player!!.uuid]
                  if (entry != null) {
                      val (name, elo) = entry
                      context.source.sendFeedback(
                          { text("Your current rank is ${getTierName(elo)}: §c§l$elo") },
                          false
                      )
                  } else {
                      context.source.sendFeedback(
                          { text("§cYou don't have a rank yet!") },
                          false
                      )
                  }
                  1
              }
              .then(
                  argument("target", StringArgumentType.word())
                      .suggests { _, builder ->
                          val onlineNames = server.playerManager.playerList.map { it.name.string }
                          val offlineNames = eloMap.values.map { it.first }
                          (onlineNames + offlineNames).distinct().forEach { builder.suggest(it) }
                          builder.buildFuture()
                      }
                      .executes { context ->
                          val nameArg = StringArgumentType.getString(context, "target")
                          val match = eloMap.entries.find { it.value.first.equals(nameArg, ignoreCase = true) }

                          if (match != null) {
                              val (name, elo) = match.value
                              context.source.sendFeedback(
                                  { text("$name's rank is ${getTierName(elo)}: §c§l$elo") },
                                  false
                              )
                          } else {
                              context.source.sendFeedback(
                                  { text("Player $nameArg not found.") },
                                  false
                              )
                          }
                          1
                      }
              )
      )
  }*/


// /setrank <elo> - Set Elo value for the player (only for ops)







//old leaderboard command that doesnt use velocity
/* CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
     dispatcher.register(
         literal("leaderboard")
             .executes { context ->
                 // Sort players by Elo (descending order) and get the top 10 players
                 val topPlayers = eloMap.entries
                     .sortedByDescending { it.value.second }  // Sort by Elo rating
                     .take(10)  // Get the top 10 players

                 if (topPlayers.isEmpty()) {
                     context.source.sendFeedback(
                         { text("§c§lNo players have a rank yet!§r") },
                         false
                     )
                     return@executes 1
                 }


                 val leaderboard = topPlayers.withIndex().joinToString("\n") { (index, entry) ->
                     val (uuid, data) = entry

                     val TopPlacements = when (index){
                         0 -> "§6§l"
                         1 -> "§7§l"
                         2 -> "§3§l"
                         else -> "§f"
                     }


                     "$TopPlacements#${index +1}. ${data.first}: §c§l${data.second}§r"
                 }

                 context.source.sendFeedback(
                     { text("Top 10 Players:\n$leaderboard") },
                     false
                 )
                 1
             }
     )
 }*/




/*
//old logic without using velocity for the leaderboard
val topPlayers = eloMap.entries
    .sortedByDescending { it.value.second }
    .take(10)

topPlayers.withIndex().forEach { (index, entry) ->
    val (uuid, data) = entry

    val colorPrefix = when (index) {
        0 -> "§6§l"  // Gold
        1 -> "§7§l"  // Silver
        2 -> "§3§l"    // Bronze (adjusted from second gold)
        else -> "§f"
    }

    val textLine = "$colorPrefix#${index + 1}. ${data.first}: §c§l${data.second}§r"
*/






/* V2
fun displayLeaderboardAsFloatingText(x: Int, y: Int, z: Int) {
    // Sort players by Elo and get the top 10
    val topPlayers = eloMap.entries
        .sortedByDescending { it.value.second }
        .take(10)

    // Format the leaderboard
    val leaderboard = topPlayers.joinToString("\n") { (uuid, data) ->
        "${data.first}: §c§l${data.second}"
    }

    // Choose the position for the floating text
    val position = BlockPos(x, y, z)  // Position from the command input

    // Get the world (assuming you want it in the Overworld)
    val world: ServerWorld? = server.getWorld(World.OVERWORLD)  // Replace with your desired world if needed
    if (world != null) {
    // Create an AreaEffectCloud (this will be invisible but can have a custom name)
    val areaEffectCloud =
        AreaEffectCloudEntity(world, position.x.toDouble(), position.y.toDouble(), position.z.toDouble())
        areaEffectCloud.customName = Text.of(leaderboard)  // Set the leaderboard as the custom name
    areaEffectCloud.isInvisible = true  // Make the entity invisible
    areaEffectCloud.duration = Integer.MAX_VALUE  // Duration for how long the text stays (adjust this as needed)
    areaEffectCloud.hasNoGravity()
        areaEffectCloud.shouldRender(0.0)
    // Add the AreaEffectCloud to the world
    world.spawnEntity(areaEffectCloud)
}
}
*/
/*
fun displayLeaderboardAsFloatingText(x: Int, y: Int, z: Int) {
    // Sort players by Elo and get the top 10
    val topPlayers = eloMap.entries
        .sortedByDescending { it.value.second }
        .take(10)

    // Format the leaderboard
    val leaderboard = topPlayers.joinToString("\n") { (uuid, data) ->
        "${data.first}: §c§l${data.second}"
    }

    // Choose the position for the floating text
    val position = BlockPos(x, y, z)  // Position from the command input

    // Create an Armor Stand with the leaderboard text
    val world: ServerWorld? = server.getWorld(World.OVERWORLD)  // Assuming the first world is the lobby
    val armorStand = ArmorStandEntity(world, position.x.toDouble(), position.y.toDouble(), position.z.toDouble())
    armorStand.setCustomName(Text.of(leaderboard))  // Set the leaderboard as the name of the armor stand
    armorStand.isInvisible = true  // Make the armor stand invisible
    armorStand.isInvulnerable = true  // Make the armor stand invulnerable

    // Add the armor stand to the world
    world?.spawnEntity(armorStand)
}
*/


/* if (!Files.exists(filePath)) return
    val jsonString = Files.readString(filePath)
    val data = json.decodeFromString<Map<String, Int>>(jsonString)
    eloMap.clear()
    data.forEach { (uuid, elo) -> eloMap[UUID.fromString(uuid)] = elo }
}*/


/*fun getTopPlayers(limit: Int): List<Pair<UUID, Int>> {
    return eloMap.entries.sortedByDescending { it.value }.take(10)
        .map { it.toPair() }*/


