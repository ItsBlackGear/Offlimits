package com.blackgear.offlimits.core.mixin.common.level.carver;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.common.level.Aquifer;
import com.blackgear.offlimits.common.level.levelgen.stonesource.SimpleStoneSource;
import com.blackgear.offlimits.common.level.surface.WorldCarverExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.BitSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

@Mixin(WorldCarver.class)
public abstract class WorldCarverMixin implements WorldCarverExtension {
    @Shadow @Final protected static FluidState LAVA;
    @Shadow @Final protected static BlockState AIR;
    
    @Shadow protected abstract boolean canReplaceBlock(BlockState state, BlockState aboveState);
    @Shadow protected abstract boolean carveBlock(ChunkAccess chunkAccess, Function<BlockPos, Biome> function, BitSet bitSet, Random random, BlockPos.MutableBlockPos mutableBlockPos, BlockPos.MutableBlockPos mutableBlockPos2, BlockPos.MutableBlockPos mutableBlockPos3, int i, int j, int k, int l, int m, int n, int o, int p, MutableBoolean mutableBoolean);
    @Shadow protected abstract boolean skip(double d, double e, double f, int i);
    @Shadow protected abstract boolean hasWater(ChunkAccess chunkAccess, int i, int j, int k, int l, int m, int n, int o, int p);
    
    @Shadow @Final protected int genHeight;
    
    @Shadow protected Set<Fluid> liquids;
    
    @Shadow protected abstract boolean isEdge(int minX, int maxX, int minZ, int maxZ, int x, int z);
    
    @Unique private Aquifer aquifer;
    
    @Override
    public Aquifer getAquifer() {
        return this.aquifer;
    }
    
    @Override
    public void setAquifer(Aquifer aquifer) {
        this.aquifer = aquifer;
    }
    
    @Inject(
        method = "carveSphere",
        at = @At("HEAD"),
        cancellable = true
    )
    private void offlimits$carveSphere(
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
        BitSet carvingMask,
        CallbackInfoReturnable<Boolean> callback
    ) {
        if (Offlimits.CONFIG.allowTerrainModifications.get()) {
            ChunkPos chunkPos = chunk.getPos();
            Random random = new Random(seed + (long) chunkX + (long) chunkZ);
            double centerX = (double) SectionPos.sectionToBlockCoord(chunkX) + 8;
            double centerZ = (double) SectionPos.sectionToBlockCoord(chunkZ) + 8;
            double maxRadius = 16.0 + horizontalRadius * 2.0;

            if (Math.abs(x - centerX) <= maxRadius && Math.abs(z - centerZ) <= maxRadius) {
                int minBlockX = chunkPos.getMinBlockX();
                int minBlockZ = chunkPos.getMinBlockZ();
                int startX = Math.max(Mth.floor(x - horizontalRadius) - minBlockX - 1, 0);
                int endX = Math.min(Mth.floor(x + horizontalRadius) - minBlockX, 15);
                int startY = Math.max(Mth.floor(y - verticalRadius) - 1, Offlimits.LEVEL.getMinBuildHeight() + 1);
                int endY = Math.min(Mth.floor(y + verticalRadius) + 1, Offlimits.LEVEL.getMinBuildHeight() + this.genHeight - 8);
                int startZ = Math.max(Mth.floor(z - horizontalRadius) - minBlockZ - 1, 0);
                int endZ = Math.min(Mth.floor(z + horizontalRadius) - minBlockZ, 15);
                
                if (!Offlimits.CONFIG.areAquifersEnabled.get() && this.hasDisallowedLiquid(chunk, startX, endX, startY, endY, startZ, endZ)) {
                    callback.setReturnValue(false);
                } else {
                    boolean carved = false;
                    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                    BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

                    for (int localX = startX; localX <= endX; localX++) {
                        int blockX = SectionPos.sectionToBlockCoord(chunkX) + localX;
                        double factorX = ((double) blockX + 0.5 - x) / horizontalRadius;

                        for (int localZ = startZ; localZ <= endZ; localZ++) {
                            int blockZ = SectionPos.sectionToBlockCoord(chunkZ) + localZ;
                            double factorZ = ((double) blockZ + 0.5 - z) / horizontalRadius;
                            
                            if (factorX * factorX + factorZ * factorZ < 1.0) {
                                MutableBoolean mutableBoolean = new MutableBoolean(false);

                                for (int localY = endY; localY > startY; localY--) {
                                    double factorY = ((double) localY - 0.5 - y) / verticalRadius;
                                    
                                    if (!this.skip(factorX, factorY, factorZ, localY)) {
                                        int realY = localY - Offlimits.LEVEL.getMinBuildHeight();
                                        int index = localX | localZ << 4 | realY << 8;
                                        
                                        if (!carvingMask.get(index)) {
                                            carvingMask.set(index);
                                            pos.set(blockX, localY, blockZ);
                                            carved |= this.carveBlock(chunk, biomeGetter, carvingMask, random, pos, checkPos, pos, startX, endX, startY, endY, startZ, endZ, minBlockX, minBlockZ, mutableBoolean);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    callback.setReturnValue(carved);
                }
            } else {
                callback.setReturnValue(false);
            }
        }
    }
    
    @Inject(
        method = "carveBlock",
        at = @At("HEAD"),
        cancellable = true
    )
    private void offlimits$carveBlock(
        ChunkAccess chunkAccess,
        Function<BlockPos, Biome> function,
        BitSet bitSet,
        Random random,
        BlockPos.MutableBlockPos mutableBlockPos,
        BlockPos.MutableBlockPos mutableBlockPos2,
        BlockPos.MutableBlockPos mutableBlockPos3,
        int i,
        int j,
        int k,
        int l,
        int m,
        int n,
        int o,
        int p,
        MutableBoolean mutableBoolean,
//        ChunkAccess chunk,
//        Function<BlockPos, Biome> biomeGetter,
//        BitSet carvingMask,
//        Random random,
//        BlockPos.MutableBlockPos pos,
//        BlockPos.MutableBlockPos checkPos,
//        BlockPos.MutableBlockPos unused,
//        int startX,
//        int endX,
//        int startY,
//        int endY,
//        int startZ,
//        int endZ,
//        int minBlockX,
//        int minBlockZ,
//        MutableBoolean reachedSurface,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (Offlimits.CONFIG.allowTerrainModifications.get()) {
            BlockState state = chunkAccess.getBlockState(mutableBlockPos);
            BlockState blockState2 = chunkAccess.getBlockState(mutableBlockPos2.setWithOffset(mutableBlockPos, Direction.UP));
            if (state.is(Blocks.GRASS_BLOCK) || blockState2.is(Blocks.MYCELIUM)) {
                mutableBoolean.setTrue();
            }
            
            if (!this.canReplaceBlock(state, blockState2)) {
                cir.setReturnValue(false);
            } else {
                BlockState carveState = this.getCarveState(mutableBlockPos, this.aquifer);
                
                if (carveState == null) {
                    cir.setReturnValue(false);
                } else {
                    chunkAccess.setBlockState(mutableBlockPos, carveState, false);
                    
                    if (this.aquifer.shouldScheduleFluidUpdate() && !carveState.getFluidState().isEmpty()) {
                        chunkAccess.getLiquidTicks().scheduleTick(mutableBlockPos, carveState.getFluidState().getType(), 0);
                    }
                    
                    if (mutableBoolean.isTrue()) {
                        mutableBlockPos2.setWithOffset(mutableBlockPos, Direction.DOWN);
                        
                        if (chunkAccess.getBlockState(mutableBlockPos2).is(Blocks.DIRT)) {
                            chunkAccess.setBlockState(mutableBlockPos2, function.apply(mutableBlockPos).getGenerationSettings().getSurfaceBuilderConfig().getTopMaterial(), false);
                        }
                    }
                    
                    cir.setReturnValue(true);
                }
            }
        } else {
            int q = n | p << 4 | o << 8;
            if (bitSet.get(q)) {
                cir.setReturnValue(false);
            } else {
                bitSet.set(q);
                mutableBlockPos.set(l, o, m);
                BlockState blockState = chunkAccess.getBlockState(mutableBlockPos);
                BlockState blockState2 = chunkAccess.getBlockState(mutableBlockPos2.setWithOffset(mutableBlockPos, Direction.UP));
                if (blockState.is(Blocks.GRASS_BLOCK) || blockState.is(Blocks.MYCELIUM)) {
                    mutableBoolean.setTrue();
                }
                
                if (!this.canReplaceBlock(blockState, blockState2)) {
                    cir.setReturnValue(false);
                } else {
                    if (o < Offlimits.CONFIG.worldGenMinY.get() + 11) {
                        chunkAccess.setBlockState(mutableBlockPos, LAVA.createLegacyBlock(), false);
                    } else {
                        chunkAccess.setBlockState(mutableBlockPos, Blocks.CAVE_AIR.defaultBlockState(), false);
                        if (mutableBoolean.isTrue()) {
                            mutableBlockPos3.setWithOffset(mutableBlockPos, Direction.DOWN);
                            if (chunkAccess.getBlockState(mutableBlockPos3).is(Blocks.DIRT)) {
                                chunkAccess.setBlockState(mutableBlockPos3, function.apply(mutableBlockPos).getGenerationSettings().getSurfaceBuilderConfig().getTopMaterial(), false);
                            }
                        }
                    }
                    
                    cir.setReturnValue(true);
                }
            }
        }
    }
    
    @Unique
    private BlockState getCarveState(BlockPos pos, Aquifer aquifer) {
        if (pos.getY() <= Offlimits.CONFIG.worldGenMinY.get() + 9) {
            return LAVA.createLegacyBlock();
        } else if (!Offlimits.CONFIG.areAquifersEnabled.get()) {
            return AIR;
        } else {
            BlockState state = aquifer.computeState(new SimpleStoneSource(Blocks.STONE), pos.getX(), pos.getY(), pos.getZ(), 0.0);
            return state == Blocks.STONE.defaultBlockState() ? null : state;
        }
    }
    
    @Unique
    protected boolean hasDisallowedLiquid(ChunkAccess chunk, int startX, int endX, int startY, int endY, int startZ, int endZ) {
        ChunkPos pos = chunk.getPos();
        int minX = pos.getMinBlockX();
        int minZ = pos.getMinBlockZ();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        
        for(int x = startX; x <= endX; ++x) {
            for(int z = startZ; z <= endZ; ++z) {
                for(int y = startY - 1; y <= endY + 1; ++y) {
                    mutable.set(minX + x, y, minZ + z);
                    
                    if (this.liquids.contains(chunk.getFluidState(mutable).getType())) {
                        return true;
                    }
                    
                    if (y != endY + 1 && !this.isEdge(x, z, startX, endX, startZ, endZ)) {
                        y = endY;
                    }
                }
            }
        }
        
        return false;
    }
}