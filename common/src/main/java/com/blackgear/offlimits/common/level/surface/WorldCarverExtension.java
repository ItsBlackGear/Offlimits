package com.blackgear.offlimits.common.level.surface;

import com.blackgear.offlimits.common.level.Aquifer;
import com.blackgear.offlimits.common.level.levelgen.WorldGenerationContext;

public interface WorldCarverExtension {
    Aquifer getAquifer();
    
    void setAquifer(Aquifer aquifer);
    
    WorldGenerationContext getContext();
    
    void setContext(WorldGenerationContext worldGenerationContext);
}