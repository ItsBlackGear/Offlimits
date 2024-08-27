package com.blackgear.offlimits.common.level.noise;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public class BlockNoiseColumn implements BlockGetter {
    private final int minY;
    private final BlockState[] column;
    
    public BlockNoiseColumn(int minY, BlockState[] column) {
        this.minY = minY;
        this.column = column;
    }
    
    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }
    
    @Override
    public BlockState getBlockState(BlockPos pos) {
        int i = pos.getY() - this.minY;
        return i >= 0 && i < this.column.length ? this.column[i] : Blocks.AIR.defaultBlockState();
    }
    
    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.getBlockState(pos).getFluidState();
    }
}