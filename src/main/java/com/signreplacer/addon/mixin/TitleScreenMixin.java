package com.signreplacer.addon.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
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

    private static final String CREDITS_PREFIX = "Made by QuiveringMudflap - ";
    private static final String CREDITS_GOLD = "Mudflap Mod";
    private static final int GOLD_COLOR = 0xFFAA00;
    private static final int WHITE_COLOR = 0xFFFFFF;

    private static final String[] MUDFLAP_SPLASHES = {
        "VOID",
        "Mudflap is love, Mudflap is life",
        "VOID SUPPLY SHOP",
        "QuiveringMudflap was here",
        "2b2t's finest",
        "Mudflap Mod loaded!"
    };

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        // 10% chance to show a Mudflap splash
        if (new Random().nextInt(100) < 10) {
            String splash = MUDFLAP_SPLASHES[new Random().nextInt(MUDFLAP_SPLASHES.length)];
            this.splashText = new SplashTextRenderer(splash);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        TextRenderer tr = client.textRenderer;
        int w = screen.width;
        int pad = 4;
        int prefixWidth = tr.getWidth(CREDITS_PREFIX);
        int goldWidth = tr.getWidth(CREDITS_GOLD);
        int totalWidth = prefixWidth + goldWidth;
        int x = w - totalWidth - pad;
        int y = pad;
        context.drawText(tr, CREDITS_PREFIX, x, y, WHITE_COLOR, false);
        context.drawText(tr, CREDITS_GOLD, x + prefixWidth, y, GOLD_COLOR, false);
    }
}
