package com.blackgear.offlimits.common.level.surface;

import com.blackgear.offlimits.common.level.Aquifer;

public interface WorldCarverExtension {
    Aquifer getAquifer();
    
    void setAquifer(Aquifer aquifer);
}