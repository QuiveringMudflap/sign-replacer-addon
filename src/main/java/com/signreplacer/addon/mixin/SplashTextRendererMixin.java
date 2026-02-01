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

@Mixin(SplashTextRenderer.class)
public class SplashTextRendererMixin {

    @Shadow @Final private String text;

    private static final int SPLASH_Y = 90;

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

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(DrawContext context, int screenWidth, TextRenderer textRenderer, int alpha, CallbackInfo ci) {
        if (!OXYGEN_SPLASHES.contains(text)) return;
        // Draw only our yellow version with outline; cancel vanilla so the black splash is never drawn
        int outlineColor = (alpha << 24) | 0x000000;
        int fillColor = (alpha << 24) | (OXYGEN_YELLOW & 0x00FFFFFF);
        int textWidth = textRenderer.getWidth(text);
        int x = (screenWidth - textWidth) / 2;
        int y = SPLASH_Y;
        int o = 1;
        // Outline
        context.drawText(textRenderer, text, x - o, y, outlineColor, false);
        context.drawText(textRenderer, text, x + o, y, outlineColor, false);
        context.drawText(textRenderer, text, x, y - o, outlineColor, false);
        context.drawText(textRenderer, text, x, y + o, outlineColor, false);
        context.drawText(textRenderer, text, x - o, y - o, outlineColor, false);
        context.drawText(textRenderer, text, x + o, y - o, outlineColor, false);
        context.drawText(textRenderer, text, x - o, y + o, outlineColor, false);
        context.drawText(textRenderer, text, x + o, y + o, outlineColor, false);
        // Yellow center
        context.drawText(textRenderer, text, x, y, fillColor, false);
        ci.cancel();
    }
}
