package com.serotonin.mixin;

import com.cobblemon.mod.common.entity.npc.NPCPlayerTexture;
import com.serotonin.common.merchant.ICobbleMerchantTexture;
import fr.harmex.cobbledollars.common.world.entity.CobbleMerchant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CobbleMerchant.class)
public abstract class CobbleMerchantTextureMixin implements ICobbleMerchantTexture {

    @Unique
    private NPCPlayerTexture customTexture;

    @Unique
    private byte[] rawTexture;

    @Override
    public NPCPlayerTexture cobblemonEvolvedModV2_1_21_1$getCustomTexture() {
        return customTexture;
    }

    @Override
    public void cobblemonEvolvedModV2_1_21_1$setCustomTexture(NPCPlayerTexture texture) {
        this.customTexture = texture;
    }

    @Override
    public byte[] cobblemonEvolvedModV2_1_21_1$getRawTexture() {
        return rawTexture;
    }

    @Override
    public void cobblemonEvolvedModV2_1_21_1$setRawTexture(byte[] bytes) {
        this.rawTexture = bytes;
    }
}