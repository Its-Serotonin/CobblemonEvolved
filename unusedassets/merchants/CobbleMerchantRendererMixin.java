package com.serotonin.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import fr.harmex.cobbledollars.common.client.renderer.entity.CobbleMerchantRenderer;
import fr.harmex.cobbledollars.common.world.entity.CobbleMerchant;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.VillagerResemblingModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntityRenderer.class)
public abstract class MobEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {
    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void cobblemonEvolved$injectOverlay(
            T entity,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (!(entity instanceof CobbleMerchant merchant)) return;
        if (!merchant.getCommandTags().contains("lobby_vendor")) return;

        @SuppressWarnings("unchecked")
        MobEntityRenderer<CobbleMerchant, VillagerResemblingModel<CobbleMerchant>> renderer =
                (MobEntityRenderer<CobbleMerchant, VillagerResemblingModel<CobbleMerchant>>) (Object) this;

        VillagerResemblingModel<CobbleMerchant> model = renderer.getModel();
        model.setAngles(merchant, 0.0F, 0.0F, tickDelta, merchant.getHeadYaw(), merchant.getPitch());
        model.render(
                matrices,
                vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(new Identifier("cobblemonevolved", "textures/entity/villager/profession/lobby_vendor.png"))),
                light,
                OverlayTexture.DEFAULT_UV
        );
    }
}