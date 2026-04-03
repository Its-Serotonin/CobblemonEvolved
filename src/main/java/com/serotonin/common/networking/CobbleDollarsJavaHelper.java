package com.serotonin.common.networking;

import fr.harmex.cobbledollars.common.utils.extensions.PlayerExtensionKt;
import net.minecraft.entity.player.PlayerEntity;
import java.math.BigInteger;

public class CobbleDollarsJavaHelper {
    public static BigInteger getCobbleDollars(PlayerEntity player) {
        return PlayerExtensionKt.getCobbleDollars(player);
    }

    public static void setCobbleDollars(PlayerEntity player, BigInteger amount) {
        PlayerExtensionKt.setCobbleDollars(player, amount);
    }
}