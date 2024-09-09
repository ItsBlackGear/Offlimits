package com.blackgear.offlimits.common.level.chunk.noise;

import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;

public class NoiseInterpolator {
    private double[][] slice0, slice1;
    private final int chunkCountY, chunkCountZ;
    private final int minY;
    private final NoiseColumnFiller noiseColumnFiller;
    private double noise000, noise001, noise100, noise101, noise010, noise011, noise110, noise111;
    private double valueXZ00, valueXZ10, valueXZ01, valueXZ11;
    private double valueZ0, valueZ1;
    private final int firstCellXInChunk, firstCellZInChunk;
    
    public NoiseInterpolator(int chunkCountX, int chunkCountY, int chunkCountZ, ChunkPos chunkPos, int minY, NoiseColumnFiller noiseColumnFiller) {
        this.chunkCountY = chunkCountY;
        this.chunkCountZ = chunkCountZ;
        this.minY = minY;
        this.noiseColumnFiller = noiseColumnFiller;
        this.slice0 = allocateSlice(chunkCountY, chunkCountZ);
        this.slice1 = allocateSlice(chunkCountY, chunkCountZ);
        this.firstCellXInChunk = chunkPos.x * chunkCountX;
        this.firstCellZInChunk = chunkPos.z * chunkCountZ;
    }
    
    private static double[][] allocateSlice(int chunkCountY, int chunkCountZ) {
        int noiseZ = chunkCountZ + 1;
        int noiseY = chunkCountY + 1;
        double[][] buffer = new double[noiseZ][noiseY];
        
        for(int i = 0; i < noiseZ; ++i) {
            buffer[i] = new double[noiseY];
        }
        
        return buffer;
    }
    
    public void initializeForFirstCellX() {
        this.fillSlice(this.slice0, this.firstCellXInChunk);
    }
    
    public void advanceCellX(int chunkCountX) {
        this.fillSlice(this.slice1, this.firstCellXInChunk + chunkCountX + 1);
    }
    
    private void fillSlice(double[][] buffer, int x) {
        for(int j = 0; j < this.chunkCountZ + 1; ++j) {
            int z = this.firstCellZInChunk + j;
            this.noiseColumnFiller.fillNoiseColumn(buffer[j], x, z, this.minY, this.chunkCountY);
        }
    }
    
    public void selectCellYZ(int chunkCountY, int chunkCountZ) {
        this.noise000 = this.slice0[chunkCountZ][chunkCountY];
        this.noise001 = this.slice0[chunkCountZ + 1][chunkCountY];
        this.noise100 = this.slice1[chunkCountZ][chunkCountY];
        this.noise101 = this.slice1[chunkCountZ + 1][chunkCountY];
        this.noise010 = this.slice0[chunkCountZ][chunkCountY + 1];
        this.noise011 = this.slice0[chunkCountZ + 1][chunkCountY + 1];
        this.noise110 = this.slice1[chunkCountZ][chunkCountY + 1];
        this.noise111 = this.slice1[chunkCountZ + 1][chunkCountY + 1];
    }
    
    public void updateForY(double factorY) {
        this.valueXZ00 = Mth.lerp(factorY, this.noise000, this.noise010);
        this.valueXZ10 = Mth.lerp(factorY, this.noise100, this.noise110);
        this.valueXZ01 = Mth.lerp(factorY, this.noise001, this.noise011);
        this.valueXZ11 = Mth.lerp(factorY, this.noise101, this.noise111);
    }
    
    public void updateForX(double factorX) {
        this.valueZ0 = Mth.lerp(factorX, this.valueXZ00, this.valueXZ10);
        this.valueZ1 = Mth.lerp(factorX, this.valueXZ01, this.valueXZ11);
    }
    
    public double calculateValue(double factorZ) {
        return Mth.lerp(factorZ, this.valueZ0, this.valueZ1);
    }
    
    public void swapSlices() {
        double[][] slices = this.slice0;
        this.slice0 = this.slice1;
        this.slice1 = slices;
    }
    
    @FunctionalInterface
    public interface NoiseColumnFiller {
        void fillNoiseColumn(double[] buffer, int x, int z, int minY, int chunkCountY);
    }
}