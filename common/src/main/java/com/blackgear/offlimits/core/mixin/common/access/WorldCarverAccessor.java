package com.blackgear.offlimits.core.mixin.common.access;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.BitSet;
import java.util.Random;
import java.util.function.Function;

@Mixin(WorldCarver.class)
public interface WorldCarverAccessor {
    @Invoker
    boolean callHasWater(ChunkAccess chunk, int chunkX, int chunkZ, int startX, int endX, int startY, int endY, int startZ, int endZ);
    
    @Invoker
    boolean callSkip(double x, double y, double z, int localY);
    
    @Invoker
    boolean callCanReplaceBlock(BlockState state, BlockState aboveState);
    
    @Invoker
    boolean callCarveBlock(
        ChunkAccess chunk,
        Function<BlockPos, Biome> biomeGetter,
        BitSet carvingMask,
        Random random,
        BlockPos.MutableBlockPos pos,
        BlockPos.MutableBlockPos checkPos,
        BlockPos.MutableBlockPos replaceSurface,
        int startX,
        int endX,
        int startY,
        int endY,
        int startZ,
        int endZ,
        int minBlockX,
        int minBlockZ,
        MutableBoolean reachedSurface
    );
}
