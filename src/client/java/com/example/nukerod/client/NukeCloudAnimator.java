package com.example.nukerod.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Animates the slow, multi-second mushroom cloud and ash fallout after a blast.
 *
 * <p>Each active blast is tracked as a {@link Cloud} and advanced one step per
 * client tick from a registered {@link ClientTickEvents#END_CLIENT_TICK}
 * callback (registered lazily on first use). The stem rises for the first
 * ~2 seconds, then a cap billows outward; ash drifts down for the full life.
 */
public final class NukeCloudAnimator {
    private NukeCloudAnimator() {}

    private static final List<Cloud> ACTIVE = new ArrayList<>();
    private static boolean registered = false;

    public static void start(ClientWorld world, Vec3d center, double radius, long seed) {
        ensureRegistered();
        synchronized (ACTIVE) {
            ACTIVE.add(new Cloud(world, center, radius, seed));
        }
    }

    private static void ensureRegistered() {
        if (registered) {
            return;
        }
        registered = true;
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            synchronized (ACTIVE) {
                ACTIVE.removeIf(Cloud::tick);
            }
        });
    }

    private static final class Cloud {
        private final ClientWorld world;
        private final Vec3d center;
        private final double radius;
        private final Random rng;
        private int age = 0;
        private final int lifetime;

        Cloud(ClientWorld world, Vec3d center, double radius, long seed) {
            this.world = world;
            this.center = center;
            this.radius = radius;
            this.rng = new Random(seed ^ 0x5DEECE66DL);
            // ~8-12 seconds depending on size.
            this.lifetime = 160 + (int) (radius * 4);
        }

        /** @return true when finished (so the list can drop it). */
        boolean tick() {
            age++;
            double stemHeight = Math.min(radius * 2.2, age * 0.9);

            // Rising stem for the first ~60 ticks.
            if (age < 60) {
                for (int i = 0; i < 6; i++) {
                    double y = center.y + rng.nextDouble() * stemHeight;
                    double jitter = radius * 0.10;
                    world.addParticleClient(ParticleTypes.LARGE_SMOKE, false, false,
                            center.x + gauss() * jitter, y, center.z + gauss() * jitter,
                            0, 0.08, 0);
                }
            }

            // Billowing cap once the stem is tall enough.
            if (age >= 30) {
                double capY = center.y + stemHeight;
                double capR = Math.min(radius * 1.4, (age - 30) * 0.25);
                for (int i = 0; i < 10; i++) {
                    double ang = rng.nextDouble() * Math.PI * 2;
                    double rr = Math.sqrt(rng.nextDouble()) * capR;
                    double px = center.x + Math.cos(ang) * rr;
                    double pz = center.z + Math.sin(ang) * rr;
                    world.addParticleClient(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, false, false,
                            px, capY + gauss() * (radius * 0.15), pz,
                            Math.cos(ang) * 0.03, 0.02, Math.sin(ang) * 0.03);
                    if (rng.nextInt(6) == 0) {
                        world.addParticleClient(ParticleTypes.LAVA, px, capY, pz, 0, -0.02, 0);
                    }
                }
            }

            // Ash fallout drifting down over the crater for the whole life.
            for (int i = 0; i < 4; i++) {
                double ax = center.x + (rng.nextDouble() - 0.5) * radius * 2.4;
                double az = center.z + (rng.nextDouble() - 0.5) * radius * 2.4;
                double ay = center.y + radius * 1.5 + rng.nextDouble() * radius;
                world.addParticleClient(ParticleTypes.ASH, ax, ay, az, 0, -0.05, 0);
            }

            return age >= lifetime;
        }

        private double gauss() {
            return rng.nextGaussian();
        }
    }
}
