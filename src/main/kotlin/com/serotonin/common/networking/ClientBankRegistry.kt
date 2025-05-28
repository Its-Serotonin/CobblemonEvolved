package com.serotonin.common.networking


import fr.harmex.cobbledollars.common.world.item.trading.shop.Offer
import java.util.*

object ClientBankRegistry {
    private val bankByMerchant: MutableMap<UUID, List<Offer>> = mutableMapOf()

    fun setBank(uuid: UUID, offers: List<Offer>) {
        bankByMerchant[uuid] = offers
    }

    fun getBank(uuid: UUID): List<Offer> = bankByMerchant[uuid] ?: emptyList()
}