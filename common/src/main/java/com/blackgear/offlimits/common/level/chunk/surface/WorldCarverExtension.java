package com.blackgear.offlimits.common.level.chunk.surface;

import com.blackgear.offlimits.common.level.chunk.Aquifer;
import com.blackgear.platform.common.worldgen.WorldGenerationContext;

public interface WorldCarverExtension {
    Aquifer aquifer();
    
    void setAquifer(Aquifer aquifer);
    
    WorldGenerationContext context();
    
    void setContext(WorldGenerationContext context);
}