package com.example.nukerod.particle;

import com.example.nukerod.NukeRodMod;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/**
 * Custom simple particle types used by the client FX handler.
 *
 * <p>These are registered as {@link SimpleParticleType}s (no extra data). On the
 * client they are bound to particle textures via
 * {@code assets/nukerod/particles/*.json}; a matching factory binding lives in
 * the client initializer.
 *
 * <p>For a zero-binary-asset run you can ignore these and compose the visuals
 * from vanilla particles (which the client handler also does); they are provided
 * so the "fully custom particle" path from the spec is available.
 */
public final class ModParticles {
    private ModParticles() {}

    public static SimpleParticleType MUSHROOM_SMOKE;
    public static SimpleParticleType SHOCKWAVE_RING;
    public static SimpleParticleType ASH_FALLOUT;
    public static SimpleParticleType FIREBALL_CORE;

    private static SimpleParticleType create(String path) {
        return Registry.register(Registries.PARTICLE_TYPE, NukeRodMod.id(path),
                FabricParticleTypes.simple());
    }

    public static void register() {
        MUSHROOM_SMOKE = create("mushroom_smoke");
        SHOCKWAVE_RING = create("shockwave_ring");
        ASH_FALLOUT = create("ash_fallout");
        FIREBALL_CORE = create("fireball_core");
        NukeRodMod.LOGGER.info("[nukerod] Registered particles");
    }
}
