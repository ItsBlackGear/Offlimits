package com.blackgear.offlimits.common.level.noiseModifiers;

import com.blackgear.offlimits.Offlimits;
import com.blackgear.offlimits.common.level.NoodleCavifier;
import com.blackgear.offlimits.common.level.noise.NoiseInterpolator;
import com.blackgear.offlimits.common.level.noise.NoiseModifier;
import net.minecraft.world.level.ChunkPos;

import java.util.function.Consumer;

public class NoodleCaveNoiseModifier implements NoiseModifier {
    private final NoiseInterpolator toggle;
    private final NoiseInterpolator thickness;
    private final NoiseInterpolator ridgeA;
    private final NoiseInterpolator ridgeB;
    private double factorZ;
    
    private final NoodleCavifier cavifier;
    
    public NoodleCaveNoiseModifier(ChunkPos pos, int chunkCountX, int chunkCountY, int chunkCountZ, NoodleCavifier cavifier, int cellNoiseMinY) {
        this.cavifier = cavifier;
        
        this.toggle = new NoiseInterpolator(chunkCountX, chunkCountY, chunkCountZ, pos, cellNoiseMinY, cavifier::fillToggleNoiseColumn);
        this.thickness = new NoiseInterpolator(chunkCountX, chunkCountY, chunkCountZ, pos, cellNoiseMinY, cavifier::fillThicknessNoiseColumn);
        this.ridgeA = new NoiseInterpolator(chunkCountX, chunkCountY, chunkCountZ, pos, cellNoiseMinY, cavifier::fillRidgeANoiseColumn);
        this.ridgeB = new NoiseInterpolator(chunkCountX, chunkCountY, chunkCountZ, pos, cellNoiseMinY, cavifier::fillRidgeBNoiseColumn);
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
        return this.cavifier.noodleCavify(density, x, y, z, toggle, thickness, ridgeA, ridgeB, Offlimits.INSTANCE.getMinBuildHeight());
    }
    
    public void listInterpolators(Consumer<NoiseInterpolator> consumer) {
        consumer.accept(this.toggle);
        consumer.accept(this.thickness);
        consumer.accept(this.ridgeA);
        consumer.accept(this.ridgeB);
    }
}