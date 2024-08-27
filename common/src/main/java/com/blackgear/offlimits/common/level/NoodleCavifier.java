package com.blackgear.offlimits.common.level;

import com.blackgear.offlimits.common.utils.MathUtils;
import com.blackgear.offlimits.common.utils.NoiseUtils;
import com.blackgear.offlimits.common.utils.SimpleRandom;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.Random;

public class NoodleCavifier {
    private static final int NOODLES_MAX_Y = 130;
    private static final double SPACING_AND_STRAIGHTNESS = 1.5;
    private static final double XZ_FREQUENCY = 2.6666666666666665;
    private static final double Y_FREQUENCY = 2.6666666666666665;
    
    private final NormalNoise toggleNoiseSource;
    private final NormalNoise thicknessNoiseSource;
    private final NormalNoise noodleANoiseSource;
    private final NormalNoise noodleBNoiseSource;
    
    public NoodleCavifier(long seed) {
        Random random = new Random(seed);
        this.toggleNoiseSource = NoiseUtils.normal(new SimpleRandom(random.nextLong()), -8, 1.0);
        this.thicknessNoiseSource = NoiseUtils.normal(new SimpleRandom(random.nextLong()), -8, 1.0);
        this.noodleANoiseSource = NoiseUtils.normal(new SimpleRandom(random.nextLong()), -7, 1.0);
        this.noodleBNoiseSource = NoiseUtils.normal(new SimpleRandom(random.nextLong()), -7, 1.0);
    }
    
    public void fillToggleNoiseColumn(double[] slices, int x, int z, int minY, int chunkCountY) {
        this.fillNoiseColumn(slices, x, z, minY, chunkCountY, this.toggleNoiseSource, 1.0);
    }
    
    public void fillThicknessNoiseColumn(double[] slices, int x, int z, int minY, int chunkCountY) {
        this.fillNoiseColumn(slices, x, z, minY, chunkCountY, this.thicknessNoiseSource, 1.0);
    }
    
    public void fillRidgeANoiseColumn(double[] slices, int x, int z, int minY, int chunkCountY) {
        this.fillNoiseColumn(slices, x, z, minY, chunkCountY, this.noodleANoiseSource, XZ_FREQUENCY, Y_FREQUENCY);
    }
    
    public void fillRidgeBNoiseColumn(double[] slices, int x, int z, int minY, int chunkCountY) {
        this.fillNoiseColumn(slices, x, z, minY, chunkCountY, this.noodleBNoiseSource, XZ_FREQUENCY, Y_FREQUENCY);
    }
    
    private void fillNoiseColumn(double[] slices, int x, int z, int minY, int chunkCountY, NormalNoise noiseSource, double frequency) {
        this.fillNoiseColumn(slices, x, z, minY, chunkCountY, noiseSource, frequency, frequency);
    }
    
    private void fillNoiseColumn(double[] slices, int x, int z, int minY, int chunkCountY, NormalNoise noiseSource, double xzFrequency, double yFrequency) {
        for (int y = 0; y < chunkCountY; ++y) {
            int realY = y + minY;
            int localX = x * 4;
            int localY = realY * 8;
            int localZ = z * 4;
            
            double noise = localY < 138
                ? NoiseUtils.sampleNoiseAndMapToRange(noiseSource, localX * xzFrequency, localY * yFrequency, localZ * xzFrequency, -1.0, 1.0)
                : 1.0;
            
            slices[y] = noise;
        }
    }
    
    public double noodleCavify(double density, int x, int y, int z, double toggle, double thickness, double ridgeA, double ridgeB, int minY) {
        if (y > NOODLES_MAX_Y || y < minY + 4 || density < 0.0 || toggle < 0.0) {
            return density;
        }
        
        double clampedThickness = MathUtils.clampedMap(thickness, -1.0, 1.0, 0.05, 0.1);
        double spacingA = Math.abs(SPACING_AND_STRAIGHTNESS * ridgeA) - clampedThickness;
        double spacingB = Math.abs(SPACING_AND_STRAIGHTNESS * ridgeB) - clampedThickness;
        double maxRidge = Math.max(spacingA, spacingB);
        
        return Math.min(density, maxRidge);
    }
}