package com.blackgear.offlimits.common.level.chunk.noise;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

import java.util.stream.IntStream;

public class BlendedNoise {
    private final PerlinNoise minLimitNoise;
    private final PerlinNoise maxLimitNoise;
    private final PerlinNoise mainNoise;
    
    public BlendedNoise(PerlinNoise minLimitNoise, PerlinNoise maxLimitNoise, PerlinNoise mainNoise) {
        this.minLimitNoise = minLimitNoise;
        this.maxLimitNoise = maxLimitNoise;
        this.mainNoise = mainNoise;
    }
    
    public BlendedNoise(WorldgenRandom random) {
        this(
            new PerlinNoise(random, IntStream.rangeClosed(-15, 0)),
            new PerlinNoise(random, IntStream.rangeClosed(-15, 0)),
            new PerlinNoise(random, IntStream.rangeClosed(-7, 0))
        );
    }
    
    public double sampleAndClampNoise(int x, int y, int z, double xzScale, double yScale, double xzFactor, double yFactor) {
        double minLimit = 0.0;
        double maxLimit = 0.0;
        double main = 0.0;
        double frequency = 1.0;
        
        // Sample main noise
        for(int octave = 0; octave < 8; ++octave) {
            ImprovedNoise mainNoise = this.mainNoise.getOctaveNoise(octave);
            if (mainNoise != null) {
                main += mainNoise.noise(
                    PerlinNoise.wrap((double) x * xzFactor * frequency),
                    PerlinNoise.wrap((double) y * yFactor * frequency),
                    PerlinNoise.wrap((double) z * xzFactor * frequency),
                    yFactor * frequency,
                    (double)y * yFactor * frequency
                ) / frequency;
            }

            frequency /= 2.0;
        }

        double normal = (main / 10.0 + 1.0) / 2.0;
        boolean isMaxNoise = normal >= 1.0;
        boolean isMinNoise = normal <= 0.0;
        frequency = 1.0;
        
        // Sample min and max limit noise
        for(int octave = 0; octave < 16; ++octave) {
            double wrappedX = PerlinNoise.wrap((double)x * xzScale * frequency);
            double wrappedY = PerlinNoise.wrap((double)y * yScale * frequency);
            double wrappedZ = PerlinNoise.wrap((double)z * xzScale * frequency);
            double scaledY = yScale * frequency;
            
            if (!isMaxNoise) {
                ImprovedNoise minNoise = this.minLimitNoise.getOctaveNoise(octave);
                if (minNoise != null) {
                    minLimit += minNoise.noise(wrappedX, wrappedY, wrappedZ, scaledY, (double) y * scaledY) / frequency;
                }
            }

            if (!isMinNoise) {
                ImprovedNoise maxNoise = this.maxLimitNoise.getOctaveNoise(octave);
                if (maxNoise != null) {
                    maxLimit += maxNoise.noise(wrappedX, wrappedY, wrappedZ, scaledY, (double) y * scaledY) / frequency;
                }
            }

            frequency /= 2.0;
        }
        
        // Interpolate between min and max noise
        return Mth.clampedLerp(minLimit / 512.0, maxLimit / 512.0, normal);
    }
}