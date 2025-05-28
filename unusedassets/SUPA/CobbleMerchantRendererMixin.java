package com.serotonin.mixin;

import com.serotonin.common.entities.ConditionalClothingFeatureRenderer;
import com.serotonin.common.entities.LobbyVendorOverlayFeature;
import com.serotonin.common.merchant.FeatureAccessBridge;
import fr.harmex.cobbledollars.common.client.renderer.entity.CobbleMerchantRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(CobbleMerchantRenderer.class)
public class CobbleMerchantRendererMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void injectCustomOverlay(EntityRendererFactory.Context context, CallbackInfo ci) {
        List<FeatureRenderer<?, ?>> features =
                ((FeatureAccessBridge) this).cobblemonEvolved$getFeatures();

        features.removeIf(f -> f.getClass().getSimpleName().equals("VillagerClothingFeatureRenderer"));

        var self = (CobbleMerchantRenderer) (Object) this;

        self.addFeature(new ConditionalClothingFeatureRenderer(self, context.getResourceManager()));
        self.addFeature(new LobbyVendorOverlayFeature(self));

        System.out.println("✅ Replaced clothing overlay + added lobby overlay");
    }
}