package com.blackgear.offlimits.common.level;

import com.blackgear.offlimits.common.level.noise.NoiseModifier;
import com.blackgear.offlimits.common.utils.NoiseUtils;
import com.blackgear.offlimits.common.utils.SimpleRandom;
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
    private static final int SURFACE_DENSITY_THRESHOLD = 170;
    
    public Cavifier(WorldgenRandom random, int minChunkY) {
        this.minChunkY = minChunkY;
        this.pillarNoiseSource = NoiseUtils.normal(new SimpleRandom(random.nextLong()), -7, 1.0, 1.0);
        this.pillarRarenessModulator = NoiseUtils.normal(new SimpleRandom(random.nextLong()), -8, 1.0);
        this.pillarThicknessModulator = NoiseUtils.normal(new SimpleRandom(random.nextLong()), -8, 1.0);
        this.spaghetti2dNoiseSource = NoiseUtils.normal(new SimpleRandom(random.nextLong()), -7, 1.0);
        this.spaghetti2dElevationModulator = NoiseUtils.normal(new SimpleRandom(random.nextLong()), -8, 1.0);
        this.spaghetti2dRarityModulator = NoiseUtils.normal(new SimpleRandom(random.nextLong()), -11, 1.0);
        this.spaghetti2dThicknessModulator = NoiseUtils.normal(new SimpleRandom(random.nextLong()), -11, 1.0);
        this.spaghetti3dNoiseSource1 = NoiseUtils.normal(new SimpleRandom(random.nextLong()), -7, 1.0);
        this.spaghetti3dNoiseSource2 = NoiseUtils.normal(new SimpleRandom(random.nextLong()), -7, 1.0);
        this.spaghetti3dRarityModulator = NoiseUtils.normal(new SimpleRandom(random.nextLong()), -11, 1.0);
        this.spaghetti3dThicknessModulator = NoiseUtils.normal(new SimpleRandom(random.nextLong()), -8, 1.0);
        this.spaghettiRoughnessNoise = NoiseUtils.normal(new SimpleRandom(random.nextLong()), -5, 1.0);
        this.spaghettiRoughnessModulator = NoiseUtils.normal(new SimpleRandom(random.nextLong()), -8, 1.0);
        this.caveEntranceNoiseSource = NoiseUtils.normal(new SimpleRandom(random.nextLong()), -7, 0.4, 0.5, 1.0);
        this.layerNoiseSource = NoiseUtils.normal(new SimpleRandom(random.nextLong()), -8, 1.0);
        this.cheeseNoiseSource = NoiseUtils.normal(new SimpleRandom(random.nextLong()), -8, 0.5, 1.0, 2.0, 1.0, 2.0, 1.0, 0.0, 2.0, 0.0);
    }
    
    @Override
    public double modifyNoise(double density, int x, int y, int z) {
        boolean isBelowThreshold = density < SURFACE_DENSITY_THRESHOLD;
        
        double bigEntrances = this.getBigEntrances(x, y, z);
        double spaghettiRoughness = this.spaghettiRoughness(x, y, z);
        double spaghetti3d = this.getSpaghetti3d(x, y, z);
        
        if (isBelowThreshold) {
            double combined = Math.min(bigEntrances, spaghetti3d + spaghettiRoughness);
            return Math.min(density, combined * CHEESE_NOISE_RANGE * 5.0);
        } else {
            double cheeseNoise = this.cheeseNoiseSource.getValue(x, (double) y / 1.5, z);
            double clamped = Mth.clamp(cheeseNoise + 0.25, -1.0, 1.0);
            double densityFactor = (density - SURFACE_DENSITY_THRESHOLD) / 100.0;
            double interpolated = clamped + Mth.clampedLerp(0.5, 0.0, densityFactor);
            double layerizedCaverns = this.getLayerizedCaverns(x, y, z);
            double spaghetti2d = this.getSpaghetti2d(x, y, z);
            double layerNoiseOffset = interpolated + layerizedCaverns;
            double spaghettiNoiseOffset = Math.min(spaghetti3d, spaghetti2d) + spaghettiRoughness;
            double combined = this.min(layerNoiseOffset, bigEntrances, spaghettiNoiseOffset);
            double noise = Math.max(combined, this.getPillars(x, y, z));
            return CHEESE_NOISE_RANGE * Mth.clamp(noise, -1.0, 1.0);
        }
    }
    
    private double getBigEntrances(int x, int y, int z) {
        return this.caveEntranceNoiseSource.getValue((double) x * 0.75, (double) y * 0.5, (double) z * 0.75) + 0.37;
    }
    
    private double getPillars(int x, int y, int z) {
        double pillarRareness = NoiseUtils.sampleNoiseAndMapToRange(this.pillarRarenessModulator, x, y, z, 0.0, 2.0);
        double pillarThickness = NoiseUtils.sampleNoiseAndMapToRange(this.pillarThicknessModulator, x, y, z, 0.0, 1.1);
        pillarThickness = Math.pow(pillarThickness, 3.0);
        double noise = this.pillarNoiseSource.getValue((double) x * 25.0, (double) y * 0.3, (double) z * 25.0);
        noise = pillarThickness * (noise * 2.0 - pillarRareness);
        return noise > 0.03 ? noise : Double.NEGATIVE_INFINITY;
    }
    
    private double getLayerizedCaverns(int x, int y, int z) {
        double noise = this.layerNoiseSource.getValue(x, y * 8, z);
        return Mth.square((float) noise) * 4.0;
    }
    
    private double getSpaghetti3d(int x, int y, int z) {
        double noise = this.spaghetti3dRarityModulator.getValue(x * 2, y, z * 2);
        double spaghettiRarity3D = QuantizedSpaghettiRarity.getSpaghettiRarity3D(noise);
        double thickness = NoiseUtils.sampleNoiseAndMapToRange(this.spaghetti3dThicknessModulator, x, y, z, 0.065, 0.088);
        double source1 = sampleWithRarity(this.spaghetti3dNoiseSource1, x, y, z, spaghettiRarity3D);
        double value1 = Math.abs(spaghettiRarity3D * source1) - thickness;
        double source2 = sampleWithRarity(this.spaghetti3dNoiseSource2, x, y, z, spaghettiRarity3D);
        double value2 = Math.abs(spaghettiRarity3D * source2) - thickness;
        return clampToUnit(Math.max(value1, value2));
    }
    
    private double getSpaghetti2d(int x, int y, int z) {
        double noise = this.spaghetti2dRarityModulator.getValue(x * 2, y, z * 2);
        double sphaghettiRarity2D = QuantizedSpaghettiRarity.getSphaghettiRarity2D(noise);
        double thickness = NoiseUtils.sampleNoiseAndMapToRange(this.spaghetti2dThicknessModulator, x * 2, y, z * 2, 0.6, 1.3);
        double noiseSource = sampleWithRarity(this.spaghetti2dNoiseSource, x, y, z, sphaghettiRarity2D);
        double value1 = Math.abs(sphaghettiRarity2D * noiseSource) - 0.083 * thickness;
        double elevation = NoiseUtils.sampleNoiseAndMapToRange(this.spaghetti2dElevationModulator, x, 0.0, z, this.minChunkY, 8.0);
        double value2 = Math.abs(elevation - (double)y / 8.0) - thickness;
        value2 = value2 * value2 * value2;
        return clampToUnit(Math.max(value2, value1));
    }
    
    private double spaghettiRoughness(int x, int y, int z) {
        double noise = NoiseUtils.sampleNoiseAndMapToRange(this.spaghettiRoughnessModulator, x, y, z, 0.0, 0.1);
        return (0.4 - Math.abs(this.spaghettiRoughnessNoise.getValue(x, y, z))) * noise;
    }
    
    private static double clampToUnit(double delta) {
        return Mth.clamp(delta, -1.0, 1.0);
    }
    
    private static double sampleWithRarity(NormalNoise noise, double x, double y, double z, double offset) {
        return noise.getValue(x / offset, y / offset, z / offset);
    }
    
    private double min(double first, double second, double third) {
        return Math.min(Math.min(first, second), third);
    }
    
    static final class QuantizedSpaghettiRarity {
        static double getSphaghettiRarity2D(double noise) {
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