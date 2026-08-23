package com.example.nukerod.mixin.client;

import com.example.nukerod.client.NukeZoom;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the vanilla crosshair while the sniper scope is active, so Minecraft's
 * {@code +} does not overlap the scope's own crosshair drawn by {@code NukeHud}.
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void nukerod$hideCrosshairWhileScoped(DrawContext context,
                                                  RenderTickCounter tickCounter,
                                                  CallbackInfo ci) {
        if (NukeZoom.isScoped()) {
            ci.cancel();
        }
    }
}
