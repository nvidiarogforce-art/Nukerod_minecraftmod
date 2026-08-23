package com.example.nukerod.client;

import com.example.nukerod.registry.ModItems;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * Nuke Rod HUD: the sniper-scope overlay (drawn while scoped) plus a small
 * corner ammo counter showing remaining Warhead Cores with the core icon as a
 * logo. Both are only visible while a Nuke Rod is held.
 */
public final class NukeHud {
    private NukeHud() {}

    public static void register() {
        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            PlayerEntity p = client.player;
            if (p == null || client.options.hudHidden) {
                return;
            }
            if (!p.getMainHandStack().isOf(ModItems.NUKE_ROD)
                    && !p.getOffHandStack().isOf(ModItems.NUKE_ROD)) {
                return;
            }

            if (NukeZoom.isScoped()) {
                drawScope(context, client);
            }

            // Ammo counter with the warhead-core icon as a logo.
            int count = countCores(p);
            int x = 8, y = 8;
            context.drawItem(new ItemStack(ModItems.WARHEAD_CORE), x, y);
            context.drawText(client.textRenderer, Text.literal("x " + count),
                    x + 20, y + 4, 0xFFFF5555, true);
        });
    }

    /** Procedural sniper scope: letterbox/vignette borders + thin crosshair. */
    private static void drawScope(DrawContext context, MinecraftClient client) {
        int w = context.getScaledWindowWidth();
        int h = context.getScaledWindowHeight();
        int cx = w / 2, cy = h / 2;
        int black = 0xFF000000;
        int bar = h / 8;
        int side = w / 6;

        // Black borders framing the lens.
        context.fill(0, 0, w, bar, black);
        context.fill(0, h - bar, w, h, black);
        context.fill(0, bar, side, h - bar, black);
        context.fill(w - side, bar, w, h - bar, black);

        // Thin sniper crosshair with a center gap.
        int line = 0xEE111111;
        int gap = 7;
        context.fill(side, cy, cx - gap, cy + 1, line);      // left arm
        context.fill(cx + gap, cy, w - side, cy + 1, line);  // right arm
        context.fill(cx, bar, cx + 1, cy - gap, line);       // top arm
        context.fill(cx, cy + gap, cx + 1, h - bar, line);   // bottom arm

        // Center aiming dot (also masks the vanilla crosshair).
        context.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFF3030);

        // Magnification readout.
        context.drawText(client.textRenderer,
                Text.literal(String.format("%.1fx", NukeZoom.magnification())),
                cx + 12, cy + 12, 0xFF66FF66, true);
    }

    private static int countCores(PlayerEntity p) {
        int n = 0;
        for (int i = 0; i < p.getInventory().size(); i++) {
            ItemStack s = p.getInventory().getStack(i);
            if (s.isOf(ModItems.WARHEAD_CORE)) {
                n += s.getCount();
            }
        }
        return n;
    }
}
