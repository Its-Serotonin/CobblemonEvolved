package com.serotonin.mixin;

import fr.harmex.cobbledollars.common.world.entity.CobbleMerchant;
import fr.harmex.cobbledollars.common.world.item.trading.ICobbleMerchant;
import fr.harmex.cobbledollars.common.world.item.trading.shop.Bank;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;


@Mixin(CobbleMerchant.class)
public abstract class CobbleMerchantBankMixin  implements com.serotonin.common.merchant.ISeroMerchant{

    @Unique
    private Bank cobbleEvolved$customBank;

    @Unique
    public Bank cobblemonEvolvedModV2_1_21_1$getBank() {
        return Objects.requireNonNullElseGet(cobbleEvolved$customBank, Bank::new);
    }

    @Unique
    public void cobblemonEvolvedModV2_1_21_1$setBank(Bank bank) {
        this.cobbleEvolved$customBank = bank;
    }
}