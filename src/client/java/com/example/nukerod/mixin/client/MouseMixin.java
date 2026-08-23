package com.example.nukerod.mixin.client;

import com.example.nukerod.client.NukeZoom;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While the sniper scope is active, redirect the mouse wheel to adjust the zoom
 * level instead of scrolling the hotbar.
 */
@Mixin(Mouse.class)
public class MouseMixin {

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void nukerod$scopeScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (NukeZoom.isScoped()) {
            NukeZoom.adjustZoom(vertical);
            ci.cancel();
        }
    }
}
