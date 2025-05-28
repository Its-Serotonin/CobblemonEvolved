package com.serotonin.mixin;

import fr.harmex.cobbledollars.common.world.entity.CobbleMerchant;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.VillagerClothingFeatureRenderer;
import net.minecraft.client.render.entity.model.VillagerResemblingModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerClothingFeatureRenderer.class)
public abstract class VillagerClothingFeatureRendererMixin<T extends LivingEntity>
        extends FeatureRenderer<T, VillagerResemblingModel<T>> {

    @Unique
    private static final Identifier LOBBY_VENDOR_OVERLAY = Identifier.of("cobblemonevolved", "textures/entity/villager/profession/lobby_vendor.png");

    public VillagerClothingFeatureRendererMixin(FeatureRendererContext<T, VillagerResemblingModel<T>> context) {
        super(context);
    }

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V", at = @At("HEAD"), cancellable = true
    )
    private void cobblemonEvolved$renderCustomOverlay(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            T entity,
            float limbAngle,
            float limbDistance,
            float tickDelta,
            float animationProgress,
            float headYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        if (!(entity instanceof CobbleMerchant merchant)) return;
        if (!merchant.getCommandTags().contains("lobby_vendor")) return;

        Logger LOGGER = LogManager.getLogger("CobblemonEvolved");

        LOGGER.info("Villager overlay mixin fired");

        VillagerResemblingModel<T> model = this.getContextModel();
        model.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        model.render(
                matrices,
                vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(LOBBY_VENDOR_OVERLAY)),
                light,
                net.minecraft.client.render.OverlayTexture.DEFAULT_UV
        );

        ci.cancel();
    }
}