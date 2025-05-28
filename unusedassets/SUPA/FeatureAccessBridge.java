package com.serotonin.common.merchant;

import net.minecraft.client.render.entity.feature.FeatureRenderer;
import java.util.List;


public interface FeatureAccessBridge {
    List<FeatureRenderer<?, ?>> cobblemonEvolved$getFeatures();
}