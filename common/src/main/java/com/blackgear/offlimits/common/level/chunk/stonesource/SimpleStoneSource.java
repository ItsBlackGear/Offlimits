package com.blackgear.offlimits.common.level.chunk.stonesource;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class SimpleStoneSource implements BaseStoneSource {
    private final BlockState baseBlock;
    
    public SimpleStoneSource(Block baseBlock) {
        this.baseBlock = baseBlock.defaultBlockState();
    }
    
    public SimpleStoneSource(BlockState baseBlock) {
        this.baseBlock = baseBlock;
    }
    
    @Override
    public BlockState getBaseBlock(int x, int y, int z) {
        return this.baseBlock;
    }
}
