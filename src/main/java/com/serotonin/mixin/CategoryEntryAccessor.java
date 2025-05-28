package com.serotonin.mixin;

import fr.harmex.cobbledollars.common.client.gui.screen.widget.CategoryListWidget;
import fr.harmex.cobbledollars.common.world.item.trading.shop.Category;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@SuppressWarnings("unused")
@Mixin(CategoryListWidget.CategoryEntry.class)
public interface CategoryEntryAccessor {
    @Accessor("category")
    Category getCategory();
}
