package com.serotonin.mixin;


import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import fr.harmex.cobbledollars.common.world.inventory.BankMenu;
import fr.harmex.cobbledollars.common.world.item.trading.ICobbleMerchant;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Mixin(BankMenu.class)
public abstract class BankMenuMixin {

    @Inject(
            method = "<init>(ILnet/minecraft/entity/player/PlayerInventory;Lnet/minecraft/entity/player/PlayerEntity;Lfr/harmex/cobbledollars/common/world/item/trading/ICobbleMerchant;)V",
            at = @At("TAIL")
    )
    private void injectCustomBankItems(int syncId, PlayerInventory inventory, PlayerEntity player, ICobbleMerchant merchant, CallbackInfo ci) {
        Entity entity = (Entity) merchant;
        if (!entity.getCommandTags().contains("lobby_vendor")) return;

        Path path = Paths.get("config", "cobbledollars", "lobby_bank.json");
        if (!Files.exists(path)) return;

        try {
            Map<String, List<Map<String, Object>>> json = new Gson().fromJson(
                    new FileReader(path.toFile()),
                    new TypeToken<Map<String, List<Map<String, Object>>>>(){}.getType()
            );

            List<Map<String, Object>> bank = json.get("bank");
            if (bank == null) return;

            BankMenu menu = (BankMenu) (Object) this;
            for (int i = 0; i < bank.size() && i < 36; i++) {
                Map<String, Object> offer = bank.get(i);
                String itemId = offer.get("item").toString();
                Identifier id = Identifier.tryParse(itemId);
                if (id != null && Registries.ITEM.containsId(id)) {
                    menu.getBankContainer().setStack(i, new ItemStack(Registries.ITEM.get(id)));
                }
            }

            System.out.println("🧾 Injected " + bank.size() + " custom bank items into BankMenu (client-side)");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}