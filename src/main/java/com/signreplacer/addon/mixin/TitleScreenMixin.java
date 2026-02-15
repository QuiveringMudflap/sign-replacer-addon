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

    private static final String[] OXYGEN_SPLASHES = {
        "VOID",
        "Oxygen is love, Oxygen is life",
        "VOID SUPPLY SHOP",
        "QuiveringMudflap was here",
        "2b2t's finest",
        "Oxygen Mod loaded!",
        "QuiveringMudflap based god",
        "THE VOID NEVER WAITS",
        "discord.gg/shop2b2t"
    };
    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        // 10% chance to show an Oxygen splash
        if (new Random().nextInt(100) < 10) {
            String splash = OXYGEN_SPLASHES[new Random().nextInt(OXYGEN_SPLASHES.length)];
            this.splashText = new SplashTextRenderer(splash);
        }
    }
}
