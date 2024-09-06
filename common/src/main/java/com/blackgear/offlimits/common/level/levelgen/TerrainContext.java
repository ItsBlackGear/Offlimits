package com.blackgear.offlimits.common.level.levelgen;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.world.level.block.state.BlockState;

public class TerrainContext {
    private final BlockState defaultBlock, defaultFluid;
    private final int chunkCountX, chunkCountY, chunkCountZ;
    private final int chunkWidth, chunkHeight;
    private final int seaLevel;
    
    public TerrainContext(
        BlockState defaultBlock,
        BlockState defaultFluid,
        int chunkCountX,
        int chunkCountY,
        int chunkCountZ,
        int chunkWidth,
        int chunkHeight,
        int seaLevel
    ) {
        this.defaultBlock = defaultBlock;
        this.defaultFluid = defaultFluid;
        this.chunkCountX = chunkCountX;
        this.chunkCountY = chunkCountY;
        this.chunkCountZ = chunkCountZ;
        this.chunkWidth = chunkWidth;
        this.chunkHeight = chunkHeight;
        this.seaLevel = seaLevel;
    }
    
    public BlockState defaultBlock() {
        return this.defaultBlock;
    }
    
    public BlockState defaultFluid() {
        return this.defaultFluid;
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
    
    public int height() {
        return Offlimits.LEVEL.getHeight();
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
    
    public boolean allowTerrainModifications() {
        return Offlimits.CONFIG.allowTerrainModifications.get();
    }
    
    public boolean areNoiseCavesEnabled() {
        return Offlimits.CONFIG.areNoiseCavesEnabled.get();
    }
    
    public boolean areNoodleCavesEnabled() {
        return Offlimits.CONFIG.areNoodleCavesEnabled.get();
    }
    
    public boolean areAquifersEnabled() {
        return Offlimits.CONFIG.areAquifersEnabled.get();
    }
}