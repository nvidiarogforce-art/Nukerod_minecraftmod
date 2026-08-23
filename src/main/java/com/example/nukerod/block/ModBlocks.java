package com.example.nukerod.block;

import com.example.nukerod.NukeRodMod;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * Block registration. Currently just {@link ScorchedGroundBlock}, plus its
 * matching {@link BlockItem} so it can be given / creative-picked.
 */
public final class ModBlocks {
    private ModBlocks() {}

    public static Block SCORCHED_GROUND;

    private static RegistryKey<Block> blockKey(String path) {
        return RegistryKey.of(RegistryKeys.BLOCK, NukeRodMod.id(path));
    }

    private static RegistryKey<Item> itemKey(String path) {
        return RegistryKey.of(RegistryKeys.ITEM, NukeRodMod.id(path));
    }

    public static void register() {
        Identifier id = NukeRodMod.id("scorched_ground");

        AbstractBlock.Settings settings = AbstractBlock.Settings.create()
                .mapColor(MapColor.BLACK)
                .strength(1.2f, 6.0f)
                .requiresTool()
                .sounds(BlockSoundGroup.BASALT)
                .registryKey(blockKey("scorched_ground"));

        SCORCHED_GROUND = Registry.register(Registries.BLOCK, id,
                new ScorchedGroundBlock(settings));

        Registry.register(Registries.ITEM, id,
                new BlockItem(SCORCHED_GROUND,
                        new Item.Settings().registryKey(itemKey("scorched_ground"))));

        NukeRodMod.LOGGER.info("[nukerod] Registered blocks");
    }
}
