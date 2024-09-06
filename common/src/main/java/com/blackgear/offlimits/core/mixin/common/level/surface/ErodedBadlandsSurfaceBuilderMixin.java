package com.blackgear.offlimits.core.mixin.common.level.surface;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.common.level.surface.BiomeExtension;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.surfacebuilders.BadlandsSurfaceBuilder;
import net.minecraft.world.level.levelgen.surfacebuilders.ErodedBadlandsSurfaceBuilder;
import net.minecraft.world.level.levelgen.surfacebuilders.SurfaceBuilderBaseConfiguration;
import net.minecraft.world.level.levelgen.surfacebuilders.SurfaceBuilderConfiguration;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(ErodedBadlandsSurfaceBuilder.class)
public class ErodedBadlandsSurfaceBuilderMixin extends BadlandsSurfaceBuilder {
    @Shadow @Final private static BlockState WHITE_TERRACOTTA;
    @Shadow @Final private static BlockState ORANGE_TERRACOTTA;
    @Shadow @Final private static BlockState TERRACOTTA;
    
    public ErodedBadlandsSurfaceBuilderMixin(Codec<SurfaceBuilderBaseConfiguration> codec) {
        super(codec);
    }
    
    @Inject(
        method = "apply(Ljava/util/Random;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/biome/Biome;IIIDLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;IJLnet/minecraft/world/level/levelgen/surfacebuilders/SurfaceBuilderConfiguration;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void offlimits$apply(
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
        SurfaceBuilderConfiguration config,
        CallbackInfo callback
    ) {
        if (Offlimits.CONFIG.allowTerrainModifications.get()) {
            double noise = 0.0;
            double pillarNoise = Math.min(Math.abs(surfaceNoise), this.pillarNoise.getValue((double)x * 0.25, (double)z * 0.25, false) * 15.0);

            if (pillarNoise > 0.0) {
                double scale = 0.001953125;
                double pillarRoofNoise = Math.abs(this.pillarRoofNoise.getValue((double)x * scale, (double)z * scale, false));
                noise = pillarNoise * pillarNoise * 2.5;
                double offset = Math.ceil(pillarRoofNoise * 50.0) + 14.0;
                if (noise > offset) {
                    noise = offset;
                }

                noise += 64.0;
            }

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
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

            for(int y = Math.max(startHeight, (int) noise + 1); y >= ((BiomeExtension) biome).getMinSurfaceLevel(); --y) {
                mutable.set(localX, y, localZ);
                if (chunk.getBlockState(mutable).isAir() && y < (int) noise) {
                    chunk.setBlockState(mutable, defaultBlock, false);
                }

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
                        if (y >= seaLevel - 1) {

                            if (y > seaLevel + 3 + factor) {
                                BlockState bandState;

                                if (y < 64 || y > 127) {
                                    bandState = ORANGE_TERRACOTTA;
                                } else if (isPositiveNoise) {
                                    bandState = TERRACOTTA;
                                } else {
                                    bandState = this.getBand(x, y, z);
                                }

                                chunk.setBlockState(mutable, bandState, false);
                            } else {
                                chunk.setBlockState(mutable, topMaterial, false);
                                isTopMaterial = true;
                            }
                        } else {
                            chunk.setBlockState(mutable, currentMaterial, false);

                            if (currentMaterial.is(Blocks.WHITE_TERRACOTTA)
                                || currentMaterial.is(Blocks.ORANGE_TERRACOTTA)
                                || currentMaterial.is(Blocks.MAGENTA_TERRACOTTA)
                                || currentMaterial.is(Blocks.LIGHT_BLUE_TERRACOTTA)
                                || currentMaterial.is(Blocks.YELLOW_TERRACOTTA)
                                || currentMaterial.is(Blocks.LIME_TERRACOTTA)
                                || currentMaterial.is(Blocks.PINK_TERRACOTTA)
                                || currentMaterial.is(Blocks.GRAY_TERRACOTTA)
                                || currentMaterial.is(Blocks.LIGHT_GRAY_TERRACOTTA)
                                || currentMaterial.is(Blocks.CYAN_TERRACOTTA)
                                || currentMaterial.is(Blocks.PURPLE_TERRACOTTA)
                                || currentMaterial.is(Blocks.BLUE_TERRACOTTA)
                                || currentMaterial.is(Blocks.BROWN_TERRACOTTA)
                                || currentMaterial.is(Blocks.GREEN_TERRACOTTA)
                                || currentMaterial.is(Blocks.RED_TERRACOTTA)
                                || currentMaterial.is(Blocks.BLACK_TERRACOTTA)) {
                                chunk.setBlockState(mutable, ORANGE_TERRACOTTA, false);
                            }
                        }
                    } else if (depth > 0) {
                        --depth;

                        if (isTopMaterial) {
                            chunk.setBlockState(mutable, ORANGE_TERRACOTTA, false);
                        } else {
                            chunk.setBlockState(mutable, this.getBand(x, y, z), false);
                        }
                    }
                }
            }

            callback.cancel();
        }
    }
}