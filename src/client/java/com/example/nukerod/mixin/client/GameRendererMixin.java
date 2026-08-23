package com.example.nukerod.mixin.client;

import com.example.nukerod.client.NukeZoom;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Shrinks the rendered field of view while {@link NukeZoom} is active, giving
 * the Nuke Rod a spyglass-style zoom. Uses MixinExtras' {@code @ModifyReturnValue}
 * on the private {@code GameRenderer#getFov} so the zoom composes cleanly with
 * vanilla FOV effects instead of replacing them.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private float nukerod$applyZoom(float fov) {
        // Multiplier is ~1.0 when not scoped, so this is a no-op unless zooming.
        return fov * NukeZoom.getFovMultiplier();
    }
}
