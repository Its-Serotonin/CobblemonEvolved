package com.serotonin.mixin;

import com.serotonin.common.networking.ShopGatekeeper;
import com.serotonin.common.saveslots.ClientShopCategoryContext;
import fr.harmex.cobbledollars.common.client.gui.screen.widget.CategoryListWidget;
import fr.harmex.cobbledollars.common.world.item.trading.shop.Category;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/*
@Mixin(CategoryListWidget.CategoryEntry.class)
public abstract class CategoryEntryMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void onCategoryClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        try {

            ShopGatekeeper.requestEloUpdate();

            Category category = ((CategoryEntryAccessor) this).getCategory();
            if (category != null) {
                String name = category.getName();
                if (name != null && !name.isBlank()) {
                    String key = name.toLowerCase();
                    ClientShopCategoryContext.INSTANCE.set(key);
                    System.out.println("Set active category context: " + key);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
*/

@Mixin(CategoryListWidget.CategoryEntry.class)
public abstract class CategoryEntryMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void onCategoryClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        try {

            ShopGatekeeper.requestEloUpdate(true);


            if (this instanceof CategoryEntryAccessor accessor) {
                Category category = accessor.getCategory();
                if (category != null) {
                    String name = category.getName();
                    if (name != null && !name.isBlank()) {
                        String key = name.toLowerCase();
                        ClientShopCategoryContext.INSTANCE.set(key);
                        System.out.println("Set active category context: " + key);
                    }
                }
            } else {
                System.err.println("CategoryEntryAccessor not available — skipping context set.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}