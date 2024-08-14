package com.blackgear.offlimits.core.mixin.common.level.surface;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.common.level.surface.BiomeExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.surfacebuilders.FrozenOceanSurfaceBuilder;
import net.minecraft.world.level.levelgen.surfacebuilders.SurfaceBuilderBaseConfiguration;
import net.minecraft.world.level.levelgen.surfacebuilders.SurfaceBuilderConfiguration;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.minecraft.world.level.material.Material;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(FrozenOceanSurfaceBuilder.class)
public abstract class FrozenOceanSurfaceBuilderMixin {
    @Shadow @Final protected static BlockState SNOW_BLOCK;
    @Shadow @Final protected static BlockState PACKED_ICE;
    @Shadow @Final private static BlockState GRAVEL;
    @Shadow @Final private static BlockState AIR;
    @Shadow @Final private static BlockState ICE;
    
    @Shadow private PerlinSimplexNoise icebergNoise;
    @Shadow private PerlinSimplexNoise icebergRoofNoise;
    
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
            double noise = 0.0;
            double threshold = 0.0;
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            float temperature = biome.getTemperature(mutable.set(x, 63, z));
            double icebergNoise = Math.min(Math.abs(surfaceNoise), this.icebergNoise.getValue((double)x * 0.1, (double)z * 0.1, false) * 15.0);
            
            if (icebergNoise > 1.8) {
                double scale = 0.09765625;
                double icebergRoofNoise = Math.abs(this.icebergRoofNoise.getValue((double)x * scale, (double)z * scale, false));
                noise = icebergNoise * icebergNoise * 1.2;
                double offset = Math.ceil(icebergRoofNoise * 40.0) + 14.0;
                if (noise > offset) {
                    noise = offset;
                }
                
                if (temperature > 0.1F) {
                    noise -= 2.0;
                }
                
                if (noise > 2.0) {
                    threshold = (double) seaLevel - noise - 7.0;
                    noise += seaLevel;
                } else {
                    noise = 0.0;
                }
            }
            
            int localX = x & 15;
            int localZ = z & 15;
            SurfaceBuilderConfiguration configuration = biome.getGenerationSettings().getSurfaceBuilderConfig();
            BlockState underMaterial = configuration.getUnderMaterial();
            BlockState topMaterial = configuration.getTopMaterial();
            BlockState currentMaterial = underMaterial;
            BlockState currentState = topMaterial;
            
            int factor = (int)(surfaceNoise / 3.0 + 3.0 + random.nextDouble() * 0.25);
            int depth = -1;
            int iceCount = 0;
            int iceLimit = 2 + random.nextInt(4);
            int iceHeight = seaLevel + 18 + random.nextInt(10);
            
            for(int localY = Math.max(startHeight, (int)noise + 1); localY >= ((BiomeExtension) biome).getPreliminarySurfaceLevel(); --localY) {
                mutable.set(localX, localY, localZ);
                if (chunk.getBlockState(mutable).isAir() && localY < (int)noise && random.nextDouble() > 0.01) {
                    chunk.setBlockState(mutable, PACKED_ICE, false);
                } else if (chunk.getBlockState(mutable).getMaterial() == Material.WATER && localY > (int)threshold && localY < seaLevel && threshold != 0.0 && random.nextDouble() > 0.15) {
                    chunk.setBlockState(mutable, PACKED_ICE, false);
                }
                
                BlockState currentBlockState = chunk.getBlockState(mutable);
                if (currentBlockState.isAir()) {
                    depth = -1;
                } else if (currentBlockState.is(defaultBlock.getBlock())) {
                    if (depth == -1) {
                        if (factor <= 0) {
                            currentState = AIR;
                            currentMaterial = defaultBlock;
                        } else if (localY >= seaLevel - 4 && localY <= seaLevel + 1) {
                            currentState = topMaterial;
                            currentMaterial = underMaterial;
                        }
                        
                        if (localY < seaLevel && (currentState == null || currentState.isAir())) {
                            if (biome.getTemperature(mutable.set(x, localY, z)) < 0.15F) {
                                currentState = ICE;
                            } else {
                                currentState = defaultFluid;
                            }
                        }
                        
                        depth = factor;
                        if (localY >= seaLevel - 1) {
                            chunk.setBlockState(mutable, currentState, false);
                        } else if (localY < seaLevel - 7 - factor) {
                            currentState = AIR;
                            currentMaterial = defaultBlock;
                            chunk.setBlockState(mutable, GRAVEL, false);
                        } else {
                            chunk.setBlockState(mutable, currentMaterial, false);
                        }
                    } else if (depth > 0) {
                        --depth;
                        chunk.setBlockState(mutable, currentMaterial, false);
                        if (depth == 0 && currentMaterial.is(Blocks.SAND) && factor > 1) {
                            depth = random.nextInt(4) + Math.max(0, localY - 63);
                            currentMaterial = currentMaterial.is(Blocks.RED_SAND) ? Blocks.RED_SANDSTONE.defaultBlockState() : Blocks.SANDSTONE.defaultBlockState();
                        }
                    }
                } else if (currentBlockState.is(Blocks.PACKED_ICE) && iceCount <= iceLimit && localY > iceHeight) {
                    chunk.setBlockState(mutable, SNOW_BLOCK, false);
                    ++iceCount;
                }
            }
            
            callback.cancel();
        }
    }
}