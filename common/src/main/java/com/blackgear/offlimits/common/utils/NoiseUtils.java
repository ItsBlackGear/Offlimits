package com.blackgear.offlimits.common.utils;

import com.blackgear.platform.core.util.MathUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class NoiseUtils {
    public static double sampleNoiseAndMapToRange(NormalNoise noise, double x, double y, double z, double start, double end) {
        double value = noise.getValue(x, y, z);
        return MathUtils.map(value, -1.0, 1.0, start, end);
    }
    
    /**
     * Creates a NormalNoise instance with the given parameters.
     *
     * @param random The random number generator.
     * @param offset The offset for the noise.
     * @param octaves The octaves for the noise.
     * @return A NormalNoise instance.
     */
    public static NormalNoise normal(WorldgenRandom random, int offset, double... octaves) {
        return NormalNoise.create(random, offset, new DoubleArrayList(octaves));
    }
    
    /**
     * Creates a NormalNoise instance with the given parameters.
     *
     * @param seed The seed for world generation.
     * @param offset The offset for the noise.
     * @param octaves The octaves for the noise.
     * @return A NormalNoise instance.
     */
    public static NormalNoise normal(long seed, int offset, double... octaves) {
        return NormalNoise.create(new WorldgenRandom(seed), offset, new DoubleArrayList(octaves));
    }
}