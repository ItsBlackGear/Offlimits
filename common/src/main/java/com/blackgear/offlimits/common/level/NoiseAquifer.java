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
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.apache.commons.lang3.mutable.MutableDouble;

import java.util.Arrays;
import java.util.Random;

public class NoiseAquifer implements Aquifer {
    private static final int PACKED_X_LENGTH = 1 + Mth.log2(Mth.smallestEncompassingPowerOfTwo(30000000));
    private static final int PACKED_Z_LENGTH = PACKED_X_LENGTH;
    public static final int PACKED_Y_LENGTH = 64 - PACKED_X_LENGTH - PACKED_Z_LENGTH;
    public static final int BITS_FOR_Y = PACKED_Y_LENGTH;
    public static final int Y_SIZE = (1 << BITS_FOR_Y) - 32;
    public static final int MAX_Y = (Y_SIZE >> 1) - 1;
    public static final int MIN_Y = MAX_Y - Y_SIZE + 1;
    public static final int WAY_BELOW_MIN_Y = MIN_Y << 4;
    
    private static final double FLOWING_UPDATE_SIMULARITY = similarity(10 * 10, 12 * 12);
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
        {-2, -1},
        {-1, -1},
        {0, -1},
        {1, -1},
        {-3, 0},
        {-2, 0},
        {-1, 0},
        {0, 0},
        {1, 0},
        {-2, 1},
        {-1, 1},
        {0, 1},
        {1, 1}
    };
    
    public NoiseAquifer(
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
    public BlockState computeState(BaseStoneSource source, int x, int y, int z, double substance) {
        if (substance > 0.0) {
            this.shouldScheduleFluidUpdate = false;
            return source.getBaseBlock(x, y, z);
        } else {
            FluidStatus fluidStatus = this.globalFluidPicker.computeFluid(x, y, z);
            if (fluidStatus.at(y).is(Blocks.LAVA)) {
                this.shouldScheduleFluidUpdate = false;
                return Blocks.LAVA.defaultBlockState();
            } else {
                int l = Math.floorDiv(x - 5, 16);
                int m = Math.floorDiv(y + 1, 12);
                int n = Math.floorDiv(z - 5, 16);
                int firstDistance = Integer.MAX_VALUE;
                int secondDistance = Integer.MAX_VALUE;
                int thirdDistance = Integer.MAX_VALUE;
                long firstSource = 0L;
                long secondSource = 0L;
                long thirdSource = 0L;
                
                int localX, localY, localZ, deltaX, deltaY, deltaZ, distance;
                long sourcePos;
                
                for (int offsetX = 0; offsetX <= 1; offsetX++) {
                    for (int offsetY = -1; offsetY <= 1; offsetY++) {
                        for (int offsetZ = 0; offsetZ <= 1; offsetZ++) {
                            localX = l + offsetX;
                            localY = m + offsetY;
                            localZ = n + offsetZ;
                            
                            int index = this.getIndex(localX, localY, localZ);
                            long cache = this.aquiferLocationCache[index];
                            
                            if (cache != Long.MAX_VALUE) {
                                sourcePos = cache;
                            } else {
                                this.random.setSeed(Mth.getSeed(localX, localY, localZ));
                                sourcePos = BlockPos.asLong(
                                    localX * 16 + this.random.nextInt(10),
                                    localY * 12 + this.random.nextInt(9),
                                    localZ * 16 + this.random.nextInt(10)
                                );
                                this.aquiferLocationCache[index] = sourcePos;
                            }
                            
                            deltaX = BlockPos.getX(sourcePos) - x;
                            deltaY = BlockPos.getY(sourcePos) - y;
                            deltaZ = BlockPos.getZ(sourcePos) - z;
                            
                            distance = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
                            
                            if (firstDistance >= distance) {
                                thirdSource = secondSource;
                                secondSource = firstSource;
                                firstSource = sourcePos;
                                thirdDistance = secondDistance;
                                secondDistance = firstDistance;
                                firstDistance = distance;
                            } else if (secondDistance >= distance) {
                                thirdSource = secondSource;
                                secondSource = sourcePos;
                                thirdDistance = secondDistance;
                                secondDistance = distance;
                            } else if (thirdDistance >= distance) {
                                thirdSource = sourcePos;
                                thirdDistance = distance;
                            }
                        }
                    }
                }
                
                FluidStatus firstFluid = this.getAquiferStatus(firstSource);
                double firstToSecond = similarity(firstDistance, secondDistance);
                BlockState fluidState = firstFluid.at(y);
                
                if (firstToSecond <= 0.0) {
                    this.shouldScheduleFluidUpdate = firstToSecond >= FLOWING_UPDATE_SIMULARITY;
                    return fluidState;
                } else if (fluidState.is(Blocks.WATER) && this.globalFluidPicker.computeFluid(x, y - 1, z).at(y - 1).is(Blocks.LAVA)) {
                    this.shouldScheduleFluidUpdate = true;
                    return fluidState;
                } else {
                    MutableDouble density = new MutableDouble(Double.NaN);
                    FluidStatus secondFluid = this.getAquiferStatus(secondSource);
                    double firstPressure = firstToSecond * this.calculatePressure(x, y, z, density, firstFluid, secondFluid);
                    
                    if (substance + firstPressure > 0.0) {
                        this.shouldScheduleFluidUpdate = false;
                        return source.getBaseBlock(x, y, z);
                    } else {
                        FluidStatus thirdFluid = this.getAquiferStatus(thirdSource);
                        double firstToThird = similarity(firstDistance, thirdDistance);
                        if (firstToThird > 0.0) {
                            double secondPressure = firstToSecond * firstToThird * this.calculatePressure(x, y, z, density, firstFluid, thirdFluid);
                            if (substance + secondPressure > 0.0) {
                                this.shouldScheduleFluidUpdate = false;
                                return source.getBaseBlock(x, y, z);
                            }
                        }
                        
                        double secondToThird = similarity(secondDistance, thirdDistance);
                        if (secondToThird > 0.0) {
                            double thirdPressure = firstToSecond * secondToThird * this.calculatePressure(x, y, z, density, secondFluid, thirdFluid);
                            if (substance + thirdPressure > 0.0) {
                                this.shouldScheduleFluidUpdate = false;
                                return source.getBaseBlock(x, y, z);
                            }
                        }
                        
                        this.shouldScheduleFluidUpdate = true;
                        return fluidState;
                    }
                }
            }
        }
    }
    
    @Override
    public boolean shouldScheduleFluidUpdate() {
        return this.shouldScheduleFluidUpdate;
    }
    
    protected static double similarity(int firstDistance, int secondDistance) {
        return 1.0 - (double)Math.abs(secondDistance - firstDistance) / 25.0;
    }
    
    private double calculatePressure(int x, int y, int z, MutableDouble substance, FluidStatus firstFluid, FluidStatus secondFluid) {
        BlockState blockState = firstFluid.at(y);
        BlockState blockState2 = secondFluid.at(y);
        
        if ((!blockState.is(Blocks.LAVA) || !blockState2.is(Blocks.WATER)) && (!blockState.is(Blocks.WATER) || !blockState2.is(Blocks.LAVA))) {
            int j = Math.abs(firstFluid.fluidLevel - secondFluid.fluidLevel);
            if (j == 0) {
                return 0.0;
            } else {
                double meanLevel = 0.5 * (double)(firstFluid.fluidLevel + secondFluid.fluidLevel);
                double distanceFromMean = (double)y + 0.5 - meanLevel;
                double f = (double)j / 2.0;
                double o = f - Math.abs(distanceFromMean);
                
                double q;
                if (distanceFromMean > 0.0) {
                    double p = 0.0 + o;
                    if (p > 0.0) {
                        q = p / 1.5;
                    } else {
                        q = p / 2.5;
                    }
                } else {
                    double p = 3.0 + o;
                    if (p > 0.0) {
                        q = p / 3.0;
                    } else {
                        q = p / 10.0;
                    }
                }
                
                double r;
                
                if (!(q < -2.0) && !(q > 2.0)) {
                    double s = substance.getValue();
                    if (Double.isNaN(s)) {
                        double t = this.barrierNoise.getValue(x, y, z);
                        substance.setValue(t);
                        r = t;
                    } else {
                        r = s;
                    }
                } else {
                    r = 0.0;
                }
                
                return 2.0 * (r + q);
            }
        } else {
            return 2.0;
        }
    }
    
    protected int gridX(int x) {
        return Math.floorDiv(x, 16);
    }
    
    protected int gridY(int y) {
        return Math.floorDiv(y, 12);
    }
    
    protected int gridZ(int z) {
        return Math.floorDiv(z, 16);
    }
    
    private FluidStatus getAquiferStatus(long packedPos) {
        int i = BlockPos.getX(packedPos);
        int j = BlockPos.getY(packedPos);
        int k = BlockPos.getZ(packedPos);
        int l = this.gridX(i);
        int m = this.gridY(j);
        int n = this.gridZ(k);
        int o = this.getIndex(l, m, n);
        FluidStatus fluidStatus = this.aquiferCache[o];
        if (fluidStatus != null) {
            return fluidStatus;
        } else {
            FluidStatus fluidStatus2 = this.computeFluid(i, j, k);
            this.aquiferCache[o] = fluidStatus2;
            return fluidStatus2;
        }
    }
    
    private FluidStatus computeFluid(int x, int y, int z) {
        FluidStatus fluidStatus = this.globalFluidPicker.computeFluid(x, y, z);
        int i = Integer.MAX_VALUE;
        int j = y + 12;
        int k = y - 12;
        boolean bl = false;
        
        for (int[] is : SURFACE_SAMPLING_OFFSETS_IN_CHUNKS) {
            int l = x + SectionPos.sectionToBlockCoord(is[0]);
            int m = z + SectionPos.sectionToBlockCoord(is[1]);
            int n = this.sampler.getPreliminarySurfaceLevel(l, y, m);
            int o = n + 8;
            boolean bl2 = is[0] == 0 && is[1] == 0;
            if (bl2 && k > o) {
                return fluidStatus;
            }
            
            boolean bl3 = j > o;
            if (bl3 || bl2) {
                FluidStatus fluidStatus2 = this.globalFluidPicker.computeFluid(l, o, m);
                if (!fluidStatus2.at(o).isAir()) {
                    if (bl2) {
                        bl = true;
                    }
                    
                    if (bl3) {
                        return fluidStatus2;
                    }
                }
            }
            
            i = Math.min(i, n);
        }
        
        int p = i + 8 - y;
        double d = bl ? MathUtils.clampedMap(p, 0.0, 64.0, 1.0, 0.0) : 0.0;
        double e = Mth.clamp(this.fluidLevelFloodednessNoise.getValue(x, y, z), -1.0, 1.0);
        double f = MathUtils.map(d, 1.0, 0.0, -0.3, 0.8);
        
        if (e > f) {
            return fluidStatus;
        } else {
            double g = MathUtils.map(d, 1.0, 0.0, -0.8, 0.4);
            if (e <= g) {
                return new FluidStatus(WAY_BELOW_MIN_Y, fluidStatus.fluidType);
            } else {
                int t = Math.floorDiv(x, 16);
                int u = Math.floorDiv(y, 40);
                int v = Math.floorDiv(z, 16);
                int w = u * 40 + 20;
                double h = this.fluidLevelSpreadNoise.getValue(t, u, v) * 10.0;
                int ab = MathUtils.quantize(h, 3);
                int ac = w + ab;
                int ad = Math.min(i, ac);
                
                if (ac <= -10) {
                    int ag = Math.floorDiv(x, 64);
                    int ah = Math.floorDiv(y, 40);
                    int ai = Math.floorDiv(z, 64);
                    double aj = this.lavaNoise.getValue(ag, ah, ai);
                    if (Math.abs(aj) > 0.3) {
                        return new FluidStatus(ad, Blocks.LAVA.defaultBlockState());
                    }
                }
                
                return new FluidStatus(ad, fluidStatus.fluidType);
            }
        }
    }
    
    public interface  FluidPicker {
        FluidStatus computeFluid(int x, int y, int z);
    }
    
    public static class FluidStatus {
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