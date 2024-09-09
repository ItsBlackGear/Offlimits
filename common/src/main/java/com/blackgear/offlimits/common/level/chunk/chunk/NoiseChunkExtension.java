package com.blackgear.offlimits.common.level.chunk.chunk;

import com.blackgear.offlimits.common.level.chunk.stonesource.BaseStoneSource;

public interface NoiseChunkExtension {
    BaseStoneSource getBaseStoneSource();
    
    void setBaseStoneSource(BaseStoneSource source);
}