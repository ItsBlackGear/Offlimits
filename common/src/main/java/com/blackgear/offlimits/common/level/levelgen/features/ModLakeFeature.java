package com.blackgear.offlimits.common.level.levelgen.features;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.common.level.chunk.chunk.NoiseChunkExtension;
import com.blackgear.offlimits.common.level.chunk.stonesource.BaseStoneSource;
import com.blackgear.platform.core.tags.PlatformTags;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import net.minecraft.world.level.material.Material;

import java.util.List;
import java.util.Random;

public class ModLakeFeature extends Feature<BlockStateConfiguration> {
    private static final List<Block> NON_REPLACEABLE = ImmutableList.of(Blocks.BEDROCK, Blocks.SPAWNER, Blocks.CHEST, Blocks.END_PORTAL_FRAME);
    private static final BlockState AIR = Blocks.CAVE_AIR.defaultBlockState();
    
    public ModLakeFeature(Codec<BlockStateConfiguration> codec) {
        super(codec);
    }
    
    @Override
    public boolean place(
        WorldGenLevel level,
        ChunkGenerator generator,
        Random random,
        BlockPos origin,
        BlockStateConfiguration config
    ) {
        while (origin.getY() > Offlimits.LEVEL.getMinBuildHeight() + 5 && level.isEmptyBlock(origin)) {
            origin = origin.below();
        }
        
        if (origin.getY() <= Offlimits.LEVEL.getMinBuildHeight() + 4) {
            return false;
        } else {
            boolean[] bls = new boolean[2048];
            int i = random.nextInt(4) + 4;
            
            for (int j = 0; j < i; j++) {
                double d = random.nextDouble() * 6.0 + 3.0;
                double e = random.nextDouble() * 4.0 + 2.0;
                double f = random.nextDouble() * 6.0 + 3.0;
                double g = random.nextDouble() * (16.0 - d - 2.0) + 1.0 + d / 2.0;
                double h = random.nextDouble() * (8.0 - e - 4.0) + 2.0 + e / 2.0;
                double k = random.nextDouble() * (16.0 - f - 2.0) + 1.0 + f / 2.0;
                
                for(int l = 1; l < 15; ++l) {
                    for(int m = 1; m < 15; ++m) {
                        for(int n = 1; n < 7; ++n) {
                            double o = ((double)l - g) / (d / 2.0);
                            double p = ((double)n - h) / (e / 2.0);
                            double q = ((double)m - k) / (f / 2.0);
                            double r = o * o + p * p + q * q;
                            if (r < 1.0) {
                                bls[(l * 16 + m) * 8 + n] = true;
                            }
                        }
                    }
                }
            }
            
            for(int j = 0; j < 16; ++j) {
                for(int s = 0; s < 16; ++s) {
                    for(int t = 0; t < 8; ++t) {
                        boolean bl = !bls[(j * 16 + s) * 8 + t]
                            && (
                            j < 15 && bls[((j + 1) * 16 + s) * 8 + t]
                                || j > 0 && bls[((j - 1) * 16 + s) * 8 + t]
                                || s < 15 && bls[(j * 16 + s + 1) * 8 + t]
                                || s > 0 && bls[(j * 16 + (s - 1)) * 8 + t]
                                || t < 7 && bls[(j * 16 + s) * 8 + t + 1]
                                || t > 0 && bls[(j * 16 + s) * 8 + (t - 1)]
                        );
                        if (bl) {
                            Material material = level.getBlockState(origin.offset(j, t, s)).getMaterial();
                            if (t >= 4 && material.isLiquid()) {
                                return false;
                            }
                            
                            if (t < 4 && !material.isSolid() && level.getBlockState(origin.offset(j, t, s)) != config.state) {
                                return false;
                            }
                        }
                    }
                }
            }
            
            for(int j = 0; j < 16; ++j) {
                for(int s = 0; s < 16; ++s) {
                    for(int t = 0; t < 8; ++t) {
                        if (bls[(j * 16 + s) * 8 + t]) {
                            BlockPos blockPos2 = origin.offset(j, t, s);
                            boolean bl2 = t >= 4;
                            level.setBlock(blockPos2, bl2 ? AIR : config.state, 2);
                            if (bl2) {
                                level.getBlockTicks().scheduleTick(blockPos2, AIR.getBlock(), 0);
                                this.markAboveForPostProcessing(level, blockPos2);
                            }
                        }
                    }
                }
            }
            
            for(int j = 0; j < 16; ++j) {
                for(int s = 0; s < 16; ++s) {
                    for(int t = 4; t < 8; ++t) {
                        if (bls[(j * 16 + s) * 8 + t]) {
                            BlockPos blockPos2 = origin.offset(j, t - 1, s);
                            if (level.getBlockState(blockPos2).is(PlatformTags.DIRT) && level.getBrightness(LightLayer.SKY, origin.offset(j, t, s)) > 0) {
                                Biome biome = level.getBiome(blockPos2);
                                if (biome.getGenerationSettings().getSurfaceBuilderConfig().getTopMaterial().is(Blocks.MYCELIUM)) {
                                    level.setBlock(blockPos2, Blocks.MYCELIUM.defaultBlockState(), 2);
                                } else {
                                    level.setBlock(blockPos2, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                                }
                            }
                        }
                    }
                }
            }
            
            if (config.state.getMaterial() == Material.LAVA) {
                if (generator instanceof NoiseChunkExtension) {
                    BaseStoneSource stoneSource = ((NoiseChunkExtension) generator).getBaseStoneSource();
                    
                    for(int s = 0; s < 16; ++s) {
                        for(int t = 0; t < 16; ++t) {
                            for(int u = 0; u < 8; ++u) {
                                boolean bl2 = !bls[(s * 16 + t) * 8 + u]
                                    && (
                                    s < 15 && bls[((s + 1) * 16 + t) * 8 + u]
                                        || s > 0 && bls[((s - 1) * 16 + t) * 8 + u]
                                        || t < 15 && bls[(s * 16 + t + 1) * 8 + u]
                                        || t > 0 && bls[(s * 16 + (t - 1)) * 8 + u]
                                        || u < 7 && bls[(s * 16 + t) * 8 + u + 1]
                                        || u > 0 && bls[(s * 16 + t) * 8 + (u - 1)]
                                );
                                if (bl2 && (u < 4 || random.nextInt(2) != 0)) {
                                    BlockState blockState = level.getBlockState(origin.offset(s, u, t));
                                    if (blockState.getMaterial().isSolid() && !(blockState.is(BlockTags.LEAVES) || blockState.is(BlockTags.LOGS) || NON_REPLACEABLE.contains(blockState.getBlock()))) {
                                        BlockPos blockPos3 = origin.offset(s, u, t);
                                        level.setBlock(blockPos3, stoneSource.getBaseBlock(blockPos3), 2);
                                        this.markAboveForPostProcessing(level, blockPos3);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            if (config.state.getMaterial() == Material.WATER) {
                for(int j = 0; j < 16; ++j) {
                    for(int s = 0; s < 16; ++s) {
                        BlockPos blockPos2 = origin.offset(j, 4, s);
                        if (level.getBiome(blockPos2).shouldFreeze(level, blockPos2, false)) {
                            level.setBlock(blockPos2, Blocks.ICE.defaultBlockState(), 2);
                        }
                    }
                }
            }
            
            return true;
        }
    }
    
    private void markAboveForPostProcessing(WorldGenLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        
        for(int i = 0; i < 2; ++i) {
            mutable.move(Direction.UP);
            if (level.getBlockState(mutable).isAir()) {
                return;
            }
            
            level.getChunk(mutable).markPosForPostprocessing(mutable);
        }
    }
}