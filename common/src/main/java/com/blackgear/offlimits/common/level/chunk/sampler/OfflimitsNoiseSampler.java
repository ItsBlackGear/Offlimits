package com.blackgear.offlimits.common.level.chunk.sampler;

import com.blackgear.offlimits.common.level.chunk.TerrainContext;
import com.blackgear.offlimits.common.level.chunk.noise.BlendedNoise;
import com.blackgear.offlimits.common.level.chunk.noisemodifiers.NoiseModifier;
import com.blackgear.offlimits.common.level.chunk.noise.NoiseSettingsExtension;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.jetbrains.annotations.Nullable;

public class OfflimitsNoiseSampler extends NoiseSampler {
    private final NoiseModifier noiseModifier;
    
    public OfflimitsNoiseSampler(BiomeSource biomeSource, TerrainContext context, NoiseSettings settings, BlendedNoise blendedNoise, @Nullable SimplexNoise islandNoise, PerlinNoise depthNoise, NoiseModifier noiseModifier) {
        super(biomeSource, context, settings, blendedNoise, islandNoise, depthNoise);
        this.noiseModifier = noiseModifier;
    }
    
    @Override
    public void fillNoiseColumn(double[] buffer, int x, int z, NoiseSettings settings, int seaLevel, int minY, int chunkCountY) {
        double terrainOffset;
        double terrainFactor;
        
        if (this.islandNoise != null) {
            terrainOffset = TheEndBiomeSource.getHeightValue(this.islandNoise, x, z) - 8.0F;
            terrainFactor = terrainOffset > 0.0 ? 0.25 : 1.0;
        } else {
            double[] offsetAndFactor = this.getOffsetAndFactor(x, seaLevel, z);
            terrainOffset = offsetAndFactor[0];
            terrainFactor = offsetAndFactor[1];
        }
        
        double xzScale = 684.412 * settings.noiseSamplingSettings().xzScale();
        double yScale = 684.412 * settings.noiseSamplingSettings().yScale();
        double xzFactor = xzScale / settings.noiseSamplingSettings().xzFactor();
        double yFactor = yScale / settings.noiseSamplingSettings().yFactor();
        double density = settings.randomDensityOffset() ? this.getRandomDensity(x, z) : 0.0;
        
        for(int i = 0; i <= chunkCountY; ++i) {
            int y = i + minY;
            double height = y * this.context.chunkHeight();
            double noise = this.blendedNoise.sampleAndClampNoise(x, y, z, xzScale, yScale, xzFactor, yFactor);
            double initialDensity = this.computeInitialDensity(height, terrainOffset, terrainFactor, density) + noise;
            initialDensity = this.noiseModifier.modifyNoise(initialDensity, x * this.context.chunkWidth(), y * this.context.chunkHeight(), z * this.context.chunkWidth());
            initialDensity = this.applySlide(initialDensity, y);
            buffer[i] = initialDensity;
        }
    }
    
    private double computeInitialDensity(double height, double terrainOffset, double terrainFactor, double density) {
        double dimensionDensity = computeDimensionDensity(this.dimensionDensityFactor, this.dimensionDensityOffset, height, density);
        double baseDensity = (dimensionDensity + terrainOffset) * terrainFactor;
        return baseDensity * (double) (baseDensity > 0.0 ? 4 : 1);
    }
    
    public static double computeDimensionDensity(double densityFactor, double densityOffset, double height, double density) {
        double baseDensity = 1.0 - height / 128.0 + density;
        return baseDensity * densityFactor + densityOffset;
    }
    
    private double applySlide(double density, int y) {
        int startY = Mth.intFloorDiv(((NoiseSettingsExtension) this.settings).minY(), this.context.chunkHeight());
        int relativeY = y - startY;
        
        if (this.topSlideSize > 0.0) {
            double slideFactor = ((double) (this.context.chunkCountY() - relativeY) - this.topSlideOffset) / this.topSlideSize;
            density = Mth.clampedLerp(this.topSlideTarget, density, slideFactor);
        }
        
        if (this.bottomSlideSize > 0.0) {
            double slideFactor = ((double) relativeY - this.bottomSlideOffset) / this.bottomSlideSize;
            density = Mth.clampedLerp(this.bottomSlideTarget, density, slideFactor);
        }
        
        return density;
    }
    
    private double getRandomDensity(int x, int z) {
        double depthNoise = this.depthNoise.getValue(x * 200, 10.0, z * 200, 1.0, 0.0, true);
        double depth = depthNoise < 0.0 ? -depthNoise * 0.3 : depthNoise;
        double density = depth * 24.575625 - 2.0;
        return density < 0.0 ? density * 0.009486607142857142 : Math.min(density, 1.0) * 0.006640625;
    }
    
    public int getPreliminarySurfaceLevel(int x, int y, int z) {
        int chunkX = Math.floorDiv(x, this.context.chunkWidth());
        int chunkZ = Math.floorDiv(z, this.context.chunkWidth());
        
        int chunkY = Mth.intFloorDiv(((NoiseSettingsExtension) this.settings).minY(), this.context.chunkHeight());
        int maxHeight = Mth.intFloorDiv(this.settings.height(), this.context.chunkHeight());
        
        int minSurfaceLevel = Integer.MAX_VALUE;
        
        for(int localX = chunkX - 2; localX <= chunkX + 2; localX += 2) {
            for(int localZ = chunkZ - 2; localZ <= chunkZ + 2; localZ += 2) {
                double[] offsetAndFactor = this.getOffsetAndFactor(localX, y, localZ);
                double terrainOffset = offsetAndFactor[0];
                double terrainFactor = offsetAndFactor[1];
                
                for(int localY = chunkY; localY <= chunkY + maxHeight; ++localY) {
                    int relativeY = localY - chunkY;
                    double height = localY * this.context.chunkHeight();
                    double density = this.computeInitialDensity(height, terrainOffset, terrainFactor, 0.0) - 90.0;
                    double slide = this.applySlide(density, relativeY);
                    
                    if (this.isAbovePreliminarySurfaceLevel(slide)) {
                        minSurfaceLevel = Math.min(localY * this.context.chunkHeight(), minSurfaceLevel);
                        break;
                    }
                }
            }
        }
        
        return minSurfaceLevel;
    }
    
    @Override
    public double[] getOffsetAndFactor(int x, int y, int z) {
        float totalScale = 0.0F;
        float totalDepth = 0.0F;
        float totalWeight = 0.0F;
        
        float baseDepth = this.biomeSource.getNoiseBiome(x, y, z).getDepth();
        
        for(int offsetX = -2; offsetX <= 2; ++offsetX) {
            for(int offsetZ = -2; offsetZ <= 2; ++offsetZ) {
                Biome biome = this.biomeSource.getNoiseBiome(x + offsetX, y, z + offsetZ);
                float depth = biome.getDepth();
                float scale = biome.getScale();
                float realDepth = this.settings.isAmplified() && depth > 0.0F ? 1.0F + depth * 2.0F : depth;
                float realScale = this.settings.isAmplified() && scale > 0.0F ? 1.0F + scale * 4.0F : scale;
                float depthWeight = depth > baseDepth ? 0.5F : 1.0F;
                float weightContribution = depthWeight * BIOME_WEIGHTS[offsetX + 2 + (offsetZ + 2) * 5] / (realDepth + 2.0F);
                
                totalScale += realScale * weightContribution;
                totalDepth += realDepth * weightContribution;
                totalWeight += weightContribution;
            }
        }
        
        float meanDepth = totalDepth / totalWeight;
        float meanScale = totalScale / totalWeight;
        double depthOffset = meanDepth * 0.5F - 0.125F;
        double scaleFactor = meanScale * 0.9F + 0.1F;
        
        double terrainOffset = depthOffset * 0.265625;
        double terrainFactor = 96.0 / scaleFactor;
        return new double[] {terrainOffset, terrainFactor};
    }
}