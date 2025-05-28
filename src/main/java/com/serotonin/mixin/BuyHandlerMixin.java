package com.serotonin.mixin;

import com.serotonin.common.networking.CategoryContext;
import com.serotonin.common.networking.ShopGatekeeper;
import com.serotonin.common.networking.ShopMetadataRegistry;
import fr.harmex.cobbledollars.common.network.packets.c2s.BuyPacket;
import fr.harmex.cobbledollars.common.network.handlers.server.BuyHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(BuyHandler.class)
public abstract class BuyHandlerMixin {

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private void injectCategoryCheck(BuyPacket packet, MinecraftServer server, ServerPlayerEntity player, CallbackInfo ci) {
        String categoryName = CategoryContext.get();
        if (categoryName == null) return;

        try {
            Map<String, Object> metadata = ShopMetadataRegistry.INSTANCE.getMetadata(categoryName.toLowerCase());
            int requiredTierLevel = (int) metadata.getOrDefault("requiredTierLevel", 0);

            boolean allowed = ShopGatekeeper.INSTANCE.canPurchaseFromCategory(player, requiredTierLevel, true);
            if (!allowed) {
                player.sendMessage(Text.literal("§cYou don't meet the tier requirement to buy from this category."));
                ci.cancel();
            }

        } catch (Exception e) {
            player.sendMessage(Text.literal("§cError checking shop category permissions."));
            e.printStackTrace();
            ci.cancel();
        } finally {
            CategoryContext.clear();
        }
    }
}