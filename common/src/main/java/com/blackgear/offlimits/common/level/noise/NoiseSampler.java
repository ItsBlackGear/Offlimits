package com.blackgear.offlimits.common.level.noise;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.jetbrains.annotations.Nullable;

public class NoiseSampler {
    private static final float[] BIOME_WEIGHTS = Util.make(new float[25], weights -> {
        for(int x = -2; x <= 2; ++x) {
            for(int z = -2; z <= 2; ++z) {
                float distance = 10.0F / Mth.sqrt((float)(x * x + z * z) + 0.2F);
                weights[x + 2 + (z + 2) * 5] = distance;
            }
        }
    });
    
    private final BiomeSource biomeSource;
    private final int chunkWidth;
    private final int chunkHeight;
    private final int chunkCountY;
    private final NoiseSettings noiseSettings;
    private final BlendedNoise blendedNoise;
    private final @Nullable SimplexNoise islandNoise;
    private final PerlinNoise depthNoise;
    private final double topSlideTarget;
    private final double topSlideSize;
    private final double topSlideOffset;
    private final double bottomSlideTarget;
    private final double bottomSlideSize;
    private final double bottomSlideOffset;
    private final double dimensionDensityFactor;
    private final double dimensionDensityOffset;
    private final NoiseModifier caveNoiseModifier;
    private final int minY = Offlimits.CONFIG.worldGenMinY.get();
    
    public NoiseSampler(
        BiomeSource biomeSource,
        int chunkWidth,
        int chunkHeight,
        int chunkCountY,
        NoiseSettings settings,
        BlendedNoise blendedNoise,
        @Nullable SimplexNoise islandNoise,
        PerlinNoise depthNoise,
        NoiseModifier caveNoiseModifier
    ) {
        this.biomeSource = biomeSource;
        this.chunkWidth = chunkWidth;
        this.chunkHeight = chunkHeight;
        this.chunkCountY = chunkCountY;
        this.noiseSettings = settings;
        this.blendedNoise = blendedNoise;
        this.islandNoise = islandNoise;
        this.depthNoise = depthNoise;
        this.topSlideTarget = settings.topSlideSettings().target();
        this.topSlideSize = settings.topSlideSettings().size();
        this.topSlideOffset = settings.topSlideSettings().offset();
        this.bottomSlideTarget = settings.bottomSlideSettings().target();
        this.bottomSlideSize = settings.bottomSlideSettings().size();
        this.bottomSlideOffset = settings.bottomSlideSettings().offset();
        this.dimensionDensityFactor = settings.densityFactor();
        this.dimensionDensityOffset = settings.densityOffset();
        this.caveNoiseModifier = caveNoiseModifier;
    }
    
    public void fillNoiseColumn(double[] slices, int x, int z, NoiseSettings noiseSettings, int seaLevel, int minY, int chunkCountY) {
        double offset;
        double factor;
        
        if (this.islandNoise != null) {
            offset = TheEndBiomeSource.getHeightValue(this.islandNoise, x, z) - 8.0F;
            factor = offset > 0.0 ? 0.25 : 1.0;
        } else {
            int chunkX = x * this.chunkWidth >> 2;
            int chunkZ = z * this.chunkWidth >> 2;
            double[] offsetAndFactor = this.getOffsetAndFactor(chunkX, seaLevel, chunkZ);
            offset = offsetAndFactor[0];
            factor = offsetAndFactor[1];
        }
        
        double xzScale = 684.412 * noiseSettings.noiseSamplingSettings().xzScale();
        double yScale = 684.412 * noiseSettings.noiseSamplingSettings().yScale();
        double xzFactor = xzScale / noiseSettings.noiseSamplingSettings().xzFactor();
        double yFactor = yScale / noiseSettings.noiseSamplingSettings().yFactor();
        double density = noiseSettings.randomDensityOffset() ? this.getRandomDensity(x, z) : 0.0;
        
        for(int i = 0; i <= chunkCountY; ++i) {
            int y = i + minY;
            double height = y * this.chunkHeight;
            double noise = this.blendedNoise.sampleAndClampNoise(x, y, z, xzScale, yScale, xzFactor, yFactor);
            double initialDensity = this.computeInitialDensity(height, offset, factor, density) + noise;
            initialDensity = this.caveNoiseModifier.modifyNoise(initialDensity, x * this.chunkWidth, y * this.chunkHeight, z * this.chunkWidth);
            initialDensity = this.applySlide(initialDensity, y);
            slices[i] = initialDensity;
        }
    }
    
    private double computeInitialDensity(double height, double offset, double factor, double density) {
        double dimensionDensity = computeDimensionDensity(this.dimensionDensityFactor, this.dimensionDensityOffset, height, density);
        double initialDensity = (dimensionDensity + offset) * factor;
        return initialDensity * (double) (initialDensity > 0.0 ? 4 : 1);
    }
    
    public static double computeDimensionDensity(double densityFactor, double densityOffset, double height, double density) {
        double dimensionDensity = 1.0 - height / 128.0 + density;
        return dimensionDensity * densityFactor + densityOffset;
    }
    
    private double applySlide(double density, int y) {
        int chunkY = Mth.intFloorDiv(this.minY, this.chunkHeight);
        int realY = y - chunkY;
        if (this.topSlideSize > 0.0) {
            double slideFactor = ((double) (this.chunkCountY - realY) - this.topSlideOffset) / this.topSlideSize;
            density = Mth.clampedLerp(this.topSlideTarget, density, slideFactor);
        }
        
        if (this.bottomSlideSize > 0.0) {
            double slideFactor = ((double) realY - this.bottomSlideOffset) / this.bottomSlideSize;
            density = Mth.clampedLerp(this.bottomSlideTarget, density, slideFactor);
        }
        
        return density;
    }
    
    private double getRandomDensity(int x, int z) {
        double depth = this.depthNoise.getValue(x * 200, 10.0, z * 200, 1.0, 0.0, true);
        double absoluteValue = depth < 0.0 ? -depth * 0.3 : depth;
        double adjustedValue = absoluteValue * 24.575625 - 2.0;
        return adjustedValue < 0.0 ? adjustedValue * 0.009486607142857142 : Math.min(adjustedValue, 1.0) * 0.006640625;
    }
    
    public int getPreliminarySurfaceLevel(int x, int y, int z) {
        int noiseX = Math.floorDiv(x, this.chunkWidth);
        int noiseZ = Math.floorDiv(z, this.chunkWidth);
        int chunkY = Mth.intFloorDiv(this.minY, this.chunkHeight);
        int noiseY = Mth.intFloorDiv(this.noiseSettings.height(), this.chunkHeight);
        int minSurfaceLevel = Integer.MAX_VALUE;
        
        for(int localX = noiseX - 2; localX <= noiseX + 2; localX += 2) {
            for(int localZ = noiseZ - 2; localZ <= noiseZ + 2; localZ += 2) {
                int chunkX = localX * this.chunkWidth >> 2;
                int chunkZ = localZ * this.chunkWidth >> 2;
                double[] offsetAndFactor = this.getOffsetAndFactor(chunkX, y, chunkZ);
                double offset = offsetAndFactor[0];
                double factor = offsetAndFactor[1];
                
                for(int localY = chunkY; localY <= chunkY + noiseY; ++localY) {
                    int realY = localY - chunkY;
                    double height = localY * this.chunkHeight;
                    double density = this.computeInitialDensity(height, offset, factor, 0.0) - 90.0;
                    double slide = this.applySlide(density, realY);
                    
                    if (this.isAbovePreliminarySurfaceLevel(slide)) {
                        minSurfaceLevel = Math.min(localY * this.chunkHeight, minSurfaceLevel);
                        break;
                    }
                }
            }
        }
        
        return minSurfaceLevel;
    }
    
    private boolean isAbovePreliminarySurfaceLevel(double slide) {
        return slide < 50.0;
    }
    
    public double[] getOffsetAndFactor(int x, int y, int z) {
        float scale = 0.0F;
        float depth = 0.0F;
        float weight = 0.0F;
        float baseDepth = this.biomeSource.getNoiseBiome(x, y, z).getDepth();
        
        for(int localX = -2; localX <= 2; ++localX) {
            for(int localZ = -2; localZ <= 2; ++localZ) {
                Biome biome = this.biomeSource.getNoiseBiome(x + localX, y, z + localZ);
                float localDepth = biome.getDepth();
                float localScale = biome.getScale();
                float realDepth = this.noiseSettings.isAmplified() && localDepth > 0.0F ? 1.0F + localDepth * 2.0F : localDepth;
                float realScale = this.noiseSettings.isAmplified() && localScale > 0.0F ? 1.0F + localScale * 4.0F : localScale;
                float depthWeight = localDepth > baseDepth ? 0.5F : 1.0F;
                float weightContribution = depthWeight * BIOME_WEIGHTS[localX + 2 + (localZ + 2) * 5] / (realDepth + 2.0F);
                
                scale += realScale * weightContribution;
                depth += realDepth * weightContribution;
                weight += weightContribution;
            }
        }
        
        float meanDepth = depth / weight;
        float meanScale = scale / weight;
        double depthOffset = meanDepth * 0.5F - 0.125F;
        double scaleFactor = meanScale * 0.9F + 0.1F;
        
        double offset = depthOffset * 0.265625;
        double factor = 96.0 / scaleFactor;
        return new double[]{offset, factor};
    }
}