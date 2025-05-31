package com.serotonin.common.renderer

import com.serotonin.common.entities.CustomNameTagRankEntity
import com.serotonin.common.entities.CustomRankedPlayerNameEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.EntityRenderer
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d


object CustomFade {

    private const val MAX_VISIBLE_DISTANCE = 250.0
    private const val FADE_START_DISTANCE = 180.0

    fun getOpacityForDistance(entity: Entity): Float {
        val player = MinecraftClient.getInstance().player ?: return 1.0f


        val distanceEntity = if (entity.hasVehicle() && entity.vehicle is PlayerEntity) {
            entity.vehicle as PlayerEntity
        } else {
            entity
        }

        val distance = player.squaredDistanceTo(distanceEntity)
        val fadeStartSq = FADE_START_DISTANCE * FADE_START_DISTANCE
        val maxDistanceSq = MAX_VISIBLE_DISTANCE * MAX_VISIBLE_DISTANCE

        return when {
            distance < fadeStartSq -> 1.0f
            distance > maxDistanceSq -> 0.0f
            else -> {
                val ratio = (maxDistanceSq - distance) / (maxDistanceSq - fadeStartSq)
                ratio.toFloat().coerceIn(0.0f, 1.0f)
            }
        }
    }
}



class CustomNameTagRankEntityRenderer(context: EntityRendererFactory.Context) : EntityRenderer<CustomNameTagRankEntity>(context) {
    override fun render(

        entity: CustomNameTagRankEntity,
        yaw: Float,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int
    ) {

        val player = MinecraftClient.getInstance().player ?: return


        val hideTag = "HIDEFROMPLAYER:${player.uuidAsString}"
        if (entity.commandTags.contains(hideTag) || entity.vehicle?.uuid == player.uuid) {
            return
        }


        val opacity = CustomFade.getOpacityForDistance(entity)
        if (opacity <= 0.0f) return
        val colorWithOpacity = ((opacity * 255).toInt() shl 24) or 0xFFFFFF


        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light)

        val customName = entity.customName ?: return

        this.shadowRadius = 0f
        this.shadowOpacity = 0f


        matrices.push()
        matrices.translate(0.0, 0.0, 0.0)
        matrices.multiply(this.dispatcher.rotation)
        matrices.scale(-0.025f, -0.025f, 0.025f)

        val text = customName.string
        val textWidth = textRenderer.getWidth(text) / 2

        val matrix = matrices.peek().positionMatrix

        textRenderer.draw(
            text,
            (-textWidth).toFloat(),
            0f,
            colorWithOpacity,
            false,
            matrix,
            vertexConsumers,
            TextRenderer.TextLayerType.SEE_THROUGH,
            0,
            light
        )

        matrices.pop()
    }

    override fun hasLabel(entity: CustomNameTagRankEntity): Boolean {
        return true
    }

    override fun getTexture(entity: CustomNameTagRankEntity?): Identifier? {
        return Identifier.of("minecraft", "textures/misc/unknown.png")
    }

    override fun getPositionOffset(entity: CustomNameTagRankEntity, tickDelta: Float): Vec3d {
        return Vec3d(0.0, -0.2, 0.0)
    }
}





class CustomRankedPlayerNameEntityRenderer(context: EntityRendererFactory.Context) : EntityRenderer<CustomRankedPlayerNameEntity>(context) {
    override fun render(
        entity: CustomRankedPlayerNameEntity,
        yaw: Float,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int
    ) {

        val player = MinecraftClient.getInstance().player ?: return


        val hideTag = "HIDEFROMPLAYER:${player.uuidAsString}"
        if (entity.commandTags.contains(hideTag) || entity.vehicle?.uuid == player.uuid)  {
            return
        }


        val opacity = CustomFade.getOpacityForDistance(entity)
        if (opacity <= 0.0f) return
        val colorWithOpacity = ((opacity * 255).toInt() shl 24) or 0xFFFFFF

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light)

        val customName = entity.customName ?: return


        this.shadowRadius = 0f
        this.shadowOpacity = 0f

        matrices.push()
        matrices.translate(0.0, 0.0, 0.0)
        matrices.multiply(this.dispatcher.rotation)
        matrices.scale(-0.025f, -0.025f, 0.025f)

        val text = customName.string
        val textWidth = textRenderer.getWidth(text) / 2

        val matrix = matrices.peek().positionMatrix

        textRenderer.draw(
            text,
            (-textWidth).toFloat(),
            0f,
            colorWithOpacity,
            false,
            matrix,
            vertexConsumers,
            TextRenderer.TextLayerType.SEE_THROUGH,
            0,
            light
        )

        matrices.pop()
    }

    override fun hasLabel(entity: CustomRankedPlayerNameEntity): Boolean {
        return true
    }

    override fun getTexture(entity: CustomRankedPlayerNameEntity?): Identifier? {
            return Identifier.of("minecraft", "textures/misc/unknown.png")
        }

        override fun getPositionOffset(entity: CustomRankedPlayerNameEntity, tickDelta: Float): Vec3d {
            return Vec3d(0.0, 0.1, 0.0)
        }
    }