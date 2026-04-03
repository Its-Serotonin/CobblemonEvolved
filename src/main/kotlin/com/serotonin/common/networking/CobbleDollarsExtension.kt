package com.serotonin.common.networking

import java.math.BigInteger
import net.minecraft.entity.player.PlayerEntity

fun PlayerEntity.getCobbleDollars(): BigInteger = CobbleDollarsJavaHelper.getCobbleDollars(this)
fun PlayerEntity.setCobbleDollars(amount: BigInteger) = CobbleDollarsJavaHelper.setCobbleDollars(this, amount)