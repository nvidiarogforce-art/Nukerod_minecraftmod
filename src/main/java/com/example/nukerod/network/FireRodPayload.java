package com.example.nukerod.network;

import com.example.nukerod.NukeRodMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Empty C2S payload: the client sends this when the player left-clicks while
 * holding the Nuke Rod, asking the server to fire (or detonate) the warhead.
 * Carries no data — the server reads the sending player's aim directly.
 */
public record FireRodPayload() implements CustomPayload {

    public static final CustomPayload.Id<FireRodPayload> ID =
            new CustomPayload.Id<>(NukeRodMod.id("fire_rod"));

    public static final PacketCodec<RegistryByteBuf, FireRodPayload> CODEC =
            PacketCodec.unit(new FireRodPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
