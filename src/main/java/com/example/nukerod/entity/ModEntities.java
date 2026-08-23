package com.example.nukerod.entity;

import com.example.nukerod.NukeRodMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

/**
 * Entity type registration for {@link NukeWarheadEntity}.
 */
public final class ModEntities {
    private ModEntities() {}

    public static EntityType<NukeWarheadEntity> NUKE_WARHEAD;

    public static void register() {
        RegistryKey<EntityType<?>> key =
                RegistryKey.of(RegistryKeys.ENTITY_TYPE, NukeRodMod.id("nuke_warhead"));

        NUKE_WARHEAD = Registry.register(
                Registries.ENTITY_TYPE,
                key,
                EntityType.Builder.<NukeWarheadEntity>create(NukeWarheadEntity::new, SpawnGroup.MISC)
                        .dimensions(0.5f, 0.5f)
                        .maxTrackingRange(128)
                        .trackingTickInterval(1)
                        .build(key)
        );

        NukeRodMod.LOGGER.info("[nukerod] Registered entities");
    }
}
