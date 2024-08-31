package com.blackgear.offlimits.common.level;

import com.blackgear.offlimits.common.level.levelgen.stonesource.BaseStoneSource;
import com.blackgear.offlimits.common.level.noise.NoiseSampler;
import com.blackgear.offlimits.common.utils.MathUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.apache.commons.lang3.mutable.MutableDouble;

import java.util.Arrays;
import java.util.Random;

public class NoiseBasedAquifer implements Aquifer {
    private static final int X_RANGE = 10;
    private static final int Y_RANGE = 9;
    private static final int Z_RANGE = 10;
    private static final int X_SPACING = 16;
    private static final int Y_SPACING = 12;
    private static final int Z_SPACING = 16;
    private static final double FLOWING_UPDATE_SIMILARITY = similarity((int) Mth.square(10), (int) Mth.square(12));
    private final NoiseSampler sampler;
    protected final NormalNoise barrierNoise;
    private final NormalNoise fluidLevelFloodednessNoise;
    private final NormalNoise fluidLevelSpreadNoise;
    protected final NormalNoise lavaNoise;
    private final Random random;
    protected final FluidStatus[] aquiferCache;
    protected final long[] aquiferLocationCache;
    private final FluidPicker globalFluidPicker;
    protected boolean shouldScheduleFluidUpdate;
    protected final int minGridX;
    protected final int minGridY;
    protected final int minGridZ;
    protected final int gridSizeX;
    protected final int gridSizeZ;
    private static final int[][] SURFACE_SAMPLING_OFFSETS_IN_CHUNKS = new int[][] {
        {0, 0}, {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {-3, 0}, {-2, 0}, {-1, 0}, {1, 0}, {-2, 1}, {-1, 1}, {0, 1}, {1, 1}
    };
    
    public NoiseBasedAquifer(
        NoiseSampler sampler,
        ChunkPos pos,
        NormalNoise barrierNoise,
        NormalNoise fluidLevelFloodednessNoise,
        NormalNoise fluidLevelSpreadNoise,
        NormalNoise lavaNoise,
        Random random,
        int minY,
        int height,
        FluidPicker globalFluidPicker
    ) {
        this.sampler = sampler;
        this.barrierNoise = barrierNoise;
        this.fluidLevelFloodednessNoise = fluidLevelFloodednessNoise;
        this.fluidLevelSpreadNoise = fluidLevelSpreadNoise;
        this.lavaNoise = lavaNoise;
        this.random = random;
        this.globalFluidPicker = globalFluidPicker;
        
        this.minGridX = this.gridX(pos.getMinBlockX()) - 1;
        int gridX = this.gridX(pos.getMaxBlockX()) + 1;
        this.gridSizeX = gridX - this.minGridX + 1;
        
        this.minGridY = this.gridY(minY) - 1;
        int gridY = this.gridY(minY + height) + 1;
        int gridSizeY = gridY - this.minGridY + 1;
        
        this.minGridZ = this.gridZ(pos.getMinBlockZ()) - 1;
        int gridZ = this.gridZ(pos.getMaxBlockZ()) + 1;
        this.gridSizeZ = gridZ - this.minGridZ + 1;
        
        int gridSize = this.gridSizeX * gridSizeY * this.gridSizeZ;
        this.aquiferCache = new FluidStatus[gridSize];
        this.aquiferLocationCache = new long[gridSize];
        Arrays.fill(this.aquiferLocationCache, Long.MAX_VALUE);
    }
    
    protected int getIndex(int gridX, int gridY, int gridZ) {
        int x = gridX - this.minGridX;
        int y = gridY - this.minGridY;
        int z = gridZ - this.minGridZ;
        return (y * this.gridSizeZ + z) * this.gridSizeX + x;
    }
    
    @Override
    public BlockState computeState(BaseStoneSource source, int x, int y, int z, double density) {
        if (density > 0.0) {
            this.shouldScheduleFluidUpdate = false;
            return source.getBaseBlock(x, y, z);
        }
        
        FluidStatus globalFluid = this.globalFluidPicker.computeFluid(x, y, z);
        if (globalFluid.at(y).is(Blocks.LAVA)) {
            this.shouldScheduleFluidUpdate = false;
            return Blocks.LAVA.defaultBlockState();
        }
        
        int startX = Math.floorDiv(x - 5, X_SPACING);
        int startY = Math.floorDiv(y + 1, Y_SPACING);
        int startZ = Math.floorDiv(z - 5, Z_SPACING);
        
        int dist1 = Integer.MAX_VALUE;
        int dist2 = Integer.MAX_VALUE;
        int dist3 = Integer.MAX_VALUE;
        
        long pos1 = 0L;
        long pos2 = 0L;
        long pos3 = 0L;
        
        for (int xOffset = 0; xOffset <= 1; xOffset++) {
            for (int yOffset = -1; yOffset <= 1; yOffset++) {
                for (int zOffset = 0; zOffset <= 1; zOffset++) {
                    int localX = startX + xOffset;
                    int localY = startY + yOffset;
                    int localZ = startZ + zOffset;
                    
                    int index = this.getIndex(localX, localY, localZ);
                    long cachedPos = this.aquiferLocationCache[index];
                    
                    long sourcePos;
                    if (cachedPos != Long.MAX_VALUE) {
                        sourcePos = cachedPos;
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
                        pos3 = pos2;
                        pos2 = pos1;
                        pos1 = sourcePos;
                        
                        dist3 = dist2;
                        dist2 = dist1;
                        dist1 = distance;
                    } else if (dist2 >= distance) {
                        pos3 = pos2;
                        pos2 = sourcePos;
                        
                        dist3 = dist2;
                        dist2 = distance;
                    } else if (dist3 >= distance) {
                        pos3 = sourcePos;
                        dist3 = distance;
                    }
                }
            }
        }
        
        FluidStatus aquifer1 = this.getAquiferStatus(pos1);
        double similarity1to2 = similarity(dist1, dist2);
        BlockState state = aquifer1.at(y);
        
        if (similarity1to2 <= 0.0) {
            this.shouldScheduleFluidUpdate = similarity1to2 >= FLOWING_UPDATE_SIMILARITY;
            return state;
        } else if (state.is(Blocks.WATER) && this.globalFluidPicker.computeFluid(x, y - 1, z).at(y - 1).is(Blocks.LAVA)) {
            this.shouldScheduleFluidUpdate = true;
            return state;
        } else {
            MutableDouble substance = new MutableDouble(Double.NaN);
            FluidStatus aquifer2 = this.getAquiferStatus(pos2);
            double pressure1 = similarity1to2 * this.calculatePressure(x, y, z, substance, aquifer1, aquifer2);
            
            if (density + pressure1 > 0.0) {
                this.shouldScheduleFluidUpdate = false;
                return source.getBaseBlock(x, y, z);
            } else {
                FluidStatus aquifer3 = this.getAquiferStatus(pos3);
                double similarity1to3 = similarity(dist1, dist3);
                
                if (similarity1to3 > 0.0) {
                    double pressure2 = similarity1to2 * similarity1to3 * this.calculatePressure(x, y, z, substance, aquifer1, aquifer3);
                    
                    if (density + pressure2 > 0.0) {
                        this.shouldScheduleFluidUpdate = false;
                        return source.getBaseBlock(x, y, z);
                    }
                }
                
                double similarity2to3 = similarity(dist2, dist3);
                if (similarity2to3 > 0.0) {
                    double pressure3 = similarity1to2 * similarity2to3 * this.calculatePressure(x, y, z, substance, aquifer2, aquifer3);
                    
                    if (density + pressure3 > 0.0) {
                        this.shouldScheduleFluidUpdate = false;
                        return source.getBaseBlock(x, y, z);
                    }
                }
                
                this.shouldScheduleFluidUpdate = true;
                return state;
            }
        }
    }
    
    @Override
    public boolean shouldScheduleFluidUpdate() {
        return this.shouldScheduleFluidUpdate;
    }
    
    protected static double similarity(int firstDistance, int secondDistance) {
        return 1.0 - (double) Math.abs(secondDistance - firstDistance) / 25.0;
    }
    
    private double calculatePressure(int x, int y, int z, MutableDouble substance, FluidStatus firstFluid, FluidStatus secondFluid) {
        BlockState firstBlock = firstFluid.at(y);
        BlockState secondBlock = secondFluid.at(y);
        
        if (!(firstBlock.is(Blocks.LAVA) && secondBlock.is(Blocks.WATER)) && !(firstBlock.is(Blocks.WATER) && secondBlock.is(Blocks.LAVA))) {
            int levelDifference = Math.abs(firstFluid.fluidLevel - secondFluid.fluidLevel);
            if (levelDifference == 0) {
                return 0.0;
            }
            
            double averageLevel = 0.5 * (double) (firstFluid.fluidLevel + secondFluid.fluidLevel);
            double yOffset = (double) y + 0.5 - averageLevel;
            double halfDiff = (double) levelDifference / 2.0;
            double offsetDiff = halfDiff - Math.abs(yOffset);
            
            double pressure;
            if (yOffset > 0.0) {
                double offset = 0.0 + offsetDiff;
                pressure = offset > 0.0 ? offset / 1.5 : offset / 2.5;
            } else {
                double offset = 3.0 + offsetDiff;
                pressure = offset > 0.0 ? offset / 3.0 : offset / 10.0;
            }
            
            double noise = 0.0;
            if (pressure >= -2.0 && pressure <= 2.0) {
                noise = substance.getValue();
                if (Double.isNaN(noise)) {
                    double barrierNoise = this.barrierNoise.getValue(x, y, z);
                    substance.setValue(barrierNoise);
                    noise = barrierNoise;
                }
            }
            
            return 2.0 * (noise + pressure);
        }
        
        return 2.0;
    }
    
    protected int gridX(int x) {
        return Math.floorDiv(x, X_SPACING);
    }
    
    protected int gridY(int y) {
        return Math.floorDiv(y, Y_SPACING);
    }
    
    protected int gridZ(int z) {
        return Math.floorDiv(z, Z_SPACING);
    }
    
    private FluidStatus getAquiferStatus(long packedPos) {
        int x = BlockPos.getX(packedPos);
        int y = BlockPos.getY(packedPos);
        int z = BlockPos.getZ(packedPos);
        
        int gridX = this.gridX(x);
        int gridY = this.gridY(y);
        int gridZ = this.gridZ(z);
        
        int index = this.getIndex(gridX, gridY, gridZ);
        FluidStatus cached = this.aquiferCache[index];
        
        if (cached != null) {
            return cached;
        } else {
            FluidStatus fluid = this.computeFluid(x, y, z);
            this.aquiferCache[index] = fluid;
            return fluid;
        }
    }
    
    private FluidStatus computeFluid(int x, int y, int z) {
        FluidStatus initialFluid = this.globalFluidPicker.computeFluid(x, y, z);
        int minSurfaceLevel = Integer.MAX_VALUE;
        int upperBoundY = y + 12;
        int lowerBoundY = y - 12;
        boolean isCenterOffset = false;
        
        for (int[] offsets : SURFACE_SAMPLING_OFFSETS_IN_CHUNKS) {
            int xOffset = x + SectionPos.sectionToBlockCoord(offsets[0]);
            int zOffset = z + SectionPos.sectionToBlockCoord(offsets[1]);
            int preliminarySurfaceLevel = this.sampler.getPreliminarySurfaceLevel(xOffset, y, zOffset);
            int adjustedSurfaceLevel = preliminarySurfaceLevel + 8;
            boolean isCenter = offsets[0] == 0 && offsets[1] == 0;
            
            if (isCenter && lowerBoundY > adjustedSurfaceLevel) {
                return initialFluid;
            }
            
            boolean isAboveSurface = upperBoundY > adjustedSurfaceLevel;
            if (isAboveSurface || isCenter) {
                FluidStatus surfaceFluid = this.globalFluidPicker.computeFluid(xOffset, adjustedSurfaceLevel, zOffset);
                if (!surfaceFluid.at(adjustedSurfaceLevel).isAir()) {
                    if (isCenter) {
                        isCenterOffset = true;
                    }
                    
                    if (isAboveSurface) {
                        return surfaceFluid;
                    }
                }
            }
            
            minSurfaceLevel = Math.min(minSurfaceLevel, preliminarySurfaceLevel);
        }
        
        int surfaceLevel = this.computeSurfaceLevel(x, y, z, initialFluid, minSurfaceLevel, isCenterOffset);
        return new FluidStatus(surfaceLevel, this.computeFluidType(x, y, z, initialFluid, surfaceLevel));
    }
    
    private int computeSurfaceLevel(int x, int y, int z, FluidStatus aquifer, int surfaceLevel, boolean isCenterOffset) {
        int levelDiff = surfaceLevel + 8 - y;
        double centerOffsetFactor = isCenterOffset ? MathUtils.clampedMap(levelDiff, 0.0, 64.0, 1.0, 0.0) : 0.0;
        double noise = Mth.clamp(this.fluidLevelFloodednessNoise.getValue(x, y, z), -1.0, 1.0);
        double h = MathUtils.map(centerOffsetFactor, 1.0, 0.0, -0.3, 0.8);
        double o = MathUtils.map(centerOffsetFactor, 1.0, 0.0, -0.8, 0.4);
        double d = noise - o;
        double e = noise - h;
        
        int p;
        if (e > 0.0) {
            p = aquifer.fluidLevel;
        } else if (d > 0.0) {
            p = this.computeRandomizedFluidSurfaceLevel(x, y, z, surfaceLevel);
        } else {
            p = MathUtils.WAY_BELOW_MIN_Y;
        }
        
        return p;
    }
    
    private int computeRandomizedFluidSurfaceLevel(int x, int y, int z, int surfaceLevel) {
        int startX = Math.floorDiv(x, 16);
        int startY = Math.floorDiv(y, 40);
        int startZ = Math.floorDiv(z, 16);
        int r = startY * 40 + 20;
        double levelSpreadNoise = this.fluidLevelSpreadNoise.getValue(startX, startY, startZ) * 10.0;
        int t = MathUtils.quantize(levelSpreadNoise, 3);
        int u = r + t;
        return Math.min(surfaceLevel, u);
    }
    
    private BlockState computeFluidType(int x, int y, int z, FluidStatus aquifer, int surfaceLevel) {
        BlockState state = aquifer.fluidType;
        if (surfaceLevel <= -10 && surfaceLevel != MathUtils.WAY_BELOW_MIN_Y && aquifer.fluidType != Blocks.LAVA.defaultBlockState()) {
            int startX = Math.floorDiv(x, 64);
            int startY = Math.floorDiv(y, 40);
            int startZ = Math.floorDiv(z, 64);
            double lavaNoise = this.lavaNoise.getValue(startX, startY, startZ);
            
            if (Math.abs(lavaNoise) > 0.3) {
                state = Blocks.LAVA.defaultBlockState();
            }
        }
        
        return state;
    }
}