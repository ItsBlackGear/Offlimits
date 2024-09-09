package com.blackgear.offlimits.common.level.chunk.stonesource;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface BaseStoneSource {
    default BlockState getBaseBlock(BlockPos pos) {
        return this.getBaseBlock(pos.getX(), pos.getY(), pos.getZ());
    }
    
    BlockState getBaseBlock(int x, int y, int z);
}