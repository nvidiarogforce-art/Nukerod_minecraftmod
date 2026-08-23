package com.example.nukerod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.TintedParticleEffect;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * Reconstructs the entire detonation visual/audio suite on the client from a
 * single {@link com.example.nukerod.network.NukeEffectsPayload}.
 *
 * <p>All randomness is seeded from {@code fxSeed} so every client draws an
 * identical effect. Uses vanilla particle types (composed with custom counts /
 * velocities) so the mod is fully functional with zero custom particle
 * textures; swap in {@code ModParticles.*} where noted for the custom look.
 */
public final class NukeFxHandler {
    private NukeFxHandler() {}

    public static void play(MinecraftClient client, BlockPos impact, float power, long fxSeed) {
        ClientWorld world = client.world;
        if (world == null) {
            return;
        }
        Random rng = new Random(fxSeed);
        Vec3d c = Vec3d.ofCenter(impact);
        double radius = MathHelper.clamp(power * 2.0, 6.0, 60.0);

        // --- Screen flash + shake for nearby players ---
        double distToPlayer = client.player != null ? client.player.getEntityPos().distanceTo(c) : 9999;
        if (distToPlayer < radius * 8) {
            float intensity = (float) MathHelper.clamp(1.0 - distToPlayer / (radius * 8), 0.0, 1.0);
            NukeScreenShake.trigger(intensity);
            NukeScreenFlash.trigger(intensity);
        }

        // --- Detonation flash core (instant bright burst) ---
        for (int i = 0; i < 40; i++) {
            // 1.21.10: FLASH is now a tinted particle and needs an RGB color.
            world.addParticleClient(TintedParticleEffect.create(ParticleTypes.FLASH, 1.0f, 1.0f, 1.0f),
                    c.x, c.y + 1, c.z, 0, 0, 0);
        }

        // --- Fireball core: expanding orange/white sphere ---
        int fireballCount = (int) (120 * (radius / 12.0));
        for (int i = 0; i < fireballCount; i++) {
            Vec3d dir = randomUnit(rng);
            double speed = 0.3 + rng.nextDouble() * 0.9;
            world.addParticleClient(ParticleTypes.LAVA, c.x, c.y + 1, c.z,
                    dir.x * speed, Math.abs(dir.y) * speed, dir.z * speed);
            world.addParticleClient(ParticleTypes.FLAME, c.x, c.y + 1, c.z,
                    dir.x * speed * 1.5, Math.abs(dir.y) * speed, dir.z * speed * 1.5);
        }

        // --- Shockwave ring: flat, fast, at ground level ---
        int ringPoints = (int) (radius * 6);
        for (int i = 0; i < ringPoints; i++) {
            double angle = (Math.PI * 2 * i) / ringPoints;
            double vx = Math.cos(angle);
            double vz = Math.sin(angle);
            world.addParticleClient(ParticleTypes.EXPLOSION,
                    c.x + vx * 1.5, c.y + 0.5, c.z + vz * 1.5,
                    vx * 1.4, 0.02, vz * 1.4);
            world.addParticleClient(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    c.x + vx * 2.0, c.y + 0.3, c.z + vz * 2.0,
                    vx * 0.8, 0.0, vz * 0.8);
        }

        // --- Mushroom cloud + ash fallout: delayed rising column + cap ---
        NukeCloudAnimator.start(world, c, radius, fxSeed);

        // --- Sounds (client-side, in addition to the server's wide boom) ---
        world.playSound(client.player, impact, SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                SoundCategory.BLOCKS, 4.0f, 0.5f);
        world.playSound(client.player, impact, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                SoundCategory.BLOCKS, 3.0f, 0.6f);
    }

    private static Vec3d randomUnit(Random rng) {
        double theta = rng.nextDouble() * Math.PI * 2;
        double phi = Math.acos(2 * rng.nextDouble() - 1);
        double x = Math.sin(phi) * Math.cos(theta);
        double y = Math.cos(phi);
        double z = Math.sin(phi) * Math.sin(theta);
        return new Vec3d(x, y, z);
    }
}
