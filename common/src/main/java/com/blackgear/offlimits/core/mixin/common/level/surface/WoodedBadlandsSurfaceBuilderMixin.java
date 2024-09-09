package com.blackgear.offlimits.core.mixin.common.level.surface;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.common.level.chunk.surface.BiomeExtension;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.surfacebuilders.BadlandsSurfaceBuilder;
import net.minecraft.world.level.levelgen.surfacebuilders.SurfaceBuilderBaseConfiguration;
import net.minecraft.world.level.levelgen.surfacebuilders.SurfaceBuilderConfiguration;
import net.minecraft.world.level.levelgen.surfacebuilders.WoodedBadlandsSurfaceBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(WoodedBadlandsSurfaceBuilder.class)
public class WoodedBadlandsSurfaceBuilderMixin extends BadlandsSurfaceBuilder {
    @Shadow @Final private static BlockState WHITE_TERRACOTTA;
    
    @Shadow @Final private static BlockState ORANGE_TERRACOTTA;
    
    @Shadow @Final private static BlockState TERRACOTTA;
    
    public WoodedBadlandsSurfaceBuilderMixin(Codec<SurfaceBuilderBaseConfiguration> codec) {
        super(codec);
    }
    
    @Inject(
        method = "apply(Ljava/util/Random;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/biome/Biome;IIIDLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;IJLnet/minecraft/world/level/levelgen/surfacebuilders/SurfaceBuilderBaseConfiguration;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    public void offlimits$apply(
        Random random,
        ChunkAccess chunk,
        Biome biome,
        int x,
        int z,
        int startHeight,
        double surfaceNoise,
        BlockState defaultBlock,
        BlockState defaultFluid,
        int seaLevel,
        long seed,
        SurfaceBuilderBaseConfiguration config,
        CallbackInfo callback
    ) {
        if (Offlimits.CONFIG.allowTerrainModifications.get()) {
            int localX = x & 15;
            int localZ = z & 15;
            BlockState currentState = WHITE_TERRACOTTA;
            SurfaceBuilderConfiguration configuration = biome.getGenerationSettings().getSurfaceBuilderConfig();
            BlockState underMaterial = configuration.getUnderMaterial();
            BlockState topMaterial = configuration.getTopMaterial();
            BlockState currentMaterial = underMaterial;
            
            int factor = (int)(surfaceNoise / 3.0 + 3.0 + random.nextDouble() * 0.25);
            boolean isPositiveNoise = Math.cos(surfaceNoise / 3.0 * Math.PI) > 0.0;
            int depth = -1;
            boolean isTopMaterial = false;
            int clayDepth = 0;
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            
            for(int y = startHeight; y >= ((BiomeExtension) biome).getMinSurfaceLevel(); --y) {
                if (clayDepth < 15) {
                    mutable.set(localX, y, localZ);
                    BlockState localState = chunk.getBlockState(mutable);
                    
                    if (localState.isAir()) {
                        depth = -1;
                    } else if (localState.is(defaultBlock.getBlock())) {
                        if (depth == -1) {
                            isTopMaterial = false;
                            
                            if (factor <= 0) {
                                currentState = Blocks.AIR.defaultBlockState();
                                currentMaterial = defaultBlock;
                            } else if (y >= seaLevel - 4 && y <= seaLevel + 1) {
                                currentState = WHITE_TERRACOTTA;
                                currentMaterial = underMaterial;
                            }
                            
                            if (y < seaLevel && (currentState == null || currentState.isAir())) {
                                currentState = defaultFluid;
                            }
                            
                            depth = factor + Math.max(0, y - seaLevel);
                            if (y < seaLevel - 1) {
                                chunk.setBlockState(mutable, currentMaterial, false);
                                if (currentMaterial == WHITE_TERRACOTTA) {
                                    chunk.setBlockState(mutable, ORANGE_TERRACOTTA, false);
                                }
                            } else if (y > 86 + factor * 2) {
                                if (isPositiveNoise) {
                                    chunk.setBlockState(mutable, Blocks.COARSE_DIRT.defaultBlockState(), false);
                                } else {
                                    chunk.setBlockState(mutable, Blocks.GRASS_BLOCK.defaultBlockState(), false);
                                }
                            } else if (y <= seaLevel + 3 + factor) {
                                chunk.setBlockState(mutable, topMaterial, false);
                                isTopMaterial = true;
                            } else {
                                BlockState bandState;
                                
                                if (y < 64 || y > 127) {
                                    bandState = ORANGE_TERRACOTTA;
                                } else if (isPositiveNoise) {
                                    bandState = TERRACOTTA;
                                } else {
                                    bandState = this.getBand(x, y, z);
                                }
                                
                                chunk.setBlockState(mutable, bandState, false);
                            }
                        } else if (depth > 0) {
                            --depth;
                            if (isTopMaterial) {
                                chunk.setBlockState(mutable, ORANGE_TERRACOTTA, false);
                            } else {
                                chunk.setBlockState(mutable, this.getBand(x, y, z), false);
                            }
                        }
                        
                        ++clayDepth;
                    }
                }
            }
            
            callback.cancel();
        }
    }
}