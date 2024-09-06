package com.blackgear.offlimits.common.level;

import com.blackgear.offlimits.common.level.levelgen.noisemodifiers.NoiseModifier;
import com.blackgear.offlimits.common.utils.NoiseUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class Cavifier implements NoiseModifier {
    private final int minChunkY;
    private final NormalNoise layerNoiseSource;
    private final NormalNoise pillarNoiseSource;
    private final NormalNoise pillarRarenessModulator;
    private final NormalNoise pillarThicknessModulator;
    private final NormalNoise spaghetti2dNoiseSource;
    private final NormalNoise spaghetti2dElevationModulator;
    private final NormalNoise spaghetti2dRarityModulator;
    private final NormalNoise spaghetti2dThicknessModulator;
    private final NormalNoise spaghetti3dNoiseSource1;
    private final NormalNoise spaghetti3dNoiseSource2;
    private final NormalNoise spaghetti3dRarityModulator;
    private final NormalNoise spaghetti3dThicknessModulator;
    private final NormalNoise spaghettiRoughnessNoise;
    private final NormalNoise spaghettiRoughnessModulator;
    private final NormalNoise caveEntranceNoiseSource;
    private final NormalNoise cheeseNoiseSource;
    
    private static final int CHEESE_NOISE_RANGE = 128;
    private static final int SURFACE_DENSITY_THRESHOLD = 200;
    
    public Cavifier(WorldgenRandom random, int minChunkY) {
        this.minChunkY = minChunkY;
        this.pillarNoiseSource = NoiseUtils.normal(random.nextLong(), -7, 1.0, 1.0);
        this.pillarRarenessModulator = NoiseUtils.normal(random.nextLong(), -8, 1.0);
        this.pillarThicknessModulator = NoiseUtils.normal(random.nextLong(), -8, 1.0);
        this.spaghetti2dNoiseSource = NoiseUtils.normal(random.nextLong(), -7, 1.0);
        this.spaghetti2dElevationModulator = NoiseUtils.normal(random.nextLong(), -8, 1.0);
        this.spaghetti2dRarityModulator = NoiseUtils.normal(random.nextLong(), -11, 1.0);
        this.spaghetti2dThicknessModulator = NoiseUtils.normal(random.nextLong(), -11, 1.0);
        this.spaghetti3dNoiseSource1 = NoiseUtils.normal(random.nextLong(), -7, 1.0);
        this.spaghetti3dNoiseSource2 = NoiseUtils.normal(random.nextLong(), -7, 1.0);
        this.spaghetti3dRarityModulator = NoiseUtils.normal(random.nextLong(), -11, 1.0);
        this.spaghetti3dThicknessModulator = NoiseUtils.normal(random.nextLong(), -8, 1.0);
        this.spaghettiRoughnessNoise = NoiseUtils.normal(random.nextLong(), -5, 1.0);
        this.spaghettiRoughnessModulator = NoiseUtils.normal(random.nextLong(), -8, 1.0);
        this.caveEntranceNoiseSource = NoiseUtils.normal(random.nextLong(), -7, 0.4, 0.5, 1.0);
        this.layerNoiseSource = NoiseUtils.normal(random.nextLong(), -8, 1.0);
        this.cheeseNoiseSource = NoiseUtils.normal(random.nextLong(), -8, 0.5, 1.0, 2.0, 1.0, 2.0, 1.0, 0.0, 2.0, 0.0);
    }
    
    @Override
    public double modifyNoise(double density, int x, int y, int z) {
        boolean isBelowThreshold = density < SURFACE_DENSITY_THRESHOLD;
        
        double entrances = this.getBigEntrances(x, y, z);
        double spaghettiRoughness = this.spaghettiRoughness(x, y, z);
        double spaghetti3d = this.getSpaghetti3d(x, y, z);
        
        if (isBelowThreshold) {
            double caveStructure = Math.min(entrances, spaghetti3d + spaghettiRoughness);
            return Math.min(density, caveStructure * CHEESE_NOISE_RANGE * 5.0);
        } else {
            double cheeseNoise = this.cheeseNoiseSource.getValue(x, (double) y / 1.5, z);
            double clampedCheeseNoise = Mth.clamp(cheeseNoise + 0.75, -1.0, 1.0);
            double densityFactor = (density - SURFACE_DENSITY_THRESHOLD) / 100.0;
            double blendedNoise = clampedCheeseNoise + Mth.clampedLerp(0.5, 0.0, densityFactor);
            double layerizedCaverns = this.getLayerizedCaverns(x, y, z);
            double spaghetti2d = this.getSpaghetti2d(x, y, z);
            double cavernSpaghettiNoise = blendedNoise + layerizedCaverns;
            double spaghettiNoiseBlend = Math.min(spaghetti3d, spaghetti2d) + spaghettiRoughness;
            double caveStructure = this.min(cavernSpaghettiNoise, entrances, spaghettiNoiseBlend);
            double addPillars = Math.max(caveStructure, this.getPillars(x, y, z));
            return CHEESE_NOISE_RANGE * Mth.clamp(addPillars, -1.0, 1.0);
        }
    }
    
    private double getBigEntrances(int x, int y, int z) {
        double entranceNoise = this.caveEntranceNoiseSource.getValue((double) x * 0.75, (double) y * 0.5, (double) z * 0.75) + 0.37;
        double heightFactor = (double) (y + 10) / 40.0;
        return entranceNoise + Mth.clampedLerp(0.3, 0.0, heightFactor);
    }
    
    private double getPillars(int x, int y, int z) {
        double rareness = NoiseUtils.sampleNoiseAndMapToRange(this.pillarRarenessModulator, x, y, z, 0.0, 2.0);
        double thickness = NoiseUtils.sampleNoiseAndMapToRange(this.pillarThicknessModulator, x, y, z, 0.0, 1.1);
        thickness = Math.pow(thickness, 3.0);
        double noise = this.pillarNoiseSource.getValue((double) x * 25.0, (double) y * 0.3, (double) z * 25.0);
        noise = thickness * (noise * 2.0 - rareness);
        return noise > 0.03 ? noise : Double.NEGATIVE_INFINITY;
    }
    
    private double getLayerizedCaverns(int x, int y, int z) {
        double cavernNoise = this.layerNoiseSource.getValue(x, y * 8, z);
        return Mth.square((float) cavernNoise) * 4.0;
    }
    
    private double getSpaghetti3d(int x, int y, int z) {
        double rarityModulator = this.spaghetti3dRarityModulator.getValue(x * 2, y, z * 2);
        double spaghettiRarity = QuantizedSpaghettiRarity.getSpaghettiRarity3D(rarityModulator);
        double thicknessModulator = NoiseUtils.sampleNoiseAndMapToRange(this.spaghetti3dThicknessModulator, x, y, z, 0.065, 0.088);
        double noiseSample1 = sampleWithRarity(this.spaghetti3dNoiseSource1, x, y, z, spaghettiRarity);
        double noiseFactor1 = Math.abs(spaghettiRarity * noiseSample1) - thicknessModulator;
        double noiseSample2 = sampleWithRarity(this.spaghetti3dNoiseSource2, x, y, z, spaghettiRarity);
        double noiseFactor2 = Math.abs(spaghettiRarity * noiseSample2) - thicknessModulator;
        return clampToUnit(Math.max(noiseFactor1, noiseFactor2));
    }
    
    private double getSpaghetti2d(int x, int y, int z) {
        double rarityModulator = this.spaghetti2dRarityModulator.getValue(x * 2, y, z * 2);
        double spaghettiRarity = QuantizedSpaghettiRarity.getSpaghettiRarity2D(rarityModulator);
        double thicknessModulator = NoiseUtils.sampleNoiseAndMapToRange(this.spaghetti2dThicknessModulator, x * 2, y, z * 2, 0.6, 1.3);
        double noiseSample = sampleWithRarity(this.spaghetti2dNoiseSource, x, y, z, spaghettiRarity);
        double noiseFactor = Math.abs(spaghettiRarity * noiseSample) - 0.083 * thicknessModulator;
        double elevationModulator = NoiseUtils.sampleNoiseAndMapToRange(this.spaghetti2dElevationModulator, x, 0.0, z, this.minChunkY, 8.0);
        double elevationFactor = Math.abs(elevationModulator - (double)y / 8.0) - thicknessModulator;
        elevationFactor = elevationFactor * elevationFactor * elevationFactor;
        return clampToUnit(Math.max(elevationFactor, noiseFactor));
    }
    
    private double spaghettiRoughness(int x, int y, int z) {
        double roughnessModulator = NoiseUtils.sampleNoiseAndMapToRange(this.spaghettiRoughnessModulator, x, y, z, 0.0, 0.1);
        return (0.4 - Math.abs(this.spaghettiRoughnessNoise.getValue(x, y, z))) * roughnessModulator;
    }
    
    private static double clampToUnit(double value) {
        return Mth.clamp(value, -1.0, 1.0);
    }
    
    private static double sampleWithRarity(NormalNoise noise, double x, double y, double z, double rarity) {
        return noise.getValue(x / rarity, y / rarity, z / rarity);
    }
    
    private double min(double first, double second, double thid) {
        return Math.min(Math.min(first, second), thid);
    }
    
    static final class QuantizedSpaghettiRarity {
        static double getSpaghettiRarity2D(double noise) {
            if (noise < -0.75) {
                return 0.5;
            } else if (noise < -0.5) {
                return 0.75;
            } else if (noise < 0.5) {
                return 1.0;
            } else {
                return noise < 0.75 ? 2.0 : 3.0;
            }
        }
        
        static double getSpaghettiRarity3D(double noise) {
            if (noise < -0.5) {
                return 0.75;
            } else if (noise < 0.0) {
                return 1.0;
            } else {
                return noise < 0.5 ? 1.5 : 2.0;
            }
        }
    }
}