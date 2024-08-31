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
    BlockState AIR = Blocks.AIR.defaultBlockState();
    BlockState WATER = Blocks.WATER.defaultBlockState();
    BlockState LAVA = Blocks.LAVA.defaultBlockState();

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
                    return y >= seaLevel ? AIR : fillState;
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
        
        private final WorldgenRandom random = new WorldgenRandom();
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
            this.aquiferCache = new AquiferStatus[gridSize];
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
                    fluidType = LAVA;
                    barrierDensity = 0.0;
                    this.shouldScheduleFluidUpdate = false;
                } else {
                    // Precompute values that remain constant for the loop iterations
                    int gridX = Math.floorDiv(x - 5, X_SPACING);
                    int gridY = Math.floorDiv(y + 1, Y_SPACING);
                    int gridZ = Math.floorDiv(z - 5, Z_SPACING);
                    
                    int[] distances = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
                    long[] sources = {0L, 0L, 0L};
                    
                    // Cache variables for reuse
                    int localX, localY, localZ, deltaX, deltaY, deltaZ, distance;
                    long sourcePos;
                    
                    for (int offsetX = 0; offsetX <= 1; ++offsetX) {
                        for (int offsetY = -1; offsetY <= 1; ++offsetY) {
                            for (int offsetZ = 0; offsetZ <= 1; ++offsetZ) {
                                localX = gridX + offsetX;
                                localY = gridY + offsetY;
                                localZ = gridZ + offsetZ;
                                
                                int index = this.getIndex(localX, localY, localZ);
                                long cache = this.aquiferLocationCache[index];
                                
                                if (cache != Long.MAX_VALUE) {
                                    sourcePos = cache;
                                } else {
                                    this.random.setSeed(Mth.getSeed(localX, localY * 3, localZ) + 1L);
                                    sourcePos = BlockPos.asLong(
                                        localX * X_SPACING + this.random.nextInt(X_RANGE),
                                        localY * Y_SPACING + this.random.nextInt(Y_RANGE),
                                        localZ * Z_SPACING + this.random.nextInt(Z_RANGE)
                                    );
                                    this.aquiferLocationCache[index] = sourcePos;
                                }
                                
                                deltaX = BlockPos.getX(sourcePos) - x;
                                deltaY = BlockPos.getY(sourcePos) - y;
                                deltaZ = BlockPos.getZ(sourcePos) - z;
                                
                                distance = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
                                
                                if (distance < distances[0]) {
                                    sources[2] = sources[1];
                                    sources[1] = sources[0];
                                    sources[0] = sourcePos;
                                    distances[2] = distances[1];
                                    distances[1] = distances[0];
                                    distances[0] = distance;
                                } else if (distance < distances[1]) {
                                    sources[2] = sources[1];
                                    sources[1] = sourcePos;
                                    distances[2] = distances[1];
                                    distances[1] = distance;
                                } else if (distance < distances[2]) {
                                    sources[2] = sourcePos;
                                    distances[2] = distance;
                                }
                            }
                        }
                    }
                    
                    AquiferStatus[] aquifers = {
                        this.getAquiferStatus(sources[0]),
                        this.getAquiferStatus(sources[1]),
                        this.getAquiferStatus(sources[2])
                    };
                    
                    double[] similarities = {
                        this.similarity(distances[0], distances[1]),
                        this.similarity(distances[0], distances[2]),
                        this.similarity(distances[1], distances[2])
                    };
                    
                    this.shouldScheduleFluidUpdate = similarities[0] > 0.0;
                    
                    if (aquifers[0].fluidLevel >= y && aquifers[0].fluidType.is(Blocks.WATER) && this.isLavaLevel(y - 1)) {
                        barrierDensity = 1.0;
                    } else if (similarities[0] > -1.0) {
                        double substance = 1.0 + (this.barrierNoise.getValue(x, y, z) + 0.05) / 4.0;
                        
                        // Precompute pressure values to avoid multiple calls
                        double[] pressures = {
                            this.calculatePressure(y, substance, aquifers[0], aquifers[1]),
                            this.calculatePressure(y, substance, aquifers[0], aquifers[2]),
                            this.calculatePressure(y, substance, aquifers[1], aquifers[2])
                        };
                        
                        double[] factors = {
                            Math.max(0.0, similarities[0]),
                            Math.max(0.0, similarities[1]),
                            Math.max(0.0, similarities[2])
                        };
                        
                        double pressure = 2.0 * factors[0] * Math.max(pressures[0], Math.max(pressures[1] * factors[1], pressures[2] * factors[2]));
                        
                        if (y <= aquifers[0].fluidLevel && y <= aquifers[1].fluidLevel && aquifers[0].fluidLevel != aquifers[1].fluidLevel && Math.abs(this.barrierNoise.getValue(x * 0.5F, y * 0.5F, z * 0.5F)) < 0.3) {
                            barrierDensity = 1.0;
                        } else {
                            barrierDensity = Math.max(0.0, pressure);
                        }
                    } else {
                        barrierDensity = 0.0;
                    }
                    
                    fluidType = y > aquifers[0].fluidLevel ? AIR : aquifers[0].fluidType;
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
                return new AquiferStatus(fluidLevel, WATER);
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
                
                return new AquiferStatus(Math.min(surfaceLevel - 8, fluidSurfaceLevel), isLava ? LAVA : WATER);
            }
        }
    }
    
    final class AquiferStatus {
        final int fluidLevel;
        final BlockState fluidType;
        
        public AquiferStatus(int fluidLevel, BlockState fluidType) {
            this.fluidLevel = fluidLevel;
            this.fluidType = fluidType;
        }
    }
    
    interface  FluidPicker {
        FluidStatus computeFluid(int x, int y, int z);
    }
    
    class FluidStatus {
        final int fluidLevel;
        final BlockState fluidType;
        
        public FluidStatus(int fluidLevel, BlockState fluidType) {
            this.fluidLevel = fluidLevel;
            this.fluidType = fluidType;
        }
        
        public BlockState at(int y) {
            return y < this.fluidLevel ? this.fluidType : Blocks.AIR.defaultBlockState();
        }
    }
}