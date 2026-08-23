package com.example.nukerod.registry;

import com.example.nukerod.NukeRodMod;
import com.example.nukerod.item.NukeRodItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.util.Rarity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;

/**
 * Item registration: the {@link NukeRodItem} and its {@code warhead_core} ammo.
 */
public final class ModItems {
    private ModItems() {}

    public static Item NUKE_ROD;
    public static Item WARHEAD_CORE;

    private static RegistryKey<Item> key(String path) {
        return RegistryKey.of(RegistryKeys.ITEM, NukeRodMod.id(path));
    }

    private static Item register(String path, Function<Item.Settings, Item> factory,
                                 Item.Settings settings) {
        Identifier id = NukeRodMod.id(path);
        Item item = factory.apply(settings.registryKey(key(path)));
        return Registry.register(Registries.ITEM, id, item);
    }

    public static void register() {
        NUKE_ROD = register("nuke_rod", NukeRodItem::new,
                new Item.Settings().maxDamage(64).rarity(Rarity.EPIC));
        WARHEAD_CORE = register("warhead_core", Item::new,
                new Item.Settings().maxCount(16).rarity(Rarity.RARE));

        // Add both to the Tools & Utilities creative tab.
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(NUKE_ROD);
            entries.add(WARHEAD_CORE);
        });

        NukeRodMod.LOGGER.info("[nukerod] Registered items");
    }
}
