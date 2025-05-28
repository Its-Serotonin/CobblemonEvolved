package com.serotonin.mixin;

import fr.harmex.cobbledollars.common.client.renderer.entity.CobbleMerchantRenderer;
import fr.harmex.cobbledollars.common.world.entity.CobbleMerchant;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CobbleMerchantRenderer.class)
public abstract class CobbleMerchantRendererMixin {
    @Inject(method = "getTextureLocation", at = @At("HEAD"), cancellable = true)
    private void injectLobbyVendorTexture(CobbleMerchant entity, CallbackInfoReturnable<Identifier> cir) {
        if (entity.getCommandTags().contains("lobby_vendor")) {
            cir.setReturnValue(Identifier.of("cobblemonevolved", "textures/lobby_vendor/lobby_vendor.png"));

        }
    }
}
