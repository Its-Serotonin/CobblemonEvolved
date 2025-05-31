package com.serotonin.common.registries

//import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricEntityTypeBuilder
import com.serotonin.Cobblemonevolved.MOD_ID
import com.serotonin.common.entities.CustomNameTagRankEntity
import com.serotonin.common.entities.CustomRankedPlayerNameEntity
import com.serotonin.common.entities.LeaderboardArmorStandEntity
import com.serotonin.common.renderer.CustomNameTagRankEntityRenderer
import com.serotonin.common.renderer.CustomRankedPlayerNameEntityRenderer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.minecraft.client.render.entity.ArmorStandEntityRenderer
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnGroup
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier


object EntityRegister {
    val CUSTOM_NAME_TAG_RANK_ENTITY: EntityType<CustomNameTagRankEntity> =
        Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(MOD_ID, "custom_player_name_tag_rank"),
            EntityType.Builder
                .create({ _, world -> CustomNameTagRankEntity(world) }, SpawnGroup.MISC)
                .dimensions(0.1f, 0.1f)
                .maxTrackingRange(100)
                .trackingTickInterval(1)
                .disableSummon()
                .build()
        )

    val CUSTOM_RANKED_PLAYER_NAME_TAG_ENTITY: EntityType<CustomRankedPlayerNameEntity> =
        Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(MOD_ID, "custom_ranked_player_name_tag"),
            EntityType.Builder
                .create({ _, world -> CustomRankedPlayerNameEntity(world) }, SpawnGroup.MISC)
                .dimensions(0.1f, 0.1f)
                .maxTrackingRange(100)
                .trackingTickInterval(1)
                .disableSummon()
                .build()
        )



    val LEADERBOARD_ARMOR_STAND: EntityType<LeaderboardArmorStandEntity> =
        Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of("cobblemonevolved", "leaderboard_armor_stand"),
            EntityType.Builder
                .create({ _, world -> LeaderboardArmorStandEntity(EntityRegister.LEADERBOARD_ARMOR_STAND, world) }, SpawnGroup.MISC)
                .dimensions(0.0f, 0.0f)
                .disableSummon()
                .build()



        )






    fun registerEntities() {
        FabricDefaultAttributeRegistry.register(
            LEADERBOARD_ARMOR_STAND,
            LeaderboardArmorStandEntity.createAttributes()
        )
        }
}




@Environment(EnvType.CLIENT)
object ClientEntitiesRenderer {
    fun register() {
        println("REGISTERING ENTITY RENDERERS")


        EntityRendererRegistry.register(
            EntityRegister.CUSTOM_NAME_TAG_RANK_ENTITY,
            ::CustomNameTagRankEntityRenderer
        )
        println("Registered CustomNameTagRankEntityRenderer")

        EntityRendererRegistry.register(
            EntityRegister.CUSTOM_RANKED_PLAYER_NAME_TAG_ENTITY,
            ::CustomRankedPlayerNameEntityRenderer
        )
        println("Registered CustomRankedPlayerNameEntityRenderer")

        EntityRendererRegistry.register(
            EntityRegister.LEADERBOARD_ARMOR_STAND,
            ::ArmorStandEntityRenderer
        )

    }
}

