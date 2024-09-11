package com.blackgear.offlimits.common.level.chunk.carver;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.common.level.chunk.Aquifer;
import com.blackgear.offlimits.common.level.chunk.stonesource.SimpleStoneSource;
import com.blackgear.offlimits.common.level.chunk.surface.WorldCarverExtension;
import com.blackgear.offlimits.core.mixin.common.access.WorldCarverAccessor;
import com.blackgear.platform.common.worldgen.WorldGenerationContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.BitSet;
import java.util.Random;
import java.util.function.Function;

public class NoiseCarver {
    private final WorldCarver<?> carver;
    
    public NoiseCarver(WorldCarver<?> carver) {
        this.carver = carver;
    }
    
    public boolean carveSphere(
        ChunkAccess chunk,
        Function<BlockPos, Biome> biomeGetter,
        long seed,
        int seaLevel,
        int chunkX,
        int chunkZ,
        double x,
        double y,
        double z,
        double horizontalRadius,
        double verticalRadius,
        BitSet carvingMask
    ) {
        WorldCarverExtension extension = (WorldCarverExtension) this.carver;
        WorldCarverAccessor access = (WorldCarverAccessor) this.carver;
        WorldGenerationContext context = extension.context();
        
        Random random = new Random(seed + (long)chunkX + (long)chunkZ);
        double centerX = chunkX * 16 + 8;
        double centerZ = chunkZ * 16 + 8;
        double maxRadius = 16.0 - horizontalRadius * 2.0;
        
        if (x >= centerX - maxRadius && z >= centerZ - maxRadius && x <= centerX + maxRadius && z <= centerZ + maxRadius) {
            int startX = Math.max(Mth.floor(x - horizontalRadius) - chunkX * 16 - 1, 0);
            int endX = Math.min(Mth.floor(x + horizontalRadius) - chunkX * 16 + 1, 16);
            int startY = Math.max(Mth.floor(y - verticalRadius) - 1, context.getMinGenY() + 1);
            int endY = Math.min(Mth.floor(y + verticalRadius) + 1, context.getMinGenY() + context.getGenDepth() - 8);
            int startZ = Math.max(Mth.floor(z - horizontalRadius) - chunkZ * 16 - 1, 0);
            int endZ = Math.min(Mth.floor(z + horizontalRadius) - chunkZ * 16 + 1, 16);
            
            if (!Offlimits.CONFIG.areAquifersEnabled.get() && access.callHasWater(chunk, chunkX, chunkZ, startX, endX, startY, endY, startZ, endZ)) {
//            if (!context.areAquifersEnabled() && access.callHasWater(chunk, chunkX, chunkZ, startX, endX, startY, endY, startZ, endZ)) {
                return false;
            } else {
                boolean carved = false;
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
                BlockPos.MutableBlockPos mutableBlockPos3 = new BlockPos.MutableBlockPos();
                
                for(int localX = startX; localX < endX; localX++) {
                    int blockX = localX + chunkX * 16;
                    double factorX = ((double)blockX + 0.5 - x) / horizontalRadius;
                    
                    for(int localZ = startZ; localZ < endZ; localZ++) {
                        int blockZ = localZ + chunkZ * 16;
                        double factorZ = ((double)blockZ + 0.5 - z) / horizontalRadius;
                        
                        if (factorX * factorX + factorZ * factorZ < 1.0) {
                            MutableBoolean reachedSurface = new MutableBoolean(false);
                            
                            for(int localY = endY; localY > startY; localY--) {
                                double factorY = ((double)localY - 0.5 - y) / verticalRadius;
                                
                                if (!access.callSkip(factorX, factorY, factorZ, localY)) {
                                    carved |= access.callCarveBlock(
                                        chunk,
                                        biomeGetter,
                                        carvingMask,
                                        random,
                                        pos,
                                        checkPos,
                                        mutableBlockPos3,
                                        seaLevel,
                                        chunkX,
                                        chunkZ,
                                        blockX,
                                        blockZ,
                                        localX,
                                        localY,
                                        localZ,
                                        reachedSurface
                                    );
                                }
                            }
                        }
                    }
                }
                
                return carved;
            }
        } else {
            return false;
        }
    }
    
    public boolean carveBlock(
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
    ) {
        WorldCarverExtension extension = (WorldCarverExtension) this.carver;
        WorldCarverAccessor access = (WorldCarverAccessor) this.carver;
        
        int realY = minBlockX - extension.context().getMinGenY();
        int mask = endZ | minBlockZ << 4 | realY << 8;
        if (carvingMask.get(mask)) {
            return false;
        } else {
            carvingMask.set(mask);
            pos.set(endY, minBlockX, startZ);
            
            BlockState current = chunk.getBlockState(pos);
            BlockState aboveState = chunk.getBlockState(checkPos.setWithOffset(pos, Direction.UP));
            
            if (current.is(Blocks.GRASS_BLOCK) || current.is(Blocks.MYCELIUM)) {
                reachedSurface.setTrue();
            }
            
            if (!access.callCanReplaceBlock(current, aboveState)) {
                return false;
            } else {
                BlockState carveState = this.getCarveState(pos, extension.aquifer());
                if (carveState == null) {
                    return false;
                } else {
                    chunk.setBlockState(pos, carveState, false);
                    if (extension.aquifer().shouldScheduleFluidUpdate() && !carveState.getFluidState().isEmpty()) {
                        chunk.getLiquidTicks().scheduleTick(pos, carveState.getFluidState().getType(), 0);
                    }
                    
                    if (reachedSurface.isTrue()) {
                        replaceSurface.setWithOffset(pos, Direction.DOWN);
                        if (chunk.getBlockState(replaceSurface).is(Blocks.DIRT)) {
                            chunk.setBlockState(replaceSurface, biomeGetter.apply(pos).getGenerationSettings().getSurfaceBuilderConfig().getTopMaterial(), false);
                        }
                    }
                }
                
                return true;
            }
        }
    }
    
    private BlockState getCarveState(BlockPos pos, Aquifer aquifer) {
        WorldCarverExtension extension = (WorldCarverExtension) this.carver;
        
        if (pos.getY() <= extension.context().getMinGenY() + 9) {
            return Blocks.LAVA.defaultBlockState().getFluidState().createLegacyBlock();
        } else if (!Offlimits.CONFIG.areAquifersEnabled.get()) {
//        } else if (!extension.context().terrain().areAquifersEnabled()) {
            return Blocks.AIR.defaultBlockState();
        } else {
            BlockState state = aquifer.computeState(new SimpleStoneSource(Blocks.STONE), pos.getX(), pos.getY(), pos.getZ(), 0.0);
            return state == Blocks.STONE.defaultBlockState() ? null : state;
        }
    }
}