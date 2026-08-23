package com.example.nukerod;

import com.example.nukerod.client.NukeFxHandler;
import com.example.nukerod.client.NukeHud;
import com.example.nukerod.client.NukeScreenShake;
import com.example.nukerod.client.NukeZoom;
import com.example.nukerod.entity.ModEntities;
import com.example.nukerod.network.NukeEffectsPayload;
import com.example.nukerod.particle.ModParticles;
import com.example.nukerod.render.NukeWarheadRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import com.example.nukerod.client.NukeParticleFactory;

/**
 * Client initializer: entity renderer binding, the S2C FX receiver, and the
 * per-frame screen-shake tick.
 */
public class NukeRodClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Custom warhead renderer.
        EntityRendererRegistry.register(ModEntities.NUKE_WARHEAD, NukeWarheadRenderer::new);

        // Receive the single detonation FX packet and play everything locally.
        ClientPlayNetworking.registerGlobalReceiver(NukeEffectsPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        NukeFxHandler.play(context.client(), payload.impactPos(),
                                payload.power(), payload.fxSeed())));

        // Bind client factories for the custom particle types. A registered
        // ParticleType with no client factory crashes the client at load, so
        // every type in ModParticles MUST be bound here. They all share a
        // simple sprite-billboard factory (NukeParticle) driven by the sprite
        // sets declared in assets/nukerod/particles/*.json.
        ParticleFactoryRegistry reg = ParticleFactoryRegistry.getInstance();
        reg.register(ModParticles.MUSHROOM_SMOKE, NukeParticleFactory::new);
        reg.register(ModParticles.SHOCKWAVE_RING, NukeParticleFactory::new);
        reg.register(ModParticles.ASH_FALLOUT, NukeParticleFactory::new);
        reg.register(ModParticles.FIREBALL_CORE, NukeParticleFactory::new);

        // Drive the screen-shake decay every client tick.
        NukeScreenShake.init();

        // Spyglass-style zoom keybinding + FOV state.
        NukeZoom.init();

        // Remaining-warhead HUD counter (shown while holding the rod).
        NukeHud.register();

        NukeRodMod.LOGGER.info("[nukerod] Client initialized");
    }
}
