package com.example.nukerod.block;

import com.example.nukerod.config.NukeConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * A charred terrain block placed at the crater rim and destruction ring.
 *
 * <p>If {@link NukeConfig#enableTerrainHealing} is on, the block slowly reverts
 * to dirt/stone via random ticks, letting craters "heal" over real time. It is
 * otherwise an inert cosmetic block.
 */
public class ScorchedGroundBlock extends Block {

    public ScorchedGroundBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected boolean hasRandomTicks(BlockState state) {
        return NukeConfig.get().enableTerrainHealing;
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!NukeConfig.get().enableTerrainHealing) {
            return;
        }
        // Low per-tick chance so healing is gradual.
        if (random.nextInt(12) != 0) {
            return;
        }
        // Heal toward the block below: if there's stone below revert to stone,
        // otherwise dirt. Kept simple and cheap.
        BlockState below = world.getBlockState(pos.down());
        BlockState healed = below.isOf(Blocks.STONE) || below.isOf(Blocks.DEEPSLATE)
                ? Blocks.STONE.getDefaultState()
                : Blocks.DIRT.getDefaultState();
        world.setBlockState(pos, healed);
    }
}
