package com.blackgear.offlimits.common.level.levelgen.noisemodifiers;

public interface NoiseModifier {
    NoiseModifier PASSTHROUGH = (density, x, y, z) -> density;
    
    double modifyNoise(double density, int x, int y, int z);
}