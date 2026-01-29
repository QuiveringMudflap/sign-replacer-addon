package com.signreplacer.addon.mixin;

import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    @Shadow private SplashTextRenderer splashText;

    private static final String[] MUDFLAP_SPLASHES = {
        "VOID",
        "Mudflap is love, Mudflap is life",
        "VOID SUPPLY SHOP",
        "Lightning Fast Delivery!",
        ".gg/shop2b2t",
        "QuiveringMudflap was here",
        "2b2t's finest",
        "Mudflap Mod loaded!"
    };

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        // 50% chance to show a Mudflap splash
        if (new Random().nextBoolean()) {
            String splash = MUDFLAP_SPLASHES[new Random().nextInt(MUDFLAP_SPLASHES.length)];
            this.splashText = new SplashTextRenderer(splash);
        }
    }
}
