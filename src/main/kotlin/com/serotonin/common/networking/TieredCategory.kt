
package com.serotonin.common.networking


import fr.harmex.cobbledollars.common.world.item.trading.shop.Category
import fr.harmex.cobbledollars.common.world.item.trading.shop.Offer


class TieredCategory(
    val category: Category,
    val metadata: Map<String, Any> = emptyMap()
) {
    val name: String get() = category.name
    val offers: ArrayList<Offer> get() = category.offers
}