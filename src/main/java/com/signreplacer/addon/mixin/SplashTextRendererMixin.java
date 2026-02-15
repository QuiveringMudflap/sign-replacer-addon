package com.signreplacer.addon.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.font.TextRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(SplashTextRenderer.class)
public class SplashTextRendererMixin {

    @Shadow @Final private String text;

    private static final String REPLACE_SPLASH_OLD = "MiniGame159 based god";
    private static final String REPLACE_SPLASH_NEW = "QuiveringMudflap based god";

    private static final String METEOR_REPLACEMENT = "discord.gg/shop2b2t";

    private static final Set<String> OXYGEN_SPLASHES = Set.of(
        "VOID",
        "Oxygen is love, Oxygen is life",
        "VOID SUPPLY SHOP",
        "QuiveringMudflap was here",
        "2b2t's finest",
        "Oxygen Mod loaded!",
        "QuiveringMudflap based god",
        "THE VOID NEVER WAITS",
        "discord.gg/shop2b2t"
    );

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static String replaceSplashName(String text) {
        if (text == null) return text;
        if (REPLACE_SPLASH_OLD.equals(text)) return REPLACE_SPLASH_NEW;
        // Replace Meteor Client splash with shop link
        String lower = text.toLowerCase();
        if (lower.equals("meteorclient.com") || lower.equals("www.meteorclient.com")) {
            return METEOR_REPLACEMENT;
        }
        return text;
    }

    private static final int OXYGEN_YELLOW = 0xFFFF00; // gold/yellow for our splashes
    private static final int OUTLINE_OFFSET = 1;

    /**
     * Redirect vanilla's single drawText call. Vanilla already computed x,y with correct
     * position (bottom-right of logo) and wobble. We only replace the draw with our
     * yellow + outline for Oxygen splashes; otherwise pass through.
     */
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)V"
        )
    )
    private void redirectSplashDraw(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int color, boolean shadow) {
        if (!OXYGEN_SPLASHES.contains(text)) {
            context.drawText(textRenderer, text, x, y, color, shadow);
            return;
        }
        // Use vanilla's x,y (has wobble + correct position). Draw outline then yellow.
        int alpha = (color >> 24) & 0xFF;
        int outlineColor = (alpha << 24) | 0x000000;
        int fillColor = (alpha << 24) | (OXYGEN_YELLOW & 0x00FFFFFF);
        context.drawText(textRenderer, text, x - OUTLINE_OFFSET, y, outlineColor, false);
        context.drawText(textRenderer, text, x + OUTLINE_OFFSET, y, outlineColor, false);
        context.drawText(textRenderer, text, x, y - OUTLINE_OFFSET, outlineColor, false);
        context.drawText(textRenderer, text, x, y + OUTLINE_OFFSET, outlineColor, false);
        context.drawText(textRenderer, text, x - OUTLINE_OFFSET, y - OUTLINE_OFFSET, outlineColor, false);
        context.drawText(textRenderer, text, x + OUTLINE_OFFSET, y - OUTLINE_OFFSET, outlineColor, false);
        context.drawText(textRenderer, text, x - OUTLINE_OFFSET, y + OUTLINE_OFFSET, outlineColor, false);
        context.drawText(textRenderer, text, x + OUTLINE_OFFSET, y + OUTLINE_OFFSET, outlineColor, false);
        context.drawText(textRenderer, text, x, y, fillColor, false);
    }
}
