package com.blackgear.offlimits.common;

import com.blackgear.offlimits.common.level.levelgen.OreFeatures;
import com.blackgear.offlimits.common.level.levelgen.UndergroundFeatures;
import com.blackgear.platform.core.ParallelDispatch;

public class CommonSetup {
    public static void postStartup(ParallelDispatch dispatch) {
        dispatch.enqueueWork(() -> {
            UndergroundFeatures.FEATURES.register();
            OreFeatures.FEATURES.register();
            
            WorldGeneration.bootstrap();
        });
    }
}