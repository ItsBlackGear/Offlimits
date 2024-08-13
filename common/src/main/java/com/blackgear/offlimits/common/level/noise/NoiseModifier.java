package com.blackgear.offlimits.common.level.noise;

public interface NoiseModifier {
    NoiseModifier PASSTHROUGH = (density, x, y, z) -> density;
    
    double modifyNoise(double substance, int x, int y, int z);
}