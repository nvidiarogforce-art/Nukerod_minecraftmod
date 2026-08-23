package com.example.nukerod.explosion;

import com.example.nukerod.block.ModBlocks;
import com.example.nukerod.config.NukeConfig;
import com.example.nukerod.network.NukeEffectsPayload;
import com.example.nukerod.sound.ModSounds;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 *   <li>Dresses the result with scorched ground, fire, rim debris, and an
 *       optional fallout {@link AreaEffectEntity}.</li>
 * <p>This does NOT call {@code World#createExplosion}. Instead it:
 * <ol>
 *   <li>Immediately applies entity damage + skyward/outward knockback and sends
 *       one {@link NukeEffectsPayload} to nearby clients for all visuals.</li>
 *   <li>Carves a wide, deep paraboloid crater — spread across multiple ticks
 *       via server end-of-tick scheduling so a big blast never stalls the
 *       server in a single tick.</li>
 *   <li>Dresses the result with scorched ground, fire, rim debris, and an
 *       optional fallout {@link AreaEffectCloudEntity}.</li>
 * </ol>
 */
public final class CustomNukeExplosion {
    private CustomNukeExplosion() {}

    /**
     * Entry point. Call on the server thread.
     *
     * @param world  server world
     * @param center detonation epicenter (the warhead's block position)
     * @param power  blast power scalar (drives damage; radius comes from config)
     */
    public static void trigger(ServerWorld world, BlockPos center, float power) {
        NukeConfig cfg = NukeConfig.get();
        int radius = cfg.effectiveRadius();
        int depth = cfg.effectiveDepth();

        boolean mayGrief = world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING);

        // 1) Instant effects: sound (loud, wide), entity damage + knockback.
        playImpactSounds(world, center);
        applyEntityEffects(world, center, radius, power);

        // 2) Client FX packet — one packet per nearby player.
        long fxSeed = world.getRandom().nextLong();
        NukeEffectsPayload payload = new NukeEffectsPayload(center, power, fxSeed);
        double fxRangeSq = Math.pow(radius * 6.0 + 64.0, 2);
        Vec3d centerVec = Vec3d.ofCenter(center);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(centerVec) <= fxRangeSq) {
                ServerPlayNetworking.send(player, payload);
            }
        }

        // 3) Terrain carving (respect mobGriefing). Spread across ticks.
        if (mayGrief) {
            scheduleCratering(world, center, radius, depth, cfg);
        }

        // 4) Optional fallout hazard cloud.
        if (cfg.enableFalloutDebuff) {
            spawnFallout(world, center, radius, cfg);
        }
    }

    // ---- Sounds -----------------------------------------------------------

    private static void playImpactSounds(ServerWorld world, BlockPos c) {
        // Primary boom, very loud so it carries well past vanilla range.
        world.playSound(null, c, ModSounds.DETONATION_BOOM, SoundCategory.BLOCKS, 8.0f, 0.6f);
        // Long aftermath rumble tail.
        world.playSound(null, c, ModSounds.AFTERMATH_RUMBLE, SoundCategory.AMBIENT, 6.0f, 0.8f);
    }

    // ---- Entity damage + knockback ---------------------------------------

    private static void applyEntityEffects(ServerWorld world, BlockPos center, int radius, float power) {
        Vec3d c = Vec3d.ofCenter(center);
        double damageRadius = radius * ExplosionConfig.DAMAGE_RADIUS_MULT;
        double damageRadiusSq = damageRadius * damageRadius;

        Box box = new Box(center).expand(damageRadius);
        for (Entity entity : world.getOtherEntities(null, box)) {
            double distSq = entity.squaredDistanceTo(c);
            if (distSq > damageRadiusSq) {
                continue;
            }
            double dist = Math.sqrt(distSq);
            double falloff = 1.0 - (dist / damageRadius); // 1 at center -> 0 at edge

            if (entity instanceof LivingEntity living) {
                float dmg = (float) (power * 4.0 * falloff);
                living.damage(world, world.getDamageSources().explosion(null, null), dmg);
            }

            // Knockback: outward horizontally + strong upward ("higher") impulse.
            Vec3d dir = entity.getEntityPos().subtract(c);
            double horiz = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
            double nx = horiz < 1.0e-4 ? 0 : dir.x / horiz;
            double nz = horiz < 1.0e-4 ? 0 : dir.z / horiz;

            double out = ExplosionConfig.MAX_OUTWARD_KNOCKBACK * falloff;
            double up = ExplosionConfig.MAX_UPWARD_KNOCKBACK * falloff;
            entity.addVelocity(nx * out, up, nz * out);
            entity.velocityModified = true;
        }
    }

    // ---- Cratering (tick-spread) -----------------------------------------

    /**
     * Build the list of columns to carve, then process them in batches at the
     * end of each server tick so a large radius doesn't hitch the server.
     */
    private static void scheduleCratering(ServerWorld world, BlockPos center,
                                          int radius, int maxDepth, NukeConfig cfg) {
        Deque<int[]> columns = new ArrayDeque<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.sqrt(dx * dx + dz * dz) <= radius) {
                    columns.add(new int[]{center.getX() + dx, center.getZ() + dz});
                }
            }
        }

        // Recursive end-of-tick task processing up to columnsPerTick per pass.
        world.getServer().execute(new Runnable() {
            @Override
            public void run() {
                int budget = cfg.columnsPerTick;
                while (budget-- > 0 && !columns.isEmpty()) {
                    int[] col = columns.poll();
                    carveColumn(world, center, col[0], col[1], radius, maxDepth, cfg);
                }
                if (!columns.isEmpty()) {
                    world.getServer().execute(this);
                }
            }
        });
    }

    /**
     * Carve one column. Depth follows a paraboloid: deepest at the center,
     * shallowing to zero at the horizontal radius, producing a wide bowl rather
     * than a spherical pocket. A small raised lip + debris is left at the rim.
     */
    private static void carveColumn(ServerWorld world, BlockPos center,
                                    int x, int z, int radius, int maxDepth, NukeConfig cfg) {
        int dx = x - center.getX();
        int dz = z - center.getZ();
        double rFrac = Math.sqrt(dx * dx + dz * dz) / radius; // 0 center -> 1 edge

        int depth = depthAt(Math.sqrt(dx * dx + dz * dz), radius, maxDepth);

        int topY = center.getY() + cfg.craterLip;
        int bottomY = center.getY() - depth;

        boolean core = rFrac <= ExplosionConfig.CORE_FRACTION;
        boolean destruction = rFrac <= ExplosionConfig.DESTRUCTION_FRACTION;
        boolean rim = rFrac > ExplosionConfig.DESTRUCTION_FRACTION
                && rFrac <= ExplosionConfig.SHOCKWAVE_FRACTION;

        Random random = world.getRandom();
        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int y = topY; y >= bottomY; y--) {
            if (y < world.getBottomY() || y > world.getTopYInclusive()) {
                continue;
            }
            pos.set(x, y, z);
            BlockState state = world.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            // Never punch through bedrock / indestructible blocks.
            // getHardness(...) returns -1 for unbreakable blocks like bedrock.
            if (state.getHardness(world, pos) < 0) {
                continue;
            }

            if (core) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
            } else if (destruction) {
                if (y == bottomY && random.nextDouble() < ExplosionConfig.SCORCH_CHANCE) {
                    world.setBlockState(pos, ModBlocks.SCORCHED_GROUND.getDefaultState(),
                            Block.NOTIFY_LISTENERS);
                } else {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                }
            } else if (rim && y == topY) {
                // Mostly spare rim blocks; scorch/debris the exposed surface for
                // a jagged edge.
                if (random.nextDouble() < ExplosionConfig.DEBRIS_CHANCE) {
                    world.setBlockState(pos, debrisBlock(random), Block.NOTIFY_LISTENERS);
                } else if (random.nextDouble() < ExplosionConfig.SCORCH_CHANCE) {
                    world.setBlockState(pos, ModBlocks.SCORCHED_GROUND.getDefaultState(),
                            Block.NOTIFY_LISTENERS);
                }
            }
        }

        // Fire scattering in the destruction ring on top of remaining ground.
        if (destruction && cfg.enableFire && random.nextDouble() < ExplosionConfig.FIRE_CHANCE) {
            placeFireOnSurface(world, x, z, center.getY() + cfg.craterLip + 2);
        }
    }

    private static BlockState debrisBlock(Random random) {
        return switch (random.nextInt(3)) {
            case 0 -> Blocks.MAGMA_BLOCK.getDefaultState();
            case 1 -> Blocks.COBBLED_DEEPSLATE.getDefaultState();
            default -> Blocks.BLACKSTONE.getDefaultState();
        };
    }

    private static void placeFireOnSurface(ServerWorld world, int x, int z, int startY) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int y = startY; y > world.getBottomY(); y--) {
            pos.set(x, y, z);
            if (world.getBlockState(pos).isAir() && world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
                world.setBlockState(pos, Blocks.FIRE.getDefaultState(), Block.NOTIFY_LISTENERS);
                return;
            }
        }
    }

// ---- Fallout ----------------------------------------------------------

    private static void spawnFallout(ServerWorld world, BlockPos center, int radius, NukeConfig cfg) {
        AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(world, center.getX() + 0.5,
                center.getY() + 1.0, center.getZ() + 0.5);
        cloud.setRadius(radius * 0.8f);
        cloud.setRadiusOnUse(0.0f);
        cloud.setWaitTime(0);
        cloud.setDuration(cfg.falloutDurationTicks);
        cloud.setRadiusGrowth(-(radius * 0.8f) / Math.max(1, cfg.falloutDurationTicks));
        cloud.addEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 0));
        cloud.addEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 120, 0));
        world.spawnEntity(cloud);
    }

    /** Paraboloid depth at a given horizontal radius {@code r}. */
    public static int depthAt(double r, int radius, int maxDepth) {
        double rFrac = MathHelper.clamp(r / radius, 0.0, 1.0);
        int d = (int) Math.round(maxDepth * (1.0 - rFrac * rFrac));
        return Math.max(0, d);
    }
}
