package com.blackgear.offlimits.common.level.levelgen;

import com.blackgear.offlimits.common.level.noise.NoiseSettingsExtension;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

public final class WorldGenContext {
    private final NoiseGeneratorSettings settings;
    private final TerrainContext context;
    
    public WorldGenContext(NoiseGeneratorSettings settings, TerrainContext context) {
        this.settings = settings;
        this.context = context;
    }
    
    public TerrainContext terrain() {
        return this.context;
    }
    
    public int getMinGenY() {
        return Math.max(this.context.minBuildHeight(), ((NoiseSettingsExtension) this.settings.noiseSettings()).minY());
    }
    
    public int getGenDepth() {
        return Math.min(this.context.maxBuildHeight(), this.settings.noiseSettings().height());
    }
}