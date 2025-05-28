package com.serotonin.common.networking

import net.minecraft.item.ItemStack
import java.math.BigInteger


@JvmRecord
data class ExtendedBuyPacket(
    val itemStack: ItemStack,
    val unitPrice: BigInteger,
    val categoryName: String
)