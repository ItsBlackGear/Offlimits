package com.blackgear.offlimits.common.level;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.common.level.levelgen.stonesource.BaseStoneSource;
import com.blackgear.offlimits.common.level.noise.NoiseSampler;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.Arrays;

public interface Aquifer {
    static Aquifer create(
        ChunkPos pos,
        NormalNoise barrierNoise,
        NormalNoise waterLevelNoise,
        NormalNoise lavaNoise,
        NoiseGeneratorSettings settings,
        NoiseSampler sampler,
        int minY,
        int chunkCountY
    ) {
        return new NoiseBasedAquifer(pos, barrierNoise, waterLevelNoise, lavaNoise, settings, sampler, minY, chunkCountY);
    }
    
    static Aquifer createDisabled(int seaLevel, BlockState fillState) {
        return new Aquifer() {
            public BlockState computeState(BaseStoneSource source, int x, int y, int z, double density) {
                if (density > 0.0) {
                    return source.getBaseBlock(x, y, z);
                } else {
                    return y >= seaLevel ? Blocks.AIR.defaultBlockState() : fillState;
                }
            }
            
            public boolean shouldScheduleFluidUpdate() {
                return false;
            }
        };
    }
    
    BlockState computeState(BaseStoneSource source, int x, int y, int z, double density);
    
    boolean shouldScheduleFluidUpdate();
    
    class NoiseBasedAquifer implements Aquifer {
        private static final int X_RANGE = 10;
        private static final int Y_RANGE = 9;
        private static final int Z_RANGE = 10;
        private static final int X_SPACING = 16;
        private static final int Y_SPACING = 12;
        private static final int Z_SPACING = 16;
        private final NormalNoise barrierNoise;
        private final NormalNoise waterLevelNoise;
        private final NormalNoise lavaNoise;
        private final NoiseGeneratorSettings noiseGeneratorSettings;
        private final AquiferStatus[] aquiferCache;
        private final long[] aquiferLocationCache;
        private boolean shouldScheduleFluidUpdate;
        private final NoiseSampler sampler;
        private final int minGridX;
        private final int minGridY;
        private final int minGridZ;
        private final int gridSizeX;
        private final int gridSizeZ;
        
        NoiseBasedAquifer(
            ChunkPos pos,
            NormalNoise barrierNoise,
            NormalNoise waterLevelNoise,
            NormalNoise lavaNoise,
            NoiseGeneratorSettings settings,
            NoiseSampler sampler,
            int minY,
            int chunkCountY
        ) {
            this.barrierNoise = barrierNoise;
            this.waterLevelNoise = waterLevelNoise;
            this.lavaNoise = lavaNoise;
            this.noiseGeneratorSettings = settings;
            this.sampler = sampler;
            this.minGridX = this.gridX(pos.getMinBlockX()) - 1;
            int gridX = this.gridX(pos.getMaxBlockX()) + 1;
            this.gridSizeX = gridX - this.minGridX + 1;
            this.minGridY = this.gridY(minY) - 1;
            int gridY = this.gridY(minY + chunkCountY) + 1;
            int gridSizeY = gridY - this.minGridY + 1;
            this.minGridZ = this.gridZ(pos.getMinBlockZ()) - 1;
            int gridZ = this.gridZ(pos.getMaxBlockZ()) + 1;
            this.gridSizeZ = gridZ - this.minGridZ + 1;
            int gridSize = this.gridSizeX * gridSizeY * this.gridSizeZ;
            this.aquiferCache = new Aquifer.NoiseBasedAquifer.AquiferStatus[gridSize];
            this.aquiferLocationCache = new long[gridSize];
            Arrays.fill(this.aquiferLocationCache, Long.MAX_VALUE);
        }
        
        private int getIndex(int x, int y, int z) {
            int localX = x - this.minGridX;
            int localY = y - this.minGridY;
            int localZ = z - this.minGridZ;
            return (localY * this.gridSizeZ + localZ) * this.gridSizeX + localX;
        }
        
        @Override
        public BlockState computeState(BaseStoneSource source, int x, int y, int z, double density) {
            if (density <= 0.0) {
                double barrierDensity;
                BlockState fluidType;
                
                if (this.isLavaLevel(y)) {
                    fluidType = Blocks.LAVA.defaultBlockState();
                    barrierDensity = 0.0;
                    this.shouldScheduleFluidUpdate = false;
                } else {
                    int gridX = Math.floorDiv(x - 5, X_SPACING);
                    int gridY = Math.floorDiv(y + 1, Y_SPACING);
                    int gridZ = Math.floorDiv(z - 5, Z_SPACING);
                    
                    int firstDistance2 = Integer.MAX_VALUE;
                    int secondDistance2 = Integer.MAX_VALUE;
                    int thirdDistance2 = Integer.MAX_VALUE;
                    
                    long firstSource = 0L;
                    long secondSource = 0L;
                    long thirdSource = 0L;
                    
                    WorldgenRandom random = new WorldgenRandom();
                    for (int offsetX = 0; offsetX <= 1; ++offsetX) {
                        for (int offsetY = -1; offsetY <= 1; ++offsetY) {
                            for (int offsetZ = 0; offsetZ <= 1; ++offsetZ) {
                                
                                int localX = gridX + offsetX;
                                int localY = gridY + offsetY;
                                int localZ = gridZ + offsetZ;
                                
                                int index = this.getIndex(localX, localY, localZ);
                                long cache = this.aquiferLocationCache[index];
                                long sourcePos;
                                
                                if (cache != Long.MAX_VALUE) {
                                    sourcePos = cache;
                                } else {
                                    random.setSeed(Mth.getSeed(localX, localY * 3, localZ) + 1L);
                                    sourcePos = BlockPos.asLong(
                                        localX * X_SPACING + random.nextInt(X_RANGE),
                                        localY * Y_SPACING + random.nextInt(Y_RANGE),
                                        localZ * Z_SPACING + random.nextInt(Z_RANGE)
                                    );
                                    this.aquiferLocationCache[index] = sourcePos;
                                }
                                
                                int deltaX = BlockPos.getX(sourcePos) - x;
                                int deltaY = BlockPos.getY(sourcePos) - y;
                                int deltaZ = BlockPos.getZ(sourcePos) - z;
                                
                                int distance2 = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
                                
                                if (firstDistance2 >= distance2) {
                                    thirdSource = secondSource;
                                    secondSource = firstSource;
                                    firstSource = sourcePos;
                                    thirdDistance2 = secondDistance2;
                                    secondDistance2 = firstDistance2;
                                    firstDistance2 = distance2;
                                } else if (secondDistance2 >= distance2) {
                                    thirdSource = secondSource;
                                    secondSource = sourcePos;
                                    thirdDistance2 = secondDistance2;
                                    secondDistance2 = distance2;
                                } else if (thirdDistance2 >= distance2) {
                                    thirdSource = sourcePos;
                                    thirdDistance2 = distance2;
                                }
                            }
                        }
                    }
                    
                    AquiferStatus firstAquifer = this.getAquiferStatus(firstSource);
                    AquiferStatus secondAquifer = this.getAquiferStatus(secondSource);
                    AquiferStatus thirdAquifer = this.getAquiferStatus(thirdSource);
                    
                    double firstToSecond = this.similarity(firstDistance2, secondDistance2);
                    double firstToThird = this.similarity(firstDistance2, thirdDistance2);
                    double secondToThird = this.similarity(secondDistance2, thirdDistance2);
                    
                    this.shouldScheduleFluidUpdate = firstToSecond > 0.0;
                    
                    if (firstAquifer.fluidLevel >= y && firstAquifer.fluidType.is(Blocks.WATER) && this.isLavaLevel(y - 1)) {
                        barrierDensity = 1.0;
                    } else if (firstToSecond > -1.0) {
                        double substance = 1.0 + (this.barrierNoise.getValue(x, y, z) + 0.05) / 4.0;
                        
                        double firstToSecondPressure = this.calculatePressure(y, substance, firstAquifer, secondAquifer);
                        double firstToThirdPressure = this.calculatePressure(y, substance, firstAquifer, thirdAquifer);
                        double secondToThirdPressure = this.calculatePressure(y, substance, secondAquifer, thirdAquifer);
                        
                        double firstToSecondFactor = Math.max(0.0, firstToSecond);
                        double firstToThirdFactor = Math.max(0.0, firstToThird);
                        double secondToThirdFactor = Math.max(0.0, secondToThird);
                        
                        double pressure = 2.0 * firstToSecondFactor * Math.max(firstToSecondPressure, Math.max(firstToThirdPressure * firstToThirdFactor, secondToThirdPressure * secondToThirdFactor));
                        if (y <= firstAquifer.fluidLevel
                            && y <= secondAquifer.fluidLevel
                            && firstAquifer.fluidLevel != secondAquifer.fluidLevel
                            && Math.abs(this.barrierNoise.getValue((float)x * 0.5F, (float)y * 0.5F, (float)z * 0.5F)) < 0.3) {
                            barrierDensity = 1.0;
                        } else {
                            barrierDensity = Math.max(0.0, pressure);
                        }
                    } else {
                        barrierDensity = 0.0;
                    }
                    
                    fluidType = y > firstAquifer.fluidLevel ? Blocks.AIR.defaultBlockState() : firstAquifer.fluidType;
                }
                
                if (density + barrierDensity <= 0.0) {
                    return fluidType;
                }
            }
            
            this.shouldScheduleFluidUpdate = false;
            return source.getBaseBlock(x, y, z);
        }
        
        public boolean shouldScheduleFluidUpdate() {
            return this.shouldScheduleFluidUpdate;
        }
        
        private boolean isLavaLevel(int y) {
            return y - Offlimits.INSTANCE.getMinBuildHeight() <= 9;
        }
        
        private double similarity(int firstDistance, int secondDistance) {
            return 1.0 - (double) Math.abs(secondDistance - firstDistance) / 25.0;
        }
        
        private double calculatePressure(int y, double substance, AquiferStatus firstFluid, AquiferStatus secondFluid) {
            if (y <= firstFluid.fluidLevel && y <= secondFluid.fluidLevel && firstFluid.fluidType != secondFluid.fluidType) {
                return 1.0;
            } else if (y > Math.max(firstFluid.fluidLevel, secondFluid.fluidLevel) + 1) {
                return 0.0;
            } else {
                int distanceFromFluids = Math.abs(firstFluid.fluidLevel - secondFluid.fluidLevel);
                double meanLevel = 0.5 * (double) (firstFluid.fluidLevel + secondFluid.fluidLevel);
                double distanceFromMean = Math.abs(meanLevel - (double) y + 0.5);
                return 0.5 * (double) distanceFromFluids * substance - distanceFromMean;
            }
        }
        
        private int gridX(int x) {
            return Math.floorDiv(x, X_SPACING);
        }
        
        private int gridY(int y) {
            return Math.floorDiv(y, Y_SPACING);
        }
        
        private int gridZ(int z) {
            return Math.floorDiv(z, Z_SPACING);
        }
        
        private AquiferStatus getAquiferStatus(long packedPos) {
            int x = BlockPos.getX(packedPos);
            int y = BlockPos.getY(packedPos);
            int z = BlockPos.getZ(packedPos);
            int gridX = this.gridX(x);
            int gridY = this.gridY(y);
            int gridZ = this.gridZ(z);
            int index = this.getIndex(gridX, gridY, gridZ);
            
            AquiferStatus cache = this.aquiferCache[index];
            
            if (cache != null) {
                return cache;
            } else {
                AquiferStatus aquifer = this.computeAquifer(x, y, z);
                this.aquiferCache[index] = aquifer;
                return aquifer;
            }
        }
        
        private AquiferStatus computeAquifer(int x, int y, int z) {
            int fluidLevel = this.noiseGeneratorSettings.seaLevel() - 1;
            int surfaceLevel = this.sampler.getPreliminarySurfaceLevel(x, y, z);
            
            if (surfaceLevel < fluidLevel && y > surfaceLevel - 8) {
                return new AquiferStatus(fluidLevel, Blocks.WATER.defaultBlockState());
            } else {
                double waterLevelNoise = this.waterLevelNoise.getValue(Math.floorDiv(x, 64), (double) Math.floorDiv(y, 40) / 1.4, Math.floorDiv(z, 64)) * 30.0 - 10.0;
                boolean isLava = false;
                if (Math.abs(waterLevelNoise) > 8.0) {
                    waterLevelNoise *= 4.0;
                }
                
                int levelSpread = Math.floorDiv(y, 40) * 40 + 20;
                int fluidSurfaceLevel = levelSpread + Mth.floor(waterLevelNoise);
                
                if (levelSpread == -20) {
                    double lavaNoise = this.lavaNoise.getValue(Math.floorDiv(x, 64), (double)Math.floorDiv(y, 40) / 1.4, Math.floorDiv(z, 64));
                    isLava = Math.abs(lavaNoise) > 0.22F;
                }
                
                return new AquiferStatus(Math.min(surfaceLevel - 8, fluidSurfaceLevel), isLava ? Blocks.LAVA.defaultBlockState() : Blocks.WATER.defaultBlockState());
            }
        }
        
        static final class AquiferStatus {
            final int fluidLevel;
            final BlockState fluidType;
            
            public AquiferStatus(int fluidLevel, BlockState fluidType) {
                this.fluidLevel = fluidLevel;
                this.fluidType = fluidType;
            }
        }
    }
}