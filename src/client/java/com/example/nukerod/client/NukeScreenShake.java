package com.example.nukerod.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

/**
 * Lightweight client-side camera shake.
 *
 * <p>{@link #trigger(float)} sets a shake timer/intensity; a client-tick hook
 * decays it. The per-frame camera offset is exposed via
 * {@link #currentPitchOffset()} / {@link #currentYawOffset()} for a camera
 * mixin to read (see README "Optional camera-shake mixin"). This class owns
 * only the state + decay so it has no hard render deps.
 */
public final class NukeScreenShake {
    private NukeScreenShake() {}

    private static float intensity = 0.0f;
    private static int ticksRemaining = 0;
    private static final int DURATION = 30;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ticksRemaining > 0) {
                ticksRemaining--;
                intensity *= 0.90f; // exponential decay toward zero
                if (ticksRemaining == 0) {
                    intensity = 0.0f;
                }
            }
        });
    }

    public static void trigger(float strength) {
        intensity = Math.max(intensity, strength);
        ticksRemaining = DURATION;
    }

    /** Pseudo-random pitch offset (degrees) for the current frame. */
    public static float currentPitchOffset() {
        if (intensity <= 0) return 0f;
        MinecraftClient mc = MinecraftClient.getInstance();
        long t = mc.world != null ? mc.world.getTime() : 0;
        return (float) Math.sin(t * 2.3) * intensity * 2.0f;
    }

    /** Pseudo-random yaw offset (degrees) for the current frame. */
    public static float currentYawOffset() {
        if (intensity <= 0) return 0f;
        MinecraftClient mc = MinecraftClient.getInstance();
        long t = mc.world != null ? mc.world.getTime() : 0;
        return (float) Math.cos(t * 1.7) * intensity * 2.0f;
    }

    public static float intensity() {
        return intensity;
    }
}
