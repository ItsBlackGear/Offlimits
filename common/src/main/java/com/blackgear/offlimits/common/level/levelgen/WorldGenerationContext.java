package com.blackgear.offlimits.common.level.levelgen;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

public class WorldGenerationContext {
    private final NoiseGeneratorSettings settings;
    
    public WorldGenerationContext(NoiseGeneratorSettings settings) {
        this.settings = settings;
    }
    
    public boolean shouldGenerate() {
        return Offlimits.CONFIG.allowTerrainModifications.get() && this.settings.equals(NoiseGeneratorSettings.bootstrap());
    }
    
    public int getMinGenY() {
        return this.shouldGenerate()
            ? Math.max(Offlimits.INSTANCE.getMinBuildHeight(), 0)
            : 0;
    }
    
    public int getGenDepth() {
        return this.shouldGenerate()
            ? Math.min(Offlimits.INSTANCE.getMaxBuildHeight(), this.settings.noiseSettings().height())
            : this.settings.noiseSettings().height();
    }
}