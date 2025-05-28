package com.serotonin.mixin;

import com.cobblemon.mod.common.api.spawning.context.SpawningContext;
import com.cobblemon.mod.common.api.spawning.fishing.FishingSpawner;
import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawner;
import com.cobblemon.mod.common.api.spawning.spawner.Spawner;
import com.github.d0ctorleon.mythsandlegends.cobblemon.spawning.condition.MythsAndLegendsConditions;
import com.github.d0ctorleon.mythsandlegends.cobblemon.spawning.condition.keyitem.KeyItemConditions;
import com.github.d0ctorleon.mythsandlegends.utils.PlayerDataUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

/*
@Mixin(value = KeyItemConditions.KeyItem.class, remap = false)
public class KeyItemConditionMixin {
    @Inject(method = "fits", at = @At("HEAD"), cancellable = true)
    private void overrideFits(SpawningContext context, CallbackInfoReturnable<Boolean> cir) {
        try {
            Identifier originalId = (Identifier)
                    KeyItemConditions.KeyItem.class.getDeclaredField("key_item").get(this);


            if (originalId == null || originalId.getPath().equals("none")) return;
            if (!originalId.getPath().equals("beads_of_ruin")) return;

            Spawner spawner = context.getSpawner();
            if (spawner instanceof PlayerSpawner playerSpawner) {
                ServerPlayerEntity player = MythsAndLegendsConditions.getPlayerFromUUID(context.getWorld(), playerSpawner.getUuid());
                if (player != null) {

                    Identifier correctId = Identifier.of("cobblemonevolved", "beads_of_ruin");
                    Item item = Registries.ITEM.get(correctId);
                    int count = PlayerDataUtils.getPlayerData(player).getItemCount(item);

                    cir.setReturnValue(count >= 1);
                } else {
                    cir.setReturnValue(false);
                }
            } else {
                cir.setReturnValue(false);
            }

        } catch (Exception e) {
            e.printStackTrace();
            cir.setReturnValue(false);
        }
    }
}
*/
@Mixin(value = KeyItemConditions.KeyItem.class, remap = false)
public class KeyItemConditionMixin {
    @Inject(method = "fits", at = @At("HEAD"), cancellable = true)
    private void overrideFits(SpawningContext context, CallbackInfoReturnable<Boolean> cir) {
        try {
            Identifier originalId = (Identifier)
                    KeyItemConditions.KeyItem.class.getDeclaredField("key_item").get(this);


            if (originalId == null || originalId.getPath().equals("none")) return;


            if (!originalId.getPath().equals("beads_of_ruin")) return;

            Spawner spawner = context.getSpawner();
            if (spawner instanceof PlayerSpawner playerSpawner) {
                ServerPlayerEntity player = MythsAndLegendsConditions.getPlayerFromUUID(context.getWorld(), playerSpawner.getUuid());
                if (player != null) {

                    Identifier correctId = Identifier.of("cobblemonevolved", "beads_of_ruin");
                    Item item = Registries.ITEM.get(correctId);
                    int count = PlayerDataUtils.getPlayerData(player).getItemCount(item);
                    cir.setReturnValue(count >= 1);
                } else {
                    cir.setReturnValue(false);
                }
            } else {
                cir.setReturnValue(false);
            }

        } catch (Exception e) {
            e.printStackTrace();
            cir.setReturnValue(false);
        }
    }
}