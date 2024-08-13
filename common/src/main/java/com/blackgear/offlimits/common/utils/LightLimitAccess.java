package com.blackgear.offlimits.common.utils;

public class LightLimitAccess {
    private final HeightLimitAccess level;
    
    public LightLimitAccess(HeightLimitAccess level) {
        this.level = level;
    }
    
    public int getLightSectionCount() {
        return this.level.getSectionsCount() + 2;
    }
    
    public int getMinLightSection() {
        return this.level.getMinSection() - 1;
    }
    
    public int getMaxLightSection() {
        return this.getMinLightSection() + getLightSectionCount();
    }
}