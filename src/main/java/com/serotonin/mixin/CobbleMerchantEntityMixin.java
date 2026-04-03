package com.serotonin.mixin;

import com.serotonin.common.registries.MerchantProfessions;
import fr.harmex.cobbledollars.common.world.entity.CobbleMerchant;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.village.VillagerDataContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CobbleMerchant.class)
public abstract class CobbleMerchantEntityMixin {

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void setLobbyVendorProfessionOnLoad(NbtCompound nbt, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        if (self.getCommandTags().contains("lobby_vendor")) {
            VillagerDataContainer vdc = (VillagerDataContainer)(Object)this;
            vdc.setVillagerData(vdc.getVillagerData().withProfession(MerchantProfessions.INSTANCE.getLOBBY_VENDOR()));
        }
    }
}