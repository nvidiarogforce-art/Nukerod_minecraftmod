package com.example.nukerod;

import com.example.nukerod.block.ModBlocks;
import com.example.nukerod.config.NukeConfig;
import com.example.nukerod.entity.ModEntities;
import com.example.nukerod.item.NukeRodItem;
import com.example.nukerod.network.FireRodPayload;
import com.example.nukerod.network.NukeEffectsPayload;
import com.example.nukerod.particle.ModParticles;
import com.example.nukerod.registry.ModItems;
import com.example.nukerod.sound.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main (common) mod initializer for the Nuke Fishing Rod mod.
 *
 * <p>Runs on both client and dedicated server. All gameplay-authoritative
 * registration (items, entities, blocks, sounds, particles, config load and
 * the S2C effects payload) happens here. Client-only rendering/particle/sound
 * playback is wired in {@link NukeRodClient}.
 */
public class NukeRodMod implements ModInitializer {
    public static final String MOD_ID = "nukerod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Convenience for building resource identifiers in the mod namespace. */
    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("[nukerod] Initializing Nuke Fishing Rod...");

        // Config first so downstream registration can read tunables if needed.
        NukeConfig.load();

        // Registries. Order matters loosely: blocks/particles/sounds before the
        // item so item tooltips / entities can reference them.
        ModSounds.register();
        ModParticles.register();
        ModBlocks.register();
        ModEntities.register();
        ModItems.register();

        // Networking: register the S2C payload type on the common side so both
        // client and server agree on the codec.
        PayloadTypeRegistry.playS2C().register(NukeEffectsPayload.ID, NukeEffectsPayload.CODEC);

        // C2S fire request (sent when the player left-clicks with the rod).
        PayloadTypeRegistry.playC2S().register(FireRodPayload.ID, FireRodPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(FireRodPayload.ID, (payload, context) ->
                context.server().execute(() ->
                        NukeRodItem.serverFire(context.player())));

        LOGGER.info("[nukerod] Initialization complete.");
    }
}
