package com.serotonin.mixin;


import com.serotonin.common.merchant.FeatureAccessBridge;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin implements FeatureAccessBridge {

    @Accessor("features")
    @Override
    public abstract List<FeatureRenderer<?, ?>> cobblemonEvolved$getFeatures();
}