package com.example.nukerod.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;

/**
 * A brief white screen flash overlay on detonation, fading over ~10 frames.
 *
 * <p>Drawn as a full-screen white quad whose alpha follows a decaying timer.
 * Registered lazily on first trigger.
 */
public final class NukeScreenFlash {
    private NukeScreenFlash() {}

    private static float alpha = 0.0f;
    private static boolean registered = false;

    public static void trigger(float intensity) {
        ensureRegistered();
        alpha = Math.max(alpha, Math.min(1.0f, intensity));
    }

    private static void ensureRegistered() {
        if (registered) {
            return;
        }
        registered = true;
        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            if (alpha <= 0.01f) {
                return;
            }
            MinecraftClient mc = MinecraftClient.getInstance();
            int w = mc.getWindow().getScaledWidth();
            int h = mc.getWindow().getScaledHeight();
            int a = (int) (alpha * 255) & 0xFF;
            int color = (a << 24) | 0x00FFFFFF; // ARGB white
            context.fill(0, 0, w, h, color);

            // Fade a bit each frame.
            alpha -= 0.06f;
            if (alpha < 0) {
                alpha = 0;
            }
        });
    }
}
