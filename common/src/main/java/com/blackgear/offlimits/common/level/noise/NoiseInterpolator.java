package com.blackgear.offlimits.common.level.noise;

import com.blackgear.offlimits.Offlimits;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;

public class NoiseInterpolator {
    private double[][] slice0, slice1;
    private final int cellCountY, cellCountZ;
    private final int cellNoiseMinY;
    private final NoiseColumnFiller noiseColumnFiller;
    private double noise000, noise001, noise100, noise101, noise010, noise011, noise110, noise111;
    private double valueXZ00, valueXZ10, valueXZ01, valueXZ11;
    private double valueZ0, valueZ1;
    private final int firstCellXInChunk, firstCellZInChunk;
    
    public NoiseInterpolator(int cellCountX, int cellCountY, int cellCountZ, ChunkPos chunkPos, NoiseColumnFiller noiseColumnFiller) {
        this(cellCountX, cellCountY, cellCountZ, chunkPos, Offlimits.LEVEL.getMinBuildHeight(), noiseColumnFiller);
    }
    
    public NoiseInterpolator(int cellCountX, int cellCountY, int cellCountZ, ChunkPos chunkPos, int cellNoiseMinY, NoiseColumnFiller noiseColumnFiller) {
        this.cellCountY = cellCountY;
        this.cellCountZ = cellCountZ;
        this.cellNoiseMinY = cellNoiseMinY;
        this.noiseColumnFiller = noiseColumnFiller;
        this.slice0 = allocateSlice(cellCountY, cellCountZ);
        this.slice1 = allocateSlice(cellCountY, cellCountZ);
        this.firstCellXInChunk = chunkPos.x * cellCountX;
        this.firstCellZInChunk = chunkPos.z * cellCountZ;
    }
    
    private static double[][] allocateSlice(int cellCountY, int cellCountZ) {
        int chunkCountZ = cellCountZ + 1;
        int chunkCountY = cellCountY + 1;
        double[][] slices = new double[chunkCountZ][chunkCountY];
        
        for(int noiseZ = 0; noiseZ < chunkCountZ; ++noiseZ) {
            slices[noiseZ] = new double[chunkCountY];
        }
        
        return slices;
    }
    
    public void initializeForFirstCellX() {
        this.fillSlice(this.slice0, this.firstCellXInChunk);
    }
    
    public void advanceCellX(int cellX) {
        this.fillSlice(this.slice1, this.firstCellXInChunk + cellX + 1);
    }
    
    private void fillSlice(double[][] slice, int cellX) {
        for(int noiseZ = 0; noiseZ < this.cellCountZ + 1; ++noiseZ) {
            int cellZ = this.firstCellZInChunk + noiseZ;
            this.noiseColumnFiller.fillNoiseColumn(slice[noiseZ], cellX, cellZ, this.cellNoiseMinY, this.cellCountY);
        }
    }
    
    public void selectCellYZ(int cellY, int cellZ) {
        this.noise000 = this.slice0[cellZ][cellY];
        this.noise001 = this.slice0[cellZ + 1][cellY];
        this.noise100 = this.slice1[cellZ][cellY];
        this.noise101 = this.slice1[cellZ + 1][cellY];
        this.noise010 = this.slice0[cellZ][cellY + 1];
        this.noise011 = this.slice0[cellZ + 1][cellY + 1];
        this.noise110 = this.slice1[cellZ][cellY + 1];
        this.noise111 = this.slice1[cellZ + 1][cellY + 1];
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
        void fillNoiseColumn(double[] slices, int x, int z, int cellNoiseMinY, int cellCountY);
    }
}