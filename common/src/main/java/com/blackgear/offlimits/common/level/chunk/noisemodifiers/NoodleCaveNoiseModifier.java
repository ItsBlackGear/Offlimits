package com.blackgear.offlimits.common.level.chunk.noisemodifiers;

import com.blackgear.offlimits.common.level.chunk.TerrainContext;
import com.blackgear.offlimits.common.level.chunk.noise.NoiseInterpolator;
import net.minecraft.world.level.ChunkPos;

import java.util.function.Consumer;

public class NoodleCaveNoiseModifier implements NoiseModifier {
    private final NoiseInterpolator toggle;
    private final NoiseInterpolator thickness;
    private final NoiseInterpolator ridgeA;
    private final NoiseInterpolator ridgeB;
    private double factorZ;
    
    private final NoodleCavifier cavifier;
    private final TerrainContext context;
    
    public NoodleCaveNoiseModifier(ChunkPos pos, TerrainContext context, NoodleCavifier cavifier, int minY) {
        this.cavifier = cavifier;
        this.context = context;
        
        this.toggle = new NoiseInterpolator(context.chunkCountX(), context.chunkCountY(), context.chunkCountZ(), pos, minY, cavifier::fillToggleNoiseColumn);
        this.thickness = new NoiseInterpolator(context.chunkCountX(), context.chunkCountY(), context.chunkCountZ(), pos, minY, cavifier::fillThicknessNoiseColumn);
        this.ridgeA = new NoiseInterpolator(context.chunkCountX(), context.chunkCountY(), context.chunkCountZ(), pos, minY, cavifier::fillRidgeANoiseColumn);
        this.ridgeB = new NoiseInterpolator(context.chunkCountX(), context.chunkCountY(), context.chunkCountZ(), pos, minY, cavifier::fillRidgeBNoiseColumn);
    }
    
    public NoiseModifier prepare(double factorZ) {
        this.factorZ = factorZ;
        return this;
    }
    
    @Override
    public double modifyNoise(double density, int x, int y, int z) {
        double toggle = this.toggle.calculateValue(this.factorZ);
        double thickness = this.thickness.calculateValue(this.factorZ);
        double ridgeA = this.ridgeA.calculateValue(this.factorZ);
        double ridgeB = this.ridgeB.calculateValue(this.factorZ);
        return this.cavifier.noodleCavify(density, x, y, z, toggle, thickness, ridgeA, ridgeB, this.context.minY());
    }
    
    public void listInterpolators(Consumer<NoiseInterpolator> consumer) {
        consumer.accept(this.toggle);
        consumer.accept(this.thickness);
        consumer.accept(this.ridgeA);
        consumer.accept(this.ridgeB);
    }
}