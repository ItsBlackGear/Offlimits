package com.blackgear.offlimits.common.level.levelgen;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.core.mixin.common.DimensionTypeAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public class ChunkGenContext {
    private final BlockState defaultBlock, defaultFluid;
    private final int chunkCountX, chunkCountY, chunkCountZ;
    private final int chunkWidth, chunkHeight;
    private final int seaLevel;
    private boolean allowTerrainModifications = Offlimits.CONFIG.allowTerrainModifications.get();
    
    public ChunkGenContext(
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
    
    public int minY() {
        return Offlimits.INSTANCE.getMinBuildHeight();
    }
    
    public int seaLevel() {
        return this.seaLevel;
    }
    
    public boolean allowTerrainModifications() {
        return this.allowTerrainModifications;
    }
    
    public boolean allowsTerrainModifications(LevelReader level) {
        this.allowTerrainModifications = level.dimensionType() == DimensionTypeAccessor.getDEFAULT_OVERWORLD() && Offlimits.CONFIG.allowTerrainModifications.get();
        return this.allowTerrainModifications;
    }
}