package com.blackgear.offlimits.common.level.levelgen;

import com.blackgear.offlimits.common.level.levelgen.chunk.NoiseChunk;

public final class WorldGenContext {
    private final TerrainContext context;
    private final int minY;
    private final int height;
    
    public WorldGenContext(NoiseChunk chunk, TerrainContext context) {
        this.context = context;
        this.minY = Math.max(context.minBuildHeight(), chunk.minY());
        this.height = Math.min(context.maxBuildHeight(), chunk.height());
    }
    
    public TerrainContext terrain() {
        return this.context;
    }
    
    public int getMinGenY() {
        return this.minY;
    }
    
    public int getGenDepth() {
        return this.height;
    }
}