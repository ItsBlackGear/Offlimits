package com.blackgear.offlimits.common.level.levelgen.sampler;

import com.blackgear.offlimits.common.level.levelgen.TerrainContext;
import com.blackgear.offlimits.common.level.noise.BlendedNoise;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.jetbrains.annotations.Nullable;

public abstract class NoiseSampler {
    protected static final float[] BIOME_WEIGHTS = Util.make(new float[25], weights -> {
        for(int x = -2; x <= 2; ++x) {
            for(int z = -2; z <= 2; ++z) {
                float distance = 10.0F / Mth.sqrt((float)(x * x + z * z) + 0.2F);
                weights[x + 2 + (z + 2) * 5] = distance;
            }
        }
    });
    
    protected final BiomeSource biomeSource;
    protected final TerrainContext context;
    protected final NoiseSettings settings;
    protected final BlendedNoise blendedNoise;
    protected final @Nullable SimplexNoise islandNoise;
    protected final PerlinNoise depthNoise;
    protected final double topSlideTarget;
    protected final double topSlideSize;
    protected final double topSlideOffset;
    protected final double bottomSlideTarget;
    protected final double bottomSlideSize;
    protected final double bottomSlideOffset;
    protected final double dimensionDensityFactor;
    protected final double dimensionDensityOffset;
    
    public NoiseSampler(
        BiomeSource biomeSource,
        TerrainContext context,
        NoiseSettings settings,
        BlendedNoise blendedNoise,
        @Nullable SimplexNoise islandNoise,
        PerlinNoise depthNoise
    ) {
        this.biomeSource = biomeSource;
        this.context = context;
        this.settings = settings;
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
    }
    
    public abstract void fillNoiseColumn(double[] buffer, int x, int z, NoiseSettings settings, int seaLevel, int minY, int chunkCountY);
    
    public int getPreliminarySurfaceLevel(int x, int y, int z) {
        return y;
    }
    
    protected boolean isAbovePreliminarySurfaceLevel(double slide) {
        return slide < 50.0;
    }
    
    protected double[] getOffsetAndFactor(int x, int y, int z) {
        return new double[] {0.03, 342.8571468713332};
    }
}