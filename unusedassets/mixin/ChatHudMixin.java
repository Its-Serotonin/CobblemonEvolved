package com.serotonin.mixin;

import com.serotonin.common.chat.MiniMessageCompat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/*
@Environment(EnvType.CLIENT)
@Mixin(ChatHud.class)
public class ChatHudMixin {

    @Inject(
            method = "addMessage(Lnet/minecraft/text/Text;)V", // disambiguates overload
            at = @At("HEAD"),
            cancellable = true
    )
    public void interceptMiniMessage(Text message, CallbackInfo ci) {
        String raw = message.getString();
        if (!raw.contains("<")) return;

        Text parsed = MiniMessageCompat.INSTANCE.tryDeserializeMiniMessage(raw);
        if (parsed != null) {
            InGameHudAccessor hud = (InGameHudAccessor) MinecraftClient.getInstance().inGameHud;
            hud.getChatHud().addMessage(parsed);
            ci.cancel();
        }
    }
}*/
@SuppressWarnings("AmbiguousMethodReference")
@Environment(EnvType.CLIENT)
@Mixin(ChatHud.class)
public class ChatHudMixin {

    @Inject(method = "addMessage", at = @At("HEAD"), cancellable = true)
    public void interceptMiniMessage(Text message, CallbackInfo ci) {
        String raw = message.getString();

        if (!raw.contains("<") || !raw.contains(">")) return;

        if (raw.startsWith("<") && raw.contains(">")) {
            raw = raw.substring(raw.indexOf(">") + 1).trim();
        }

        Text parsed = MiniMessageCompat.INSTANCE.tryDeserializeMiniMessage(raw);
        if (parsed != null) {
            System.out.println("[ChatHudMixin] Raw intercepted: " + raw);
            InGameHudAccessor accessor = (InGameHudAccessor) MinecraftClient.getInstance().inGameHud;
            accessor.getChatHud().addMessage(parsed);
            ci.cancel();
        }
    }
}