package com.signreplacer.addon.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.font.TextRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * Oxygen mod: custom yellow splash texts, replace Meteor splash with shop link.
 * Uses @Inject at HEAD with require = 0 so mapping changes don't hard-crash the game.
 */
@Mixin(value = SplashTextRenderer.class, priority = 900)
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

    private static final int OXYGEN_YELLOW = 0xFFFF00;
    /** Vanilla splash position (bottom-right of logo). Match SplashTextRenderer constants for 1.21.4. */
    private static final int SPLASH_OFFSET_X = 76;
    private static final int SPLASH_Y = 68;
    private static final int OUTLINE_OFFSET = 1;

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private static String replaceSplashName(String text) {
        if (text == null) return text;
        if (REPLACE_SPLASH_OLD.equals(text)) return REPLACE_SPLASH_NEW;
        String lower = text.toLowerCase();
        if (lower.equals("meteorclient.com") || lower.equals("www.meteorclient.com")) {
            return METEOR_REPLACEMENT;
        }
        return text;
    }

    /**
     * For our Oxygen splashes: draw yellow + outline at vanilla splash position and cancel vanilla.
     * require = 0: if "render" signature changes in a future version, mixin is skipped instead of crashing.
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void onRenderHead(DrawContext context, int screenWidth, TextRenderer textRenderer, int alpha, CallbackInfo ci) {
        if (!OXYGEN_SPLASHES.contains(text)) return;

        int textWidth = textRenderer.getWidth(text);
        int x = (screenWidth - textWidth) / 2 + SPLASH_OFFSET_X;
        int y = SPLASH_Y;
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

        ci.cancel();
    }
}
