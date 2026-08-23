package com.example.nukerod.explosion;

/**
 * Derived, tunable ring geometry for the explosion, expressed as fractions of
 * the configured horizontal radius. Kept separate from {@code NukeConfig} so the
 * ring *shape* can be tuned independently of the user-facing size settings.
 */
public final class ExplosionConfig {
    private ExplosionConfig() {}

    /** Core vaporization: everything inside this fraction of R is set to air. */
    public static final double CORE_FRACTION = 0.35;

    /** Heavy destruction ring outer edge (fraction of R). */
    public static final double DESTRUCTION_FRACTION = 0.70;

    /** Shockwave/knockback ring outer edge (fraction of R). Blocks mostly spared. */
    public static final double SHOCKWAVE_FRACTION = 1.00;

    /** Entity damage reaches this multiple of R (soft falloff beyond ring). */
    public static final double DAMAGE_RADIUS_MULT = 1.15;

    /** Chance a block in the destruction ring becomes scorched rather than air. */
    public static final double SCORCH_CHANCE = 0.45;

    /** Chance a flammable block in the destruction ring is set on fire. */
    public static final double FIRE_CHANCE = 0.20;

    /** Chance a rim column gets a debris block (deepslate/magma). */
    public static final double DEBRIS_CHANCE = 0.15;

    /** Peak upward knockback velocity applied at the epicenter. */
    public static final double MAX_UPWARD_KNOCKBACK = 2.6;

    /** Peak outward (horizontal) knockback velocity at the epicenter. */
    public static final double MAX_OUTWARD_KNOCKBACK = 3.2;
}
