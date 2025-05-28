package com.serotonin.mixin;


import com.serotonin.common.networking.ShopGatekeeper;
import com.serotonin.common.saveslots.ClientShopCategoryContext;
import fr.harmex.cobbledollars.common.client.gui.screen.widget.BuyButton;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.MinecraftClient;
import com.serotonin.common.networking.ShopMetadataRegistry;
import com.serotonin.common.networking.ClientEloStorage;

@Mixin(ButtonWidget.class)
public class BuyButtonRedirectMixin {

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void injectBeforePress(CallbackInfo ci) {

        if (!(((Object) this) instanceof BuyButton buyButton)) return;

        String category = ClientShopCategoryContext.INSTANCE.get();
        if (category == null) {
            System.out.println("No category selected.");
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        Integer eloObj = ClientEloStorage.INSTANCE.getElo(player.getUuid().toString());
        int elo = eloObj != null ? eloObj : 0;
        int playerTier = ShopGatekeeper.getTierLevelFromElo(elo);
        int requiredTier = ShopMetadataRegistry.INSTANCE.getRequiredTierLevel(category);

        System.out.println("Buying from category: " + category);
        System.out.println("Required tier: " + requiredTier);
        System.out.println("Player tier: " + playerTier);

        if (playerTier < requiredTier) {
            player.sendMessage(Text.literal("§cYou need a higher rank to buy from this category!"));
            ci.cancel();
            return;
        }

        System.out.println("Tier check passed for category: " + category);
    }
}