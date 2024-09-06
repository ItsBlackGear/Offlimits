package com.blackgear.offlimits.common.level;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.common.level.levelgen.sampler.NoiseSampler;
import com.blackgear.offlimits.common.level.levelgen.stonesource.BaseStoneSource;
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
        private final NoiseSampler sampler;
        private final int minGridX;
        private final int minGridY;
        private final int minGridZ;
        private final int gridSizeX;
        private final int gridSizeZ;
        
        public NoiseBasedAquifer(
            ChunkPos chunkPos,
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
            
            this.minGridX = this.gridX(chunkPos.getMinBlockX()) - 1;
            int gridX = this.gridX(chunkPos.getMaxBlockX()) + 1;
            this.gridSizeX = gridX - this.minGridX + 1;
            
            this.minGridY = this.gridY(minY) - 1;
            int gridY = this.gridY(minY + chunkCountY) + 1;
            int gridSizeY = gridY - this.minGridY + 1;
            
            this.minGridZ = this.gridZ(chunkPos.getMinBlockZ()) - 1;
            int gridZ = this.gridZ(chunkPos.getMaxBlockZ()) + 1;
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
                
                boolean shouldScheduleFluidUpdate;
                if (this.isLavaLevel(y)) {
                    fluidType = LAVA;
                    barrierDensity = 0.0;
                    shouldScheduleFluidUpdate = false;
                } else {
                    int gridX = Math.floorDiv(x - 5, X_SPACING);
                    int gridY = Math.floorDiv(y + 1, Y_SPACING);
                    int gridZ = Math.floorDiv(z - 5, Z_SPACING);
                    
                    int dist1 = Integer.MAX_VALUE;
                    int dist2 = Integer.MAX_VALUE;
                    int dist3 = Integer.MAX_VALUE;
                    
                    long src1 = 0L;
                    long src2 = 0L;
                    long src3 = 0L;
                    
                    for(int offsetX = 0; offsetX <= 1; ++offsetX) {
                        for(int offsetY = -1; offsetY <= 1; ++offsetY) {
                            for(int offsetZ = 0; offsetZ <= 1; ++offsetZ) {
                                int localX = gridX + offsetX;
                                int localY = gridY + offsetY;
                                int localZ = gridZ + offsetZ;
                                
                                int index = this.getIndex(localX, localY, localZ);
                                long cache = this.aquiferLocationCache[index];
                                
                                long sourcePos;
                                if (cache != Long.MAX_VALUE) {
                                    sourcePos = cache;
                                } else {
                                    WorldgenRandom random = new WorldgenRandom(Mth.getSeed(localX, localY * 3, localZ) + 1L);
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
                                
                                int distance = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
                                
                                if (dist1 >= distance) {
                                    src3 = src2;
                                    src2 = src1;
                                    src1 = sourcePos;
                                    dist3 = dist2;
                                    dist2 = dist1;
                                    dist1 = distance;
                                } else if (dist2 >= distance) {
                                    src3 = src2;
                                    src2 = sourcePos;
                                    dist3 = dist2;
                                    dist2 = distance;
                                } else if (dist3 >= distance) {
                                    src3 = sourcePos;
                                    dist3 = distance;
                                }
                            }
                        }
                    }
                    
                    AquiferStatus aquifer1 = this.getAquiferStatus(src1);
                    AquiferStatus aquifer2 = this.getAquiferStatus(src2);
                    AquiferStatus aquifer3 = this.getAquiferStatus(src3);
                    
                    double similarity1to2 = this.similarity(dist1, dist2);
                    double similarity1to3 = this.similarity(dist1, dist3);
                    double similarity2to3 = this.similarity(dist2, dist3);
                    
                    shouldScheduleFluidUpdate = similarity1to2 > 0.0;
                    
                    if (aquifer1.fluidLevel >= y && aquifer1.fluidType.is(Blocks.WATER) && this.isLavaLevel(y - 1)) {
                        barrierDensity = 1.0;
                    } else if (similarity1to2 > -1.0) {
                        double baseDensity = 1.0 + (this.barrierNoise.getValue(x, y, z) + 0.05) / 4.0;
                        
                        double pressure1to2 = this.calculatePressure(y, baseDensity, aquifer1, aquifer2);
                        double pressure1to3 = this.calculatePressure(y, baseDensity, aquifer1, aquifer3);
                        double pressure2to3 = this.calculatePressure(y, baseDensity, aquifer2, aquifer3);
                        
                        double factor1 = Math.max(0.0, similarity1to2);
                        double factor2 = Math.max(0.0, similarity1to3);
                        double factor3 = Math.max(0.0, similarity2to3);
                        
                        double pressure = 2.0 * factor1 * Math.max(pressure1to2, Math.max(pressure1to3 * factor2, pressure2to3 * factor3));
                        
                        if (y <= aquifer1.fluidLevel
                            && y <= aquifer2.fluidLevel
                            && aquifer1.fluidLevel != aquifer2.fluidLevel
                            && Math.abs(this.barrierNoise.getValue((float)x * 0.5F, (float)y * 0.5F, (float)z * 0.5F)) < 0.3) {
                            barrierDensity = 1.0;
                        } else {
                            barrierDensity = Math.max(0.0, pressure);
                        }
                    } else {
                        barrierDensity = 0.0;
                    }
                    
                    fluidType = y > aquifer1.fluidLevel ? AIR : aquifer1.fluidType;
                }
                
                if (density + barrierDensity <= 0.0) {
                    this.shouldScheduleFluidUpdate = shouldScheduleFluidUpdate;
                    return fluidType;
                }
            }
            
            this.shouldScheduleFluidUpdate = false;
            return source.getBaseBlock(x, y, z);
        }
        
        @Override
        public boolean shouldScheduleFluidUpdate() {
            return this.shouldScheduleFluidUpdate;
        }
        
        private boolean isLavaLevel(int y) {
            return y - Offlimits.CONFIG.worldGenMinY.get() <= 9;
        }
        
        private double similarity(int firstDistance, int secondDistance) {
            return 1.0 - (double) Math.abs(secondDistance - firstDistance) / 25.0;
        }
        
        private double calculatePressure(int y, double density, AquiferStatus firstFluid, AquiferStatus secondFluid) {
            if (y <= firstFluid.fluidLevel && y <= secondFluid.fluidLevel && firstFluid.fluidType != secondFluid.fluidType) {
                return 1.0;
            } else if (y > Math.max(firstFluid.fluidLevel, secondFluid.fluidLevel) + 1) {
                return 0.0;
            } else {
                int distanceFromLevels = Math.abs(firstFluid.fluidLevel - secondFluid.fluidLevel);
                double meanLevel = 0.5 * (double)(firstFluid.fluidLevel + secondFluid.fluidLevel);
                double distanceFromMean = Math.abs(meanLevel - (double)y + 0.5);
                return 0.5 * (double)distanceFromLevels * density - distanceFromMean;
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
            int seaLevel = this.noiseGeneratorSettings.seaLevel() - 1;
            int surfaceLevel = this.sampler.getPreliminarySurfaceLevel(x, y, z);
            
            if (surfaceLevel < seaLevel && y > surfaceLevel - 8) {
                return new AquiferStatus(seaLevel, WATER);
            } else {
                double waterLevelNoise = this.waterLevelNoise.getValue(Math.floorDiv(x, 64), Math.floorDiv(y, 40), Math.floorDiv(z, 64)) * 50.0 + -20.0;
                boolean isLava = false;
                
                if (waterLevelNoise > 4.0) {
                    waterLevelNoise *= 4.0;
                }
                
                if (waterLevelNoise < -10.0) {
                    waterLevelNoise = -40.0;
                }
                
                int levelSpread = Math.floorDiv(y, 40) * 40 + 20;
                int fluidSurfaceLevel = levelSpread + Mth.floor(waterLevelNoise);
                
                if (levelSpread == -20) {
                    double lavaNoise = this.lavaNoise.getValue(Math.floorDiv(x, 64), (double)Math.floorDiv(y, 40) / 1.4, Math.floorDiv(z, 64));
                    isLava = Math.abs(lavaNoise) > 0.22F;
                }
                
                int fluidLevel = Math.max(seaLevel, surfaceLevel - 8);
                return new AquiferStatus(Math.min(fluidLevel, fluidSurfaceLevel), isLava ? LAVA : WATER);
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