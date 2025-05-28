package com.serotonin.mixin;

import com.cobblemon.mod.common.api.net.serializers.NPCPlayerTextureSerializer;
import com.cobblemon.mod.common.entity.npc.NPCPlayerTexture;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import static com.cobblemon.mod.common.util.BufferUtilsKt.readEnumConstant;

@Mixin(NPCPlayerTextureSerializer.class)
public abstract class NPCPlayerTextureSerializerMixin {

    @Inject(method = "read", at = @At("HEAD"), cancellable = true)
    private void cancelOnBrokenRead(RegistryByteBuf buffer, CallbackInfoReturnable<NPCPlayerTexture> cir) {
        try {

        } catch (Exception e) {
            System.err.println("NPC texture read failed. Cancelling.");
            e.printStackTrace();
            cir.setReturnValue(null);
        }
    }
}