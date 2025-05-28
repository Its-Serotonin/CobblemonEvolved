package com.serotonin.mixin;

import fr.harmex.cobbledollars.common.client.gui.screen.ShopScreen;
import fr.harmex.cobbledollars.common.client.gui.screen.widget.CategoryListWidget;
import fr.harmex.cobbledollars.common.client.gui.screen.widget.OfferListWidget;
import fr.harmex.cobbledollars.common.network.CobbleDollarsNetwork;
import fr.harmex.cobbledollars.common.network.packets.c2s.BuyPacket;
import fr.harmex.cobbledollars.common.world.item.trading.shop.Category;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShopScreen.class)
public abstract class ShopScreenMixin {

    @Inject(method = "buy", at = @At("HEAD"), cancellable = true)
    private void injectBuyContext(CallbackInfo ci) {
        try {
            ShopScreen screen = (ShopScreen) (Object) this;

            CategoryListWidget.CategoryEntry selectedCategory =
                    (CategoryListWidget.CategoryEntry) screen.getCategoryList().method_25334();

            if (selectedCategory != null) {
                Category category = ((CategoryEntryAccessor) selectedCategory).getCategory();
                if (category != null) {
                    String categoryName = category.getName();
                    if (categoryName != null) {
                        com.serotonin.common.networking.CategoryContext.INSTANCE.set(categoryName.toLowerCase());
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}