package com.example.nukerod.config;

import com.example.nukerod.NukeRodMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Hand-rolled JSON config stored at {@code .minecraft/config/nukerod.json}.
 *
 * <p>Kept dependency-free (plain Gson, no Cloth/AutoConfig) so the mod has no
 * extra runtime deps. All fields are simple public primitives that the
 * explosion / item logic reads directly through the {@link #get()} singleton.
 */
public class NukeConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static NukeConfig INSTANCE = new NukeConfig();

    // ---- Explosion shape --------------------------------------------------
    /** Horizontal crater radius in blocks. Capped by {@link #MAX_RADIUS}. */
    public int explosionRadius = 24;
    /** Maximum crater depth at the epicenter, in blocks. */
    public int explosionDepth = 18;
    /** Extra raised lip height above the surface at the rim. */
    public int craterLip = 2;
    /** Total blast "power" used for entity damage / knockback scaling. */
    public float power = 12.0f;

    // ---- Item behavior ----------------------------------------------------
    /** Cooldown after a detonation, in ticks (20 ticks = 1 second). */
    public int cooldownTicks = 40 * 20;
    /** If true, each strike consumes one warhead-core item as ammo. */
    public boolean requiresWarheadCore = true;
    /** Durability damage dealt to the rod per launch. */
    public int durabilityPerUse = 5;

    // ---- Optional systems -------------------------------------------------
    /** Spawn a lingering fallout AreaEffectCloud over the crater. */
    public boolean enableFalloutDebuff = true;
    /** Duration of the fallout cloud in ticks. */
    public int falloutDurationTicks = 30 * 20;
    /** Scorched ground slowly reverts to normal terrain via random ticks. */
    public boolean enableTerrainHealing = false;
    /** Scatter short-lived fire in the destruction ring. */
    public boolean enableFire = true;
    /** How many ticks fire is allowed to persist before auto-extinguish. */
    public int fireLifetimeTicks = 12 * 20;

    // ---- Performance ------------------------------------------------------
    /** Hard cap on horizontal radius regardless of config, protects servers. */
    public static final int MAX_RADIUS = 60;
    /** Hard cap on depth. */
    public static final int MAX_DEPTH = 35;
    /** Max block columns processed per tick when tick-spreading large blasts. */
    public int columnsPerTick = 512;

    public static NukeConfig get() {
        return INSTANCE;
    }

    /** Effective, clamped horizontal radius. */
    public int effectiveRadius() {
        return Math.max(1, Math.min(explosionRadius, MAX_RADIUS));
    }

    /** Effective, clamped depth. */
    public int effectiveDepth() {
        return Math.max(1, Math.min(explosionDepth, MAX_DEPTH));
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("nukerod.json");
    }

    /** Loads the config from disk, creating a default file if none exists. */
    public static void load() {
        Path path = configPath();
        try {
            if (Files.exists(path)) {
                String json = Files.readString(path);
                NukeConfig loaded = GSON.fromJson(json, NukeConfig.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                }
                // Re-save to add any newly-introduced fields with defaults.
                save();
            } else {
                save();
            }
        } catch (IOException | RuntimeException e) {
            NukeRodMod.LOGGER.error("[nukerod] Failed to load config, using defaults", e);
            INSTANCE = new NukeConfig();
        }
    }

    /** Persists the current config instance to disk. */
    public static void save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            NukeRodMod.LOGGER.error("[nukerod] Failed to save config", e);
        }
    }
}
