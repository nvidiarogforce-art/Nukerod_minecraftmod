package com.example.nukerod.entity;

import com.example.nukerod.config.NukeConfig;
import com.example.nukerod.explosion.CustomNukeExplosion;
import com.example.nukerod.sound.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Custom warhead projectile that replaces the vanilla fishing bobber.
 *
 * <p>Two-phase flight for an "orbital strike" feel:
 * <ul>
 *   <li>{@code ASCENDING} — after launch it arcs and climbs; once vertical
 *       velocity crosses zero (apex) it flips to falling.</li>
 *   <li>{@code FALLING} — accelerates downward, emitting a smoke/ember trail
 *       and a rising-pitch whistle, until it hits ground/entity or is
 *       manually reel-detonated.</li>
 * </ul>
 *
 * <p>Detonation always routes through {@link CustomNukeExplosion#trigger} on the
 * server; the entity then discards itself.
 */
public class NukeWarheadEntity extends ProjectileEntity {

    public enum Phase {ASCENDING, FALLING}

    private static final TrackedData<Integer> PHASE =
            DataTracker.registerData(NukeWarheadEntity.class, TrackedDataHandlerRegistry.INTEGER);

    // Low gravity + near-frictionless drag give a fast, flat, long-range shot
    // that still arcs down onto the ground when aimed high.
    private static final double GRAVITY = 0.02;
    private static final double DRAG = 0.997;
    private int airTicks = 0;
    /**
     * Safety fuse: detonate no matter what after this many ticks.
     */
    private static final int MAX_AIR_TICKS = 20 * 20;

    public NukeWarheadEntity(EntityType<? extends NukeWarheadEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(PHASE, Phase.ASCENDING.ordinal());
    }

    public Phase getPhase() {
        return Phase.values()[this.dataTracker.get(PHASE)];
    }

    public void setPhase(Phase phase) {
        this.dataTracker.set(PHASE, phase.ordinal());
    }

    /**
     * Launch helper mirroring the vanilla bobber cast arc.
     */
    public void launch(float pitch, float yaw, float speed) {
        float f = -MathHelper.sin(yaw * 0.017453292f) * MathHelper.cos(pitch * 0.017453292f);
        float g = -MathHelper.sin(pitch * 0.017453292f);
        float h = MathHelper.cos(yaw * 0.017453292f) * MathHelper.cos(pitch * 0.017453292f);
        this.setVelocity(f, g, h, speed, 0.0f);
    }

    @Override
    public void tick() {
        super.tick();
        airTicks++;

        Vec3d velocity = this.getVelocity();

        // Phase transition: flip to FALLING at the apex of the arc.
        if (getPhase() == Phase.ASCENDING && velocity.y <= 0.0) {
            setPhase(Phase.FALLING);
        }

        // Collision check against the path we are about to travel.
        HitResult hit = ProjectileUtil.getCollision(this, this::canHit);
        if (hit.getType() != HitResult.Type.MISS) {
            this.onCollision(hit);
            if (this.isRemoved()) {
                return;
            }
        }

        // Ballistic flight: launched straight down the crosshair, but gravity
        // pulls the arc back down so shots aimed high still fall and detonate on
        // the ground instead of flying off into the sky and vanishing.
        double startX = this.getX(), startY = this.getY(), startZ = this.getZ();
        double vy = velocity.y - GRAVITY;
        Vec3d next = new Vec3d(velocity.x * DRAG, vy, velocity.z * DRAG);
        this.setVelocity(next);
        this.setPosition(startX + next.x, startY + next.y, startZ + next.z);
        this.updateRotation();

        // Trail + whistle while falling (server-side authoritative FX).
        if (this.getEntityWorld() instanceof ServerWorld sw) {
            // Draw a CONTINUOUS fiery trajectory by spawning particles at several
            // interpolated points along this tick's travel segment. The warhead
            // moves several blocks per tick, so emitting at a single point would
            // leave visible gaps instead of a clear flight path.
            double ex = this.getX(), ey = this.getY(), ez = this.getZ();
            int steps = 8;
            for (int i = 1; i <= steps; i++) {
                double t = i / (double) steps;
                double tx = startX + (ex - startX) * t;
                double ty = startY + (ey - startY) * t;
                double tz = startZ + (ez - startZ) * t;
                sw.spawnParticles(ParticleTypes.FLAME, tx, ty, tz, 1, 0.02, 0.02, 0.02, 0.0);
                sw.spawnParticles(ParticleTypes.SMOKE, tx, ty, tz, 1, 0.02, 0.02, 0.02, 0.0);
            }
            // Bright head markers + molten flecks so the leading edge is obvious.
            sw.spawnParticles(ParticleTypes.END_ROD, ex, ey, ez, 2, 0.03, 0.03, 0.03, 0.0);
            sw.spawnParticles(ParticleTypes.LARGE_SMOKE, ex, ey, ez, 2, 0.06, 0.06, 0.06, 0.0);
            sw.spawnParticles(ParticleTypes.LAVA, ex, ey, ez, 1, 0.05, 0.05, 0.05, 0.0);
            if (airTicks % 4 == 0) {
                float pitch = 0.6f + Math.min(1.4f, airTicks / 60.0f);
                sw.playSound(null, this.getBlockPos(), ModSounds.FALLING_WHISTLE,
                        SoundCategory.HOSTILE, 3.0f, pitch);
            }
        }

        // Safety fuse.
        if (!this.getEntityWorld().isClient() && airTicks > MAX_AIR_TICKS) {
            detonate();
        }
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (!this.getEntityWorld().isClient()) {
            detonate();
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
    }

    /**
     * Detonate at the current position and remove the entity.
     */
    public void detonate() {
        if (!(this.getEntityWorld() instanceof ServerWorld sw) || this.isRemoved()) {
            return;
        }
        CustomNukeExplosion.trigger(sw, this.getBlockPos(), NukeConfig.get().power);
        this.discard();
    }

    @Override
    protected boolean canHit(Entity entity) {
        // Don't collide with the owner while ascending out of the barrel.
        return super.canHit(entity) && entity != this.getOwner();
    }

    @Override
    public void readData(ReadView view) {
        this.airTicks = view.getInt("AirTicks", 0);
    }

    @Override
    public void writeData(WriteView view) {
        view.putInt("AirTicks", this.airTicks);
    }
}
