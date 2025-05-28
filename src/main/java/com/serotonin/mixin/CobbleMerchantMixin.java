package com.serotonin.mixin;

import com.serotonin.common.merchant.ISeroMerchant;
import com.serotonin.common.networking.JsonMetadataBuilder;
import com.serotonin.common.networking.RawJsonPayload;
import com.serotonin.common.networking.ShopMetadataRegistry;
import fr.harmex.cobbledollars.common.network.CobbleDollarsNetwork;
import fr.harmex.cobbledollars.common.network.packets.s2c.SyncBankPacket;
import fr.harmex.cobbledollars.common.utils.extensions.PlayerExtensionKt;
import fr.harmex.cobbledollars.common.world.entity.CobbleMerchant;
import fr.harmex.cobbledollars.common.world.item.trading.ICobbleMerchant;
import fr.harmex.cobbledollars.common.world.item.trading.shop.Bank;
import fr.harmex.cobbledollars.common.world.item.trading.shop.Shop;
import kotlinx.serialization.json.Json;
import kotlinx.serialization.json.JsonObject;
import kotlinx.serialization.json.JsonPrimitive;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import fr.harmex.cobbledollars.common.world.item.trading.shop.Category;
import fr.harmex.cobbledollars.common.world.item.trading.shop.Offer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.registry.Registries;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.LinkedHashMap;


@Mixin(CobbleMerchant.class)
public abstract class CobbleMerchantMixin
        implements ISeroMerchant, ICobbleMerchant {

    @Unique private Shop customShop;
    @Unique private Bank customBank;

    @Override
    public @NotNull Shop getShop() {
        Entity self = (Entity) (Object) this;
        if (self.getCommandTags().contains("lobby_vendor")) {
            return customShop != null ? customShop : new Shop();
        }
        return new Shop();
    }

    @Override
    public void setShop(@NotNull Shop shop) {
        if (((Entity)(Object)this).getCommandTags().contains("lobby_vendor")) {
            this.customShop = shop;
        }
    }

    @Unique
    public @NotNull Bank getBank() {
        Entity self = (Entity) (Object) this;
        if (self.getCommandTags().contains("lobby_vendor")) {
            System.out.println("getBank() called, returning custom bank with " + (customBank != null ? customBank.size() : 0) + " entries");
            return customBank != null ? customBank : new Bank();
        }
        return new Bank();
    }

    @Unique
    public void setBank(Bank bank) {
        if (((Entity)(Object)this).getCommandTags().contains("lobby_vendor")) {
            this.customBank = bank;
        }
    }
    /*
    //new experimental
    @Unique
    private final UUID lobbyVendorUUID = UUID.randomUUID();

    @Override
    public @NotNull UUID getMerchantUUID() {
        return lobbyVendorUUID;
    }
    //
    */
    @Override public Shop cobblemonEvolvedModV2_1_21_1$getShop() { return customShop; }
    @Override public void cobblemonEvolvedModV2_1_21_1$setShop(Shop shop) { this.customShop = shop; }
    @Override public Bank cobblemonEvolvedModV2_1_21_1$getBank() { return customBank; }
    @Override public void cobblemonEvolvedModV2_1_21_1$setBank(Bank bank) { this.customBank = bank; }

    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void injectCustomShop(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        Entity entity = (Entity)(Object)this;
        if (!entity.getCommandTags().contains("lobby_vendor")) return;

        try {
            Path shopPath = Paths.get("config", "cobbledollars", "lobby_vendor.json");
            Path bankPath = Paths.get("config", "cobbledollars", "bank.json");
            Gson gson = new Gson();


            Map<String, Object> shopJson = gson.fromJson(new FileReader(shopPath.toFile()), new TypeToken<>() {}.getType());
            List<?> defaultShop = (List<?>) shopJson.get("defaultShop");

            Shop shop = new Shop();

            for (Object categoryObj : defaultShop) {
                if (!(categoryObj instanceof Map<?, ?> categoryMap)) continue;

                for (Map.Entry<?, ?> entry : categoryMap.entrySet()) {
                    String categoryId = entry.getKey().toString();
                    Map<?, ?> categoryData = (Map<?, ?>) entry.getValue();

                    int requiredTier = ((Number) categoryData.get("requiredTier")).intValue();
                    List<?> offersRaw = (List<?>) categoryData.get("offers");

                    List<Offer> offers = new ArrayList<>();
                    for (Object offerObj : offersRaw) {
                        if (!(offerObj instanceof Map<?, ?> offerMap)) continue;

                        String itemId = offerMap.get("item").toString();
                        int price = ((Number) offerMap.get("price")).intValue();

                        Identifier id = Identifier.tryParse(itemId);
                        if (id == null || !Registries.ITEM.containsId(id)) {
                            player.sendMessage(Text.literal("§cInvalid item in shop config: " + itemId), false);
                            continue;
                        }

                        ItemStack stack = new ItemStack(Registries.ITEM.get(id));
                        offers.add(new Offer(stack, price));
                    }

                    Category category = new Category(categoryId, new ArrayList<>(offers));


                    ShopMetadataRegistry.INSTANCE.getMetadataByCategory().put(
                            categoryId.toLowerCase(),
                            Map.of("requiredTierLevel", requiredTier)
                    );

                    shop.add(category);
                }
            }


            Map<String, Object> bankRoot = gson.fromJson(
                    new FileReader(bankPath.toFile()),
                    new TypeToken<Map<String, Object>>(){}.getType()
            );

            Bank bank = new Bank();
            Object rawBank = bankRoot.get("bank");
            if (rawBank instanceof List<?> bankList) {
                for (Object obj : bankList) {
                    if (obj instanceof Map<?, ?> offerMap) {
                        String itemId = offerMap.get("item").toString();
                        int price = ((Number) offerMap.get("price")).intValue();
                        Identifier id = Identifier.tryParse(itemId);
                        if (id != null && Registries.ITEM.containsId(id)) {
                            bank.add(new Offer(new ItemStack(Registries.ITEM.get(id)), price));
                        }
                    }
                }
            } else {
                player.sendMessage(Text.literal("§cInvalid bank format in config!"), false);
            }

            this.setShop(shop);
            this.setBank(bank);

            //experimental
            /*
            CobbleDollarsNetwork.INSTANCE.sendPacket(
                    (ServerPlayerEntity) player,
                    new SyncBankPacket(0, getMerchantUUID())
            );
            */
            JsonObject root = JsonMetadataBuilder.buildShopMetadata(ShopMetadataRegistry.INSTANCE.getMetadataByCategory());
            ServerPlayNetworking.send((ServerPlayerEntity) player, new RawJsonPayload(root.toString()));




            if (player.isSneaking()) {
                PlayerExtensionKt.openBank((ServerPlayerEntity) player, (ICobbleMerchant) this);
            } else {
                PlayerExtensionKt.openShop((ServerPlayerEntity) player, (ICobbleMerchant) this);
            }

            cir.setReturnValue(ActionResult.SUCCESS);

        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(Text.literal("§cFailed to open vendor: " + e.getMessage()), false);
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}