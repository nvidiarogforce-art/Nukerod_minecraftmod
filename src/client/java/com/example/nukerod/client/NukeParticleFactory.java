package com.example.nukerod.client;

import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

/**
 * Generic sprite-billboard factory shared by every custom particle type in
 * {@code ModParticles}.
 */
public class NukeParticleFactory implements ParticleFactory<SimpleParticleType> {

    private final SpriteProvider sprites;

    public NukeParticleFactory(SpriteProvider sprites) {
        this.sprites = sprites;
    }

    @Override
    public Particle createParticle(SimpleParticleType type, ClientWorld world,
                                   double x, double y, double z,
                                   double vx, double vy, double vz, Random random) {
        return new NukeParticle(world, x, y, z, vx, vy, vz, this.sprites, random);
    }

    /** A smoke-like billowing particle: rises/drifts, fades, and shrinks. */
    private static class NukeParticle extends BillboardParticle {

        protected NukeParticle(ClientWorld world, double x, double y, double z,
                               double vx, double vy, double vz,
                               SpriteProvider sprites, Random random) {
            super(world, x, y, z, vx, vy, vz, sprites.getSprite(random));
            this.velocityX = vx;
            this.velocityY = vy;
            this.velocityZ = vz;
            this.scale = 1.2f + this.random.nextFloat() * 1.5f;
            this.maxAge = 40 + this.random.nextInt(40);
            this.gravityStrength = 0.0f;
            this.collidesWithWorld = false;
            // Dark smoke tint by default.
            this.red = this.green = this.blue = 0.15f + this.random.nextFloat() * 0.1f;
        }

        @Override
        protected RenderType getRenderType() {
            // 1.21.10: particle render sheets moved to BillboardParticle.RenderType.
            return RenderType.PARTICLE_ATLAS_TRANSLUCENT;
        }

        @Override
        public void tick() {
            if (this.age++ >= this.maxAge) {
                this.markDead();
                return;
            }
            this.velocityX *= 0.96;
            this.velocityZ *= 0.96;
            this.move(this.velocityX, this.velocityY, this.velocityZ);
            // Fade out over the back half of life.
            float lifeFrac = (float) this.age / this.maxAge;
            this.alpha = 1.0f - Math.max(0f, (lifeFrac - 0.5f) * 2f);
        }
    }
}
