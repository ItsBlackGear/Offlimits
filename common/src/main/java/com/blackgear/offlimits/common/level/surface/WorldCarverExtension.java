package com.blackgear.offlimits.common.level.surface;

import com.blackgear.offlimits.common.level.Aquifer;
import com.blackgear.offlimits.common.level.levelgen.WorldGenContext;

public interface WorldCarverExtension {
    Aquifer aquifer();
    
    void setAquifer(Aquifer aquifer);
    
    WorldGenContext context();
    
    void setContext(WorldGenContext context);
}