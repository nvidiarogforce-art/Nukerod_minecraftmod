package com.example.nukerod.sound;

import com.example.nukerod.NukeRodMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Custom sound events. The referenced {@code .ogg} files must be supplied as
 * binary assets under {@code assets/nukerod/sounds/} and mapped in
 * {@code sounds.json}. Until real audio is dropped in, {@code sounds.json}
 * points these at existing vanilla sounds as placeholders so the mod still
 * plays audibly.
 */
public final class ModSounds {
    private ModSounds() {}

    public static SoundEvent CHARGE_HUM;
    public static SoundEvent WARHEAD_LAUNCH;
    public static SoundEvent FALLING_WHISTLE;
    public static SoundEvent DETONATION_BOOM;
    public static SoundEvent AFTERMATH_RUMBLE;

    private static SoundEvent create(String path) {
        Identifier id = NukeRodMod.id(path);
        SoundEvent event = SoundEvent.of(id);
        return Registry.register(Registries.SOUND_EVENT, id, event);
    }

    public static void register() {
        CHARGE_HUM = create("charge_hum");
        WARHEAD_LAUNCH = create("warhead_launch");
        FALLING_WHISTLE = create("falling_whistle");
        DETONATION_BOOM = create("detonation_boom");
        AFTERMATH_RUMBLE = create("aftermath_rumble");
        NukeRodMod.LOGGER.info("[nukerod] Registered sounds");
    }
}
