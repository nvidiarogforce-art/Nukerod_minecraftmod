package com.example.nukerod.client;

import com.example.nukerod.network.FireRodPayload;
import com.example.nukerod.registry.ModItems;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Sniper-scope controller for the Nuke Rod (client-side).
 *
 * <ul>
 *   <li><b>Right-click</b> (use key) toggles the scope while holding the rod.</li>
 *   <li><b>Scroll</b> (via {@code MouseMixin}) adjusts magnification while scoped.</li>
 *   <li><b>Left-click</b> (attack key) sends a {@link FireRodPayload} to fire.</li>
 * </ul>
 *
 * The FOV multiplier is smoothed each tick for an AWP-style zoom, and read by
 * {@code GameRendererMixin}. Input keys are only polled while the rod is held,
 * so other items' controls are never consumed.
 */
public final class NukeZoom {
    private NukeZoom() {}

    private static boolean scoped = false;
    private static double targetZoom = 0.25;      // selected level (4x)
    private static double currentZoom = 1.0;      // smoothed FOV multiplier
    private static final double MIN_ZOOM = 0.12;  // ~8x
    private static final double MAX_ZOOM = 0.5;   // 2x

    // Previous held-state of the mouse buttons, for rising-edge detection.
    private static boolean wasUseDown = false;
    private static boolean wasAttackDown = false;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(NukeZoom::clientTick);
    }

    private static void clientTick(MinecraftClient client) {
        // We read the buttons' HELD state (isPressed) and detect the rising edge
        // ourselves. Polling wasPressed() here does not work: Minecraft's own
        // input handler already drained that queue earlier in the same tick.
        if (!holdingRod(client) || client.currentScreen != null) {
            scoped = false;
            wasUseDown = client.options.useKey.isPressed();
            wasAttackDown = client.options.attackKey.isPressed();
        } else {
            boolean useDown = client.options.useKey.isPressed();
            if (useDown && !wasUseDown) {
                scoped = !scoped;   // right-click toggles the scope
            }
            wasUseDown = useDown;

            boolean attackDown = client.options.attackKey.isPressed();
            if (attackDown && !wasAttackDown) {
                ClientPlayNetworking.send(new FireRodPayload());  // left-click fires
            }
            wasAttackDown = attackDown;
        }
    }

    private static boolean holdingRod(MinecraftClient client) {
        PlayerEntity p = client.player;
        return p != null && (p.getMainHandStack().isOf(ModItems.NUKE_ROD)
                || p.getOffHandStack().isOf(ModItems.NUKE_ROD));
    }

    /** Called from MouseMixin while scoped; scroll up zooms in, down zooms out. */
    public static void adjustZoom(double scrollY) {
        targetZoom = MathHelper.clamp(targetZoom - scrollY * 0.03, MIN_ZOOM, MAX_ZOOM);
    }

    public static boolean isScoped() {
        return scoped;
    }

    /**
     * Smoothed FOV multiplier (≈1.0 when not scoped, smaller when zoomed in).
     *
     * <p>Eased HERE, per rendered frame, rather than in the 20 TPS client tick —
     * ticking the zoom only 20 times a second is what made it look stepped and
     * laggy. This is called once per frame by {@code GameRendererMixin}.
     */
    public static float getFovMultiplier() {
        double target = scoped ? targetZoom : 1.0;
        // Frame-rate independent easing toward the target.
        currentZoom += (target - currentZoom) * 0.35;
        if (Math.abs(currentZoom - target) < 0.0005) {
            currentZoom = target;
        }
        return (float) currentZoom;
    }

    /** Current magnification for the HUD readout (e.g. 4.0×). */
    public static float magnification() {
        return (float) (1.0 / targetZoom);
    }
}
