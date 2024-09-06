package com.blackgear.offlimits.common.level.levelgen;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.common.level.noise.NoiseGeneratorSettingsExtension;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

public class TerrainContext {
    private final NoiseGeneratorSettings settings;
    private final int chunkCountX, chunkCountY, chunkCountZ;
    private final int chunkWidth, chunkHeight;
    private final int seaLevel;
    
    public TerrainContext(
        NoiseGeneratorSettings settings,
        int chunkCountX,
        int chunkCountY,
        int chunkCountZ,
        int chunkWidth,
        int chunkHeight,
        int seaLevel
    ) {
        this.settings = settings;
        this.chunkCountX = chunkCountX;
        this.chunkCountY = chunkCountY;
        this.chunkCountZ = chunkCountZ;
        this.chunkWidth = chunkWidth;
        this.chunkHeight = chunkHeight;
        this.seaLevel = seaLevel;
    }
    
    public BlockState defaultBlock() {
        return this.settings.getDefaultBlock();
    }
    
    public BlockState defaultFluid() {
        return this.settings.getDefaultFluid();
    }
    
    public int chunkCountX() {
        return this.chunkCountX;
    }
    
    public int chunkCountY() {
        return this.chunkCountY;
    }
    
    public int chunkCountZ() {
        return this.chunkCountZ;
    }
    
    public int chunkWidth() {
        return this.chunkWidth;
    }
    
    public int chunkHeight() {
        return this.chunkHeight;
    }
    
    public int seaLevel() {
        return this.seaLevel;
    }
    
    public int maxBuildHeight() {
        return Offlimits.LEVEL.getMaxBuildHeight();
    }
    
    public int minBuildHeight() {
        return Offlimits.LEVEL.getMinBuildHeight();
    }
    
    public int minY() {
        return Offlimits.CONFIG.worldGenMinY.get();
    }
    
    public boolean areNoiseCavesEnabled() {
        return ((NoiseGeneratorSettingsExtension) this.settings).noiseCavesEnabled();
    }
    
    public boolean areNoodleCavesEnabled() {
        return ((NoiseGeneratorSettingsExtension) this.settings).noodleCavesEnabled();
    }
    
    public boolean areAquifersEnabled() {
        return ((NoiseGeneratorSettingsExtension) this.settings).aquifersEnabled();
    }
    
    public boolean isDeepslateEnabled() {
        return ((NoiseGeneratorSettingsExtension) this.settings).deepslateEnabled();
    }
    
    public boolean areOreVeinsEnabled() {
        return ((NoiseGeneratorSettingsExtension) this.settings).oreVeinsEnabled();
    }
}