package com.serotonin.common.entities
import fr.harmex.cobbledollars.common.world.entity.CobbleMerchant
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.feature.FeatureRenderer
import net.minecraft.client.render.entity.feature.FeatureRendererContext
import net.minecraft.client.render.entity.model.VillagerResemblingModel
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.resource.ResourceManager
import net.minecraft.util.Identifier


class ConditionalClothingFeatureRenderer(
    context: FeatureRendererContext<CobbleMerchant, VillagerResemblingModel<CobbleMerchant>>,
    private val resourceManager: ResourceManager
) : FeatureRenderer<CobbleMerchant, VillagerResemblingModel<CobbleMerchant>>(context) {

    private val defaultTexture = Identifier.of("minecraft", "textures/entity/villager/profession/villager.png")

    override fun render(
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        entity: CobbleMerchant,
        limbAngle: Float,
        limbDistance: Float,
        tickDelta: Float,
        animationProgress: Float,
        headYaw: Float,
        headPitch: Float
    ) {
        val name = entity.name.string
        if (name == "Lobby Vendor") return

        val model = contextModel
        model.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch)
        model.render(
            matrices,
            vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(defaultTexture)),
            light,
            OverlayTexture.DEFAULT_UV
        )
    }
}