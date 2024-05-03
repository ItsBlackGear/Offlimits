package com.blackgear.offlimits.common;

import com.blackgear.offlimits.Offlimits;

public interface LightLimitAccess {
    default int getLightSectionCount() {
        return Offlimits.INSTANCE.getSectionsCount() + 2;
    }
    
    default int getMinLightSection() {
        return Offlimits.INSTANCE.getMinSection() - 1;
    }
    
    default int getMaxLightSection() {
        return this.getMinLightSection() + this.getLightSectionCount();
    }
}