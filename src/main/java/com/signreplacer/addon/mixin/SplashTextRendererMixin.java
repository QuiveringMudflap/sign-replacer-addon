package com.signreplacer.addon.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.font.TextRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(SplashTextRenderer.class)
public class SplashTextRendererMixin {

    @Shadow @Final private String text;

    private static final int SPLASH_Y = 90;

    private static final Set<String> MUDFLAP_SPLASHES = Set.of(
        "VOID",
        "Mudflap is love, Mudflap is life",
        "VOID SUPPLY SHOP",
        "QuiveringMudflap was here",
        "2b2t's finest",
        "Mudflap Mod loaded!"
    );

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DrawContext context, int screenWidth, TextRenderer textRenderer, int alpha, CallbackInfo ci) {
        if (!MUDFLAP_SPLASHES.contains(text)) return;
        int outlineColor = (alpha << 24) | 0x000000; // black outline with same alpha
        int textWidth = textRenderer.getWidth(text);
        int x = (screenWidth - textWidth) / 2;
        int y = SPLASH_Y;
        int o = 1;
        // Cardinal directions
        context.drawText(textRenderer, text, x - o, y, outlineColor, false);
        context.drawText(textRenderer, text, x + o, y, outlineColor, false);
        context.drawText(textRenderer, text, x, y - o, outlineColor, false);
        context.drawText(textRenderer, text, x, y + o, outlineColor, false);
        // Diagonals for thicker outline
        context.drawText(textRenderer, text, x - o, y - o, outlineColor, false);
        context.drawText(textRenderer, text, x + o, y - o, outlineColor, false);
        context.drawText(textRenderer, text, x - o, y + o, outlineColor, false);
        context.drawText(textRenderer, text, x + o, y + o, outlineColor, false);
    }
}
