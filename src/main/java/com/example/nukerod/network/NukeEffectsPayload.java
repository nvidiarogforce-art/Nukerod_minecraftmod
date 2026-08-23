package com.example.nukerod.network;

import com.example.nukerod.NukeRodMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

/**
 * S2C payload telling clients to play the full detonation FX suite locally.
 *
 * <p>Instead of the server spamming hundreds of individual particle/sound
 * packets, it sends one of these. The client reconstructs every particle,
 * sound and the screen-shake deterministically from {@code impactPos},
 * {@code power} and {@code fxSeed}.
 *
 * @param impactPos block position of the detonation epicenter
 * @param power     blast power / radius scalar (drives FX magnitude)
 * @param fxSeed    seed so all clients draw an identical randomized effect
 */
public record NukeEffectsPayload(BlockPos impactPos, float power, long fxSeed)
        implements CustomPayload {

    public static final CustomPayload.Id<NukeEffectsPayload> ID =
            new CustomPayload.Id<>(NukeRodMod.id("nuke_effects"));

    public static final PacketCodec<RegistryByteBuf, NukeEffectsPayload> CODEC =
            PacketCodec.tuple(
                    BlockPos.PACKET_CODEC, NukeEffectsPayload::impactPos,
                    PacketCodecs.FLOAT, NukeEffectsPayload::power,
                    PacketCodecs.VAR_LONG, NukeEffectsPayload::fxSeed,
                    NukeEffectsPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
