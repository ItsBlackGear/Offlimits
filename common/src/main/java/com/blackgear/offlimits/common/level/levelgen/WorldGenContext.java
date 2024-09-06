package com.blackgear.offlimits.common.level.levelgen;

import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

public final class WorldGenContext {
    private final NoiseGeneratorSettings settings;
    private final TerrainContext context;
    
    public WorldGenContext(NoiseGeneratorSettings settings, TerrainContext context) {
        this.settings = settings;
        this.context = context;
    }
    
    public boolean shouldGenerate() {
        return this.context.allowTerrainModifications() && this.settings.equals(NoiseGeneratorSettings.bootstrap());
    }
    
    public TerrainContext terrain() {
        return this.context;
    }
    
    public int getMinGenY() {
        return this.shouldGenerate()
            ? Math.max(this.context.minBuildHeight(), this.context.minY())
            : 0;
    }
    
    public int getGenDepth() {
        return this.shouldGenerate()
            ? Math.min(this.context.maxBuildHeight(), this.context.height())
            : this.settings.noiseSettings().height();
    }
}