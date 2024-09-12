package com.blackgear.offlimits.common;

import com.blackgear.offlimits.common.level.levelgen.OreFeatureReplacements;
import com.blackgear.offlimits.common.level.levelgen.UndergroundFeatureReplacements;
import com.blackgear.platform.core.ParallelDispatch;

public class CommonSetup {
    public static void postStartup(ParallelDispatch dispatch) {
        dispatch.enqueueWork(() -> {
            UndergroundFeatureReplacements.FEATURES.register();
            OreFeatureReplacements.FEATURES.register();
            
            WorldGeneration.bootstrap();
        });
    }
}